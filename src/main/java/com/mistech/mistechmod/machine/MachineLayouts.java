package com.mistech.mistechmod.machine;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechMenus;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.VegetationBlock;
import net.neoforged.neoforge.common.SpecialPlantable;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.*;

/** Layouts (espaços e tela) de todas as máquinas genéricas. As texturas seguem as mesmas posições. */
public final class MachineLayouts {
    private MachineLayouts() {}

    /** Triturador: entrada, runa (Terra) e 4 saídas. */
    public static final MachineLayout CRUSHER = MachineLayout.builder("runic_crusher", MistechMenus.RUNIC_CRUSHER::get)
            .slot(40, 25, INPUT)
            .slot(40, 53, RUNE)
            .grid(98, 26, 2, 2, OUTPUT)
            .capacity(10_000)
            .rune(s -> s.is(Mistech.EARTH_RUNE.get()))
            .arrow(66, 34)
            .build();

    /** Gerador de Cinzas: 1 espaço de combustível. info0 = FE/t gerando agora. */
    public static final MachineLayout ASH_GENERATOR = MachineLayout.builder("ash_generator", MistechMenus.ASH_GENERATOR::get)
            .slot(30, 50, FUEL)
            .capacity(20_000)
            .fuel(s -> s.is(MistechTags.ASH_FUELS))
            .flame(31, 32)
            .info(56, 32, menu -> List.of(generating(menu.info(0))))
            .build();

    /**
     * Bicicleta do Gnomo: comida (maçãs) e bateria. info0 = FE/t agora; info1 = situação do gnomo
     * (ver {@link GnomeBicycleBlockEntity#STATUS_NO_GNOME}). A chama mostra quanto falta da comida atual.
     */
    public static final MachineLayout GNOME_BICYCLE = MachineLayout.builder("gnome_bicycle", MistechMenus.GNOME_BICYCLE::get)
            .slot(30, 50, FUEL)
            .slot(118, 35, RUNE)
            .capacity(20_000)
            .fuel(s -> GnomeBicycleBlockEntity.foodTicks(s) > 0)
            .rune(s -> GnomeBicycleBlockEntity.batteryHandler(s) != null)
            .flame(31, 32)
            .info(54, 58, menu -> {
                int status = menu.info(1);
                if (status == GnomeBicycleBlockEntity.STATUS_PEDALING) {
                    return List.of(generating(menu.info(0)));
                }
                return List.of(Component.translatable("gui.mistech.gnome_bicycle.status." + status));
            })
            .build();

    /**
     * Gerador Astral: sem espaços. info0 = FE/t agora; info1 = fase da lua (bits 0-2), situação do céu (bits 3-4:
     * 0 = aberto, 1 = dia, 2 = coberto, 3 = chuva) e bônus da Mesa de Astrologia (bit 5).
     */
    public static final MachineLayout ASTRAL_GENERATOR = MachineLayout.builder("astral_generator", MistechMenus.ASTRAL_GENERATOR::get)
            .capacity(50_000)
            .info(12, 22, menu -> {
                int info = menu.info(1);
                List<Component> lines = new ArrayList<>();
                lines.add(generating(menu.info(0)));
                lines.add(Component.translatable("gui.mistech.astral.moon",
                        Component.translatable("gui.mistech.moon." + (info & 7))));
                lines.add(Component.translatable("gui.mistech.astral.sky." + ((info >> 3) & 3)));
                if ((info & 32) != 0) {
                    lines.add(Component.translatable("gui.mistech.astral.table_bonus"));
                }
                return lines;
            })
            .build();

    /** Colheitadeira: runa (Espaço) e 9 saídas. info0 = lado da área. */
    public static final MachineLayout HARVESTER = MachineLayout.builder("runic_harvester", MistechMenus.RUNIC_HARVESTER::get)
            .slot(26, 35, RUNE)
            .grid(62, 17, 3, 3, OUTPUT)
            .capacity(10_000)
            .rune(s -> s.is(Mistech.SPACE_RUNE.get()))
            .info(10, 60, menu -> List.of(area(menu.info(0))))
            .build();

