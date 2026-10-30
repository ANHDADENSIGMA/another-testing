package com.stalker.sixnights;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StalkerRenderer extends MobRenderer<StalkerEntity, StalkerModel> {
    private static final ResourceLocation WATCHER = new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/stalker.png");
    private static final ResourceLocation RUNNER = new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/runner.png");
    private static final ResourceLocation MIMIC = new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/mimic.png");
    private static final ResourceLocation MINION = new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/minion.png");
    private static final ResourceLocation FACE = new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/face.png");

    public StalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new StalkerModel(context.bakeLayer(StalkerModel.LAYER)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(StalkerEntity entity) {
        return switch (entity.getRole()) {
            case StalkerEntity.ROLE_RUNNER -> RUNNER;
            case StalkerEntity.ROLE_MIMIC -> MIMIC;
            case StalkerEntity.ROLE_MINION -> MINION;
            case StalkerEntity.ROLE_FACE -> FACE;
            default -> WATCHER;
        };
    }

    @Override
    protected void scale(StalkerEntity entity, PoseStack poseStack, float partialTick) {
        float scale = switch (entity.getRole()) {
            case StalkerEntity.ROLE_RUNNER -> 1.08F;
            case StalkerEntity.ROLE_MIMIC -> 0.94F;
            case StalkerEntity.ROLE_MINION -> 0.46F;
            case StalkerEntity.ROLE_FACE -> 1.12F;
            default -> 1.0F;
        };
        poseStack.scale(scale, scale, scale);
    }
}
