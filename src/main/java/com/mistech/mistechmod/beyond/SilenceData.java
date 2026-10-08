package com.mistech.mistechmod.beyond;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/** Zonas do Selo do Silêncio de um mundo (salvas com o mundo): centro + gameTime em que acabam. */
public class SilenceData extends SavedData {
    public static final int RADIUS = 48;
    public static final long DURATION = 24000L;

    public record Zone(BlockPos pos, long until) {
        public static final Codec<Zone> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Zone::pos),
                Codec.LONG.fieldOf("until").forGetter(Zone::until)
        ).apply(i, Zone::new));
    }

    public static final Codec<SilenceData> CODEC = Zone.CODEC.listOf().optionalFieldOf("zones", List.of()).codec()
            .xmap(SilenceData::new, data -> List.copyOf(data.zones));

    public static final SavedDataType<SilenceData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "silence_zones"), SilenceData::new, CODEC);

    private final List<Zone> zones = new ArrayList<>();

    public SilenceData() {}

    private SilenceData(List<Zone> zones) {
        this.zones.addAll(zones);
    }

    public static SilenceData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(BlockPos pos, long until) {
        this.zones.add(new Zone(pos.immutable(), until));
        this.setDirty();
    }

    public boolean isEmpty() {
        return this.zones.isEmpty();
    }

    /** Está dentro de alguma zona ativa? (limpa as vencidas) */
    public boolean isSilenced(long now, Vec3 at) {
        if (this.zones.isEmpty()) {
            return false;
        }
        if (this.zones.removeIf(z -> z.until() <= now)) {
            this.setDirty();
        }
        for (Zone zone : this.zones) {
            if (zone.pos().getCenter().distanceToSqr(at) <= (double) RADIUS * RADIUS) {
                return true;
            }
        }
        return false;
    }
}
