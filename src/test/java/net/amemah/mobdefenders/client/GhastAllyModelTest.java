package net.amemah.mobdefenders.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GhastAllyModelTest {
	@Test void suppliedPivotsAndTentacleKeysAreUsed() {
		var root = GhastAllyModel.createBodyLayer().bakeRoot();
		var model = new GhastAllyModel(root);
		assertEquals(11, root.getAllParts().size());
		var first = root.getChild("tentacle1");
		var third = root.getChild("tentacle3");
		assertEquals(-1.85F, first.x);
		assertEquals(17.5F, third.y);
		var state = new LivingEntityRenderState();
		model.setupAnim(state);
		assertEquals(Math.toRadians(22.92), first.xRot, 1e-6);
		assertEquals(Math.toRadians(33.34), third.xRot, 1e-6);
		state.ageInTicks = 5;
		model.setupAnim(state);
		assertEquals(Math.toRadians(34.38), first.xRot, 1e-6);
		assertEquals(Math.toRadians(18.15), third.xRot, 1e-6);
		state.ageInTicks = 40;
		model.setupAnim(state);
		assertEquals(Math.toRadians(22.92), first.xRot, 1e-6);
	}
}
