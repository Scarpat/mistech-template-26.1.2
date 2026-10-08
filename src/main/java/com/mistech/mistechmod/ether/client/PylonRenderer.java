package com.mistech.mistechmod.ether.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.ether.PylonMesh;
import com.mistech.mistechmod.ether.TransmissionPylonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Feixes roxos entre Pilares de Transmissão ligados (cada feixe é desenhado só por um dos dois pilares). */
public class PylonRenderer implements BlockEntityRenderer<TransmissionPylonBlockEntity, PylonRenderer.State> {
    private static final Identifier BEAM = Identifier.fromNamespaceAndPath(Mistech.MODID, "block/miner_vein");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float TOP = 14.0F / 16.0F;

    public PylonRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public final List<Vec3> targets = new ArrayList<>();
        public float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TransmissionPylonBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.targets.clear();
        BlockPos here = be.getBlockPos();
        for (BlockPos other : be.links()) {
            if (other.asLong() > here.asLong()) {
                state.targets.add(new Vec3(other.getX() - here.getX(), other.getY() - here.getY(), other.getZ() - here.getZ()));
            }
        }
        state.time = be.getLevel() == null ? 0 : (be.getLevel().getGameTime() % 72000L) + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.targets.isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(BEAM);
        int alpha = 140 + (int) (50 * Mth.sin(state.time * 0.2F));
        int color = (alpha << 24) | 0xC58BFF;
        for (Vec3 t : state.targets) {
            float len = (float) t.length();
            if (len < 0.01F) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.5F, TOP, 0.5F);
            poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) t.x / len, (float) t.y / len, (float) t.z / len));
            float w = 0.06F;
            collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, buffer) -> {
                float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
                quad(pose, buffer, color, u0, v0, u1, v1, -w, 0, -w, w, 0, -w, w, len, -w, -w, len, -w);
                quad(pose, buffer, color, u0, v0, u1, v1, w, 0, w, -w, 0, w, -w, len, w, w, len, w);
                quad(pose, buffer, color, u0, v0, u1, v1, -w, 0, w, -w, 0, -w, -w, len, -w, -w, len, w);
                quad(pose, buffer, color, u0, v0, u1, v1, w, 0, -w, w, 0, w, w, len, w, w, len, -w);
            });
            poseStack.popPose();
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color, float u0, float v0, float u1, float v1,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(pose, buffer, ax, ay, az, color, u0, v1);
        vertex(pose, buffer, bx, by, bz, color, u1, v1);
        vertex(pose, buffer, cx, cy, cz, color, u1, v0);
        vertex(pose, buffer, dx, dy, dz, color, u0, v0);
        // verso (o feixe é fino: as duas faces aparecem)
        vertex(pose, buffer, dx, dy, dz, color, u0, v0);
        vertex(pose, buffer, cx, cy, cz, color, u1, v0);
        vertex(pose, buffer, bx, by, bz, color, u1, v1);
        vertex(pose, buffer, ax, ay, az, color, u0, v1);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int color, float u, float v) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(TransmissionPylonBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(PylonMesh.LINK_RANGE);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
