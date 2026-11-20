package net.amemah.mobdefenders.client;

import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndermanAllyModelTest {
	@Test void preservesSuppliedGeometryAndUsesNativeCombatPose() {
		var root = EndermanAllyModel.createBodyLayer().bakeRoot();
		var model = new EndermanModel<EndermanRenderState>(root);
		assertEquals(8, root.getAllParts().size());
		assertEquals(-15, model.head.y);
		assertEquals(-13, model.rightArm.y);
		assertEquals(-6, model.leftLeg.y);
		var cube = model.leftLeg.getRandomCube(RandomSource.create());
		assertEquals(30, cube.maxY - cube.minY);
		for (var face : cube.polygons) for (var vertex : face.vertices())
			assertTrue(vertex.u() >= 56 / 64F && vertex.u() <= 1);

		var state = new EndermanRenderState();
		state.isCreepy = true;
		state.walkAnimationSpeed = 1;
		model.setupAnim(state);
		assertEquals(-20, model.head.y);
		assertNotEquals(0, model.rightLeg.xRot);
		state.isCreepy = false;
		model.setupAnim(state);
		assertEquals(-15, model.head.y);
	}
}
