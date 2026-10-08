package com.mistech.mistechmod.seeking;

import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.folklore.WerewolfEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Pêndulo Localizador (Pêndulo de Ametista + bússola + Pó de Fada). {@link #USES} buscas.
 * <ul>
 * <li>Agachar + botão direito com um item-chave na outra mão escolhe o alvo ({@link SeekKind}) e procura.</li>
 * <li>Botão direito: procura de novo (a partir de onde você está).</li>
 * <li>Segurando: a HUD mostra a direção e a distância. Lobisomem: o alvo se atualiza sozinho a cada 2 s.</li>
 * </ul>
 */
public class SeekingPendulumItem extends Item {
    public static final int USES = 128;
    /** Raio da busca de estruturas, em chunks. */
    public static final int STRUCTURE_RADIUS = 100;
    /** Raio da busca de minério, em blocos. */
    public static final int ORE_RADIUS = 32;
    public static final double CREATURE_RADIUS = 128.0;

    public SeekingPendulumItem(Properties properties) {
        super(properties);
    }

    public static @Nullable SeekTarget target(ItemStack stack) {
        return stack.get(SeekingContent.TARGET.get());
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().getItem() instanceof SeekingPendulumItem || player.getOffhandItem().getItem() instanceof SeekingPendulumItem;
    }

    public static ItemStack held(Player player) {
        return player.getMainHandItem().getItem() instanceof SeekingPendulumItem ? player.getMainHandItem() : player.getOffhandItem();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        SeekKind kind;
        if (player.isShiftKeyDown()) {
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            kind = SeekKind.byKey(other);
            if (kind == null) {
                player.sendOverlayMessage(Component.translatable("item.mistech.seeking_pendulum.how").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
        } else {
            SeekTarget current = target(stack);
            kind = current == null ? null : SeekKind.byId(current.kind());
            if (kind == null) {
                player.sendOverlayMessage(Component.translatable("item.mistech.seeking_pendulum.how").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
        }
        BlockPos found = search(server, player, kind);
        stack.set(SeekingContent.TARGET.get(), new SeekTarget(kind.id, Optional.ofNullable(found).map(p -> GlobalPos.of(server.dimension(), p))));
        Component name = Component.translatable(kind.translationKey());
        if (found == null) {
            player.sendOverlayMessage(Component.translatable("item.mistech.seeking_pendulum.nothing", name).withStyle(ChatFormatting.GRAY));
            server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.PLAYERS, 0.6F, 0.6F);
        } else {
            int dist = (int) Math.sqrt(found.distSqr(player.blockPosition()));
            player.sendOverlayMessage(Component.translatable("item.mistech.seeking_pendulum.found", name, dist).withColor(kind.color));
            server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        player.getCooldowns().addCooldown(stack, 40);
        return InteractionResult.SUCCESS;
    }

    /** Procura o alvo a partir do jogador (null = nada por perto). */
    public static @Nullable BlockPos search(ServerLevel level, Player player, SeekKind kind) {
        return switch (kind.type) {
            case STRUCTURE -> level.findNearestMapStructure(kind.structureTag(), player.blockPosition(), STRUCTURE_RADIUS, false);
            case ORE -> nearestBlock(level, player.blockPosition(), kind.oreTag(), ORE_RADIUS);
            case CREATURE -> {
                WerewolfEntity best = null;
                for (WerewolfEntity e : level.getEntitiesOfClass(WerewolfEntity.class, player.getBoundingBox().inflate(CREATURE_RADIUS), Entity::isAlive)) {
                    if (best == null || e.distanceToSqr(player) < best.distanceToSqr(player)) {
                        best = e;
                    }
                }
                yield best == null ? null : best.blockPosition();
            }
        };
    }

    /** Bloco mais perto da tag num raio (pula as seções que não têm o bloco). */
    private static @Nullable BlockPos nearestBlock(ServerLevel level, BlockPos center, TagKey<Block> tag, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int minY = Math.max(level.getMinY(), center.getY() - radius);
        int maxY = Math.min(level.getMaxY(), center.getY() + radius);
        ChunkPos cc = ChunkPos.containing(center);
        int cr = (radius >> 4) + 1;
        for (int cx = cc.x() - cr; cx <= cc.x() + cr; cx++) {
            for (int cz = cc.z() - cr; cz <= cc.z() + cr; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int sy = level.getSectionIndex(minY); sy <= level.getSectionIndex(maxY); sy++) {
                    if (sy < 0 || sy >= chunk.getSections().length) {
                        continue;
                    }
                    LevelChunkSection section = chunk.getSections()[sy];
                    if (section.hasOnlyAir() || !section.maybeHas(s -> s.is(tag))) {
                        continue;
                    }
                    int baseY = level.getSectionYFromSectionIndex(sy) << 4;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState state = section.getBlockState(x, y, z);
                                if (state.is(tag)) {
                                    BlockPos pos = new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z);
                                    double d = pos.distSqr(center);
                                    if (d < bestDist && d <= (double) radius * radius) {
                                        bestDist = d;
                                        best = pos;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Lobisomem: o alvo segue a criatura enquanto o pêndulo está na mão. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (slot == null || !(entity instanceof Player player) || level.getGameTime() % 40 != 0) {
            return;
        }
        SeekTarget target = target(stack);
        if (target == null || SeekKind.byId(target.kind()) != SeekKind.WEREWOLF) {
            return;
        }
        BlockPos found = search(level, player, SeekKind.WEREWOLF);
        Optional<GlobalPos> pos = Optional.ofNullable(found).map(p -> GlobalPos.of(level.dimension(), p));
        if (!pos.equals(target.pos())) {
            stack.set(SeekingContent.TARGET.get(), new SeekTarget(target.kind(), pos));
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        SeekTarget target = target(stack);
        SeekKind kind = target == null ? null : SeekKind.byId(target.kind());
        if (kind != null) {
            builder.accept(Component.translatable("item.mistech.seeking_pendulum.target", Component.translatable(kind.translationKey())).withColor(kind.color));
        }
        builder.accept(Component.translatable("item.mistech.seeking_pendulum.keys").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.mistech.seeking_pendulum.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
