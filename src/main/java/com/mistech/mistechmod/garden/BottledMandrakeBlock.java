package com.mistech.mistechmod.garden;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.GlassJarBlock;
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
 * Mandrágora Engarrafada: Pote de Vidro com uma Mandrágora dentro.
 * Alimentada com Farinha de Osso, canta para as plantações em volta (raio 4) e elas crescem ~3× mais rápido.
 * Propriedade FED = está alimentada agora (muda as partículas).
 */
public class BottledMandrakeBlock extends BaseEntityBlock {
    public static final MapCodec<BottledMandrakeBlock> CODEC = simpleCodec(BottledMandrakeBlock::new);
    public static final BooleanProperty FED = BooleanProperty.create("fed");

    public BottledMandrakeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FED, false));
    }

    @Override
    public MapCodec<BottledMandrakeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BottledMandrakeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, GardenContent.BOTTLED_MANDRAKE_BE.get(), BottledMandrakeBlockEntity::serverTick);
    }

    /** Farinha de osso (ou bloco de osso) na mão: a mandrágora "come". */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (BottledMandrakeBlockEntity.foodTicks(stack) <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof BottledMandrakeBlockEntity mandrake)) {
            return InteractionResult.SUCCESS;
        }
        if (!mandrake.canEat(stack)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.bottled_mandrake.full"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            mandrake.feed(stack);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.sendOverlayMessage(mandrake.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    /** Mão vazia: mostra quanto tempo ainda resta. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BottledMandrakeBlockEntity mandrake) {
            player.sendOverlayMessage(mandrake.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    /** Alimentada: notas e esporos verdes subindo. Com fome: fumacinha e o vidro embaça. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.95, z = pos.getZ() + 0.5;
        if (state.getValue(FED)) {
            if (random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.NOTE, x, y, z, random.nextInt(24) / 24.0, 0.0, 0.0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 0.2 + random.nextDouble() * 0.6,
                        z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.0, 0.0);
            }
        } else if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BottledMandrakeBlockEntity mandrake ? mandrake.comparatorSignal() : 0;
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
