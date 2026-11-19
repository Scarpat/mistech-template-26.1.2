package com.mistech.mistechmod.refining;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Receita da cadeia de minério (tipo "mistech:ore_refining"). Um tipo só para as três máquinas; o {@code stage} diz qual:
 * <ul>
 * <li>{@code washing}: Lavadora de Essência (minério → 3 Torrões Impregnados)</li>
 * <li>{@code crystallizing}: Cristalizador Rúnico (minério → 4 Cristais de Minério)</li>
 * <li>{@code transmutation}: Prisma Astral (minério → 5 pós)</li>
 * </ul>
 * <pre>
 * { "type": "mistech:ore_refining", "stage": "washing", "ingredient": "#c:ores/iron",
 *   "result": { "id": "mistech:iron_clump", "count": 3 },
 *   "byproducts": [ { "item": { "id": "mistech:tainted_residue" }, "chance": 0.1 } ] }
 * </pre>
 */
public record OreRefiningRecipe(Recipe.CommonInfo commonInfo, Stage stage, Ingredient ingredient, ItemStackTemplate result,
        List<Byproduct> byproducts) implements Recipe<OreRefiningRecipe.Input> {

    public enum Stage implements StringRepresentable {
        WASHING("washing"), CRYSTALLIZING("crystallizing"), TRANSMUTATION("transmutation");

        public static final Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);
        private final String id;

        Stage(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }

    /** Subproduto com chance (Borra, Resíduo, metal do mod). */
    public record Byproduct(ItemStackTemplate item, float chance) {
        public static final Codec<Byproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStackTemplate.CODEC.fieldOf("item").forGetter(Byproduct::item),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Byproduct::chance)).apply(i, Byproduct::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Byproduct> STREAM_CODEC = StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, Byproduct::item,
                ByteBufCodecs.FLOAT, Byproduct::chance,
                Byproduct::new);
    }

    /** Entrada: a máquina diz o estágio, para cada uma achar só as receitas dela. */
    public record Input(Stage stage, ItemStack item) implements net.minecraft.world.item.crafting.RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return this.item;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static final MapCodec<OreRefiningRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(OreRefiningRecipe::commonInfo),
            Stage.CODEC.fieldOf("stage").forGetter(OreRefiningRecipe::stage),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(OreRefiningRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(OreRefiningRecipe::result),
            Byproduct.CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(OreRefiningRecipe::byproducts)
    ).apply(i, OreRefiningRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreRefiningRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, OreRefiningRecipe::commonInfo,
            ByteBufCodecs.idMapper(i -> Stage.values()[i], Stage::ordinal), OreRefiningRecipe::stage,
            Ingredient.CONTENTS_STREAM_CODEC, OreRefiningRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, OreRefiningRecipe::result,
            Byproduct.STREAM_CODEC.apply(ByteBufCodecs.list()), OreRefiningRecipe::byproducts,
            OreRefiningRecipe::new);

    /** Sorteia os subprodutos desta vez. */
    public List<ItemStack> rollByproducts(RandomSource random) {
        List<ItemStack> out = new java.util.ArrayList<>();
        for (Byproduct b : this.byproducts) {
            if (random.nextFloat() < b.chance()) {
                out.add(b.item().create());
            }
        }
        return out;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.stage() == this.stage && this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(Input input) {
        return this.result.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<OreRefiningRecipe> getSerializer() {
        return RefiningContent.SERIALIZER.get();
    }

    @Override
    public RecipeType<OreRefiningRecipe> getType() {
        return RefiningContent.TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
