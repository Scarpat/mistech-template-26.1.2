package com.mistech.mistechmod.machine.arcane;

import java.util.function.Supplier;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Blocos das máquinas arcanas (versões avançadas das máquinas iniciais). */
public abstract class ArcaneMachineBlock extends MachineBlock {
    protected ArcaneMachineBlock(BlockBehaviour.Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type) {
        super(properties, type);
    }

    protected abstract ParticleOptions particle();

    private void particles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(this.particle(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.7, pos.getY() + 1.02,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.7, 0.0, 0.03, 0.0);
        }
    }

    private static boolean formed(BlockState state) {
        return state.hasProperty(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED)
                && state.getValue(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED);
    }

    /** Peça de uma Forja Trituradora montada: o clique abre a Forja. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (formed(state) && com.mistech.mistechmod.machine.forge.CrusherForgeStructure.openCore(level, pos, player)) {
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!formed(state)) {
            this.particles(state, level, pos, random);
        }
    }

    /** Frasco/balde na mão: põe ou tira essência direto (sem abrir a tela). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof EssenceTankHolder holder && holder.handles(stack)) {
            if (!level.isClientSide()) {
                holder.interact(player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Máquina com tanque de essência que aceita frascos/baldes na mão. */
    public interface EssenceTankHolder {
        boolean handles(ItemStack stack);

        void interact(Player player, InteractionHand hand);
    }

    public static final class Furnace extends ArcaneMachineBlock {
        public static final MapCodec<Furnace> CODEC = simpleCodec(Furnace::new);

        public Furnace(BlockBehaviour.Properties properties) {
            super(properties, ArcaneMachines.ARCANE_FURNACE_BE);
            this.registerDefaultState(this.defaultBlockState().setValue(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED, false));
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED);
        }

        @Override
        public MapCodec<Furnace> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.SOUL_FIRE_FLAME;
        }
    }

    public static final class Extractor extends ArcaneMachineBlock {
        public static final MapCodec<Extractor> CODEC = simpleCodec(Extractor::new);

        public Extractor(BlockBehaviour.Properties properties) {
            super(properties, ArcaneMachines.ARCANE_EXTRACTOR_BE);
        }

        @Override
        public MapCodec<Extractor> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.WITCH;
        }
    }

    public static final class Compressor extends ArcaneMachineBlock {
        public static final MapCodec<Compressor> CODEC = simpleCodec(Compressor::new);

        public Compressor(BlockBehaviour.Properties properties) {
            super(properties, ArcaneMachines.ARCANE_COMPRESSOR_BE);
        }

        @Override
        public MapCodec<Compressor> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.REVERSE_PORTAL;
        }
    }

    public static final class Crusher extends ArcaneMachineBlock {
        public static final MapCodec<Crusher> CODEC = simpleCodec(Crusher::new);

        public Crusher(BlockBehaviour.Properties properties) {
            super(properties, ArcaneMachines.ARCANE_CRUSHER_BE);
            this.registerDefaultState(this.defaultBlockState().setValue(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED, false));
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED);
        }

        @Override
        public MapCodec<Crusher> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.CRIT;
        }
    }

    public static final class Liquefier extends ArcaneMachineBlock {
        public static final MapCodec<Liquefier> CODEC = simpleCodec(Liquefier::new);

        public Liquefier(BlockBehaviour.Properties properties) {
            super(properties, ArcaneMachines.LIQUEFIER_BE);
        }

        @Override
        public MapCodec<Liquefier> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.DRIPPING_OBSIDIAN_TEAR;
        }
    }
}
