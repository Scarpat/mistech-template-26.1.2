package com.mistech.mistechmod.item.mithril;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ToolMaterial;

/** Item revestido de mitril (energia no lugar da durabilidade, ver {@link MithrilGear}). */
public class MithrilAxeItem extends AxeItem implements MithrilGear.Gear {
    private final GearTier tier;

    public MithrilAxeItem(ToolMaterial material, float damage, float speed, Item.Properties properties) {
        this(GearTier.MITHRIL, material, damage, speed, properties);
    }

    public MithrilAxeItem(GearTier tier, ToolMaterial material, float damage, float speed, Item.Properties properties) {
        super(material, damage, speed, properties);
        this.tier = tier;
    }

    @Override
    public GearTier tier() {
        return this.tier;
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
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return MithrilGear.isCharged(stack) ? super.getDestroySpeed(stack, state) : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return MithrilGear.isCharged(stack) && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        MithrilGear.tooltip(stack, builder);
    }
}
