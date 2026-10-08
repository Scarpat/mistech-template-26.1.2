package com.mistech.mistechmod.ether;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Sintonias da Rede Etérea (salvas no Mundo Superior, valem para todas as dimensões).
 * A energia mora aqui, não no bloco: a rede funciona mesmo com o Nexo descarregado. O Nexo só define o teto ({@code capacity}).
 */
public class EtherNetworks extends SavedData {
    public record NetData(int id, String name, long energy, long capacity) {
        public static final Codec<NetData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(NetData::id),
                Codec.STRING.fieldOf("name").forGetter(NetData::name),
                Codec.LONG.fieldOf("energy").forGetter(NetData::energy),
                Codec.LONG.fieldOf("capacity").forGetter(NetData::capacity)
        ).apply(i, NetData::new));
    }

    /** Uma sintonia (mutável; salva como {@link NetData}). */
    public static final class Net {
        public final int id;
        public String name;
        public long energy;
        public long capacity;
        /** Só em memória: quem está ligado agora (para a informação do Nexo). */
        public final Set<String> inputs = new HashSet<>();
        public final Set<String> outputs = new HashSet<>();
        public long movedInLastSecond, movedOutLastSecond, movedIn, movedOut;

        Net(int id, String name, long energy, long capacity) {
            this.id = id;
            this.name = name;
            this.energy = energy;
            this.capacity = capacity;
        }

        public long space() {
            return Math.max(0L, this.capacity - this.energy);
        }
    }

    public static final Codec<EtherNetworks> CODEC = RecordCodecBuilder.create(i -> i.group(
            NetData.CODEC.listOf().optionalFieldOf("networks", List.of()).forGetter(d -> d.nets.values().stream()
                    .map(n -> new NetData(n.id, n.name, n.energy, n.capacity)).toList()),
            Codec.INT.optionalFieldOf("next_id", 1).forGetter(d -> d.nextId)
    ).apply(i, EtherNetworks::new));

    public static final SavedDataType<EtherNetworks> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "ether_networks"), EtherNetworks::new, CODEC);

    private final Map<Integer, Net> nets = new LinkedHashMap<>();
    private int nextId = 1;

    public EtherNetworks() {}

    private EtherNetworks(List<NetData> list, int nextId) {
        for (NetData d : list) {
            this.nets.put(d.id(), new Net(d.id(), d.name(), d.energy(), d.capacity()));
        }
        this.nextId = Math.max(nextId, this.nets.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1);
    }

    public static EtherNetworks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Net create(@Nullable String name) {
        int id = this.nextId++;
        Net net = new Net(id, name == null || name.isBlank() ? "#" + id : name, 0L, 0L);
        this.nets.put(id, net);
        this.setDirty();
        return net;
    }

    public @Nullable Net get(int id) {
        return this.nets.get(id);
    }

    /** Coloca até {@code amount} na rede. @return quanto entrou. */
    public long insert(Net net, long amount) {
        long moved = Math.min(amount, net.space());
        if (moved > 0) {
            net.energy += moved;
            net.movedIn += moved;
            this.setDirty();
        }
        return moved;
    }

    /** Tira até {@code amount} da rede. @return quanto saiu. */
    public long extract(Net net, long amount) {
        long moved = Math.min(amount, net.energy);
        if (moved > 0) {
            net.energy -= moved;
            net.movedOut += moved;
            this.setDirty();
        }
        return moved;
    }

    public void setCapacity(Net net, long capacity) {
        if (net.capacity != capacity) {
            net.capacity = capacity;
            this.setDirty();
        }
    }

    /** Fecha a contagem por segundo (chamado pelo Nexo a cada 20 ticks). */
    public static void rollSecond(Net net) {
        net.movedInLastSecond = net.movedIn;
        net.movedOutLastSecond = net.movedOut;
        net.movedIn = 0;
        net.movedOut = 0;
    }

    public Map<Integer, Net> all() {
        return new HashMap<>(this.nets);
    }
}
