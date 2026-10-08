package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bicicleta do Gnomo: clique com um Gnomo Engarrafado para colocar o gnomo para pedalar (o pote vazio volta).
 * Clique com um Pote de Vidro vazio para tirar o gnomo de volta. Quebrar a bicicleta devolve o Gnomo Engarrafado.
 * GNOME = tem gnomo montado; LIT = está pedalando (gerando).
 */
public class GnomeBicycleBlock extends MachineBlock {
    public static final MapCodec<GnomeBicycleBlock> CODEC = simpleCodec(GnomeBicycleBlock::new);
    public static final BooleanProperty GNOME = BooleanProperty.create("gnome");
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public GnomeBicycleBlock(BlockBehaviour.Properties properties) {
        super(properties, MistechBlockEntities.GNOME_BICYCLE);
        this.registerDefaultState(this.defaultBlockState().setValue(GNOME, false));
    }

    @Override
    public MapCodec<GnomeBicycleBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        boolean hasGnome = state.getValue(GNOME);
        if (!hasGnome && stack.is(GnomeContent.BOTTLED_GNOME_ITEM.get())) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(GNOME, true), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                give(player, new ItemStack(Mistech.GLASS_JAR_ITEM.get()));
                level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.7F, 1.8F);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            6, 0.3, 0.3, 0.3, 0.0);
                }
                player.sendOverlayMessage(Component.translatable("message.mistech.gnome_bicycle.mounted"));
            }
            return InteractionResult.SUCCESS;
        }
        if (hasGnome && stack.is(Mistech.GLASS_JAR_ITEM.get()) && !stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(GNOME, false).setValue(LIT, false), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                give(player, new ItemStack(GnomeContent.BOTTLED_GNOME_ITEM.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 1.2F);
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.8F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** Pedalando: notas musicais e faíscas no dínamo. Gnomo parado (com fome): fumacinha. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(GNOME)) {
            return;
        }
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        if (state.getValue(LIT)) {
            if (random.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.NOTE, x, pos.getY() + 1.6, z, random.nextInt(24) / 24.0, 0.0, 0.0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.3,
                        pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.04, 0.0);
            }
        } else if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.5, z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GNOME);
    }
}
