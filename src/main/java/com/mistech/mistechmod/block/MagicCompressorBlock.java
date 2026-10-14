package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Compressor de Magia: pedestal com um buraco redondo no topo.
 * Botão direito com Frasco de Essência Mágica Impura enche o buraco. Com incenso aceso por perto
 * (jogador segurando/carregando incenso aceso ou Porta-Incenso ativo ao alcance), a essência endurece
 * e sai um Fragmento de Magia Impura, que fica guardado em cima (mão vazia, funil ou golem tiram).
 */
public class MagicCompressorBlock extends BaseEntityBlock {
    public static final MapCodec<MagicCompressorBlock> CODEC = simpleCodec(MagicCompressorBlock::new);
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),    // base
            Block.box(4, 2, 4, 12, 12, 12),   // coluna
            Block.box(2, 12, 2, 14, 16, 14)); // topo

    public MagicCompressorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, false));
    }

    @Override
    public MapCodec<MagicCompressorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicCompressorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.MAGIC_COMPRESSOR.get(), MagicCompressorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        boolean igneous = stack.is(com.mistech.mistechmod.beyond.PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get());
        if (state.getValue(FILLED) || !(stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || igneous)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(FILLED, true), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MagicCompressorBlockEntity compressor) {
                compressor.setIgneous(igneous);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Mão vazia: pega o fragmento pronto que fica em cima. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof MagicCompressorBlockEntity compressor) || compressor.getOutput().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack out = compressor.takeOutput();
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof MagicCompressorBlockEntity compressor) || compressor.getOutput().isEmpty()) {
            return 0;
        }
        ItemStack out = compressor.getOutput();
        return 1 + Math.round(14.0F * out.getCount() / out.getMaxStackSize());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FILLED) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.4 + random.nextDouble() * 0.2, pos.getY() + 1.0,
                    pos.getZ() + 0.4 + random.nextDouble() * 0.2, 0.0, 0.15, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }
}
