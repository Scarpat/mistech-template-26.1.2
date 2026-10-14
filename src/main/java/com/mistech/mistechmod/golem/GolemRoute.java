package com.mistech.mistechmod.golem;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Rota com várias origens e destinos (Vara de Comando Rúnica) e como o golem escolhe entre eles.
 * Guardada no item da vara (componente {@code mistech:golem_route}) e no golem.
 */
public record GolemRoute(List<BlockPos> sources, List<BlockPos> targets, Collect collect, Deliver deliver) {
    public static final int MAX_POINTS = 8;
    public static final GolemRoute EMPTY = new GolemRoute(List.of(), List.of(), Collect.ORDER, Deliver.ORDER);

    /** De qual origem o golem pega. */
    public enum Collect {
        /** A primeira da lista que tiver algo (prioridade). */
        ORDER,
        /** Uma de cada vez, em rodízio. */
        ROUND_ROBIN,
        /** A que tiver mais. */
        FULLEST;

        public String key() {
            return "golem_route.mistech.collect." + this.name().toLowerCase(java.util.Locale.ROOT);
        }

        public Collect next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        static Collect byId(int id) {
            return values()[Math.max(0, Math.min(values().length - 1, id))];
        }
    }

    /** Para qual destino o golem leva. */
    public enum Deliver {
        /** O primeiro da lista que tiver espaço (prioridade). */
        ORDER,
        /** Um de cada vez, em rodízio. */
        ROUND_ROBIN,
        /** O que tiver menos daquilo (deixa todos iguais). */
        EQUAL;

        public String key() {
            return "golem_route.mistech.deliver." + this.name().toLowerCase(java.util.Locale.ROOT);
        }

        public Deliver next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        static Deliver byId(int id) {
            return values()[Math.max(0, Math.min(values().length - 1, id))];
        }
    }

    public GolemRoute {
        sources = List.copyOf(sources);
        targets = List.copyOf(targets);
    }

    public static final Codec<GolemRoute> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.listOf().optionalFieldOf("sources", List.of()).forGetter(GolemRoute::sources),
            BlockPos.CODEC.listOf().optionalFieldOf("targets", List.of()).forGetter(GolemRoute::targets),
            Codec.INT.optionalFieldOf("collect", 0).xmap(Collect::byId, Collect::ordinal).forGetter(GolemRoute::collect),
            Codec.INT.optionalFieldOf("deliver", 0).xmap(Deliver::byId, Deliver::ordinal).forGetter(GolemRoute::deliver))
            .apply(i, GolemRoute::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GolemRoute> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), GolemRoute::sources,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), GolemRoute::targets,
            ByteBufCodecs.VAR_INT.map(Collect::byId, Collect::ordinal), GolemRoute::collect,
            ByteBufCodecs.VAR_INT.map(Deliver::byId, Deliver::ordinal), GolemRoute::deliver,
            GolemRoute::new);

    public boolean isComplete() {
        return !this.sources.isEmpty() && !this.targets.isEmpty();
    }

    /** Liga/desliga um ponto (origem ou destino). Um bloco não pode ser os dois. @return null se a lista já está cheia. */
    public GolemRoute toggle(BlockPos pos, boolean target) {
        List<BlockPos> src = new java.util.ArrayList<>(this.sources);
        List<BlockPos> dst = new java.util.ArrayList<>(this.targets);
        List<BlockPos> list = target ? dst : src;
        if (list.remove(pos)) {
            return new GolemRoute(src, dst, this.collect, this.deliver);
        }
        if (list.size() >= MAX_POINTS) {
            return null;
        }
        (target ? src : dst).remove(pos);
        list.add(pos.immutable());
        return new GolemRoute(src, dst, this.collect, this.deliver);
    }

    public GolemRoute withCollect(Collect collect) {
        return new GolemRoute(this.sources, this.targets, collect, this.deliver);
    }

    public GolemRoute withDeliver(Deliver deliver) {
        return new GolemRoute(this.sources, this.targets, this.collect, deliver);
    }
}
