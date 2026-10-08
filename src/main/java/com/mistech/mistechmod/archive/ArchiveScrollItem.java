package com.mistech.mistechmod.archive;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Pergaminho Espacial: célula de armazenamento do Arquivo (vai no Núcleo). O conteúdo fica em {@link ArchiveCells}. */
public class ArchiveScrollItem extends Item {
    private final ScrollTier tier;

    public ArchiveScrollItem(Properties properties, ScrollTier tier) {
        super(properties);
        this.tier = tier;
    }

    public ScrollTier tier() {
        return this.tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        ArchiveContent.Usage usage = stack.get(ArchiveContent.CELL_USAGE.get());
        int types = usage == null ? 0 : usage.types();
        long items = usage == null ? 0 : usage.items();
        builder.accept(Component.translatable("item.mistech.archive_scroll.types", types, this.tier.types).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (this.tier == ScrollTier.VOID) {
            builder.accept(Component.translatable("item.mistech.archive_scroll.items_void", items).withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            builder.accept(Component.translatable("item.mistech.archive_scroll.items", items, this.tier.capacity).withStyle(ChatFormatting.AQUA));
        }
        builder.accept(Component.translatable("item.mistech.archive_scroll.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
