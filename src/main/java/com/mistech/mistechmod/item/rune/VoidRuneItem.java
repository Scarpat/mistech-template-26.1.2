package com.mistech.mistechmod.item.rune;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Runa do Vazio (Espaço + Escuridão + Terra): apaga o bloco alvo, sem deixar drop.
 * Também apaga fontes de água/lava. Não afeta blocos inquebráveis (bedrock, portais...).
 * Baús e afins derrubam o que tinham dentro. 16 usos.
 */
public class VoidRuneItem extends Item {
    public VoidRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.5F);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.SPACE, 1);
            Affinity.grant(player, RuneElement.DARKNESS, 1);
            Affinity.grant(player, RuneElement.EARTH, 1);
        }
        return InteractionResult.SUCCESS;
    }
}
