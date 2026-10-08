package com.mistech.mistechmod.item.ritual;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Adaga Ritual: golpe dá Wither I por 3 s; 25% de chance de Resíduo Mágico ao matar (RitualEvents);
 * agachado + botão direito no Altar Impuro encerra o ritual sem refluxo.
 */
public class RitualDaggerItem extends Item {
    public RitualDaggerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        boolean infernal = stack.is(com.mistech.mistechmod.beyond.PaleNetherContent.INFERNAL_DAGGER.get());
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, infernal ? 80 : 60, infernal ? 1 : 0), attacker);
    }
}
