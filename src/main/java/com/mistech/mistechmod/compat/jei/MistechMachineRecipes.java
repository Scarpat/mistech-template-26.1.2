package com.mistech.mistechmod.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyField;
import com.mistech.mistechmod.beyond.BlazeGeneratorBlockEntity;
import com.mistech.mistechmod.beyond.PaleNetherContent;
import com.mistech.mistechmod.block.CondenserBlockEntity;
import com.mistech.mistechmod.block.EssenceExtractorBlockEntity;
import com.mistech.mistechmod.block.KettleBlockEntity;
import com.mistech.mistechmod.block.MagicCompressorBlockEntity;
import com.mistech.mistechmod.celestial.CelestialContent;
import com.mistech.mistechmod.drink.CaneStillBlockEntity;
import com.mistech.mistechmod.drink.DrinkContent;
import com.mistech.mistechmod.drink.FermentationBarrelBlockEntity;
import com.mistech.mistechmod.item.MagicSmokableItem;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.machine.AshGeneratorBlockEntity;
import com.mistech.mistechmod.machine.GnomeBicycleBlockEntity;
import com.mistech.mistechmod.mage.MagicLoomBlockEntity;
import com.mistech.mistechmod.mage.MageContent;
import com.mistech.mistechmod.power.ExperienceGeneratorBlockEntity;
import com.mistech.mistechmod.power.IgneousBoilerBlockEntity;
import com.mistech.mistechmod.power.ImpureReactorBlockEntity;
import com.mistech.mistechmod.power.PowerContent;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.block.DistillerBlockEntity;
import com.mistech.mistechmod.purity.block.PureGeneratorBlockEntity;
import com.mistech.mistechmod.purity.block.PurityFluids;
import com.mistech.mistechmod.recipe.MagicSmokeRecipe;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MachineContent;
import com.mistech.mistechmod.registry.MistechFluids;
import com.mistech.mistechmod.registry.MurkyWood;
import com.mistech.mistechmod.tarot.TarotContent;
import com.mistech.mistechmod.tarot.TarotField;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/**
 * As "receitas" das máquinas do Mistech para o JEI. Os números vêm das constantes dos próprios blocos, então mudar o
 * balanceamento lá já atualiza o JEI.
 */
final class MistechMachineRecipes {
    private MistechMachineRecipes() {}

    private static String fe(int value) {
        return String.format(java.util.Locale.ROOT, "%,d", value).replace(',', '.');
    }

    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    /** Dias do jogo (24.000 ticks), com uma casa se precisar ("0,5", "1"). */
    private static String days(int ticks) {
        double d = ticks / 24000.0;
        return d == Math.floor(d) ? Integer.toString((int) d) : String.format(java.util.Locale.ROOT, "%.1f", d).replace('.', ',');
    }

    // ----- Compressor Mágico e Cristalizador -----

    static List<MachineRecipe> compressor() {
        String time = seconds(MagicCompressorBlockEntity.HARDEN_TICKS);
        return List.of(
                MachineRecipe.create().in(Mistech.IMPURE_ESSENCE_BOTTLE.get()).out(Mistech.IMPURE_MAGIC_SHARD.get())
                        .note("jei.mistech.compressor.time", time).note("jei.mistech.compressor.incense"),
                MachineRecipe.create().in(PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get()).out(PaleNetherContent.CRYING_SHARD.get())
                        .note("jei.mistech.compressor.time", time).note("jei.mistech.compressor.incense"));
    }

    static List<MachineRecipe> crystallizer() {
        return List.of(MachineRecipe.create().in(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()).out(PurityContent.PURE_MAGIC_SHARD.get())
                .note("jei.mistech.compressor.time", seconds(com.mistech.mistechmod.purity.block.CrystallizerBlockEntity.HARDEN_TICKS))
                .note("jei.mistech.crystallizer.light"));
    }

    // ----- Extrator de Essência -----

