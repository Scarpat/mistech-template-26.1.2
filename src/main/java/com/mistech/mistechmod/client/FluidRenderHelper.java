package com.mistech.mistechmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Desenha fluido: caixa dentro de tanques (mundo) e medidor (telas). */
public final class FluidRenderHelper {
    private FluidRenderHelper() {}

    public static FluidModel model(Fluid fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
    }

    public static TextureAtlasSprite stillSprite(Fluid fluid) {
        return model(fluid).stillMaterial().sprite();
    }

    /** Cor ARGB do fluido (água fica azul; lava/essência sem tinta). */
    public static int tint(Fluid fluid) {
        FluidModel model = model(fluid);
        if (model.fluidTintSource() == null) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | model.fluidTintSource().colorAsStack(new FluidStack(fluid, 1000));
    }

    /**
     * Caixa de fluido em coordenadas de bloco (0..1), da altura y0 até y0 + (y1 - y0) * fill.
     * Só desenha as faces de cima e dos lados (o fundo fica escondido).
     */
    public static void submitBox(PoseStack poseStack, SubmitNodeCollector collector, Fluid fluid, float fill, int light,
                                 float x0, float y0, float z0, float x1, float y1, float z1) {
        if (fluid == Fluids.EMPTY || fill <= 0.0F) {
            return;
        }
        TextureAtlasSprite sprite = stillSprite(fluid);
        int color = tint(fluid);
        float top = y0 + (y1 - y0) * Math.min(1.0F, fill);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, buffer) -> {
            // cima
            quad(pose, buffer, sprite, color, light,
                    x0, top, z0, x0, top, z1, x1, top, z1, x1, top, z0, 0, 1, 0, x0, z0, x1, z1);
            // norte (z0) e sul (z1)
            quad(pose, buffer, sprite, color, light,
                    x1, y0, z0, x0, y0, z0, x0, top, z0, x1, top, z0, 0, 0, -1, x0, y0, x1, top);
            quad(pose, buffer, sprite, color, light,
                    x0, y0, z1, x1, y0, z1, x1, top, z1, x0, top, z1, 0, 0, 1, x0, y0, x1, top);
            // oeste (x0) e leste (x1)
            quad(pose, buffer, sprite, color, light,
                    x0, y0, z0, x0, y0, z1, x0, top, z1, x0, top, z0, -1, 0, 0, z0, y0, z1, top);
            quad(pose, buffer, sprite, color, light,
                    x1, y0, z1, x1, y0, z0, x1, top, z0, x1, top, z1, 1, 0, 0, z0, y0, z1, top);
        });
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, float u0, float v0, float u1, float v1) {
        float su0 = sprite.getU(u0), su1 = sprite.getU(u1);
        float sv0 = sprite.getV(1.0F - v1), sv1 = sprite.getV(1.0F - v0);
        vertex(pose, buffer, ax, ay, az, color, su0, sv1, light, nx, ny, nz);
        vertex(pose, buffer, bx, by, bz, color, su1, sv1, light, nx, ny, nz);
        vertex(pose, buffer, cx, cy, cz, color, su1, sv0, light, nx, ny, nz);
        vertex(pose, buffer, dx, dy, dz, color, su0, sv0, light, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int color,
                               float u, float v, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** Medidor de fluido na tela, enchendo de baixo para cima com a textura do fluido. */
    public static void drawGauge(GuiGraphicsExtractor graphics, Fluid fluid, int amount, int capacity, int x, int y, int w, int h) {
        if (fluid == Fluids.EMPTY || amount <= 0 || capacity <= 0) {
            return;
        }
        int filled = Math.max(1, Math.round(h * Math.min(1.0F, amount / (float) capacity)));
        TextureAtlasSprite sprite = stillSprite(fluid);
        int color = tint(fluid);
        graphics.enableScissor(x, y + h - filled, x + w, y + h);
        for (int ty = y + h - 16; ty > y + h - filled - 16; ty -= 16) {
            for (int tx = x; tx < x + w; tx += 16) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 16, 16, color);
            }
        }
        graphics.disableScissor();
    }
}
