package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.forge.CrusherForgeBlockEntity;
import com.mistech.mistechmod.machine.forge.CrusherForgeStructure;
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
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * As duas mós da Forja Trituradora montada (estilo britador): ficam na metade de cima do cubo, eixos de frente para trás,
 * e giram em sentidos opostos (as bordas do meio descem, puxando o minério para a fenda da forja) enquanto trabalha.
 * O resto (forja e moldura) é o modelo de bloco {@code crusher_forge_formed}. Medidas em pixels do modelo voltado para o norte
 * (o cubo vai de x -16..16, y 0..32, z 0..32 a partir do núcleo); as mesmas de {@code tools/qol/preview_forge.py}.
 */
public class CrusherForgeRenderer implements BlockEntityRenderer<CrusherForgeBlockEntity, CrusherForgeRenderer.State> {
    private static final Identifier FACE = Identifier.fromNamespaceAndPath(Mistech.MODID, "block/crusher_wheel_face");
    private static final Identifier RIM = Identifier.fromNamespaceAndPath(Mistech.MODID, "block/crusher_wheel_rim");
    private static final Identifier TOOTH = Identifier.fromNamespaceAndPath(Mistech.MODID, "block/crusher_wheel_tooth");
    private static final Identifier HUB = Identifier.withDefaultNamespace("block/iron_block");
    private static final Identifier AXLE = Identifier.withDefaultNamespace("block/gold_block");
    private static final float PX = 1.0F / 16.0F;

    private static final int SIDES = 16;
    private static final float R_BODY = 6.5F, HALF = 11.0F;
    private static final int TEETH = 12;
    private static final float T_IN = 5.8F, T_OUT = 7.5F, T_W = 1.0F, T_HALF = 10.5F;
    private static final float WHEEL_X = 7.5F, WHEEL_Y = 24.0F, WHEEL_Z = 16.0F;
    /** Fundo da forja (onde os eixos encaixam nos mancais). */
    private static final float BACK = 29.0F;
    /** Graus por tick trabalhando. */
    private static final float SPEED = 7.0F;

