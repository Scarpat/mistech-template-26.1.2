package com.mistech.mistechmod.power;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Gerador de Experiência: clique abre a tela; agachado com a mão vazia deposita toda a sua experiência. */
public class ExperienceGeneratorBlock extends MachineBlock {
    public static final MapCodec<ExperienceGeneratorBlock> CODEC = simpleCodec(ExperienceGeneratorBlock::new);

    public ExperienceGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties, PowerContent.EXPERIENCE_GENERATOR_BE);
    }

    @Override
    public MapCodec<ExperienceGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof ExperienceGeneratorBlockEntity generator) {
            if (!level.isClientSide()) {
                int moved = generator.deposit(player, true);
                if (moved > 0) {
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 0.8F);
                    player.sendOverlayMessage(Component.translatable("message.mistech.experience_generator.deposit", moved, generator.points(),
                            ExperienceGeneratorBlockEntity.TANK).withStyle(ChatFormatting.GREEN));
                } else {
                    player.sendOverlayMessage(Component.translatable(generator.space() <= 0 ? "message.mistech.experience_generator.full"
                            : "message.mistech.experience_generator.empty").withStyle(ChatFormatting.GRAY));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.9, z = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x + (random.nextDouble() - 0.5) * 0.7, y + random.nextDouble() * 0.4,
                    z + (random.nextDouble() - 0.5) * 0.7, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SCULK_SOUL, x, y + 0.2, z, 0.0, 0.03, 0.0);
        }
    }
}
