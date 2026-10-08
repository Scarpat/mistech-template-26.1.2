package com.mistech.mistechmod.block.confector;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.FluidInteraction;
import com.mistech.mistechmod.block.RunicTankBlockEntity;
import com.mistech.mistechmod.block.SpatialBarrelBlockEntity;
import com.mistech.mistechmod.block.WritingTableBlock;
import com.mistech.mistechmod.block.WritingTableBlockEntity;
import com.mistech.mistechmod.menu.WritingTableMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Confector de Runas: multibloco 2x2 (Barril Espacial + Mesa de Escrita embaixo, 2 Tanques Rúnicos em cima),
 * montado clicando com a Pedra Guia na Mesa de Escrita. Funciona como uma Mesa de Escrita automática.
 * Quebrar qualquer parte desmonta tudo: devolve o barril, a mesa e os 2 tanques (com o fluido) e derruba os itens.
 */
public class RuneConfectorBlock extends BaseEntityBlock {
    public static final MapCodec<RuneConfectorBlock> CODEC = simpleCodec(RuneConfectorBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ConfectorPart> PART = EnumProperty.create("part", ConfectorPart.class);
    /** false = barril à esquerda da mesa (olhando de frente); true = à direita. */
    public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");

    private static final VoxelShape TANK_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);

