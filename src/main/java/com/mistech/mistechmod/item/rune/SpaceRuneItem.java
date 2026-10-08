package com.mistech.mistechmod.item.rune;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.level.Level;

/** Runa do Espaço: teleporta aleatoriamente, igual comer uma Chorus Fruit. 16 usos, recarga de 1 s. */
public class SpaceRuneItem extends Item {
    private static final TeleportRandomlyConsumeEffect TELEPORT = new TeleportRandomlyConsumeEffect();

    public SpaceRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (TELEPORT.apply(level, stack, player)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.SPACE, 1);
            }
            player.getCooldowns().addCooldown(stack, 20);
        }
        return InteractionResult.SUCCESS;
    }
}
