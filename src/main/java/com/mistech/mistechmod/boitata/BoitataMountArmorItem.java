package com.mistech.mistechmod.boitata;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Armadura do Boitatá montaria: botão direito no seu Boitatá veste; tesoura tira. */
public class BoitataMountArmorItem extends Item {
    public enum Kind {
        SCALE(10.0, 3.0), ORICHALCUM(16.0, 5.0);

        public final double armor, toughness;

        Kind(double armor, double toughness) {
            this.armor = armor;
            this.toughness = toughness;
        }
    }

    private final Kind kind;

    public BoitataMountArmorItem(Kind kind, Item.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.boitata_mount_armor.stats", (int) this.kind.armor, (int) this.kind.toughness)
                .withStyle(ChatFormatting.BLUE));
        if (this.kind == Kind.ORICHALCUM) {
            builder.accept(Component.translatable("item.mistech.boitata_mount_armor.orichalcum").withStyle(ChatFormatting.GOLD));
        }
        builder.accept(Component.translatable("item.mistech.boitata_mount_armor.help").withStyle(ChatFormatting.GRAY));
    }
}