    static List<MachineRecipe> extractor() {
        String perLevel = seconds(EssenceExtractorBlockEntity.TICKS_PER_LEVEL);
        int energy = EssenceExtractorBlockEntity.ENERGY_PER_TICK;
        return List.of(
                MachineRecipe.create().in(Items.GLASS_BOTTLE).out(Mistech.IMPURE_ESSENCE_BOTTLE.get())
                        .note("jei.mistech.extractor.level", perLevel, energy).note("jei.mistech.extractor.overworld"),
                MachineRecipe.create().in(Items.BUCKET).out(Mistech.IMPURE_ESSENCE_BUCKET.get())
                        .note("jei.mistech.extractor.full", com.mistech.mistechmod.block.EssenceExtractorBlock.MAX_LEVEL),
                MachineRecipe.create().in(Items.GLASS_BOTTLE).out(PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get())
                        .note("jei.mistech.extractor.level", perLevel, energy).note("jei.mistech.extractor.nether"));
    }

    // ----- Condensador -----

    static List<MachineRecipe> condenser() {
        String time = seconds(CondenserBlockEntity.WORK_TICKS);
        int energy = CondenserBlockEntity.ENERGY_PER_TICK;
        List<MachineRecipe> list = new ArrayList<>();
        list.add(MachineRecipe.create().inFluid(Fluids.WATER, 1000).out(Items.ICE)
                .note("jei.mistech.machine.time_energy", time, energy).note("jei.mistech.condenser.fluid"));
        list.add(MachineRecipe.create().inFluid(Fluids.WATER, 1000).in(Items.ICE).out(Items.PACKED_ICE)
                .note("jei.mistech.machine.time_energy", time, energy).note("jei.mistech.condenser.fluid"));
        list.add(MachineRecipe.create().inFluid(Fluids.WATER, 1000).in(Items.PACKED_ICE).out(Items.BLUE_ICE)
                .note("jei.mistech.machine.time_energy", time, energy).note("jei.mistech.condenser.fluid"));
        list.add(MachineRecipe.create().inFluid(Fluids.LAVA, 1000).out(Items.OBSIDIAN)
                .note("jei.mistech.machine.time_energy", time, energy).note("jei.mistech.condenser.fluid"));
        return list;
    }

    // ----- Destilador -----

    static List<MachineRecipe> distiller() {
        return List.of(MachineRecipe.create()
                .inFluid(MistechFluids.IMPURE_ESSENCE.get(), DistillerBlockEntity.BATCH_IN)
                .in(PurityContent.MITHRIL_FILTER.get())
                .outFluid(MistechFluids.PURIFIED_ESSENCE.get(), DistillerBlockEntity.BATCH_OUT)
                .out(PurityContent.TAINTED_RESIDUE.get())
                .note("jei.mistech.distiller.1", seconds(DistillerBlockEntity.DURATION), DistillerBlockEntity.FE_PER_TICK).note("jei.mistech.distiller.2"));
    }

    // ----- Tear Mágico -----

    static List<MachineRecipe> loom() {
        return List.of(
                MachineRecipe.create().in(Mistech.HEMP_FIBER.get(), MagicLoomBlockEntity.FIBERS).out(MageContent.MAGIC_CLOTH.get())
                        .note("jei.mistech.machine.time_energy", seconds(MagicLoomBlockEntity.WORK_TICKS), MagicLoomBlockEntity.ENERGY_PER_TICK),
                MachineRecipe.create().in(Mistech.HEMP_FIBER.get(), MagicLoomBlockEntity.FIBERS).in(Mistech.AIR_RUNE.get())
                        .out(MageContent.MAGIC_CLOTH.get(), 2)
                        .note("jei.mistech.machine.time_energy", seconds(MagicLoomBlockEntity.WORK_TICKS), MagicLoomBlockEntity.ENERGY_PER_TICK)
                        .note("jei.mistech.machine.rune_use", MagicLoomBlockEntity.RUNE_OPS_PER_USE));
    }

    // ----- Chaleira -----

    static List<MachineRecipe> kettle() {
        int cups = KettleBlockEntity.MAX_WATER;
        return List.of(
                kettle(Mistech.COFFEE_POWDER.get(), Mistech.COFFEE_CUP.get(), cups),
                kettle(Mistech.BELLADONNA_BERRIES.get(), Mistech.BELLADONNA_TEA_CUP.get(), cups),
                kettle(GnomeContent.YELLOW_MUSHROOM_ITEM.get(), com.mistech.mistechmod.registry.DreamContent.MUSHROOM_TEA_CUP.get(), cups));
    }

