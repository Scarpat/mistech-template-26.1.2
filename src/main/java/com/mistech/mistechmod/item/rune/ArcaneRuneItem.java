package com.mistech.mistechmod.item.rune;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Runa de Energia Mágica (ritual Tier I: Runa de Energia + 2 Pó de Fada + 2 Fragmentos de Magia Impura). 16 usos.
 * - Botão direito no ar: descarrega {@link #ITEM_CHARGE} FE nos itens com energia do inventário
 *   (mão, outra mão, armadura e o resto: equipamento de mitril, Cajado do Gnomo...).
 * - Num bloco que guarda energia (máquina, bateria): coloca {@link #BLOCK_CHARGE} FE.
 */
public class ArcaneRuneItem extends Item {
    public static final int ITEM_CHARGE = 5_000;
    public static final int BLOCK_CHARGE = 10_000;

    public ArcaneRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        int moved = MithrilGear.chargeInventory(player, ITEM_CHARGE);
        if (moved <= 0) {
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_rune.nothing").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        fx(server, player.getX(), player.getY() + 1.0, player.getZ());
        player.sendOverlayMessage(Component.translatable("item.mistech.arcane_rune.charged", moved).withColor(RuneElement.ARCANE.color()));
        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        Affinity.grant(player, RuneElement.ARCANE, 1);
        player.getCooldowns().addCooldown(stack, 10);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, context.getClickedFace());
        if (handler == null) {
            handler = level.getCapability(Capabilities.Energy.BLOCK, pos, null);
        }
        if (handler == null) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        int moved;
        try (Transaction tx = Transaction.openRoot()) {
            moved = handler.insert(BLOCK_CHARGE, tx);
            tx.commit();
        }
        Player player = context.getPlayer();
        if (moved <= 0) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("item.mistech.arcane_rune.full").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.FAIL;
        }
        fx(server, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_rune.charged", moved).withColor(RuneElement.ARCANE.color()));
            context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            Affinity.grant(player, RuneElement.ARCANE, 1);
        }
        return InteractionResult.SUCCESS;
    }

    public static void fx(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 30, 0.4, 0.5, 0.4, 0.15);
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 30, 0.5, 0.6, 0.5, 0.6);
        level.playSound(null, x, y, z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
    }
}
