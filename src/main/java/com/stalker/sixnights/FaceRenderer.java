package com.stalker.sixnights;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** The Face is a world entity using the supplied full-body transparent image, not the GUI jumpscare layer. */
public class FaceRenderer extends EntityRenderer<StalkerEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(StalkerSixNights.MOD_ID, "textures/entity/face_sprite.png");
    public FaceRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(StalkerEntity entity) { return TEXTURE; }
    @Override public void render(StalkerEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.translate(0.0D, -1.45D, 0.0D);
        float width = 1.25F, height = 2.8F;
        Matrix4f matrix = pose.last().pose();
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        out.vertex(matrix, -width/2, 0, 0).color(255,255,255,255).uv(0,1).overlayCoords(0).uv2(light).normal(0,0,1).endVertex();
        out.vertex(matrix, width/2, 0, 0).color(255,255,255,255).uv(1,1).overlayCoords(0).uv2(light).normal(0,0,1).endVertex();
        out.vertex(matrix, width/2, height, 0).color(255,255,255,255).uv(1,0).overlayCoords(0).uv2(light).normal(0,0,1).endVertex();
        out.vertex(matrix, -width/2, height, 0).color(255,255,255,255).uv(0,0).overlayCoords(0).uv2(light).normal(0,0,1).endVertex();
        pose.popPose();
    }
}
