package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.item.SmokableItem;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Mistech.MODID)
public final class IncenseEvents {
    private IncenseEvents() {}

    /** Zumbis/esqueletos não conseguem mirar em jogadores com o Véu de Incenso. */
    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget instanceof Player player
                && player.hasEffect(MistechEffects.INCENSE_VEIL)
                && event.getEntity().is(MistechTags.INCENSE_IGNORES)) {
            event.setCanceled(true);
        }
    }

    /**
     * Isqueiro na mão principal + incenso na secundária mirando um bloco:
     * sem isso o isqueiro colocaria fogo no bloco em vez de acender o incenso.
     */
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack flint = player.getItemInHand(hand);
        ItemStack incense = player.getItemInHand(otherHand);

        if (IncenseItem.isLighter(flint) && IncenseItem.isIncense(incense) && !IncenseItem.isLit(incense)) {
            IncenseItem.lightInHand(event.getLevel(), player, otherHand, flint, hand);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
        // mesmo caso para charuto/baseado
        else if (IncenseItem.isLighter(flint) && incense.getItem() instanceof SmokableItem smokable && !SmokableItem.isLit(incense)
                && smokable.canLight(incense)) {
            smokable.lightInHand(event.getLevel(), player, otherHand, flint, hand);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
