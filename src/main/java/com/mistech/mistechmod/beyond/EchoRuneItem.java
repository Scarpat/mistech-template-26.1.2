package com.mistech.mistechmod.beyond;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Runa do Eco (ritual Tier I: Runa de Ar + 2 Fragmentos de Eco, Bloco de Notas, Runa da Escuridão). 8 usos.
 * Botão direito: solta o <b>grito do Warden</b> — {@link #RANGE} blocos em linha reta, {@link #DAMAGE} de dano que ignora armadura,
 * repulsão forte. Quem grita fica com Escuridão por 2 s, e o grito é uma vibração: perto de sculk, chama o Warden.
 */
public class EchoRuneItem extends Item {
    public static final double RANGE = 15.0;
    public static final float DAMAGE = 10.0F;
    public static final int COOLDOWN = 60;

    public EchoRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Sonic.boom(server, player, RANGE, DAMAGE, true);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false, true));
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.ECHO, 1);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }
}
