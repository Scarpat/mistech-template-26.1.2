package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import com.mistech.mistechmod.golem.GolemRoute;
import com.mistech.mistechmod.golem.RoutedGolem;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Vara de Comando Rúnica (Vara de Comando + Runa do Espaço): até {@value GolemRoute#MAX_POINTS} origens e
 * {@value GolemRoute#MAX_POINTS} destinos, e o modo de coleta/entrega.
 * <ul>
 * <li>Botão direito num bloco: marca/desmarca como origem. Agachado: marca/desmarca como destino.</li>
 * <li>Botão direito no ar: troca o modo de entrega. Agachado: troca o modo de coleta.</li>
 * <li>Botão direito no golem: passa a rota. Agachado no golem: copia a rota dele para a vara.</li>
 * <li>Sozinha na mesa de trabalho: limpa tudo.</li>
 * </ul>
 * Todos os pontos a até {@link #RANGE} blocos do primeiro ponto marcado.
 */
public class RunicGolemWandItem extends Item {
    public static final int RANGE = 48;

    public RunicGolemWandItem(Item.Properties properties) {
        super(properties);
    }

    public static GolemRoute routeOf(ItemStack stack) {
        GolemRoute route = stack.get(MistechComponents.GOLEM_ROUTE.get());
        return route == null ? GolemRoute.EMPTY : route;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return this.useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!GolemWandItem.isRoutePoint(level, pos)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        GolemRoute route = routeOf(stack);
        boolean target = player != null && player.isShiftKeyDown();
        BlockPos anchor = !route.sources().isEmpty() ? route.sources().get(0) : !route.targets().isEmpty() ? route.targets().get(0) : null;
        if (anchor != null && !anchor.equals(pos) && !anchor.closerThan(pos, RANGE)) {
            message(player, Component.translatable("message.mistech.runic_wand.too_far", RANGE).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        boolean wasMarked = (target ? route.targets() : route.sources()).contains(pos);
        GolemRoute next = route.toggle(pos, target);
        if (next == null) {
            message(player, Component.translatable("message.mistech.runic_wand.full", GolemRoute.MAX_POINTS).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        stack.set(MistechComponents.GOLEM_ROUTE.get(), next);
        String key = (target ? "message.mistech.runic_wand.target" : "message.mistech.runic_wand.source") + (wasMarked ? "_removed" : "_added");
        message(player, Component.translatable(key, pos.toShortString(), next.sources().size(), next.targets().size()));
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, wasMarked ? 0.9F : target ? 1.6F : 1.3F);
        return InteractionResult.SUCCESS;
    }

    /** No ar: troca os modos. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            GolemRoute route = routeOf(stack);
            if (player.isShiftKeyDown()) {
                route = route.withCollect(route.collect().next());
                message(player, Component.translatable("message.mistech.runic_wand.collect", Component.translatable(route.collect().key()))
                        .withStyle(ChatFormatting.GREEN));
            } else {
                route = route.withDeliver(route.deliver().next());
                message(player, Component.translatable("message.mistech.runic_wand.deliver", Component.translatable(route.deliver().key()))
                        .withStyle(ChatFormatting.GOLD));
            }
            stack.set(MistechComponents.GOLEM_ROUTE.get(), route);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.5F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof RoutedGolem golem)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            GolemRoute copied = golem.getRoute();
            if (copied.isComplete()) {
                held.set(MistechComponents.GOLEM_ROUTE.get(), copied);
                message(player, Component.translatable("message.mistech.runic_wand.copied", copied.sources().size(), copied.targets().size()));
            } else {
                message(player, golem.describe());
            }
            golem.showRoute((ServerLevel) player.level());
            return InteractionResult.SUCCESS;
        }
        GolemRoute route = routeOf(held);
        if (!route.isComplete()) {
            message(player, Component.translatable("message.mistech.golem_wand.incomplete"));
            return InteractionResult.FAIL;
        }
        golem.setRoute(route);
        golem.showRoute((ServerLevel) player.level());
        entity.playSound(SoundEvents.VILLAGER_YES, 0.6F, 1.6F);
        message(player, Component.translatable("message.mistech.runic_wand.assigned", route.sources().size(), route.targets().size(),
                Component.translatable(route.collect().key()), Component.translatable(route.deliver().key())));
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.sendOverlayMessage(text);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        GolemRoute route = routeOf(stack);
        tooltip.accept(Component.translatable("tooltip.mistech.runic_wand.points", route.sources().size(), route.targets().size(), GolemRoute.MAX_POINTS)
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.mistech.runic_wand.collect", Component.translatable(route.collect().key()))
                .withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.mistech.runic_wand.deliver", Component.translatable(route.deliver().key()))
                .withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.mistech.runic_wand.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
