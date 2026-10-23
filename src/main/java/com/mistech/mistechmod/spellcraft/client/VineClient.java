package com.mistech.mistechmod.spellcraft.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.SpellFocusItem;
import com.mistech.mistechmod.spellcraft.Spells;
import com.mistech.mistechmod.spellcraft.VinePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Física do Cipó no cliente: uma corda presa no ponto. Enquanto o botão está pressionado a corda encolhe (puxa você);
 * se você está mais longe que o comprimento da corda, ela segura e vira pêndulo — dá para balançar. Shift encolhe mais
 * rápido. Perto do ponto, você fica pendurado em vez de quicar.
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class VineClient {
    private static boolean active;
    private static Vec3 anchor = Vec3.ZERO;
    private static int entityId = -1;
    private static int level;
    private static double length;
    private static int kind;

    private VineClient() {}

    @SubscribeEvent
    static void onPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(VinePayload.TYPE, (payload, context) -> {
            LocalPlayer self = Minecraft.getInstance().player;
            if (active && !payload.active() && payload.kind() == 2 && self != null) {
                // Teia Viva: ao soltar, o balanço ganha um impulso para frente e para cima
                Vec3 v = self.getDeltaMovement();
                self.setDeltaMovement(v.scale(1.35).add(0, 0.45, 0));
            }
            kind = payload.kind();
            active = payload.active();
            anchor = new Vec3(payload.x(), payload.y(), payload.z());
            entityId = payload.entityId();
            level = payload.level();
            LocalPlayer player = Minecraft.getInstance().player;
            length = player == null ? 0.0 : hand(player).distanceTo(currentAnchor(player));
        });
    }

    private static Vec3 hand(LocalPlayer player) {
        return player.getEyePosition().subtract(0, 0.35, 0);
    }

    private static Vec3 currentAnchor(LocalPlayer player) {
        if (entityId >= 0) {
            @Nullable Entity e = player.level().getEntity(entityId);
            if (e != null) {
                return e.position().add(0, e.getBbHeight() * 0.5, 0);
            }
        }
        return anchor;
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Pre event) {
        if (!active || !(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (!player.isUsingItem() || !com.mistech.mistechmod.spellcraft.VineGrapple.isVine(SpellFocusItem.selected(player.getUseItem()))) {
            active = false;
            return;
        }
        Vec3 target = currentAnchor(player);
        Vec3 delta = target.subtract(hand(player));
        double dist = delta.length();
        if (dist < 0.01) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / dist);
        // criatura leve presa: é ela que vem até você (o servidor puxa); você não se move
        Entity held = entityId >= 0 ? player.level().getEntity(entityId) : null;
        if (held instanceof net.minecraft.world.entity.LivingEntity living && !(held instanceof net.minecraft.world.entity.player.Player)
                && living.getMaxHealth() <= 40.0F && living.getBbWidth() < 1.8F) {
            return;
        }
        // a corda encolhe enquanto você segura (Shift: mais rápido), até 1,8 bloco
        double reel = (0.32 + 0.025 * level) * (player.isShiftKeyDown() ? 1.8 : 1.0) * (kind == 1 ? 1.6 : kind == 2 ? 1.3 : 1.0);
        length = Math.max(1.8, Math.min(length, dist) - reel);
        Vec3 v = player.getDeltaMovement();
        if (dist > length) {
            double away = -v.dot(dir); // velocidade se afastando do ponto
            if (away > 0) {
                v = v.add(dir.scale(away)); // a corda segura: tira o afastamento (vira balanço)
            }
            v = v.add(dir.scale(Math.min(0.45, (dist - length) * 0.35)));
        }
        if (dist <= 2.4) {
            // pendurado: segura contra a gravidade e amortece, sem quicar
            v = new Vec3(v.x * 0.8, Math.max(v.y * 0.6, 0.0) + 0.08 * Math.max(0.0, dir.y), v.z * 0.8);
        }
        double max = (1.6 + 0.08 * level) * (kind == 1 ? 1.25 : kind == 2 ? 1.2 : 1.0);
        if (v.length() > max) {
            v = v.normalize().scale(max);
        }
        player.setDeltaMovement(v);
        player.resetFallDistance();
    }
}
