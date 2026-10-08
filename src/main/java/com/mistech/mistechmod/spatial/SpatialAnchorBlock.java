package com.mistech.mistechmod.spatial;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.SpatialContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Âncora Espacial: pilar de 2 blocos que funciona como uma waystone. Tocar (botão direito) registra a âncora para você
 * e abre a lista das âncoras que você conhece; escolha uma para se teletransportar até ela (custa níveis pela distância).
 * O nome vem do item renomeado na bigorna (sem nome: "Âncora x, z"). Quebrar qualquer metade derruba o pilar inteiro.
 */
public class SpatialAnchorBlock extends Block {
    public static final MapCodec<SpatialAnchorBlock> CODEC = simpleCodec(SpatialAnchorBlock::new);

    public enum Half implements StringRepresentable {
        LOWER("lower"), UPPER("upper");

        private final String name;

        Half(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    /** Cor da runa e do cristal: 0 = violeta original, 1..16 = corante (id + 1). Shift + botão direito com corante. */
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 16);
    /** Violeta original (cor 0). */
    public static final int DEFAULT_COLOR = 0xFFA66BEB;

    /** Cor (ARGB) para o tingimento do modelo. */
    public static int tint(BlockState state) {
        int color = state.getValue(COLOR);
        return color == 0 ? DEFAULT_COLOR : 0xFF000000 | DyeColor.byId(color - 1).getTextureDiffuseColor();
    }

    private static final VoxelShape LOWER_SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(3, 3, 3, 13, 16, 13));
    private static final VoxelShape UPPER_SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 8, 13), Block.box(2, 8, 2, 14, 11, 14),
            Block.box(5, 11, 5, 11, 15, 11));

    public SpatialAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, Half.LOWER).setValue(COLOR, 0));
    }

    @Override
    protected MapCodec<SpatialAnchorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, COLOR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == Half.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    public static BlockPos baseOf(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == Half.UPPER ? pos.below() : pos;
    }

    // ----- colocar -----

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() + 1 < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, Half.UPPER).setValue(COLOR, state.getValue(COLOR)), Block.UPDATE_ALL);
        if (level instanceof ServerLevel server) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            String name = custom != null ? custom.getString() : Component.translatable("block.mistech.spatial_anchor.default_name",
                    pos.getX(), pos.getZ()).getString();
            SpatialAnchors.get(server.getServer()).add(new SpatialAnchors.Anchor(server.dimension().identifier(), pos.immutable(), name));
            if (placer instanceof ServerPlayer player) {
                SpatialService.learn(player, SpatialAnchors.key(server, pos));
            }
        }
    }

    // ----- usar -----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            BlockPos base = baseOf(pos, state);
            String key = SpatialAnchors.key(server, base);
            SpatialAnchors anchors = SpatialAnchors.get(server.getServer());
            if (anchors.get(key) == null) {
                // âncora antiga ou colocada por comando: registra agora
                anchors.add(new SpatialAnchors.Anchor(server.dimension().identifier(), base.immutable(),
                        Component.translatable("block.mistech.spatial_anchor.default_name", base.getX(), base.getZ()).getString()));
            }
            SpatialService.learn(serverPlayer, key);
            SpatialService.openList(serverPlayer, key);
        }
        return InteractionResult.SUCCESS;
    }

    // ----- quebrar: sai inteiro, e a âncora some da lista -----

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Half half = state.getValue(HALF);
        if (half == Half.LOWER && direction == Direction.UP && !(neighborState.is(this) && neighborState.getValue(HALF) == Half.UPPER)
                || half == Half.UPPER && direction == Direction.DOWN && !(neighborState.is(this) && neighborState.getValue(HALF) == Half.LOWER)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** O item só sai da base (loot table). Quebrando o topo, a base solta o item aqui. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HALF) == Half.UPPER) {
            BlockPos base = pos.below();
            BlockState baseState = level.getBlockState(base);
            if (baseState.is(this)) {
                if (!player.preventsBlockDrops() && player.hasCorrectToolForDrops(baseState, level, base)) {
                    dropResources(baseState, level, base, null, player, player.getMainHandItem());
                }
                level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.getValue(HALF) == Half.LOWER && !level.getBlockState(pos).is(this)) {
            SpatialAnchors.get(level.getServer()).remove(SpatialAnchors.key(level, pos));
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == Half.UPPER) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 1.5, -random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 1.5);
            }
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 1.0,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
            }
        }
    }

    /** Shift + botão direito com corante: pinta a runa e o cristal das duas metades. @return se pintou. */
    public static boolean dye(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        DyeColor dye = stack.get(DataComponents.DYE);
        if (dye == null) {
            return false;
        }
        BlockPos base = baseOf(pos, state);
        BlockState baseState = level.getBlockState(base);
        if (!baseState.is(state.getBlock()) || baseState.getValue(COLOR) == dye.getId() + 1) {
            return false;
        }
        if (!level.isClientSide()) {
            int color = dye.getId() + 1;
            level.setBlock(base, baseState.setValue(COLOR, color), Block.UPDATE_ALL);
            BlockState top = level.getBlockState(base.above());
            if (top.is(state.getBlock())) {
                level.setBlock(base.above(), top.setValue(COLOR, color), Block.UPDATE_ALL);
            }
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DYE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.WITCH, base.getX() + 0.5, base.getY() + 1.6, base.getZ() + 0.5, 12, 0.25, 0.4, 0.25, 0.02);
            }
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }
        return true;
    }

    /** Usado pelo serviço: a âncora ainda existe naquele lugar? */
    public static boolean isAnchor(Level level, BlockPos base) {
        BlockState state = level.getBlockState(base);
        return state.is(SpatialContent.SPATIAL_ANCHOR.get()) && state.getValue(HALF) == Half.LOWER;
    }
}
