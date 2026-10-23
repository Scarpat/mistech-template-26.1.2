package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.affinity.AffinityData;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.staff.GnomeStaffItem;
import com.mistech.mistechmod.mage.MageRobes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Mana do jogador. O máximo cresce com a soma dos níveis de afinidade (quem estuda mais elementos tem mais mana)
 * e a regeneração também. Funciona igual no cliente (a afinidade e a mana são sincronizadas com o dono) para a barra.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ManaService {
    public static final float BASE_MAX = 100.0F;
    public static final float PER_LEVEL = 15.0F;
    public static final float HARD_CAP = 600.0F;
    /** Mana por segundo: base + por nível de afinidade somado. */
    public static final float BASE_REGEN = 2.0F;
    public static final float REGEN_PER_LEVEL = 0.4F;

    private ManaService() {}

    /** Soma dos níveis de afinidade (10 elementos básicos). */
    public static int totalLevels(Player player) {
        AffinityData data = Affinity.get(player);
        int sum = 0;
        for (RuneElement element : RuneElement.values()) {
            if (!element.isAdvanced()) {
                sum += Affinity.level(data.get(element));
            }
        }
        return sum;
    }

    /** Coroa de Éter: +150 de mana máxima (acima do teto) e regeneração ×1,75. */
    public static final float CROWN_MANA = 150.0F;

    public static boolean crown(Player player) {
        return com.mistech.mistechmod.accessory.Accessories.has(player, com.mistech.mistechmod.accessory.Accessory.AETHER_CROWN);
    }

    public static float max(Player player) {
        return Math.min(HARD_CAP, BASE_MAX + PER_LEVEL * totalLevels(player)) + player.getData(SpellcraftContent.MANA_BONUS);
    }

    public static float regenPerSecond(Player player) {
        return (BASE_REGEN + REGEN_PER_LEVEL * totalLevels(player)) * (crown(player) ? 1.75F : 1.0F);
    }

    public static float get(Player player) {
        return Mth.clamp(player.getData(SpellcraftContent.MANA), 0.0F, max(player));
    }

    public static void set(Player player, float mana) {
        float value = Mth.clamp(mana, 0.0F, max(player));
        if (Math.abs(player.getData(SpellcraftContent.MANA) - value) > 0.001F) {
            player.setData(SpellcraftContent.MANA, value);
        }
    }

    /** Custo real: Vestes de Mago, Amuleto de Essência e Surto Arcano valem para a mana também. */
    public static int cost(Player player, int base) {
        return Math.max(1, GnomeStaffItem.cost(player, MageRobes.cost(player, base)));
    }

    /** O Tomo/Varinha/Cajado na mão que tem reserva de mana (vazio se nenhum). */
    public static net.minecraft.world.item.ItemStack reserveStack(Player player) {
        for (net.minecraft.world.item.ItemStack stack : new net.minecraft.world.item.ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.getItem() instanceof SpellFocusItem item && item.focus().reserve() > 0) {
                return stack;
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public static int reserveOf(net.minecraft.world.item.ItemStack stack) {
        return stack.getOrDefault(SpellcraftContent.STAFF_MANA.get(), 0);
    }

    public static int reserveMax(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof SpellFocusItem item ? item.focus().reserve() : 0;
    }

    public static int reserve(Player player) {
        return reserveOf(reserveStack(player));
    }

    /** Põe mana na reserva de um Cajado; devolve quanto coube. */
    public static int addReserve(net.minecraft.world.item.ItemStack stack, int amount) {
        int max = reserveMax(stack);
        int current = reserveOf(stack);
        int added = Math.max(0, Math.min(amount, max - current));
        if (added > 0) {
            stack.set(SpellcraftContent.STAFF_MANA.get(), current + added);
        }
        return added;
    }

    public static boolean has(Player player, int amount) {
        return player.hasInfiniteMaterials() || get(player) + reserve(player) >= amount;
    }

    /** Gasta a mana (criativo não gasta). */
    public static boolean spend(Player player, int amount) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        float current = get(player);
        net.minecraft.world.item.ItemStack stack = reserveStack(player);
        int reserve = reserveOf(stack);
        if (current + reserve < amount) {
            return false;
        }
        // a reserva do Cajado é gasta antes da mana do jogador
        int fromReserve = Math.min(reserve, amount);
        if (fromReserve > 0) {
            stack.set(SpellcraftContent.STAFF_MANA.get(), reserve - fromReserve);
        }
        set(player, current - (amount - fromReserve));
        return true;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        net.minecraft.world.item.ItemStack held = reserveStack(player);
        if (!held.isEmpty()) {
            // a reserva do Cajado recarrega sozinha, devagar (1% por segundo)
            addReserve(held, Math.max(1, (int) Math.round(reserveMax(held) * 0.005)));
        }
        float bonus = crown(player) ? CROWN_MANA : 0.0F;
        if (player.getData(SpellcraftContent.MANA_BONUS) != bonus) {
            player.setData(SpellcraftContent.MANA_BONUS, bonus);
        }
        float max = max(player);
        float current = player.getData(SpellcraftContent.MANA);
        if (current > max) {
            set(player, max);
        } else if (current < max) {
            set(player, current + regenPerSecond(player) * 0.5F);
        }
    }
}
