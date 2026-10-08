package com.mistech.mistechmod.beyond;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Coração Rangente: um dos Quatro Corações. Usado num <b>Tronco de Carvalho Pálido</b>, é gasto e desperta uma
 * {@link PaleSentinelEntity Sentinela Rangente} presa àquele tronco (até {@link PaleSentinelEntity#MAX_PER_PLAYER} por jogador).
 */
public class PaleHeartItem extends HeartItem {
    public PaleHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null || !context.getLevel().getBlockState(pos).is(BlockTags.PALE_OAK_LOGS)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (PaleSentinelEntity.countOwned(level, player.getUUID()) >= PaleSentinelEntity.MAX_PER_PLAYER) {
            player.sendOverlayMessage(Component.translatable("message.mistech.pale_sentinel.limit", PaleSentinelEntity.MAX_PER_PLAYER));
            return InteractionResult.FAIL;
        }
        BlockPos spawn = pos.relative(context.getClickedFace());
        PaleSentinelEntity sentinel = PaleNetherContent.PALE_SENTINEL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (sentinel == null) {
            return InteractionResult.FAIL;
        }
        // nasce no chão ao lado do tronco
        BlockPos ground = spawn;
        for (int i = 0; i < 6 && level.getBlockState(ground.below()).isAir(); i++) {
            ground = ground.below();
        }
        sentinel.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        sentinel.bind(pos, player);
        level.addFreshEntity(sentinel);
        level.sendParticles(ParticleTypes.WHITE_ASH, ground.getX() + 0.5, ground.getY() + 1.4, ground.getZ() + 0.5, 50, 0.5, 1.0, 0.5, 0.02);
        level.playSound(null, pos, SoundEvents.CREAKING_HEART_SPAWN, SoundSource.BLOCKS, 1.2F, 0.8F);
        player.sendOverlayMessage(Component.translatable("message.mistech.pale_sentinel.awake").withColor(0xE07A1F));
        ItemStack stack = context.getItemInHand();
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
