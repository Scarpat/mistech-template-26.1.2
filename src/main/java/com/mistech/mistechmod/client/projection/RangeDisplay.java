package com.mistech.mistechmod.client.projection;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.Mistech;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Alcance dos blocos por perto, com a Pedra Guia na mão (agachado, ou sempre — config do cliente).
 * Caixa = área em cubo; círculo = raio (esfera) visto de cima.
 */
public final class RangeDisplay {
    private RangeDisplay() {}

    /** Área de um bloco: caixa e/ou raio a partir do centro, cor e nome. */
    private record Area(@Nullable AABB box, double radius, int rgb, Component label) {}

    static void draw(Minecraft mc, LocalPlayer player) {
        boolean holding = player.getMainHandItem().is(Mistech.GUIDE_STONE.get()) || player.getOffhandItem().is(Mistech.GUIDE_STONE.get());
        if (!holding || Config.rangeNeedsSneak() && !player.isShiftKeyDown()) {
            return;
        }
        int scan = Config.rangeScanRadius();
        BlockPos center = player.blockPosition();
        Vec3 eye = player.getEyePosition();
        int minCx = SectionPos.blockToSectionCoord(center.getX() - scan), maxCx = SectionPos.blockToSectionCoord(center.getX() + scan);
        int minCz = SectionPos.blockToSectionCoord(center.getZ() - scan), maxCz = SectionPos.blockToSectionCoord(center.getZ() + scan);
        long scanSqr = (long) scan * scan;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!mc.level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = mc.level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (pos.distSqr(center) > scanSqr) {
                        continue;
                    }
                    Area area = area(be);
                    if (area == null) {
                        continue;
                    }
                    int stroke = 0xD0000000 | area.rgb();
                    if (area.box() != null) {
                        Gizmos.cuboid(area.box(), GizmoStyle.strokeAndFill(stroke, 2.0F, 0x14000000 | area.rgb()));
                    }
                    if (area.radius() > 0) {
                        Vec3 c = pos.getCenter();
                        Gizmos.circle(new Vec3(c.x, pos.getY() + 0.05, c.z), (float) area.radius(), GizmoStyle.stroke(stroke, 2.5F));
                        Gizmos.circle(new Vec3(c.x, pos.getY() + 0.05, c.z), 0.45F, GizmoStyle.fill(0x80000000 | area.rgb()));
                    }
                    Gizmos.cuboid(new AABB(pos).inflate(0.03), GizmoStyle.stroke(stroke, 2.0F));
                    if (pos.getCenter().distanceToSqr(eye) < 14 * 14) {
                        Gizmos.billboardText(area.label().getString(), pos.getCenter().add(0, 0.9, 0),
                                TextGizmo.Style.forColorAndCentered(0xFF000000 | area.rgb()).withScale(0.28F)).setAlwaysOnTop();
                    }
                }
            }
        }
    }

    private static Component radiusLabel(BlockEntity be, double r) {
        return Component.translatable("gui.mistech.range.label", be.getBlockState().getBlock().getName(),
                String.format(java.util.Locale.ROOT, "%.0f", r));
    }

    private static @Nullable Area area(BlockEntity be) {
        BlockPos pos = be.getBlockPos();
        if (be instanceof com.mistech.mistechmod.machine.FarmMachineBlockEntity farm) {
            AABB box = farm.workArea();
            return new Area(box, 0, 0x7CD04A, Component.translatable("gui.mistech.range.area", be.getBlockState().getBlock().getName(),
                    (int) box.getXsize(), (int) box.getZsize()));
        }
        if (be instanceof com.mistech.mistechmod.block.IncenseHolderBlockEntity holder) {
            double r = holder.clientRadius();
            if (r <= 0) {
                return new Area(null, com.mistech.mistechmod.block.IncenseHolderBlockEntity.EFFECT_RADIUS, 0x9A9AB0,
                        Component.translatable("gui.mistech.range.unlit", be.getBlockState().getBlock().getName()));
            }
            if (holder.clientSilver()) {
                return new Area(null, com.mistech.mistechmod.folklore.SilverIncenseItem.WARD_RADIUS, 0xD8E0F0,
                        Component.translatable("gui.mistech.range.silver", (int) (r * 2), com.mistech.mistechmod.folklore.SilverIncenseItem.WARD_RADIUS));
            }
            return new Area(null, r, 0xC49CF5, radiusLabel(be, r));
        }
        if (be instanceof com.mistech.mistechmod.block.arcane.ArcaneNodeBlockEntity) {
            return new Area(null, com.mistech.mistechmod.block.arcane.ArcaneNetwork.RANGE, 0x5FD8F0,
                    radiusLabel(be, com.mistech.mistechmod.block.arcane.ArcaneNetwork.RANGE));
        }
        if (be instanceof com.mistech.mistechmod.block.BottledGnomeBlockEntity) {
            int r = com.mistech.mistechmod.machine.GnomeBoost.RADIUS;
            return new Area(new AABB(pos).inflate(r), 0, 0xE05050, radiusLabel(be, r));
        }
        if (be instanceof com.mistech.mistechmod.garden.BottledMandrakeBlockEntity) {
            int r = com.mistech.mistechmod.garden.BottledMandrakeBlockEntity.RADIUS;
            return new Area(new AABB(pos.getX() - r, pos.getY() - 2, pos.getZ() - r, pos.getX() + r + 1, pos.getY() + 2, pos.getZ() + r + 1),
                    0, 0x7CD04A, radiusLabel(be, r));
        }
        if (be instanceof com.mistech.mistechmod.machine.AdvancedRunicBatteryBlockEntity) {
            int push = com.mistech.mistechmod.machine.AdvancedRunicBatteryBlockEntity.PUSH_RANGE;
            int pull = com.mistech.mistechmod.machine.AdvancedRunicBatteryBlockEntity.PULL_RANGE;
            Gizmos.cuboid(new AABB(pos).inflate(pull), GizmoStyle.stroke(0xC0F0C870, 1.5F));
            return new Area(new AABB(pos).inflate(push), 0, 0xB78CF0,
                    Component.translatable("gui.mistech.range.battery", pull, push));
        }
        if (be instanceof com.mistech.mistechmod.purity.block.SanctuaryFontBlockEntity) {
            int r = com.mistech.mistechmod.purity.SanctuaryStructure.PURITY_RADIUS, h = com.mistech.mistechmod.purity.SanctuaryStructure.PURITY_HEIGHT;
            return new Area(new AABB(pos.getX() - r, pos.getY() - h, pos.getZ() - r, pos.getX() + r + 1, pos.getY() + h + 1, pos.getZ() + r + 1),
                    0, 0xF0E0A0, Component.translatable("gui.mistech.range.sanctuary", r));
        }
        if (be instanceof com.mistech.mistechmod.block.MagicCompressorBlockEntity) {
            double r = com.mistech.mistechmod.block.MagicCompressorBlockEntity.PLAYER_RANGE;
            return new Area(null, r, 0xB78CF0, Component.translatable("gui.mistech.range.compressor", (int) r));
        }
        if (be instanceof com.mistech.mistechmod.machine.AstralGeneratorBlockEntity) {
            int r = com.mistech.mistechmod.machine.AstralGeneratorBlockEntity.TABLE_RANGE;
            return new Area(new AABB(pos.getX() - r, pos.getY() - 4, pos.getZ() - r, pos.getX() + r + 1, pos.getY() + 5, pos.getZ() + r + 1),
                    0, 0x4A88C0, Component.translatable("gui.mistech.range.astral_generator", r));
        }
        if (be instanceof com.mistech.mistechmod.beyond.EchoCollectorBlockEntity) {
            int r = com.mistech.mistechmod.beyond.EchoCollectorBlockEntity.CATALYST_RANGE;
            return new Area(new AABB(pos).inflate(r), 0, 0x2AC0C8, radiusLabel(be, r));
        }
        return null;
    }
}
