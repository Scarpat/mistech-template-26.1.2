package com.mistech.mistechmod.spellcraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Cipó (magia de Vida): prende num bloco ou criatura, como a teia do Homem-Aranha. O servidor guarda o ponto, valida
 * (alcance, fio atravessando blocos), puxa criaturas leves e desenha o fio; o balanço e o puxão do jogador rodam no
 * cliente ({@code VineClient}), sem tremer. Ao soltar, você mantém a velocidade e fica uns 2 s sem dano de queda.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class VineGrapple {
    private static final double RANGE = 24.0;
    private static final int FALL_GRACE_TICKS = 40;

    /** Variantes: alcance e força da corda. */
    public enum Kind {
        VINE(24.0, 40), QUICK(14.0, 30), WEB(40.0, 80);

        public final double range;
        public final int grace;

        Kind(double range, int grace) {
            this.range = range;
            this.grace = grace;
        }
    }

    private record State(Vec3 anchor, @Nullable LivingEntity target, int level, Kind kind) {}

    /** O feitiço é um dos cipós? */
    public static boolean isVine(@Nullable Spell spell) {
        return spell != null && (spell == Spells.VINE || spell == Spells.QUICK_VINE || spell == Spells.LIVING_WEB);
    }

    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> GRACE = new HashMap<>();

    private VineGrapple() {}

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    /** Começa (ou renova) o cipó. Falso se não há onde prender. */
    static boolean start(ServerLevel level, ServerPlayer player) {
        return start(level, player, Kind.VINE);
    }

    static boolean start(ServerLevel level, ServerPlayer player, Kind kind) {
        if (isActive(player)) {
            return true;
        }
        Spells.Aim aim = Spells.aim(level, player, kind.range);
        if (aim == null || aim.distance() < 1.5) {
            return false;
        }
        int lifeLevel = Affinity.effectiveLevel(player, RuneElement.LIFE);
        ACTIVE.put(player.getUUID(), new State(aim.point(), aim.entity(), lifeLevel, kind));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new VinePayload(true, aim.point().x, aim.point().y, aim.point().z,
                aim.entity() == null ? -1 : aim.entity().getId(), lifeLevel, kind.ordinal()));
        if (kind == Kind.WEB) {
            // Teia Viva: cipós extras prendem as criaturas hostis em volta do ponto
            for (LivingEntity e : Spells.around(level, player, aim.point(), 5.0)) {
                if (e instanceof Player) {
                    continue;
                }
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 80, 5));
                com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.VINE, 0, null, 30.0F, aim.point(), e.position().add(0, e.getBbHeight() * 0.5, 0));
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.VINE_LATCH, null, 0.0F, aim.point());
        return true;
    }

    public static void release(ServerPlayer player, boolean stopUsing) {
        State removed = ACTIVE.remove(player.getUUID());
        if (removed != null) {
            GRACE.put(player.getUUID(), removed.kind().grace);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, VinePayload.off(removed.kind().ordinal()));
            player.level().playSound(null, player.blockPosition(), SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 0.8F, 0.9F);
            if (stopUsing && player.isUsingItem()) {
                player.stopUsingItem();
            }
        }
    }

    private static boolean holdingVine(ServerPlayer player) {
        if (!player.isUsingItem()) {
            return false;
        }
        return isVine(SpellFocusItem.selected(player.getUseItem()));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();
        State state = ACTIVE.get(id);
        if (state == null) {
            Integer grace = GRACE.get(id);
            if (grace != null) {
                player.resetFallDistance();
                if (grace <= 1) {
                    GRACE.remove(id);
                } else {
                    GRACE.put(id, grace - 1);
                }
            }
            return;
        }
        ServerLevel level = player.level();
        if (!player.isAlive() || !holdingVine(player)) {
            release(player, false);
            return;
        }
        LivingEntity target = state.target();
        if (target != null && (!target.isAlive() || target.level() != level)) {
            release(player, true);
            return;
        }
        Vec3 anchor = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : state.anchor();
        Vec3 hand = player.getEyePosition().subtract(0, 0.35, 0);
        Vec3 delta = anchor.subtract(hand);
        double dist = delta.length();
        if (dist > state.kind().range * 1.4 || dist < 0.01) {
            release(player, true);
            return;
        }
        Vec3 dir = delta.scale(1.0 / dist);
        // o fio não pode atravessar blocos
        if (dist > 2.0) {
            Vec3 end = anchor.subtract(dir.scale(0.6));
            var hit = level.clip(new ClipContext(hand, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(hand) < dist - 1.5) {
                release(player, true);
                return;
            }
        }
        boolean pullTarget = target != null && !(target instanceof Player) && target.getMaxHealth() <= 40.0F && target.getBbWidth() < 1.8F;
        double speed = 0.5 + 0.04 * state.level();
        if (pullTarget && dist > 2.5) {
            // criatura leve: vem até você (o movimento do jogador é feito no cliente, em VineClient)
            Vec3 toPlayer = dir.scale(-1);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.6).add(toPlayer.scale(speed * 0.6)));
            target.hurtMarked = true;
            target.resetFallDistance();
        }
        player.resetFallDistance();

        if (player.tickCount % 2 == 0) {
            drawVine(level, hand, anchor, dir, dist, player.tickCount);
        }
    }

    /** O fio: dois fios de folhas trançados, com um pouco de ondulação que diminui quando está esticado. */
    private static void drawVine(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir, double dist, int tick) {
        // o cipó inteiro é desenhado no cliente; dura 3 ticks e é mandado de novo a cada 2, então parece contínuo
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.VINE, tick, null, 3.0F, from, to);
    }
}
