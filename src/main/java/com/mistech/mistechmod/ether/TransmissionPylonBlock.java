package com.mistech.mistechmod.ether;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pilar de Transmissão: liga-se a até 4 pilares a até 48 blocos (Diapasão Arcano). Ver {@link PylonMesh}.
 * Botão direito (mão vazia) troca o modo: <b>Relé</b> (só repassa), <b>Entrada</b> (puxa do bloco embaixo, ex.: gerador)
 * ou <b>Saída</b> (entrega no bloco embaixo, ex.: máquina). Agachado mostra o estado da malha.
 */
public class TransmissionPylonBlock extends BaseEntityBlock {
    public static final MapCodec<TransmissionPylonBlock> CODEC = simpleCodec(TransmissionPylonBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<PylonMode> MODE =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("mode", PylonMode.class);

    public TransmissionPylonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, PylonMode.RELAY));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.@Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(MODE) != PylonMode.INPUT ? null
                : createTickerHelper(type, EtherContent.PYLON_BE.get(), TransmissionPylonBlockEntity::serverTick);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && level.getBlockEntity(pos) instanceof TransmissionPylonBlockEntity be) {
            if (!player.isSecondaryUseActive()) {
                PylonMode mode = state.getValue(MODE).next();
                level.setBlock(pos, state.setValue(MODE, mode), 3);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F,
                        mode == PylonMode.INPUT ? 1.4F : mode == PylonMode.OUTPUT ? 0.8F : 1.1F);
            }
            be.showStatus(server, player);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public MapCodec<TransmissionPylonBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransmissionPylonBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
    }
}
