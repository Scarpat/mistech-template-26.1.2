package com.mistech.mistechmod.drink;

import java.util.function.Consumer;

import com.mistech.mistechmod.purity.Taint;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Cachaça (e variações). Cada gole dá Força + Resistência e soma Náusea; a Náusea acumulada é o "porre":
 * passar de {@link #DRUNK_TICKS} (umas 3 doses seguidas) vira Embriaguez (Náusea forte, Lentidão II, Fadiga).
 */
public class CachacaItem extends Item {
    public static final int NAUSEA_PER_DOSE = 300;
    public static final int DRUNK_TICKS = 700;

    public enum Kind {
        PLAIN(1200, 0, NAUSEA_PER_DOSE), AGED(3600, 0, 200), RUE(1200, 0, NAUSEA_PER_DOSE), HONEY(1200, 0, 240);

        final int duration, amplifier, nausea;

        Kind(int duration, int amplifier, int nausea) {
            this.duration = duration;
            this.amplifier = amplifier;
            this.nausea = nausea;
        }
    }

    public static final float RUE_CLEANSE = 5.0F;

    private final Kind kind;

    public CachacaItem(Kind kind, Item.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            drink(entity, this.kind);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    static void drink(LivingEntity entity, Kind kind) {
        entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, kind.duration, kind.amplifier));
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, kind.duration, kind.amplifier));
        switch (kind) {
            case AGED -> entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            case HONEY -> {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
                entity.removeEffect(MobEffects.POISON);
            }
            case RUE -> {
                if (entity instanceof Player player) {
                    Taint.reduce(player, RUE_CLEANSE, 0.0F);
                }
            }
            default -> { }
        }
        MobEffectInstance old = entity.getEffect(MobEffects.NAUSEA);
        int nausea = (old == null ? 0 : old.getDuration()) + kind.nausea;
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nausea, 0));
        if (nausea > DRUNK_TICKS) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 600, 0));
            if (entity instanceof Player player) {
                player.sendOverlayMessage(Component.translatable("item.mistech.cachaca.drunk").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mistech.cachaca.tip." + this.kind.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.mistech.cachaca.tip.warning").withStyle(ChatFormatting.DARK_GRAY));
    }
}
