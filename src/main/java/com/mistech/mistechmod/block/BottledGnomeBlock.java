package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gnomo Engarrafado: um Pote de Vidro com um gnomo preso dentro (nasce da Isca de Gnomo no pote).
 * Alimentado com maçãs, deixa as máquinas do Mistech num raio de 3 blocos 50% mais rápidas (Impulso de Gnomo).
 * Propriedade FED = está alimentado agora (muda as partículas).
 */
public class BottledGnomeBlock extends BaseEntityBlock {
    public static final MapCodec<BottledGnomeBlock> CODEC = simpleCodec(BottledGnomeBlock::new);
    public static final BooleanProperty FED = BooleanProperty.create("fed");

    public BottledGnomeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FED, false));
    }

    @Override
    public MapCodec<BottledGnomeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BottledGnomeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.BOTTLED_GNOME.get(), BottledGnomeBlockEntity::serverTick);
    }

    /** Maçã (5 min) ou maçã dourada (30 min) na mão: o gnomo come. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        int ticks = BottledGnomeBlockEntity.foodTicks(stack);
        if (ticks <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof BottledGnomeBlockEntity) && !level.isClientSide()) {
            // potes colocados antes desta versão não tinham block entity
            level.setBlockEntity(new BottledGnomeBlockEntity(pos, state));
        }
        if (!(level.getBlockEntity(pos) instanceof BottledGnomeBlockEntity gnome)) {
            return InteractionResult.SUCCESS;
        }
        if (!gnome.canEat(stack)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.bottled_gnome.full"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            gnome.feed(stack);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.sendOverlayMessage(gnome.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    /** Mão vazia: mostra quanto tempo de comida ainda resta. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BottledGnomeBlockEntity gnome) {
            player.sendOverlayMessage(gnome.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    /** Alimentado: notas musicais e brilhos dourados. Com fome: fumacinha cinza. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.95, z = pos.getZ() + 0.5;
        if (state.getValue(FED)) {
            if (random.nextInt(10) == 0) {
                level.addParticle(ParticleTypes.NOTE, x, y, z, random.nextInt(24) / 24.0, 0.0, 0.0);
            }
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.WAX_ON, x + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 0.3 + random.nextDouble() * 0.5,
                        z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.0, 0.0);
            }
        } else if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BottledGnomeBlockEntity gnome ? gnome.comparatorSignal() : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GlassJarBlock.SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FED);
    }
}
