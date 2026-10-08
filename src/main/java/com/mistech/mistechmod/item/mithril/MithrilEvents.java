package com.mistech.mistechmod.item.mithril;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regras do mitril:
 * - sem energia, o item perde todos os atributos (armadura, dano, velocidade de ataque);
 * - armadura completa carregada: +20% de velocidade e 60% de resistência a dano mágico (tag {@code mistech:magic});
 * - Égide Arcana (Charuto de Energia Mágica): -50% de dano mágico.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MithrilEvents {
    public static final Identifier SET_SPEED = Identifier.fromNamespaceAndPath(Mistech.MODID, "mithril_set_speed");
    public static final double SET_SPEED_BONUS = 0.20;
    public static final float SET_MAGIC_MULTIPLIER = 0.4F;
    public static final float WARD_MAGIC_MULTIPLIER = 0.5F;

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private MithrilEvents() {}

    /** As 4 peças de mitril vestidas e todas com energia. */
    public static boolean hasFullSet(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!MithrilGear.isMithril(stack) || !MithrilGear.isCharged(stack) || com.mistech.mistechmod.mage.MageRobes.isRobe(stack)) {
                return false; // as Vestes de Mago têm o próprio bônus de conjunto (Escudo de Mana)
            }
        }
        return true;
    }

    @SubscribeEvent
    static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!MithrilGear.isMithril(stack)) {
            return;
        }
        if (!MithrilGear.isCharged(stack)) {
            event.clearModifiers();
            return;
        }
        RuneSocketEffects.addModifiers(event, stack); // runas engastadas + constelação vinculada
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 10 != 0) {
            return;
        }
        boolean full = hasFullSet(player);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean has = speed.getModifier(SET_SPEED) != null;
            if (full && !has) {
                speed.addTransientModifier(new AttributeModifier(SET_SPEED, SET_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!full && has) {
                speed.removeModifier(SET_SPEED);
            }
        }
        if (full && player.tickCount % 40 == 0 && player.isSprinting()) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.2, player.getZ(), 3, 0.25, 0.1, 0.25, 0.02);
        }
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(MistechTags.MAGIC_DAMAGE)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        float amount = event.getAmount();
        if (hasFullSet(entity)) {
            amount *= SET_MAGIC_MULTIPLIER;
            if (entity.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 8, 0.3, 0.5, 0.3, 0.1);
            }
        }
        if (entity.hasEffect(MistechEffects.ARCANE_WARD)) {
            amount *= WARD_MAGIC_MULTIPLIER;
        }
        event.setAmount(amount);
    }
}
