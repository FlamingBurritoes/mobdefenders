package net.amemah.mobdefenders.client;

import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkeletonAllyModelTest {
	@Test void matchesSuppliedThinLimbsAndPivots() {
		var root = SkeletonAllyModel.createBodyLayer().bakeRoot();
		var model = new SkeletonModel<SkeletonRenderState>(root);
		assertEquals(8, root.getAllParts().size());
		assertNotNull(model.hat);
		assertEquals(-5, model.rightArm.x);
		assertEquals(5, model.leftArm.x);
		assertEquals(-2, model.rightLeg.x);
		assertEquals(2, model.leftLeg.x);
		for (var part : java.util.List.of(model.leftArm, model.rightArm, model.leftLeg, model.rightLeg)) {
			var cube = part.getRandomCube(RandomSource.create());
			assertEquals(2, cube.maxX - cube.minX);
			assertEquals(12, cube.maxY - cube.minY);
			assertEquals(2, cube.maxZ - cube.minZ);
		}
	}
	@Test void uses64By32TextureAndOriginalArmRegion() {
		var root = SkeletonAllyModel.createBodyLayer().bakeRoot();
		var cube = root.getChild("left_arm").getRandomCube(RandomSource.create());
		float maxV = 0;
		for (var face : cube.polygons) for (var vertex : face.vertices()) {
			assertTrue(vertex.u() >= 40 / 64F && vertex.u() <= 48 / 64F);
			maxV = Math.max(maxV, vertex.v());
		}
		assertEquals(30 / 32F, maxV);
	}
}
