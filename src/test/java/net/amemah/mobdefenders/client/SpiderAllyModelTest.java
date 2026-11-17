package net.amemah.mobdefenders.client;

import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpiderAllyModelTest {
	@Test void preservesAllElevenBoxesAndLegPivots() {
		var root = SpiderAllyModel.createBodyLayer().bakeRoot();
		var vanilla = SpiderModel.createSpiderBodyLayer().bakeRoot();
		assertEquals(12, root.getAllParts().size());
		var head = root.getChild("head");
		assertEquals(15, head.y);
		assertEquals(-3, head.z);
		var cube = head.getRandomCube(RandomSource.create());
		assertEquals(-8, cube.minZ);
		assertEquals(8, cube.maxX - cube.minX);
		String[] pairs = {"hind_leg", "middle_hind_leg", "middle_front_leg", "front_leg"};
		for (int i = 0; i < pairs.length; i++) for (String side : new String[]{"right_", "left_"}) {
			var leg = root.getChild(side + pairs[i]);
			assertEquals(side.equals("right_") ? -4 : 4, leg.x);
			assertEquals(4 - 3 * i, leg.z);
			assertEquals(vanilla.getChild(side + pairs[i]).yRot, leg.yRot, .00001F);
			assertEquals(vanilla.getChild(side + pairs[i]).zRot, leg.zRot, .00001F);
			var box = leg.getRandomCube(RandomSource.create());
			assertEquals(16, box.maxX - box.minX);
			assertEquals(2, box.maxY - box.minY);
			assertEquals(2, box.maxZ - box.minZ);
			for (var face : box.polygons) for (var vertex : face.vertices()) {
				assertTrue(vertex.u() >= 18 / 64F && vertex.u() <= 54 / 64F);
				assertTrue(vertex.v() >= 0 && vertex.v() <= 4 / 32F);
			}
		}
	}
	@Test void vanillaAnimationMovesLegsAndHeadWithoutAccumulating() {
		var root = SpiderAllyModel.createBodyLayer().bakeRoot();
		var model = new SpiderModel(root);
		var state = new LivingEntityRenderState();
		state.yRot = 30;
		state.walkAnimationPos = 2;
		state.walkAnimationSpeed = 1;
		model.setupAnim(state);
		float rotation = root.getChild("right_hind_leg").yRot;
		assertNotEquals(0, rotation);
		assertEquals((float) Math.PI / 6, root.getChild("head").yRot, .0001F);
		model.setupAnim(state);
		assertEquals(rotation, root.getChild("right_hind_leg").yRot);
	}
	@Test void matchesVanillaLegAnimationAtRestAndThroughoutWalking() {
		var root = SpiderAllyModel.createBodyLayer().bakeRoot();
		var vanillaRoot = SpiderModel.createSpiderBodyLayer().bakeRoot();
		var model = new SpiderModel(root);
		var vanilla = new SpiderModel(vanillaRoot);
		var state = new LivingEntityRenderState();
		for (float speed : new float[]{0, .3F, 1}) for (int frame = 0; frame < 24; frame++) {
			state.walkAnimationPos = frame * .3F;
			state.walkAnimationSpeed = speed;
			state.yRot = frame;
			model.setupAnim(state);
			vanilla.setupAnim(state);
			for (String name : new String[]{"head", "right_hind_leg", "left_hind_leg",
					"right_middle_hind_leg", "left_middle_hind_leg", "right_middle_front_leg",
					"left_middle_front_leg", "right_front_leg", "left_front_leg"}) {
				var actual = root.getChild(name);
				var expected = vanillaRoot.getChild(name);
				assertEquals(expected.xRot, actual.xRot, .00001F, name);
				assertEquals(expected.yRot, actual.yRot, .00001F, name);
				assertEquals(expected.zRot, actual.zRot, .00001F, name);
			}
		}
	}
}
