package com.mistech.mistechmod.husbandry;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.GlassJarBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pote com Criatura. O bicho olha para quem colocou o pote.
 * - Mão vazia: pega os produtos (ou mostra quanto falta). Shift + mão vazia: solta o bicho (o pote volta vazio).
 * - Balde vazio (vaca): tira um balde de leite.
 */
public class CreatureJarBlock extends BaseEntityBlock {
    public static final MapCodec<CreatureJarBlock> CODEC = simpleCodec(CreatureJarBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CreatureJarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<CreatureJarBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreatureJarBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, HusbandryContent.CREATURE_JAR_BE.get(), CreatureJarBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.BUCKET) || !(level.getBlockEntity(pos) instanceof CreatureJarBlockEntity jar)
                || jar.kind() != JarCreatures.Kind.COW) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            if (jar.milkInto()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.MILK_BUCKET)));
                level.playSound(null, pos, SoundEvents.COW_MILK, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                player.sendOverlayMessage(jar.statusMessage());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof CreatureJarBlockEntity jar)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            release(server, pos, state, jar);
            return InteractionResult.SUCCESS;
        }
        ItemStack out = jar.takeOutput();
        if (!out.isEmpty()) {
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.2F);
        } else {
            player.sendOverlayMessage(jar.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    /** Solta o bicho na frente do pote; o bloco vira um Pote de Vidro vazio (os produtos caem). */
    private static void release(ServerLevel level, BlockPos pos, BlockState state, CreatureJarBlockEntity jar) {
        if (jar.getCreature() != null) {
            Entity creature = JarCreatures.recreate(jar.getCreature(), level, EntitySpawnReason.BUCKET);
            if (creature != null) {
                BlockPos spot = pos.relative(state.getValue(FACING));
                if (!level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()) {
                    spot = pos.above();
                }
                creature.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, state.getValue(FACING).toYRot(), 0.0F);
                if (creature instanceof Mob mob) {
                    mob.setPersistenceRequired();
                }
                level.addFreshEntity(creature);
                level.sendParticles(ParticleTypes.POOF, creature.getX(), creature.getY() + 0.4, creature.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            }
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        // trocar o bloco derruba os produtos/baldes que estavam no pote
        level.setBlock(pos, Mistech.GLASS_JAR.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Às vezes o bicho "mexe" no pote. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(40) == 0) {
            level.addParticle(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GlassJarBlock.SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CreatureJarBlockEntity jar ? jar.comparatorSignal() : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
