package com.mistech.mistechmod.block.ritual;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Braseiro de Oricalco: peça do círculo Tier III (cardeais a 5 do altar). Precisa estar aceso.
 * Acende com isqueiro, carga de fogo ou Runa de Fogo; apaga com pá, balde/frasco de água ou chuva forte (tempestade).
 * Aceso: chama de alma alaranjada, luz 15, queima quem pisar.
 */
public class OrichalcumBrazierBlock extends Block {
    public static final MapCodec<OrichalcumBrazierBlock> CODEC = simpleCodec(OrichalcumBrazierBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),
            Block.box(6, 2, 6, 10, 8, 10),
            Block.box(2, 8, 2, 14, 12, 14));

    public OrichalcumBrazierBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    public MapCodec<OrichalcumBrazierBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        boolean lit = state.getValue(LIT);
        boolean igniter = stack.getItem() instanceof net.minecraft.world.item.FlintAndSteelItem || stack.is(Items.FIRE_CHARGE) || stack.is(Mistech.FIRE_RUNE.get());
        boolean douser = stack.is(ItemTags.SHOVELS) || stack.is(Items.WATER_BUCKET) || stack.is(Items.POTION);
        if (!lit && igniter) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS, 1.0F, 0.9F);
                if (stack.is(Items.FIRE_CHARGE)) {
                    stack.consume(1, player);
                } else {
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (lit && douser) {
            if (!level.isClientSide()) {
                extinguish(level, pos, state);
                if (stack.is(ItemTags.SHOVELS)) {
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    public static void extinguish(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT);
    }

    /** Tempestade em cima do braseiro apaga o fogo (cuidado com o Chamado da Tempestade). */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && level.isThundering() && level.isRainingAt(pos.above()) && random.nextInt(4) == 0) {
            extinguish(level, pos, state);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        if (state.getValue(LIT) && level instanceof ServerLevel server && entity.getY() >= pos.getY() + 0.7) {
            entity.igniteForSeconds(3.0F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.8, z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                    x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.03, 0.0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.3, z, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(12) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }
}
