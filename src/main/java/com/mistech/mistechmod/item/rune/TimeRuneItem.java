package com.mistech.mistechmod.item.rune;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Runa do Tempo. 16 usos.
 * - Em plantas/blocos: efeito de farinha de osso.
 * - Em ametista brotando: todos os brotos em volta crescem 1 estágio (e nascem brotos novos nos lados livres).
 * - Em um broto/cristal de ametista: ele cresce 1 estágio.
 * - Em um animal filhote: ele vira adulto na hora.
 */
public class TimeRuneItem extends Item {
    public TimeRuneItem(Properties properties) {
        super(properties);
    }

    // ----- blocos -----

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        boolean worked;
        if (state.getBlock() instanceof BuddingAmethystBlock) {
            worked = !level.isClientSide() && growAroundBudding(level, pos);
        } else if (nextBudStage(state) != null) {
            worked = !level.isClientSide() && growBud(level, pos, state);
        } else {
            // farinha de osso "virtual" (não gasta farinha de verdade)
            worked = BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos);
            if (worked && !level.isClientSide()) {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15); // partículas verdes da farinha de osso
            }
        }

        if (!worked) {
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            timeFx(serverLevel, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
                com.mistech.mistechmod.affinity.Affinity.grant(context.getPlayer(), com.mistech.mistechmod.item.RuneElement.TIME, 1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static @Nullable Block nextBudStage(BlockState state) {
        if (state.is(Blocks.SMALL_AMETHYST_BUD)) return Blocks.MEDIUM_AMETHYST_BUD;
        if (state.is(Blocks.MEDIUM_AMETHYST_BUD)) return Blocks.LARGE_AMETHYST_BUD;
        if (state.is(Blocks.LARGE_AMETHYST_BUD)) return Blocks.AMETHYST_CLUSTER;
        return null;
    }

    private static boolean growBud(Level level, BlockPos pos, BlockState state) {
        Block next = nextBudStage(state);
        if (next == null) {
            return false;
        }
        level.setBlockAndUpdate(pos, next.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, state.getValue(AmethystClusterBlock.FACING))
                .setValue(AmethystClusterBlock.WATERLOGGED, state.getValue(AmethystClusterBlock.WATERLOGGED)));
        return true;
    }

    /** Faz cada lado da ametista brotando avançar 1 estágio (igual ao crescimento natural, mas em todos os lados). */
    private static boolean growAroundBudding(Level level, BlockPos budding) {
        boolean any = false;
        for (Direction dir : Direction.values()) {
            BlockPos pos = budding.relative(dir);
            BlockState state = level.getBlockState(pos);
            if (BuddingAmethystBlock.canClusterGrowAtState(state)) {
                level.setBlockAndUpdate(pos, Blocks.SMALL_AMETHYST_BUD.defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, dir)
                        .setValue(AmethystClusterBlock.WATERLOGGED, state.getFluidState().is(Fluids.WATER)));
                any = true;
            } else if (nextBudStage(state) != null && state.getValue(AmethystClusterBlock.FACING) == dir) {
                any |= growBud(level, pos, state);
            }
        }
        return any;
    }

    // ----- filhotes -----

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof AgeableMob mob) || !mob.isBaby()) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            mob.setAge(0); // vira adulto
            timeFx(serverLevel, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ());
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.TIME, 1);
        }
        return InteractionResult.SUCCESS;
    }

    private static void timeFx(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 25, 0.35, 0.35, 0.35, 0.05);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 8, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
