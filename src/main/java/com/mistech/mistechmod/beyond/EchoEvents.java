package com.mistech.mistechmod.beyond;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Tudo o que mexe com vibrações do Deep Dark:
 * <ul>
 *   <li><b>Passo Silencioso</b> (Charuto do Eco) e <b>Runa do Eco engastada</b>: o jogador não gera vibrações;</li>
 *   <li><b>Selo do Silêncio</b>: dentro da zona, nada que um jogador faça vibra e o aviso dos shriekers zera;</li>
 *   <li><b>Ecolocalização</b> (Baseado do Eco) e <b>Ecólito</b>: criaturas que fazem barulho perto do jogador ganham contorno só para ele;</li>
 *   <li><b>Guardião</b> (Warden do Chamado do Guardião): não volta para a terra e dropa o Coração de Sculk;</li>
 *   <li><b>Coletor de Ecos</b>: guarda a experiência dos mobs que morrem perto de um Catalisador de Sculk vizinho.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class EchoEvents {
    public static final String GUARDIAN_TAG = "mistech_guardian";
    public static final double LISTEN_RANGE = 24.0;
    public static final int REVEAL_TICKS = 60;

    /** Jogadores com Runa do Eco engastada (recalculado a cada segundo). */
    private static final Set<UUID> SILENT_GEAR = new HashSet<>();
    /** Jogadores "ouvindo" (Ecolocalização ou Ecólito alimentado por perto) → gameTime até quando. */
    private static final Map<UUID, Long> LISTENERS = new HashMap<>();

    private EchoEvents() {}

    /** O Ecólito avisa que está ouvindo pelo dono (chamado a cada segundo). */
    public static void listenFor(Player player, long until) {
        LISTENERS.merge(player.getUUID(), until, Math::max);
    }

    public static boolean isSilent(Player player) {
        return player.hasEffect(MistechEffects.SILENT_STEP) || SILENT_GEAR.contains(player.getUUID());
    }

    private static Player playerSource(Entity cause) {
        if (cause instanceof Player player) {
            return player;
        }
        if (cause instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
            return owner;
        }
        if (cause != null && cause.getControllingPassenger() instanceof Player rider) {
            return rider;
        }
        return null;
    }

    @SubscribeEvent
    static void onGameEvent(VanillaGameEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity cause = event.getCause();
        Player source = playerSource(cause);
        Vec3 pos = event.getEventPosition();

        // Selo do Silêncio: abafa tudo o que os jogadores (ou ninguém) fazem dentro da zona
        if ((source != null || cause == null) && SilenceData.get(level).isSilenced(level.getGameTime(), pos)) {
            event.setCanceled(true);
            return;
        }
        // Passo Silencioso / Runa do Eco engastada
        if (source != null && isSilent(source)) {
            event.setCanceled(true);
            return;
        }
        // Ecolocalização / Ecólito: quem faz barulho aparece
        if (!LISTENERS.isEmpty() && cause instanceof LivingEntity living && !(cause instanceof Player)) {
            long now = level.getGameTime();
            for (ServerPlayer player : level.players()) {
                Long until = LISTENERS.get(player.getUUID());
                if (until == null || until < now) {
                    continue;
                }
                if (player.position().distanceToSqr(pos) <= LISTEN_RANGE * LISTEN_RANGE) {
                    EchoSense.reveal(player, living, REVEAL_TICKS);
                }
            }
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 11) {
            return;
        }
        if (GearRunes.count(GearRunes.tallyEquipped(player), RuneElement.ECHO) > 0.0) {
            SILENT_GEAR.add(player.getUUID());
        } else {
            SILENT_GEAR.remove(player.getUUID());
        }
        if (player.hasEffect(MistechEffects.ECHOLOCATION)) {
            listenFor(player, level.getGameTime() + 30);
        }
        // dentro do silêncio, os avisos dos shriekers não acumulam
        if (SilenceData.get(level).isSilenced(level.getGameTime(), player.position())) {
            player.getWardenSpawnTracker().ifPresent(tracker -> {
                if (tracker.getWarningLevel() > 0) {
                    tracker.setWarningLevel(0);
                }
            });
        }
        LISTENERS.values().removeIf(until -> until < level.getGameTime() - 100);
    }

    // ------------------------------------------------------------------ Guardião (Warden do ritual)

    public static boolean isGuardian(Entity entity) {
        return entity instanceof Warden && entity.getPersistentData().getBooleanOr(GUARDIAN_TAG, false);
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % 100 == 0 && entity instanceof Warden warden && !warden.level().isClientSide() && isGuardian(warden)) {
            // nunca fica "entediado" o bastante para cavar de volta
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        }
    }

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isGuardian(entity) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY() + 1.0, entity.getZ(), new ItemStack(BeyondContent.SCULK_HEART.get())));
        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY() + 1.0, entity.getZ(),
                new ItemStack(Items.ECHO_SHARD, 2 + level.getRandom().nextInt(3))));
    }

    // ------------------------------------------------------------------ Coletor de Ecos

    @SubscribeEvent
    static void onExperienceDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        int xp = event.getDroppedExperience();
        if (xp <= 0 || !(entity.level() instanceof ServerLevel level) || entity instanceof Player) {
            return;
        }
        if (EchoCollectorBlockEntity.capture(level, entity.blockPosition(), xp)) {
            event.setDroppedExperience(0);
        }
    }
}
