package com.mistech.mistechmod.registry;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components (dados guardados no ItemStack) do Mistech. */
public final class MistechComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    /** Presente (true) quando o incenso está aceso. Sincronizado com o cliente para trocar o modelo. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> INCENSE_LIT =
            DATA_COMPONENTS.registerComponentType("incense_lit",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Segundos de queima restantes. Só existe em incensos que já foram acesos alguma vez. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> INCENSE_BURN =
            DATA_COMPONENTS.registerComponentType("incense_burn",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Fluido guardado no item do Tanque Rúnico (vem do bloco ao quebrar). */
    // SimpleFluidContent (e não FluidStack): componentes precisam de equals/hashCode e ser imutáveis no 26.1
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent>> TANK_FLUID =
            DATA_COMPONENTS.registerComponentType("tank_fluid",
                    builder -> builder.persistent(net.neoforged.neoforge.fluids.SimpleFluidContent.CODEC)
                            .networkSynchronized(net.neoforged.neoforge.fluids.SimpleFluidContent.STREAM_CODEC));

    /** Runa usada num charuto/baseado mágico ("fire", "water"...). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SMOKE_RUNE =
            DATA_COMPONENTS.registerComponentType("smoke_rune",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Bicho preso no Pote com Criatura (galinha, vaca, ovelha, lula...). Vem do bloco ao quebrar. */
    public static final Codec<net.minecraft.world.item.component.TypedEntityData<net.minecraft.world.entity.EntityType<?>>> JAR_CREATURE_CODEC =
            net.minecraft.world.item.component.TypedEntityData.codec(net.minecraft.world.entity.EntityType.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.TypedEntityData<net.minecraft.world.entity.EntityType<?>>>> JAR_CREATURE =
            DATA_COMPONENTS.registerComponentType("jar_creature",
                    builder -> builder.persistent(JAR_CREATURE_CODEC)
                            .networkSynchronized(net.minecraft.world.item.component.TypedEntityData.streamCodec(net.minecraft.world.entity.EntityType.STREAM_CODEC)));

    /** Energia (FE) guardada no item das Baterias Rúnicas (vem do bloco ao quebrar). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_ENERGY =
            DATA_COMPONENTS.registerComponentType("battery_energy",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Bateria de Bolso ligada (padrão: ligada quando ausente). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> POCKET_BATTERY_ACTIVE =
            DATA_COMPONENTS.registerComponentType("pocket_battery_active",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // ----- Cajado do Gnomo -----

    /** Energia (FE) guardada na bateria do cajado. */
    /** Energia do equipamento revestido de mitril. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GEAR_ENERGY =
            DATA_COMPONENTS.registerComponentType("gear_energy",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Runas engastadas no equipamento revestido (Bigorna Rúnica). Posição = engaste. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> GEAR_RUNES =
            DATA_COMPONENTS.registerComponentType("gear_runes",
                    builder -> builder.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                            .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));

    /** Constelação vinculada ao equipamento de oricalco (índice de Constellations.ALL). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GEAR_CONSTELLATION =
            DATA_COMPONENTS.registerComponentType("gear_constellation",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STAFF_ENERGY =
            DATA_COMPONENTS.registerComponentType("staff_energy",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Runas colocadas no cajado ("fire", "water"...), na ordem em que entraram. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<String>>> STAFF_RUNES =
            DATA_COMPONENTS.registerComponentType("staff_runes",
                    builder -> builder.persistent(Codec.STRING.listOf())
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    /** Runa escolhida: 0 = nenhuma (modo padrão), 1..n = posição na lista de runas. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STAFF_SELECTED =
            DATA_COMPONENTS.registerComponentType("staff_selected",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // ----- Vara de Comando (Golem de Barro) -----
    /** Vara de Comando Rúnica: origens, destinos e modos. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.mistech.mistechmod.golem.GolemRoute>> GOLEM_ROUTE =
            DATA_COMPONENTS.registerComponentType("golem_route",
                    builder -> builder.persistent(com.mistech.mistechmod.golem.GolemRoute.CODEC)
                            .networkSynchronized(com.mistech.mistechmod.golem.GolemRoute.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> GOLEM_SOURCE =
            DATA_COMPONENTS.registerComponentType("golem_source",
                    builder -> builder.persistent(net.minecraft.core.BlockPos.CODEC).networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> GOLEM_TARGET =
            DATA_COMPONENTS.registerComponentType("golem_target",
                    builder -> builder.persistent(net.minecraft.core.BlockPos.CODEC).networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));

    /** Constelação gravada no Amuleto de Constelação (índice em Constellations.ALL). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMULET_CONSTELLATION =
            DATA_COMPONENTS.registerComponentType("amulet_constellation",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // ----- Cachimbo -----

    /** Erva carregada no cachimbo: "tobacco" ou "hemp". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PIPE_HERB =
            DATA_COMPONENTS.registerComponentType("pipe_herb",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Quantas folhas secas ainda há no cachimbo (contando a que está queimando). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PIPE_LEAVES =
            DATA_COMPONENTS.registerComponentType("pipe_leaves",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** O que está escrito num Mapa Astral. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.mistech.mistechmod.astrology.NatalChartCopy>> NATAL_CHART_COPY =
            DATA_COMPONENTS.registerComponentType("natal_chart_copy",
                    builder -> builder.persistent(com.mistech.mistechmod.astrology.NatalChartCopy.CODEC)
                            .networkSynchronized(com.mistech.mistechmod.astrology.NatalChartCopy.STREAM_CODEC));

    private MistechComponents() {}

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
