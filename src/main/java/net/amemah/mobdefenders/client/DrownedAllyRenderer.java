package net.amemah.mobdefenders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.amemah.mobdefenders.entity.DrownedAlly;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public final class DrownedAllyRenderer extends AbstractZombieRenderer<DrownedAlly, ZombieRenderState, DrownedModel> {
	private static final Identifier INNER = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/drowned_ally_inner.png");
	private static final Identifier OUTER = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/drowned_ally_outer.png");

	public DrownedAllyRenderer(EntityRendererProvider.Context context) {
		super(context, new DrownedModel(context.bakeLayer(DrownedAllyModel.INNER)), new DrownedModel(context.bakeLayer(DrownedAllyModel.INNER)),
			ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, context.getModelSet(), DrownedModel::new),
			ArmorModelSet.bake(ModelLayers.DROWNED_BABY_ARMOR, context.getModelSet(), DrownedModel::new));
		var shell = new DrownedModel(context.bakeLayer(DrownedAllyModel.OUTER));
		addLayer(new RenderLayer<>(this) {
			@Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, ZombieRenderState state, float yRot, float xRot) {
				coloredCutoutModelCopyLayerRender(shell, OUTER, pose, collector, light, state, -1, 1);
			}
		});
	}
	@Override public ZombieRenderState createRenderState() { return new ZombieRenderState(); }
	@Override public Identifier getTextureLocation(ZombieRenderState state) { return INNER; }
	@Override protected ArmPose getArmPose(DrownedAlly ally, HumanoidArm arm) {
		if (arm == ally.getMainArm() && ally.isAggressive() && ally.getItemHeldByArm(arm).is(Items.TRIDENT)) return ArmPose.THROW_TRIDENT;
		return super.getArmPose(ally, arm);
	}
	@Override protected void setupRotations(ZombieRenderState state, PoseStack pose, float bodyRot, float scale) {
		super.setupRotations(state, pose, bodyRot, scale);
		if (state.swimAmount > 0) pose.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(state.swimAmount, 0, -10 - state.xRot)),
			0, state.boundingBoxHeight / 2 / scale, 0);
	}
}
