package com.mistech.mistechmod.machine;

import java.util.List;

import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Tuneladora: a cada pulso de redstone (borda de subida), se tiver {@link #ENERGY_PER_BLOCK} FE, quebra o bloco
 * à frente como uma picareta de ferro. Os drops vão para o recipiente atrás dela (baú, barril, funil...);
 * sem recipiente, ou se não couber, caem no lugar do bloco quebrado.
 */
public class TunnelerBlockEntity extends BlockEntity {
    public static final int CAPACITY = 10_000;
    public static final int MAX_INPUT = 500;
    public static final int ENERGY_PER_BLOCK = 400;

    private static ItemStack tool;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, MAX_INPUT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            TunnelerBlockEntity.this.setChanged();
        }
    };

    public TunnelerBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.TUNNELER.get(), pos, state);
    }

    /** Picareta virtual (ferro): decide o que dá para quebrar e o que cai. */
    public static ItemStack tool() {
        if (tool == null) {
            tool = new ItemStack(Items.IRON_PICKAXE);
        }
        return tool;
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public int getEnergy() {
        return this.energy.getAmountAsInt();
    }

    /** Chamado pelo tick agendado depois do pulso de redstone. */
    public void dig(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(TunnelerBlock.FACING);
        BlockPos target = pos.relative(facing);
        if (!level.isLoaded(target)) {
            return;
        }
        BlockState targetState = level.getBlockState(target);
        if (!MiningHelper.canMine(level, target, targetState, tool())) {
            return;
        }
        if (this.energy.getAmountAsInt() < ENERGY_PER_BLOCK) {
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 0.8F);
            return;
        }
        List<ItemStack> drops = MiningHelper.drops(level, target, targetState, tool());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, targetState),
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.1);
        level.destroyBlock(target, false);
        this.energy.set(this.energy.getAmountAsInt() - ENERGY_PER_BLOCK);

        ResourceHandler<ItemResource> behind = MiningHelper.container(level, pos.relative(facing.getOpposite()), facing);
        for (ItemStack drop : drops) {
            ItemStack rest = behind != null ? MiningHelper.insert(behind, drop) : drop;
            if (!rest.isEmpty()) {
                Block.popResource(level, target, rest);
            }
        }
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, 1.4F + level.getRandom().nextFloat() * 0.3F);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output);
    }
}
