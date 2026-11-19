package com.mistech.mistechmod.folklore.machine.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.folklore.machine.SaciWindmillBlockEntity;
import com.mistech.mistechmod.machine.MachineBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Pás do Moinho do Saci: 4 braços de madeira com velas de pano, 3×3 blocos, na frente do bloco. Giram enquanto ele gera
 * (como um redemoinho: a velocidade sobe com o FE). Medidas em pixels com o moinho virado para o norte (frente = −z).
 */
public class SaciWindmillRenderer implements BlockEntityRenderer<SaciWindmillBlockEntity, SaciWindmillRenderer.State> {
    private static final Identifier WOOD = Identifier.withDefaultNamespace("block/stripped_spruce_log");
    private static final Identifier SAIL = Identifier.withDefaultNamespace("block/white_wool");
    private static final Identifier RED = Identifier.withDefaultNamespace("block/red_wool");
    private static final Identifier HUB = Identifier.withDefaultNamespace("block/copper_block");
    private static final float PX = 1.0F / 16.0F;
    private static final float CX = 8.0F, CY = 9.0F, Z = -2.5F;
    private static final float SPEED = 6.0F;

    public SaciWindmillRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public float angle;
        public float yRot;
        public int light = 0xF000F0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SaciWindmillBlockEntity mill, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mill, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = mill.getBlockState();
        boolean working = bs.hasProperty(MachineBlock.LIT) && bs.getValue(MachineBlock.LIT);
        Direction facing = bs.hasProperty(MachineBlock.FACING) ? bs.getValue(MachineBlock.FACING) : Direction.NORTH;
        state.yRot = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        if (mill.getLevel() != null) {
            double now = mill.getLevel().getGameTime() + partialTicks;
            if (working && mill.clientSpinClock > 0 && now > mill.clientSpinClock) {
                mill.clientSpin = (float) ((mill.clientSpin + (now - mill.clientSpinClock) * SPEED) % 360.0);
            }
            mill.clientSpinClock = now;
            state.angle = mill.clientSpin;
            state.light = LevelRenderer.getLightCoords(mill.getLevel(), mill.getBlockPos().relative(facing));
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite wood = sprite(WOOD), sail = sprite(SAIL), red = sprite(RED), hub = sprite(HUB);
        int light = state.light;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(PX, PX, PX);
        poseStack.translate(CX, CY, Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.angle));
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, buffer) -> {
            // cubo e eixo
            box(pose, buffer, hub, light, -2, -2, -1.5F, 2, 2, 1.5F);
            for (int arm = 0; arm < 4; arm++) {
                float a = (float) Math.toRadians(arm * 90.0);
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                // braço: de 2 a 23 px do centro
                rotatedBox(pose, buffer, wood, light, c, s, -0.75F, 2.0F, 0.75F, 23.0F, -0.6F, 0.6F);
                // vela: de 6 a 22 px, só de um lado do braço (o lado do vento)
                rotatedBox(pose, buffer, arm % 2 == 0 ? sail : red, light, c, s, 0.75F, 6.0F, 6.5F, 22.0F, -0.2F, 0.2F);
            }
        });
        poseStack.popPose();
    }

    /** Caixa definida no "sistema do braço" (x = largura, r = raio) e girada (c, s) em volta do eixo z. */
    private static void rotatedBox(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sp, int light, float c, float s,
                                   float x0, float r0, float x1, float r1, float z0, float z1) {
        float[][] p = {{x0, r0}, {x1, r0}, {x1, r1}, {x0, r1}};
        float[] X = new float[4], Y = new float[4];
        for (int i = 0; i < 4; i++) {
            X[i] = p[i][0] * c - p[i][1] * s;
            Y[i] = p[i][0] * s + p[i][1] * c;
        }
        float u0 = sp.getU0(), u1 = sp.getU(Math.min(1.0F, (x1 - x0) / 16.0F)), v0 = sp.getV0(), v1 = sp.getV(Math.min(1.0F, (r1 - r0) / 16.0F));
        float ut = sp.getU(Math.min(1.0F, (z1 - z0) / 16.0F + 0.05F));
        // frente e trás
        quad(pose, buffer, light, 0, 0, -1, X[0], Y[0], z0, u0, v1, X[3], Y[3], z0, u0, v0, X[2], Y[2], z0, u1, v0, X[1], Y[1], z0, u1, v1);
        quad(pose, buffer, light, 0, 0, 1, X[1], Y[1], z1, u1, v1, X[2], Y[2], z1, u1, v0, X[3], Y[3], z1, u0, v0, X[0], Y[0], z1, u0, v1);
        // bordas
        quad(pose, buffer, light, c, s, 0, X[1], Y[1], z0, u0, v1, X[2], Y[2], z0, u0, v0, X[2], Y[2], z1, ut, v0, X[1], Y[1], z1, ut, v1);
        quad(pose, buffer, light, -c, -s, 0, X[0], Y[0], z1, u0, v1, X[3], Y[3], z1, u0, v0, X[3], Y[3], z0, ut, v0, X[0], Y[0], z0, ut, v1);
        quad(pose, buffer, light, -s, c, 0, X[3], Y[3], z0, u0, v0, X[3], Y[3], z1, ut, v0, X[2], Y[2], z1, ut, v1, X[2], Y[2], z0, u0, v1);
        quad(pose, buffer, light, s, -c, 0, X[0], Y[0], z0, u0, v0, X[1], Y[1], z0, u0, v1, X[1], Y[1], z1, ut, v1, X[0], Y[0], z1, ut, v0);
    }

    private static void box(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sp, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        rotatedBox(pose, buffer, sp, light, 1.0F, 0.0F, x0, y0, x1, y1, z0, z1);
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

    /** As pás saem 1 bloco para cada lado e para a frente. */
    @Override
    public AABB getRenderBoundingBox(SaciWindmillBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.6);
    }
}
