package com.mistech.mistechmod.ritual;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.registry.RitualContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
 * Ritual do Altar Impuro (tipo "mistech:ritual").
 * <pre>
 * { "type": "mistech:ritual",
 *   "catalyst": "minecraft:feather",
 *   "ingredients": [ "mistech:air_rune", "mistech:darkness_rune", "minecraft:book", "minecraft:rabbit" ],
 *   "essence": 2, "duration": 200, "condition": "night",
 *   "result": { "id": "..." },          (opcional: item que aparece no altar)
 *   "copy_components": true,            (opcional: o resultado herda os componentes do catalisador)
 *   "action": "summon_owl",             (opcional: efeito no mundo, ver RitualActions)
 *   "effect": "shadow" }                (opcional: visual do ritual, ver RitualEffect; padrão "impure")
 * </pre>
 */
public class RitualRecipe implements Recipe<RitualInput> {
    public static final int MAX_INGREDIENTS = 8;
    public static final int MAX_ESSENCE = 8;

    public static final MapCodec<RitualRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(RitualRecipe::commonInfo),
            Ingredient.CODEC.fieldOf("catalyst").forGetter(RitualRecipe::catalyst),
            Ingredient.CODEC.listOf(0, MAX_INGREDIENTS).optionalFieldOf("ingredients", List.of()).forGetter(RitualRecipe::ingredients),
            Codec.intRange(0, MAX_ESSENCE).optionalFieldOf("essence", 1).forGetter(RitualRecipe::essence),
            Codec.intRange(20, 20 * 60 * 5).optionalFieldOf("duration", 200).forGetter(RitualRecipe::duration),
            RitualCondition.CODEC.optionalFieldOf("condition", RitualCondition.NONE).forGetter(RitualRecipe::condition),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(RitualRecipe::result),
            Codec.BOOL.optionalFieldOf("copy_components", false).forGetter(RitualRecipe::copyComponents),
            Codec.STRING.optionalFieldOf("action", "").forGetter(RitualRecipe::action),
            Codec.intRange(1, 3).optionalFieldOf("min_tier", 1).forGetter(RitualRecipe::minTier),
            RitualEffect.CODEC.optionalFieldOf("effect", RitualEffect.IMPURE).forGetter(RitualRecipe::effect),
            Codec.BOOL.optionalFieldOf("sanctuary", false).forGetter(RitualRecipe::sanctuary)
    ).apply(i, RitualRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                Recipe.CommonInfo.STREAM_CODEC.encode(buf, r.commonInfo);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.catalyst);
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)).encode(buf, r.ingredients);
                buf.writeVarInt(r.essence);
                buf.writeVarInt(r.duration);
                buf.writeVarInt(r.condition.ordinal());
                ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buf, r.result);
                buf.writeBoolean(r.copyComponents);
                buf.writeUtf(r.action);
                buf.writeVarInt(r.minTier);
                buf.writeVarInt(r.effect.ordinal());
                buf.writeBoolean(r.sanctuary);
            },
            buf -> new RitualRecipe(
                    Recipe.CommonInfo.STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)).decode(buf),
                    buf.readVarInt(), buf.readVarInt(), RitualCondition.values()[buf.readVarInt()],
                    ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buf),
                    buf.readBoolean(), buf.readUtf(), buf.readVarInt(), RitualEffect.values()[buf.readVarInt()], buf.readBoolean()));

    private final Recipe.CommonInfo commonInfo;
    private final Ingredient catalyst;
    private final List<Ingredient> ingredients;
    private final int essence;
    private final int duration;
    private final RitualCondition condition;
    private final Optional<ItemStackTemplate> result;
    private final boolean copyComponents;
    private final String action;
    private final int minTier;
    private final RitualEffect effect;
    private final boolean sanctuary;

    public RitualRecipe(Recipe.CommonInfo commonInfo, Ingredient catalyst, List<Ingredient> ingredients, int essence, int duration,
                        RitualCondition condition, Optional<ItemStackTemplate> result, boolean copyComponents, String action, int minTier,
                        RitualEffect effect, boolean sanctuary) {
        this.sanctuary = sanctuary;
        this.minTier = minTier;
        this.effect = effect;
        this.commonInfo = commonInfo;
        this.catalyst = catalyst;
        this.ingredients = List.copyOf(ingredients);
        this.essence = essence;
        this.duration = duration;
        this.condition = condition;
        this.result = result;
        this.copyComponents = copyComponents;
        this.action = action;
    }

    public Recipe.CommonInfo commonInfo() { return this.commonInfo; }
    public Ingredient catalyst() { return this.catalyst; }
    public List<Ingredient> ingredients() { return this.ingredients; }
    public int essence() { return this.essence; }
    public int duration() { return this.duration; }
    public RitualCondition condition() { return this.condition; }
    public Optional<ItemStackTemplate> result() { return this.result; }
    public boolean copyComponents() { return this.copyComponents; }
    public String action() { return this.action; }
    /** Tier mínimo do Altar Impuro para este ritual. */
    public int minTier() { return this.minTier; }
    /** Visual do ritual enquanto roda. */
    public RitualEffect effect() { return this.effect; }
    /** Ritual da Fonte do Santuário (magia pura, essência = Essência Purificada) em vez do Altar Impuro. */
    public boolean sanctuary() { return this.sanctuary; }

    @Override
    public boolean matches(RitualInput input, Level level) {
        if (this.sanctuary != input.sanctuary() || !this.catalyst.test(input.catalyst())) {
            return false;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : input.pedestals()) {
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks.size() == this.ingredients.size() && assign(stacks, 0, new boolean[this.ingredients.size()]);
    }

    private boolean assign(List<ItemStack> stacks, int index, boolean[] used) {
        if (index == stacks.size()) {
            return true;
        }
        for (int i = 0; i < this.ingredients.size(); i++) {
            if (!used[i] && this.ingredients.get(i).test(stacks.get(index))) {
                used[i] = true;
                if (assign(stacks, index + 1, used)) {
                    return true;
                }
                used[i] = false;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(RitualInput input) {
        if (this.result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = this.result.get().create();
        if (this.copyComponents && !input.catalyst().isEmpty()) {
            // encantamentos, nome, dano da ferramenta etc. passam para o resultado
            out.applyComponents(input.catalyst().getComponentsPatch());
        }
        if (Boolean.TRUE.equals(out.get(com.mistech.mistechmod.purity.PurityContent.SEALED.get()))) {
            out.setDamageValue(0); // runa selada: volta inteira
        }
        if (out.getItem() instanceof com.mistech.mistechmod.item.mithril.MithrilGear.Gear) {
            // revestido de mitril: a durabilidade da netherite some e ele já sai com a carga da Runa de Energia Mágica
            if (out.isDamageableItem()) {
                out.setDamageValue(0);
            }
            // (subindo de nível, a carga que já tinha continua)
            com.mistech.mistechmod.item.mithril.MithrilGear.setEnergy(out, Math.max(com.mistech.mistechmod.item.mithril.MithrilGear.getEnergy(out),
                    com.mistech.mistechmod.item.mithril.MithrilGear.INITIAL_CHARGE));
        }
        return out;
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
    public RecipeSerializer<RitualRecipe> getSerializer() {
        return RitualContent.RITUAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<RitualRecipe> getType() {
        return RitualContent.RITUAL_TYPE.get();
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
