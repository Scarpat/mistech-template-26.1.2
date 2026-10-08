package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.GlassJarBlock;
import com.mistech.mistechmod.block.GlassJarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Quando um raio cai, potes de vidro VAZIOS por perto viram Raio Engarrafado. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class LightningEvents {
    /** Distância (em blocos, em cada eixo) do ponto do raio até o pote. */
    public static final int CAPTURE_RADIUS = 2;

    private LightningEvents() {}

    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = bolt.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-CAPTURE_RADIUS, -CAPTURE_RADIUS - 1, -CAPTURE_RADIUS),
                center.offset(CAPTURE_RADIUS, CAPTURE_RADIUS, CAPTURE_RADIUS))) {
            if (level.getBlockState(pos).getBlock() instanceof GlassJarBlock
                    && level.getBlockEntity(pos) instanceof GlassJarBlockEntity jar && jar.isEmpty()) {
                BlockPos jarPos = pos.immutable();
                level.setBlock(jarPos, Mistech.BOTTLED_LIGHTNING.get().defaultBlockState(), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, jarPos.getX() + 0.5, jarPos.getY() + 0.4, jarPos.getZ() + 0.5,
                        30, 0.25, 0.3, 0.25, 0.4);
                level.playSound(null, jarPos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.6F, 1.6F);
            }
        }
    }
}
