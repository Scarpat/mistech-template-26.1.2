package com.mistech.mistechmod.infuser;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.celestial.CelestialContent;
import com.mistech.mistechmod.purity.PurityContent;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Regras do Infusor Arcano (iguais no cliente e no servidor).
 * Nível máximo: Infusor I = até II · Maior = máximo normal · Astral = máximo + 1 (só encantos com mais de 1 nível) e também
 * os encantos de tesouro (Remendo, Passos Gelados...). Maldições nunca.
 * Custo do nível L: 3 + 3L níveis de XP, 5.000 × L FE, L lápis e L frascos de Essência Impura; acima do máximo normal
 * mais 1 Fragmento Estelar e 1 frasco de Essência Purificada.
 */
public final class InfuserRules {
    public static final int SLOT_TARGET = 0, SLOT_LAPIS = 1, SLOT_ESSENCE = 2, SLOT_STAR = 3, SLOT_PURIFIED = 4, SIZE = 5;
    public static final int FE_PER_LEVEL = 5000;

    public record Option(Holder<Enchantment> enchantment, int rawId, int current, int max) {}

    public record Cost(int xp, int energy, int lapis, int essence, int star, int purified) {}

    private InfuserRules() {}

    public static boolean isBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    public static ItemEnchantments current(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static boolean allowed(int tier, Holder<Enchantment> enchantment) {
        if (enchantment.is(EnchantmentTags.CURSE)) {
            return false;
        }
        return tier >= 3 || enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE);
    }

    public static int maxLevel(int tier, Holder<Enchantment> enchantment) {
        int max = enchantment.value().getMaxLevel();
        return switch (tier) {
            case 1 -> Math.min(2, max);
            case 2 -> max;
            default -> max > 1 ? max + 1 : max;
        };
    }

    /** Encantos que este item aceita no Infusor deste nível (inclui os já no máximo, para mostrar). */
    public static List<Option> options(Registry<Enchantment> registry, int tier, ItemStack stack) {
        List<Option> out = new ArrayList<>();
        if (stack.isEmpty() || (!stack.isEnchantable() && !isBook(stack) && !stack.isEnchanted())) {
            return out;
        }
        ItemEnchantments existing = current(stack);
        for (int id = 0; id < registry.size(); id++) {
            Holder<Enchantment> holder = registry.get(id).orElse(null);
            if (holder == null || !allowed(tier, holder)) {
                continue;
            }
            if (!isBook(stack) && !holder.value().canEnchant(stack)) {
                continue;
            }
            boolean compatible = true;
            for (Holder<Enchantment> other : existing.keySet()) {
                if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                out.add(new Option(holder, id, existing.getLevel(holder), maxLevel(tier, holder)));
            }
        }
        return out;
    }

    public static Cost cost(Holder<Enchantment> enchantment, int level) {
        boolean over = level > enchantment.value().getMaxLevel();
        return new Cost(3 + 3 * level, FE_PER_LEVEL * level, level, level, over ? 1 : 0, over ? 1 : 0);
    }

    public static boolean isLapis(ItemStack stack) {
        return stack.is(Items.LAPIS_LAZULI);
    }

    public static boolean isEssence(ItemStack stack) {
        return stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get());
    }

    public static boolean isStar(ItemStack stack) {
        return stack.is(CelestialContent.STAR_FRAGMENT.get());
    }

    public static boolean isPurified(ItemStack stack) {
        return stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
    }

    public static boolean validFor(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_TARGET -> true;
            case SLOT_LAPIS -> isLapis(stack);
            case SLOT_ESSENCE -> isEssence(stack);
            case SLOT_STAR -> isStar(stack);
            case SLOT_PURIFIED -> isPurified(stack);
            default -> false;
        };
    }
}
