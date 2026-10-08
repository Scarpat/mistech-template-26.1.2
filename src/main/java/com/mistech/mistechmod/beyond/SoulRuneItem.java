package com.mistech.mistechmod.beyond;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Runa das Almas (Mesa de Escrita: Areia das Almas + Terra das Almas + Lágrima de Ghast + Runa de Fogo; 15 de giz). 16 usos.
 * Botão direito: {@link #SECONDS} s de <b>Véu das Almas</b> (os mobs do Nether não te escolhem como alvo); mirando um bloco, ainda
 * acende fogo em cima dele (fogo das almas sobre Areia/Terra das Almas).
 */
public class SoulRuneItem extends Item {
    public static final int SECONDS = 60;

    public SoulRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos fire = hit.getBlockPos().relative(hit.getDirection());
                if (level.getBlockState(fire).isAir() && BaseFireBlock.canBePlacedAt(level, fire, player.getDirection())
                        && level.mayInteract(player, fire)) {
                    level.setBlock(fire, BaseFireBlock.getState(level, fire), Block.UPDATE_ALL_IMMEDIATE);
                }
            }
            player.addEffect(new MobEffectInstance(MistechEffects.SOUL_VEIL, SECONDS * 20, 0, false, true, true));
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.6, 0.4, 0.02);
            server.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.6, 0.4, 0.02);
            server.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2F, 0.8F);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.SOUL, 1);
            player.getCooldowns().addCooldown(stack, 40);
        }
        return InteractionResult.SUCCESS;
    }
}
