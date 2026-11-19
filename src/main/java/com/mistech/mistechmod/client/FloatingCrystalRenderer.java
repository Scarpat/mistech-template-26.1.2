package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Cristal flutuando na câmara da frente de uma máquina (Fornalha Rúnica: chama roxa; Condensador: gelo).
 * <ul>
 * <li>Ligada: o cristal gira, sobe e desce devagar, brilha no escuro, e dois estilhaços orbitam em volta.</li>
 * <li>Desligada: fica parado, com a textura apagada.</li>
 * </ul>
 * Medidas em pixels do bloco; a câmara fica na frente (norte no modelo) e gira junto com o FACING do bloco.
 */
public class FloatingCrystalRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, FloatingCrystalRenderer.State> {
    private static final float PX = 1.0F / 16.0F;
    /** Centro do cristal dentro da câmara (x3..13, y2..11, z0..8). */
    private static final float CX = 8.0F, CY = 6.5F, CZ = 4.0F;
    /** Graus por tick do cristal e dos estilhaços; raio da órbita. */
    private static final float SPIN = 3.0F, ORBIT = 5.0F, ORBIT_RADIUS = 3.2F;

    private final Identifier litTexture;
    private final Identifier offTexture;

    public FloatingCrystalRenderer(BlockEntityRendererProvider.Context context, Identifier litTexture, Identifier offTexture) {
        this.litTexture = litTexture;
        this.offTexture = offTexture;
    }

    public static class State extends BlockEntityRenderState {
        public boolean lit;
        public float facing, spin, orbit, bob;
        public int light = 0xF000F0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        state.lit = bs.hasProperty(BlockStateProperties.LIT) && bs.getValue(BlockStateProperties.LIT);
        // frente do modelo = norte; girar Y por θ leva o norte para (-sin θ, -cos θ)
        state.facing = bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? 180.0F - bs.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() : 0.0F;
        if (be.getLevel() == null) {
            return;
        }
        state.light = LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos());
        double now = be.getLevel().getGameTime() + partialTicks;
        if (state.lit) {
            state.spin = (float) ((now * SPIN) % 360.0);
            state.orbit = (float) ((now * ORBIT) % 360.0);
            state.bob = (float) Math.sin(now * 0.08) * 0.4F;
        } else {
            state.spin = 0.0F;
            state.orbit = 40.0F;
            state.bob = -0.5F;
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite sp = sprite(state.lit ? this.litTexture : this.offTexture);
        int light = state.lit ? 0xF000F0 : state.light;

        poseStack.pushPose();
        poseStack.scale(PX, PX, PX);
        poseStack.translate(8.0F, 0.0F, 8.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facing));
        poseStack.translate(CX - 8.0F, CY + state.bob, CZ - 8.0F);

        // cristal: corpo girado 45 graus e duas pontas
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + state.spin));
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, buffer) -> {
            box(pose, buffer, sp, light, -2.0F, -2.5F, -2.0F, 2.0F, 2.5F, 2.0F);
            box(pose, buffer, sp, light, -1.0F, 2.5F, -1.0F, 1.0F, 4.0F, 1.0F);
            box(pose, buffer, sp, light, -1.0F, -4.0F, -1.0F, 1.0F, -2.5F, 1.0F);
        });
        poseStack.popPose();

        // dois estilhaços em órbita, um mais alto e um mais baixo
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(state.orbit + i * 180.0F));
            poseStack.translate(ORBIT_RADIUS, i == 0 ? 1.5F : -2.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.spin * 2.0F));
            collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
                    (pose, buffer) -> box(pose, buffer, sp, light, -0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** Caixa com as 6 faces; a textura usa um pedaço do sprite do tamanho da face (1 pixel do bloco = 1 pixel da textura). */
    private static void box(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sp, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        float w = x1 - x0, h = y1 - y0, d = z1 - z0;
        // norte (-z)
        face(pose, buffer, sp, light, w, h, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        // sul (+z)
        face(pose, buffer, sp, light, w, h, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        // oeste (-x)
        face(pose, buffer, sp, light, d, h, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        // leste (+x)
        face(pose, buffer, sp, light, d, h, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        // cima e baixo
        face(pose, buffer, sp, light, w, d, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(pose, buffer, sp, light, w, d, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
    }

    /** Uma face: 4 cantos na ordem baixo-esquerda, baixo-direita, cima-direita, cima-esquerda (olhando de fora). */
    private static void face(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sp, int light, float fw, float fh,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        float u0 = sp.getU(5.0F / 16.0F), u1 = sp.getU((5.0F + fw) / 16.0F);
        float vTop = sp.getV(5.0F / 16.0F), vBot = sp.getV((5.0F + fh) / 16.0F);
        vertex(pose, buffer, ax, ay, az, u0, vBot, light, nx, ny, nz);
        vertex(pose, buffer, bx, by, bz, u1, vBot, light, nx, ny, nz);
        vertex(pose, buffer, cx, cy, cz, u1, vTop, light, nx, ny, nz);
        vertex(pose, buffer, dx, dy, dz, u0, vTop, light, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** Fornalha Rúnica: cristal de chama roxa; apagado, cinza. */
    public static class Furnace extends FloatingCrystalRenderer<com.mistech.mistechmod.block.RunicFurnaceBlockEntity> {
        public Furnace(BlockEntityRendererProvider.Context context) {
            super(context, Identifier.fromNamespaceAndPath("mistech", "block/magic_incense_ember"),
                    Identifier.fromNamespaceAndPath("mistech", "block/ash_block"));
        }
    }

    /** Condensador: cristal de gelo celeste; parado, gelo compacto. */
    public static class Condenser extends FloatingCrystalRenderer<com.mistech.mistechmod.block.CondenserBlockEntity> {
        public Condenser(BlockEntityRendererProvider.Context context) {
            super(context, Identifier.fromNamespaceAndPath("mistech", "block/celestial_block"),
                    Identifier.fromNamespaceAndPath("minecraft", "block/packed_ice"));
        }
    }
}
