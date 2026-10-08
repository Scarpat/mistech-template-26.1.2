package com.mistech.mistechmod.tarot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Plano Arcano: dimensão vazia onde acontecem as lutas contra os arcanos. Cada luta ganha uma arena própria
 * (um "espaço" a cada {@link #SLOT_SPACING} blocos no eixo X), então vários jogadores podem lutar ao mesmo tempo.
 * <ul>
 * <li><b>Entrar</b>: pela Fenda Arcana. Qualquer um pode entrar enquanto ela estiver aberta: quem entra pela mesma fenda
 * cai na mesma arena (grupo). A posição de volta fica nos dados de cada jogador ({@link #RETURN_TAG}).</li>
 * <li><b>Grupo</b>: o chefe ganha +{@link #GROUP_HEALTH_BONUS} da vida base por jogador extra (recalculado quando alguém
 * entra ou sai).</li>
 * <li><b>Luta</b>: contagem de {@link #COUNTDOWN} ticks, o chefe ({@link ArcanaBosses}) surge no centro; barra de vida
 * na cor do arcano; o chefe que sai da arena volta para o centro.</li>
 * <li><b>Vitória</b>: cai UMA Carta do Arcano (para quem abriu a fenda; ver {@link #victory}); todos que estão na arena
 * ficam com o arcano derrotado ({@link TarotData}); depois de {@link #VICTORY_DELAY} ticks todos voltam.</li>
 * <li><b>Derrota</b>: morrer lá não mata — volta com todos os itens; o grupo continua sem ele. Se todos caírem, a luta
 * acaba e a Carta Invertida foi gasta.</li>
 * <li>Sair do jogo no meio da luta tira o jogador da luta; ao entrar de novo, volta para casa.</li>
 * <li>No plano não dá para quebrar/colocar blocos (fora do criativo) nem explodir a arena.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ArcanePlane {
    public static final String RETURN_TAG = "mistech_arcane_return";
    /** Etiqueta dos chefes (para limpar sobras depois de reiniciar o servidor). */
    public static final String BOSS_TAG = "mistech_arcana_boss";

    public static final int ARENA_Y = 100;
    public static final int ARENA_RADIUS = 20;
    public static final int SLOT_SPACING = 1024;
    public static final int COUNTDOWN = 60;
    public static final int VICTORY_DELAY = 100;

    public enum Stage { COUNTDOWN, FIGHT, VICTORY }

    /** Vida extra do chefe por jogador além do primeiro (+50% da vida base cada). */
    public static final double GROUP_HEALTH_BONUS = 0.5;
    private static final Identifier GROUP_HEALTH_ID = Identifier.fromNamespaceAndPath(Mistech.MODID, "arcana_group");

    /** Uma luta em andamento: um arcano, uma arena e o grupo que entrou pela mesma fenda. */
    public static final class Fight {
        private final UUID opener;
        private final GlobalPos rift;
        private final TarotArcana arcana;
        private final int slot;
        private final BlockPos center;
        private final ServerBossEvent bar;
        private final ArenaLayout layout;
        private final Set<UUID> players = new LinkedHashSet<>();
        /** Quem caiu ou saiu: não pode voltar para esta luta pela fenda. */
        private final Set<UUID> fallen = new HashSet<>();
        private @Nullable UUID boss;
        private Stage stage = Stage.COUNTDOWN;
        private int ticks;
        private int respawns;
        private int arrivals;

        Fight(UUID opener, GlobalPos rift, TarotArcana arcana, int slot) {
            this.opener = opener;
            this.rift = rift;
            this.arcana = arcana;
            this.slot = slot;
            this.center = arenaCenter(slot);
            this.layout = ArcanaBosses.arena(arcana);
            this.bar = new ServerBossEvent(UUID.randomUUID(), arcana.fullName().copy().withStyle(ChatFormatting.LIGHT_PURPLE),
                    barColor(arcana), BossEvent.BossBarOverlay.NOTCHED_10);
            this.bar.setProgress(0.0F);
        }

        /** Quem abriu a fenda (fica com a carta, se ainda estiver na arena no fim). */
        public UUID opener() {
            return this.opener;
        }

        /** Jogadores na arena agora. */
        public Set<UUID> players() {
            return Collections.unmodifiableSet(this.players);
        }

        public TarotArcana arcana() {
            return this.arcana;
        }

        public BlockPos center() {
            return this.center;
        }

        public ArenaLayout layout() {
            return this.layout;
        }

        public Stage stage() {
            return this.stage;
        }

        /** Ticks desde o começo da etapa atual. */
        public int ticks() {
            return this.ticks;
        }

        public @Nullable UUID boss() {
            return this.boss;
        }

        List<ServerPlayer> online(MinecraftServer server) {
            List<ServerPlayer> list = new ArrayList<>();
            for (UUID id : this.players) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    list.add(player);
                }
            }
            return list;
        }
    }

    private static final List<Fight> FIGHTS = new ArrayList<>();
    /** Quem entrou numa fenda neste tick (o teleporte acontece no fim do tick do servidor, fora do movimento do jogador). */
    private record PendingEntry(UUID player, TarotArcana arcana, GlobalPos rift, UUID opener) {}
    private static final List<PendingEntry> PENDING = new ArrayList<>();
    /** Quantas vezes o chefe pode sumir (sem morrer) e ser chamado de novo antes de a luta ser cancelada. */
    private static final int MAX_RESPAWNS = 3;

    private ArcanePlane() {}

    public static boolean isPlane(Level level) {
        return level.dimension() == TarotContent.ARCANE_PLANE;
    }

    public static boolean isFighting(Player player) {
        return fightOf(player) != null || PENDING.stream().anyMatch(p -> p.player.equals(player.getUUID()));
    }

    /** Chamado pela fenda: o jogador entra no fim deste tick (na luta dessa fenda, ou numa nova se for o primeiro). */
    public static void enterNextTick(ServerPlayer player, TarotArcana arcana, GlobalPos rift, UUID opener) {
        PENDING.add(new PendingEntry(player.getUUID(), arcana, rift, opener));
    }

    public static @Nullable Fight fightOf(Player player) {
        for (Fight fight : FIGHTS) {
            if (fight.players.contains(player.getUUID())) {
                return fight;
            }
        }
        return null;
    }

    /** Luta aberta por esta fenda (ou alguém entrando nela agora)? A fenda fica aberta enquanto houver. */
    public static boolean hasFightAt(GlobalPos rift) {
        return fightAt(rift) != null || PENDING.stream().anyMatch(p -> p.rift.equals(rift));
    }

    private static @Nullable Fight fightAt(GlobalPos rift) {
        for (Fight fight : FIGHTS) {
            if (fight.rift.equals(rift)) {
                return fight;
            }
        }
        return null;
    }

    public static BlockPos arenaCenter(int slot) {
        return new BlockPos(slot * SLOT_SPACING, ARENA_Y, 0);
    }

    private static int freeSlot() {
        Set<Integer> used = new HashSet<>();
        for (Fight fight : FIGHTS) {
            used.add(fight.slot);
        }
        int slot = 0;
        while (used.contains(slot)) {
            slot++;
        }
        return slot;
    }

    private static BossEvent.BossBarColor barColor(TarotArcana arcana) {
        int c = arcana.color();
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        if (r > 200 && g > 180 && b > 180) return BossEvent.BossBarColor.WHITE;
        if (r > 200 && g > 160 && b < 120) return BossEvent.BossBarColor.YELLOW;
        if (r > g && r > b && b > 120) return BossEvent.BossBarColor.PINK;
        if (r > g && r > b) return BossEvent.BossBarColor.RED;
        if (g > r && g > b) return BossEvent.BossBarColor.GREEN;
        if (b > r && b > g) return BossEvent.BossBarColor.BLUE;
        return BossEvent.BossBarColor.PURPLE;
    }

    // ----- entrar -----

    /** Leva o jogador para a arena da luta desta fenda (criando a luta se ele for o primeiro). */
    private static void enter(ServerPlayer player, TarotArcana arcana, GlobalPos rift, UUID opener) {
        MinecraftServer server = player.level().getServer();
        ServerLevel plane = server.getLevel(TarotContent.ARCANE_PLANE);
        Fight fight = fightAt(rift);
        if (plane == null) {
            player.sendSystemMessage(Component.translatable("message.mistech.tarot.no_plane"));
            if (fight == null) {
                Block.popResource(player.level(), rift.pos(), new ItemStack(TarotContent.invertedCard(arcana)));
            }
            return;
        }
        if (fight != null && fight.stage == Stage.VICTORY) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tarot.fight_over"));
            return;
        }
        if (fight != null && fight.fallen.contains(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tarot.cant_return"));
            return;
        }
        if (fight == null) {
            fight = new Fight(opener, rift, arcana, freeSlot());
            FIGHTS.add(fight);
            fight.layout.build(plane, fight.center);
            plane.playSound(null, fight.center, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.8F, 0.7F);
        }

        CompoundTag back = new CompoundTag();
        back.putString("Dim", player.level().dimension().identifier().toString());
        back.putDouble("X", player.getX());
        back.putDouble("Y", player.getY());
        back.putDouble("Z", player.getZ());
        back.putFloat("Yaw", player.getYRot());
        player.getPersistentData().put(RETURN_TAG, back);

        // cada um chega num ponto diferente, olhando para o centro
        Vec3 arrival = fight.layout.arrival(fight.center, fight.arrivals++);
        float yaw = yawToward(arrival, Vec3.atBottomCenterOf(fight.center));
        player.teleport(new TeleportTransition(plane, arrival, Vec3.ZERO, yaw, 0.0F, TeleportTransition.DO_NOTHING));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        fight.players.add(player.getUUID());
        fight.bar.addPlayer(player);
        rescaleBoss(plane, fight);
        player.sendSystemMessage(Component.translatable("message.mistech.tarot.enter", arcana.fullName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (ServerPlayer other : fight.online(server)) {
            if (other != player) {
                other.sendSystemMessage(Component.translatable("message.mistech.tarot.joined", player.getDisplayName())
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    /** Rotação (yaw) para olhar de {@code from} para {@code to}. */
    public static float yawToward(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }

    /** Vida do chefe conforme o tamanho do grupo (mantém a porcentagem de vida atual). */
    private static void rescaleBoss(ServerLevel plane, Fight fight) {
        if (fight.boss == null || !(plane.getEntity(fight.boss) instanceof LivingEntity boss) || !boss.isAlive()) {
            return;
        }
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        float ratio = boss.getHealth() / boss.getMaxHealth();
        health.removeModifier(GROUP_HEALTH_ID);
        int extra = Math.max(0, fight.players.size() - 1);
        if (extra > 0) {
            health.addTransientModifier(new AttributeModifier(GROUP_HEALTH_ID, extra * GROUP_HEALTH_BONUS,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        boss.setHealth(Math.max(1.0F, ratio * boss.getMaxHealth()));
    }

    // ----- sair -----

    /** Volta para onde estava ao entrar na fenda (ou o spawn do mundo). */
    public static void sendHome(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel target = server.overworld();
        Vec3 pos = null;
        float yaw = player.getYRot();
        CompoundTag back = player.getPersistentData().getCompoundOrEmpty(RETURN_TAG);
        if (back.contains("Dim")) {
            Identifier dimId = Identifier.tryParse(back.getStringOr("Dim", "minecraft:overworld"));
            ServerLevel level = dimId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
            if (level != null && !isPlane(level)) {
                target = level;
                pos = new Vec3(back.getDoubleOr("X", 0.0), back.getDoubleOr("Y", 64.0), back.getDoubleOr("Z", 0.0));
                yaw = back.getFloatOr("Yaw", yaw);
            }
        }
        if (pos == null) {
            BlockPos spawn = target.getRespawnData().pos();
            pos = Vec3.atBottomCenterOf(target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn));
        }
        player.getPersistentData().remove(RETURN_TAG);
        player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, yaw, 0.0F, TeleportTransition.DO_NOTHING));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));
    }

    /** Tira um jogador da luta (morreu, saiu do jogo...). Sem ninguém, a luta acaba. Não teleporta. */
    private static void removePlayer(MinecraftServer server, Fight fight, UUID id) {
        if (!fight.players.remove(id)) {
            return;
        }
        fight.fallen.add(id);
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            fight.bar.removePlayer(player);
        }
        if (fight.players.isEmpty()) {
            endFight(server, fight);
            return;
        }
        ServerLevel plane = server.getLevel(TarotContent.ARCANE_PLANE);
        if (plane != null) {
            rescaleBoss(plane, fight);
        }
    }

    /** Encerra a luta (tira a barra e o chefe). Não teleporta. */
    private static void endFight(MinecraftServer server, Fight fight) {
        FIGHTS.remove(fight);
        fight.bar.removeAllPlayers();
        ServerLevel plane = server.getLevel(TarotContent.ARCANE_PLANE);
        if (plane != null && fight.boss != null) {
            Entity boss = plane.getEntity(fight.boss);
            if (boss != null && boss.isAlive()) {
                boss.discard();
            }
        }
    }

    /** Cancela a luta (o chefe não pôde aparecer): todos voltam e a carta volta para quem abriu (ou o primeiro). */
    private static void cancelFight(MinecraftServer server, Fight fight) {
        List<ServerPlayer> players = fight.online(server);
        endFight(server, fight);
        ServerPlayer receiver = server.getPlayerList().getPlayer(fight.opener);
        if (receiver == null || !players.contains(receiver)) {
            receiver = players.isEmpty() ? null : players.get(0);
        }
        for (ServerPlayer player : players) {
            sendHome(player);
            player.sendSystemMessage(Component.translatable("message.mistech.tarot.boss_vanished"));
        }
        if (receiver != null) {
            receiver.getInventory().placeItemBackInInventory(new ItemStack(TarotContent.invertedCard(fight.arcana)));
        }
    }

    // ----- luta -----

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!PENDING.isEmpty()) {
            List<PendingEntry> entries = new ArrayList<>(PENDING);
            PENDING.clear();
            for (PendingEntry entry : entries) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.player);
                if (player != null && fightOf(player) == null && !isPlane(player.level())) {
                    enter(player, entry.arcana, entry.rift, entry.opener);
                }
            }
        }
        if (FIGHTS.isEmpty()) {
            return;
        }
        ServerLevel plane = server.getLevel(TarotContent.ARCANE_PLANE);
        for (Fight fight : new ArrayList<>(FIGHTS)) {
            if (plane == null) {
                endFight(server, fight);
                continue;
            }
            // quem não está mais lá (saiu da dimensão por outro meio) deixa a luta
            for (UUID id : new ArrayList<>(fight.players)) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null || player.level() != plane) {
                    removePlayer(server, fight, id);
                }
            }
            if (!FIGHTS.contains(fight)) {
                continue;
            }
            List<ServerPlayer> players = fight.online(server);
            fight.ticks++;
            // caiu abaixo da arena: a arena pode "pegar" o jogador (senão o vazio cuida)
            for (ServerPlayer player : players) {
                if (player.getY() < fight.center.getY() + fight.layout.fallLimit() && !player.isSpectator()) {
                    fight.layout.catchFall(plane, fight, player);
                }
            }
            if (fight.stage != Stage.VICTORY) {
                fight.layout.tick(plane, fight, players);
            }
            switch (fight.stage) {
                case COUNTDOWN -> tickCountdown(plane, players, fight);
                case FIGHT -> tickFight(plane, fight);
                case VICTORY -> {
                    if (fight.ticks >= VICTORY_DELAY) {
                        endFight(server, fight);
                        players.forEach(ArcanePlane::sendHome);
                    } else if (fight.ticks % 5 == 0) {
                        plane.sendParticles(ParticleTypes.END_ROD, fight.center.getX() + 0.5, fight.center.getY() + 1.5,
                                fight.center.getZ() + 0.5, 6, 2.0, 1.5, 2.0, 0.05);
                    }
                }
            }
        }
    }

    private static void tickCountdown(ServerLevel plane, List<ServerPlayer> players, Fight fight) {
        Vec3 c = fight.layout.bossSpawn(fight.center);
        double radius = 3.0 * (1.0 - fight.ticks / (double) COUNTDOWN) + 0.3;
        for (int i = 0; i < 6; i++) {
            double angle = fight.ticks * 0.3 + i * Math.PI / 3.0;
            plane.sendParticles(new DustParticleOptions(fight.arcana.color(), 1.6F), c.x + Math.cos(angle) * radius,
                    c.y + 0.2 + fight.ticks / (double) COUNTDOWN * 2.0, c.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
        fight.bar.setProgress(fight.ticks / (float) COUNTDOWN);
        if (fight.ticks % 20 == 0) {
            plane.playSound(null, fight.center, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.0F, 0.5F + fight.ticks / 60.0F);
        }
        if (fight.ticks < COUNTDOWN || players.isEmpty()) {
            return;
        }
        ServerPlayer challenger = players.get(plane.getRandom().nextInt(players.size()));
        LivingEntity boss = ArcanaBosses.create(plane, fight, c, challenger);
        if (boss == null) {
            Mistech.LOGGER.error("Não foi possível criar o chefe do arcano {}", fight.arcana.id());
            cancelFight(plane.getServer(), fight);
            return;
        }
        boss.snapTo(c.x, c.y, c.z, 0.0F, 0.0F);
        // a luta precisa conhecer o chefe ANTES de ele entrar no mundo: o onJoin descarta chefes sem luta
        fight.boss = boss.getUUID();
        if (!plane.addFreshEntity(boss)) {
            Mistech.LOGGER.error("O chefe do arcano {} não pôde entrar no mundo", fight.arcana.id());
        }
        fight.stage = Stage.FIGHT;
        fight.ticks = 0;
        rescaleBoss(plane, fight);
        plane.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1.0, c.z, 1, 0, 0, 0, 0);
        plane.playSound(null, fight.center, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.7F, 1.4F);
        for (ServerPlayer player : players) {
            player.sendOverlayMessage(Component.translatable("message.mistech.tarot.boss_appears", fight.arcana.displayName()));
        }
    }

    private static void tickFight(ServerLevel plane, Fight fight) {
        Entity entity = fight.boss == null ? null : plane.getEntity(fight.boss);
        if (!(entity instanceof LivingEntity boss) || !boss.isAlive()) {
            if (entity == null && fight.ticks > 20) {
                // sumiu sem morrer (descarregado/removido): chama de novo, algumas vezes
                if (++fight.respawns > MAX_RESPAWNS) {
                    cancelFight(plane.getServer(), fight);
                    return;
                }
                fight.stage = Stage.COUNTDOWN;
                fight.ticks = COUNTDOWN - 20;
            }
            return;
        }
        fight.bar.setProgress(Mth.clamp(boss.getHealth() / boss.getMaxHealth(), 0.0F, 1.0F));
        Vec3 c = fight.layout.bossSpawn(fight.center);
        double dx = boss.getX() - (fight.center.getX() + 0.5);
        double dz = boss.getZ() - (fight.center.getZ() + 0.5);
        double leash = fight.layout.leashRadius();
        if (dx * dx + dz * dz > leash * leash || boss.getY() < fight.center.getY() + fight.layout.leashBelow()
                || boss.getY() > fight.center.getY() + 40.0) {
            boss.teleportTo(c.x, c.y, c.z);
            boss.resetFallDistance();
            plane.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1.0, c.z, 30, 0.4, 0.8, 0.4, 0.05);
        }
        if (boss instanceof ArcanaBossEntity arcanaBoss) {
            arcanaBoss.onFightTick(fight);
        }
    }

    private static @Nullable Fight fightOfBoss(Entity entity) {
        for (Fight fight : FIGHTS) {
            if (entity.getUUID().equals(fight.boss)) {
                return fight;
            }
        }
        return null;
    }

    /**
     * Vitória: o chefe morreu. Todos que estão na arena contam como tendo derrotado o arcano, mas só cai UMA carta:
     * fica com quem abriu a fenda; se ele não estiver mais lá, com quem deu o golpe final; senão, com o primeiro do grupo.
     */
    private static void victory(ServerLevel plane, Fight fight, @Nullable Entity killer) {
        fight.stage = Stage.VICTORY;
        fight.ticks = 0;
        fight.bar.setProgress(0.0F);
        List<ServerPlayer> players = fight.online(plane.getServer());
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer receiver = null;
        for (ServerPlayer player : players) {
            if (player.getUUID().equals(fight.opener)) {
                receiver = player;
            }
        }
        if (receiver == null && killer instanceof ServerPlayer k && players.contains(k)) {
            receiver = k;
        }
        if (receiver == null) {
            receiver = players.get(0);
        }
        receiver.getInventory().placeItemBackInInventory(new ItemStack(TarotContent.card(fight.arcana)));
        for (ServerPlayer player : players) {
            TarotData data = player.getData(TarotContent.TAROT);
            boolean first = !data.hasDefeated(fight.arcana);
            player.setData(TarotContent.TAROT, data.with(fight.arcana));
            plane.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            plane.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 1.0, 0.5, 0.4);
            if (player == receiver) {
                player.sendSystemMessage(Component.translatable("message.mistech.tarot.victory", fight.arcana.fullName())
                        .withStyle(ChatFormatting.GOLD));
            } else {
                player.sendSystemMessage(Component.translatable("message.mistech.tarot.victory_group", fight.arcana.fullName(),
                        receiver.getDisplayName()).withStyle(ChatFormatting.GOLD));
            }
            if (first) {
                player.sendSystemMessage(Component.translatable("message.mistech.tarot.first_victory", fight.arcana.displayName())
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !isPlane(level)) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            // cair na arena não mata: volta com tudo; o grupo continua sem ele
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.removeAllEffects();
            player.resetFallDistance();
            player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), 6));
            Fight fight = fightOf(player);
            if (fight != null) {
                boolean lost = fight.stage != Stage.VICTORY;
                removePlayer(level.getServer(), fight, player.getUUID());
                if (lost) {
                    boolean groupLeft = FIGHTS.contains(fight);
                    player.sendSystemMessage(Component.translatable(groupLeft ? "message.mistech.tarot.fell" : "message.mistech.tarot.defeat",
                            fight.arcana.displayName()).withStyle(ChatFormatting.DARK_RED));
                    for (ServerPlayer other : fight.online(level.getServer())) {
                        other.sendSystemMessage(Component.translatable("message.mistech.tarot.ally_fell", player.getDisplayName())
                                .withStyle(ChatFormatting.RED));
                    }
                }
            }
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            sendHome(player);
            return;
        }
        Fight fight = fightOfBoss(entity);
        if (fight != null && fight.stage == Stage.FIGHT) {
            victory(level, fight, event.getSource().getEntity());
        }
    }

    /** Chefes não derrubam nada (o prêmio é a carta). */
    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().entityTags().contains(BOSS_TAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Fight fight = fightOf(player);
            if (fight != null) {
                removePlayer(player.level().getServer(), fight, player.getUUID());
            }
        }
    }

    /** Entrou no jogo dentro do plano sem luta (caiu a conexão, servidor reiniciou...): volta para casa. */
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isPlane(player.level()) && !isFighting(player)) {
            sendHome(player);
            player.sendSystemMessage(Component.translatable("message.mistech.tarot.fight_lost"));
        }
    }

    /** Chefes que sobraram de lutas antigas (servidor reiniciado) não voltam. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide() && isPlane(event.getLevel()) && entity.entityTags().contains(BOSS_TAG)
                && fightOfBoss(entity) == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        for (Fight fight : new ArrayList<>(FIGHTS)) {
            fight.bar.removeAllPlayers();
        }
        FIGHTS.clear();
        PENDING.clear();
    }

    // ----- proteger a arena -----

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof Level level && isPlane(level) && !event.getPlayer().hasInfiniteMaterials()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && isPlane(level)
                && !(event.getEntity() instanceof Player player && player.hasInfiniteMaterials())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onExplosion(ExplosionEvent.Detonate event) {
        if (isPlane(event.getLevel())) {
            event.getAffectedBlocks().clear();
        }
    }
}
