package com.mistech.mistechmod.machine.arcane;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fornalha Arcana: versão avançada da Fornalha Rúnica. Três linhas independentes (entrada → saída).
 * <ul>
 * <li><b>Com energia</b> ({@link #FE_PER_LANE} FE/t por linha): 1/4 do tempo da fornalha comum, experiência ×1,5 e
 * minérios/brutos têm {@link #ORE_BONUS} de chance de sair um lingote a mais.</li>
 * <li><b>Sem energia</b>: continua funcionando como uma Fornalha Rúnica (3/4 do tempo), sem os bônus.</li>
 * </ul>
 * Faz as receitas da fornalha comum e, se não houver, as do alto-forno (adamantina, oricalco).
 * A experiência acumulada sai em orbes em cima da fornalha.
 * Espaços: 0..2 = entradas, 3..5 = saídas (a entrada i sai na saída 3+i).
 */
public class ArcaneFurnaceBlockEntity extends MachineBlockEntity {
    public static final int LANES = 3;
    public static final int FE_PER_LANE = 15;
    public static final float POWERED_TIME = 0.25F;
    public static final float UNPOWERED_TIME = 0.75F;
    public static final float ORE_BONUS = 0.25F;
    public static final float XP_BONUS = 1.5F;

    private final int[] laneProgress = new int[LANES];
    private final int[] laneTotal = new int[LANES];
    private float storedXp;
    /** Trabalhou com energia no último tick (mostra na tela/Jade). */
    private boolean powered;

    public ArcaneFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneMachines.ARCANE_FURNACE_BE.get(), pos, state, ArcaneMachines.FURNACE_LAYOUT, 400, 0);
    }

    private static boolean isOre(ItemStack stack) {
        return stack.is(net.neoforged.neoforge.common.Tags.Items.ORES) || stack.is(net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean working = false;
        boolean anyPowered = false;
        int shownProgress = 0, shownTotal = 0;
        for (int lane = 0; lane < LANES; lane++) {
            ItemStack input = this.items.get(lane);
            var found = com.mistech.mistechmod.machine.SmeltingLookup.find(level, input);
            if (found.isEmpty()) {
                this.laneProgress[lane] = 0;
                continue;
            }
            var recipe = found.get();
            ItemStack result = recipe.output();
            ItemStack out = this.items.get(LANES + lane);
            int extra = isOre(input) ? 1 : 0;
            boolean fits = out.isEmpty() || ItemStack.isSameItemSameComponents(out, result)
                    && out.getCount() + result.getCount() + extra <= out.getMaxStackSize();
            if (!fits) {
                continue;
            }
            int cost = this.energyStep(FE_PER_LANE);
            boolean lanePowered = cost > 0 && this.hasEnergy(cost);
            if (lanePowered) {
                this.drainEnergy(cost);
                anyPowered = true;
            }
            working = true;
            int base = recipe.blasting() ? recipe.cookingTime() * 2 : recipe.cookingTime();
            this.laneTotal[lane] = Math.max(2, Math.round(base * (lanePowered ? POWERED_TIME : UNPOWERED_TIME)) * 2);
            // sem energia: anda 2 por tick (velocidade normal); com energia: o passo da máquina (gnomo, melhorias)
            this.laneProgress[lane] += lanePowered ? this.workStep() : 2;
            if (this.laneProgress[lane] >= this.laneTotal[lane]) {
                this.laneProgress[lane] = 0;
                input.shrink(1);
                ItemStack made = result.copy();
                if (lanePowered && extra > 0 && level.getRandom().nextFloat() < ORE_BONUS) {
                    made.grow(1);
                }
                if (out.isEmpty()) {
                    this.items.set(LANES + lane, made);
                } else {
                    out.grow(made.getCount());
                }
                this.storedXp += recipe.experience() * (lanePowered ? XP_BONUS : 1.0F);
            }
            if (this.laneProgress[lane] * (long) Math.max(1, shownTotal) >= (long) shownProgress * this.laneTotal[lane]) {
                shownProgress = this.laneProgress[lane];
                shownTotal = this.laneTotal[lane];
            }
        }
        this.powered = anyPowered;
        this.info0 = anyPowered ? 1 : 0;
        this.progress = shownProgress;
        this.maxProgress = shownTotal;
        if (this.storedXp >= 1.0F) {
            int whole = Mth.floor(this.storedXp);
            this.storedXp -= whole;
            ExperienceOrb.award(level, pos.getCenter().add(0, 0.7, 0), whole);
        }
        if (working) {
            this.setChanged();
        }
        this.setLit(level, pos, state, working);
    }

    /** Funis e canos: só itens que a fornalha (ou o alto-forno) sabe fundir. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (!super.canPlaceItemThroughFace(slot, stack, direction)) {
            return false;
        }
        return slot >= LANES || !(this.level instanceof ServerLevel server) || com.mistech.mistechmod.machine.SmeltingLookup.canSmelt(server, stack);
    }

    @Override
    public void addInfo(List<Component> lines) {
        int active = 0;
        for (int lane = 0; lane < LANES; lane++) {
            if (this.laneProgress[lane] > 0) {
                active++;
                lines.add(Component.translatable("info.mistech.lane", lane + 1,
                        this.laneProgress[lane] * 100 / Math.max(1, this.laneTotal[lane])));
            }
        }
        if (active == 0) {
            lines.add(Component.translatable("gui.mistech.idle"));
        } else {
            lines.add(Component.translatable(this.powered ? "info.mistech.arcane_furnace.powered" : "info.mistech.arcane_furnace.unpowered"));
        }
        super.addInfo(lines);
        lines.removeIf(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("info.mistech.progress"));
    }

    @Override
    protected void loadExtra(ValueInput input) {
        int[] p = input.getIntArray("Lanes").orElse(new int[0]);
        for (int i = 0; i < Math.min(p.length, LANES); i++) {
            this.laneProgress[i] = p[i];
        }
        this.storedXp = input.getFloatOr("Xp", 0.0F);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putIntArray("Lanes", this.laneProgress.clone());
        output.putFloat("Xp", this.storedXp);
    }
}
