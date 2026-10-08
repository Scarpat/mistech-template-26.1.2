package com.mistech.mistechmod.power;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.ether.EtherContent;
import com.mistech.mistechmod.ether.EtherNetworks;
import com.mistech.mistechmod.ether.EtherealNexusBlockEntity;
import com.mistech.mistechmod.item.PocketBatteryItem;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * Bateria de Bolso Astral: não guarda energia — é uma janela para uma sintonia da Rede Etérea.
 * <ul>
 * <li>Botão direito num Nexo Etéreo: sintoniza (também copia de um Diapasão sintonizado na outra mão).</li>
 * <li>Ligada e no inventário: carrega os itens com a energia da rede, em qualquer distância e dimensão —
 * até {@link #PER_ITEM_PER_TICK} FE/t por item e {@link #TOTAL_PER_TICK} FE/t no total.</li>
 * <li>Em qualquer carregador: entrega (e recebe) até {@link #MAX_TRANSFER} FE/t da rede.</li>
 * <li>Agachar + botão direito no ar: liga/desliga.</li>
 * </ul>
 * A barra mostra quanto a rede tem; ela é atualizada pelo servidor (componente {@code mistech:astral_battery_view}).
 */
public class AstralPocketBatteryItem extends Item implements PocketCharger {
    public static final int PER_ITEM_PER_TICK = 10_000;
    public static final long TOTAL_PER_TICK = 50_000;
    public static final int MAX_TRANSFER = 20_000;

    public AstralPocketBatteryItem(Properties properties) {
        super(properties);
    }

    public static int network(ItemStack stack) {
        return stack.getOrDefault(EtherContent.NETWORK.get(), 0);
    }

    public static @Nullable NetworkEnergyHandler handler(ItemStack stack, int maxInsert, int maxExtract) {
        return NetworkEnergyHandler.of(network(stack), maxInsert, maxExtract);
    }

    /**
     * Capability de energia do item: no servidor, a rede de verdade; no cliente (ou sem rede), só leitura do que a barra mostra —
     * assim os espaços de carga aceitam a bateria dos dois lados.
     */
    public static EnergyHandler capability(ItemStack stack) {
        NetworkEnergyHandler handler = handler(stack, MAX_TRANSFER, MAX_TRANSFER);
        if (handler != null) {
            return handler;
        }
        AstralView view = view(stack);
        return new EnergyHandler() {
            @Override
            public long getAmountAsLong() {
                return view.energy();
            }

            @Override
            public long getCapacityAsLong() {
                return view.capacity();
            }

            @Override
            public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                return 0;
            }

            @Override
            public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                return 0;
            }
        };
    }

    // ----- carregar -----

    @Override
    public boolean canChargeNow(Player player, ItemStack stack) {
        return PocketBatteryItem.isActive(stack) && network(stack) > 0;
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
    public @Nullable EnergyHandler chargeSource(Player player, int slot, ItemStack stack) {
        NetworkEnergyHandler handler = handler(stack, 0, Integer.MAX_VALUE);
        return handler == null || handler.getAmountAsLong() <= 0 ? null : handler;
    }

    /** Atualiza o que o cliente vê na barra/dica (chamado no tick do jogador, a cada 10 ticks). */
    public static void refreshView(ItemStack stack) {
        NetworkEnergyHandler handler = handler(stack, 0, 0);
        AstralView view = handler == null ? AstralView.NONE
                : new AstralView(handler.net().energy, handler.net().capacity, handler.net().name);
        AstralView old = stack.get(PowerContent.ASTRAL_VIEW.get());
        if (old == null || !old.similar(view)) {
            stack.set(PowerContent.ASTRAL_VIEW.get(), view);
        }
    }

    // ----- sintonizar / ligar -----

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof EtherealNexusBlockEntity nexus)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        tune(stack, nexus.network(), context.getPlayer(), server, pos);
        return InteractionResult.SUCCESS;
    }

    private static void tune(ItemStack stack, int id, @Nullable Player player, ServerLevel level, BlockPos pos) {
        EtherNetworks.Net net = EtherNetworks.get(level.getServer()).get(id);
        if (net == null) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.tuner.no_network").withStyle(ChatFormatting.RED));
            }
            return;
        }
        stack.set(EtherContent.NETWORK.get(), id);
        refreshView(stack);
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.astral_battery.tuned", net.name).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.05);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Diapasão sintonizado na outra mão: copia a sintonia dele
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        Integer tunerNet = other.is(EtherContent.ARCANE_TUNER.get()) ? other.get(EtherContent.NETWORK.get()) : null;
        if (!player.isShiftKeyDown() && tunerNet != null) {
            if (level instanceof ServerLevel server) {
                tune(stack, tunerNet, player, server, player.blockPosition());
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        boolean active = !PocketBatteryItem.isActive(stack);
        if (!level.isClientSide()) {
            stack.set(MistechComponents.POCKET_BATTERY_ACTIVE.get(), active);
            player.sendOverlayMessage(Component.translatable(active ? "message.mistech.pocket_battery.on" : "message.mistech.pocket_battery.off")
                    .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.4F, 1.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    // ----- barra e dica -----

    private static AstralView view(ItemStack stack) {
        return stack.getOrDefault(PowerContent.ASTRAL_VIEW.get(), AstralView.NONE);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return PocketBatteryItem.isActive(stack) && network(stack) > 0 && view(stack).energy() > 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return network(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * view(stack).fill());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = view(stack).fill();
        return (Mth.lerpInt(f, 0x3A, 0xC0) << 16) | (Mth.lerpInt(f, 0x4A, 0xE8) << 8) | Mth.lerpInt(f, 0xC8, 0xFF);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        if (network(stack) <= 0) {
            builder.accept(Component.translatable("tooltip.mistech.astral_battery.untuned").withStyle(ChatFormatting.RED));
        } else {
            AstralView view = view(stack);
            builder.accept(Component.translatable("tooltip.mistech.astral_battery.network", view.name().isEmpty() ? "#" + network(stack) : view.name())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            builder.accept(Component.translatable("gui.mistech.energy_tooltip", fmt(view.energy()), fmt(view.capacity())).withStyle(ChatFormatting.GRAY));
        }
        boolean active = PocketBatteryItem.isActive(stack);
        builder.accept(Component.translatable(active ? "tooltip.mistech.pocket_battery.on" : "tooltip.mistech.pocket_battery.off")
                .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.mistech.astral_battery.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fmt(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", value).replace(',', '.');
    }
}
