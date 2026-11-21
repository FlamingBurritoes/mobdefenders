package net.amemah.mobdefenders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BruteAllyModelTest {
	@Test void suppliedGeometryBakesAndKeepsNativeAxePose() {
		var root = BruteAllyModel.createBodyLayer().bakeRoot();
		var model = new BruteAllyModel(root);
		int[] cubes = {0};
		root.visit(new PoseStack(), (pose, part, index, cube) -> cubes[0]++);
		assertEquals(16, cubes[0]); // Head, snout, tusks, ears, body/limbs and their clothing shells.
		assertEquals(Math.toRadians(35), model.rightEar.getInitialPose().zRot(), 1e-6);
		assertEquals(2, model.leftLeg.x);

		var state = new PiglinRenderState();
		state.mainArm = HumanoidArm.RIGHT;
		state.armPose = PiglinArmPose.DEFAULT;
		model.setupAnim(state);
		float idleArm = model.rightArm.xRot;
		state.armPose = PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON;
		model.setupAnim(state);
		assertTrue(model.rightArm.xRot < idleArm, "Native axe pose should raise the weapon arm");
	}
}
