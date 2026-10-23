package com.mistech.mistechmod.machine.forge;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.SmeltingLookup;
import com.mistech.mistechmod.machine.arcane.ArcaneCrusherBlockEntity;
import com.mistech.mistechmod.recipe.CrushingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Núcleo da Forja Trituradora ({@link CrusherForgeStructure}): tritura e funde de uma vez.
 * Minério → pó (com a Runa da Terra embutida: +1 nas receitas de minério, mais {@link #EXTRA_CHANCE} de outro) → lingote.
 * Itens sem receita de trituração mas com receita de forno são só fundidos. Tudo isso em {@link #WORK_TICKS} ticks por item,
 * 3 linhas ao mesmo tempo, {@link #FE_PER_LANE} FE/t por linha. Só funciona com o cubo completo.
 * Espaços: 0..2 = entradas, 3..8 = saídas. info0 = estrutura (1 = completa).
 */
public class CrusherForgeBlockEntity extends MachineBlockEntity {
    public static final int LANES = 3;
    public static final int WORK_TICKS = 80;
    public static final int FE_PER_LANE = 60;
    public static final float EXTRA_CHANCE = 0.25F;

    private final int[] laneProgress = new int[LANES];
    private boolean formed;
    private int checkTimer;
    private float storedXp;
    private @Nullable RecipeHolder<CrushingRecipe> lastRecipe;
    /** Só no cliente: giro das mós (graus) e o instante da última conta (ver CrusherForgeRenderer). */
    public float clientSpin;
    public double clientSpinClock;

    public CrusherForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ForgeContent.CRUSHER_FORGE_BE.get(), pos, state, ForgeContent.FORGE_LAYOUT, 1000, 0);
        this.maxProgress = WORK_TICKS * 2;
    }

    private Direction front(BlockState state) {
        return state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    public boolean isFormed() {
        return this.formed;
    }

    /** O que sai de um item: lingotes (ou o pó/item, se não der para fundir) e a experiência. */
    private record Product(ItemStack stack, float xp) {}

    private @Nullable Product product(ServerLevel level, ItemStack input, boolean roll) {
        Optional<RecipeHolder<CrushingRecipe>> crush = ArcaneCrusherBlockEntity.recipe(level, input, this.lastRecipe);
        if (crush.isPresent()) {
            this.lastRecipe = crush.get();
            CrushingRecipe recipe = crush.get().value();
            ItemStack dust = recipe.result().create();
            if (recipe.runeBonus()) {
                dust.grow(1);
            }
            if (roll) {
                if (recipe.bonusChance() > 0 && level.getRandom().nextFloat() < recipe.bonusChance()) {
                    dust.grow(1);
                }
                if (recipe.runeBonus() && level.getRandom().nextFloat() < EXTRA_CHANCE) {
                    dust.grow(1);
                }
            } else {
                dust.grow(2); // espaço para o pior caso
            }
            Optional<SmeltingLookup.Result> smelt = SmeltingLookup.find(level, dust.copyWithCount(1));
            if (smelt.isPresent()) {
                ItemStack ingot = smelt.get().output().copy();
                ingot.setCount(ingot.getCount() * dust.getCount());
                return new Product(ingot, smelt.get().experience() * dust.getCount());
            }
            return new Product(dust, 0);
        }
        Optional<SmeltingLookup.Result> smelt = SmeltingLookup.find(level, input);
        return smelt.map(r -> new Product(r.output().copy(), r.experience())).orElse(null);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--this.checkTimer <= 0) {
            this.checkTimer = 20;
            boolean now = CrusherForgeStructure.firstError(level, pos, this.front(state)) == null;
            if (now != this.formed || state.getValue(CrusherForgeStructure.FORMED) != now) {
                this.formed = now;
                CrusherForgeStructure.setFormed(level, pos, this.front(state), now);
                if (now) {
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 0.7F);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 60, 0.8, 0.8, 0.8, 0.05);
                }
                state = level.getBlockState(pos);
            }
        }
        this.info0 = this.formed ? 1 : 0;
        boolean working = false;
        int shown = 0;
        if (this.formed) {
            for (int lane = 0; lane < LANES; lane++) {
                ItemStack input = this.items.get(lane);
                Product worst = input.isEmpty() ? null : this.product(level, input, false);
                if (worst == null) {
                    this.laneProgress[lane] = 0;
                    continue;
                }
                int cost = this.energyStep(FE_PER_LANE);
                if (!this.canOutput(worst.stack()) || !this.hasEnergy(cost)) {
                    continue;
                }
                working = true;
                this.drainEnergy(cost);
                this.laneProgress[lane] += this.workStep();
                if (this.laneProgress[lane] >= this.maxProgress) {
                    this.laneProgress[lane] = 0;
                    Product made = this.product(level, input, true);
                    input.shrink(1);
                    if (made != null) {
                        this.insertOutput(made.stack());
                        this.storedXp += made.xp();
                    }
                    level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.3F, 0.7F);
                    level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F);
                    level.sendParticles(ParticleTypes.LAVA, pos.getX() + 1.0, pos.getY() + 2.05, pos.getZ() + 0.5, 3, 0.4, 0.05, 0.4, 0.0);
                }
                shown = Math.max(shown, this.laneProgress[lane]);
            }
        }
        this.progress = shown;
        if (this.storedXp >= 1.0F) {
            int whole = Mth.floor(this.storedXp);
            this.storedXp -= whole;
            ExperienceOrb.award(level, pos.getCenter().add(0, 0.7, 0), whole);
        }
        if (working) {
            this.setChanged();
            if (level.getGameTime() % 4 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.01);
            }
        }
        this.setLit(level, pos, state, working);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (!super.canPlaceItemThroughFace(slot, stack, direction)) {
            return false;
        }
        return slot >= LANES || !(this.level instanceof ServerLevel server) || this.product(server, stack, false) != null;
    }

    @Override
    public void addInfo(List<Component> lines) {
        if (!this.formed) {
            lines.add(Component.translatable("info.mistech.forge.incomplete"));
        }
        for (int lane = 0; lane < LANES; lane++) {
            if (this.laneProgress[lane] > 0) {
                lines.add(Component.translatable("info.mistech.lane", lane + 1, this.laneProgress[lane] * 100 / Math.max(1, this.maxProgress)));
            }
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
