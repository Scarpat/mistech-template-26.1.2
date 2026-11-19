package com.mistech.mistechmod.folklore.machine;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.FluidTank;
import com.mistech.mistechmod.lore.CucaCauldronBlockEntity;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Alambique de Poções: poções em lotes de até {@link #BATCH}, com água do tanque ({@link #WATER_PER_BOTTLE} mB por garrafa),
 * frascos vazios e os ingredientes nos 3 espaços de ingrediente (usados em ordem: o primeiro que serve na poção atual).
 * Cada passo leva {@link #STEP_TICKS} ticks a {@link #FE_PER_TICK} FE/t e gasta 1 ingrediente a cada 3 garrafas, como o
 * suporte de poções. Usa as receitas do jogo (PotionBrewing), então as de outros mods também entram.
 * <p>Modo Cuca: com 2 ou 3 poções prontas nos espaços de ingrediente (e, se quiser, um Frasco de Essência Impura para subir
 * um nível), o alambique junta tudo numa Poção da Cuca, com as regras do Caldeirão da Cuca.
 * info0 = situação ({@link #STATUS}), info1 = água (mB), info2 = poções no lote.
 */
public class PotionStillBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 4000;
    public static final int BATCH = 8;
    public static final int WATER_PER_BOTTLE = 333;
    public static final int STEP_TICKS = 400;
    public static final int CUCA_TICKS = 200;
    public static final int FE_PER_TICK = 30;
    public static final int SLOT_BOTTLES = 0, FIRST_INGREDIENT = 1, INGREDIENTS = 3;
    public static final String[] STATUS = {"idle", "brewing", "cuca", "no_water", "no_bottles", "no_energy", "full"};
    public static final int ST_IDLE = 0, ST_BREWING = 1, ST_CUCA = 2, ST_NO_WATER = 3, ST_NO_BOTTLES = 4, ST_NO_ENERGY = 5, ST_FULL = 6;

    private final FluidTank tank = new FluidTank(TANK, this::setChanged) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isEmpty() || resource.getFluid().isSame(Fluids.WATER);
        }
    };
    /** Poção do lote em andamento (uma cópia; o lote tem {@link #batchSize} delas). */
    private ItemStack batch = ItemStack.EMPTY;
    private int batchSize;
    private boolean cuca;

    public PotionStillBlockEntity(BlockPos pos, BlockState state) {
        super(FolkMachines.POTION_STILL_BE.get(), pos, state, FolkMachines.STILL_LAYOUT, 500, 0);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public static boolean isBottle(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE);
    }

    public static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    public static boolean isEmpowerer(ItemStack stack) {
        return stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get());
    }

    /** Filtro dos espaços de ingrediente (vale no cliente também). */
    public static boolean isIngredient(ItemStack stack) {
        return !stack.isEmpty() && !isBottle(stack);
    }

    private static ItemStack water() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        PotionBrewing brewing = level.potionBrewing();
        int status = ST_IDLE;
        if (this.batchSize == 0 && !this.cuca) {
            status = this.tryStart(brewing);
        }
        if (this.cuca) {
            status = this.cucaStep(level, pos);
        } else if (this.batchSize > 0) {
            status = this.brewStep(level, pos, brewing);
        }
        this.info0 = status;
        this.info1 = this.tank.getAmount();
        this.info2 = this.batchSize;
        this.setLit(level, pos, state, status == ST_BREWING || status == ST_CUCA);
    }

    private int potionsInSlots() {
        int n = 0;
        for (int i = 0; i < INGREDIENTS; i++) {
            if (isPotion(this.items.get(FIRST_INGREDIENT + i))) {
                n++;
            }
        }
        return n;
    }

    /** Começa um lote (ou a mistura da Cuca) se der. */
    private int tryStart(PotionBrewing brewing) {
        if (this.potionsInSlots() >= 2) {
            this.cuca = true;
            this.progress = 0;
            this.maxProgress = CUCA_TICKS * 2;
            return ST_CUCA;
        }
        ItemStack water = water();
        boolean any = false;
        for (int i = 0; i < INGREDIENTS; i++) {
            ItemStack ing = this.items.get(FIRST_INGREDIENT + i);
            if (!ing.isEmpty() && brewing.hasMix(water, ing)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return ST_IDLE;
        }
        int bottles = isBottle(this.items.get(SLOT_BOTTLES)) ? this.items.get(SLOT_BOTTLES).getCount() : 0;
        if (bottles <= 0) {
            return ST_NO_BOTTLES;
        }
        int byWater = this.tank.getAmount() / WATER_PER_BOTTLE;
        if (byWater <= 0) {
            return ST_NO_WATER;
        }
        int n = Math.min(BATCH, Math.min(bottles, byWater));
        this.items.get(SLOT_BOTTLES).shrink(n);
        this.tank.drain(FluidResource.of(Fluids.WATER), n * WATER_PER_BOTTLE, false);
        this.batch = water;
        this.batchSize = n;
        this.progress = 0;
        this.maxProgress = STEP_TICKS * 2;
        this.setChanged();
        return ST_BREWING;
    }

    private int brewStep(ServerLevel level, BlockPos pos, PotionBrewing brewing) {
        int need = (this.batchSize + 2) / 3;
        int slot = -1;
        for (int i = 0; i < INGREDIENTS; i++) {
            ItemStack ing = this.items.get(FIRST_INGREDIENT + i);
            if (!ing.isEmpty() && ing.getCount() >= need && brewing.hasMix(this.batch, ing)) {
                slot = FIRST_INGREDIENT + i;
                break;
            }
        }
        if (slot < 0) {
            // nada mais serve: sai o lote
            return this.output() ? ST_IDLE : ST_FULL;
        }
        int fe = this.energyStep(FE_PER_TICK);
        if (!this.hasEnergy(fe)) {
            return ST_NO_ENERGY;
        }
        this.drainEnergy(fe);
        this.progress += this.workStep();
        if (level.getGameTime() % 10 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.0);
        }
        if (this.progress >= this.maxProgress) {
            this.progress = 0;
            ItemStack ing = this.items.get(slot);
            ItemStack next = brewing.mix(ing.copyWithCount(1), this.batch.copy());
            ing.shrink(need);
            this.batch = next;
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.0F);
            this.setChanged();
        }
        return ST_BREWING;
    }

    /** Põe o lote na saída; false se não coube tudo (o resto espera). */
    private boolean output() {
        while (this.batchSize > 0) {
            ItemStack one = this.batch.copyWithCount(1);
            if (!this.canOutput(one)) {
                return false;
            }
            this.insertOutput(one);
            this.batchSize--;
        }
        this.batch = ItemStack.EMPTY;
        this.setChanged();
        return true;
    }

    private int cucaStep(ServerLevel level, BlockPos pos) {
        if (this.potionsInSlots() < 2) {
            this.cuca = false;
            this.progress = 0;
            return ST_IDLE;
        }
        int fe = this.energyStep(FE_PER_TICK);
        if (!this.hasEnergy(fe)) {
            return ST_NO_ENERGY;
        }
        this.drainEnergy(fe);
        this.progress += this.workStep();
        if (level.getGameTime() % 8 == 0) {
            level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.0);
        }
        if (this.progress < this.maxProgress) {
            return ST_CUCA;
        }
        List<PotionContents> potions = new ArrayList<>();
        boolean empowered = false;
        int empowerSlot = -1;
        for (int i = 0; i < INGREDIENTS; i++) {
            ItemStack s = this.items.get(FIRST_INGREDIENT + i);
            if (isPotion(s)) {
                potions.add(s.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY));
            } else if (isEmpowerer(s) && empowerSlot < 0) {
                empowered = true;
                empowerSlot = FIRST_INGREDIENT + i;
            }
        }
        ItemStack result = CucaCauldronBlockEntity.brew(potions, empowered);
        if (!result.isEmpty() && !this.canOutput(result)) {
            this.progress = this.maxProgress;
            return ST_FULL;
        }
        for (int i = 0; i < INGREDIENTS; i++) {
            ItemStack s = this.items.get(FIRST_INGREDIENT + i);
            if (isPotion(s)) {
                this.items.set(FIRST_INGREDIENT + i, ItemStack.EMPTY);
                ItemStack glass = new ItemStack(Items.GLASS_BOTTLE);
                if (this.canOutput(glass)) {
                    this.insertOutput(glass);
                }
            }
        }
        if (empowerSlot >= 0) {
            this.items.get(empowerSlot).shrink(1);
        }
        if (!result.isEmpty()) {
            this.insertOutput(result);
        }
        level.playSound(null, pos, SoundEvents.WITCH_DRINK, SoundSource.BLOCKS, 0.8F, 0.8F);
        this.cuca = false;
        this.progress = 0;
        this.setChanged();
        return ST_IDLE;
    }

    // ----- água na mão -----

    @Override
    public boolean handles(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET);
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        if (this.tank.fill(FluidResource.of(Fluids.WATER), 1000, true) < 1000) {
            return;
        }
        this.tank.fill(FluidResource.of(Fluids.WATER), 1000, false);
        com.mistech.mistechmod.purity.block.PurityFluids.swap(player, hand, new ItemStack(Items.BUCKET));
        player.level().playSound(null, this.worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("block.minecraft.water"), this.tank.getAmount(), TANK));
        lines.add(Component.translatable("gui.mistech.potion_still.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.info0))]));
        super.addInfo(lines);
    }

    /** Linhas da tela (cliente). */
    public static List<Component> infoLines(int status, int batch) {
        Component line = Component.translatable("gui.mistech.potion_still.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]);
        return batch > 0 ? List.of(line, Component.translatable("gui.mistech.potion_still.batch", batch)) : List.of(line);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.deserialize(input);
        this.batch = input.read("Batch", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.batchSize = this.batch.isEmpty() ? 0 : input.getIntOr("BatchSize", 0);
        this.cuca = input.getBooleanOr("Cuca", false);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        this.tank.serialize(output);
        if (!this.batch.isEmpty()) {
            output.store("Batch", ItemStack.OPTIONAL_CODEC, this.batch);
            output.putInt("BatchSize", this.batchSize);
        }
        output.putBoolean("Cuca", this.cuca);
    }

}
