package com.mistech.mistechmod.power;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.PocketBatteryItem;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Bateria de Bolso Criativa (só para testes; sem receita): energia infinita.
 * <ul>
 * <li>Ligada e no inventário: enche todos os itens com energia de uma vez (mão, armadura, acessórios, inventário).</li>
 * <li>Em qualquer carregador: dá energia sem fim; recebendo, some com a energia (serve para testar geradores).</li>
 * <li>Botão direito num bloco com energia: enche o bloco até o talo.</li>
 * <li>Agachar + botão direito no ar: liga/desliga.</li>
 * </ul>
 */
public class CreativePocketBatteryItem extends Item implements PocketCharger {
    public CreativePocketBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canChargeNow(Player player, ItemStack stack) {
        return PocketBatteryItem.isActive(stack);
    }

    @Override
    public int perItemPerTick() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long totalPerTick() {
        return Long.MAX_VALUE;
    }

    @Override
    public @Nullable EnergyHandler chargeSource(Player player, int slot, ItemStack stack) {
        return InfiniteEnergyHandler.INSTANCE;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (target == null) {
            target = level.getCapability(Capabilities.Energy.BLOCK, context.getClickedPos(), null);
        }
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        long total = 0;
        for (int i = 0; i < 64; i++) {
            int moved;
            try (Transaction tx = Transaction.openRoot()) {
                moved = target.insert(Integer.MAX_VALUE, tx);
                tx.commit();
            }
            if (moved <= 0) {
                break;
            }
            total += moved;
        }
        Player player = context.getPlayer();
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.creative_battery.filled", String.format(java.util.Locale.ROOT, "%,d", total)
                    .replace(',', '.'), target.getAmountAsLong(), target.getCapacityAsLong()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        level.playSound(null, context.getClickedPos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5F, 1.8F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean active = !PocketBatteryItem.isActive(stack);
        if (!level.isClientSide()) {
            stack.set(MistechComponents.POCKET_BATTERY_ACTIVE.get(), active);
            player.sendOverlayMessage(Component.translatable(active ? "message.mistech.pocket_battery.on" : "message.mistech.pocket_battery.off")
                    .withStyle(active ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.4F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return PocketBatteryItem.isActive(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // arco-íris girando
        float hue = (System.currentTimeMillis() % 4000L) / 4000.0F;
        return net.minecraft.util.Mth.hsvToRgb(hue, 0.6F, 1.0F) & 0xFFFFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("tooltip.mistech.creative_battery.energy").withStyle(ChatFormatting.LIGHT_PURPLE));
        boolean active = PocketBatteryItem.isActive(stack);
        builder.accept(Component.translatable(active ? "tooltip.mistech.pocket_battery.on" : "tooltip.mistech.pocket_battery.off")
                .withStyle(active ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.mistech.creative_battery.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
