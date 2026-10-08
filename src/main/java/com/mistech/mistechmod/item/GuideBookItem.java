package com.mistech.mistechmod.item;

import com.mistech.mistechmod.network.OpenGuidePayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Grimório Mistech: o livro de tutorial do mod. Botão direito abre o livro (a tela é só do cliente). */
public class GuideBookItem extends Item {
    public GuideBookItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, OpenGuidePayload.INSTANCE);
        }
        return InteractionResult.SUCCESS;
    }
}
