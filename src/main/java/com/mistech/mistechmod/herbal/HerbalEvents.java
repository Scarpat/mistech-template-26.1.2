package com.mistech.mistechmod.herbal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Efeitos das ervas:
 * <ul>
 * <li><b>Cafeinado</b> (café): zera o cansaço (Phantoms não vêm), não deixa dormir e dá +10% de experiência.</li>
 * <li><b>Visão Mística / Psicodélica</b> (beladona / cogumelo): criaturas invisíveis e as do folclore que se escondem
 * (tag {@code mistech:hidden_creatures}) aparecem com contorno a até {@link #SIGHT_RANGE} blocos — só para quem bebeu.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class HerbalEvents {
    public static final TagKey<EntityType<?>> HIDDEN = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Mistech.MODID, "hidden_creatures"));
    public static final double SIGHT_RANGE = 24.0;
    public static final float CAFFEINE_XP_BONUS = 0.10F;

    /** Entidades que cada jogador está vendo "reveladas" (para devolver as flags verdadeiras depois). */
    private static final Map<UUID, Set<Integer>> REVEALED = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Byte> FLAGS = sharedFlags();

    private HerbalEvents() {}

    private static EntityDataAccessor<Byte> sharedFlags() {
        try {
            var field = Entity.class.getDeclaredField("DATA_SHARED_FLAGS_ID");
            field.setAccessible(true);
            return (EntityDataAccessor<Byte>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean caffeinated(Player player) {
        return player.hasEffect(MistechEffects.CAFFEINATED);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0 && caffeinated(player)) {
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        }
        if (player.tickCount % 10 == 0) {
            updateSight(player);
        }
    }

    private static void updateSight(ServerPlayer player) {
        boolean sees = player.hasEffect(MistechEffects.MYSTIC_SIGHT) || player.hasEffect(MistechEffects.PSYCHEDELIC_SIGHT);
        Set<Integer> before = REVEALED.getOrDefault(player.getUUID(), Set.of());
        if (!sees && before.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        Set<Integer> now = new HashSet<>();
        if (sees) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(SIGHT_RANGE),
                    e -> e != player && e.isAlive() && (e.isInvisible() || e.is(HIDDEN)))) {
                now.add(e.getId());
                byte flags = e.getEntityData().get(FLAGS);
                byte shown = (byte) ((flags | (1 << 6)) & ~(1 << 5)); // brilhando, visível
                player.connection.send(new ClientboundSetEntityDataPacket(e.getId(), List.of(SynchedEntityData.DataValue.create(FLAGS, shown))));
            }
        }
        for (int id : before) {
            if (!now.contains(id)) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    player.connection.send(new ClientboundSetEntityDataPacket(id,
                            List.of(SynchedEntityData.DataValue.create(FLAGS, e.getEntityData().get(FLAGS)))));
                }
            }
        }
        if (now.isEmpty()) {
            REVEALED.remove(player.getUUID());
        } else {
            REVEALED.put(player.getUUID(), now);
        }
    }

    @SubscribeEvent
    static void onCanSleep(CanPlayerSleepEvent event) {
        if (event.getProblem() == null && caffeinated(event.getEntity())) {
            event.setProblem(new Player.BedSleepingProblem(Component.translatable("message.mistech.caffeinated.no_sleep")));
        }
    }

    @SubscribeEvent
    static void onXp(PlayerXpEvent.XpChange event) {
        int amount = event.getAmount();
        if (amount <= 0 || !caffeinated(event.getEntity())) {
            return;
        }
        float extra = amount * CAFFEINE_XP_BONUS;
        int whole = (int) extra;
        if (event.getEntity().getRandom().nextFloat() < extra - whole) {
            whole++;
        }
        event.setAmount(amount + whole);
    }
}
