package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZombieAllyModelTest {
	@Test void bakesWithVanillaZombieHierarchyAndExactSourceLegBounds() {
		ModelPart root = ZombieAllyModel.createBodyLayer().bakeRoot();
		ZombieModel<ZombieRenderState> model = new ZombieModel<>(root);
		assertNotNull(model.hat);
		assertEquals(8, root.getAllParts().size()); // root plus seven supplied cubes/parts
		assertEquals(-1.9F, model.rightLeg.x, 0.00001);
		assertEquals(1.9F, model.leftLeg.x, 0.00001);
		ModelPart.Cube right = model.rightLeg.getRandomCube(RandomSource.create());
		ModelPart.Cube left = model.leftLeg.getRandomCube(RandomSource.create());
		assertEquals(-4, model.rightLeg.x + right.minX, 0.00001);
		assertEquals(0, model.rightLeg.x + right.maxX, 0.00001);
		assertEquals(0, model.leftLeg.x + left.minX, 0.00001);
		assertEquals(4, model.leftLeg.x + left.maxX, 0.00001);
	}

	@Test void leftLimbsUseSuppliedMirroredLegacyUvsNotModernSeparateRegions() {
		ModelPart root = ZombieAllyModel.createBodyLayer().bakeRoot();
		assertUvBounds(root.getChild("left_arm"), 40, 56, 16, 32);
		assertUvBounds(root.getChild("right_arm"), 40, 56, 16, 32);
		assertUvBounds(root.getChild("left_leg"), 0, 16, 16, 32);
		assertUvBounds(root.getChild("right_leg"), 0, 16, 16, 32);
		ModelPart.Cube left = root.getChild("left_arm").getRandomCube(RandomSource.create());
		ModelPart.Cube right = root.getChild("right_arm").getRandomCube(RandomSource.create());
		// Compare U against spatial X on the front face, not a vertex index:
		// the baker reverses vertex order when mirroring, so matching indices can retain the same U.
		assertEquals(-frontUvSlope(right), frontUvSlope(left), 0.00001);
	}

	private float frontUvSlope(ModelPart.Cube cube) {
		for (var polygon : cube.polygons) {
			if (polygon.normal().z() > -0.5F) continue;
			var first = polygon.vertices()[0];
			for (var vertex : polygon.vertices()) {
				if (vertex.x() != first.x()) return (vertex.u() - first.u()) / (vertex.x() - first.x());
			}
		}
		throw new AssertionError("No nondegenerate front face");
	}

	private void assertUvBounds(ModelPart part, int minU, int maxU, int minV, int maxV) {
		var cube = part.getRandomCube(RandomSource.create());
		for (var polygon : cube.polygons) for (var vertex : polygon.vertices()) {
			assertTrue(vertex.u() >= minU / 64F && vertex.u() <= maxU / 64F);
			assertTrue(vertex.v() >= minV / 64F && vertex.v() <= maxV / 64F);
		}
	}
}
