package com.mistech.mistechmod.item.rune;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Runa de Energia: invoca um raio onde o jogador está mirando (até 64 blocos). 16 usos, recarga de 1 s. */
public class EnergyRuneItem extends Item {
    private static final double RANGE = 64.0;
    private static final int COOLDOWN_TICKS = 20;

    public EnergyRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(RANGE, 0.0F, false);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS; // mirando no céu: não gasta
        }

        if (level instanceof ServerLevel serverLevel) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(hit.getLocation());
                if (player instanceof ServerPlayer serverPlayer) {
                    bolt.setCause(serverPlayer);
                }
                serverLevel.addFreshEntity(bolt);
            }
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.ENERGY, 1);
        }
        return InteractionResult.SUCCESS;
    }
}
