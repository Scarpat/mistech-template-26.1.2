package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Extrator de Essência Mágica: parece um caldeirão. Com energia, enche MUITO devagar (3 níveis)
 * de Essência Mágica Impura. Frasco de vidro tira 1 nível; balde tira os 3 (cheio);
 * frasco de essência devolve 1 nível.
 */
public class EssenceExtractorBlock extends BaseEntityBlock {
    public static final MapCodec<EssenceExtractorBlock> CODEC = simpleCodec(EssenceExtractorBlock::new);
    public static final int MAX_LEVEL = 3;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL);
    /** Ligado: tem energia e está enchendo (as rachaduras pulsam em roxo e laranja). */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(),
            Shapes.or(Block.box(0, 0, 4, 16, 3, 12), Block.box(4, 0, 0, 12, 3, 16),
                    Block.box(2, 0, 2, 14, 3, 14), Block.box(2, 4, 2, 14, 16, 14)),
            BooleanOp.ONLY_FIRST);

    public EssenceExtractorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0).setValue(LIT, false));
    }

    @Override
    public MapCodec<EssenceExtractorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.ESSENCE_EXTRACTOR.get(), EssenceExtractorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        int current = state.getValue(LEVEL);
        ItemStack result;
        int newLevel;
        // no Nether a essência sai Ígnea (e não dá para tirar de balde: ela ferve)
        boolean nether = level.dimension() == Level.NETHER;
        if (stack.is(Items.GLASS_BOTTLE) && current >= 1) {
            result = new ItemStack(nether ? com.mistech.mistechmod.beyond.PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get()
                    : Mistech.IMPURE_ESSENCE_BOTTLE.get());
            newLevel = current - 1;
        } else if (stack.is(Items.BUCKET) && current == MAX_LEVEL && nether) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mistech.extractor.no_bucket_nether"));
            return InteractionResult.FAIL;
        } else if (stack.is(com.mistech.mistechmod.beyond.PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get()) && current < MAX_LEVEL) {
            result = new ItemStack(Items.GLASS_BOTTLE);
            newLevel = current + 1;
        } else if (stack.is(Items.BUCKET) && current == MAX_LEVEL) {
            result = new ItemStack(Mistech.IMPURE_ESSENCE_BUCKET.get());
            newLevel = 0;
        } else if (stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) && current < MAX_LEVEL) {
            result = new ItemStack(Items.GLASS_BOTTLE);
            newLevel = current + 1;
        } else if (stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get()) && current == 0) {
            result = new ItemStack(Items.BUCKET);
            newLevel = MAX_LEVEL;
        } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(LEVEL, newLevel), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            boolean filling = newLevel < current;
            level.playSound(null, pos, filling ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Bolhas roxas subindo da essência. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int lvl = state.getValue(LEVEL);
        if (state.getValue(LIT) && random.nextInt(4) == 0) {
            // brasas de magma saindo das rachaduras
            level.addParticle(random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.WITCH,
                    pos.getX() + random.nextDouble(), pos.getY() + 0.2 + random.nextDouble() * 0.6, pos.getZ() + random.nextDouble(),
                    0.0, 0.01, 0.0);
        }
        if (lvl > 0 && random.nextInt(3) == 0) {
            double y = pos.getY() + 0.35 + lvl * 0.19;
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.25 + random.nextDouble() * 0.5, y,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.2, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(LEVEL) * 5;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, LIT);
    }
}
