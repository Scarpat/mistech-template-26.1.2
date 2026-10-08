package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechWear;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Bônus das vestes místicas que dependem do tipo de dano. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MysticWearEvents {
    private MysticWearEvents() {}

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        // Roupão Místico: frio (congelamento) machuca metade
        if (event.getSource().is(DamageTypeTags.IS_FREEZING) && target.getItemBySlot(EquipmentSlot.CHEST).is(MistechWear.MYSTIC_ROBE.get())) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        // Calças Magicamente Confortáveis: projéteis machucam 30% menos
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE) && target.getItemBySlot(EquipmentSlot.LEGS).is(MistechWear.COMFY_PANTS.get())) {
            event.setAmount(event.getAmount() * 0.7F);
        }
    }
}
