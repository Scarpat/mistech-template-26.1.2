package com.mistech.mistechmod.block.goblin;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.GoblinContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fechadura de Ossos: fica ao lado da porta da arena da Rainha Duende.
 * Botão direito com a Chave de Ossos encaixa uma chave; com as 3 o selo da porta some.
 */
public class BoneLockBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BoneLockBlock> CODEC = simpleCodec(BoneLockBlock::new);
    public static final int NEEDED = 3;
    public static final IntegerProperty KEYS = IntegerProperty.create("keys", 0, NEEDED);

    public BoneLockBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KEYS, 0));
    }

    @Override
    public MapCodec<BoneLockBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(GoblinContent.BONE_KEY.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int keys = state.getValue(KEYS);
        if (keys >= NEEDED) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        stack.consume(1, player);
        keys++;
        server.setBlock(pos, state.setValue(KEYS, keys), Block.UPDATE_ALL);
        Direction face = state.getValue(FACING);
        server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5 + face.getStepX() * 0.6, pos.getY() + 0.5,
                pos.getZ() + 0.5 + face.getStepZ() * 0.6, 10, 0.2, 0.2, 0.2, 0.02);
        server.playSound(null, pos, SoundEvents.SKELETON_HURT, SoundSource.BLOCKS, 1.0F, 0.6F);
        server.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.6F);
        if (keys < NEEDED) {
            player.sendOverlayMessage(Component.translatable("message.mistech.bone_lock.progress", keys, NEEDED));
        } else {
            GoblinSealBlock.dissolveNearest(server, pos, 4);
            server.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 1.4F);
            for (Player p : server.getEntitiesOfClass(Player.class, new AABB(pos).inflate(24))) {
                p.sendSystemMessage(Component.translatable("message.mistech.bone_lock.open"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int keys = state.getValue(KEYS);
        if (keys < NEEDED && !level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.bone_lock.hint", NEEDED - keys));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KEYS);
    }
}
