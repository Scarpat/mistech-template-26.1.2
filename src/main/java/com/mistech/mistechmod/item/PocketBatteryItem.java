package com.mistech.mistechmod.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;

/**
 * Bateria de Bolso: 1.000.000 FE (mesmo componente {@code mistech:battery_energy} das Baterias Rúnicas,
 * então o ritual passa a energia da Bateria Aprimorada para ela).
 * Carrega em qualquer carregador (espaço de carga das baterias, Bicicleta do Gnomo...), até 10.000 FE/t.
 * <p>
 * Ligada (padrão) e no inventário: a cada {@link #INTERVAL} ticks carrega os itens com energia do jogador —
 * mão, outra mão, armadura, acessórios e o resto do inventário — até {@link #PER_ITEM_PER_TICK} FE/t por item
 * e {@link #TOTAL_PER_TICK} FE/t no total. Não carrega outras Baterias de Bolso.
 * Agachar + botão direito liga/desliga (brilho encantado = ligada).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public class PocketBatteryItem extends Item implements com.mistech.mistechmod.power.PocketCharger {
    public static final int CAPACITY = 1_000_000;
    public static final int MAX_CHARGE = 10_000;
    public static final int PER_ITEM_PER_TICK = 1_000;
    public static final int TOTAL_PER_TICK = 5_000;
    private static final int INTERVAL = 10;

    public PocketBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canChargeNow(Player player, ItemStack stack) {
        return isActive(stack) && energy(stack) > 0;
    }

    @Override
    public int perItemPerTick() {
        return PER_ITEM_PER_TICK;
    }

    @Override
    public long totalPerTick() {
        return TOTAL_PER_TICK;
    }

    @Override
    public EnergyHandler chargeSource(Player player, int slot, ItemStack stack) {
        return stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forPlayerSlot(player, slot));
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.BATTERY_ENERGY.get(), 0);
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.POCKET_BATTERY_ACTIVE.get(), true);
    }

    // ----- liga/desliga -----

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean active = !isActive(stack);
        if (!level.isClientSide()) {
            stack.set(MistechComponents.POCKET_BATTERY_ACTIVE.get(), active);
            player.sendOverlayMessage(Component.translatable(active ? "message.mistech.pocket_battery.on" : "message.mistech.pocket_battery.off")
                    .withStyle(active ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.4F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack) && energy(stack) > 0;
    }

    // ----- barra e tooltip -----

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Math.min(1.0F, energy(stack) / (float) CAPACITY));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.min(1.0F, energy(stack) / (float) CAPACITY);
        return (Mth.lerpInt(f, 0x96, 0xF0) << 16) | (Mth.lerpInt(f, 0x5A, 0xC8) << 8) | Mth.lerpInt(f, 0xE6, 0x70);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("gui.mistech.energy_tooltip", fmt(energy(stack)), fmt(CAPACITY)).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(isActive(stack) ? "tooltip.mistech.pocket_battery.on" : "tooltip.mistech.pocket_battery.off")
                .withStyle(isActive(stack) ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.mistech.pocket_battery.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fmt(int value) {
        return String.format(java.util.Locale.ROOT, "%,d", value).replace(',', '.');
    }

    // ----- carregar o inventário -----

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isSpectator() || player.tickCount % INTERVAL != 0) {
            return;
        }
        Inventory inventory = player.getInventory();
        int size = Math.min(inventory.getContainerSize(), 41);

        // carregadores ligados (Baterias de Bolso comum, Astral, Criativa)
        List<Integer> chargers = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof com.mistech.mistechmod.power.AstralPocketBatteryItem) {
                com.mistech.mistechmod.power.AstralPocketBatteryItem.refreshView(stack);
            }
            if (stack.getItem() instanceof com.mistech.mistechmod.power.PocketCharger charger && charger.canChargeNow(player, stack)) {
                chargers.add(slot);
            }
        }
        if (chargers.isEmpty()) {
            return;
        }

        // alvos: mão, outra mão, armadura, acessórios, resto do inventário
        List<EnergyHandler> targets = new ArrayList<>();
        int selected = inventory.getSelectedSlot();
        List<Integer> order = new ArrayList<>();
        order.add(selected);
        order.add(40);
        for (int i = 36; i < 40; i++) order.add(i);
        for (int slot : order) {
            addSlot(player, inventory, slot, size, targets);
        }
        for (ItemStack accessory : Accessories.all(player)) {
            if (!accessory.isEmpty() && !(accessory.getItem() instanceof com.mistech.mistechmod.power.PocketCharger)) {
                EnergyHandler handler = accessory.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(accessory));
                if (handler != null) {
                    targets.add(handler);
                }
            }
        }
        for (int slot = 0; slot < 36; slot++) {
            if (slot != selected) {
                addSlot(player, inventory, slot, size, targets);
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        // cada tipo de bateria tem o seu teto (várias do mesmo tipo dividem)
        java.util.Map<Class<?>, Long> budgets = new java.util.HashMap<>();
        for (int chargerSlot : chargers) {
            ItemStack stack = inventory.getItem(chargerSlot);
            if (!(stack.getItem() instanceof com.mistech.mistechmod.power.PocketCharger charger)) {
                continue;
            }
            long total = charger.totalPerTick();
            long budget = budgets.computeIfAbsent(charger.getClass(), c -> total >= Long.MAX_VALUE / INTERVAL ? Long.MAX_VALUE : total * INTERVAL);
            if (budget <= 0) {
                continue;
            }
            EnergyHandler source = charger.chargeSource(player, chargerSlot, stack);
            if (source == null) {
                continue;
            }
            long perItem = Math.min((long) charger.perItemPerTick() * INTERVAL, Integer.MAX_VALUE);
            for (EnergyHandler target : targets) {
                if (budget <= 0 || source.getAmountAsLong() <= 0) {
                    break;
                }
                int moved = EnergyHandlerUtil.move(source, target, (int) Math.min(perItem, budget), null);
                if (budget != Long.MAX_VALUE) {
                    budget -= moved;
                }
            }
            budgets.put(charger.getClass(), budget);
        }
    }

    private static void addSlot(Player player, Inventory inventory, int slot, int size, List<EnergyHandler> targets) {
        if (slot >= size) {
            return;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty() || stack.getItem() instanceof com.mistech.mistechmod.power.PocketCharger) {
            return;
        }
        EnergyHandler handler = stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forPlayerSlot(player, slot));
        if (handler != null) {
            targets.add(handler);
        }
    }
}
