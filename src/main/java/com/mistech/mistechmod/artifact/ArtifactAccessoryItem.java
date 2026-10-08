package com.mistech.mistechmod.artifact;

import java.util.function.Consumer;

import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.accessory.AccessoryItem;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Acessório-artefato (Ímã do Vazio, Ampulheta de Bolso, Anel Espacial): vai nos espaços de acessório (tecla J)
 * e tem energia própria (100.000 FE; carrega na Bateria de Bolso e nas baterias).
 * Ímã e Ampulheta: botão direito na mão liga/desliga. Ímã: agachar + botão direito com um item na outra mão
 * põe/tira esse item da lista de apagar (até {@link #FILTER_SIZE}).
 */
public class ArtifactAccessoryItem extends AccessoryItem implements MithrilGear.Gear {
    public static final int FILTER_SIZE = 9;

    public ArtifactAccessoryItem(Properties properties, Accessory accessory) {
        super(properties, accessory);
    }

    @Override
    public GearTier tier() {
        return GearTier.MITHRIL;
    }

    public static boolean active(ItemStack stack) {
        return stack.getOrDefault(ArtifactContent.ARTIFACT_ACTIVE.get(), true);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.accessory() == Accessory.SPATIAL_RING) {
            return InteractionResult.PASS;
        }
        if (this.accessory() == Accessory.VOID_MAGNET && player.isSecondaryUseActive()) {
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!level.isClientSide() && !other.isEmpty()) {
                toggleFilter(player, stack, other);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            boolean on = !active(stack);
            stack.set(ArtifactContent.ARTIFACT_ACTIVE.get(), on);
            player.sendOverlayMessage(Component.translatable(on ? "tooltip.mistech.artifact.on" : "tooltip.mistech.artifact.off")
                    .withStyle(on ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, on ? 1.4F : 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    public static NonNullList<ItemStack> filter(ItemStack magnet) {
        NonNullList<ItemStack> list = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        ItemContainerContents contents = magnet.get(ArtifactContent.VOID_FILTER.get());
        if (contents != null) {
            contents.copyInto(list);
        }
        return list;
    }

    public static boolean filters(ItemStack magnet, ItemStack item) {
        for (ItemStack f : filter(magnet)) {
            if (!f.isEmpty() && f.is(item.getItem())) {
                return true;
            }
        }
        return false;
    }

    private static void toggleFilter(Player player, ItemStack magnet, ItemStack item) {
        NonNullList<ItemStack> list = filter(magnet);
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isEmpty() && list.get(i).is(item.getItem())) {
                list.set(i, ItemStack.EMPTY);
                magnet.set(ArtifactContent.VOID_FILTER.get(), ItemContainerContents.fromItems(list));
                player.sendOverlayMessage(Component.translatable("item.mistech.void_magnet.removed", item.getHoverName()).withStyle(ChatFormatting.GRAY));
                return;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isEmpty()) {
                list.set(i, item.copyWithCount(1));
                magnet.set(ArtifactContent.VOID_FILTER.get(), ItemContainerContents.fromItems(list));
                player.sendOverlayMessage(Component.translatable("item.mistech.void_magnet.added", item.getHoverName()).withStyle(ChatFormatting.DARK_PURPLE));
                return;
            }
        }
        player.sendOverlayMessage(Component.translatable("item.mistech.void_magnet.full").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return MithrilGear.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB05CFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        int energy = MithrilGear.getEnergy(stack);
        tooltip.accept(Component.translatable("item.mistech.mithril.energy", energy, MithrilGear.capacity(stack))
                .withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        if (this.accessory() != Accessory.SPATIAL_RING) {
            tooltip.accept(Component.translatable(active(stack) ? "tooltip.mistech.artifact.on" : "tooltip.mistech.artifact.off")
                    .withStyle(active(stack) ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        }
        if (this.accessory() == Accessory.VOID_MAGNET) {
            for (ItemStack f : filter(stack)) {
                if (!f.isEmpty()) {
                    tooltip.accept(Component.literal(" ✕ ").append(f.getHoverName()).withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
        }
    }
}
