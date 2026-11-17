package net.amemah.mobdefenders.spawner;

import net.amemah.mobdefenders.ModContent;
import net.amemah.mobdefenders.config.ZombieSpawnerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** One slot per source. Missing loaded UUIDs are never treated as confirmed deaths. */
public final class ZombieSpawnerData extends SavedData {
	private static final SavedDataType<ZombieSpawnerData> TYPE = new SavedDataType<>(
		ModContent.id("zombie_spawners"), ZombieSpawnerData::new,
		CompoundTag.CODEC.xmap(ZombieSpawnerData::read, ZombieSpawnerData::write), null);
	private final Map<BlockPos, Source> sources = new HashMap<>();
	private final Map<UUID, Source> ownership = new HashMap<>();
	private final Set<UUID> pendingDeaths = new HashSet<>();

	public static final class Source {
		public final BlockPos pos;
		public UUID entity;
		public long readyAt;
		public boolean standing = true;
		private Source(BlockPos pos, long readyAt) { this.pos = pos.immutable(); this.readyAt = readyAt; }
	}

	public static ZombieSpawnerData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
	public List<Source> sources() { return List.copyOf(sources.values()); }
	public Source source(BlockPos pos) { return sources.get(pos); }
	public Source owner(UUID uuid) { return ownership.get(uuid); }

	public void register(BlockPos pos, long now) {
		if (!sources.containsKey(pos)) {
			sources.put(pos.immutable(), new Source(pos, now + ZombieSpawnerConfig.get().initialDelayTicks()));
			setDirty();
		}
	}

	public void occupy(Source source, UUID uuid) {
		if (source.entity != null) throw new IllegalStateException("Spawner slot already occupied");
		source.entity = uuid;
		ownership.put(uuid, source);
		setDirty();
	}

	public void death(UUID uuid, long now) {
		Source source = ownership.remove(uuid);
		if (source != null) {
			source.entity = null;
			source.readyAt = now + ZombieSpawnerConfig.get().delayTicks();
			setDirty();
		}
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
		if (source.entity != null) {
			ownership.remove(source.entity); // Clear BEFORE normal death callbacks.
			var entity = level.getEntity(source.entity);
			if (entity != null) entity.kill(level);
			else pendingDeaths.add(source.entity);
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
			entry.putLong("ready_at", source.readyAt);
			entry.putBoolean("standing", source.standing);
			if (source.entity != null) entry.putString("entity", source.entity.toString());
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

	public static ZombieSpawnerData read(CompoundTag root) {
		ZombieSpawnerData data = new ZombieSpawnerData();
		ListTag entries = root.getListOrEmpty("sources");
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entry = entries.getCompoundOrEmpty(i);
			Source source = new Source(BlockPos.of(entry.getLongOr("position", 0)), entry.getLongOr("ready_at", 0));
			source.standing = entry.getBooleanOr("standing", true);
			source.entity = entry.getString("entity").map(UUID::fromString).orElse(null);
			data.sources.put(source.pos, source);
			if (source.entity != null) data.ownership.put(source.entity, source);
		}
		ListTag deaths = root.getListOrEmpty("pending_deaths");
		for (int i = 0; i < deaths.size(); i++) {
			deaths.getCompoundOrEmpty(i).getString("entity").map(UUID::fromString).ifPresent(data.pendingDeaths::add);
		}
		return data;
	}
}
