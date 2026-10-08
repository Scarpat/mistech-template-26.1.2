package com.mistech.mistechmod.item.mithril;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Runas engastadas no equipamento revestido (Adamantina: 3, Oricalco: 6, Oricalco Puro: 8).
 * A runa vai inteira para a peça (componente {@code mistech:gear_runes}) e volta com os usos que tinha ao ser tirada.
 * Runa Selada conta como 1,5 runa.
 */
public final class GearRunes {
    public static final int MAX_SLOTS = 8;
    public static final double SEALED_WEIGHT = 1.5;

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private GearRunes() {}

    public static int slots(ItemStack gear) {
        return MithrilGear.isMithril(gear) ? MithrilGear.tier(gear).runeSlots() : 0;
    }

    /** As runas da peça, sempre com {@link #MAX_SLOTS} posições. */
    public static NonNullList<ItemStack> load(ItemStack gear) {
        NonNullList<ItemStack> list = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        ItemContainerContents contents = gear.get(MistechComponents.GEAR_RUNES.get());
        if (contents != null) {
            contents.copyInto(list);
        }
        return list;
    }

    public static void store(ItemStack gear, List<ItemStack> runes) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack rune : runes) {
            copy.add(rune.copy());
        }
        ItemContainerContents contents = ItemContainerContents.fromItems(copy);
        if (contents.equals(ItemContainerContents.EMPTY)) {
            gear.remove(MistechComponents.GEAR_RUNES.get());
        } else {
            gear.set(MistechComponents.GEAR_RUNES.get(), contents);
        }
    }

    public static boolean isSealed(ItemStack rune) {
        return Boolean.TRUE.equals(rune.get(PurityContent.SEALED.get()));
    }

    public static double weight(ItemStack rune) {
        return isSealed(rune) ? SEALED_WEIGHT : 1.0;
    }

    /** Peso de cada elemento engastado nesta peça (só as posições que o nível permite). */
    public static Map<RuneElement, Double> tally(ItemStack gear) {
        Map<RuneElement, Double> map = new EnumMap<>(RuneElement.class);
        int slots = slots(gear);
        if (slots <= 0 || !gear.has(MistechComponents.GEAR_RUNES.get())) {
            return map;
        }
        NonNullList<ItemStack> runes = load(gear);
        for (int i = 0; i < slots; i++) {
            ItemStack rune = runes.get(i);
            RuneElement element = RuneElement.ofRune(rune);
            if (element != null) {
                map.merge(element, weight(rune), Double::sum);
            }
        }
        return map;
    }

    /** Soma das runas de tudo que está vestido + a peça na mão principal (só peças com energia). */
    public static Map<RuneElement, Double> tallyEquipped(LivingEntity entity) {
        Map<RuneElement, Double> map = new EnumMap<>(RuneElement.class);
        for (EquipmentSlot slot : ARMOR) {
            add(map, entity.getItemBySlot(slot));
        }
        add(map, entity.getMainHandItem());
        return map;
    }

    private static void add(Map<RuneElement, Double> map, ItemStack stack) {
        if (MithrilGear.isMithril(stack) && MithrilGear.isCharged(stack)) {
            tally(stack).forEach((element, value) -> map.merge(element, value, Double::sum));
        }
    }

    public static double count(Map<RuneElement, Double> tally, RuneElement element) {
        return tally.getOrDefault(element, 0.0);
    }

    public static void tooltip(ItemStack gear, Consumer<Component> builder) {
        int slots = slots(gear);
        if (slots <= 0) {
            return;
        }
        NonNullList<ItemStack> runes = load(gear);
        int used = 0;
        for (int i = 0; i < slots; i++) {
            if (!runes.get(i).isEmpty()) {
                used++;
            }
        }
        builder.accept(Component.translatable("item.mistech.gear.runes", used, slots).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (int i = 0; i < slots; i++) {
            ItemStack rune = runes.get(i);
            RuneElement element = RuneElement.ofRune(rune);
            if (element == null) {
                continue;
            }
            MutableComponent line = Component.literal(" ◆ ").withColor(element.color())
                    .append(Component.translatable(element.translationKey()).withColor(element.color()));
            if (isSealed(rune)) {
                line.append(Component.translatable("item.mistech.gear.rune_sealed").withStyle(ChatFormatting.GOLD));
            }
            builder.accept(line);
        }
    }
}
