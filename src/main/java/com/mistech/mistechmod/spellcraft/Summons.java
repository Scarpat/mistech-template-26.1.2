package com.mistech.mistechmod.spellcraft;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Invocações (só têm duração, sem mana reservada) e utilidades com estado: Âncora e Rebobinar.
 * Limite de invocações simultâneas = 1 + nível/3 (nível mais alto nos componentes do elemento).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class Summons {
    public static final String TAG = "mistech_summon";

    /** {@code group}: invocações lançadas juntas (os 3 Guardiões da Floresta Viva) contam como uma vaga só. */
    private record Entry(UUID owner, long expire, UUID group) {}

    private static final Map<UUID, Entry> LIVE = new HashMap<>();
    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();
    private static final Map<UUID, ArrayDeque<Snapshot>> HISTORY = new HashMap<>();

    private record Anchor(ResourceKey<Level> dim, Vec3 pos) {}

    private record Snapshot(ResourceKey<Level> dim, Vec3 pos, float health) {}

    private Summons() {}

    // ------------------------------------------------------------------ invocações

    static int limit(ServerPlayer player, RuneElement element) {
        return 1 + Affinity.effectiveLevel(player, element) / 3;
    }

    static int count(UUID owner) {
        java.util.Set<UUID> groups = new java.util.HashSet<>();
        for (Entry e : LIVE.values()) {
            if (e.owner().equals(owner)) {
                groups.add(e.group());
            }
        }
        return groups.size();
    }

    /** Há vaga para mais {@code amount} invocações? Se não, avisa o jogador. */
    static boolean canSummon(ServerPlayer player, RuneElement element) {
        int limit = limit(player, element);
        if (count(player.getUUID()) + 1 > limit) {
            player.sendOverlayMessage(Component.translatable("message.mistech.summon.limit", limit)
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static void register(ServerLevel level, ServerPlayer owner, Mob mob, int durationTicks, Vec3 at, RuneElement element, UUID group) {
        mob.setPos(at);
        mob.addTag(TAG);
        mob.setPersistenceRequired();
        mob.setGlowingTag(true);
        LIVE.put(mob.getUUID(), new Entry(owner.getUUID(), level.getGameTime() + durationTicks, group));
        level.addFreshEntity(mob);
        SpellFx.burst(level, at.add(0, 0.8, 0), element, 0.25, 30, ParticleTypes.POOF);
        SpellFx.risingRing(level, at, element, 1.0, 8, 2.0);
        com.mistech.mistechmod.spellcraft.fx.Vfx.circle(level, at, element, 1.6, 24);
        com.mistech.mistechmod.spellcraft.fx.Vfx.beam(level, at, at.add(0, 4, 0), com.mistech.mistechmod.spellcraft.fx.Vfx.glowMaterial(element), 0.9F, 10);
        com.mistech.mistechmod.spellcraft.fx.Vfx.shockwave(level, at, element, 3.0, 10);
    }

    private static Vec3 spot(ServerLevel level, ServerPlayer player, int index, int total) {
        double angle = player.getYRot() * Math.PI / 180.0 + Math.PI * 2.0 * index / Math.max(1, total);
        Vec3 pos = player.position().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
        BlockPos bp = BlockPos.containing(pos);
        for (int dy = 2; dy >= -2; dy--) {
            if (level.getBlockState(bp.offset(0, dy, 0)).isAir() && level.getBlockState(bp.offset(0, dy + 1, 0)).isAir()
                    && !level.getBlockState(bp.offset(0, dy - 1, 0)).isAir()) {
                return new Vec3(pos.x, bp.getY() + dy, pos.z);
            }
        }
        return player.position();
    }

    /** Golem de Pedra: protege você por 60 s. */
    static boolean stoneGolem(ServerLevel level, ServerPlayer player) {
        if (!canSummon(player, RuneElement.EARTH)) {
            return false;
        }
        IronGolem golem = SpellcraftContent.STONE_GOLEM.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            return false;
        }
        golem.setCustomName(Component.translatable("entity.mistech.summon.stone_golem").withColor(RuneElement.EARTH.color()));
        double power = Spells.pw(player, RuneElement.EARTH);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0 * power);
        golem.setHealth((float) (60.0 * power));
        register(level, player, golem, (int) (1200 * power), spot(level, player, 0, 1), RuneElement.EARTH, UUID.randomUUID());
        level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** Floresta Viva: três guardiões de madeira, mais frágeis, por 45 s. */
    static boolean livingForest(ServerLevel level, ServerPlayer player) {
        if (!canSummon(player, RuneElement.LIFE)) {
            return false;
        }
        double power = Spells.pw(player, RuneElement.LIFE);
        UUID group = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            IronGolem golem = SpellcraftContent.WOOD_GUARDIAN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (golem == null) {
                continue;
            }
            golem.setCustomName(Component.translatable("entity.mistech.summon.wood_guardian").withColor(RuneElement.LIFE.color()));
            golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0 * power);
            golem.setHealth((float) (30.0 * power));
            register(level, player, golem, (int) (900 * power), spot(level, player, i, 3), RuneElement.LIFE, group);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
        return true;
    }

    /** Lobo Sombrio: lobo domado que luta ao seu lado por 40 s. */
    static boolean shadowWolf(ServerLevel level, ServerPlayer player) {
        if (!canSummon(player, RuneElement.DARKNESS)) {
            return false;
        }
        Wolf wolf = SpellcraftContent.SHADOW_WOLF.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (wolf == null) {
            return false;
        }
        wolf.tame(player);
        wolf.setCustomName(Component.translatable("entity.mistech.summon.shadow_wolf").withColor(RuneElement.DARKNESS.color()));
        double power = Spells.pw(player, RuneElement.DARKNESS);
        wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0 * power);
        wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0 * power);
        wolf.setHealth((float) (30.0 * power));
        register(level, player, wolf, (int) (800 * power), spot(level, player, 0, 1), RuneElement.DARKNESS, UUID.randomUUID());
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.5F);
        return true;
    }

    /** Invocação morta libera a vaga na hora (antes ficava ocupada até o tempo acabar). */
    /** Espírito Guardião (Alma): dois espíritos voadores defendem você por 45 s. */
    static boolean spiritGuardian(ServerLevel level, ServerPlayer player) {
        if (!canSummon(player, RuneElement.SOUL)) {
            return false;
        }
        double power = Spells.pw(player, RuneElement.SOUL);
        UUID group = UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            SummonFlyerEntity spirit = SpellcraftContent.SPIRIT_GUARDIAN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (spirit == null) {
                continue;
            }
            spirit.setMaster(player);
            spirit.setCustomName(Component.translatable("entity.mistech.summon.spirit_guardian").withColor(RuneElement.SOUL.color()));
            spirit.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0 * power);
            spirit.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0 * power);
            spirit.setHealth((float) (20.0 * power));
            register(level, player, spirit, (int) (900 * power), player.position().add(i == 0 ? -1.5 : 1.5, 2.0, 0), RuneElement.SOUL, group);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5F, 1.0F);
        return true;
    }

    /** Fênix (Fogo): uma ave de fogo luta por você por 60 s, incendiando o que acerta. */
    static boolean phoenix(ServerLevel level, ServerPlayer player) {
        if (!canSummon(player, RuneElement.FIRE)) {
            return false;
        }
        SummonFlyerEntity bird = SpellcraftContent.PHOENIX.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (bird == null) {
            return false;
        }
        double power = Spells.pw(player, RuneElement.FIRE);
        bird.setMaster(player);
        bird.setCustomName(Component.translatable("entity.mistech.summon.phoenix").withColor(RuneElement.FIRE.color()));
        bird.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50.0 * power);
        bird.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(9.0 * power);
        bird.setHealth((float) (50.0 * power));
        register(level, player, bird, (int) (1200 * power), player.position().add(0, 2.5, 0), RuneElement.FIRE, UUID.randomUUID());
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.7F);
        return true;
    }

    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            LIVE.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.entityTags().contains(TAG)) {
            return;
        }
        Entry entry = LIVE.get(entity.getUUID());
        if (entry == null || entry.expire() <= event.getLevel().getGameTime()) {
            // sobrou de uma sessão anterior: some
            LIVE.remove(entity.getUUID());
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ Âncora e Rebobinar

    /** Primeira vez: marca o lugar. Segunda: volta para lá. */
    static boolean anchor(ServerLevel level, ServerPlayer player) {
        Anchor anchor = ANCHORS.remove(player.getUUID());
        if (anchor == null || !anchor.dim().equals(level.dimension())) {
            ANCHORS.put(player.getUUID(), new Anchor(level.dimension(), player.position()));
            SpellFx.sigil(level, player.position(), RuneElement.SPACE, 1.2);
            player.sendOverlayMessage(Component.translatable("message.mistech.anchor.set").withColor(RuneElement.SPACE.color()));
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
            return true;
        }
        SpellFx.burst(level, player.position().add(0, 1.0, 0), RuneElement.SPACE, 0.3, 40, ParticleTypes.PORTAL);
        player.teleportTo(anchor.pos().x, anchor.pos().y, anchor.pos().z);
        player.resetFallDistance();
        SpellFx.burst(level, anchor.pos().add(0, 1.0, 0), RuneElement.SPACE, 0.3, 40, ParticleTypes.PORTAL);
        player.sendOverlayMessage(Component.translatable("message.mistech.anchor.back").withColor(RuneElement.SPACE.color()));
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Volta para onde (e com a vida que) você estava 5 s atrás. */
    static boolean rewind(ServerLevel level, ServerPlayer player) {
        ArrayDeque<Snapshot> history = HISTORY.get(player.getUUID());
        Snapshot past = history == null ? null : history.peekFirst();
        if (past == null || !past.dim().equals(level.dimension())) {
            player.sendOverlayMessage(Component.translatable("message.mistech.rewind.nothing").withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        SpellFx.contractingRing(level, player.position(), RuneElement.TIME, 3.0, 8, ParticleTypes.REVERSE_PORTAL);
        player.teleportTo(past.pos().x, past.pos().y, past.pos().z);
        player.resetFallDistance();
        player.setHealth(Math.max(player.getHealth(), Math.min(player.getMaxHealth(), past.health())));
        player.clearFire();
        SpellFx.burst(level, past.pos().add(0, 1.0, 0), RuneElement.TIME, 0.3, 40, ParticleTypes.REVERSE_PORTAL);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        history.clear();
        return true;
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();
        if (tick % 10 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ArrayDeque<Snapshot> history = HISTORY.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
                history.addLast(new Snapshot(player.level().dimension(), player.position(), player.getHealth()));
                while (history.size() > 10) {
                    history.removeFirst();
                }
            }
        }
        if (tick % 20 != 0 || LIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Entry>> it = LIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Entry> e = it.next();
            Entity entity = null;
            for (ServerLevel level : server.getAllLevels()) {
                entity = level.getEntity(e.getKey());
                if (entity != null) {
                    break;
                }
            }
            if (entity == null || !entity.isAlive()) {
                if (entity != null || server.overworld().getGameTime() > e.getValue().expire()) {
                    it.remove();
                }
                continue;
            }
            if (entity.level().getGameTime() >= e.getValue().expire() && entity.level() instanceof ServerLevel level) {
                SpellFx.burst(level, entity.position().add(0, entity.getBbHeight() * 0.5, 0), RuneElement.ARCANE, 0.2, 25, ParticleTypes.POOF);
                entity.discard();
                it.remove();
            }
        }
    }
}
