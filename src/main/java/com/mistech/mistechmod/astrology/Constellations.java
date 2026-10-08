package com.mistech.mistechmod.astrology;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * As 12 constelações do zodíaco da Mesa de Astrologia, com as estrelas e o desenho (linhas) das constelações reais.
 * Posições projetadas a partir da ascensão reta/declinação reais (norte para cima, leste à esquerda, como nas cartas celestes).
 * A ordem de desbloqueio começa pela estrela mais brilhante e segue as linhas do desenho: cada estrela tem uma
 * "estrela-mãe" (parent) que precisa ser desbloqueada antes. Linhas extras (links) fecham os laços do desenho.
 * Dados gerados por script — as estrelas estão na ordem de desbloqueio.
 *
 * Bônus (PROVISÓRIOS — "decidiremos depois"): o bônus total da constelação é dividido igualmente entre as estrelas.
 */
public final class Constellations {
    public static final int SKY_WIDTH = 3200;
    public static final int SKY_HEIGHT = 2200;

    /** Custo em itens de uma estrela. */
    public record Cost(Supplier<? extends Item> item, int count) {
        public ItemStack stack() {
            return new ItemStack(item.get(), count);
        }
    }

    /**
     * Uma estrela: letra de Bayer (ou número de Flamsteed), nome próprio (pode ser null), posição relativa ao centro
     * da constelação, estrela anterior (-1 = primeira) e tamanho (pelo brilho real).
     */
    public record Star(String key, String properName, float dx, float dy, int parent, float size) {}

    /** Linha extra do desenho (não muda a ordem de desbloqueio). */
    public record Link(int a, int b) {}

    /** Bônus de cada estrela desbloqueada. */
    public record Perk(Holder<Attribute> attribute, AttributeModifier.Operation operation, double perStar, boolean percent) {
        /** Valor formatado para a tela (ex.: "0.5", ou "6" para 6%). */
        public String format(double value) {
            double shown = percent ? value * 100.0 : value;
            if (Math.abs(shown - Math.rint(shown)) < 1.0E-6) {
                return String.valueOf((long) Math.rint(shown));
            }
            String text = String.format(Locale.ROOT, "%.2f", shown);
            return text.endsWith("0") ? text.substring(0, text.length() - 1) : text;
        }
    }

    public record Constellation(String id, float cx, float cy, List<Star> stars, List<Link> links, Perk perk,
                                Supplier<? extends Item> commonItem, Supplier<? extends Item> rareItem) {
        /** O Perk recebido tem o bônus TOTAL; aqui ele vira bônus por estrela. */
        public Constellation {
            perk = new Perk(perk.attribute(), perk.operation(), perk.perStar() / stars.size(), perk.percent());
        }

        public Component name() {
            return Component.translatable("astrology.mistech.constellation." + id);
        }

        public Component perkDescription() {
            return Component.translatable("astrology.mistech.perk." + id);
        }

        public Component starName(int index) {
            Star star = stars.get(index);
            if (star.properName() != null) {
                return Component.literal(star.properName());
            }
            return Component.translatable("astrology.mistech.star", star.key(), name());
        }

        /** 0 na primeira estrela, 1 na última. */
        private double progress(int index) {
            return stars.size() <= 1 ? 0.0 : (double) index / (stars.size() - 1);
        }

        /** Níveis de XP da estrela: ~60 níveis na constelação toda, crescendo ao longo da ordem. */
        public int xpCost(int index) {
            double weight = 1.0 + 2.0 * progress(index);
            return Math.max(1, (int) Math.round(60.0 * weight / (2.0 * stars.size())));
        }

        /** Itens da estrela (a primeira só pede XP). Totais parecidos em todas as constelações. */
        public List<Cost> itemCost(int index) {
            if (index == 0) {
                return List.of();
            }
            double t = progress(index);
            int n = stars.size();
            List<Cost> costs = new ArrayList<>();
            costs.add(new Cost(commonItem, Math.min(64, Math.max(1, (int) Math.round(96.0 * (1.0 + t) / n)))));
            if (t >= 0.4) {
                costs.add(new Cost(rareItem, Math.max(1, (int) Math.round(24.0 * t / n))));
            }
            if (t >= 0.75) {
                costs.add(new Cost(Mistech.IMPURE_MAGIC_SHARD, Math.max(1, (int) Math.round(10.0 * t / n))));
            }
            return costs;
        }
    }

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final AttributeModifier.Operation MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