    /** Plantadeira: runa (Água ou Tempo) e 9 entradas de sementes. info0 = lado da área. */
    public static final MachineLayout PLANTER = MachineLayout.builder("runic_planter", MistechMenus.RUNIC_PLANTER::get)
            .slot(26, 35, RUNE)
            .grid(62, 17, 3, 3, INPUT)
            .capacity(10_000)
            .input(MachineLayouts::isPlantable)
            .rune(s -> s.is(Mistech.WATER_RUNE.get()) || s.is(Mistech.TIME_RUNE.get()))
            .info(10, 60, menu -> List.of(area(menu.info(0))))
            .build();

    /**
     * Mineradora: 4 runas em linha e filtro de lixo 3×3 (espaços fantasmas). info0/info1: ver {@link MinerBlockEntity}.
     * As linhas mostram área, camada e situação; os detalhes aparecem na dica da {@link com.mistech.mistechmod.client.MinerScreen}.
     */
    public static final MachineLayout MINER = MachineLayout.builder("miner", MistechMenus.MINER::get)
            .grid(8, 17, MinerBlockEntity.RUNE_SLOTS, 1, RUNE)
            .grid(94, 17, 3, 3, FILTER)
            .capacity(MinerBlockEntity.CAPACITY)
            .rune(MinerBlockEntity::isMinerRune)
            .info(8, 41, menu -> {
                int info = menu.info(0);
                int side = ((info >> 3) & 7) * 2 + 1;
                int status = info & 7;
                return List.of(
                        Component.translatable("gui.mistech.miner.area", side, side),
                        Component.translatable("gui.mistech.miner.layer", menu.info(1)),
                        Component.translatable("gui.mistech.miner.status." + status)
                                .withStyle(status == MinerBlockEntity.STATUS_WORKING ? net.minecraft.ChatFormatting.GREEN
                                        : status == MinerBlockEntity.STATUS_FINISHED ? net.minecraft.ChatFormatting.AQUA
                                        : net.minecraft.ChatFormatting.GOLD));
            })
            .build();

    /** Bateria Rúnica: espaço de carga. info0 = FE/t entrando, info1 = FE/t saindo. */
    public static final MachineLayout RUNIC_BATTERY = MachineLayout.builder("runic_battery", MistechMenus.RUNIC_BATTERY::get)
            .slot(126, 53, RUNE)
            .capacity(RunicBatteryBlockEntity.CAPACITY)
            .rune(s -> GnomeBicycleBlockEntity.batteryHandler(s) != null)
            .info(10, 22, menu -> List.of(
                    Component.translatable("gui.mistech.battery.in", menu.info(0)),
                    Component.translatable("gui.mistech.battery.out", menu.info(1)),
                    Component.translatable("gui.mistech.battery.front").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)))
            .build();

    /** Bateria Rúnica Aprimorada: espaço de carga. info0/1 = FE/t entrando/saindo, info2 = geradores, info3 = máquinas. */
    public static final MachineLayout ADVANCED_RUNIC_BATTERY = MachineLayout.builder("advanced_runic_battery", MistechMenus.ADVANCED_RUNIC_BATTERY::get)
            .slot(126, 53, RUNE)
            .capacity(AdvancedRunicBatteryBlockEntity.CAPACITY)
            .rune(s -> GnomeBicycleBlockEntity.batteryHandler(s) != null)
            .info(10, 22, menu -> List.of(
                    Component.translatable("gui.mistech.battery.in", menu.info(0)),
                    Component.translatable("gui.mistech.battery.out", menu.info(1)),
                    Component.translatable("gui.mistech.battery.generators", menu.info(2)),
                    Component.translatable("gui.mistech.battery.machines", menu.info(3))))
            .build();

    public static boolean isPlantable(ItemStack stack) {
        if (stack.getItem() instanceof SpecialPlantable) {
            return true;
        }
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof VegetationBlock;
    }

    private static Component generating(int fePerTick) {
        return fePerTick > 0
                ? Component.translatable("gui.mistech.generating", fePerTick)
                : Component.translatable("gui.mistech.idle");
    }

    private static Component area(int side) {
        return Component.translatable("gui.mistech.area", side, side);
    }
}
