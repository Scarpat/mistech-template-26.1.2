package com.mistech.mistechmod.item.mithril;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Equipamento revestido de mitril: gasta energia (FE) no lugar da durabilidade.
 * <ul>
 * <li>Cada ponto de "dano" que o item levaria vira {@link #COST_PER_DAMAGE} FE (Inquebrável ainda reduz).</li>
 * <li>Nunca quebra. Sem energia: ferramentas mineram na velocidade da mão e não coletam drops que precisam
 * de ferramenta; armas e armaduras perdem os atributos (ver {@link MithrilEvents}).</li>
 * <li>Carrega em qualquer carregador (Bateria Rúnica etc.), pela Runa de Energia Mágica ou tragando o Cachimbo Rúnico com ela.</li>
 * </ul>
 */
public final class MithrilGear {
    /** Capacidade do mitril; os outros níveis em {@link GearTier}. */
    public static final int CAPACITY = 200_000;
    public static final int MAX_CHARGE_PER_TICK = 4_000;
    public static final int COST_PER_DAMAGE = 50;
    /** Carga com que o item sai do ritual (vem da Runa de Energia Mágica). */
    public static final int INITIAL_CHARGE = 25_000;
    public static final int BAR_COLOR = 0x7FD8FF;

    private MithrilGear() {}

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.GEAR_ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.set(MistechComponents.GEAR_ENERGY.get(), Mth.clamp(energy, 0, capacity(stack)));
    }

    /** Nível do equipamento (Mitril se não for equipamento revestido). */
    public static GearTier tier(ItemStack stack) {
        return stack.getItem() instanceof Gear gear ? gear.tier() : GearTier.MITHRIL;
    }

    public static int capacity(ItemStack stack) {
        return tier(stack).capacity();
    }

    public static boolean isCharged(ItemStack stack) {
        return getEnergy(stack) > 0;
    }

    public static boolean isMithril(ItemStack stack) {
        return stack.getItem() instanceof Gear;
    }

    /** Marca dos itens revestidos (mitril, adamantina, oricalco...; todas as classes implementam). */
    public interface Gear {
        default GearTier tier() {
            return GearTier.MITHRIL;
        }
    }

    /** Troca o desgaste por energia. @return sempre 0 (a durabilidade nunca muda). */
    public static int damageItem(ItemStack stack, int amount, @Nullable LivingEntity entity) {
        if (amount <= 0 || entity == null || !(entity.level() instanceof ServerLevel level)) {
            return 0;
        }
        if (entity instanceof Player player && player.hasInfiniteMaterials()) {
            return 0;
        }
        if (entity.hasEffect(MistechEffects.ARCANE_SURGE)) {
            return 0; // Surto Arcano: o mitril não gasta energia
        }
        int processed = EnchantmentHelper.processDurabilityChange(level, stack, amount); // Inquebrável
        int energy = getEnergy(stack);
        if (processed > 0 && energy > 0) {
            setEnergy(stack, energy - processed * COST_PER_DAMAGE);
        }
        return 0;
    }

    public static int barWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / capacity(stack));
    }

    public static void tooltip(ItemStack stack, Consumer<Component> builder) {
        int energy = getEnergy(stack);
        GearTier tier = tier(stack);
        builder.accept(Component.translatable("item.mistech.mithril.energy", energy, tier.capacity())
                .withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        if (energy <= 0) {
            builder.accept(Component.translatable("item.mistech.mithril.empty").withStyle(ChatFormatting.DARK_RED));
        }
        GearRunes.tooltip(stack, builder);
        GearConstellation.tooltip(stack, builder);
        com.mistech.mistechmod.tarot.GearArcana.tooltip(stack, builder);
        builder.accept(Component.translatable("item.mistech." + tier.id() + "_gear.hint").withStyle(ChatFormatting.DARK_GRAY));
        if (tier.atLeast(GearTier.ADAMANTINE)) {
            builder.accept(Component.translatable("item.mistech." + tier.id() + "_gear.set").withStyle(tier.textColor()));
            builder.accept(Component.translatable("item.mistech." + tier.id() + "_gear.ability").withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Carrega os itens com energia do inventário do jogador (outra mão/mão, armadura, depois o resto) até gastar {@code amount}.
     * @return quanto entrou
     */
    public static int chargeInventory(Player player, int amount) {
        int left = amount;
        int size = Math.min(player.getInventory().getContainerSize(), 41);
        int selected = player.getInventory().getSelectedSlot();
        // ordem: mão, outra mão, armadura, resto
        java.util.List<Integer> order = new java.util.ArrayList<>();
        order.add(selected);
        order.add(40);
        for (int i = 36; i < 40; i++) order.add(i);
        for (int i = 0; i < 36; i++) if (i != selected) order.add(i);
        for (int slot : order) {
            if (left <= 0 || slot >= size) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            EnergyHandler handler = stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forPlayerSlot(player, slot));
            if (handler == null) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int moved = handler.insert(left, tx);
                tx.commit();
                left -= moved;
            }
        }
        return amount - left;
    }
}