    private static MachineRecipe kettle(ItemLike ingredient, ItemLike drink, int cups) {
        return MachineRecipe.create().in(new ItemStack(Items.WATER_BUCKET), net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER)).in(ingredient)
                .in(Mistech.CUP.get(), cups).out(drink, cups)
                .note("jei.mistech.kettle.1", seconds(KettleBlockEntity.BREW_TICKS)).note("jei.mistech.kettle.2", cups, KettleBlockEntity.BOTTLE);
    }

    // ----- Cachaça -----

    static List<MachineRecipe> barrel() {
        return List.of(
                MachineRecipe.create().in(DrinkContent.GARAPA.get()).out(DrinkContent.FERMENTED_GARAPA.get())
                        .note("jei.mistech.barrel.time", days(FermentationBarrelBlockEntity.FERMENT_TICKS))
                        .note("jei.mistech.barrel.max", FermentationBarrelBlockEntity.MAX),
                MachineRecipe.create().in(DrinkContent.CACHACA.get()).out(DrinkContent.AGED_CACHACA.get())
                        .note("jei.mistech.barrel.time", days(FermentationBarrelBlockEntity.AGE_TICKS))
                        .note("jei.mistech.barrel.max", FermentationBarrelBlockEntity.MAX));
    }

    static List<MachineRecipe> still() {
        return List.of(MachineRecipe.create().in(DrinkContent.FERMENTED_GARAPA.get()).out(DrinkContent.CACHACA.get())
                .note("jei.mistech.still.time", seconds(CaneStillBlockEntity.WORK_TICKS)).note("jei.mistech.still.heat", CaneStillBlockEntity.ENERGY_PER_TICK, CaneStillBlockEntity.RUNE_OPS_PER_USE));
    }

    // ----- Charutos e Baseados Mágicos (Enroladora e mesa de trabalho) -----

    static List<MachineRecipe> smokes() {
        List<MachineRecipe> list = new ArrayList<>();
        for (RuneElement element : RuneElement.values()) {
            for (boolean tobacco : new boolean[] {true, false}) {
                list.add(MachineRecipe.create()
                        .in(element.rune())
                        .in(tobacco ? Mistech.DRIED_TOBACCO_LEAF.get() : Mistech.DRIED_HEMP_LEAF.get())
                        .in(Mistech.MAGIC_RESIDUE.get())
                        .out(MagicSmokableItem.create(MagicSmokeRecipe.resultItem(tobacco), element, 1))
                        .note("jei.mistech.smoke.rune"));
            }
        }
        return list;
    }

    // ----- Bigorna Rúnica (engastes) -----

    static List<MachineRecipe> runeAnvil() {
        List<ItemStack> gear = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (GearRunes.slots(stack) > 0) {
                gear.add(stack);
            }
        }
        if (gear.isEmpty()) {
            return List.of();
        }
        List<MachineRecipe> list = new ArrayList<>();
        for (RuneElement element : RuneElement.values()) {
            list.add(MachineRecipe.create().in(gear).in(element.rune())
                    .label(Component.translatable("jei.mistech.rune_anvil.socketed"))
                    .note("rune_socket.mistech." + element.id())
                    .note("jei.mistech.rune_anvil.slots"));
        }
        return list;
    }

    // ----- Geradores -----

    static List<MachineRecipe> generators() {
        List<MachineRecipe> list = new ArrayList<>();
        // Gerador de Cinzas
        for (ItemLike fuel : new ItemLike[] {Mistech.INCENSE_ASH.get(), Mistech.ASH_BLOCK_ITEM.get(), Mistech.MAGIC_RESIDUE.get()}) {
            int[] value = AshGeneratorBlockEntity.fuelValue(new ItemStack(fuel));
            list.add(MachineRecipe.create().station(MachineContent.ASH_GENERATOR_ITEM.get()).in(fuel)
                    .note("jei.mistech.generator.fuel", fe(value[0]), value[1]));
        }
        // Gerador de Blaze
        for (ItemLike fuel : new ItemLike[] {Items.BLAZE_ROD, Items.BLAZE_POWDER}) {
            list.add(MachineRecipe.create().station(PaleNetherContent.BLAZE_GENERATOR_ITEM.get()).in(fuel)
                    .note("jei.mistech.generator.fuel", fe(BlazeGeneratorBlockEntity.fuelValue(new ItemStack(fuel))), BlazeGeneratorBlockEntity.BASE_RATE)
                    .note("jei.mistech.generator.blaze_bonus"));
        }
        // Caldeira Ígnea
        int bucketFe = 1000 / IgneousBoilerBlockEntity.LAVA_PER_TICK * IgneousBoilerBlockEntity.BASE_RATE;
        list.add(MachineRecipe.create().station(PowerContent.IGNEOUS_BOILER_ITEM.get()).in(Items.LAVA_BUCKET)
                .note("jei.mistech.generator.fuel", fe(bucketFe), IgneousBoilerBlockEntity.BASE_RATE)
                .note("jei.mistech.generator.boiler_catalyst"));
        for (ItemLike catalyst : new ItemLike[] {Items.BLAZE_ROD, Items.BLAZE_POWDER, PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get()}) {
            list.add(MachineRecipe.create().station(PowerContent.IGNEOUS_BOILER_ITEM.get()).in(catalyst)
                    .note("jei.mistech.generator.catalyst", seconds(IgneousBoilerBlockEntity.catalystTicks(new ItemStack(catalyst)))));
        }
        // Reator Impuro
        for (ItemLike fuel : new ItemLike[] {MachineContent.IMPURE_MAGIC_DUST.get(), Mistech.IMPURE_MAGIC_SHARD.get(), Mistech.IMPURE_MAGIC_BLOCK_ITEM.get()}) {
            list.add(MachineRecipe.create().station(PowerContent.IMPURE_REACTOR_ITEM.get()).in(fuel).out(PurityContent.TAINTED_RESIDUE.get())
                    .note("jei.mistech.generator.fuel", fe(ImpureReactorBlockEntity.solidValue(new ItemStack(fuel))), ImpureReactorBlockEntity.RATE)
                    .note("jei.mistech.generator.reactor_residue", fe(ImpureReactorBlockEntity.RESIDUE_EVERY)));
        }
        list.add(MachineRecipe.create().station(PowerContent.IMPURE_REACTOR_ITEM.get())
                .in(new ItemStack(Mistech.IMPURE_ESSENCE_BOTTLE.get()), new ItemStack(Mistech.IMPURE_ESSENCE_BUCKET.get()))
                .out(PurityContent.TAINTED_RESIDUE.get())
                .note("jei.mistech.generator.per_mb", ImpureReactorBlockEntity.FE_PER_MB, fe(PurityFluids.BOTTLE * ImpureReactorBlockEntity.FE_PER_MB),
                        fe(PurityFluids.BUCKET * ImpureReactorBlockEntity.FE_PER_MB))
                .note("jei.mistech.generator.reactor_residue", fe(ImpureReactorBlockEntity.RESIDUE_EVERY)));
        // Gerador Purificado
        list.add(MachineRecipe.create().station(PurityContent.PURE_GENERATOR_ITEM.get())
                .in(new ItemStack(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()), new ItemStack(PurityContent.PURIFIED_ESSENCE_BUCKET.get()))
                .note("jei.mistech.generator.per_mb", PureGeneratorBlockEntity.FE_PER_TICK, fe(PurityFluids.BOTTLE * PureGeneratorBlockEntity.FE_PER_TICK),
                        fe(PurityFluids.BUCKET * PureGeneratorBlockEntity.FE_PER_TICK)));
        // Gerador de Experiência
        list.add(MachineRecipe.create().station(PowerContent.EXPERIENCE_GENERATOR_ITEM.get())
                .in(new ItemStack(Items.EXPERIENCE_BOTTLE), new ItemStack(Items.ENCHANTED_BOOK))
                .note("jei.mistech.generator.experience", ExperienceGeneratorBlockEntity.FE_PER_POINT, ExperienceGeneratorBlockEntity.RATE)
                .note("jei.mistech.generator.experience_items"));
        // Bicicleta do Gnomo
        for (ItemLike food : new ItemLike[] {Items.APPLE, Items.GOLDEN_APPLE}) {
            int ticks = food == Items.APPLE ? GnomeBicycleBlockEntity.APPLE_TICKS : GnomeBicycleBlockEntity.GOLDEN_APPLE_TICKS;
            list.add(MachineRecipe.create().station(GnomeContent.GNOME_BICYCLE_ITEM.get()).in(food)
                    .note("jei.mistech.generator.fuel", fe(ticks * GnomeBicycleBlockEntity.RATE), GnomeBicycleBlockEntity.RATE)
                    .note("jei.mistech.generator.gnome"));
        }
        // sem combustível
        list.add(MachineRecipe.create().station(MachineContent.ASTRAL_GENERATOR_ITEM.get())
                .note("jei.mistech.generator.astral"));
        list.add(MachineRecipe.create().station(PowerContent.STELLAR_OBSERVATORY_ITEM.get()).in(CelestialContent.STAR_FRAGMENT.get())
                .note("jei.mistech.generator.observatory.1").note("jei.mistech.generator.observatory.2"));
        return list;
    }

    // ----- Multiblocos (Pedra Guia) -----

    static List<MachineRecipe> multiblocks() {
        List<MachineRecipe> list = new ArrayList<>();
        list.add(MachineRecipe.create().station(Mistech.GUIDE_STONE.get())
                .in(Mistech.WRITING_TABLE_ITEM.get()).in(Mistech.SPATIAL_BARREL_ITEM.get()).in(Mistech.RUNIC_TANK_ITEM.get(), 2)
                .label(Component.translatable("block.mistech.rune_confector"))
                .note("jei.mistech.multiblock.confector"));
        list.add(MachineRecipe.create().station(Mistech.GUIDE_STONE.get())
                .in(Mistech.IMPURE_MAGIC_BLOCK_ITEM.get()).in(Mistech.SPATIAL_BARREL_ITEM.get()).in(MachineContent.TUNNELER_ITEM.get(), 4)
                .label(Component.translatable("block.mistech.miner"))
                .note("jei.mistech.multiblock.miner"));
        list.add(MachineRecipe.create().station(Mistech.GUIDE_STONE.get())
                .in(MurkyWood.LOG_ITEM.get()).in(Items.LAPIS_BLOCK).in(Mistech.IMPURE_MAGIC_BLOCK_ITEM.get())
                .out(Mistech.ASTRAL_PILLAR_ITEM.get())
                .note("jei.mistech.multiblock.astral_pillar"));

        int lapis = count(AstrologyField.PATTERN, 'L'), gold = count(AstrologyField.PATTERN, 'O');
        list.add(MachineRecipe.create().station(Mistech.GUIDE_STONE.get())
                .in(Mistech.WRITING_TABLE_ITEM.get()).in(Blocks.LAPIS_BLOCK, lapis).in(Blocks.GOLD_BLOCK, gold)
                .out(Mistech.ASTROLOGY_TABLE_ITEM.get())
                .note("jei.mistech.multiblock.astrology"));

        int amethyst = count(TarotField.PATTERN, 'A'), obsidian = count(TarotField.PATTERN, 'O'), crying = count(TarotField.PATTERN, 'C');
        list.add(MachineRecipe.create().station(Mistech.GUIDE_STONE.get())
                .in(Mistech.WRITING_TABLE_ITEM.get()).in(Blocks.AMETHYST_BLOCK, amethyst).in(Blocks.OBSIDIAN, obsidian)
                .in(Blocks.CRYING_OBSIDIAN, crying)
                .out(TarotContent.TAROT_TABLE_ITEM.get())
                .note("jei.mistech.multiblock.tarot"));
        return list;
    }

    private static int count(String[] pattern, char c) {
        int n = 0;
        for (String row : pattern) {
            for (int i = 0; i < row.length(); i++) {
                if (row.charAt(i) == c) {
                    n++;
                }
            }
        }
        return n;
    }
}
