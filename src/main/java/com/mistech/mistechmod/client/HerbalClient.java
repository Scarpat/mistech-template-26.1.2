package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.Tags;

/**
 * Visão Mística / Psicodélica: os minérios escondidos na pedra por perto soltam brilhos (só quem bebeu vê).
 * Sorteia algumas posições por tick em volta do jogador (barato) e marca as que são minério.
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class HerbalClient {
    public static final int RADIUS = 12;
    private static final int SAMPLES = 48;

    /** Minérios achados e até quando aparecem (tick do jogo). */
    private static final java.util.Map<BlockPos, Long> FOUND = new java.util.HashMap<>();
    private static final int SHOW_TICKS = 60, MAX_FOUND = 48;

    private HerbalClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        boolean mystic = player.hasEffect(MistechEffects.MYSTIC_SIGHT);
        boolean psychedelic = player.hasEffect(MistechEffects.PSYCHEDELIC_SIGHT);
        if (!mystic && !psychedelic) {
            FOUND.clear();
            return;
        }
        long now = mc.level.getGameTime();
        FOUND.values().removeIf(until -> until < now);
        RandomSource random = player.getRandom();
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < SAMPLES; i++) {
            cursor.set(origin.getX() + random.nextInt(RADIUS * 2 + 1) - RADIUS, origin.getY() + random.nextInt(RADIUS + 1) - RADIUS / 2,
                    origin.getZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS);
            BlockState state = mc.level.getBlockState(cursor);
            if (!state.is(Tags.Blocks.ORES)) {
                continue;
            }
            boolean gnomeMetal = state.getBlock().builtInRegistryHolder().key().identifier().getNamespace().equals(Mistech.MODID);
            if (FOUND.size() < MAX_FOUND) {
                FOUND.put(cursor.immutable(), now + SHOW_TICKS);
            }
            for (int n = 0; n < 2; n++) {
                double x = cursor.getX() + random.nextDouble(), y = cursor.getY() + random.nextDouble(), z = cursor.getZ() + random.nextDouble();
                mc.level.addParticle(gnomeMetal ? ParticleTypes.END_ROD : ParticleTypes.WITCH, true, true, x, y, z, 0.0, 0.01, 0.0);
            }
        }
        // os brilhos atravessam a pedra (contorno por cima de tudo, piscando devagar)
        try {
            float pulse = 0.5F + 0.5F * net.minecraft.util.Mth.sin(now * 0.25F);
            int alpha = (int) (0x40 + 0x60 * pulse);
            for (var entry : FOUND.entrySet()) {
                BlockPos pos = entry.getKey();
                boolean gnomeMetal = mc.level.getBlockState(pos).getBlock().builtInRegistryHolder().key().identifier().getNamespace().equals(Mistech.MODID);
                int rgb = gnomeMetal ? 0xF0D890 : 0xB78CF0;
                net.minecraft.gizmos.Gizmos.cuboid(new net.minecraft.world.phys.AABB(pos).deflate(0.3),
                        net.minecraft.gizmos.GizmoStyle.strokeAndFill((alpha << 24) | rgb, 1.5F, ((alpha / 2) << 24) | rgb)).setAlwaysOnTop();
            }
        } catch (IllegalStateException ignored) {
            // sem coletor de gizmos neste momento
        }
    }
}
