package com.mistech.mistechmod.boitata;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Apito do Boitatá: agachar + botão direito escolhe a ordem (Seguir, Ficar, Vagar, Chamar); botão direito apita e todos os seus
 * Boitatás em {@link #RANGE} blocos obedecem. "Chamar" traz para perto quem estiver longe (na mesma dimensão, carregado).
 */
public class BoitataWhistleItem extends Item {
    public static final int RANGE = 96;
    public static final int CALL_RANGE = 512;
    public static final int MODES = 4;
    public static final int MODE_CALL = 3;

    public BoitataWhistleItem(Item.Properties properties) {
        super(properties);
    }

    public static int mode(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("Mode", 0);
    }

    private static void setMode(ItemStack stack, int mode) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("Mode", mode);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            int mode = (mode(stack) + 1) % MODES;
            setMode(stack, mode);
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.mistech.boitata_whistle.selected",
                        Component.translatable("item.mistech.boitata_whistle.mode." + mode)).withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server) {
            int mode = mode(stack);
            int range = mode == MODE_CALL ? CALL_RANGE : RANGE;
            List<BoitataMountEntity> mounts = server.getEntitiesOfClass(BoitataMountEntity.class, new AABB(player.blockPosition()).inflate(range),
                    m -> m.isOwnedBy(player) && m.isAlive());
            for (BoitataMountEntity mount : mounts) {
                if (mode == MODE_CALL) {
                    mount.setCommand(BoitataMountEntity.COMMAND_FOLLOW);
                    if (mount.distanceToSqr(player) > 16 * 16 && !mount.isVehicle()) {
                        mount.teleportTo(player.getX() + (server.getRandom().nextDouble() - 0.5) * 4, player.getY(),
                                player.getZ() + (server.getRandom().nextDouble() - 0.5) * 4);
                    }
                } else {
                    mount.setCommand(mode);
                }
            }
            server.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.5F, 1.6F);
            server.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.2F, 2.0F);
            player.sendOverlayMessage(Component.translatable("item.mistech.boitata_whistle.used",
                    Component.translatable("item.mistech.boitata_whistle.mode." + mode), mounts.size()).withStyle(ChatFormatting.GOLD));
            player.getCooldowns().addCooldown(stack, 20);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.boitata_whistle.selected",
                Component.translatable("item.mistech.boitata_whistle.mode." + mode(stack))).withStyle(ChatFormatting.GOLD));
        builder.accept(Component.translatable("item.mistech.boitata_whistle.help").withStyle(ChatFormatting.GRAY));
    }
}
