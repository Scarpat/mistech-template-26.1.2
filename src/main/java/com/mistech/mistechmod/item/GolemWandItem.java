package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import com.mistech.mistechmod.entity.ClayGolemEntity;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Vara de Comando: 1º clique num container/tanque = origem, 2º = destino (reinicia depois).
 * Clique no Golem de Barro passa a rota para ele; agachado no golem mostra a rota dele.
 * Origem e destino a no máximo {@link ClayGolemEntity#MAX_ROUTE} blocos entre si.
 */
public class GolemWandItem extends Item {
    public GolemWandItem(Item.Properties properties) {
        super(properties);
    }

    /** Roda antes do clique do bloco, para blocos que reagem ao clique (Extrator, Compressor...) não "engolirem" a vara. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return this.useOn(context);
    }

    /**
     * Pontos válidos: qualquer bloco com itens ou com líquido (tanques, Extrator de Essência, Compressor de Magia, caldeirão...),
     * ou um bloco encostado numa fonte de líquido (para o golem com Runa da Água tirar de lago/poça).
     */
    public static boolean isRoutePoint(Level level, BlockPos pos) {
        if (level.getCapability(Capabilities.Item.BLOCK, pos, null) != null
                || level.getCapability(Capabilities.Fluid.BLOCK, pos, null) != null) {
            return true;
        }
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getFluidState(at).isSource()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (!isRoutePoint(level, pos)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos source = stack.get(MistechComponents.GOLEM_SOURCE.get());
        BlockPos target = stack.get(MistechComponents.GOLEM_TARGET.get());
        if (source == null || target != null) {
            stack.set(MistechComponents.GOLEM_SOURCE.get(), pos.immutable());
            stack.remove(MistechComponents.GOLEM_TARGET.get());
            message(player, Component.translatable("message.mistech.golem_wand.source", pos.toShortString()));
        } else {
            if (!source.closerThan(pos, ClayGolemEntity.MAX_ROUTE)) {
                message(player, Component.translatable("message.mistech.golem_wand.too_far", ClayGolemEntity.MAX_ROUTE));
                return InteractionResult.FAIL;
            }
            stack.set(MistechComponents.GOLEM_TARGET.get(), pos.immutable());
            message(player, Component.translatable("message.mistech.golem_wand.target", pos.toShortString()));
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof com.mistech.mistechmod.golem.RoutedGolem golem)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            golem.showRoute((ServerLevel) player.level());
            message(player, golem.describe());
            return InteractionResult.SUCCESS;
        }
        BlockPos source = stack.get(MistechComponents.GOLEM_SOURCE.get());
        BlockPos target = stack.get(MistechComponents.GOLEM_TARGET.get());
        if (source == null || target == null) {
            message(player, Component.translatable("message.mistech.golem_wand.incomplete"));
            return InteractionResult.FAIL;
        }
        golem.setRoute(source, target);
        golem.showRoute((ServerLevel) player.level());
        entity.playSound(SoundEvents.VILLAGER_YES, 0.6F, 1.6F);
        message(player, Component.translatable("message.mistech.golem_wand.assigned"));
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.sendOverlayMessage(text);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        BlockPos source = stack.get(MistechComponents.GOLEM_SOURCE.get());
        BlockPos target = stack.get(MistechComponents.GOLEM_TARGET.get());
        tooltip.accept(Component.translatable("tooltip.mistech.golem_wand.source",
                source == null ? "-" : source.toShortString()).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.mistech.golem_wand.target",
                target == null ? "-" : target.toShortString()).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
