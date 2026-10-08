package com.mistech.mistechmod.item.rune;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Runa de Magma (Fogo + Terra): água → obsidiana; obsidiana → lava; pedra → bloco de magma. 16 usos.
 * Mira em fontes de água (como um balde vazio) ou direto no bloco.
 * Cada uso dá 1 ponto de afinidade com o Fogo e 1 com a Terra.
 */
public class MagmaRuneItem extends Item {
    public MagmaRuneItem(Properties properties) {
        super(properties);
    }

    /** Para qual bloco o alvo se transforma, ou null se não reage. */
    public static @Nullable BlockState meltResult(BlockState state, FluidState fluid) {
        if (state.is(Blocks.WATER) && fluid.isSourceOfType(Fluids.WATER)) {
            return Blocks.OBSIDIAN.defaultBlockState();
        }
        if (state.is(Blocks.OBSIDIAN)) {
            return Blocks.LAVA.defaultBlockState();
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE)) {
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        }
        return null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }
        BlockState state = level.getBlockState(pos);
        BlockState result = meltResult(state, level.getFluidState(pos));
        if (result == null || state.is(Blocks.MAGMA_BLOCK)) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            level.setBlock(pos, result, Block.UPDATE_ALL);
            double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
            if (result.is(Blocks.OBSIDIAN)) {
                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.4, 0.2, 0.4, 0.02);
            } else if (result.is(Blocks.LAVA)) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 1.0F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.LAVA, x, y, z, 8, 0.3, 0.1, 0.3, 0.0);
            } else {
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.8F);
                serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 12, 0.4, 0.1, 0.4, 0.01);
            }
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.FIRE, 1);
            Affinity.grant(player, RuneElement.EARTH, 1);
        }
        return InteractionResult.SUCCESS;
    }
}
