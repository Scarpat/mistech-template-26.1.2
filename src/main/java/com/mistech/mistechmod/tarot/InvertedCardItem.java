package com.mistech.mistechmod.tarot;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Carta Invertida: usar abre a Fenda Arcana à sua frente, que leva à luta contra o arcano no Plano Arcano. */
public class InvertedCardItem extends Item {
    private final TarotArcana arcana;

    public InvertedCardItem(Item.Properties properties, TarotArcana arcana) {
        super(properties);
        this.arcana = arcana;
    }

    public TarotArcana arcana() {
        return this.arcana;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (ArcanePlane.isPlane(level)) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.tarot.rift_in_plane"));
            return InteractionResult.FAIL;
        }
        if (server.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.tarot.peaceful"));
            return InteractionResult.FAIL;
        }
        if (ArcanePlane.isFighting(serverPlayer) || ArcanaRiftBlockEntity.hasOpenRift(serverPlayer)) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.tarot.rift_busy"));
            return InteractionResult.FAIL;
        }
        BlockPos pos = findRiftSpot(server, player);
        if (pos == null) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.tarot.rift_no_space"));
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getItemInHand(hand);
        ArcanaRiftBlock.open(server, pos, player.getDirection().getOpposite(), this.arcana, serverPlayer);
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(stack, 40);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** 2 ou 1 bloco à frente, no nível dos pés (ou um acima/abaixo), com 2 de altura livre. */
    private static BlockPos findRiftSpot(ServerLevel level, Player player) {
        Direction facing = player.getDirection();
        BlockPos feet = player.blockPosition();
        for (int distance = 2; distance >= 1; distance--) {
            for (int dy : new int[] {0, 1, -1}) {
                BlockPos pos = feet.relative(facing, distance).above(dy);
                if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.above()).canBeReplaced()
                        && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                        && pos.getY() + 1 < level.getMaxY()) {
                    return pos;
                }
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.literal(this.arcana.numeral()).withStyle(ChatFormatting.DARK_RED));
        builder.accept(Component.translatable("item.mistech.inverted_tarot_card.hint").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.mistech.inverted_tarot_card.warning").withStyle(ChatFormatting.DARK_GRAY,
                ChatFormatting.ITALIC));
    }
}
