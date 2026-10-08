package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mistech.mistechmod.astrology.AstrologyField;
import com.mistech.mistechmod.network.OpenAstrologyPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/** Mesa de Astrologia: nasce de uma Mesa de Escrita sobre o Campo de Astrologia. Abre o mapa do céu. */
public class AstrologyTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AstrologyTableBlock> CODEC = simpleCodec(AstrologyTableBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 12.0, 0.0, 16.0, 14.5, 16.0),
            Block.box(1.0, 0.0, 1.0, 3.0, 12.0, 3.0),
            Block.box(13.0, 0.0, 1.0, 15.0, 12.0, 3.0),
            Block.box(1.0, 0.0, 13.0, 3.0, 12.0, 15.0),
            Block.box(13.0, 0.0, 13.0, 15.0, 12.0, 15.0));

    public AstrologyTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<AstrologyTableBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (AstrologyField.isValid(level, pos)) {
                com.mistech.mistechmod.astrology.AstralPillars.refresh(serverPlayer, pos);
                PacketDistributor.sendToPlayer(serverPlayer, new OpenAstrologyPayload(pos));
            } else {
                serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.astrology_field_broken"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Brilhinhos subindo da mesa quando o campo está completo. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && AstrologyField.isValid(level, pos)) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.0, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
