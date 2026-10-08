package com.mistech.mistechmod.mage;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Bônus das Vestes de Mago (só peças vestidas e com energia; somam por peça, com teto):
 * <ul>
 * <li>custo de energia das magias (Cajado do Gnomo, Cajado do Mago, Besta Mágica) — teto {@link #MAX_COST_CUT};</li>
 * <li>dano mágico causado (as mesmas magias que o Pingente do Arcano conta);</li>
 * <li>afinidade ganha e Mácula recebida — teto {@link #MAX_TAINT_CUT};</li>
 * <li><b>Escudo de Mana</b> (as 4 peças): parte do dano vira gasto de energia ({@link #FE_PER_DAMAGE} FE por ponto).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MageRobes {
    public static final double MAX_COST_CUT = 0.5;
    public static final double MAX_TAINT_CUT = 0.6;
    public static final int FE_PER_DAMAGE = 500;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private MageRobes() {}

    private interface Stat {
        double of(MageRobeTier tier);
    }

    private static double sum(LivingEntity entity, Stat stat) {
        double total = 0.0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof MageRobeItem robe && MithrilGear.isCharged(stack)) {
                total += stat.of(robe.robe());
            }
        }
        return total;
    }

    public static boolean isRobe(ItemStack stack) {
        return stack.getItem() instanceof MageRobeItem;
    }

    /** Custo de energia de uma magia com as vestes. */
    public static int cost(Player player, int amount) {
        double cut = Math.min(MAX_COST_CUT, sum(player, t -> t.costCut));
        return cut <= 0 ? amount : (int) Math.ceil(amount * (1.0 - cut));
    }

    /** Multiplicador do dano mágico causado. */
    public static double damageMultiplier(Player player) {
        return 1.0 + sum(player, t -> t.magicDamage);
    }

    /** Afinidade ganha (arredondamento sorteado, para o +20% valer mesmo em ganhos de 1 ponto). */
    public static int affinity(Player player, int amount) {
        double mult = 1.0 + sum(player, t -> t.affinity);
        if (mult <= 1.0) {
            return amount;
        }
        double value = amount * mult;
        int whole = (int) Math.floor(value);
        return whole + (player.getRandom().nextDouble() < value - whole ? 1 : 0);
    }

    /** Mácula recebida com as vestes. */
    public static float taint(Player player, float amount) {
        double cut = Math.min(MAX_TAINT_CUT, sum(player, t -> t.taintCut));
        return cut <= 0 ? amount : (float) (amount * (1.0 - cut));
    }

    /** Fração do Escudo de Mana (0 sem as 4 peças carregadas). */
    public static double shield(LivingEntity entity) {
        double min = Double.MAX_VALUE;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof MageRobeItem robe) || !MithrilGear.isCharged(stack)) {
                return 0.0;
            }
            min = Math.min(min, robe.robe().shield);
        }
        return min;
    }

    /** Dano mágico causado (+) e Escudo de Mana. Roda depois dos outros bônus. */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onDamage(LivingIncomingDamageEvent event) {
        if (Accessories.isMagicDamage(event.getSource()) && event.getSource().getEntity() instanceof Player attacker) {
            double mult = damageMultiplier(attacker);
            if (mult > 1.0) {
                event.setAmount((float) (event.getAmount() * mult));
            }
        }
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        double fraction = shield(entity);
        if (fraction <= 0 || event.getAmount() <= 0) {
            return;
        }
        float wanted = (float) (event.getAmount() * fraction);
        int need = (int) Math.ceil(wanted * FE_PER_DAMAGE);
        int paid = 0;
        // tira das peças com mais energia primeiro
        for (int round = 0; round < 4 && paid < need; round++) {
            ItemStack best = ItemStack.EMPTY;
            for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (MithrilGear.getEnergy(stack) > MithrilGear.getEnergy(best)) {
                    best = stack;
                }
            }
            if (best.isEmpty() || MithrilGear.getEnergy(best) <= 0) {
                break;
            }
            int take = Math.min(need - paid, MithrilGear.getEnergy(best));
            MithrilGear.setEnergy(best, MithrilGear.getEnergy(best) - take);
            paid += take;
        }
        if (entity instanceof Player p && p.hasInfiniteMaterials()) {
            paid = need;
        }
        float absorbed = Math.min(wanted, paid / (float) FE_PER_DAMAGE);
        if (absorbed <= 0) {
            return;
        }
        event.setAmount(event.getAmount() - absorbed);
        level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 12, 0.4, 0.6, 0.4, 0.4);
        level.sendParticles(ParticleTypes.GLOW, entity.getX(), entity.getY() + 1.0, entity.getZ(), 4, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.5F);
    }
}
