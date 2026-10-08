package com.mistech.mistechmod.spatial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Todas as Âncoras Espaciais do servidor (salvas no Mundo Superior, valem para todas as dimensões).
 * Cada uma tem uma chave única ({@link #key}) = dimensão + posição da base.
 */
public class SpatialAnchors extends SavedData {
    public record Anchor(Identifier dimension, BlockPos pos, String name) {
        public static final Codec<Anchor> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Anchor::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Anchor::pos),
                Codec.STRING.fieldOf("name").forGetter(Anchor::name)
        ).apply(i, Anchor::new));

        public String key() {
            return SpatialAnchors.key(this.dimension, this.pos);
        }

        public ResourceKey<Level> levelKey() {
            return ResourceKey.create(Registries.DIMENSION, this.dimension);
        }
    }

    public static final Codec<SpatialAnchors> CODEC = Anchor.CODEC.listOf().optionalFieldOf("anchors", List.of()).codec()
            .xmap(SpatialAnchors::new, data -> List.copyOf(data.anchors.values()));

    public static final SavedDataType<SpatialAnchors> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "spatial_anchors"), SpatialAnchors::new, CODEC);

    private final Map<String, Anchor> anchors = new LinkedHashMap<>();

    public SpatialAnchors() {}

    private SpatialAnchors(List<Anchor> list) {
        for (Anchor anchor : list) {
            this.anchors.put(anchor.key(), anchor);
        }
    }

    public static SpatialAnchors get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static String key(Identifier dimension, BlockPos pos) {
        return dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static String key(Level level, BlockPos pos) {
        return key(level.dimension().identifier(), pos);
    }

    public void add(Anchor anchor) {
        this.anchors.put(anchor.key(), anchor);
        this.setDirty();
    }

    public void remove(String key) {
        if (this.anchors.remove(key) != null) {
            this.setDirty();
        }
    }

    public @Nullable Anchor get(String key) {
        return this.anchors.get(key);
    }

    public List<Anchor> all() {
        return new ArrayList<>(this.anchors.values());
    }
}
