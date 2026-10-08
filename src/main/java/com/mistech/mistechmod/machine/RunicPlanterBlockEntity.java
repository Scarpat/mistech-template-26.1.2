package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.SpecialPlantable;

/**
 * Plantadeira Rúnica: planta as sementes dos 9 espaços de entrada numa área 9×9 à frente (mesma área da Colheitadeira).
 * Planta na altura da máquina, em cima do bloco de baixo; ara terra/grama quando a semente precisa de terra arada.
 * Custo: 100 FE por plantio, 50 FE por arar.
 * Runa de Água: mantém a terra arada da área hidratada (1 uso a cada 64 hidratações).
 * Runa do Tempo: farinha de osso numa planta aleatória da área a cada 5 s (1 uso a cada 32 aplicações, 100 FE).
 */
public class RunicPlanterBlockEntity extends FarmMachineBlockEntity {
    public static final int ENERGY_PER_PLANT = 100;
    public static final int ENERGY_PER_TILL = 50;
    public static final int ENERGY_PER_GROWTH = 100;
    public static final int WATER_OPS_PER_USE = 64;
    public static final int TIME_OPS_PER_USE = 32;
    public static final int GROWTH_TICKS = 100;

    private int growthTimer;

    public RunicPlanterBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNIC_PLANTER.get(), pos, state, MachineLayouts.PLANTER);
    }

    @Override
    protected int radius() {
        return BASE_RADIUS;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean working = this.hasEnergy(ENERGY_PER_TILL);
        if (working && this.stepReady()) {
            BlockPos column = this.nextColumn(pos, state);
            if (level.isLoaded(column)) {
                this.workColumn(level, column);
            }
            this.setChanged();
        }
        if (working && this.hasRune(Mistech.TIME_RUNE.get()) && ++this.growthTimer >= GROWTH_TICKS) {
            this.growthTimer = 0;
            this.growRandom(level, pos, state);
        }
        if (this.info0 == 0) {
            this.info0 = this.radius() * 2 + 1;
        }
        this.setLit(level, pos, state, working);
    }

    private void workColumn(ServerLevel level, BlockPos plantPos) {
        BlockPos soilPos = plantPos.below();
        BlockState soil = level.getBlockState(soilPos);

        // Runa de Água: terra arada sempre molhada
        if (soil.getBlock() instanceof FarmlandBlock && soil.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE
                && this.hasRune(Mistech.WATER_RUNE.get())) {
            level.setBlock(soilPos, soil.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
            this.countRuneOp(WATER_OPS_PER_USE);
        }

        if (!level.getBlockState(plantPos).isAir()) {
            return;
        }
        // tenta plantar alguma semente que sobreviva aqui
        for (int slot = 0; slot < this.items.size(); slot++) {
            if (this.layout.kind(slot) != MachineLayout.Kind.INPUT) {
                continue;
            }
            ItemStack seed = this.items.get(slot);
            if (seed.isEmpty() || !this.hasEnergy(ENERGY_PER_PLANT)) {
                continue;
            }
            if (this.plant(level, plantPos, seed)) {
                this.drainEnergy(ENERGY_PER_PLANT);
                return;
            }
        }
        // nenhuma serviu: ara a terra se houver semente de plantação e tenta de novo na próxima volta
        if ((soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT_PATH))
                && level.getBlockState(plantPos).isAir() && this.hasCropSeed()) {
            level.setBlock(soilPos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, soilPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 0.6F, 1.0F);
            this.drainEnergy(ENERGY_PER_TILL);
        }
    }

    private boolean plant(ServerLevel level, BlockPos plantPos, ItemStack seed) {
        if (seed.getItem() instanceof SpecialPlantable plantable) {
            if (plantable.canPlacePlantAtPosition(seed, level, plantPos, Direction.DOWN)) {
                plantable.spawnPlantAtPosition(seed, level, plantPos, Direction.DOWN);
                seed.shrink(1);
                return true;
            }
            return false;
        }
        if (seed.getItem() instanceof BlockItem blockItem) {
            BlockState plant = blockItem.getBlock().defaultBlockState();
            if (plant.canSurvive(level, plantPos)) {
                level.setBlock(plantPos, plant, Block.UPDATE_ALL);
                level.playSound(null, plantPos, plant.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 1.0F);
                seed.shrink(1);
                return true;
            }
        }
        return false;
    }

    private boolean hasCropSeed() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack seed = this.items.get(slot);
            if (this.layout.kind(slot) == MachineLayout.Kind.INPUT && seed.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof CropBlock) {
                return true;
            }
        }
        return false;
    }

    /** Runa do Tempo: farinha de osso numa planta aleatória da área. */
    private void growRandom(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.hasEnergy(ENERGY_PER_GROWTH)) {
            return;
        }
        int radius = this.radius();
        BlockPos center = this.areaCenter(pos, state, radius);
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos target = new BlockPos(center.getX() + level.getRandom().nextInt(radius * 2 + 1) - radius,
                    pos.getY() + level.getRandom().nextInt(2), center.getZ() + level.getRandom().nextInt(radius * 2 + 1) - radius);
            BlockState plant = level.getBlockState(target);
            if (plant.getBlock() instanceof BonemealableBlock bonemealable && !(plant.getBlock() instanceof net.minecraft.world.level.block.GrassBlock)
                    && bonemealable.isValidBonemealTarget(level, target, plant)) {
                bonemealable.performBonemeal(level, level.getRandom(), target, plant);
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, 15);
                this.drainEnergy(ENERGY_PER_GROWTH);
                this.countRuneOp(TIME_OPS_PER_USE);
                return;
            }
        }
    }
}
