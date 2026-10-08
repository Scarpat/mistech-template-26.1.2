package com.mistech.mistechmod.item;

import java.util.List;
import java.util.function.Consumer;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.staff.GnomeStaffItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Cachimbo Rúnico (ritual Tier II): junta o Cachimbo com o Cajado do Gnomo.
 * - Carrega folhas e acende igual ao Cachimbo comum.
 * - Runa na outra mão + botão direito: o cachimbo aprende a runa (cada elemento uma vez; a runa é gasta).
 *   Usa os mesmos componentes do cajado (mistech:staff_runes / staff_selected), e o orbe do fornilho fica da cor da runa.
 * - Shift + roda do mouse troca a runa escolhida (posição 0 = sem runa: tragada normal de charuto/baseado).
 * - Com runa escolhida, cada tragada dá os efeitos do Charuto Mágico (tabaco) ou do Baseado Mágico (cânhamo)
 *   daquela runa por 90 s, e 1 ponto de afinidade com o elemento.
 */
public class RunicPipeItem extends PipeItem {
    public RunicPipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        RuneElement rune = RuneElement.ofRune(other);
        if (rune != null) {
            return GnomeStaffItem.learnRune(level, player, stack, other, rune, "item.mistech.runic_pipe.added");
        }
        return super.use(level, player, hand);
    }

    @Override
    protected void applyHerbEffects(ItemStack stack, String herb, LivingEntity entity) {
        RuneElement selected = GnomeStaffItem.getSelected(stack);
        if (selected == null) {
            super.applyHerbEffects(stack, herb, entity);
            return;
        }
        boolean tobacco = TOBACCO.equals(herb);
        for (RuneElement.Effect effect : selected.effects(tobacco)) {
            entity.addEffect(new MobEffectInstance(effect.effect(), RuneElement.DURATION_TICKS, effect.amplifier(), false, true, true));
        }
        if (entity instanceof Player player) {
            Affinity.grant(player, selected, 1);
            pipeBonus(selected, tobacco, player);
        }
    }

    public static final double LUCK_SHARE_RADIUS = 8.0;
    public static final float LUCK_EMERALD_CHANCE = 0.08F;
    public static final int ARCANE_PUFF_CHARGE = 2_000;
    public static final double ECHO_PULSE_RADIUS = 24.0;

    /**
     * Efeito extra que só o Cachimbo Rúnico tem com as runas complexas:
     * - Sorte: a fumaça se espalha — jogadores a até 8 blocos ganham os mesmos efeitos; 8% de chance de "achar" uma esmeralda.
     * - Energia Mágica: cada tragada carrega 2.000 FE nos itens com energia do inventário (mitril, cajado...).
     * - Eco: cada tragada solta um pulso de sonar — criaturas a até 24 blocos ganham contorno por 5 s (só para quem fuma).
     * - Nuvens: tragada caindo deixa uma plataforma de nuvem 3×3 embaixo dos pés (some em 30 s).
     */
    private static void pipeBonus(RuneElement selected, boolean tobacco, Player player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        if (selected == RuneElement.LUCK) {
            for (Player other : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(LUCK_SHARE_RADIUS),
                    p -> p != player && p.isAlive() && p.distanceTo(player) <= LUCK_SHARE_RADIUS)) {
                for (RuneElement.Effect effect : selected.effects(tobacco)) {
                    other.addEffect(new MobEffectInstance(effect.effect(), RuneElement.DURATION_TICKS, effect.amplifier(), false, true, true));
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, other.getX(), other.getY() + 1.6, other.getZ(),
                        5, 0.3, 0.2, 0.3, 0.0);
            }
            if (player.getRandom().nextFloat() < LUCK_EMERALD_CHANCE) {
                player.getInventory().placeItemBackInInventory(new ItemStack(net.minecraft.world.item.Items.EMERALD));
                player.sendOverlayMessage(Component.translatable("item.mistech.runic_pipe.lucky_find").withColor(selected.color()));
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.4F);
            }
        } else if (selected == RuneElement.ECHO && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // pulso de sonar: criaturas a até 24 blocos ganham contorno por 5 s, só para quem fuma
            int found = 0;
            for (net.minecraft.world.entity.LivingEntity living : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    player.getBoundingBox().inflate(ECHO_PULSE_RADIUS), e -> e != player && e.isAlive() && e.distanceTo(player) <= ECHO_PULSE_RADIUS)) {
                com.mistech.mistechmod.beyond.EchoSense.reveal(serverPlayer, living, 100);
                found++;
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.2, player.getZ(),
                    10, 0.5, 0.3, 0.5, 0.03);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SCULK_CLICKING,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
            if (found > 0) {
                player.sendOverlayMessage(Component.translatable("item.mistech.runic_pipe.echo_pulse", found).withColor(selected.color()));
            }
        } else if (selected == RuneElement.CLOUD) {
            if (!player.onGround() && player.getDeltaMovement().y < -0.3 && !player.getAbilities().flying) {
                com.mistech.mistechmod.sky.Clouds.platform(level,
                        net.minecraft.core.BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ()), 1, player);
            }
        } else if (selected == RuneElement.LIFE) {
            // tragada da Vida: cura 2 de quem fuma e dos aliados colados (2 blocos)
            for (net.minecraft.world.entity.LivingEntity living : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    player.getBoundingBox().inflate(2.0), e -> com.mistech.mistechmod.life.LifeRuneItem.isAlly(player, e))) {
                com.mistech.mistechmod.life.LifeRuneItem.mend(level, living, 2.0F, 0);
            }
        } else if (selected == RuneElement.ARCANE) {
            int moved = com.mistech.mistechmod.item.mithril.MithrilGear.chargeInventory(player, ARCANE_PUFF_CHARGE);
            if (moved > 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                        10, 0.4, 0.6, 0.4, 0.05);
                player.sendOverlayMessage(Component.translatable("item.mistech.arcane_rune.charged", moved).withColor(selected.color()));
            }
        }
    }

    @Override
    public int getBarColor(ItemStack stack) {
        RuneElement selected = GnomeStaffItem.getSelected(stack);
        return selected == null ? super.getBarColor(stack) : selected.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(GnomeStaffItem.selectedName(stack).copy().withStyle(ChatFormatting.GRAY));
        List<RuneElement> runes = GnomeStaffItem.getRunes(stack);
        if (runes.isEmpty()) {
            builder.accept(Component.translatable("item.mistech.gnome_staff.no_runes").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (RuneElement rune : runes) {
                builder.accept(Component.literal(" • ").append(Component.translatable(rune.translationKey())).withColor(rune.color()));
            }
        }
        builder.accept(Component.translatable("item.mistech.runic_pipe.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
