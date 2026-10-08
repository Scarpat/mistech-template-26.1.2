package com.mistech.mistechmod.archive;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Conteúdo dos Pergaminhos Espaciais (o item só guarda o UUID; assim não carrega NBT gigante).
 * Salvo no Mundo Superior como {@code mistech:archive_cells}.
 */
public class ArchiveCells extends SavedData {
    public record Entry(ItemResource resource, long count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemResource.CODEC.fieldOf("item").forGetter(Entry::resource),
                Codec.LONG.fieldOf("count").forGetter(Entry::count)
        ).apply(i, Entry::new));
    }

    public record CellData(UUID id, List<Entry> items) {
        public static final Codec<CellData> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(CellData::id),
                Entry.CODEC.listOf().fieldOf("items").forGetter(CellData::items)
        ).apply(i, CellData::new));
    }

    /** Uma célula: tipo -> quantidade (na ordem em que entraram). */
    public static final class Cell {
        public final UUID id;
        public final LinkedHashMap<ItemResource, Long> items = new LinkedHashMap<>();
        private long total;

        Cell(UUID id) {
            this.id = id;
        }

        public long total() {
            return this.total;
        }

        void add(ItemResource resource, long amount) {
            long now = this.items.getOrDefault(resource, 0L) + amount;
            if (now <= 0) {
                this.items.remove(resource);
            } else {
                this.items.put(resource, now);
            }
            this.total += amount;
        }
    }

    public static final Codec<ArchiveCells> CODEC = CellData.CODEC.listOf().optionalFieldOf("cells", List.of()).codec()
            .xmap(ArchiveCells::new, d -> d.cells.values().stream()
                    .map(c -> new CellData(c.id, c.items.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList()))
                    .toList());

    public static final SavedDataType<ArchiveCells> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "archive_cells"), ArchiveCells::new, CODEC);

    private final Map<UUID, Cell> cells = new HashMap<>();

    public ArchiveCells() {}

    private ArchiveCells(List<CellData> list) {
        for (CellData data : list) {
            Cell cell = new Cell(data.id());
            for (Entry e : data.items()) {
                cell.add(e.resource(), e.count());
            }
            this.cells.put(data.id(), cell);
        }
    }

    public static ArchiveCells get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Cell cell(UUID id) {
        return this.cells.computeIfAbsent(id, Cell::new);
    }

    /** Põe até {@code amount} na célula respeitando o nível. @return quanto entrou. */
    public long insert(Cell cell, ScrollTier tier, ItemResource resource, long amount, boolean simulate) {
        if (!cell.items.containsKey(resource) && cell.items.size() >= tier.types) {
            return 0;
        }
        long moved = Math.min(amount, tier.capacity - cell.total);
        if (moved <= 0) {
            return 0;
        }
        if (!simulate) {
            cell.add(resource, moved);
            this.setDirty();
        }
        return moved;
    }

    public long extract(Cell cell, ItemResource resource, long amount, boolean simulate) {
        long moved = Math.min(amount, cell.items.getOrDefault(resource, 0L));
        if (moved <= 0) {
            return 0;
        }
        if (!simulate) {
            cell.add(resource, -moved);
            this.setDirty();
        }
        return moved;
    }
}
