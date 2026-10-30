package com.stalker.sixnights;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Camera-facing transparent PNG sprite renderer for the small stalker children. */
public class StalkerChildRenderer extends EntityRenderer<StalkerChildEntity> {
    private static final ResourceLocation[] TEXTURES = {
            new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/child_1.png"),
            new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/child_2.png"),
            new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/child_3.png")
    };
    public StalkerChildRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(StalkerChildEntity entity) { return TEXTURES[Math.floorMod(entity.getVariant(), TEXTURES.length)]; }
    @Override public void render(StalkerChildEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.translate(0.0D, -0.78D, 0.0D);
        float width = 0.95F, height = 1.75F;
        Matrix4f matrix = pose.last().pose();
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        vertex(out, matrix, -width/2, 0, 0, 0, 1, light);
        vertex(out, matrix, width/2, 0, 0, 1, 1, light);
        vertex(out, matrix, width/2, height, 0, 1, 0, light);
        vertex(out, matrix, -width/2, height, 0, 0, 0, light);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }
    private static void vertex(VertexConsumer out, Matrix4f m, float x, float y, float z, float u, float v, int light) {
        out.vertex(m, x, y, z).color(255,255,255,255).uv(u,v).overlayCoords(0).uv2(light).normal(0,0,1).endVertex();
    }
}
