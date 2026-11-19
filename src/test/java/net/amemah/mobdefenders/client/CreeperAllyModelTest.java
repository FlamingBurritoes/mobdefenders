package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreeperAllyModelTest {
	@Test void matchesSuppliedSixBoxesAndGroupPivots() {
		var root = CreeperAllyModel.createBodyLayer().bakeRoot();
		assertEquals(7, root.getAllParts().size());
		var head = root.getChild("head");
		assertEquals(6, head.y);
		var box = head.getRandomCube(RandomSource.create());
		assertEquals(-8, box.minY);
		assertEquals(8, box.maxX - box.minX);
		var body = root.getChild("body");
		assertEquals(6, body.y);
		var torso = body.getRandomCube(RandomSource.create());
		assertEquals(12, torso.maxY - torso.minY);
		for (String pair : new String[]{"hind_leg", "front_leg"}) for (String side : new String[]{"right_", "left_"}) {
			var leg = root.getChild(side + pair);
			assertEquals(side.equals("right_") ? -2 : 2, leg.x);
			assertEquals(18, leg.y);
			assertEquals(pair.equals("hind_leg") ? 4 : -4, leg.z);
			var cube = leg.getRandomCube(RandomSource.create());
			assertEquals(4, cube.maxX - cube.minX);
			assertEquals(6, cube.maxY - cube.minY);
			assertEquals(4, cube.maxZ - cube.minZ);
			for (var face : cube.polygons) for (var vertex : face.vertices())
				assertTrue(vertex.v() >= 16 / 32F && vertex.v() <= 26 / 32F);
		}
	}
	@Test void usesVanillaWalkingAndHeadAnimation() {
		var root = CreeperAllyModel.createBodyLayer().bakeRoot();
		var vanillaRoot = CreeperModel.createBodyLayer(CubeDeformation.NONE).bakeRoot();
		var model = new CreeperModel(root);
		var vanilla = new CreeperModel(vanillaRoot);
		var state = new CreeperRenderState();
		for (int i = 0; i < 24; i++) {
			state.walkAnimationPos = i * .3F;
			state.walkAnimationSpeed = .7F;
			state.yRot = i;
			state.xRot = -i;
			model.setupAnim(state);
			vanilla.setupAnim(state);
			for (String part : new String[]{"head", "right_hind_leg", "left_hind_leg", "right_front_leg", "left_front_leg"}) {
				assertEquals(vanillaRoot.getChild(part).xRot, root.getChild(part).xRot);
				assertEquals(vanillaRoot.getChild(part).yRot, root.getChild(part).yRot);
			}
		}
	}
}
