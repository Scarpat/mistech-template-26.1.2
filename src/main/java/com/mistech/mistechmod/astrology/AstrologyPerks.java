package com.mistech.mistechmod.astrology;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Aplica os bônus das estrelas desbloqueadas como modificadores de atributo (temporários: são recalculados
 * ao entrar no mundo, renascer, trocar de dimensão e ao desbloquear uma estrela).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class AstrologyPerks {
    private AstrologyPerks() {}

    public static Identifier modifierId(Constellations.Constellation constellation) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "astrology_" + constellation.id());
    }

    /** Quanto cada Pilar de Conexão Astral aumenta os bônus (12,5%). */
    public static final double PILLAR_BONUS = 0.125;

    /** Multiplicador dos bônus pela Conexão Astral (1,0 a 1,5). */
    public static double multiplier(int pillars) {
        return 1.0 + PILLAR_BONUS * Math.max(0, Math.min(4, pillars));
    }

    /** Valor total do bônus da constelação para o jogador (sem pilares). */
    public static double totalBonus(AstrologyData data, int constellation) {
        return data.countIn(constellation) * Constellations.get(constellation).perk().perStar();
    }

    /** Valor total do bônus com a Conexão Astral aplicada. */
    public static double totalBonus(AstrologyData data, int constellation, int pillars) {
        return totalBonus(data, constellation) * multiplier(pillars);
    }

    public static void apply(Player player) {
        AstrologyData data = player.getData(MistechAttachments.ASTROLOGY);
        for (int i = 0; i < Constellations.ALL.size(); i++) {
            Constellations.Constellation constellation = Constellations.get(i);
            AttributeInstance instance = player.getAttribute(constellation.perk().attribute());
            if (instance == null) {
                continue;
            }
            Identifier id = modifierId(constellation);
            instance.removeModifier(id);
            double amount = totalBonus(data, i, player.getData(MistechAttachments.ASTRAL_LINK));
            if (com.mistech.mistechmod.accessory.Accessories.boundConstellation(player) == i) {
                amount *= 2.0; // Amuleto de Constelação
            }
            if (amount != 0.0) {
                instance.addTransientModifier(new AttributeModifier(id, amount, constellation.perk().operation()));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        apply(event.getEntity());
    }
}
