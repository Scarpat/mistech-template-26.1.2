package com.mistech.mistechmod.spellcraft.well;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Blocos da Fase 5: Poço de Mana e Sangria Arcana. Nos dois, agachar + clique abre a tela. */
public final class WellBlocks {
    private WellBlocks() {}

    /** Pia de quartzo com mana brilhando. Clique: enche a mana; frasco/balde de Essência Purificada: enche o tanque. */
    public static final class Well extends ArcaneMachineBlock {
        public static final MapCodec<Well> CODEC = simpleCodec(Well::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(5, 3, 5, 11, 9, 11), Block.box(1, 9, 1, 15, 14, 15));

        public Well(BlockBehaviour.Properties properties) {
            super(properties, WellContent.MANA_WELL_BE);
        }

        @Override
        public MapCodec<Well> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.ENCHANT;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            if (player.isShiftKeyDown()) {
                return super.useWithoutItem(state, level, pos, player, hitResult);
            }
            if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof ManaWellBlockEntity well) {
                well.fill(server, player);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            super.animateTick(state, level, pos, random);
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.9,
                        pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.01, 0.0);
            }
        }
    }

    /** Pedestal com taça de cobre. Segurar o clique: drena a sua mana em FE. */
    public static final class Siphon extends MachineBlock {
        public static final MapCodec<Siphon> CODEC = simpleCodec(Siphon::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(4, 4, 4, 12, 10, 12), Block.box(3, 10, 3, 13, 15, 13));

        public Siphon(BlockBehaviour.Properties properties) {
            super(properties, WellContent.MANA_SIPHON_BE);
        }

        @Override
        public MapCodec<Siphon> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            if (player.isShiftKeyDown()) {
                return super.useWithoutItem(state, level, pos, player, hitResult);
            }
            if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof ManaSiphonBlockEntity siphon) {
                siphon.use(server, player);
            }
            return InteractionResult.SUCCESS;
        }

        /** Poção de Mana na mão: despeja na Sangria (vira FE). */
        @Override
        protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
                Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
            if (stack.is(WellContent.MANA_POTION.get())) {
                if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof ManaSiphonBlockEntity siphon) {
                    siphon.pour(server, player, hand);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT) && random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 1.5, -0.5, (random.nextDouble() - 0.5) * 1.5);
            }
        }
    }
}