    public CrusherForgeRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public boolean formed;
        public float angle;
        public float yRot;
        public int light = 0xF000F0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrusherForgeBlockEntity forge, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(forge, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = forge.getBlockState();
        state.formed = bs.hasProperty(CrusherForgeStructure.FORMED) && bs.getValue(CrusherForgeStructure.FORMED);
        boolean working = bs.hasProperty(MachineBlock.LIT) && bs.getValue(MachineBlock.LIT);
        Direction facing = bs.hasProperty(MachineBlock.FACING) ? bs.getValue(MachineBlock.FACING) : Direction.NORTH;
        state.yRot = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        if (forge.getLevel() != null) {
            // ângulo acumulado no cliente: ao desligar, a mó para onde está (sem voltar ao zero)
            double now = forge.getLevel().getGameTime() + partialTicks;
            if (working && forge.clientSpinClock > 0 && now > forge.clientSpinClock) {
                forge.clientSpin = (float) ((forge.clientSpin + (now - forge.clientSpinClock) * SPEED) % 360.0);
            }
            forge.clientSpinClock = now;
            state.angle = forge.clientSpin;
            state.light = LevelRenderer.getLightCoords(forge.getLevel(), forge.getBlockPos().above());
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(PX, PX, PX);
        // esquerda de quem olha (+x no norte) gira num sentido, a direita no outro; a da direita vai meio dente adiantada
        wheel(poseStack, collector, state.light, WHEEL_X, state.angle + 180.0F / TEETH);
        wheel(poseStack, collector, state.light, -WHEEL_X, -state.angle);
        poseStack.popPose();
    }

    /** Uma mó em (cx, WHEEL_Y, WHEEL_Z), girada {@code angle} graus em volta do eixo z. */
    private static void wheel(PoseStack poseStack, SubmitNodeCollector collector, int light, float cx, float angle) {
        TextureAtlasSprite face = sprite(FACE), rim = sprite(RIM), tooth = sprite(TOOTH), hub = sprite(HUB), axle = sprite(AXLE);
        // eixo de trás até o mancal (não gira visivelmente: é redondo)
        submitBox(poseStack, collector, axle, light, cx - 1, WHEEL_Y - 1, WHEEL_Z + HALF, cx + 1, WHEEL_Y + 1, BACK);
        poseStack.pushPose();
        poseStack.translate(cx, WHEEL_Y, WHEEL_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, buffer) -> {
            float step = Mth.TWO_PI / SIDES;
            for (int k = 0; k < SIDES; k++) {
                float a0 = k * step + step / 2, a1 = a0 + step;
                float x0 = R_BODY * Mth.cos(a0), y0 = R_BODY * Mth.sin(a0), x1 = R_BODY * Mth.cos(a1), y1 = R_BODY * Mth.sin(a1);
                // faces da frente (-z) e de trás (+z): leque de triângulos, textura pela posição (gira junto)
                quad(pose, buffer, light, 0, 0, -1,
                        0, 0, -HALF, fu(face, 0), fv(face, 0),
                        x1, y1, -HALF, fu(face, x1), fv(face, y1),
                        x0, y0, -HALF, fu(face, x0), fv(face, y0),
                        x0, y0, -HALF, fu(face, x0), fv(face, y0));
                quad(pose, buffer, light, 0, 0, 1,
                        0, 0, HALF, fu(face, 0), fv(face, 0),
                        x0, y0, HALF, fu(face, -x0), fv(face, y0),
                        x1, y1, HALF, fu(face, -x1), fv(face, y1),
                        x1, y1, HALF, fu(face, -x1), fv(face, y1));
                // borda
                float nx = Mth.cos(a0 + step / 2), ny = Mth.sin(a0 + step / 2);
                float u0 = rim.getU(0.3F), u1 = rim.getU(0.45F);
                quad(pose, buffer, light, nx, ny, 0,
                        x0, y0, -HALF, u0, rim.getV0(),
                        x1, y1, -HALF, u1, rim.getV0(),
                        x1, y1, HALF, u1, rim.getV1(),
                        x0, y0, HALF, u0, rim.getV1());
            }
            // dentes
            for (int t = 0; t < TEETH; t++) {
                float b = t * Mth.TWO_PI / TEETH;
                float c = Mth.cos(b), s = Mth.sin(b);
                toothBox(pose, buffer, tooth, light, c, s);
            }
            // cubo e ponta do eixo na frente
            flatBox(pose, buffer, hub, light, -1.6F, -1.6F, -HALF - 0.6F, 1.6F, 1.6F, -HALF);
            flatBox(pose, buffer, axle, light, -0.8F, -0.8F, -HALF - 1.4F, 0.8F, 0.8F, -HALF - 0.6F);
        });
        poseStack.popPose();
    }

    private static float fu(TextureAtlasSprite s, float x) {
        return s.getU((x + 8.0F) / 16.0F);
    }

    private static float fv(TextureAtlasSprite s, float y) {
        return s.getV((8.0F - y) / 16.0F);
    }

