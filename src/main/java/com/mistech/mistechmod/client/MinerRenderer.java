package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MinerBlockEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Parte animada da Mineradora (o berço, as pernas e a câmara são modelos de bloco):
 * <ul>
 * <li>Cristal de Magia Impura flutuando no núcleo, girando e subindo/descendo (mais rápido trabalhando).</li>
 * <li>4 gemas de ametista orbitando o cristal.</li>
 * <li>Feixe roxo do cristal até o vazio da Câmara Coletora (só trabalhando).</li>
 * <li>Broca central embaixo da câmara, girando enquanto minera.</li>
 * </ul>
 * Tudo em coordenadas do núcleo (a câmara fica 1 bloco abaixo).
 */
public class MinerRenderer implements BlockEntityRenderer<MinerBlockEntity, MinerRenderer.State> {
    private static final Identifier CORE = Identifier.fromNamespaceAndPath(Mistech.MODID, "block/impure_magic_block");
    private static final Identifier GEM = Identifier.withDefaultNamespace("block/amethyst_block");
    private static final Identifier DRILL = Identifier.fromNamespaceAndPath(Mistech.MODID, "block/tunneler_drill");
    private static final Identifier VEIN = Identifier.fromNamespaceAndPath(Mistech.MODID, "block/miner_vein");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float PX = 1.0F / 16.0F;

    public MinerRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public boolean working;
        public float time;
        public int drillLight = FULL_BRIGHT;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MinerBlockEntity miner, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(miner, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = miner.getBlockState();
        state.working = blockState.hasProperty(MachineBlock.LIT) && blockState.getValue(MachineBlock.LIT);
        if (miner.getLevel() != null) {
            state.time = (miner.getLevel().getGameTime() % 72000L) + partialTicks;
            state.drillLight = LevelRenderer.getLightCoords(miner.getLevel(), miner.getBlockPos().below(2));
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = state.time;
        float spin = t * (state.working ? 4.0F : 0.8F);

        // cristal: gira, flutua e fica de ponta (cubo apoiado num vértice)
        TextureAtlasSprite core = sprite(CORE);
        float bob = Mth.sin(t * 0.08F) * 0.6F * PX;
        poseStack.pushPose();
        poseStack.translate(0.5F, 8.0F * PX + bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(35.26F));
        float half = 3.2F * PX;
        submitBox(poseStack, collector, core, 0xFFFFFFFF, FULL_BRIGHT, -half, -half, -half, half, half, half);
        poseStack.popPose();

        // gemas em órbita
        TextureAtlasSprite gem = sprite(GEM);
        float orbit = -t * (state.working ? 3.0F : 0.6F);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(orbit + i * 90.0F));
            float y = 8.0F * PX + Mth.sin(t * 0.1F + i * 1.57F) * 1.2F * PX;
            poseStack.translate(6.4F * PX, y, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 2.0F));
            float g = 0.9F * PX;
            submitBox(poseStack, collector, gem, 0xFFFFFFFF, FULL_BRIGHT, -g, -g, -g, g, g, g);
            poseStack.popPose();
        }

        if (state.working) {
            // feixe: do cristal até o vazio da câmara (que fica 1 bloco abaixo)
            TextureAtlasSprite vein = sprite(VEIN);
            int alpha = 150 + (int) (60 * Mth.sin(t * 0.3F));
            int color = (alpha << 24) | 0xD9A0FF;
            float w = 0.9F * PX;
            submitBox(poseStack, collector, vein, color, FULL_BRIGHT, 0.5F - w, -5.0F * PX, 0.5F - w, 0.5F + w, 5.0F * PX, 0.5F + w);
        }

        // broca central embaixo da câmara (a câmara ocupa y -16..0 px; o eixo do modelo vai até -18)
        TextureAtlasSprite drill = sprite(DRILL);
        float drillSpin = state.working ? t * 18.0F : 0.0F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(drillSpin));
        float[][] steps = {{4.0F, -18.0F, -22.0F}, {3.0F, -22.0F, -26.0F}, {2.0F, -26.0F, -29.0F}, {1.0F, -29.0F, -32.0F}};
        for (float[] step : steps) {
            float r = step[0] * PX;
            submitBox(poseStack, collector, drill, 0xFFFFFFFF, state.drillLight, -r, step[2] * PX, -r, r, step[1] * PX, r);
        }
        poseStack.popPose();
    }

    /** Cubo com a textura inteira em cada face (faces para fora). */
    private static void submitBox(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, int color,
                                  int light, float x0, float y0, float z0, float x1, float y1, float z1) {
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, buffer) -> {
            float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
            // cima / baixo
            quad(pose, buffer, color, light, 0, 1, 0, u0, v0, u1, v1,
                    x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
            quad(pose, buffer, color, light, 0, -1, 0, u0, v0, u1, v1,
                    x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
            // norte / sul
            quad(pose, buffer, color, light, 0, 0, -1, u0, v0, u1, v1,
                    x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
            quad(pose, buffer, color, light, 0, 0, 1, u0, v0, u1, v1,
                    x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            // oeste / leste
            quad(pose, buffer, color, light, -1, 0, 0, u0, v0, u1, v1,
                    x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            quad(pose, buffer, color, light, 1, 0, 0, u0, v0, u1, v1,
                    x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        });
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color, int light, float nx, float ny, float nz,
                             float u0, float v0, float u1, float v1,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(pose, buffer, ax, ay, az, color, u0, v1, light, nx, ny, nz);
        vertex(pose, buffer, bx, by, bz, color, u1, v1, light, nx, ny, nz);
        vertex(pose, buffer, cx, cy, cz, color, u1, v0, light, nx, ny, nz);
        vertex(pose, buffer, dx, dy, dz, color, u0, v0, light, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int color,
                               float u, float v, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** Inclui a câmara e a broca de baixo (e um pouco em volta) para não sumir na borda da tela. */
    @Override
    public AABB getRenderBoundingBox(MinerBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0).expandTowards(0.0, -2.0, 0.0);
    }
}
