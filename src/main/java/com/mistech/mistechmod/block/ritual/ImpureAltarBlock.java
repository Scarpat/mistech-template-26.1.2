package com.mistech.mistechmod.block.ritual;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.RitualContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Altar Impuro: centro do multibloco de rituais Tier I.
 * <ul>
 * <li>Frasco de Essência Impura = +1 nível, balde = +3 (máx. 8); frasco vazio tira 1 nível.</li>
 * <li>Outro item = catalisador (1 unidade); mão vazia tira o catalisador (ou o resultado do ritual).</li>
 * <li>Pedra Guia = começa o ritual. Adaga Ritual agachado = encerra sem refluxo.</li>
 * </ul>
 */
public class ImpureAltarBlock extends BaseEntityBlock {
    public static final MapCodec<ImpureAltarBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.intRange(1, 3).fieldOf("tier").forGetter(ImpureAltarBlock::tier),
            propertiesCodec()).apply(i, (tier, props) -> new ImpureAltarBlock(props, tier)));
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),     // base
            Block.box(4, 3, 4, 12, 10, 12),    // coluna
            Block.box(1, 10, 1, 15, 14, 15));  // bacia

    /** Tier deste altar: cada tier é um bloco (e item) próprio — Tier II e III só nascem do Ritual de Ascensão. */
    private final int tier;

    public ImpureAltarBlock(BlockBehaviour.Properties properties) {
        this(properties, 1);
    }

    public ImpureAltarBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, false).setValue(RUNNING, false));
    }

    public int tier() {
        return this.tier;
    }

    @Override
    public MapCodec<ImpureAltarBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImpureAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, RitualContent.IMPURE_ALTAR_BE.get(), ImpureAltarBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ImpureAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (stack.is(Mistech.GUIDE_STONE.get())) {
            if (level instanceof ServerLevel server) {
                altar.tryStart(server, player);
            }
            return InteractionResult.SUCCESS;
        }
        if ((stack.is(RitualContent.RITUAL_DAGGER.get()) || stack.is(com.mistech.mistechmod.beyond.PaleNetherContent.INFERNAL_DAGGER.get()))
                && player.isShiftKeyDown() && altar.isRunning()) {
            if (level instanceof ServerLevel server) {
                altar.cancel(server, player);
            }
            return InteractionResult.SUCCESS;
        }
        // essência (no Nether a Impura ferve e evapora: só a Ígnea serve)
        boolean nether = level.dimension() == Level.NETHER;
        boolean igneous = stack.is(com.mistech.mistechmod.beyond.PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get());
        int add = stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) ? 1 : stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get()) ? 3 : igneous ? 1 : 0;
        if (add > 0 && nether && !igneous) {
            if (level instanceof ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        12, 0.25, 0.1, 0.25, 0.02);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            player.sendOverlayMessage(Component.translatable("message.mistech.altar.evaporates"));
            return InteractionResult.FAIL;
        }
        if (add > 0) {
            if (altar.getEssence() + add > ImpureAltarBlockEntity.MAX_ESSENCE) {
                player.sendOverlayMessage(Component.translatable("message.mistech.altar.full"));
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                altar.setEssence(altar.getEssence() + add);
                giveBack(player, stack, new ItemStack(add == 1 ? Items.GLASS_BOTTLE : Items.BUCKET));
                level.playSound(null, pos, add == 1 ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.7F);
                player.sendOverlayMessage(Component.translatable("message.mistech.altar.essence", altar.getEssence(), ImpureAltarBlockEntity.MAX_ESSENCE));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.GLASS_BOTTLE) && altar.getEssence() > 0 && !altar.isRunning()) {
            if (!level.isClientSide()) {
                altar.setEssence(altar.getEssence() - 1);
                giveBack(player, stack, new ItemStack(level.dimension() == Level.NETHER
                        ? com.mistech.mistechmod.beyond.PaleNetherContent.IGNEOUS_ESSENCE_BOTTLE.get() : Mistech.IMPURE_ESSENCE_BOTTLE.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
            return InteractionResult.SUCCESS;
        }
        if (altar.isRunning()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.already_running"));
            return InteractionResult.FAIL;
        }
        if (altar.getStored().isEmpty() && !stack.isEmpty()) {
            if (!level.isClientSide()) {
                altar.setStored(stack.split(1));
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ImpureAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (!altar.isRunning() && !altar.getStored().isEmpty()) {
                player.getInventory().placeItemBackInInventory(altar.getStored());
                altar.setStored(ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
            } else {
                long ward = altar.wardRemaining();
                player.sendOverlayMessage(ward > 0
                        ? Component.translatable("message.mistech.altar.status_ward", altar.getEssence(), ImpureAltarBlockEntity.MAX_ESSENCE, (ward + 1199) / 1200)
                        : Component.translatable("message.mistech.altar.essence", altar.getEssence(), ImpureAltarBlockEntity.MAX_ESSENCE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void giveBack(Player player, ItemStack used, ItemStack back) {
        if (player.getAbilities().instabuild) {
            return;
        }
        used.shrink(1);
        player.getInventory().placeItemBackInInventory(back);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ImpureAltarBlockEntity altar ? altar.getEssence() * 15 / ImpureAltarBlockEntity.MAX_ESSENCE : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FILLED) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.9,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.1, 0.0);
        }
        if (state.getValue(RUNNING)) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 1.0 + random.nextDouble(),
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.05, 0.0);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED, RUNNING);
    }
}
