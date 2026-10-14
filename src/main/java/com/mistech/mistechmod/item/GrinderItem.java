package com.mistech.mistechmod.item;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Moedor: com algo moível na outra mão, usar mói 1 unidade (gasta 1 de durabilidade).
 * - Grão de café torrado → Pó de Café
 * - Bloco de ametista → 4 fragmentos de ametista
 */
public class GrinderItem extends Item {
    public GrinderItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack beans = player.getItemInHand(otherHand);
        ItemStack result = grind(beans);
        if (result.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack grinder = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) {
                beans.shrink(1);
            }
            ItemStack powder = result.copy();
            if (!player.getInventory().add(powder)) {
                player.drop(powder, false);
            }
            grinder.hurtAndBreak(1, player, hand.asEquipmentSlot());
            boolean amethyst = result.is(Items.AMETHYST_SHARD);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS,
                    0.5F, 1.4F + serverLevel.getRandom().nextFloat() * 0.2F);
            if (amethyst) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS,
                        0.7F, 1.0F);
            }
            Vec3 look = player.getLookAngle();
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, result.getItem()),
                    player.getX() + look.x * 0.5, player.getEyeY() - 0.4, player.getZ() + look.z * 0.5, 6, 0.1, 0.05, 0.1, 0.02);
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 6);
        return InteractionResult.SUCCESS;
    }

    /** O que sai de moer 1 unidade deste item (vazio = não mói). */
    public static ItemStack grind(ItemStack input) {
        if (input.is(Mistech.ROASTED_COFFEE_BEANS.get())) {
            return new ItemStack(Mistech.COFFEE_POWDER.get());
        }
        if (input.is(Items.AMETHYST_BLOCK)) {
            return new ItemStack(Items.AMETHYST_SHARD, 4);
        }
        return ItemStack.EMPTY;
    }
}
