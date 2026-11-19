package com.mistech.mistechmod.spellcraft.well;

import java.util.function.Consumer;

import com.mistech.mistechmod.spellcraft.ManaService;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Poção de Mana: uma garrafa de vidro cheia da mana líquida do Poço ({@link #MANA} pontos). Beber devolve a mana (o que passar do
 * máximo vai para a reserva do cajado na mão) e a garrafa. Na Sangria Arcana vira FE na hora.
 */
public class ManaPotionItem extends Item {
    public static final int MANA = 100;

    public ManaPotionItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            float space = ManaService.max(player) - ManaService.get(player);
            float toPlayer = Math.min(space, MANA);
            ManaService.set(player, ManaService.get(player) + toPlayer);
            int rest = Math.round(MANA - toPlayer);
            ItemStack staff = ManaService.reserveStack(player);
            if (rest > 0 && !staff.isEmpty()) {
                ManaService.addReserve(staff, rest);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mistech.mana_potion.tooltip", MANA).withStyle(ChatFormatting.AQUA));
    }
}
