package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Vanilla climbing and leap/melee movement with friendly, daylight-independent guard AI. */
public final class SpiderAlly extends Spider implements Defender {
	public SpiderAlly(EntityType<? extends Spider> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}
	public static AttributeSupplier.Builder createAllyAttributes() {
		return Spider.createAttributes().add(Attributes.ATTACK_DAMAGE, 2);
	}
	@Override protected void registerGoals() {
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, .4F));
		// Vanilla SpiderAttackGoal randomly abandons targets in daylight; defenders must not.
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1, false));
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}
	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target == null || canAttack(target) ? target : null); }
	@Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		// No hostile skeleton jockey or random Hard-mode potion buffs from Spider.finalizeSpawn.
		return data;
	}
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
