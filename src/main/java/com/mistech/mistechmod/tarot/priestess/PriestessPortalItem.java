package com.mistech.mistechmod.tarot.priestess;

import java.util.function.Consumer;

import com.mistech.mistechmod.tarot.ArcanePlane;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * O Portal (Forja da Sacerdotisa): botão direito leva para a sala própria do jogador no Santuário de Bolso
 * ({@link PocketSanctum}) e, lá dentro, de volta para onde ele estava. Não funciona no Plano Arcano nem montado.
 */
public class PriestessPortalItem extends Item {
    public static final int COOLDOWN = 60;

    public PriestessPortalItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ArcanePlane.isPlane(level) || ArcanePlane.isFighting(player)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.priestess_portal.blocked").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (player.isPassenger() || player.isVehicle()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.priestess_portal.mounted").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PocketSanctum.use(serverPlayer);
            serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.mistech.priestess_portal").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.mistech.priestess_portal.exit").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
