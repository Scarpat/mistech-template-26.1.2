package com.mistech.mistechmod.extraplanar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.FluidTank;
import com.mistech.mistechmod.menu.SpatialStorageMenu;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * Canais do Barril e do Tanque Extraplanares: todos os blocos com o mesmo código dividem o mesmo conteúdo,
 * em qualquer distância e dimensão. Os códigos de barril e de tanque são independentes.
 * Salvo no Mundo Superior como {@code mistech:extraplanar}.
 */
public class ExtraplanarData extends SavedData {
    public static final int MAX_CODE_LENGTH = 32;
    public static final int ITEM_SLOTS = SpatialStorageMenu.SIZE;
    /** Igual ao Tanque Rúnico: 100 baldes. */
    public static final int TANK_CAPACITY = 100_000;

    public record SlotEntry(int slot, ItemStack stack) {
        public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(SlotEntry::slot),
                ItemStack.CODEC.fieldOf("stack").forGetter(SlotEntry::stack)
        ).apply(i, SlotEntry::new));
    }

    public record ItemChannelData(String code, List<SlotEntry> slots) {
        public static final Codec<ItemChannelData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("code").forGetter(ItemChannelData::code),
                SlotEntry.CODEC.listOf().fieldOf("slots").forGetter(ItemChannelData::slots)
        ).apply(i, ItemChannelData::new));
    }

    public record FluidChannelData(String code, SimpleFluidContent fluid) {
        public static final Codec<FluidChannelData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("code").forGetter(FluidChannelData::code),
                SimpleFluidContent.CODEC.fieldOf("fluid").forGetter(FluidChannelData::fluid)
        ).apply(i, FluidChannelData::new));
    }

    /** Canal de tanque: o fluido e quem está de olho nele (para o cliente ver o nível). */
    public static final class FluidChannel {
        public final FluidTank tank;
        private final Set<ExtraplanarTankBlockEntity> listeners = Collections.newSetFromMap(new WeakHashMap<>());

        FluidChannel(ExtraplanarData data) {
            this.tank = new FluidTank(TANK_CAPACITY, () -> {
                data.setDirty();
                for (ExtraplanarTankBlockEntity listener : List.copyOf(this.listeners)) {
                    listener.syncFromChannel();
                }
            });
        }

        public void addListener(ExtraplanarTankBlockEntity tank) {
            this.listeners.add(tank);
        }

        public void removeListener(ExtraplanarTankBlockEntity tank) {
            this.listeners.remove(tank);
        }
    }

    public static final Codec<ExtraplanarData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemChannelData.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(ExtraplanarData::exportItems),
            FluidChannelData.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(ExtraplanarData::exportFluids)
    ).apply(i, ExtraplanarData::new));

    public static final SavedDataType<ExtraplanarData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "extraplanar"), ExtraplanarData::new, CODEC);

    private final Map<String, NonNullList<ItemStack>> itemChannels = new HashMap<>();
    private final Map<String, FluidChannel> fluidChannels = new HashMap<>();

    public ExtraplanarData() {}

    private ExtraplanarData(List<ItemChannelData> items, List<FluidChannelData> fluids) {
        for (ItemChannelData channel : items) {
            NonNullList<ItemStack> list = NonNullList.withSize(ITEM_SLOTS, ItemStack.EMPTY);
            for (SlotEntry entry : channel.slots()) {
                if (entry.slot() >= 0 && entry.slot() < ITEM_SLOTS) {
                    list.set(entry.slot(), entry.stack());
                }
            }
            this.itemChannels.put(channel.code(), list);
        }
        for (FluidChannelData channel : fluids) {
            FluidChannel fluid = new FluidChannel(this);
            fluid.tank.setFluid(channel.fluid().copy());
            this.fluidChannels.put(channel.code(), fluid);
        }
    }

    private List<ItemChannelData> exportItems() {
        List<ItemChannelData> out = new ArrayList<>();
        for (Map.Entry<String, NonNullList<ItemStack>> channel : this.itemChannels.entrySet()) {
            List<SlotEntry> slots = new ArrayList<>();
            NonNullList<ItemStack> list = channel.getValue();
            for (int slot = 0; slot < list.size(); slot++) {
                if (!list.get(slot).isEmpty()) {
                    slots.add(new SlotEntry(slot, list.get(slot)));
                }
            }
            if (!slots.isEmpty()) {
                out.add(new ItemChannelData(channel.getKey(), slots));
            }
        }
        return out;
    }

    private List<FluidChannelData> exportFluids() {
        List<FluidChannelData> out = new ArrayList<>();
        for (Map.Entry<String, FluidChannel> channel : this.fluidChannels.entrySet()) {
            if (!channel.getValue().tank.isEmpty()) {
                out.add(new FluidChannelData(channel.getKey(), SimpleFluidContent.copyOf(channel.getValue().tank.getFluid())));
            }
        }
        return out;
    }

    public static ExtraplanarData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Os 162 espaços do canal de barril {@code code} (criado vazio se ainda não existe). */
    public NonNullList<ItemStack> items(String code) {
        return this.itemChannels.computeIfAbsent(code, c -> NonNullList.withSize(ITEM_SLOTS, ItemStack.EMPTY));
    }

    public FluidChannel fluid(String code) {
        return this.fluidChannels.computeIfAbsent(code, c -> new FluidChannel(this));
    }
}
