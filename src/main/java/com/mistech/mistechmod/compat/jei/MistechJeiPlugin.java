package com.mistech.mistechmod.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.compat.ClientRecipes;
import com.mistech.mistechmod.folklore.FolkloreContent;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.recipe.CrushingRecipe;
import com.mistech.mistechmod.recipe.WritingRecipe;
import com.mistech.mistechmod.registry.MachineContent;
import com.mistech.mistechmod.registry.MistechRecipes;
import com.mistech.mistechmod.registry.RitualContent;
import com.mistech.mistechmod.ritual.RitualRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Integração com o JEI: Mesa de Escrita, Triturador Rúnico, Rituais do Altar Impuro e da Fonte do Santuário, e páginas de
 * informação de alguns itens. As receitas chegam do servidor por {@link com.mistech.mistechmod.compat.RecipeSync}.
 * Esta classe só é carregada pelo JEI (o Mistech não depende dele).
 */
@JeiPlugin
public class MistechJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(Mistech.MODID, "jei_plugin");


    /** Máquinas sem receita de jogo: id, título, ícone, estações, receitas. */
    private record Machine(IRecipeType<MachineRecipe> type, String title, Supplier<? extends net.minecraft.world.level.ItemLike> icon,
                           Supplier<List<? extends net.minecraft.world.level.ItemLike>> stations, Supplier<List<MachineRecipe>> recipes) {}

    private static IRecipeType<MachineRecipe> machineType(String path) {
        return IRecipeType.create(Mistech.MODID, path, MachineRecipe.class);
    }



    /**
     * Tudo que toca nos itens/blocos do mod fica aqui dentro: o JEI cria o plugin enquanto os mods ainda estão sendo
     * construídos (em outra thread), e carregar as classes de conteúdo nessa hora fazia registros se perderem
     * (crash "unbound value: mistech:magic_cloth"). Esta classe só carrega quando o JEI pede as categorias.
     */
    private static final class Lazy {
        public static final Supplier<IRecipeHolderType<WritingRecipe>> WRITING = IRecipeHolderType.createDeferred(MistechRecipes.WRITING_TYPE);
        public static final Supplier<IRecipeHolderType<CrushingRecipe>> CRUSHING = IRecipeHolderType.createDeferred(MistechRecipes.CRUSHING_TYPE);
        public static final IRecipeType<RecipeHolder<RitualRecipe>> IMPURE_RITUAL = ritualType("impure_ritual");
        public static final IRecipeType<RecipeHolder<RitualRecipe>> SANCTUARY_RITUAL = ritualType("sanctuary_ritual");

        static final List<Machine> MACHINES = List.of(
                new Machine(machineType("magic_compressor"), "block.mistech.magic_compressor", Mistech.MAGIC_COMPRESSOR_ITEM,
                        () -> List.of(Mistech.MAGIC_COMPRESSOR_ITEM.get(), com.mistech.mistechmod.machine.arcane.ArcaneMachines.ARCANE_COMPRESSOR_ITEM.get()), MistechMachineRecipes::compressor),
                new Machine(machineType("crystallizer"), "block.mistech.crystallizer", PurityContent.CRYSTALLIZER_ITEM,
                        () -> List.of(PurityContent.CRYSTALLIZER_ITEM.get()), MistechMachineRecipes::crystallizer),
                new Machine(machineType("essence_extractor"), "block.mistech.essence_extractor", Mistech.ESSENCE_EXTRACTOR_ITEM,
                        () -> List.of(Mistech.ESSENCE_EXTRACTOR_ITEM.get(), com.mistech.mistechmod.machine.arcane.ArcaneMachines.ARCANE_EXTRACTOR_ITEM.get()), MistechMachineRecipes::extractor),
                new Machine(machineType("condenser"), "block.mistech.condenser", Mistech.CONDENSER_ITEM,
                        () -> List.of(Mistech.CONDENSER_ITEM.get()), MistechMachineRecipes::condenser),
                new Machine(machineType("distiller"), "block.mistech.distiller", PurityContent.DISTILLER_ITEM,
                        () -> List.of(PurityContent.DISTILLER_ITEM.get()), MistechMachineRecipes::distiller),
                new Machine(machineType("magic_loom"), "block.mistech.magic_loom", com.mistech.mistechmod.mage.MageContent.MAGIC_LOOM_ITEM,
                        () -> List.of(com.mistech.mistechmod.mage.MageContent.MAGIC_LOOM_ITEM.get()), MistechMachineRecipes::loom),
                new Machine(machineType("kettle"), "block.mistech.kettle", Mistech.KETTLE_ITEM,
                        () -> List.of(Mistech.KETTLE_ITEM.get()), MistechMachineRecipes::kettle),
                new Machine(machineType("fermentation_barrel"), "block.mistech.fermentation_barrel",
                        com.mistech.mistechmod.drink.DrinkContent.FERMENTATION_BARREL_ITEM,
                        () -> List.of(com.mistech.mistechmod.drink.DrinkContent.FERMENTATION_BARREL_ITEM.get()), MistechMachineRecipes::barrel),
                new Machine(machineType("cane_still"), "block.mistech.cane_still", com.mistech.mistechmod.drink.DrinkContent.CANE_STILL_ITEM,
                        () -> List.of(com.mistech.mistechmod.drink.DrinkContent.CANE_STILL_ITEM.get()), MistechMachineRecipes::still),
                new Machine(machineType("magic_smoke"), "jei.mistech.category.magic_smoke", Mistech.RUNE_ROLLER_ITEM,
                        () -> List.of(Mistech.RUNE_ROLLER_ITEM.get(), net.minecraft.world.item.Items.CRAFTING_TABLE), MistechMachineRecipes::smokes),
                new Machine(machineType("rune_anvil"), "block.mistech.rune_anvil", com.mistech.mistechmod.registry.MithrilContent.RUNE_ANVIL_ITEM,
                        () -> List.of(com.mistech.mistechmod.registry.MithrilContent.RUNE_ANVIL_ITEM.get()), MistechMachineRecipes::runeAnvil),
                new Machine(machineType("generators"), "jei.mistech.category.generators", MachineContent.ASH_GENERATOR_ITEM,
                        () -> List.of(MachineContent.ASH_GENERATOR_ITEM.get(), com.mistech.mistechmod.beyond.PaleNetherContent.BLAZE_GENERATOR_ITEM.get(),
                                com.mistech.mistechmod.power.PowerContent.IGNEOUS_BOILER_ITEM.get(), com.mistech.mistechmod.power.PowerContent.IMPURE_REACTOR_ITEM.get(),
                                PurityContent.PURE_GENERATOR_ITEM.get(), com.mistech.mistechmod.power.PowerContent.EXPERIENCE_GENERATOR_ITEM.get(),
                                com.mistech.mistechmod.registry.GnomeContent.GNOME_BICYCLE_ITEM.get(), MachineContent.ASTRAL_GENERATOR_ITEM.get(),
                                com.mistech.mistechmod.power.PowerContent.STELLAR_OBSERVATORY_ITEM.get()),
                        MistechMachineRecipes::generators),
                new Machine(machineType("multiblocks"), "jei.mistech.category.multiblocks", Mistech.GUIDE_STONE,
                        () -> List.of(Mistech.GUIDE_STONE.get()), MistechMachineRecipes::multiblocks));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IRecipeType<RecipeHolder<RitualRecipe>> ritualType(String path) {
        return IRecipeType.create(Mistech.MODID, path, (Class) RecipeHolder.class);
    }

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        // charutos/baseados mágicos: cada runa é um item diferente no JEI
        registration.registerFromDataComponentTypes(Mistech.MAGIC_CIGAR.get(), com.mistech.mistechmod.registry.MistechComponents.SMOKE_RUNE.get());
        registration.registerFromDataComponentTypes(Mistech.MAGIC_JOINT.get(), com.mistech.mistechmod.registry.MistechComponents.SMOKE_RUNE.get());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new WritingCategory(Lazy.WRITING.get(), gui),
                new CrushingCategory(Lazy.CRUSHING.get(), gui),
                new RitualCategory(Lazy.IMPURE_RITUAL, gui, false, RitualContent.IMPURE_ALTAR_ITEM.get(), "block.mistech.impure_altar"),
                new RitualCategory(Lazy.SANCTUARY_RITUAL, gui, true, PurityContent.SANCTUARY_FONT_ITEM.get(), "block.mistech.sanctuary_font"));
        for (Machine machine : Lazy.MACHINES) {
            registration.addRecipeCategories(new MachineCategory(machine.type(), gui, machine.title(), machine.icon().get()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeMap recipes = ClientRecipes.get();
        registration.addRecipes(Lazy.WRITING.get(), List.copyOf(recipes.byType(MistechRecipes.WRITING_TYPE.get())));
        registration.addRecipes(Lazy.CRUSHING.get(), List.copyOf(recipes.byType(MistechRecipes.CRUSHING_TYPE.get())));

        List<RecipeHolder<RitualRecipe>> impure = new ArrayList<>();
        List<RecipeHolder<RitualRecipe>> sanctuary = new ArrayList<>();
        for (RecipeHolder<RitualRecipe> ritual : recipes.byType(RitualContent.RITUAL_TYPE.get())) {
            (ritual.value().sanctuary() ? sanctuary : impure).add(ritual);
        }
        registration.addRecipes(Lazy.IMPURE_RITUAL, impure);
        registration.addRecipes(Lazy.SANCTUARY_RITUAL, sanctuary);

        for (Machine machine : Lazy.MACHINES) {
            List<MachineRecipe> list = machine.recipes().get();
            for (int i = 0; i < list.size(); i++) {
                list.get(i).id = machine.type().getUid().withSuffix("/" + i);
            }
            registration.addRecipes(machine.type(), list);
        }

        // informações de itens que não saem de receita
        registration.addIngredientInfo(Mistech.INCENSE_ASH.get(), Component.translatable("jei.mistech.info.incense_ash"));
        registration.addIngredientInfo(Mistech.MAGIC_RESIDUE.get(), Component.translatable("jei.mistech.info.magic_residue"));
        registration.addIngredientInfo(FolkloreContent.WEREWOLF_FANG.get(), Component.translatable("jei.mistech.info.werewolf_fang"));
        registration.addIngredientInfo(FolkloreContent.SILVER_INCENSE.get(), Component.translatable("guide.mistech.folklore.silver_incense"));
        registration.addIngredientInfo(Mistech.GUIDE_STONE.get(), Component.translatable("jei.mistech.info.guide_stone"));
        registration.addItemStackInfo(List.of(new net.minecraft.world.item.ItemStack(com.mistech.mistechmod.infuser.InfuserContent.ARCANE_INFUSER_ITEM.get()),
                        new net.minecraft.world.item.ItemStack(com.mistech.mistechmod.infuser.InfuserContent.GREATER_ARCANE_INFUSER_ITEM.get()),
                        new net.minecraft.world.item.ItemStack(com.mistech.mistechmod.infuser.InfuserContent.ASTRAL_ARCANE_INFUSER_ITEM.get())),
                Component.translatable("guide.mistech.arcane_infuser.1"), Component.translatable("guide.mistech.arcane_infuser.2"),
                Component.translatable("guide.mistech.arcane_infuser.3"));
        registration.addIngredientInfo(com.mistech.mistechmod.utility.UtilityContent.RUNIC_HOOKAH_ITEM.get(), Component.translatable("guide.mistech.runic_hookah.1"),
                Component.translatable("guide.mistech.runic_hookah.2"), Component.translatable("guide.mistech.runic_hookah.3"));
        registration.addIngredientInfo(com.mistech.mistechmod.registry.GnomeContent.BOTTLED_GNOME_ITEM.get(), Component.translatable("jei.mistech.info.bottled_gnome"));
        registration.addIngredientInfo(com.mistech.mistechmod.husbandry.HusbandryContent.GROWING_POT_ITEM.get(), Component.translatable("jei.mistech.info.growing_pot"));
        registration.addIngredientInfo(com.mistech.mistechmod.husbandry.HusbandryContent.CREATURE_JAR_ITEM.get(), Component.translatable("jei.mistech.info.creature_jar"));
        registration.addIngredientInfo(com.mistech.mistechmod.utility.UtilityContent.HAMMOCK_ITEM.get(), Component.translatable("jei.mistech.info.hammock"));
        registration.addIngredientInfo(com.mistech.mistechmod.garden.GardenContent.MANDRAKE_SEEDS.get(), Component.translatable("jei.mistech.info.mandrake_seeds"));
        registration.addIngredientInfo(com.mistech.mistechmod.garden.GardenContent.MANDRAKE.get(), Component.translatable("jei.mistech.info.mandrake_root"));
        registration.addIngredientInfo(com.mistech.mistechmod.garden.GardenContent.BOTTLED_MANDRAKE_ITEM.get(), Component.translatable("jei.mistech.info.bottled_mandrake"));
        registration.addIngredientInfo(com.mistech.mistechmod.Mistech.GRINDER.get(), Component.translatable("jei.mistech.info.grinder"));
        registration.addIngredientInfo(MachineContent.TUNNELER_ITEM.get(), Component.translatable("jei.mistech.info.tunneler"));
        registration.addIngredientInfo(com.mistech.mistechmod.lore.LoreContent.CUCA_CAULDRON_ITEM.get(), Component.translatable("jei.mistech.info.cuca_cauldron"));
        registration.addIngredientInfo(Mistech.RUNIC_ASHTRAY_ITEM.get(), Component.translatable("jei.mistech.info.runic_ashtray"));
        registration.addIngredientInfo(com.mistech.mistechmod.machine.arcane.ArcaneMachines.LIQUEFIER_ITEM.get(), Component.translatable("guide.mistech.liquefier.1"));
        registration.addIngredientInfo(com.mistech.mistechmod.machine.forge.ForgeContent.CRUSHER_FORGE_ITEM.get(),
                Component.translatable("guide.mistech.crusher_forge.1"), Component.translatable("guide.mistech.crusher_forge.2"));
        registration.addIngredientInfo(com.mistech.mistechmod.garden.GardenContent.MANDRAKE_POWDER.get(), Component.translatable("item.mistech.mandrake_powder.desc"));
        registration.addIngredientInfo(Mistech.BELLADONNA_BERRIES.get(), Component.translatable("guide.mistech.belladonna_uses.1"));
        registration.addIngredientInfo(Mistech.COFFEE_CUP.get(), Component.translatable("guide.mistech.coffee_uses.1"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(Lazy.WRITING.get(), Mistech.WRITING_TABLE_ITEM.get());
        registration.addCraftingStation(Lazy.CRUSHING.get(), MachineContent.RUNIC_CRUSHER_ITEM.get(),
                com.mistech.mistechmod.machine.arcane.ArcaneMachines.ARCANE_CRUSHER_ITEM.get(),
                com.mistech.mistechmod.machine.forge.ForgeContent.CRUSHER_FORGE_ITEM.get());
        registration.addCraftingStation(Lazy.IMPURE_RITUAL, RitualContent.IMPURE_ALTAR_ITEM.get(), RitualContent.IMPURE_ALTAR_2_ITEM.get(),
                RitualContent.IMPURE_ALTAR_3_ITEM.get(), RitualContent.RITUAL_PEDESTAL_ITEM.get());
        registration.addCraftingStation(Lazy.SANCTUARY_RITUAL, PurityContent.SANCTUARY_FONT_ITEM.get());
        // a Fornalha Rúnica faz as receitas da fornalha comum
        registration.addCraftingStation(mezz.jei.api.constants.RecipeTypes.SMELTING, Mistech.RUNIC_FURNACE_ITEM.get(),
                com.mistech.mistechmod.machine.arcane.ArcaneMachines.ARCANE_FURNACE_ITEM.get(),
                com.mistech.mistechmod.machine.forge.ForgeContent.CRUSHER_FORGE_ITEM.get());
        // as fornalhas do Mistech também fazem as receitas do alto-forno (minérios de adamantina e oricalco)
        registration.addCraftingStation(mezz.jei.api.constants.RecipeTypes.BLASTING, Mistech.RUNIC_FURNACE_ITEM.get(),
                com.mistech.mistechmod.machine.arcane.ArcaneMachines.ARCANE_FURNACE_ITEM.get(),
                com.mistech.mistechmod.machine.forge.ForgeContent.CRUSHER_FORGE_ITEM.get());
        for (Machine machine : Lazy.MACHINES) {
            registration.addCraftingStation(machine.type(), machine.stations().get().toArray(net.minecraft.world.level.ItemLike[]::new));
        }
    }

    /** Botão "+" do JEI: passa os ingredientes do inventário para a máquina. */
    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        // Mesa de Escrita: papel + 4 da cruz (espaços 1..5); o giz fica onde está
        registration.addRecipeTransferHandler(com.mistech.mistechmod.menu.WritingTableMenu.class,
                com.mistech.mistechmod.registry.MistechMenus.WRITING_TABLE.get(), Lazy.WRITING.get(),
                com.mistech.mistechmod.menu.WritingTableMenu.PAPER, 5, com.mistech.mistechmod.menu.WritingTableMenu.RESULT + 1, 36);
        // Triturador Rúnico: entrada no espaço 0
        registration.addRecipeTransferHandler(com.mistech.mistechmod.machine.MachineMenu.class,
                com.mistech.mistechmod.registry.MistechMenus.RUNIC_CRUSHER.get(), Lazy.CRUSHING.get(),
                0, 1, com.mistech.mistechmod.machine.MachineLayouts.CRUSHER.size(), 36);
        // Fornalha Rúnica e Fornalha Arcana: receitas da fornalha comum
        registration.addRecipeTransferHandler(com.mistech.mistechmod.menu.RunicFurnaceMenu.class,
                com.mistech.mistechmod.registry.MistechMenus.RUNIC_FURNACE.get(), mezz.jei.api.constants.RecipeTypes.SMELTING, 0, 1, 2, 36);
        registration.addRecipeTransferHandler(com.mistech.mistechmod.machine.MachineMenu.class,
                com.mistech.mistechmod.machine.arcane.ArcaneMachines.FURNACE_MENU.get(), mezz.jei.api.constants.RecipeTypes.SMELTING,
                0, 1, com.mistech.mistechmod.machine.arcane.ArcaneMachines.FURNACE_LAYOUT.size(), 36);
        registration.addRecipeTransferHandler(com.mistech.mistechmod.menu.RunicFurnaceMenu.class,
                com.mistech.mistechmod.registry.MistechMenus.RUNIC_FURNACE.get(), mezz.jei.api.constants.RecipeTypes.BLASTING, 0, 1, 2, 36);
        registration.addRecipeTransferHandler(com.mistech.mistechmod.machine.MachineMenu.class,
                com.mistech.mistechmod.machine.arcane.ArcaneMachines.FURNACE_MENU.get(), mezz.jei.api.constants.RecipeTypes.BLASTING,
                0, 1, com.mistech.mistechmod.machine.arcane.ArcaneMachines.FURNACE_LAYOUT.size(), 36);
        // Triturador Arcano e Forja Trituradora: primeira entrada
        registration.addRecipeTransferHandler(com.mistech.mistechmod.machine.MachineMenu.class,
                com.mistech.mistechmod.machine.arcane.ArcaneMachines.CRUSHER_MENU.get(), Lazy.CRUSHING.get(),
                0, 1, com.mistech.mistechmod.machine.arcane.ArcaneMachines.CRUSHER_LAYOUT.size(), 36);
        registration.addRecipeTransferHandler(com.mistech.mistechmod.machine.MachineMenu.class,
                com.mistech.mistechmod.machine.forge.ForgeContent.FORGE_MENU.get(), Lazy.CRUSHING.get(),
                0, 1, com.mistech.mistechmod.machine.forge.ForgeContent.FORGE_LAYOUT.size(), 36);
    }

    /** As abas de configuração das máquinas ficam fora da tela: o JEI desvia delas. */
    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(com.mistech.mistechmod.client.MachineScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.mistech.mistechmod.client.MachineScreen>() {
                    @Override
                    public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(com.mistech.mistechmod.client.MachineScreen screen) {
                        return screen.configAreas();
                    }
                });
    }
}
