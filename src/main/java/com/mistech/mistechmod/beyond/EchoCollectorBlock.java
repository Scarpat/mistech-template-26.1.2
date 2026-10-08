package com.mistech.mistechmod.beyond;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Coletor de Ecos: colocado <b>encostado num Catalisador de Sculk</b>, guarda a experiência dos mobs que morrem perto do
 * catalisador (até {@link EchoCollectorBlockEntity#MAX_XP} pontos) em vez de soltar as bolinhas.
 * <ul>
 *   <li>Mão vazia: pega toda a experiência guardada;</li>
 *   <li>Agachado com a mão vazia: guarda 1 nível seu (vira um banco de XP);</li>
 *   <li>Frasco de vidro: 10 pontos viram uma Garrafa de Encantamento (agachado: até 16).</li>
 * </ul>
 * Comparador = quanto está cheio.
 */
public class EchoCollectorBlock extends BaseEntityBlock {
    public static final MapCodec<EchoCollectorBlock> CODEC = simpleCodec(EchoCollectorBlock::new);
    public static final IntegerProperty FULLNESS = IntegerProperty.create("fullness", 0, 4);

    public EchoCollectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FULLNESS, 0));
    }

    @Override
    public MapCodec<EchoCollectorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EchoCollectorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof EchoCollectorBlockEntity collector) {
            int want = player.isShiftKeyDown() ? Math.min(16, stack.getCount()) : 1;
            int made = 0;
            while (made < want && collector.take(EchoCollectorBlockEntity.XP_PER_BOTTLE)) {
                made++;
            }
            if (made == 0) {
                player.sendOverlayMessage(Component.translatable("block.mistech.echo_collector.empty"));
                return InteractionResult.FAIL;
            }
            stack.consume(made, player);
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.EXPERIENCE_BOTTLE, made));
            server.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof EchoCollectorBlockEntity collector)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            // guarda 1 nível do jogador
            // primeiro o que já passou do nível atual; se estiver certinho no nível, 1 nível inteiro
            int points = (int) (player.experienceProgress * player.getXpNeededForNextLevel());
            if (points <= 0 && player.experienceLevel > 0) {
                points = xpForLevel(player.experienceLevel - 1);
            }
            points = Math.min(points, EchoCollectorBlockEntity.MAX_XP - collector.stored());
            if (points <= 0) {
                player.sendOverlayMessage(Component.translatable(collector.stored() >= EchoCollectorBlockEntity.MAX_XP
                        ? "block.mistech.echo_collector.full" : "block.mistech.echo_collector.no_xp"));
                return InteractionResult.FAIL;
            }
            player.giveExperiencePoints(-points);
            collector.add(points);
            server.playSound(null, pos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            int points = collector.stored();
            if (points <= 0) {
                player.sendOverlayMessage(Component.translatable("block.mistech.echo_collector.empty"));
                return InteractionResult.FAIL;
            }
            collector.take(points);
            player.giveExperiencePoints(points);
            server.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.03);
        }
        player.sendOverlayMessage(Component.translatable("block.mistech.echo_collector.stored", collector.stored(), EchoCollectorBlockEntity.MAX_XP)
                .withColor(0x29DFEB));
        return InteractionResult.SUCCESS;
    }

    /** Pontos de experiência para passar do nível {@code level} para o seguinte (fórmula vanilla). */
    static int xpForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FULLNESS) > 0 && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.03, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof EchoCollectorBlockEntity collector
                ? (int) Math.ceil(15.0 * collector.stored() / EchoCollectorBlockEntity.MAX_XP) : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FULLNESS);
    }
}
