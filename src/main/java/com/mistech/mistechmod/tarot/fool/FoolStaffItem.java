package com.mistech.mistechmod.tarot.fool;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cajado do Louco (Forja do Louco). Arma leve (7 de dano, ataque rápido, empurra muito).
 * <ul>
 * <li><b>Botão direito</b>: lança um tornado que vai reto, ergue e empurra as criaturas que toca (não afeta jogadores).</li>
 * <li><b>Agachado + botão direito</b>: <b>Rajada</b> — impulso na direção do olhar, sem dano de queda até pousar.</li>
 * </ul>
 * Cada uso gasta 1 de durabilidade; conserta com Vara de Blaze do Vento (breeze rod).
 */
public class FoolStaffItem extends Item {
    public static final int TORNADO_COOLDOWN = 40;
    public static final int DASH_COOLDOWN = 50;
    public static final double DASH_SPEED = 1.6;

    public FoolStaffItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * DASH_SPEED, Math.max(0.35, look.y * DASH_SPEED + 0.2), look.z * DASH_SPEED);
            player.hurtMarked = true;
            player.resetFallDistance();
            if (player instanceof ServerPlayer serverPlayer) {
                FoolEvents.grantSafeLanding(serverPlayer);
            }
            server.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            server.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
            player.getCooldowns().addCooldown(stack, DASH_COOLDOWN);
        } else {
            server.addFreshEntity(FoolTornadoEntity.fromPlayer(server, player));
            server.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.getCooldowns().addCooldown(stack, TORNADO_COOLDOWN);
        }
        server.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.4F, 1.8F);
        stack.hurtAndBreak(1, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.fool_staff.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("item.mistech.fool_staff.tornado").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("item.mistech.fool_staff.dash").withStyle(ChatFormatting.AQUA));
    }
}
