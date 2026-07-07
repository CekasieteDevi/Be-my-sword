package com.ck7.bemysword.client.renderer;

import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.entity.GuardianSkins;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class GuardianRenderer extends HumanoidMobRenderer<GuardianEntity, HumanoidModel<GuardianEntity>> {

    private static final ResourceLocation MALE_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    private static final ResourceLocation FEMALE_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/alex.png");

    // Dos variantes de modelo de brazos; this.model (heredado, mutable) se intercambia entre
    // estas dos referencias FIJAS en cada render() según la skin de cada guardián. Ojo: no usar
    // this.getModel()/this.model como "el modelo ancho" en ningún lado, porque una vez que se
    // pisa con slimModel quedaría trabado ahí para siempre.
    private final HumanoidModel<GuardianEntity> wideModel;
    private final HumanoidModel<GuardianEntity> slimModel;

    public GuardianRenderer(EntityRendererProvider.Context context) {
        super(context,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
                0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));

        // Capa de armadura (misma para ambos modelos de brazos: la diferencia de ancho
        // no se nota tanto en la armadura como en la piel del brazo)
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    @Override
    public void render(GuardianEntity entity, float entityYaw, float partialTicks,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        GuardianSkins.Skin skin = entity.getSkin();
        this.model = (skin != null && skin.slim()) ? slimModel : wideModel;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GuardianEntity entity) {
        GuardianSkins.Skin skin = entity.getSkin();
        if (skin != null) return skin.texture();
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }
}
