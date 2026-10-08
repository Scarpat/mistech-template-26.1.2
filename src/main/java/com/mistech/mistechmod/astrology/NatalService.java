package com.mistech.mistechmod.astrology;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.NatalChart.Part;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.MistechAttachments;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regras do Mapa Astral (servidor) e os bônus dos signos.
 *
 * Bônus: cada parte usa o mesmo atributo da constelação do signo, com uma fração do bônus total dela:
 * Sol 25% (sempre), Lua 25% (só à noite), Ascendente 15% (sempre). Enquanto a parte está escondida vale a metade.
 * Sol também dá +50% de afinidade no elemento do signo (+25% escondido) e deixa as estrelas da constelação
 * do seu Sol 25% mais baratas em XP (só depois de revelado).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class NatalService {
    public static final double SUN_SHARE = 0.25;
    public static final double MOON_SHARE = 0.25;
    public static final double RISING_SHARE = 0.15;
    public static final double HIDDEN_FACTOR = 0.5;
    public static final double SUN_AFFINITY_BONUS = 0.5;
    public static final double SUN_XP_DISCOUNT = 0.25;
    /** Janela do amanhecer (hora do relógio do mundo): 22.800 → 1.000. */
    public static final long DAWN_START = 22800L;
    public static final long DAWN_END = 1000L;

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    /** Custo de revelar cada parte. */
    public record RevealCost(int xp, List<Constellations.Cost> items) {}

    private NatalService() {}

    public static RevealCost cost(Part part) {
        return switch (part) {
            case SUN -> new RevealCost(8, List.of(new Constellations.Cost(() -> Items.GOLD_INGOT, 4)));
            case MOON -> new RevealCost(12, List.of(new Constellations.Cost(() -> Items.GLOWSTONE_DUST, 8),
                    new Constellations.Cost(Mistech.LIGHT_RUNE, 1)));
            case RISING -> new RevealCost(16, List.of(new Constellations.Cost(Mistech.TIME_RUNE, 1),
                    new Constellations.Cost(Mistech.IMPURE_MAGIC_SHARD, 2)));
        };
    }

    public static boolean isDawn(Level level) {
        long time = Math.floorMod(level.getDefaultClockTime(), 24000L);
        return time >= DAWN_START || time < DAWN_END;
    }

    /** A hora certa para revelar a parte? Sol = de dia, Lua = de noite, Ascendente = no amanhecer. */
    public static boolean rightTime(Level level, Part part) {
        return switch (part) {
            case SUN -> !level.isDarkOutside();
            case MOON -> level.isDarkOutside();
            case RISING -> isDawn(level);
        };
    }

    /** A parte anterior (Sol → Lua → Ascendente) já foi revelada? */
    public static boolean previousRevealed(NatalChart chart, Part part) {
        return part == Part.SUN || chart.isRevealed(Part.byIndex(part.ordinal() - 1));
    }

    public static boolean canAfford(Player player, Part part) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        RevealCost cost = cost(part);
        if (player.experienceLevel < cost.xp()) {
            return false;
        }
        for (Constellations.Cost item : cost.items()) {
            if (AstrologyService.countItem(player, item.item().get()) < item.count()) {
                return false;
            }
        }
        return true;
    }

    public static NatalChart chart(Player player) {
        return player.getData(MistechAttachments.NATAL_CHART);
    }

    /** Garante que o jogador tem um mapa sorteado (servidor). */
    public static NatalChart ensureRolled(Player player) {
        NatalChart chart = chart(player);
        if (!chart.isRolled()) {
            chart = NatalChart.roll(player.getRandom());
            player.setData(MistechAttachments.NATAL_CHART, chart);
        }
        return chart;
    }

    // ----- ações vindas da tela -----

    /** Checagens comuns: perto da mesa e com o campo completo. */
    private static boolean atTable(ServerPlayer player, BlockPos tablePos) {
        ServerLevel level = player.level();
        if (player.distanceToSqr(tablePos.getCenter()) > MAX_DISTANCE_SQR
                || !level.getBlockState(tablePos).is(Mistech.ASTROLOGY_TABLE.get())) {
            return false;
        }
        if (!AstrologyField.isValid(level, tablePos)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.astrology_field_broken"));
            return false;
        }
        return true;
    }

    public static void tryReveal(ServerPlayer player, BlockPos tablePos, Part part) {
        if (!atTable(player, tablePos)) {
            return;
        }
        ServerLevel level = player.level();
        NatalChart chart = ensureRolled(player);
        if (chart.isRevealed(part) || !previousRevealed(chart, part)) {
            return;
        }
        if (!rightTime(level, part)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.natal.wrong_time." + part.id()));
            return;
        }
        if (!canAfford(player, part)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.astrology_cannot_afford"));
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            RevealCost cost = cost(part);
            player.giveExperienceLevels(-cost.xp());
            for (Constellations.Cost item : cost.items()) {
                AstrologyService.removeItem(player, item.item().get(), item.count());
            }
        }
        NatalChart updated = chart.reveal(part);
        player.setData(MistechAttachments.NATAL_CHART, updated);
        apply(player);

        int sign = updated.sign(part);
        level.playSound(null, tablePos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.4F, 0.7F);
        level.playSound(null, tablePos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7F, 1.3F);
        level.sendParticles(ParticleTypes.END_ROD, tablePos.getX() + 0.5, tablePos.getY() + 1.3, tablePos.getZ() + 0.5,
                40, 0.4, 0.9, 0.4, 0.06);
        player.sendSystemMessage(Component.translatable("message.mistech.natal.revealed", part.displayName(),
                NatalChart.signName(sign)).withColor(NatalChart.elementOf(sign).color()));
    }

    /** Copia o mapa (só o que já foi revelado) num papel: vira o item Mapa Astral. */
    public static void tryCopy(ServerPlayer player, BlockPos tablePos) {
        if (!atTable(player, tablePos)) {
            return;
        }
        NatalChart chart = ensureRolled(player);
        if (chart.revealed() == 0) {
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            if (AstrologyService.countItem(player, Items.PAPER) < 1) {
                player.sendOverlayMessage(Component.translatable("message.mistech.natal.need_paper"));
                return;
            }
            AstrologyService.removeItem(player, Items.PAPER, 1);
        }
        player.getInventory().placeItemBackInInventory(copyOf(player, chart));
        player.level().playSound(null, tablePos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static ItemStack copyOf(Player player, NatalChart chart) {
        ItemStack stack = new ItemStack(Mistech.NATAL_CHART.get());
        stack.set(MistechComponents.NATAL_CHART_COPY.get(), new NatalChartCopy(player.getName().getString(),
                chart.isRevealed(Part.SUN) ? chart.sun() : -1,
                chart.isRevealed(Part.MOON) ? chart.moon() : -1,
                chart.isRevealed(Part.RISING) ? chart.rising() : -1));
        return stack;
    }

    /** Renascimento Astral (ritual): sorteia um mapa novo, todo escondido de novo. */
    public static void rebirth(Player player) {
        player.setData(MistechAttachments.NATAL_CHART, NatalChart.roll(player.getRandom()));
        apply(player);
    }

    /** Rituais de Realinhamento: troca uma parte do mapa para o signo escolhido (e ela já fica revelada). */
    public static NatalChart setSign(Player player, Part part, int sign) {
        NatalChart chart = ensureRolled(player);
        NatalChart updated = switch (part) {
            case SUN -> new NatalChart(sign, chart.moon(), chart.rising(), chart.revealed());
            case MOON -> new NatalChart(chart.sun(), sign, chart.rising(), chart.revealed());
            case RISING -> new NatalChart(chart.sun(), chart.moon(), sign, chart.revealed());
        };
        updated = updated.reveal(part);
        player.setData(MistechAttachments.NATAL_CHART, updated);
        apply(player);
        return updated;
    }

    /** Signo indicado pelo item raro de uma constelação (-1 se não for nenhum). */
    public static int signOfToken(ItemStack stack) {
        for (int i = 0; i < Constellations.ALL.size(); i++) {
            if (stack.is(Constellations.get(i).rareItem().get())) {
                return i;
            }
        }
        return -1;
    }

    // ----- bônus -----

    private static Identifier modifierId(Part part) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "natal_" + part.id());
    }

    private static double share(Part part) {
        return switch (part) {
            case SUN -> SUN_SHARE;
            case MOON -> MOON_SHARE;
            case RISING -> RISING_SHARE;
        };
    }

    /** Bônus total da constelação do signo (todas as estrelas). */
    public static double constellationTotal(int sign) {
        Constellations.Constellation c = Constellations.get(sign);
        return c.perk().perStar() * c.stars().size();
    }

    /** Valor do bônus daquela parte agora (0 se for a Lua de dia). Serve para o cliente também. */
    public static double bonus(Level level, NatalChart chart, Part part, int sign) {
        if (sign < 0 || part == Part.MOON && !level.isDarkOutside()) {
            return 0.0;
        }
        double value = constellationTotal(sign) * share(part);
        return chart.isRevealed(part) ? value : value * HIDDEN_FACTOR;
    }

    /**
     * Quanto o equipamento de Oricalco vestido fortalece cada parte do mapa (1,0 = nada).
     * Cada peça de armadura com energia soma {@link com.mistech.mistechmod.item.mithril.GearTier#natalBoost}.
     */
    public static double gearBoost(Player player, Part part) {
        double boost = 1.0;
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (com.mistech.mistechmod.item.mithril.MithrilGear.isMithril(stack)
                    && com.mistech.mistechmod.item.mithril.MithrilGear.isCharged(stack)) {
                boost += com.mistech.mistechmod.item.mithril.MithrilGear.tier(stack).natalBoost(part);
            }
        }
        return boost;
    }

    /** {@link #bonus(Level, NatalChart, Part, int)} com o reforço do equipamento de Oricalco. */
    public static double bonus(Player player, NatalChart chart, Part part, int sign) {
        return bonus(player.level(), chart, part, sign) * gearBoost(player, part);
    }

    public static void apply(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        NatalChart chart = chart(player);
        for (Part part : Part.values()) {
            Identifier id = modifierId(part);
            int sign = chart.sign(part);
            double amount = sign < 0 ? 0.0 : bonus(player, chart, part, sign);
            Constellations.Perk wanted = sign < 0 ? null : Constellations.get(sign).perk();
            for (Constellations.Constellation c : Constellations.ALL) {
                AttributeInstance instance = player.getAttribute(c.perk().attribute());
                if (instance == null) {
                    continue;
                }
                AttributeModifier existing = instance.getModifier(id);
                boolean here = wanted != null && amount != 0.0 && c.perk().attribute().equals(wanted.attribute());
                if (here && existing != null && existing.amount() == amount && existing.operation() == wanted.operation()) {
                    continue; // já está certo: não mexe (tirar e pôr vida máxima faria perder corações)
                }
                if (existing != null) {
                    instance.removeModifier(id);
                }
                if (here) {
                    instance.addTransientModifier(new AttributeModifier(id, amount, wanted.operation()));
                }
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Afinidade extra quando o elemento é o do seu Sol (arredonda com sorte: 1 ponto +50% = 50% de chance de +1). */
    public static int affinityWithBonus(Player player, RuneElement element, int amount) {
        NatalChart chart = chart(player);
        if (chart.sun() < 0 || NatalChart.elementOf(chart.sun()) != element) {
            return amount;
        }
        double bonus = amount * SUN_AFFINITY_BONUS * (chart.isRevealed(Part.SUN) ? 1.0 : HIDDEN_FACTOR) * gearBoost(player, Part.SUN);
        int whole = (int) Math.floor(bonus);
        if (player.getRandom().nextDouble() < bonus - whole) {
            whole++;
        }
        return amount + whole;
    }

    /** XP de uma estrela, com o desconto da constelação do seu Sol (só revelado). */
    public static int xpCost(@Nullable Player player, int constellation, int star) {
        int base = Constellations.get(constellation).xpCost(star);
        if (player != null) {
            NatalChart chart = chart(player);
            if (chart.isRevealed(Part.SUN) && chart.sun() == constellation) {
                return Math.max(1, (int) Math.round(base * (1.0 - SUN_XP_DISCOUNT)));
            }
        }
        return base;
    }

    // ----- eventos -----

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ensureRolled(event.getEntity());
        apply(event.getEntity());
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        apply(event.getEntity());
    }

    /** A Lua liga/desliga com a noite: recalcula a cada 5 s. */
    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % 100 == 0) {
            apply(player);
        }
    }
}