    public RuneConfectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(PART, ConfectorPart.TABLE).setValue(MIRRORED, false));
    }

    @Override
    public MapCodec<RuneConfectorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, MIRRORED);
    }

    // ----- geometria do multibloco -----

    /** Direção da mesa para o barril. A "direita" de quem olha de frente é facing.getCounterClockWise(). */
    public static Direction barrelSide(Direction facing, boolean mirrored) {
        return mirrored ? facing.getCounterClockWise() : facing.getClockWise();
    }

    public static BlockPos masterPos(BlockState state, BlockPos pos) {
        Direction toBarrel = barrelSide(state.getValue(FACING), state.getValue(MIRRORED));
        return switch (state.getValue(PART)) {
            case TABLE -> pos;
            case BARREL -> pos.relative(toBarrel.getOpposite());
            case TABLE_TANK -> pos.below();
            case BARREL_TANK -> pos.below().relative(toBarrel.getOpposite());
        };
    }

    public static BlockPos partPos(BlockPos master, Direction facing, boolean mirrored, ConfectorPart part) {
        Direction toBarrel = barrelSide(facing, mirrored);
        return switch (part) {
            case TABLE -> master;
            case BARREL -> master.relative(toBarrel);
            case TABLE_TANK -> master.above();
            case BARREL_TANK -> master.above().relative(toBarrel);
        };
    }

    public static @Nullable RuneConfectorBlockEntity getMaster(Level level, BlockState state, BlockPos pos) {
        return level.getBlockEntity(masterPos(state, pos)) instanceof RuneConfectorBlockEntity master ? master : null;
    }

    // ----- montagem (Pedra Guia na Mesa de Escrita) -----

    /**
     * Tenta montar o Confector a partir da Mesa de Escrita em {@code tablePos}.
     * @return false se não tem Barril Espacial ao lado (aí a mesa segue com o comportamento normal da Pedra Guia);
     *         true se tentou (montou, ou avisou o que falta).
     */
    public static boolean tryAssemble(ServerLevel level, BlockPos tablePos, BlockState tableState, Player player) {
        Direction facing = tableState.getValue(WritingTableBlock.FACING);
        Boolean mirrored = null;
        for (boolean m : new boolean[] {false, true}) {
            if (level.getBlockState(tablePos.relative(barrelSide(facing, m))).is(Mistech.SPATIAL_BARREL.get())) {
                mirrored = m;
                break;
            }
        }
        if (mirrored == null) {
            return false;
        }
        BlockPos barrelPos = partPos(tablePos, facing, mirrored, ConfectorPart.BARREL);
        for (ConfectorPart part : new ConfectorPart[] {ConfectorPart.BARREL_TANK, ConfectorPart.TABLE_TANK}) {
            BlockPos tankPos = partPos(tablePos, facing, mirrored, part);
            if (!level.getBlockState(tankPos).is(Mistech.RUNIC_TANK.get())) {
                player.sendOverlayMessage(Component.translatable("message.mistech.confector_incomplete",
                        tankPos.getX(), tankPos.getY(), tankPos.getZ()));
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, tankPos.getX() + 0.5, tankPos.getY() + 0.8, tankPos.getZ() + 0.5,
                        3, 0.2, 0.1, 0.2, 0.0);
                return true;
            }
        }

        // junta o que estava nas peças
        ItemStack chalk = ItemStack.EMPTY;
        NonNullList<ItemStack> stored = NonNullList.create();
        if (level.getBlockEntity(tablePos) instanceof WritingTableBlockEntity table) {
            for (int i = 0; i < table.getContainerSize(); i++) {
                ItemStack stack = table.removeItemNoUpdate(i);
                if (i == WritingTableMenu.CHALK && chalk.isEmpty()) {
                    chalk = stack;
                } else if (!stack.isEmpty()) {
                    stored.add(stack);
                }
            }
        }
        if (level.getBlockEntity(barrelPos) instanceof SpatialBarrelBlockEntity barrel) {
            for (ItemStack stack : barrel.takeAllItems()) {
                if (!stack.isEmpty()) {
                    stored.add(stack);
                }
            }
        }
        FluidStack barrelTankFluid = takeFluid(level, partPos(tablePos, facing, mirrored, ConfectorPart.BARREL_TANK));
        FluidStack tableTankFluid = takeFluid(level, partPos(tablePos, facing, mirrored, ConfectorPart.TABLE_TANK));
        DyeColor color = tableState.getValue(WritingTableBlock.COLOR);

        // troca as 4 peças pelas partes do Confector
        BlockState base = Mistech.RUNE_CONFECTOR.get().defaultBlockState().setValue(FACING, facing).setValue(MIRRORED, mirrored);
        for (ConfectorPart part : ConfectorPart.values()) {
            level.setBlock(partPos(tablePos, facing, mirrored, part), base.setValue(PART, part), Block.UPDATE_ALL);
        }

        if (level.getBlockEntity(tablePos) instanceof RuneConfectorBlockEntity master) {
            master.setup(stored, chalk, barrelTankFluid, tableTankFluid, color, tablePos);
        }

        level.sendParticles(ParticleTypes.ENCHANT, tablePos.getX() + 0.5, tablePos.getY() + 1.0, tablePos.getZ() + 0.5,
                150, 1.2, 1.0, 1.2, 0.6);
        level.sendParticles(ParticleTypes.WITCH, tablePos.getX() + 0.5, tablePos.getY() + 1.2, tablePos.getZ() + 0.5,
                40, 1.0, 0.8, 1.0, 0.05);
        level.playSound(null, tablePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.playSound(null, tablePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.7F);
        return true;
    }

    private static FluidStack takeFluid(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RunicTankBlockEntity tank) {
            FluidStack fluid = tank.getTank().getFluid().copy();
            tank.getTank().setFluid(FluidStack.EMPTY);
            return fluid;
        }
        return FluidStack.EMPTY;
    }

    // ----- desmontar -----

    /** Uma parte sumiu (quebrada, explodida...): some com as outras. A parte principal derruba tudo ao sair. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockPos master = masterPos(state, pos);
        Direction facing = state.getValue(FACING);
        boolean mirrored = state.getValue(MIRRORED);
        // primeiro a principal (derruba itens e as peças), depois o resto
        List<ConfectorPart> order = List.of(ConfectorPart.TABLE, ConfectorPart.BARREL, ConfectorPart.TABLE_TANK, ConfectorPart.BARREL_TANK);
        for (ConfectorPart part : order) {
            BlockPos other = partPos(master, facing, mirrored, part);
            if (other.equals(pos)) {
                continue;
            }
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this) && otherState.getValue(PART) == part && masterPos(otherState, other).equals(master)) {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    // ----- interação -----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            RuneConfectorBlockEntity master = getMaster(level, state, pos);
            if (master != null) {
                player.openMenu(master);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Balde/garrafa/frasco clicando num dos tanques de cima. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(PART).isTank() || !FluidInteraction.isFluidItem(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        RuneConfectorBlockEntity master = getMaster(level, state, pos);
        if (master != null && FluidInteraction.interact(player, hand, level, pos, master.getTank(state.getValue(PART).tankIndex()))) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    // ----- block entity (só na parte principal) -----

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ConfectorPart.TABLE ? new RuneConfectorBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.RUNE_CONFECTOR.get(), RuneConfectorBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == ConfectorPart.TABLE && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + (random.nextFloat() - 0.5),
                    pos.getY() + 1.1 + random.nextFloat() * 0.5, pos.getZ() + 0.5 + (random.nextFloat() - 0.5), 0.0, -0.3, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART).isTank() ? TANK_SHAPE : Shapes.block();
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        RuneConfectorBlockEntity master = getMaster(level, state, pos);
        return master != null ? master.getComparatorLevel() : 0;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
