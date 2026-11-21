package net.amemah.mobdefenders.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** The supplied 32x16 model and two-second Blockbench idle clip. */
public final class GhastAllyModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mobdefenders", "ghast_ally"), "main");
	private static final AnimationDefinition IDLE = idleDefinition();
	private final KeyframeAnimation idle;

	public GhastAllyModel(ModelPart root) {
		super(root);
		idle = IDLE.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8),
			PartPose.offset(0, 14, 0));
		// Blockbench coordinates transform as (-x, 24-y, z). Several pivots are deliberately off-center.
		tentacle(root, 1, 1.85F, -2.5F, 0, 4);
		tentacle(root, 2, -.65F, -2.5F, 2.5F, 4.5F);
		tentacle(root, 3, -3.15F, -2.5F, -2.5F, 6.5F);
		tentacle(root, 4, 3.15F, 0, 0, 5.5F);
		tentacle(root, 5, .65F, 0, 0, 5.5F);
		tentacle(root, 6, -1.85F, 0, 0, 5);
		tentacle(root, 7, 1.85F, 2.5F, 0, 6);
		tentacle(root, 8, -.65F, 2.5F, 2.5F, 6);
		tentacle(root, 9, -3.15F, 2.5F, -2.5F, 4.5F);
		return LayerDefinition.create(mesh, 32, 16);
	}

	private static void tentacle(PartDefinition root, int index, float pivotX, float z, float offsetX, float length) {
		root.addOrReplaceChild("tentacle" + index, CubeListBuilder.create().texOffs(0, 0)
			.addBox(offsetX - .5F, 0, -.5F, 1, length, 1), PartPose.offset(-pivotX, 17.5F, z));
	}

	private static AnimationDefinition idleDefinition() {
		// The file contains two identical sets of quarter-second keys; the final key closes the clip.
		float[][] pitch = {
			{-22.92F, -34.38F, -22.92F, -11.46F}, {-32.56F, -29.11F, -13.28F, -16.73F},
			{-33.34F, -18.15F, -12.5F, -27.69F}, {-24.54F, -11.57F, -21.30F, -34.27F},
			{-14.25F, -15.43F, -31.59F, -30.41F}, {-11.93F, -26.17F, -33.91F, -19.67F},
			{-19.72F, -33.92F, -26.12F, -11.92F}, {-30.45F, -31.56F, -15.39F, -14.28F},
			{-34.26F, -21.25F, -11.58F, -24.59F}
		};
		var animation = AnimationDefinition.Builder.withLength(2).looping();
		for (int i = 0; i < pitch.length; i++) {
			var keys = new Keyframe[9];
			for (int k = 0; k < keys.length; k++) {
				keys[k] = new Keyframe(k * .25F, KeyframeAnimations.degreeVec(-pitch[i][k % 4], 0, 0),
					AnimationChannel.Interpolations.CATMULLROM);
			}
			animation.addAnimation("tentacle" + (i + 1), new AnimationChannel(AnimationChannel.Targets.ROTATION, keys));
		}
		return animation.build();
	}

	@Override public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		idle.apply((long) (state.ageInTicks * 50), 1);
	}
}
