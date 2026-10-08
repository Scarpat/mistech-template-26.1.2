package com.mistech.mistechmod.ranged;

import java.util.Map;
import java.util.function.Consumer;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Arco Turvo (madeira de Árvore Turva): arco comum mais resistente; as flechas levam um pouco do elemento em que o
 * atirador tem mais afinidade (nível 1 ou mais), com força {@link ArrowMagic#AFFINITY_POWER}.
 */
public class MurkyBowItem extends BowItem {
    public MurkyBowItem(Properties properties) {
        super(properties);
    }

    /** Elemento básico de maior afinidade (null se nenhum chegou ao nível 1). */
    public static RuneElement strongestAffinity(Player player) {
        RuneElement best = null;
        int bestPoints = 0;
        for (Map.Entry<String, Integer> e : Affinity.get(player).points().entrySet()) {
            RuneElement element = RuneElement.byId(e.getKey());
            if (element != null && !element.isAdvanced() && e.getValue() > bestPoints && Affinity.level(e.getValue()) >= 1) {
                best = element;
                bestPoints = e.getValue();
            }
        }
        return best;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack weapon) {
        if (arrow.getOwner() instanceof Player player) {
            RuneElement element = strongestAffinity(player);
            if (element != null) {
                ArrowMagic magic = arrow.getData(RangedContent.ARROW_MAGIC).with(element, ArrowMagic.AFFINITY_POWER);
                arrow.setData(RangedContent.ARROW_MAGIC, magic);
            }
        }
        return super.customArrow(arrow, projectile, weapon);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.murky_bow.desc").withStyle(ChatFormatting.GRAY));
    }
}
