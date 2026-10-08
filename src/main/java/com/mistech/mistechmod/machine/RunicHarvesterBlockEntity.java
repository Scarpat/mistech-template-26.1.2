package com.mistech.mistechmod.machine;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Colheitadeira Rúnica: colhe plantas maduras numa área 9×9 à frente (13×13 com Runa do Espaço, que gasta
 * 1 uso a cada 64 colheitas). Gasta 200 FE por planta. Guarda o que colhe nos 9 espaços de saída; cheia = para.
 * Colhe: plantações ({@link CropBlock} maduro, inclui tabaco, cânhamo, beladona e café), verruga do Nether,
 * cacau, frutas silvestres (deixa o arbusto), abóbora e melancia (só o fruto), cana, bambu e cacto (deixa o de baixo).
 * Não replanta: isso é trabalho da Plantadeira.
 */
public class RunicHarvesterBlockEntity extends FarmMachineBlockEntity {
    public static final int ENERGY_PER_HARVEST = 200;
    public static final int RUNE_OPS_PER_USE = 64;

    public RunicHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNIC_HARVESTER.get(), pos, state, MachineLayouts.HARVESTER);
    }

    @Override
    protected int radius() {
        return this.hasRune(Mistech.SPACE_RUNE.get()) ? BIG_RADIUS : BASE_RADIUS;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean working = !this.outputFull() && this.hasEnergy(ENERGY_PER_HARVEST);
        if (working && this.stepReady()) {
            BlockPos column = this.nextColumn(pos, state);
            // de cima para baixo: cana/bambu/cacto caem na ordem certa
            for (int dy = 2; dy >= -1; dy--) {
                BlockPos target = column.above(dy);
                if (level.isLoaded(target) && this.tryHarvest(level, target)) {
                    this.drainEnergy(ENERGY_PER_HARVEST);
                    if (this.hasRune(Mistech.SPACE_RUNE.get())) {
                        this.countRuneOp(RUNE_OPS_PER_USE);
                    }
                    if (!this.hasEnergy(ENERGY_PER_HARVEST) || this.outputFull()) {
                        break;
                    }
                }
            }
            this.setChanged();
        }
        if (this.info0 == 0) {
            this.info0 = this.radius() * 2 + 1;
        }
        this.setLit(level, pos, state, working);
    }

    private boolean tryHarvest(ServerLevel level, BlockPos target) {
        BlockState plant = level.getBlockState(target);
        Block block = plant.getBlock();

        // frutas silvestres: tira as frutas e deixa o arbusto
        if (block instanceof SweetBerryBushBlock) {
            int age = plant.getValue(SweetBerryBushBlock.AGE);
            if (age < 2) {
                return false;
            }
            int count = 1 + level.getRandom().nextInt(2) + (age == 3 ? 1 : 0);
            this.store(level, target, List.of(new ItemStack(Items.SWEET_BERRIES, count)));
            level.setBlock(target, plant.setValue(SweetBerryBushBlock.AGE, 1), Block.UPDATE_CLIENTS);
            level.playSound(null, target, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 0.8F, 1.0F);
            return true;
        }

        boolean ripe;
        if (block instanceof CropBlock crop) {
            ripe = crop.isMaxAge(plant);
        } else if (block instanceof NetherWartBlock) {
            ripe = plant.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        } else if (block instanceof CocoaBlock) {
            ripe = plant.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        } else if (plant.is(Blocks.PUMPKIN) || plant.is(Blocks.MELON)) {
            ripe = true;
        } else if (block instanceof SugarCaneBlock || block instanceof BambooStalkBlock || block instanceof CactusBlock) {
            ripe = level.getBlockState(target.below()).is(block); // deixa o bloco de baixo
        } else {
            ripe = false;
        }
        if (!ripe) {
            return false;
        }
        List<ItemStack> drops = Block.getDrops(plant, level, target, level.getBlockEntity(target));
        level.destroyBlock(target, false);
        this.store(level, target, drops);
        return true;
    }

    /** Guarda na saída; o que não couber cai no chão onde foi colhido. */
    private void store(ServerLevel level, BlockPos target, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack rest = this.insertOutput(drop);
            if (!rest.isEmpty()) {
                Block.popResource(level, target, rest);
            }
        }
    }
}
