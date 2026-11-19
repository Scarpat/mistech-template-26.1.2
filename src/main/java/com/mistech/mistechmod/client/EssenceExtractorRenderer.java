package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.EssenceExtractorBlock;
import com.mistech.mistechmod.block.EssenceExtractorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Extrator de Essência (cadinho de obsidiana):
 * <ul>
 * <li>superfície da essência numa altura contínua: sobe aos poucos dentro de cada nível, conforme o progresso;</li>
 * <li>Olho do Fim em cubo boiando na boca. Ligado, vira devagar para o jogador (câmera) a até {@link #RANGE} blocos,
 * com inclinação para cima/baixo; sem ninguém perto, gira procurando. Desligado, para e olha para baixo.</li>
 * </ul>
 * Medidas em pixels do bloco.
 */
public class EssenceExtractorRenderer implements BlockEntityRenderer<EssenceExtractorBlockEntity, EssenceExtractorRenderer.State> {
    private static final Identifier ESSENCE = Identifier.fromNamespaceAndPath("mistech", "block/impure_essence_still");
    private static final Identifier EYE_FRONT = Identifier.fromNamespaceAndPath("mistech", "block/essence_extractor_eye_front");
    private static final Identifier EYE_BACK = Identifier.fromNamespaceAndPath("mistech", "block/essence_extractor_eye_back");
    private static final Identifier EYE_TOP = Identifier.fromNamespaceAndPath("mistech", "block/essence_extractor_eye_top");
    private static final float PX = 1.0F / 16.0F;
    /** Fundo da boca e quanto sobe por nível (igual às partículas do bloco). */
    private static final float FLOOR = 12.0F, PER_LEVEL = 1.2F;
    private static final double RANGE = 8.0;
    /** Graus por tick: virar para o jogador (fração do que falta) e girar procurando. */
    private static final float TURN = 0.18F, IDLE_SPIN = 1.5F;

    public EssenceExtractorRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public float surface;
        public float yaw, pitch, bob;
        public boolean lit;
        public int light = 0xF000F0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EssenceExtractorBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        int level = bs.hasProperty(EssenceExtractorBlock.LEVEL) ? bs.getValue(EssenceExtractorBlock.LEVEL) : 0;
        state.lit = bs.hasProperty(EssenceExtractorBlock.LIT) && bs.getValue(EssenceExtractorBlock.LIT);
        state.surface = level == 0 && be.clientFillFraction(partialTicks) <= 0.0F ? -1.0F
                : FLOOR + (level + be.clientFillFraction(partialTicks)) * PER_LEVEL;
        if (be.getLevel() == null) {
            return;
        }
        state.light = LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos().above());
        double now = be.getLevel().getGameTime() + partialTicks;
        float dt = be.eyeClock > 0 ? (float) Math.min(5.0, Math.max(0.0, now - be.eyeClock)) : 0.0F;
        be.eyeClock = now;
        state.bob = state.lit ? (float) Math.sin(now * 0.08) * 0.35F : 0.0F;

        float targetYaw, targetPitch;
        if (state.lit) {
            Vec3 eye = Vec3.atLowerCornerOf(be.getBlockPos()).add(0.5, 16.5 * PX, 0.5);
            Vec3 d = cameraPosition.subtract(eye);
            if (d.lengthSqr() < RANGE * RANGE) {
                // frente do cubo = -z; depois de girar Y por θ ela aponta para (-sin θ, -cos θ)
                targetYaw = (float) Math.toDegrees(Math.atan2(-d.x, -d.z));
                targetPitch = (float) Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
                targetPitch = Math.max(-60.0F, Math.min(70.0F, targetPitch));
            } else {
                targetYaw = be.eyeYaw + IDLE_SPIN * dt;
                targetPitch = 0.0F;
            }
        } else {
            targetYaw = be.eyeYaw;
            targetPitch = -35.0F; // "dormindo": olha para baixo
        }
        float k = 1.0F - (float) Math.pow(1.0F - TURN, dt);
        be.eyeYaw = be.eyeYaw + wrap(targetYaw - be.eyeYaw) * k;
        be.eyePitch = be.eyePitch + (targetPitch - be.eyePitch) * k;
        state.yaw = be.eyeYaw;
        state.pitch = be.eyePitch;
    }

    private static float wrap(float deg) {
        deg %= 360.0F;
        if (deg >= 180.0F) deg -= 360.0F;
        if (deg < -180.0F) deg += 360.0F;
        return deg;
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite essence = sprite(ESSENCE), front = sprite(EYE_FRONT), back = sprite(EYE_BACK), top = sprite(EYE_TOP);
        int light = state.light;
        int eyeLight = state.lit ? 0xF000F0 : light;
        float surface = state.surface;
        if (surface > 0) {
            poseStack.pushPose();
            poseStack.scale(PX, PX, PX);
            int essenceLight = Math.max(light & 0xFFFF, 0x70) | (light & 0xFFFF0000);
            collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, buffer) -> {
                // a boca tem 3.5..12.5 px, mas o gargalo (13.5..14.5 px de altura) é mais estreito: 4..12
                float lo = surface >= 13.5F && surface < 14.5F ? 4.0F : 3.5F, hi = 16.0F - lo;
                float u0 = essence.getU(lo / 16.0F), u1 = essence.getU(hi / 16.0F);
                float v0 = essence.getV(lo / 16.0F), v1 = essence.getV(hi / 16.0F);
                vertex(pose, buffer, lo, surface, lo, u0, v0, essenceLight, 0, 1, 0);
                vertex(pose, buffer, lo, surface, hi, u0, v1, essenceLight, 0, 1, 0);
                vertex(pose, buffer, hi, surface, hi, u1, v1, essenceLight, 0, 1, 0);
                vertex(pose, buffer, hi, surface, lo, u1, v0, essenceLight, 0, 1, 0);
            });
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.scale(PX, PX, PX);
        // olho: cubo 6x6x6 centrado em (8, 16.5, 8), meio afundado na boca
        poseStack.translate(8.0F, 16.5F + state.bob, 8.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, buffer) -> {
            float a = 3.0F;
            // norte (-z): frente com a pupila
            face(pose, buffer, front, eyeLight, 0, 0, -1, a, -a, -a, -a, -a, -a, -a, a, -a, a, a, -a);
            // sul (+z)
            face(pose, buffer, back, eyeLight, 0, 0, 1, -a, -a, a, a, -a, a, a, a, a, -a, a, a);
            // oeste (-x)
            face(pose, buffer, back, eyeLight, -1, 0, 0, -a, -a, -a, -a, -a, a, -a, a, a, -a, a, -a);
            // leste (+x)
            face(pose, buffer, back, eyeLight, 1, 0, 0, a, -a, a, a, -a, -a, a, a, -a, a, a, a);
            // cima e baixo
            face(pose, buffer, top, eyeLight, 0, 1, 0, -a, a, -a, -a, a, a, a, a, a, a, a, -a);
            face(pose, buffer, top, eyeLight, 0, -1, 0, -a, -a, a, -a, -a, -a, a, -a, -a, a, -a, a);
        });
        poseStack.popPose();
    }

    /**
     * Uma face do olho: 4 cantos na ordem baixo-esquerda, baixo-direita, cima-direita, cima-esquerda (olhando a face de fora).
     * A textura usa a região 5..11 px do sprite (desenho 6x6 do olho).
     */
    private static void face(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sp, int light, float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        float u0 = sp.getU(5.0F / 16.0F), u1 = sp.getU(11.0F / 16.0F), vTop = sp.getV(5.0F / 16.0F), vBot = sp.getV(11.0F / 16.0F);
        vertex(pose, buffer, x0, y0, z0, u0, vBot, light, nx, ny, nz);
        vertex(pose, buffer, x1, y1, z1, u1, vBot, light, nx, ny, nz);
        vertex(pose, buffer, x2, y2, z2, u1, vTop, light, nx, ny, nz);
        vertex(pose, buffer, x3, y3, z3, u0, vTop, light, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** O olho sai um pouco acima do bloco. */
    @Override
    public AABB getRenderBoundingBox(EssenceExtractorBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 0.4, 0);
    }
}
