package com.mistech.mistechmod.purity.item;

import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.purity.block.TaintedSoilBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pêndulo de Ametista (radiestesia): mede a Mácula.
 * No ar = a sua; num bloco = a contaminação do solo; num jogador = a dele. Segurando, o HUD mostra a sua.
 */
public class DowsingPendulumItem extends Item {
    public DowsingPendulumItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().getItem() instanceof DowsingPendulumItem || player.getOffhandItem().getItem() instanceof DowsingPendulumItem;
    }

    private static void swing(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.pendulum.self", Taint.describe(Taint.get(player))));
            swing(level, player);
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 10);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            Component message;
            if (state.is(PurityContent.TAINTED_SOIL.get())) {
                int strength = state.getValue(TaintedSoilBlock.STRENGTH);
                message = Component.translatable("message.mistech.pendulum.soil", strength, TaintedSoilBlock.MAX_STRENGTH)
                        .withStyle(ChatFormatting.DARK_PURPLE);
            } else if (state.is(PurityContent.IMPURE_BLOCKS)) {
                message = Component.translatable("message.mistech.pendulum.impure_block", state.getBlock().getName()).withStyle(ChatFormatting.DARK_PURPLE);
            } else {
                message = Component.translatable("message.mistech.pendulum.clean").withStyle(ChatFormatting.YELLOW);
            }
            player.sendOverlayMessage(message);
            swing(level, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Player other) {
            if (!player.level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.pendulum.other", other.getDisplayName(), Taint.describe(Taint.get(other))));
                swing(player.level(), player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
