package net.amemah.mobdefenders;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.DrownedAlly;
import net.amemah.mobdefenders.spawner.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class DrownedAllySpawnerGameTests {
	private AllySpawnerData.Source place(GameTestHelper h, BlockPos relative, boolean water) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var level = h.getLevel();
		var pos = h.absolutePos(relative);
		level.setBlockAndUpdate(pos, ModContent.DROWNED_ALLY_SPAWNER.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, water));
		var source = AllySpawnerData.get(level).source(pos);
		var settings = AllySpawnerConfig.get(SpawnerKind.DROWNED_ALLY);
		h.assertTrue(source.kind == SpawnerKind.DROWNED_ALLY && source.entities().isEmpty() && source.standing
			&& source.readyAt == level.getGameTime() + settings.initialDelayTicks(), "Shared initial delay and standing mode");
		source.readyAt = level.getGameTime();
		return source;
	}

	private DrownedAlly ally(GameTestHelper h, AllySpawnerData.Source source) {
		h.assertTrue(source.entities().size() == 1, "Expected one owned Drowned");
		var ally = h.getLevel().getEntity(source.entity());
		h.assertTrue(ally instanceof DrownedAlly, "Wrong ally type");
		return (DrownedAlly) ally;
	}

	@GameTest(maxTicks = 450, padding = 36, skyAccess = true)
	public void submergedGuardSpawnsSwimsAndReturnsToPost(GameTestHelper h) {
		for (int x = 1; x <= 6; x++) for (int z = 1; z <= 6; z++) for (int y = 1; y <= 7; y++)
			h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
		var source = place(h, new BlockPos(3, 4, 3), true);
		var data = AllySpawnerData.get(h.getLevel());
		h.assertTrue(h.getLevel().getFluidState(source.pos).is(FluidTags.WATER), "Waterlogged spawner retains water");
		h.runAtTickTime(30, () -> {
			var ally = ally(h, source);
			var post = Vec3.atBottomCenterOf(source.pos.above());
			h.assertTrue(ally.isUnderWater() && ally.position().distanceToSqr(post) < .1, "Standing should hold its underwater post");
			data.toggle(source.pos);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(Math.abs(ally.getY() - post.y) > .6 && ally.isSwimming(),
				"Wander should swim vertically"))
				.thenExecute(() -> data.toggle(source.pos))
				.thenWaitUntil(() -> h.assertTrue(ally.position().distanceToSqr(post) < .1,
					"Standing must swim back to the underwater post"))
				.thenExecute(() -> {
					ally.kill(h.getLevel());
					h.assertTrue(source.entities().isEmpty() && source.readyAt == h.getLevel().getGameTime()
						+ AllySpawnerConfig.get(source.kind).delayTicks(), "Shared replacement delay");
				}).thenSucceed();
		});
	}

	@GameTest(maxTicks = 150, padding = 36, skyAccess = true)
	public void underwaterIdleMovementCannotCrossSevenBlockCeilingOrFloor(GameTestHelper h) {
		// Use an elevated column so both vertical boundaries are in the water.
		for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) for (int y = 1; y <= 19; y++)
			h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
		var source = place(h, new BlockPos(3, 9, 3), true);
		h.runAtTickTime(30, () -> {
			var ally = ally(h, source);
			AllySpawnerData.get(h.getLevel()).toggle(source.pos);
			for (int sign : new int[]{1, -1}) {
				ally.snapTo(source.pos.getX() + .5, source.pos.getY() + sign * 6.8, source.pos.getZ() + .5, 0, 0);
				h.assertTrue(ally.isInWater(), "Fixture must exercise swimming movement");
				ally.move(MoverType.SELF, new Vec3(0, sign * 2, 0));
				h.assertTrue(Math.abs(ally.getY() - (source.pos.getY() + sign * 7)) < .01,
					"Idle swimming must stop at exactly seven vertical blocks in either direction");
			}
			h.succeed();
		});
	}

	@GameTest(maxTicks = 300, padding = 36, skyAccess = true)
	public void tridentOnlyNoPickupAndBothAttacksAtCloseRange(GameTestHelper h) {
		var source = place(h, new BlockPos(3, 1, 3), false);
		var level = h.getLevel();
		h.setTime(6000);
		h.runAtTickTime(30, () -> {
			var ally = ally(h, source);
			h.assertTrue(ally.getHealth() == 20 && !ally.isOnFire() && ally.getAttributeValue(Attributes.ARMOR) == 2,
				"Normal stats and sun safety");
			h.assertTrue(ally.getAttributeValue(Attributes.ATTACK_DAMAGE) == 3 && ally.getMainHandItem().is(Items.TRIDENT),
				"Normal melee damage and a guaranteed trident");
			for (var slot : EquipmentSlot.values())
				if (slot != EquipmentSlot.MAINHAND) h.assertTrue(ally.getItemBySlot(slot).isEmpty(), "No secondary gear");
			h.assertTrue(!ally.canPickUpLoot() && !ally.wantsToPickUp(level, new ItemStack(Items.DIAMOND_SWORD)), "No pickup");
			var turtle = EntityType.TURTLE.create(level, EntitySpawnReason.COMMAND);
			turtle.snapTo(ally.position().add(0, 0, .5), 0, 0);
			h.assertTrue(!ally.canAttack(turtle), "Turtles are not targets");
			var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.7, 2, 3.5));
			enemy.setPersistenceRequired();
			enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
			enemy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
			enemy.setNoGravity(true);
			h.startSequence()
				.thenWaitUntil(() -> h.assertTrue(enemy.getHealth() == 17, "Expected a 3-point melee strike before the first throw"))
				.thenWaitUntil(() -> h.assertTrue(!level.getEntitiesOfClass(ThrownTrident.class, ally.getBoundingBox().inflate(8),
					t -> t.getOwner() == ally).isEmpty(), "Close-range melee must not prevent the real trident throw"))
				.thenExecute(enemy::discard).thenSucceed();
		});
	}

	@GameTest(padding = 24)
	public void recipeAcceptsMixedCoralsAndRequiresIron(GameTestHelper h) {
		var level = h.getLevel();
		var flesh = new ItemStack(Items.ROTTEN_FLESH);
		List<ItemStack> grid = new ArrayList<>(List.of(new ItemStack(Items.BRAIN_CORAL), flesh, new ItemStack(Items.FIRE_CORAL_FAN),
			flesh, new ItemStack(ModContent.IRON_REINFORCED_PLANKS_ITEM), flesh, new ItemStack(Items.TUBE_CORAL_BLOCK),
			flesh, new ItemStack(Items.DEAD_HORN_CORAL)));
		var input = CraftingInput.of(3, 3, grid);
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("drowned_ally_spawner"))
			&& recipe.value().assemble(input).is(ModContent.DROWNED_ALLY_SPAWNER_ITEM), "Exact recipe accepts mixed corals");
		grid.set(4, new ItemStack(ModContent.COPPER_REINFORCED_PLANKS_ITEM));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Iron, not copper");
		h.succeed();
	}
}
