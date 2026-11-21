package net.amemah.mobdefenders.spawner;

import net.amemah.mobdefenders.ModContent;
import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** Bounded ownership per source. Missing loaded UUIDs are never treated as confirmed deaths. */
public final class AllySpawnerData extends SavedData {
	// Keep this legacy storage key so existing ownership/deadlines are not lost by the registry rename.
	private static final SavedDataType<AllySpawnerData> TYPE = new SavedDataType<>(
		ModContent.id("zombie_spawners"), AllySpawnerData::new,
		CompoundTag.CODEC.xmap(AllySpawnerData::read, AllySpawnerData::write), null);
	private final Map<BlockPos, Source> sources = new HashMap<>();
	private final Map<UUID, Source> ownership = new HashMap<>();
	private final Set<UUID> pendingDeaths = new HashSet<>();

	public static final class Source {
		public final BlockPos pos;
		public final SpawnerKind kind;
		private final Set<UUID> entities = new LinkedHashSet<>();
		private final Set<UUID> entitiesView = Collections.unmodifiableSet(entities);
		int quietTicks;
		public long readyAt;
		public boolean standing = true;
		private Source(BlockPos pos, long readyAt, SpawnerKind kind) {
			this.pos = pos.immutable(); this.readyAt = readyAt; this.kind = kind;
		}
		public Set<UUID> entities() { return entitiesView; }
		public UUID entity() { return entities.isEmpty() ? null : entities.iterator().next(); }
	}

	public static AllySpawnerData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
	public List<Source> sources() { return List.copyOf(sources.values()); }
	public Source source(BlockPos pos) { return sources.get(pos); }
	public Source owner(UUID uuid) { return ownership.get(uuid); }

	public void register(BlockPos pos, long now) {
		register(pos, now, SpawnerKind.ZOMBIE_ALLY);
	}
	public void register(BlockPos pos, long now, SpawnerKind kind) {
		if (!sources.containsKey(pos)) {
			sources.put(pos.immutable(), new Source(pos, now + AllySpawnerConfig.get(kind).initialDelayTicks(), kind));
			setDirty();
		}
	}

	public void occupy(Source source, UUID uuid) {
		replace(source, List.of(), List.of(uuid), 0);
	}

	public void death(UUID uuid, long now) {
		Source source = owner(uuid);
		if (source != null) replace(source, List.of(uuid), List.of(), now);
	}

	/** Change ownership as one operation so splits/merges never schedule a replacement. */
	void replace(Source source, Collection<UUID> removed, Collection<UUID> added, long now) {
		var retiring = List.copyOf(removed);
		if (sources.get(source.pos) != source || !source.entities.containsAll(retiring)
			|| new HashSet<>(retiring).size() != retiring.size()
			|| source.entities.size() - retiring.size() + added.size() > source.kind.capacity()
			|| new HashSet<>(added).size() != added.size() || added.stream().anyMatch(ownership::containsKey))
			throw new IllegalStateException("Invalid spawner ownership change");
		for (UUID uuid : retiring) {
			source.entities.remove(uuid);
			ownership.remove(uuid);
		}
		for (UUID uuid : added) {
			source.entities.add(uuid);
			ownership.put(uuid, source);
		}
		if (source.entities.isEmpty()) source.readyAt = now + AllySpawnerConfig.get(source.kind).delayTicks();
		setDirty();
	}

	public void toggle(BlockPos pos) {
		Source source = sources.get(pos);
		if (source != null) {
			source.standing = !source.standing;
			setDirty();
		}
	}

	public void unregister(ServerLevel level, BlockPos pos) {
		Source source = sources.remove(pos);
		if (source == null) return;
		var retiring = List.copyOf(source.entities);
		source.entities.clear();
		for (UUID uuid : retiring) ownership.remove(uuid);
		for (UUID uuid : retiring) {
			var entity = level.getEntity(uuid);
			if (entity != null) entity.kill(level);
			else pendingDeaths.add(uuid);
		}
		setDirty();
	}

	public boolean consumePendingDeath(UUID uuid) {
		boolean removed = pendingDeaths.remove(uuid);
		if (removed) setDirty();
		return removed;
	}

	public CompoundTag write() {
		CompoundTag root = new CompoundTag();
		ListTag entries = new ListTag();
		for (Source source : sources.values()) {
			CompoundTag entry = new CompoundTag();
			entry.putLong("position", source.pos.asLong());
			entry.putString("kind", source.kind.name());
			entry.putLong("ready_at", source.readyAt);
			entry.putBoolean("standing", source.standing);
			if (source.kind.hasSplitFamily()) {
				ListTag members = new ListTag();
				for (UUID uuid : source.entities) {
					CompoundTag member = new CompoundTag();
					member.putString("entity", uuid.toString());
					members.add(member);
				}
				entry.put("entities", members);
			} else if (source.entity() != null) entry.putString("entity", source.entity().toString());
			entries.add(entry);
		}
		root.put("sources", entries);
		ListTag deaths = new ListTag();
		for (UUID uuid : pendingDeaths) {
			CompoundTag entry = new CompoundTag();
			entry.putString("entity", uuid.toString());
			deaths.add(entry);
		}
		root.put("pending_deaths", deaths);
		return root;
	}

	public static AllySpawnerData read(CompoundTag root) {
		AllySpawnerData data = new AllySpawnerData();
		ListTag entries = root.getListOrEmpty("sources");
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entry = entries.getCompoundOrEmpty(i);
			Source source = new Source(BlockPos.of(entry.getLongOr("position", 0)), entry.getLongOr("ready_at", 0),
				SpawnerKind.valueOf(entry.getStringOr("kind", "ZOMBIE_ALLY")));
			source.standing = entry.getBooleanOr("standing", true);
			data.sources.put(source.pos, source);
			entry.getString("entity").ifPresent(uuid -> data.readOwner(source, uuid));
			for (var member : entry.getListOrEmpty("entities"))
				if (member instanceof CompoundTag tag) tag.getString("entity").ifPresent(uuid -> data.readOwner(source, uuid));
		}
		ListTag deaths = root.getListOrEmpty("pending_deaths");
		for (int i = 0; i < deaths.size(); i++) {
			deaths.getCompoundOrEmpty(i).getString("entity").map(UUID::fromString).ifPresent(data.pendingDeaths::add);
		}
		return data;
	}
	private void readOwner(Source source, String value) {
		UUID uuid = UUID.fromString(value);
		if (source.entities.size() < source.kind.capacity() && ownership.putIfAbsent(uuid, source) == null)
			source.entities.add(uuid);
	}
}