    /** Dente: caixa de x -T_W..T_W, raio T_IN..T_OUT, girada (c, s) em volta do centro. */
    private static void toothBox(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sp, int light, float c, float s) {
        // pontos no plano: (x, r) -> (x*c - r*s, x*s + r*c)
        float[][] p = {{-T_W, T_IN}, {T_W, T_IN}, {T_W, T_OUT}, {-T_W, T_OUT}};
        float[] X = new float[4], Y = new float[4];
        for (int i = 0; i < 4; i++) {
            X[i] = p[i][0] * c - p[i][1] * s;
            Y[i] = p[i][0] * s + p[i][1] * c;
        }
        float zf = -T_HALF, zb = T_HALF;
        float uA = sp.getU0(), uB = sp.getU(0.25F), vA = sp.getV0(), vB = sp.getV1(), vS = sp.getV(0.2F);
        float uC = uB * 0.8F + uA * 0.2F;
        // topo (para fora)
        quad(pose, buffer, light, -s, c, 0, X[3], Y[3], zf, uA, vA, X[3], Y[3], zb, uA, vB, X[2], Y[2], zb, uB, vB, X[2], Y[2], zf, uB, vA);
        // laterais
        quad(pose, buffer, light, c, s, 0, X[1], Y[1], zf, uA, vA, X[2], Y[2], zf, uC, vA, X[2], Y[2], zb, uC, vB, X[1], Y[1], zb, uA, vB);
        quad(pose, buffer, light, -c, -s, 0, X[0], Y[0], zf, uA, vA, X[0], Y[0], zb, uA, vB, X[3], Y[3], zb, uC, vB, X[3], Y[3], zf, uC, vA);
        // frente e trás
        quad(pose, buffer, light, 0, 0, -1, X[0], Y[0], zf, uA, vS, X[3], Y[3], zf, uA, vA, X[2], Y[2], zf, uB, vA, X[1], Y[1], zf, uB, vS);
        quad(pose, buffer, light, 0, 0, 1, X[1], Y[1], zb, uB, vS, X[2], Y[2], zb, uB, vA, X[3], Y[3], zb, uA, vA, X[0], Y[0], zb, uA, vS);
    }

    /** Caixa alinhada com UV proporcional ao tamanho (em pixels). */
    private static void flatBox(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sp, int light,
                                float x0, float y0, float z0, float x1, float y1, float z1) {
        float uw = sp.getU(Math.min(1.0F, (x1 - x0) / 16.0F)), vh = sp.getV(Math.min(1.0F, (y1 - y0) / 16.0F));
        float ud = sp.getU(Math.min(1.0F, (z1 - z0) / 16.0F)), vd = sp.getV(Math.min(1.0F, (z1 - z0) / 16.0F));
        float u0 = sp.getU0(), v0 = sp.getV0();
        quad(pose, buffer, light, 0, 0, -1, x1, y0, z0, u0, vh, x0, y0, z0, uw, vh, x0, y1, z0, uw, v0, x1, y1, z0, u0, v0);
        quad(pose, buffer, light, 0, 0, 1, x0, y0, z1, u0, vh, x1, y0, z1, uw, vh, x1, y1, z1, uw, v0, x0, y1, z1, u0, v0);
        quad(pose, buffer, light, -1, 0, 0, x0, y0, z0, u0, vh, x0, y0, z1, ud, vh, x0, y1, z1, ud, v0, x0, y1, z0, u0, v0);
        quad(pose, buffer, light, 1, 0, 0, x1, y0, z1, u0, vh, x1, y0, z0, ud, vh, x1, y1, z0, ud, v0, x1, y1, z1, u0, v0);
        quad(pose, buffer, light, 0, 1, 0, x0, y1, z0, u0, v0, x0, y1, z1, u0, vd, x1, y1, z1, uw, vd, x1, y1, z0, uw, v0);
        quad(pose, buffer, light, 0, -1, 0, x0, y0, z0, u0, v0, x1, y0, z0, uw, v0, x1, y0, z1, uw, vd, x0, y0, z1, u0, vd);
    }

    private static void submitBox(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sp, int light,
                                  float x0, float y0, float z0, float x1, float y1, float z1) {
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
                (pose, buffer) -> flatBox(pose, buffer, sp, light, x0, y0, z0, x1, y1, z1));
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv) {
        vertex(pose, buffer, ax, ay, az, au, av, light, nx, ny, nz);
        vertex(pose, buffer, bx, by, bz, bu, bv, light, nx, ny, nz);
        vertex(pose, buffer, cx, cy, cz, cu, cv, light, nx, ny, nz);
        vertex(pose, buffer, dx, dy, dz, du, dv, light, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** O cubo inteiro (2×2×2 a partir do núcleo, para qualquer lado). */
    @Override
    public AABB getRenderBoundingBox(CrusherForgeBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0).expandTowards(0.0, 1.0, 0.0);
    }
}
