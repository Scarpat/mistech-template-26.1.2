package com.mistech.mistechmod.conduit;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Conduto Rúnico: um cano só para itens, fluidos e energia.
 * <ul>
 * <li>Liga sozinho em outros condutos e em blocos com inventário, tanque ou energia.</li>
 * <li>Cada ponta encostada num bloco tem um modo: <b>entrega</b> (padrão: recebe o que a rede manda), <b>puxa</b> (tira do bloco e
 * manda para as pontas de entrega) ou <b>desligada</b>. Agachar + clique com a mão vazia na ponta troca o modo.</li>
 * <li>Velocidade pelo nível ({@link ConduitTier}: Rúnico, Mitril, Adamantina, Oricalco); numa rede misturada vale o mais lento.</li>
 * <li>Agachar + clique no miolo troca o que o conduto leva ({@link ConduitCarry}); clique no miolo sem agachar mostra o estado.</li>
 * </ul>
 */
public class RunicConduitBlock extends BaseEntityBlock {
    private static final Map<ConduitTier, MapCodec<RunicConduitBlock>> CODECS = new EnumMap<>(ConduitTier.class);

    private final ConduitTier tier;

    public ConduitTier tier() {
        return this.tier;
    }

    /** O que há em cada lado. */
    public enum Side implements StringRepresentable {
        NONE, CONDUIT, LINK, PULL, OFF;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Ponta num bloco (não conduto). */
        public boolean endpoint() {
            return this == LINK || this == PULL || this == OFF;
        }
    }

