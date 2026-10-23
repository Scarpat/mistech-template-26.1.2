package com.mistech.mistechmod.spellcraft;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Uma magia. Cada uma tem um elemento, um <b>tier</b> (1 a 5) e um custo de mana. Para <b>lançar</b> é preciso ter o
 * nível de afinidade do tier naquele elemento ({@link #requiredLevel()}); aprender (usar o pergaminho) não exige nada.
 * As marcadas como {@code worldOnly} só existem como pergaminhos achados no mundo: a Mesa de Inscrição não copia.
 */
public final class Spell {
    /** Efeito da magia. Devolve false se não funcionou (sem alvo etc.): então não gasta mana nem recarga. */
    @FunctionalInterface
    public interface Effect {
        boolean cast(ServerLevel level, ServerPlayer player);
    }

    /** Item pedido na Mesa de Inscrição. */
    public record Cost(Supplier<Item> item, int count) {}

    /** Nível de afinidade necessário por tier (índice = tier). */
    public static final int[] LEVEL_BY_TIER = {0, 0, 2, 3, 4, 5, 6};
    public static final int MAX_TIER = 6;

    private final String id;
    private final RuneElement element;
    private final int tier;
    private final int mana;
    private final int cooldown;
    private final boolean worldOnly;
    private final List<Cost> costs;
    private final int xpLevels;
    private final Effect effect;
    private int channelTicks;
    private Kind kind = Kind.INSTANT;
    private int castTicks;

    /** Como a magia é lançada: um clique, segurando (contínua), carregando e soltando, ou parado em ritual. */
    public enum Kind {
        INSTANT, CHANNEL, CHARGE, RITUAL
    }

    Spell(String id, RuneElement element, int tier, int mana, int cooldown, boolean worldOnly, List<Cost> costs, int xpLevels, Effect effect) {
        this.id = id;
        this.element = element;
        this.tier = tier;
        this.mana = mana;
        this.cooldown = cooldown;
        this.worldOnly = worldOnly;
        this.costs = costs;
        this.xpLevels = xpLevels;
        this.effect = effect;
    }

    /** Magia contínua: enquanto o botão direito fica pressionado, repete o efeito a cada {@code ticks}, gastando a mana a cada pulso. */
    public Spell channeled(int ticks) {
        this.channelTicks = ticks;
        this.kind = Kind.CHANNEL;
        return this;
    }

    /** Carregada: segure para aumentar o poder (×1 a ×2) até {@code ticks}; solte para lançar. */
    public Spell charged(int ticks) {
        this.kind = Kind.CHARGE;
        this.castTicks = ticks;
        return this;
    }

    /** Ritual: fique parado {@code ticks} (círculo no chão) e a magia é liberada sozinha. */
    public Spell ritual(int ticks) {
        this.kind = Kind.RITUAL;
        this.castTicks = ticks;
        return this;
    }

    public Kind kind() {
        return this.kind;
    }

    /** Tempo de carga máxima (carregada) ou de conjuração (ritual), em ticks. */
    public int castTicks() {
        return this.castTicks;
    }

    /** Intervalo entre pulsos em ticks, ou 0 se a magia é instantânea. */
    public int channelTicks() {
        return this.channelTicks;
    }

    public String id() {
        return this.id;
    }

    public RuneElement element() {
        return this.element;
    }

    public int tier() {
        return this.tier;
    }

    /** Mana base (antes de reduções). */
    public int mana() {
        return this.mana;
    }

    /** Recarga em ticks. */
    public int cooldown() {
        return this.cooldown;
    }

    public boolean worldOnly() {
        return this.worldOnly;
    }

    public List<Cost> costs() {
        return this.costs;
    }

    public int xpLevels() {
        return this.xpLevels;
    }

    Effect effect() {
        return this.effect;
    }

    public int requiredLevel() {
        return LEVEL_BY_TIER[Math.min(this.tier, MAX_TIER)];
    }

    /** O jogador tem a afinidade para lançar? */
    public boolean hasAffinity(Player player) {
        return Affinity.meets(player, this.element, this.requiredLevel());
    }

    /** Nome do que a magia pede de afinidade: o elemento, ou os componentes ("Água + Terra") nos avançados. */
    public net.minecraft.network.chat.Component requirementName() {
        java.util.List<RuneElement> basics = Affinity.basics(this.element);
        if (!this.element.isAdvanced() || basics.size() <= 1) {
            return net.minecraft.network.chat.Component.translatable(this.element.translationKey());
        }
        net.minecraft.network.chat.MutableComponent text = net.minecraft.network.chat.Component.empty();
        for (int i = 0; i < basics.size(); i++) {
            if (i > 0) {
                text.append(" + ");
            }
            text.append(net.minecraft.network.chat.Component.translatable(basics.get(i).translationKey()));
        }
        return text;
    }

    public String nameKey() {
        return "spell.mistech." + this.id;
    }

    public String descKey() {
        return "spell.mistech." + this.id + ".desc";
    }

    /** Pergaminho desta magia. */
    public ItemStack scroll() {
        return SpellScrollItem.of(this);
    }
}
