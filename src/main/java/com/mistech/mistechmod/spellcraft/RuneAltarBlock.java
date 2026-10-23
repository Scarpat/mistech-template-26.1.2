package com.mistech.mistechmod.spellcraft;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Altar do Círculo de Pedras Rúnico: rezar nele (botão direito) abençoa o jogador para sempre — a marca de ressonância dura 8 s. */
public class RuneAltarBlock extends Block {
    public static final MapCodec<RuneAltarBlock> CODEC = simpleCodec(RuneAltarBlock::new);

    public RuneAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getData(SpellcraftContent.CIRCLE_BLESSED.get())) {
                serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.circle.already").withStyle(net.minecraft.ChatFormatting.GRAY));
            } else {
                serverPlayer.setData(SpellcraftContent.CIRCLE_BLESSED.get(), true);
                serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.circle.blessed").withColor(0xB98CFF));
                server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 80, 0.8, 0.8, 0.8, 0.5);
                server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.4F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.6, 0.4, (random.nextDouble() - 0.5) * 0.6);
        }
    }
}
