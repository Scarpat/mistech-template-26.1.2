package com.mistech.mistechmod.spatial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Orbe de Translocação (ritual Tier II): uma Âncora Espacial de bolso. Botão direito abre a lista das âncoras que você
 * conhece, de qualquer lugar. Cada viagem gasta 1 uso e põe o orbe em recarga; o custo em níveis é o mesmo da âncora.
 */
public class TranslocationOrbItem extends Item {
    public static final int USES = 48;
    public static final int COOLDOWN_TICKS = 20 * 15;

    public TranslocationOrbItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            SpatialService.openList(serverPlayer, "");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
