package com.mistech.mistechmod.archive;

import java.util.function.Consumer;

import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Espelho do Arquivo: botão direito num Núcleo vincula; botão direito no ar abre o Arquivo de longe.
 * Até {@link #FREE_RANGE} blocos (mesma dimensão) é de graça; mais longe gasta {@link #FAR_COST} FE e, em outra dimensão,
 * {@link #DIMENSION_COST} FE por abertura (energia do próprio espelho: carrega na Bateria de Bolso).
 */
public class ArchiveMirrorItem extends Item implements MithrilGear.Gear {
    public static final double FREE_RANGE = 64.0;
    public static final int FAR_COST = 500;
    public static final int DIMENSION_COST = 1_000;

    public ArchiveMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public GearTier tier() {
        return GearTier.MITHRIL;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof ArchiveCoreBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && context.getPlayer() != null) {
            context.getItemInHand().set(ArchiveContent.MIRROR_CORE.get(), GlobalPos.of(server.dimension(), context.getClickedPos().immutable()));
            context.getPlayer().sendOverlayMessage(Component.translatable("message.mistech.archive_mirror.bound").withStyle(ChatFormatting.LIGHT_PURPLE));
            server.playSound(null, context.getClickedPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GlobalPos bound = stack.get(ArchiveContent.MIRROR_CORE.get());
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (bound == null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.archive_mirror.unbound").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        ServerLevel coreLevel = server.getServer().getLevel(bound.dimension());
        if (coreLevel == null) {
            return InteractionResult.FAIL;
        }
        coreLevel.getChunk(bound.pos());
        if (!(coreLevel.getBlockEntity(bound.pos()) instanceof ArchiveCoreBlockEntity)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.archive_mirror.gone").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        boolean otherDim = coreLevel != server;
        boolean far = otherDim || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bound.pos())) > FREE_RANGE * FREE_RANGE;
        int cost = otherDim ? DIMENSION_COST : far ? FAR_COST : 0;
        if (cost > 0 && !player.hasInfiniteMaterials()) {
            int energy = MithrilGear.getEnergy(stack);
            if (energy < cost) {
                player.sendOverlayMessage(Component.translatable("message.mistech.archive_mirror.no_energy", cost).withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            MithrilGear.setEnergy(stack, energy - cost);
        }
        GlobalPos target = bound;
        ArchiveTerminalBlock.open(player, coreLevel, bound.pos(), p -> holds(p, target));
        server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    private static boolean holds(Player player, GlobalPos target) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ArchiveMirrorItem && target.equals(stack.get(ArchiveContent.MIRROR_CORE.get()))) {
                return true;
            }
        }
        return false;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        GlobalPos bound = stack.get(ArchiveContent.MIRROR_CORE.get());
        builder.accept(bound == null ? Component.translatable("item.mistech.archive_mirror.unbound").withStyle(ChatFormatting.GRAY)
                : Component.translatable("item.mistech.archive_mirror.bound", bound.pos().toShortString(), bound.dimension().identifier().getPath())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        int energy = MithrilGear.getEnergy(stack);
        builder.accept(Component.translatable("item.mistech.mithril.energy", energy, MithrilGear.capacity(stack))
                .withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        builder.accept(Component.translatable("item.mistech.archive_mirror.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
