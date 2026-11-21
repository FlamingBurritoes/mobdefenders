package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.monster.piglin.AdultPiglinModel;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.resources.Identifier;

/** Supplied Blockbench geometry and clothing mapped to vanilla piglin animation parts. */
public final class BruteAllyModel extends AdultPiglinModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mobdefenders", "brute_ally"), "main");
	private static final CubeDeformation CLOTHING = new CubeDeformation(.25F);
	private static final float EAR_ADJUSTMENT = (float) Math.toRadians(5);

	public BruteAllyModel(ModelPart root) { super(root); }

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		var head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-5, -8, -4, 10, 8, 8)
			.texOffs(31, 1).addBox(-2, -4, -5, 4, 4, 1)
			.texOffs(2, 4).addBox(2, -2, -5, 1, 2, 1)
			.texOffs(2, 0).addBox(-3, -2, -5, 1, 2, 1), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(51, 6)
			.addBox(0, 0, -2, 1, 5, 4), PartPose.offsetAndRotation(4.5F, -6, 0, 0, 0, (float) Math.toRadians(-35)));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(39, 6)
			.addBox(-1, 0, -2, 1, 5, 4), PartPose.offsetAndRotation(-4.5F, -6, 0, 0, 0, (float) Math.toRadians(35)));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4)
			.texOffs(16, 32).addBox(-4, 0, -2, 8, 12, 4, CLOTHING), PartPose.ZERO);
		root.addOrReplaceChild("left_arm", limb(32, 48, 48, 48, -1, -2), PartPose.offset(5, 2, 0));
		root.addOrReplaceChild("right_arm", limb(40, 16, 40, 32, -3, -2), PartPose.offset(-5, 2, 0));
		root.addOrReplaceChild("left_leg", limb(16, 48, 0, 48, -2, 0), PartPose.offset(2, 12, 0));
		root.addOrReplaceChild("right_leg", limb(0, 16, 0, 32, -2, 0), PartPose.offset(-2, 12, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static CubeListBuilder limb(int u, int v, int outerU, int outerV, float x, float y) {
		return CubeListBuilder.create().texOffs(u, v).addBox(x, y, -2, 4, 12, 4)
			.texOffs(outerU, outerV).addBox(x, y, -2, 4, 12, 4, CLOTHING);
	}

	@Override public void setupAnim(PiglinRenderState state) {
		super.setupAnim(state);
		// Retain native movement while opening the supplied ears five degrees farther than vanilla.
		leftEar.zRot -= EAR_ADJUSTMENT;
		rightEar.zRot += EAR_ADJUSTMENT;
	}
}
