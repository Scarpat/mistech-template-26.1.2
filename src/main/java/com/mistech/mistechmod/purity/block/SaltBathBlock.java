package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Banho de Sal: caldeirão com água + Sal Grosso (clique com o sal num caldeirão cheio de água).
 * Entrar tira {@link #CLEANSE} de Mácula (no máximo até 20), uma vez por dia de jogo; cada banho gasta 1 nível.
 */
public class SaltBathBlock extends Block {
    public static final MapCodec<SaltBathBlock> CODEC = simpleCodec(SaltBathBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);
    public static final float CLEANSE = 10.0F;

    private static final VoxelShape INSIDE = Block.box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(),
            Shapes.or(Block.box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0), Block.box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0),
                    Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0), INSIDE), BooleanOp.ONLY_FIRST);

    public SaltBathBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 3));
    }

    @Override
    public MapCodec<SaltBathBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.isOnFire()) {
            entity.clearFire();
        }
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player) || entity.getY() > pos.getY() + 0.95) {
            return;
        }
        long day = level.getOverworldClockTime() / 24000L;
        if (player.getData(PurityContent.LAST_SALT_BATH) == day) {
            return;
        }
        player.setData(PurityContent.LAST_SALT_BATH, day);
        float before = Taint.get(player);
        Taint.cleanse(player, CLEANSE);
        player.sendOverlayMessage(Component.translatable(before > Taint.EARLY_FLOOR ? "message.mistech.salt_bath.done" : "message.mistech.salt_bath.floor",
                Taint.describe(Taint.get(player))).withStyle(ChatFormatting.AQUA));
        server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
        server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 0.8F, 1.2F);
        int lvl = state.getValue(LEVEL);
        level.setBlock(pos, lvl > 1 ? state.setValue(LEVEL, lvl - 1) : Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double y = pos.getY() + 0.4 + state.getValue(LEVEL) * 0.18;
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.2 + random.nextDouble() * 0.6, y, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }
}
