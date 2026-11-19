package com.mistech.mistechmod.archive.craft;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Receita guardada num Pergaminho de Padrão.
 * <ul>
 * <li>Mesa ({@code crafting}): a receita de mesa de trabalho; {@code output} = o que ela faz.</li>
 * <li>Processamento: {@code input} vai para a máquina em {@code machine}; volta {@code output}.</li>
 * </ul>
 * {@code priority}: quando há mais de um padrão para o mesmo item, o planejador tenta primeiro o de prioridade maior
 * (1 = alta, 0 = normal, −1 = baixa); empate = ordem nas Estantes. Se o primeiro não der (falta ingrediente), tenta o próximo.
 */
public record Pattern(boolean crafting, Optional<ResourceKey<Recipe<?>>> recipe, ItemStack input, ItemStack output, Optional<BlockPos> machine,
        int priority) {
    public Pattern(boolean crafting, Optional<ResourceKey<Recipe<?>>> recipe, ItemStack input, ItemStack output, Optional<BlockPos> machine) {
        this(crafting, recipe, input, output, machine, 0);
    }

    public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("crafting").forGetter(Pattern::crafting),
            ResourceKey.codec(Registries.RECIPE).optionalFieldOf("recipe").forGetter(Pattern::recipe),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("input", ItemStack.EMPTY).forGetter(Pattern::input),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("output", ItemStack.EMPTY).forGetter(Pattern::output),
            BlockPos.CODEC.optionalFieldOf("machine").forGetter(Pattern::machine),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Pattern::priority)).apply(i, Pattern::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Pattern> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, Pattern::crafting,
            ResourceKey.streamCodec(Registries.RECIPE).apply(ByteBufCodecs::optional), Pattern::recipe,
            ItemStack.OPTIONAL_STREAM_CODEC, Pattern::input,
            ItemStack.OPTIONAL_STREAM_CODEC, Pattern::output,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), Pattern::machine,
            ByteBufCodecs.VAR_INT, Pattern::priority,
            Pattern::new);

    public static Pattern crafting(ResourceKey<Recipe<?>> recipe, ItemStack output) {
        return new Pattern(true, Optional.of(recipe), ItemStack.EMPTY, output.copy(), Optional.empty());
    }

    public static Pattern processing(BlockPos machine, ItemStack input) {
        return new Pattern(false, Optional.empty(), input.copy(), ItemStack.EMPTY, Optional.of(machine.immutable()));
    }

    public Pattern withOutput(ItemStack output) {
        return new Pattern(this.crafting, this.recipe, this.input, output.copy(), this.machine, this.priority);
    }

    /** Alta → normal → baixa → alta. */
    public Pattern nextPriority() {
        int p = this.priority >= 1 ? 0 : this.priority == 0 ? -1 : 1;
        return new Pattern(this.crafting, this.recipe, this.input, this.output, this.machine, p);
    }

    /** Pronto para usar (tem saída e, no processamento, entrada e máquina). */
    public boolean complete() {
        if (this.output.isEmpty()) {
            return false;
        }
        return this.crafting ? this.recipe.isPresent() : !this.input.isEmpty() && this.machine.isPresent();
    }

    public static boolean same(Pattern a, Pattern b) {
        return a.crafting == b.crafting && a.recipe.equals(b.recipe) && ItemStack.matches(a.input, b.input)
                && ItemStack.matches(a.output, b.output) && a.machine.equals(b.machine) && a.priority == b.priority;
    }

    public List<ItemStack> describeItems() {
        return this.crafting ? List.of(this.output) : List.of(this.input, this.output);
    }
}
