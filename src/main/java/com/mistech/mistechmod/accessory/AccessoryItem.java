package com.mistech.mistechmod.accessory;

import java.util.function.Consumer;

import com.mistech.mistechmod.astrology.Constellations;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Anel ou amuleto do Mistech. Vai nos espaços de acessório (tecla J); o efeito vem de {@link Accessory}. */
public class AccessoryItem extends Item {
    private final Accessory accessory;

    public AccessoryItem(Item.Properties properties, Accessory accessory) {
        super(properties);
        this.accessory = accessory;
    }

    public Accessory accessory() {
        return this.accessory;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.mistech.accessory." + this.accessory.id).withStyle(ChatFormatting.GRAY));
        if (this.accessory == Accessory.CONSTELLATION_AMULET) {
            Integer bound = stack.get(MistechComponents.AMULET_CONSTELLATION.get());
            if (bound == null || bound < 0 || bound >= Constellations.ALL.size()) {
                tooltip.accept(Component.translatable("tooltip.mistech.constellation_amulet.unbound").withStyle(ChatFormatting.DARK_PURPLE));
            } else {
                tooltip.accept(Component.translatable("tooltip.mistech.constellation_amulet.bound",
                        Constellations.get(bound).name()).withStyle(ChatFormatting.BLUE));
            }
        }
        if (this.accessory == Accessory.CORRUPTED_CROWN || this.accessory == Accessory.PURIFIED_CROWN) {
            com.mistech.mistechmod.fey.CrownCall.appendTooltip(this.accessory, tooltip);
        }
        boolean crown = this.accessory == Accessory.CORRUPTED_CROWN || this.accessory == Accessory.PURIFIED_CROWN;
        tooltip.accept(Component.translatable(this.accessory.ring ? "tooltip.mistech.accessory.ring"
                : crown ? "tooltip.mistech.accessory.crown" : "tooltip.mistech.accessory.amulet")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