    private static Star star(String key, String properName, float dx, float dy, int parent, float size) {
        return new Star(key, properName, dx, dy, parent, size);
    }

    private static Link link(int a, int b) {
        return new Link(a, b);
    }

    /** Bônus total da constelação (dividido entre as estrelas). */
    private static Perk perk(Holder<Attribute> attribute, AttributeModifier.Operation op, double total) {
        return new Perk(attribute, op, total, op == MUL);
    }

    private static Perk percentPerk(Holder<Attribute> attribute, double total) {
        return new Perk(attribute, AttributeModifier.Operation.ADD_VALUE, total, true);
    }

    public static final List<Constellation> ALL = List.of(
            new Constellation("aries", 330, 720, List.of(
                    star("α", "Hamal", 37.1F, -2.0F, -1, 1.29F),
                    star("β", "Sheratan", 68.7F, 27.1F, 0, 1.11F),
                    star("41", "Bharani", -71.6F, -43.8F, 0, 0.84F),
                    star("γ", "Mesarthim", 71.6F, 43.8F, 1, 0.76F)),
                    List.of(),
                    perk(Attributes.ATTACK_DAMAGE, ADD, 3.0), () -> Items.IRON_INGOT, () -> Items.BLAZE_POWDER),
            new Constellation("taurus", 820, 340, List.of(
                    star("α", "Aldebaran", -8.5F, 17.4F, -1, 1.61F),
                    star("ζ", "Tianguan", -120.0F, -17.5F, 0, 1.01F),
                    star("θ2", null, 4.6F, 22.3F, 0, 0.9F),
                    star("γ", "Prima Hyadum", 20.7F, 24.1F, 2, 0.83F),
                    star("λ", null, 55.3F, 47.7F, 3, 0.9F),
                    star("δ1", null, 15.0F, 9.7F, 3, 0.8F),
                    star("ξ", null, 115.9F, 68.6F, 4, 0.81F),
                    star("ε", "Ain", 4.7F, -2.7F, 5, 0.87F),
                    star("ο", null, 120.0F, 73.8F, 6, 0.84F),
                    star("β", "Elnath", -99.5F, -73.8F, 7, 1.39F)),
                    List.of(),
                    perk(Attributes.MAX_HEALTH, ADD, 10.0), () -> Items.LEATHER, () -> Items.GOLDEN_APPLE),
            new Constellation("gemini", 1380, 620, List.of(
                    star("β", "Pollux", -98.0F, -43.7F, -1, 1.53F),
                    star("κ", null, -96.1F, -9.2F, 0, 0.84F),
                    star("υ", null, -77.7F, -33.0F, 0, 0.7F),
                    star("δ", "Wasat", -43.4F, 13.8F, 2, 0.87F),
                    star("ι", null, -55.6F, -41.5F, 2, 0.79F),
                    star("λ", null, -38.9F, 65.4F, 3, 0.84F),
                    star("ζ", "Mekbuda", -8.6F, 27.2F, 3, 0.79F),
                    star("τ", null, -24.0F, -64.8F, 4, 0.65F),
                    star("ξ", null, 32.0F, 100.0F, 5, 0.91F),
                    star("γ", "Alhena", 48.4F, 66.8F, 6, 1.32F),
                    star("α", "Castor", -74.8F, -80.3F, 7, 1.4F),
                    star("ε", "Mebsuta", 35.0F, -16.1F, 7, 1.01F),
                    star("θ", null, 15.8F, -100.0F, 7, 0.84F),
                    star("μ", "Tejat", 80.4F, 8.7F, 11, 1.04F),
                    star("η", "Propus", 98.0F, 8.7F, 13, 0.93F)),
                    List.of(),
                    perk(Attributes.MOVEMENT_SPEED, MUL, 0.20), () -> Items.SUGAR, () -> Items.RABBIT_FOOT),
            new Constellation("cancer", 1900, 290, List.of(
                    star("β", "Tarf", 51.1F, 100.0F, -1, 0.87F),
                    star("δ", "Asellus Australis", -17.5F, 8.4F, 0, 0.76F),
                    star("α", "Acubens", -51.1F, 72.7F, 1, 0.66F),
                    star("γ", "Asellus Borealis", -14.0F, -25.5F, 1, 0.65F),
                    star("ι", null, -22.3F, -100.0F, 3, 0.73F)),
                    List.of(),
                    perk(Attributes.ARMOR, ADD, 5.0), () -> Items.PRISMARINE_SHARD, () -> Items.NAUTILUS_SHELL),
            new Constellation("leo", 2480, 470, List.of(
                    star("α", "Regulus", 76.1F, 57.9F, -1, 1.47F),
                    star("θ", "Chertan", -52.1F, 29.4F, 0, 0.93F),
                    star("η", null, 78.2F, 18.4F, 0, 0.87F),
                    star("β", "Denebola", -120.0F, 36.4F, 1, 1.26F),
                    star("δ", "Zosma", -51.9F, -12.6F, 1, 1.14F),
                    star("γ", "Algieba", 53.5F, -7.0F, 2, 1.29F),
                    star("ζ", "Adhafera", 59.9F, -36.5F, 5, 0.9F),
                    star("μ", "Rasalas", 106.6F, -57.9F, 6, 0.76F),
                    star("ε", null, 120.0F, -39.4F, 7, 1.01F)),
                    List.of(link(5, 4), link(4, 3)),
                    perk(Attributes.ATTACK_SPEED, MUL, 0.30), () -> Items.GOLD_INGOT, () -> Items.BLAZE_ROD),
            new Constellation("virgo", 2900, 960, List.of(
                    star("α", "Spica", -12.2F, 58.9F, -1, 1.58F),
                    star("ζ", "Heze", -24.8F, 2.7F, 0, 0.9F),
                    star("ι", null, -79.8F, 31.4F, 0, 0.7F),
                    star("θ", null, 8.1F, 29.0F, 0, 0.65F),
                    star("δ", "Minelauva", 27.1F, -18.6F, 1, 0.9F),
                    star("109", null, -120.0F, -10.6F, 1, 0.81F),
                    star("μ", null, -115.8F, 29.6F, 2, 0.76F),
                    star("γ", "Porrima", 45.7F, 7.2F, 3, 1.08F),
                    star("ε", "Vindemiatrix", 18.4F, -58.9F, 4, 1.07F),
                    star("η", "Zaniah", 74.7F, 3.0F, 7, 0.76F),
                    star("β", "Zavijava", 113.5F, -9.9F, 9, 0.84F),
                    star("ν", null, 120.0F, -35.3F, 10, 0.73F)),
                    List.of(link(7, 4)),
                    perk(Attributes.LUCK, ADD, 3.0), () -> Items.WHEAT, () -> Items.EMERALD),
            new Constellation("libra", 2640, 1480, List.of(
                    star("β", "Zubeneschamali", -5.1F, -100.0F, -1, 1.12F),
                    star("α", "Zubenelgenubi", 54.8F, -34.7F, 0, 1.08F),
                    star("γ", "Zubenelakrab", -47.6F, -47.0F, 0, 0.76F),
                    star("σ", "Brachium", 24.5F, 55.9F, 1, 0.93F),
                    star("υ", null, -51.1F, 83.9F, 2, 0.84F),
                    star("τ", null, -54.8F, 100.0F, 4, 0.83F)),
                    List.of(link(2, 1)),
                    percentPerk(Attributes.KNOCKBACK_RESISTANCE, 0.30), () -> Items.QUARTZ, () -> Items.IRON_BLOCK),
            new Constellation("scorpius", 2120, 1830, List.of(
                    star("α", "Antares", 40.7F, -43.5F, -1, 1.56F),
                    star("τ", "Paikauhale", 29.2F, -28.2F, 0, 1.07F),
                    star("σ", "Alniyat", 55.4F, -50.7F, 0, 1.04F),
                    star("ε", "Larawag", 3.7F, 23.6F, 1, 1.21F),
                    star("δ", "Dschubba", 92.5F, -76.0F, 2, 1.21F),
                    star("μ1", null, 0.7F, 55.7F, 3, 1.01F),
                    star("β", "Acrab", 83.4F, -100.0F, 4, 1.12F),
                    star("π", "Fang", 95.2F, -46.2F, 4, 1.04F),
                    star("ζ2", null, -4.2F, 92.5F, 5, 0.84F),
                    star("ρ", null, 98.7F, -19.8F, 7, 0.76F),
                    star("η", null, -35.6F, 100.0F, 8, 0.93F),
                    star("θ", "Sargas", -80.4F, 98.0F, 10, 1.33F),
                    star("ι1", null, -98.7F, 73.5F, 11, 1.01F),
                    star("κ", null, -89.6F, 64.1F, 12, 1.18F),
                    star("λ", "Shaula", -73.8F, 47.6F, 13, 1.4F),
                    star("υ", "Lesath", -68.8F, 49.3F, 14, 1.09F)),
                    List.of(),
                    perk(Attributes.ARMOR_TOUGHNESS, ADD, 4.0), () -> Items.SPIDER_EYE, () -> Items.FERMENTED_SPIDER_EYE),
            new Constellation("sagittarius", 1540, 1460, List.of(
                    star("ε", "Kaus Australis", 33.0F, 60.4F, -1, 1.33F),
                    star("ζ", "Ascella", -60.1F, 10.9F, 0, 1.12F),
                    star("δ", "Kaus Media", 40.7F, 10.3F, 0, 1.09F),
                    star("γ", "Alnasl", 77.5F, 16.8F, 0, 1.01F),
                    star("η", null, 48.9F, 86.6F, 0, 0.98F),
                    star("φ", null, -19.1F, -20.9F, 1, 0.95F),
                    star("τ", null, -70.7F, -13.4F, 1, 0.93F),
                    star("λ", "Kaus Borealis", 23.9F, -38.2F, 2, 1.07F),
                    star("σ", "Nunki", -42.3F, -28.5F, 5, 1.28F),
                    star("μ", null, 58.3F, -86.1F, 7, 0.79F),
                    star("π", "Albaldah", -77.5F, -86.6F, 8, 1.04F)),
                    List.of(link(2, 5), link(3, 2), link(7, 5), link(8, 6)),
                    perk(Attributes.BLOCK_INTERACTION_RANGE, ADD, 3.0), () -> Items.ARROW, () -> Items.ENDER_PEARL),
            new Constellation("capricornus", 930, 1860, List.of(
                    star("δ", "Deneb Algedi", -115.9F, -39.6F, -1, 1.05F),
                    star("γ", "Nashira", -97.8F, -33.8F, 0, 0.81F),
                    star("ε", null, -90.1F, -2.9F, 0, 0.65F),
                    star("ι", null, -51.4F, -31.9F, 1, 0.65F),
                    star("ζ", null, -62.8F, 29.5F, 2, 0.8F),
                    star("θ", null, -8.9F, -27.5F, 3, 0.7F),
                    star("ω", null, 27.8F, 79.1F, 4, 0.7F),
                    star("β", "Dabih", 108.1F, -54.5F, 5, 1.0F),
                    star("ψ", null, 42.8F, 60.9F, 6, 0.7F),
                    star("α2", "Algedi", 115.9F, -79.1F, 7, 0.84F)),
                    List.of(link(7, 8)),
                    perk(Attributes.BLOCK_BREAK_SPEED, MUL, 0.40), () -> Items.COAL, () -> Items.DIAMOND),
            new Constellation("aquarius", 400, 1500, List.of(
                    star("β", "Sadalsuud", 52.2F, -27.0F, -1, 1.04F),
                    star("α", "Sadalmelik", -0.7F, -59.8F, 0, 1.02F),
                    star("ε", "Albali", 120.0F, -2.5F, 0, 0.79F),
                    star("ι", null, -1.7F, 24.8F, 0, 0.65F),
                    star("γ", "Sadachbia", -25.2F, -53.1F, 1, 0.77F),
                    star("θ", null, -17.8F, -13.2F, 1, 0.69F),
                    star("π", null, -30.9F, -70.4F, 1, 0.65F),
                    star("ζ", null, -36.4F, -61.7F, 4, 0.83F),
                    star("λ", null, -73.1F, -14.5F, 5, 0.81F),
                    star("η", null, -46.4F, -61.1F, 7, 0.73F),
                    star("τ", null, -68.3F, 23.1F, 8, 0.73F),
                    star("φ", null, -106.6F, -24.0F, 8, 0.67F),
                    star("δ", "Skat", -76.2F, 37.0F, 10, 0.93F),
                    star("ψ1", null, -109.1F, -5.0F, 11, 0.67F),
                    star("98", null, -120.0F, 63.7F, 13, 0.74F),
                    star("88", null, -99.0F, 70.4F, 14, 0.83F)),
                    List.of(link(7, 6)),
                    perk(Attributes.OXYGEN_BONUS, ADD, 4.0), () -> Items.PRISMARINE_CRYSTALS, () -> Items.GLOW_INK_SAC),
            new Constellation("pisces", 1080, 1100, List.of(
                    star("η", "Alpherg", -75.5F, 1.9F, -1, 0.84F),
                    star("ο", null, -95.8F, 38.5F, 0, 0.66F),
                    star("φ", null, -49.7F, -52.6F, 0, 0.65F),
                    star("α", "Alrescha", -120.0F, 76.3F, 1, 0.79F),
                    star("τ", null, -46.6F, -85.2F, 2, 0.65F),
                    star("υ", null, -58.1F, -68.5F, 2, 0.65F),
                    star("ν", null, -90.0F, 60.2F, 3, 0.65F),
                    star("μ", null, -73.6F, 56.4F, 6, 0.65F),
                    star("ζ", null, -49.7F, 47.9F, 7, 0.65F),
                    star("ε", null, -34.0F, 46.0F, 8, 0.65F),
                    star("δ", null, -13.2F, 47.8F, 9, 0.65F),
                    star("ω", null, 58.7F, 52.1F, 10, 0.73F),
                    star("ι", null, 86.8F, 59.4F, 11, 0.7F),
                    star("θ", null, 104.3F, 54.9F, 12, 0.65F),
                    star("λ", null, 83.8F, 82.1F, 12, 0.65F),
                    star("γ", null, 120.0F, 73.3F, 13, 0.81F),
                    star("κ", null, 105.8F, 85.2F, 14, 0.65F)),
                    List.of(link(16, 15), link(5, 4)),
                    percentPerk(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.60), () -> Items.COD, () -> Items.TROPICAL_FISH)
    );

    private Constellations() {}

    public static Constellation get(int index) {
        return ALL.get(index);
    }

    public static boolean isValid(int constellation, int star) {
        return constellation >= 0 && constellation < ALL.size() && star >= 0 && star < ALL.get(constellation).stars().size();
    }
}
