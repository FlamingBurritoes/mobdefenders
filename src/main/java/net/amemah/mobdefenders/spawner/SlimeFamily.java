package net.amemah.mobdefenders.spawner;

import net.amemah.mobdefenders.ModContent;
import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.SlimeAlly;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class SlimeFamily {
	private static final int QUIET_TICKS = 20 * 20;

	static boolean split(ServerLevel level, AllySpawnerData data, AllySpawnerData.Source source, SlimeAlly parent) {
		if (parent.getSize() <= 1) return false;
		var children = new ArrayList<SlimeAlly>(2);
		for (int i = 0; i < 2; i++) {
			var child = ModContent.SLIME_ALLY.create(level, EntitySpawnReason.SPAWNER);
			if (child == null) {
				children.forEach(Entity::discard);
				return false;
			}
			child.setSize(parent.getSize() - 1, true);
			var pos = parent.position();
			double offset = child.getBbWidth() * .55 * (i == 0 ? -1 : 1);
			for (Vec3 candidate : List.of(pos.add(offset, 0, 0), pos.add(0, 0, offset), pos.add(0, .5, 0))) {
				if (AllySpawnerManager.safe(level, child, candidate, e -> e == parent, false)) {
					pos = candidate;
					break;
				}
			}
			// Like vanilla splitting, use the death position if the nearby space is cramped.
			child.snapTo(pos, parent.getYRot(), 0);
			child.setHomeTo(source.pos, AllySpawnerConfig.get(source.kind).roamRadius());
			if (!level.addFreshEntity(child)) {
				children.forEach(Entity::discard);
				return false;
			}
			children.add(child);
		}
		data.replace(source, List.of(parent.getUUID()), children.stream().map(Entity::getUUID).toList(), level.getGameTime());
		source.quietTicks = 0;
		return true;
	}

	static void tryMerge(ServerLevel level, AllySpawnerData data, AllySpawnerData.Source source) {
		if (source.entities().size() < 2 || !level.isPositionEntityTicking(source.pos)) {
			source.quietTicks = 0;
			return;
		}
		int radius = AllySpawnerConfig.get(source.kind).detectionRadius();
		var detection = TargetingConditions.forCombat().range(radius).ignoreLineOfSight();
		var members = new ArrayList<SlimeAlly>(source.entities().size());
		for (var uuid : source.entities()) {
			if (!(level.getEntity(uuid) instanceof SlimeAlly slime) || !slime.isAlive()
				|| !level.isPositionEntityTicking(slime.blockPosition()) || slime.getTarget() != null
				|| !level.getEntitiesOfClass(LivingEntity.class, slime.getBoundingBox().inflate(radius),
					e -> slime.canAttack(e) && detection.test(level, slime, e)).isEmpty()) {
				source.quietTicks = 0;
				return;
			}
			members.add(slime);
		}
		source.quietTicks = Math.min(QUIET_TICKS, source.quietTicks + 1);
		if (source.quietTicks < QUIET_TICKS) return;
		// Retry an obstructed merge without consuming any of the family.
		if (level.getGameTime() % 20 == 0) merge(level, data, source, members);
	}

	private static void merge(ServerLevel level, AllySpawnerData data, AllySpawnerData.Source source, List<SlimeAlly> members) {
		var merged = ModContent.SLIME_ALLY.create(level, EntitySpawnReason.SPAWNER);
		if (merged == null) return;
		var settings = AllySpawnerConfig.get(source.kind);
		Vec3 position = AllySpawnerManager.findPosition(level, source, merged, settings.roamRadius(), false,
			e -> source.entities().contains(e.getUUID()));
		if (position == null) return;
		double mass = 0;
		for (var slime : members)
			mass += (1 << (slime.getSize() - 1)) * Mth.clamp(slime.getHealth() / slime.getMaxHealth(), 0F, 1F);
		merged.setHealth((float) Mth.clamp(merged.getMaxHealth() * mass / 8, 1, merged.getMaxHealth()));
		merged.snapTo(position, 0, 0);
		merged.setHomeTo(source.pos, settings.roamRadius());
		if (!level.addFreshEntity(merged)) return;
		data.replace(source, members.stream().map(Entity::getUUID).toList(), List.of(merged.getUUID()), level.getGameTime());
		source.quietTicks = 0;
		members.forEach(Entity::discard);
		AllySpawnerManager.spawnEffects(level, merged);
	}

	private SlimeFamily() { }
}
