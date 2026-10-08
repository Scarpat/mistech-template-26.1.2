package com.mistech.mistechmod.ranged;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Arcos revestidos (Mitril → Adamantina → Oricalco → Oricalco Puro). Energia no lugar da durabilidade
 * ({@link MithrilGear#COST_PER_DAMAGE} FE por flecha). Com energia:
 * <ul>
 * <li>puxa mais rápido ({@link #drawSpeed}) e a flecha dá mais dano ({@link #damageBonus});</li>
 * <li>as <b>runas engastadas</b> (Bigorna Rúnica) mudam a flecha — ver {@link RangedEvents}.</li>
 * </ul>
 * Sem energia é um arco comum.
 */
public class GearBowItem extends BowItem implements MithrilGear.Gear {
    private final GearTier tier;

    public GearBowItem(GearTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public GearTier tier() {
        return this.tier;
    }

    /** Multiplicador da velocidade de puxar. */
    public double drawSpeed() {
        return 1.0 + 0.15 * (this.tier.ordinal() + 1);
    }

    /** Dano extra na flecha (a flecha comum tem 2). */
    public double damageBonus() {
        return 0.5 * (this.tier.ordinal() + 1);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (MithrilGear.isCharged(stack) || entity instanceof Player player && player.hasInfiniteMaterials()) {
            int held = this.getUseDuration(stack, entity) - remainingTime;
            int faster = (int) Math.round(held * this.drawSpeed());
            remainingTime = this.getUseDuration(stack, entity) - faster;
        }
        return super.releaseUsing(stack, level, entity, remainingTime);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack weapon) {
        boolean creative = arrow.getOwner() instanceof Player p && p.hasInfiniteMaterials();
        if (MithrilGear.isCharged(weapon) || creative) {
            arrow.setBaseDamage(2.0 + this.damageBonus());
            ArrowMagic magic = arrow.getData(RangedContent.ARROW_MAGIC).withAll(GearRunes.tally(weapon))
                    .sneaking(arrow.getOwner() != null && arrow.getOwner().isShiftKeyDown());
            if (!magic.isEmpty()) {
                arrow.setData(RangedContent.ARROW_MAGIC, magic);
            }
        }
        return super.customArrow(arrow, projectile, weapon);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return MithrilGear.damageItem(stack, amount, entity);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return MithrilGear.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return this.tier.barColor();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.gear_bow.desc", Math.round((this.drawSpeed() - 1.0) * 100), this.damageBonus())
                .withStyle(ChatFormatting.GRAY));
        if (this.tier.runeSlots() > 0) {
            builder.accept(Component.translatable("item.mistech.gear_bow.runes").withStyle(ChatFormatting.DARK_PURPLE));
        }
        MithrilGear.tooltip(stack, builder);
    }
}
