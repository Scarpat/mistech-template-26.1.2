package com.mistech.mistechmod.utility;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Elevador Rúnico: pular em cima leva até o próximo elevador da mesma cor acima (até {@link #RANGE} blocos); agachar leva
 * até o de baixo. O destino precisa de 2 blocos livres em cima. Botão direito com corante pinta a runa (só liga com a mesma cor).
 */
public class RunicElevatorBlock extends Block {
    public static final MapCodec<RunicElevatorBlock> CODEC = simpleCodec(RunicElevatorBlock::new);
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 16);
    public static final int DEFAULT_COLOR = 0xFFA66BEB;
    public static final int RANGE = 32;

    public RunicElevatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    public static int tint(BlockState state) {
        int color = state.getValue(COLOR);
        return color == 0 ? DEFAULT_COLOR : 0xFF000000 | DyeColor.byId(color - 1).getTextureDiffuseColor();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        DyeColor dye = stack.get(DataComponents.DYE);
        if (dye == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (state.getValue(COLOR) == dye.getId() + 1) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(COLOR, dye.getId() + 1), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Pacote do cliente: o jogador em cima de um elevador pulou ({@code up}) ou agachou. */
    public static void travel(ServerPlayer player, boolean up) {
        ServerLevel level = player.level();
        BlockPos from = BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
        BlockState state = level.getBlockState(from);
        if (!(state.getBlock() instanceof RunicElevatorBlock) || player.isPassenger()) {
            return;
        }
        int color = state.getValue(COLOR);
        int step = up ? 1 : -1;
        for (int i = 1; i <= RANGE; i++) {
            BlockPos pos = from.above(i * step);
            if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY()) {
                return;
            }
            BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof RunicElevatorBlock && s.getValue(COLOR) == color) {
                if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                        || !level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty()) {
                    continue; // destino tapado: procura o próximo
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.5, player.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
                player.teleportTo(player.getX(), pos.getY() + 1.0, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
                player.hurtMarked = true;
                player.resetFallDistance();
                level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, up ? 1.6F : 1.2F);
                level.sendParticles(ParticleTypes.WITCH, player.getX(), pos.getY() + 1.2, player.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                return;
            }
        }
    }
}
