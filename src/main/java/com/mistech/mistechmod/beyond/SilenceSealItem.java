package com.mistech.mistechmod.beyond;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Selo do Silêncio (ritual Tier II, sai 2 por ritual). Botão direito: quebra o selo e cria uma zona de silêncio de
 * {@link SilenceData#RADIUS} blocos por 1 dia (20 min) em volta de onde você está. Dentro dela nenhum jogador gera vibração
 * (sensores, shriekers e o Warden não percebem) e o aviso dos shriekers não acumula.
 */
public class SilenceSealItem extends Item {
    public SilenceSealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            SilenceData.get(server).add(player.blockPosition(), server.getGameTime() + SilenceData.DURATION);
            server.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 0.5, player.getZ(), 60, 3.0, 0.5, 3.0, 0.02);
            server.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 30, 1.0, 0.6, 1.0, 0.02);
            server.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.5F, 0.5F);
            server.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.sendOverlayMessage(Component.translatable("item.mistech.silence_seal.used", SilenceData.RADIUS).withColor(0x29DFEB));
            stack.consume(1, player);
            player.getCooldowns().addCooldown(stack, 40);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("item.mistech.silence_seal.desc", SilenceData.RADIUS).withStyle(ChatFormatting.GRAY));
    }
}
