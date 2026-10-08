package com.mistech.mistechmod.tarot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.celestial.CelestialContent;
import com.mistech.mistechmod.purity.PurityContent;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Os 22 Arcanos Maiores.
 * <ul>
 * <li><b>Invocação</b>: os itens de {@link #invocation()} (1 em cada Pedestal Ritual do Círculo de Tarologia, sem sobrar nada),
 * mais {@link #xpCost()} níveis e 1 Carta de Tarô em Branco do inventário → Carta Invertida do arcano.</li>
 * <li>Usar a Carta Invertida abre a Fenda Arcana → luta no Plano Arcano → vencendo, Carta do Arcano + arcano "derrotado".</li>
 * <li><b>Forja</b>: Carta do Arcano (inventário) + materiais da opção nos pedestais → uma das duas {@link #options()}.</li>
 * <li>Derrotado, o arcano pode ser vinculado ao equipamento de Oricalco Puro na Mesa de Tarologia.</li>
 * </ul>
 * Itens, custos e opções são PROVISÓRIOS; as opções de equipamento entram com {@link #setOptions} quando existirem.
 */
public enum TarotArcana {
    THE_FOOL("the_fool", 0, 0xF2D16B,
            () -> Mistech.AIR_RUNE.get(), () -> Items.FEATHER, () -> Items.OXEYE_DAISY, () -> Items.RABBIT_FOOT),
    THE_MAGICIAN("the_magician", 1, 0xD9493C,
            () -> Mistech.ARCANE_RUNE.get(), () -> Items.BLAZE_ROD, () -> Items.LAPIS_LAZULI, () -> Items.BOOK,
            () -> Mistech.IMPURE_MAGIC_SHARD.get()),
    THE_HIGH_PRIESTESS("the_high_priestess", 2, 0x4F6FD1,
            () -> Mistech.DARKNESS_RUNE.get(), () -> Mistech.WATER_RUNE.get(), () -> Items.BOOK, () -> Items.ENDER_PEARL),
    THE_EMPRESS("the_empress", 3, 0x5DBB63,
            () -> Mistech.EARTH_RUNE.get(), () -> Items.WHEAT, () -> Items.ROSE_BUSH, () -> Items.HONEY_BOTTLE, () -> Items.EMERALD),
    THE_EMPEROR("the_emperor", 4, 0xC4402F,
            () -> Mistech.FIRE_RUNE.get(), () -> Items.GOLDEN_HELMET, () -> Items.IRON_SWORD, () -> Items.GOLD_INGOT,
            () -> Items.IRON_BLOCK),
    THE_HIEROPHANT("the_hierophant", 5, 0xE0B85A,
            () -> Mistech.LIGHT_RUNE.get(), () -> Items.BELL, () -> Items.BOOK, () -> Items.CANDLE, () -> Items.GOLD_INGOT),
    THE_LOVERS("the_lovers", 6, 0xF07AA8,
            () -> Mistech.AIR_RUNE.get(), () -> Items.POPPY, () -> Items.PINK_TULIP, () -> Items.HONEY_BOTTLE,
            () -> Items.AMETHYST_SHARD),
    THE_CHARIOT("the_chariot", 7, 0x6E8FB3,
            () -> Mistech.ENERGY_RUNE.get(), () -> Items.SADDLE, () -> Items.MINECART, () -> Items.IRON_INGOT,
            () -> Items.GOLDEN_CARROT),
    STRENGTH("strength", 8, 0xE8913A,
            () -> Mistech.FIRE_RUNE.get(), () -> Items.GOLDEN_APPLE, () -> Items.LEATHER, () -> Items.IRON_BLOCK),
    THE_HERMIT("the_hermit", 9, 0x8C8C9E,
            () -> Mistech.LIGHT_RUNE.get(), () -> Items.LANTERN, () -> Items.BOOK, () -> Items.COBWEB),
    WHEEL_OF_FORTUNE("wheel_of_fortune", 10, 0x9B5FD6,
            () -> Mistech.LUCK_RUNE.get(), () -> Items.CLOCK, () -> Items.COMPASS, () -> Items.EMERALD, () -> Items.RABBIT_FOOT),
    JUSTICE("justice", 11, 0xD8D8E8,
            () -> Mistech.LIGHT_RUNE.get(), () -> Items.IRON_SWORD, () -> Items.GOLD_INGOT, () -> Items.GOLD_INGOT,
            () -> Items.BOOK),
    THE_HANGED_MAN("the_hanged_man", 12, 0x3FA6A0,
            () -> Mistech.TIME_RUNE.get(), () -> Items.LEAD, () -> Items.IRON_CHAIN, () -> Items.STRING),
    DEATH("death", 13, 0x3A3A44,
            () -> Mistech.DARKNESS_RUNE.get(), () -> Items.WITHER_ROSE, () -> Items.SKELETON_SKULL, () -> Items.BONE_BLOCK,
            () -> Items.ROTTEN_FLESH),
    TEMPERANCE("temperance", 14, 0x7FC8E8,
            () -> Mistech.WATER_RUNE.get(), () -> Mistech.FIRE_RUNE.get(), () -> Mistech.VAPOR_RUNE.get(),
            () -> Items.GLASS_BOTTLE, () -> PurityContent.PURE_MAGIC_SHARD.get()),
    THE_DEVIL("the_devil", 15, 0x8E1F2A,
            () -> Mistech.MAGMA_RUNE.get(), () -> Items.IRON_CHAIN, () -> Items.NETHER_WART, () -> Items.GOAT_HORN,
            () -> Items.CRYING_OBSIDIAN, () -> Mistech.IMPURE_MAGIC_SHARD.get()),
    THE_TOWER("the_tower", 16, 0xB7A27A,
            () -> Mistech.ENERGY_RUNE.get(), () -> Mistech.BOTTLED_LIGHTNING_ITEM.get(), () -> Items.LIGHTNING_ROD,
            () -> Items.TNT, () -> Items.STONE_BRICKS),
    THE_STAR("the_star", 17, 0x9FD8FF,
            () -> CelestialContent.STAR_FRAGMENT.get(), () -> Mistech.WATER_RUNE.get(), () -> Items.GLOWSTONE_DUST,
            () -> Items.PRISMARINE_CRYSTALS),
    THE_MOON("the_moon", 18, 0xB9B6E8,
            () -> CelestialContent.LUNAR_BLOOD.get(), () -> Mistech.DARKNESS_RUNE.get(), () -> Items.BONE,
            () -> Items.PRISMARINE_SHARD),
    THE_SUN("the_sun", 19, 0xFFC93C,
            () -> Mistech.LIGHT_RUNE.get(), () -> Mistech.FIRE_RUNE.get(), () -> Items.SUNFLOWER, () -> Items.GOLD_BLOCK,
            () -> Items.GLOWSTONE),
    JUDGEMENT("judgement", 20, 0xE86A5A,
            () -> Mistech.TIME_RUNE.get(), () -> Items.GOAT_HORN, () -> Items.ECHO_SHARD, () -> Items.BONE_BLOCK,
            () -> Mistech.LIGHT_RUNE.get()),
    THE_WORLD("the_world", 21, 0x4FC48A,
            () -> Mistech.EARTH_RUNE.get(), () -> Mistech.WATER_RUNE.get(), () -> Mistech.FIRE_RUNE.get(),
            () -> Mistech.AIR_RUNE.get(), () -> Mistech.SPACE_RUNE.get(), () -> Items.HEART_OF_THE_SEA, () -> Items.ENDER_EYE,
            () -> Items.DIAMOND);

    /** Cada arcano tem até 2 opções de equipamento na aba Forja. */
    public static final int OPTION_COUNT = 2;

    private static final String[] NUMERALS = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII",
            "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX", "XXI"};

    /**
     * Uma opção da Forja: o que sai e os materiais (1 de cada nos pedestais; a Carta do Arcano vem do inventário).
     * @param id nome curto (tradução {@code tarot.mistech.option.<arcano>.<id>})
     */
    public record Option(String id, Supplier<ItemStack> result, List<Supplier<Item>> materials) {}

    private final String id;
    private final int number;
    private final int color;
    private final List<Supplier<Item>> invocation;
    private final List<@Nullable Option> options = new ArrayList<>();

    @SafeVarargs
    TarotArcana(String id, int number, int color, Supplier<Item>... invocation) {
        this.id = id;
        this.number = number;
        this.color = color;
        this.invocation = List.of(invocation);
        for (int i = 0; i < OPTION_COUNT; i++) {
            this.options.add(null);
        }
    }

    public String id() {
        return this.id;
    }

    public int number() {
        return this.number;
    }

    public String numeral() {
        return NUMERALS[this.number];
    }

    /** Cor do arcano (RGB) — carta, tela, partículas e barra do chefe. */
    public int color() {
        return this.color;
    }

    /** Itens (1 por pedestal) para a Carta Invertida. No máximo {@link TarotField#PEDESTALS} itens. */
    public List<Supplier<Item>> invocation() {
        return this.invocation;
    }

    /** Níveis de XP da invocação (provisório: 10 + número do arcano). */
    public int xpCost() {
        return 10 + this.number;
    }

    /** As duas opções da Forja; {@code null} = ainda não existe ("em breve"). */
    public @Nullable Option option(int index) {
        return index >= 0 && index < OPTION_COUNT ? this.options.get(index) : null;
    }

    /** Define as opções de equipamento (chamar durante o carregamento, quando os itens existirem). */
    public void setOptions(@Nullable Option first, @Nullable Option second) {
        this.options.set(0, first);
        this.options.set(1, second);
    }

    public Component displayName() {
        return Component.translatable("tarot.mistech.arcana." + this.id);
    }

    /** "XIII · A Morte" */
    public Component fullName() {
        return Component.literal(this.numeral() + " · ").append(this.displayName());
    }

    public Component description() {
        return Component.translatable("tarot.mistech.arcana." + this.id + ".desc");
    }

    public Component optionName(int index) {
        Option option = this.option(index);
        return option == null ? Component.translatable("screen.mistech.tarot.option_soon")
                : Component.translatable("tarot.mistech.option." + this.id + "." + option.id());
    }

    public static TarotArcana byIndex(int index) {
        TarotArcana[] values = values();
        return values[Math.floorMod(index, values.length)];
    }

    public static @Nullable TarotArcana byId(String id) {
        for (TarotArcana arcana : values()) {
            if (arcana.id.equals(id)) {
                return arcana;
            }
        }
        return null;
    }

    public static boolean isValid(int index) {
        return index >= 0 && index < values().length;
    }
}
