package com.mistech.mistechmod.garden;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mandrágora: planta em terra arada, 4 estágios (como a beterraba, um pouco mais lenta).
 * Madura, as folhas tremem e ela resmunga baixinho. Ao ser arrancada, GRITA: quem está perto e sem capacete
 * fica tonto (Náusea) e vê tudo escurecer por um instante.
 */
public class MandrakeCropBlock extends CropBlock {
    public static final MapCodec<MandrakeCropBlock> CODEC = simpleCodec(MandrakeCropBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int MAX_AGE = 3;
    public static final double SCREAM_RADIUS = 8.0;
    private static final VoxelShape[] SHAPES = Block.boxes(MAX_AGE, age -> Block.column(16.0, 0.0, 3 + age * 3));

    public MandrakeCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MandrakeCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return GardenContent.MANDRAKE_SEEDS.get();
    }

    /** Cresce em ~metade das tentativas (é uma planta teimosa). */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextBoolean()) {
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return 1;
    }

    /** Madura: as folhas tremem e às vezes ela resmunga. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!this.isMaxAge(state)) {
            return;
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(160) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.BLOCKS,
                    0.3F, 0.5F + random.nextFloat() * 0.2F, false);
        }
    }

    /** Arrancou madura (mão, água, pistão...): grita. */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (this.isMaxAge(state)) {
            scream(level, pos);
        }
    }

    /** O grito da Mandrágora: capacete abafa (só um susto). */
    public static void scream(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        level.playSound(null, pos, SoundEvents.GHAST_SCREAM, SoundSource.BLOCKS, 1.0F, 1.7F + random.nextFloat() * 0.2F);
        level.playSound(null, pos, SoundEvents.FOX_SCREECH, SoundSource.BLOCKS, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.03);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(pos.getCenter()) > SCREAM_RADIUS * SCREAM_RADIUS) {
                continue;
            }
            if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.mandrake.muffled"));
                continue;
            }
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 10 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 3 * 20, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 4 * 20, 1));
            player.sendOverlayMessage(Component.translatable("message.mistech.mandrake.scream"));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }
}