    public static final Map<Direction, EnumProperty<Side>> SIDES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.values()) {
            SIDES.put(d, EnumProperty.create(d.getSerializedName(), Side.class));
        }
    }

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    /** Ponta desligada: só um tampão encostado no miolo (dá para clicar e religar). */
    private static final Map<Direction, VoxelShape> STUBS = new EnumMap<>(Direction.class);

    static {
        STUBS.put(Direction.NORTH, Block.box(6, 6, 3.5, 10, 10, 5));
        STUBS.put(Direction.SOUTH, Block.box(6, 6, 11, 10, 10, 12.5));
        STUBS.put(Direction.WEST, Block.box(3.5, 6, 6, 5, 10, 10));
        STUBS.put(Direction.EAST, Block.box(11, 6, 6, 12.5, 10, 10));
        STUBS.put(Direction.DOWN, Block.box(6, 3.5, 6, 10, 5, 10));
        STUBS.put(Direction.UP, Block.box(6, 11, 6, 10, 12.5, 10));
    }

    static {
        ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 5));
        ARMS.put(Direction.SOUTH, Block.box(6, 6, 11, 10, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 6, 5, 10, 10));
        ARMS.put(Direction.EAST, Block.box(11, 6, 6, 16, 10, 10));
        ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 5, 10));
        ARMS.put(Direction.UP, Block.box(6, 11, 6, 10, 16, 10));
    }

    public RunicConduitBlock(BlockBehaviour.Properties properties, ConduitTier tier) {
        super(properties);
        this.tier = tier;
        BlockState state = this.stateDefinition.any();
        for (EnumProperty<Side> p : SIDES.values()) {
            state = state.setValue(p, Side.NONE);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected MapCodec<RunicConduitBlock> codec() {
        return CODECS.computeIfAbsent(this.tier, t -> simpleCodec(p -> new RunicConduitBlock(p, t)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RunicConduitBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ConduitContent.RUNIC_CONDUIT_BE.get(), RunicConduitBlockEntity::serverTick);
    }

    // ----- ligações -----

    /** O vizinho aceita ligação? Conduto, ou bloco com entidade/capability de item, fluido ou energia. */
    private static boolean connectable(LevelReader level, BlockPos pos, Direction dir, BlockState neighbor) {
        if (neighbor.getBlock() instanceof RunicConduitBlock) {
            return true;
        }
        if (neighbor.isAir()) {
            return false;
        }
        if (level instanceof Level l) {
            BlockPos at = pos.relative(dir);
            Direction face = dir.getOpposite();
            if (l.getCapability(Capabilities.Item.BLOCK, at, face) != null || l.getCapability(Capabilities.Fluid.BLOCK, at, face) != null
                    || l.getCapability(Capabilities.Energy.BLOCK, at, face) != null) {
                return true;
            }
        }
        return neighbor.hasBlockEntity();
    }

    private static Side sideFor(LevelReader level, BlockPos pos, Direction dir, BlockState neighbor, Side current) {
        if (neighbor.getBlock() instanceof RunicConduitBlock) {
            // desligado de um lado = desligado dos dois (o tampão aparece nos dois e qualquer um religa)
            return neighbor.getValue(SIDES.get(dir.getOpposite())) == Side.OFF ? Side.OFF : Side.CONDUIT;
        }
        if (!connectable(level, pos, dir, neighbor)) {
            return Side.NONE;
        }
        // mantém o modo escolhido pelo jogador
        return current == Side.PULL || current == Side.OFF ? current : Side.LINK;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction d : Direction.values()) {
            state = state.setValue(SIDES.get(d), sideFor(level, pos, d, level.getBlockState(pos.relative(d)), Side.NONE));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        RunicConduitBlockEntity.invalidateNetworks();
        return state.setValue(SIDES.get(direction), sideFor(level, pos, direction, neighborState, state.getValue(SIDES.get(direction))));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            net.minecraft.world.level.redstone.@Nullable Orientation orientation, boolean movedByPiston) {
        // um bloco ganhou/perdeu a entidade (máquina colocada): reavalia as pontas
        BlockState updated = state;
        for (Direction d : Direction.values()) {
            updated = updated.setValue(SIDES.get(d), sideFor(level, pos, d, level.getBlockState(pos.relative(d)), state.getValue(SIDES.get(d))));
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
        RunicConduitBlockEntity.invalidateNetworks();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        RunicConduitBlockEntity.invalidateNetworks();
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    // ----- formato -----

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction d : Direction.values()) {
            Side s = state.getValue(SIDES.get(d));
            if (s == Side.CONDUIT || s == Side.LINK || s == Side.PULL) {
                shape = Shapes.or(shape, ARMS.get(d));
            } else if (s == Side.OFF) {
                shape = Shapes.or(shape, STUBS.get(d));
            }
        }
        return shape;
    }

    // ----- trocar o modo de uma ponta -----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean core = onCore(pos, hit.getLocation());
        if (core) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RunicConduitBlockEntity be) {
                if (player.isSecondaryUseActive()) {
                    be.setCarry(be.carry().next());
                    level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
                }
                player.sendOverlayMessage(Component.translatable("message.mistech.conduit.state",
                        Component.translatable(be.carry().key()), this.tier.items, this.tier.interval / 20.0F, this.tier.fluid, this.tier.energy)
                        .withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Direction dir = this.armAt(state, pos, hit.getLocation(), hit.getDirection());
        if (dir == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Side now = state.getValue(SIDES.get(dir));
            Side next = switch (now) {
                case LINK -> Side.PULL;
                case PULL -> Side.OFF;
                case OFF -> Side.LINK;
                case CONDUIT -> Side.OFF;
                default -> now;
            };
            if (now == Side.OFF && level.getBlockState(pos.relative(dir)).getBlock() instanceof RunicConduitBlock) {
                next = Side.CONDUIT;
            }
            level.setBlock(pos, state.setValue(SIDES.get(dir), next), Block.UPDATE_ALL);
            // conduto com conduto: desligar vale dos dois lados
            BlockState other = level.getBlockState(pos.relative(dir));
            if (other.getBlock() instanceof RunicConduitBlock) {
                EnumProperty<Side> back = SIDES.get(dir.getOpposite());
                level.setBlock(pos.relative(dir), other.setValue(back, next == Side.OFF ? Side.OFF : Side.CONDUIT), Block.UPDATE_ALL);
            }
            RunicConduitBlockEntity.invalidateNetworks();
            player.sendOverlayMessage(Component.translatable("message.mistech.runic_conduit." + next.getSerializedName(),
                    Component.translatable("direction.mistech." + dir.getSerializedName())).withStyle(ChatFormatting.GOLD));
            level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6F, next == Side.PULL ? 1.4F : 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    /** O clique foi no miolo (cubo de 6 pixels no meio)? */
    private static boolean onCore(BlockPos pos, Vec3 hit) {
        Vec3 d = hit.subtract(Vec3.atCenterOf(pos));
        return Math.max(Math.abs(d.x), Math.max(Math.abs(d.y), Math.abs(d.z))) <= 0.19;
    }

    /** A ponta clicada (braço ou tampão): a direção do ponto clicado em relação ao centro. */
    private @Nullable Direction armAt(BlockState state, BlockPos pos, Vec3 hit, Direction face) {
        Vec3 d = hit.subtract(Vec3.atCenterOf(pos));
        Direction best = Direction.getApproximateNearest(d.x, d.y, d.z);
        return state.getValue(SIDES.get(best)) != Side.NONE ? best : null;
    }
}
