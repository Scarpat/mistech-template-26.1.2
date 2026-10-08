package com.mistech.mistechmod.item.rune;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Runa de Gelo: água → gelo → gelo compacto → gelo azul; lava → obsidiana. 16 usos.
 * Mira em fontes de água/lava (como um balde vazio) ou direto no bloco de gelo.
 */
public class IceRuneItem extends Item {
    public IceRuneItem(Properties properties) {
        super(properties);
    }

    /** Para qual bloco o alvo se transforma, ou null se não reage. */
    public static @Nullable Block freezeResult(BlockState state, FluidState fluid) {
        if (state.is(Blocks.WATER) && fluid.isSourceOfType(Fluids.WATER)) {
            return Blocks.ICE;
        }
        if (state.is(Blocks.LAVA) && fluid.isSourceOfType(Fluids.LAVA)) {
            return Blocks.OBSIDIAN;
        }
        if (state.is(Blocks.ICE)) {
            return Blocks.PACKED_ICE;
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return Blocks.BLUE_ICE;
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
        Block result = freezeResult(state, level.getFluidState(pos));
        if (result == null) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            level.setBlock(pos, result.defaultBlockState(), Block.UPDATE_ALL);
            boolean fromLava = result == Blocks.OBSIDIAN;
            level.playSound(null, pos, fromLava ? SoundEvents.LAVA_EXTINGUISH : SoundEvents.GLASS_PLACE,
                    SoundSource.PLAYERS, 1.0F, fromLava ? 1.0F : 1.4F);
            serverLevel.sendParticles(fromLava ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.4, 0.2, 0.4, 0.02);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.ICE, 1);
        }
        return InteractionResult.SUCCESS;
    }
}
