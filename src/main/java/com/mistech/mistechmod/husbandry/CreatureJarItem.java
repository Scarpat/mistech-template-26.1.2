package com.mistech.mistechmod.husbandry;

import java.util.function.Consumer;

import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;

/** Item do Pote com Criatura: o nome mostra o bicho ("Pote com Galinha"). */
public class CreatureJarItem extends BlockItem {
    public CreatureJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        TypedEntityData<EntityType<?>> data = stack.get(MistechComponents.JAR_CREATURE.get());
        if (data == null) {
            return super.getName(stack);
        }
        return Component.translatable("block.mistech.creature_jar.with", data.type().getDescription());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        TypedEntityData<EntityType<?>> data = stack.get(MistechComponents.JAR_CREATURE.get());
        if (data == null) {
            tooltip.accept(Component.translatable("block.mistech.creature_jar.empty_hint").withStyle(ChatFormatting.GRAY));
            return;
        }
        JarCreatures.Kind kind = JarCreatures.kindOf(data.type());
        if (kind != null) {
            tooltip.accept(Component.translatable("block.mistech.creature_jar.makes." + kind.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("block.mistech.creature_jar.release_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
