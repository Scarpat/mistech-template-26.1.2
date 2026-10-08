package com.mistech.mistechmod.item.rune;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Runa de Fogo: funciona exatamente como um isqueiro (põe fogo no chão, acende fogueiras/velas...),
 * gastando 1 uso por vez. 16 usos. Cada uso dá 1 ponto de afinidade com o Fogo.
 */
public class FireRuneItem extends FlintAndSteelItem {
    public FireRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (result.consumesAction() && !context.getLevel().isClientSide()) {
            Affinity.grant(context.getPlayer(), RuneElement.FIRE, 1);
        }
        return result;
    }
}
