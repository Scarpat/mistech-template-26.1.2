package com.mistech.mistechmod.artifact;

import java.util.function.Consumer;

import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;
import com.mistech.mistechmod.item.mithril.MithrilItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Vassoura das Nuvens (Oricalco, 6 engastes; Nuvens + Ar). Botão direito: solta a vassoura ({@link BroomEntity}) e o jogador monta.
 * A vassoura guarda o item (energia e runas) e devolve ao ser recolhida (agachar + botão direito nela).
 */
public class CloudBroomItem extends MithrilItem {
    public CloudBroomItem(GearTier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!MithrilGear.isCharged(stack) && !player.hasInfiniteMaterials()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.mistech.cloud_broom.empty").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            BroomEntity broom = ArtifactContent.BROOM_ENTITY.get().create(server, EntitySpawnReason.SPAWN_ITEM_USE);
            if (broom == null) {
                return InteractionResult.FAIL;
            }
            broom.snapTo(player.getX(), player.getY() + 0.1, player.getZ(), player.getYRot(), 0.0F);
            broom.setItem(stack.copyWithCount(1));
            server.addFreshEntity(broom);
            player.startRiding(broom);
            server.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.cloud_broom.desc").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
