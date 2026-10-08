package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mesa de Escrita: "crafting table" inicial das runas. A cor do pano vem do tapete usado na receita. Guarda os itens (block entity). */
public class WritingTableBlock extends BaseEntityBlock {
    public static final MapCodec<WritingTableBlock> CODEC = simpleCodec(WritingTableBlock::new);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Variante de Carvalho Pálido (receita com laje de carvalho pálido). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty PALE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("pale");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 12.0, 0.0, 16.0, 14.5, 16.0),   // tampo + pano
            Block.box(1.0, 0.0, 1.0, 3.0, 12.0, 3.0),      // pernas
            Block.box(13.0, 0.0, 1.0, 15.0, 12.0, 3.0),
            Block.box(1.0, 0.0, 13.0, 3.0, 12.0, 15.0),
            Block.box(13.0, 0.0, 13.0, 15.0, 12.0, 15.0));

    public WritingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, DyeColor.WHITE).setValue(FACING, Direction.NORTH).setValue(PALE, false));
    }

    @Override
    public MapCodec<WritingTableBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // a cor vem do componente block_state do item (aplicado pelo BlockItem depois daqui)
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WritingTableBlockEntity table) {
            player.openMenu(table);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Pedra Guia (não é gasta):
     * - Barril Espacial ao lado + 2 Tanques Rúnicos em cima = Confector de Runas;
     * - Campo de Astrologia completo embaixo = Mesa de Astrologia.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Mistech.GUIDE_STONE.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel) {
            // Barril Espacial ao lado = tentativa de montar o Confector de Runas
            if (com.mistech.mistechmod.block.confector.RuneConfectorBlock.tryAssemble(serverLevel, pos, state, player)) {
                return InteractionResult.SUCCESS;
            }
            // Círculo de Tarologia completo embaixo = Mesa de Tarologia (centro de obsidiana chorona)
            if (com.mistech.mistechmod.tarot.TarotField.looksLikeTarot(level, pos)) {
                BlockPos tarotError = com.mistech.mistechmod.tarot.TarotField.firstError(level, pos);
                if (tarotError != null) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.tarot.field_incomplete",
                            tarotError.getX(), tarotError.getY(), tarotError.getZ()));
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, tarotError.getX() + 0.5, tarotError.getY() + 1.2,
                            tarotError.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.0);
                    return InteractionResult.FAIL;
                }
                serverLevel.setBlock(pos, com.mistech.mistechmod.tarot.TarotContent.TAROT_TABLE.get().defaultBlockState()
                        .setValue(com.mistech.mistechmod.tarot.TarotTableBlock.FACING, state.getValue(FACING)), Block.UPDATE_ALL);
                serverLevel.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        60, 0.4, 0.8, 0.4, 0.08);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        120, 3.0, 0.3, 3.0, 0.5);
                serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.7F);
                serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.6F);
                return InteractionResult.SUCCESS;
            }
            BlockPos error = AstrologyField.firstError(level, pos);
            if (error != null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.astrology_field_incomplete",
                        error.getX(), error.getY(), error.getZ()));
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, error.getX() + 0.5, error.getY() + 1.2, error.getZ() + 0.5,
                        3, 0.2, 0.1, 0.2, 0.0);
                return InteractionResult.FAIL;
            }
            // os itens guardados na mesa caem no chão ao trocar o bloco
            serverLevel.setBlock(pos, Mistech.ASTROLOGY_TABLE.get().defaultBlockState()
                    .setValue(AstrologyTableBlock.FACING, state.getValue(FACING)), Block.UPDATE_ALL);
            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    60, 0.4, 0.8, 0.4, 0.08);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    120, 3.0, 0.3, 3.0, 0.5);
            serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.3F);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WritingTableBlockEntity(pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR, FACING, PALE);
    }
}
