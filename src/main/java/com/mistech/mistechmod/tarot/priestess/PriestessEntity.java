package com.mistech.mistechmod.tarot.priestess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.tarot.ArcanaBossEntity;
import com.mistech.mistechmod.tarot.ArcanePlane;
import com.mistech.mistechmod.tarot.TarotArcana;
import com.mistech.mistechmod.tarot.magus.MagusSpellEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Sacerdotisa (arcano II): instinto, introspecção e sabedoria. Fica no trono entre os pilares e alterna dois modos.
 * <ul>
 * <li><b>Boaz</b> (escuridão, {@link #BOAZ_TICKS}): todos são jogados ao centro e ficam cegos; ela, velada no trono,
 * não sofre dano. Só dá para <b>desviar</b>: cada golpe vem com um presságio (som posicional, partículas perto do
 * jogador e um brilho na borda da tela do lado de onde vem):
 * <ul>
 * <li><b>Rajada Lunar</b>: um sussurro de um lado; 1 s depois um feixe varre a linha que passa pelo jogador.</li>
 * <li><b>Romãs</b>: um anel vermelho nos pés e gotas pingando; algo cai ali — sair do anel.</li>
 * <li><b>Maré</b>: som de onda subindo; uma onda vem da borda da arena até o centro pelo chão — pular quando passar.</li>
 * <li><b>Silêncio</b>: "Aquieta-te." — quem se mexer nos 2,5 s seguintes leva dano.</li>
 * </ul>
 * Cada presságio esquivado dá 1 acúmulo de <b>Intuição</b> (até {@link #MAX_INTUITION}).</li>
 * <li><b>Jachin</b> (luz, {@link #JACHIN_TICKS}): a visão volta, o trono fica vazio e surgem reflexos iguais a ela pela
 * arena. Só a verdadeira tem <b>sombra</b> (e deixa cair sombra no chão). Os golpes são rápidos e fracos. Bater num
 * reflexo o estoura (com um contra-ataque); bater na verdadeira causa dano, +{@link #INTUITION_BONUS} por acúmulo de
 * Intuição. De tempos em tempos (ou depois de alguns golpes) todas trocam de lugar.</li>
 * </ul>
 * Abaixo de 50% o Boaz junta dois presságios de uma vez; abaixo de 25% aparece mais um reflexo e tudo fica mais rápido.
 */
public class PriestessEntity extends ArcanaBossEntity implements PriestessFigure {
    public static final int BOAZ = 0, JACHIN = 1;

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(PriestessEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(PriestessEntity.class, EntityDataSerializers.INT);

    // ----- números (provisórios) -----
    public static final double MAX_HEALTH = 600.0;
    public static final int BOAZ_TICKS = 400;
    public static final int JACHIN_TICKS = 300;
    public static final int MAX_INTUITION = 10;
    public static final float INTUITION_BONUS = 0.12F;
    private static final float GUST_DAMAGE = 10.0F;
    private static final float POMEGRANATE_DAMAGE = 12.0F;
    private static final float TIDE_DAMAGE = 8.0F;
    private static final float SILENCE_DAMAGE = 3.0F;
    private static final float BOLT_DAMAGE = 3.5F;
    private static final float SHATTER_DAMAGE = 5.0F;
    /** Fração da vida máxima que, perdida num Jachin, já faz ela voltar ao Boaz. */
    private static final float JACHIN_DAMAGE_CAP = 0.2F;

    private @Nullable BlockPos arenaCenter;
    private @Nullable Vec3 home;
    private @Nullable Set<UUID> fightPlayers;
    private boolean started;
    private int modeTicks;
    private int phase = 1;
    private int omenTimer;
    private int lastOmen = -1;
    private int boltTimer;
    private int shuffleTimer;
    private int hitsSinceShuffle;
    private float jachinStartHealth;
    private int actionTicks;
    private final Map<UUID, Integer> intuition = new HashMap<>();
    private final Map<UUID, Long> lastVeiledMessage = new HashMap<>();
    private final List<UUID> reflections = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();

    /** Cliente: tick em que a ação atual começou. */
    private int clientActionStart;

    private record Task(int at, Runnable run) {}

    public PriestessEntity(EntityType<? extends PriestessEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public TarotArcana arcana() {
        return TarotArcana.THE_HIGH_PRIESTESS;
    }

    public void setArena(BlockPos center) {
        this.arenaCenter = center;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODE, BOAZ);
        builder.define(DATA_ACTION, IDLE);
    }

    public int mode() {
        return this.entityData.get(DATA_MODE);
    }

    @Override
    public int figureAction() {
        return this.entityData.get(DATA_ACTION);
    }

    @Override
    public float figureActionTime(float partialTick) {
        return this.tickCount - this.clientActionStart + partialTick;
    }

    @Override
    public boolean seated() {
        return this.mode() == BOAZ;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ACTION.equals(accessor)) {
            this.clientActionStart = this.tickCount;
        }
    }

    private void setAction(int action) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = 0;
    }

    // ------------------------------------------------------------------ geometria

    /** Centro da arena (onde o jogador é jogado no Boaz). Fora do Plano Arcano: 7 blocos à frente de onde ela surgiu. */
    private Vec3 center() {
        if (this.arenaCenter != null) {
            return Vec3.atBottomCenterOf(this.arenaCenter.above());
        }
        Vec3 h = this.home();
        return h.add(0.0, 0.0, 7.0);
    }

    private Vec3 home() {
        if (this.home == null) {
            this.home = this.position();
        }
        return this.home;
    }

    private Vec3 seat() {
        return this.arenaCenter != null ? PriestessArena.throneSeat(this.arenaCenter) : this.home();
    }

    private double arenaRadius() {
        return this.arenaCenter != null ? PriestessArena.RADIUS : 12.0;
    }

    private double floorY() {
        return this.arenaCenter != null ? this.arenaCenter.getY() + 1.0 : this.home().y;
    }

    // ------------------------------------------------------------------ jogadores

    @Override
    public void onFightTick(ArcanePlane.Fight fight) {
        if (this.arenaCenter == null) {
            this.setArena(fight.center());
        }
        this.fightPlayers = Set.copyOf(fight.players());
    }

    private List<ServerPlayer> players(ServerLevel level) {
        List<ServerPlayer> list = new ArrayList<>();
        if (this.fightPlayers != null) {
            for (UUID id : this.fightPlayers) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null && player.level() == level && player.isAlive() && !player.isSpectator() && !player.isCreative()) {
                    list.add(player);
                }
            }
            return list;
        }
        Vec3 c = this.center();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(32.0, 16.0, 32.0),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            list.add(player);
        }
        return list;
    }

    public int intuition(Player player) {
        return this.intuition.getOrDefault(player.getUUID(), 0);
    }

    private void addIntuition(ServerLevel level, ServerPlayer player) {
        int value = Math.min(MAX_INTUITION, this.intuition(player) + 1);
        this.intuition.put(player.getUUID(), value);
        level.sendParticles(player, ParticleTypes.END_ROD, true, false, player.getX(), player.getY() + 2.1, player.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, 1.2F + value * 0.05F);
        player.sendOverlayMessage(Component.translatable("message.mistech.priestess.intuition", value).withStyle(ChatFormatting.AQUA));
    }

    private static boolean grounded(Player player) {
        return player.onGround() || player.isInWater();
    }

    // ------------------------------------------------------------------ agenda

    private void schedule(int delay, Runnable run) {
        this.tasks.add(new Task(this.tickCount + delay, run));
    }

    private void runTasks() {
        if (this.tasks.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = this.tasks.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (task.at <= this.tickCount) {
                due.add(task);
                it.remove();
            }
        }
        for (Task task : due) {
            task.run.run();
        }
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.home();
        if (!this.started) {
            this.started = true;
            this.schedule(40, () -> this.startBoaz(level));
            this.say(level, "message.mistech.priestess.greet");
        }
        this.runTasks();
        this.actionTicks++;
        if (this.figureAction() != IDLE && this.actionTicks > 22) {
            this.setAction(IDLE);
        }
        this.modeTicks++;
        this.updatePhase(level);
        if (this.mode() == BOAZ) {
            this.tickBoaz(level);
        } else {
            this.tickJachin(level);
        }
    }

    private void updatePhase(ServerLevel level) {
        float ratio = this.getHealth() / this.getMaxHealth();
        if (this.phase < 2 && ratio < 0.5F) {
            this.phase = 2;
            this.say(level, "message.mistech.priestess.phase2");
        }
        if (this.phase < 3 && ratio < 0.25F) {
            this.phase = 3;
            this.say(level, "message.mistech.priestess.phase3");
        }
    }

    private void say(ServerLevel level, String key) {
        for (ServerPlayer player : this.players(level)) {
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }
    }

    private void face(Mob mob, @Nullable Vec3 target) {
        if (target == null) {
            return;
        }
        double dx = target.x - mob.getX();
        double dz = target.z - mob.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
    }

    private @Nullable ServerPlayer nearest(ServerLevel level, Entity from) {
        return this.players(level).stream().min(Comparator.comparingDouble(from::distanceToSqr)).orElse(null);
    }

    // ================================================================== BOAZ

    private void startBoaz(ServerLevel level) {
        this.entityData.set(DATA_MODE, BOAZ);
        this.setAction(IDLE);
        this.modeTicks = 0;
        this.tasks.clear();
        this.clearReflections(level);
        this.intuition.clear();
        this.omenTimer = 45;
        this.lastOmen = -1;
        Vec3 seat = this.seat();
        this.teleportTo(seat.x, seat.y, seat.z);
        this.setDeltaMovement(Vec3.ZERO);
        if (this.arenaCenter != null) {
            PriestessArena.setMode(level, this.arenaCenter, true);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.6F);
        // todos ao centro, no escuro
        List<ServerPlayer> players = this.players(level);
        Vec3 c = this.center();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            double angle = i * (Math.PI * 2.0 / Math.max(1, players.size()));
            double r = players.size() > 1 ? 1.6 : 0.0;
            player.teleportTo(c.x + Math.cos(angle) * r, c.y + 0.1, c.z + Math.sin(angle) * r);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            player.resetFallDistance();
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, true, false, true));
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            PriestessOmenPayload.send(player, PriestessOmenPayload.BOAZ, 0.0F, 60, 0);
        }
    }

    private void tickBoaz(ServerLevel level) {
        Vec3 seat = this.seat();
        if (this.position().distanceToSqr(seat) > 0.01) {
            this.teleportTo(seat.x, seat.y, seat.z);
        }
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(0.0F);
        this.setYHeadRot(0.0F);
        this.setYBodyRot(0.0F);
        List<ServerPlayer> players = this.players(level);
        if (this.modeTicks % 20 == 0 && this.modeTicks < BOAZ_TICKS - 15) {
            for (ServerPlayer player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false, true));
            }
        }
        if (this.modeTicks % 30 == 0) {
            for (ServerPlayer player : players) {
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.5F, 0.8F);
            }
        }
        if (this.modeTicks % 4 == 0) {
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 2.0, this.getZ(), 1, 0.5, 0.8, 0.5, 0.01);
        }
        if (--this.omenTimer <= 0 && this.modeTicks < BOAZ_TICKS - 60 && !players.isEmpty()) {
            this.castOmens(level, players);
            this.omenTimer = this.phase >= 3 ? 58 : this.phase == 2 ? 66 : 76;
        }
        if (this.modeTicks >= BOAZ_TICKS) {
            this.startJachin(level);
        }
    }

    private static final int OMEN_GUST = 0, OMEN_POMEGRANATE = 1, OMEN_TIDE = 2, OMEN_SILENCE = 3;

    private void castOmens(ServerLevel level, List<ServerPlayer> players) {
        int omen;
        do {
            omen = this.random.nextInt(4);
        } while (omen == this.lastOmen);
        this.lastOmen = omen;
        this.setAction(OMEN);
        boolean combine = this.phase >= 2 && omen != OMEN_SILENCE && this.random.nextFloat() < 0.65F;
        switch (omen) {
            case OMEN_TIDE -> {
                this.omenTide(level, players);
                if (combine) {
                    for (ServerPlayer player : players) {
                        if (this.random.nextBoolean()) this.omenGust(level, player, 10);
                        else this.omenPomegranate(level, player, 6);
                    }
                }
            }
            case OMEN_SILENCE -> this.omenSilence(level, players);
            default -> {
                for (ServerPlayer player : players) {
                    if (omen == OMEN_GUST) this.omenGust(level, player, 0);
                    else this.omenPomegranate(level, player, 0);
                    if (combine) {
                        if (omen == OMEN_GUST) this.omenPomegranate(level, player, 8);
                        else this.omenGust(level, player, 8);
                    }
                }
            }
        }
    }

    // ----- Rajada Lunar -----

    private void omenGust(ServerLevel level, ServerPlayer target, int delay) {
        if (delay > 0) {
            this.schedule(delay, () -> {
                if (target.isAlive() && this.mode() == BOAZ) this.omenGust(level, target, 0);
            });
            return;
        }
        int warn = this.phase >= 2 ? 24 : 30;
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        Vec3 dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)); // de onde vem (a partir do jogador)
        Vec3 origin = target.position();
        Vec3 source = origin.add(dir.scale(10.0));
        PriestessOmenPayload.send(target, PriestessOmenPayload.GUST, (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F, warn, 0);
        for (int t = 0; t < warn; t += 2) {
            final int tick = t;
            this.schedule(t, () -> {
                // a linha do golpe, perto do jogador
                for (double s = -5.0; s <= 5.0; s += 0.8) {
                    Vec3 p = origin.add(dir.scale(s));
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, origin.y + 0.15, p.z, 1, 0.05, 0.0, 0.05, 0.0);
                }
                // o lado de onde vem: um brilho ao lado da cabeça
                Vec3 side = target.position().add(dir.scale(1.4));
                level.sendParticles(ParticleTypes.END_ROD, side.x, target.getEyeY(), side.z, 2, 0.1, 0.2, 0.1, 0.0);
                if (tick % 8 == 0) {
                    level.playSound(null, BlockPos.containing(source), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 3.0F, 0.7F + tick * 0.01F);
                    level.playSound(null, BlockPos.containing(source), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE, 1.5F, 0.5F);
                }
            });
        }
        this.schedule(warn, () -> {
            if (this.mode() != BOAZ) {
                return;
            }
            level.playSound(null, BlockPos.containing(origin), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.HOSTILE, 2.0F, 0.7F);
            level.playSound(null, BlockPos.containing(origin), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
            for (double s = -14.0; s <= 14.0; s += 1.4) {
                Vec3 p = origin.add(dir.scale(s));
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, origin.y + 1.0, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            boolean hitTarget = false;
            for (ServerPlayer player : this.players(level)) {
                Vec3 v = player.position().subtract(origin);
                Vec3 flat = new Vec3(v.x, 0.0, v.z);
                double along = flat.dot(dir);
                double perp = flat.subtract(dir.scale(along)).length();
                if (perp < 1.5 && Math.abs(v.y) < 2.5 && Math.abs(along) < 30.0) {
                    if (player.hurtServer(level, this.damageSources().mobAttack(this), GUST_DAMAGE)) {
                        push(player, dir.scale(-1.2), 0.4);
                    }
                    if (player == target) {
                        hitTarget = true;
                    }
                }
            }
            if (!hitTarget && target.isAlive()) {
                this.addIntuition(level, target);
            }
        });
    }

    // ----- Romãs -----

    private void omenPomegranate(ServerLevel level, ServerPlayer target, int delay) {
        if (delay > 0) {
            this.schedule(delay, () -> {
                if (target.isAlive() && this.mode() == BOAZ) this.omenPomegranate(level, target, 0);
            });
            return;
        }
        int warn = this.phase >= 2 ? 26 : 32;
        Vec3 c = target.position();
        double radius = 2.6;
        PriestessOmenPayload.send(target, PriestessOmenPayload.POMEGRANATE, 0.0F, warn, 0);
        DustParticleOptions red = new DustParticleOptions(0xB3122E, 1.4F);
        for (int t = 0; t < warn; t += 2) {
            final int tick = t;
            this.schedule(t, () -> {
                for (int i = 0; i < 18; i++) {
                    double a = i * Math.PI * 2.0 / 18 + tick * 0.05;
                    level.sendParticles(red, c.x + Math.cos(a) * radius, c.y + 0.1, c.z + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0);
                }
                level.sendParticles(ParticleTypes.DRIPPING_WATER, c.x, c.y + 3.0, c.z, 3, 1.2, 0.3, 1.2, 0.0);
                if (tick % 6 == 0) {
                    level.playSound(null, BlockPos.containing(c), SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.HOSTILE, 1.5F,
                            0.8F + tick / (float) warn);
                }
                if (tick == warn - 10) {
                    level.playSound(null, BlockPos.containing(c.add(0.0, 4.0, 0.0)), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                            SoundSource.HOSTILE, 2.0F, 0.6F);
                }
            });
        }
        this.schedule(warn, () -> {
            if (this.mode() != BOAZ) {
                return;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState()),
                    c.x, c.y + 0.5, c.z, 60, radius * 0.5, 0.4, radius * 0.5, 0.15);
            level.sendParticles(red, c.x, c.y + 0.4, c.z, 30, radius * 0.5, 0.3, radius * 0.5, 0.0);
            level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, BlockPos.containing(c), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
            level.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.4F);
            boolean hitTarget = false;
            for (ServerPlayer player : this.players(level)) {
                Vec3 v = player.position().subtract(c);
                if (v.x * v.x + v.z * v.z < (radius + 0.3) * (radius + 0.3) && Math.abs(v.y) < 3.0) {
                    player.hurtServer(level, this.damageSources().mobAttack(this), POMEGRANATE_DAMAGE);
                    if (player == target) {
                        hitTarget = true;
                    }
                }
            }
            if (!hitTarget && target.isAlive()) {
                this.addIntuition(level, target);
            }
        });
    }

    // ----- Maré -----

    private void omenTide(ServerLevel level, List<ServerPlayer> targets) {
        int warn = this.phase >= 2 ? 22 : 28;
        double start = this.arenaRadius() - 0.5;
        double speed = this.phase >= 3 ? 0.55 : 0.45;
        int travel = (int) Math.ceil(start / speed);
        Vec3 c = this.center();
        for (ServerPlayer player : targets) {
            PriestessOmenPayload.send(player, PriestessOmenPayload.TIDE, 0.0F, warn, 0);
        }
        for (int t = 0; t < warn; t += 2) {
            final int tick = t;
            this.schedule(t, () -> {
                for (ServerPlayer player : this.players(level)) {
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, player.getX(), player.getY() + 0.1, player.getZ(), 4, 1.0, 0.0, 1.0, 0.05);
                    level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.1, player.getZ(), 6, 1.2, 0.0, 1.2, 0.0);
                    if (tick % 6 == 0) {
                        level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.HOSTILE, 1.5F,
                                0.6F + tick * 0.02F);
                    }
                }
                if (tick == 0) {
                    level.playSound(null, BlockPos.containing(c), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.6F);
                }
            });
        }
        Set<UUID> hit = new HashSet<>();
        for (int t = 0; t <= travel; t++) {
            final double r = start - t * speed;
            this.schedule(warn + t, () -> {
                if (this.mode() != BOAZ) {
                    return;
                }
                for (ServerPlayer player : this.players(level)) {
                    double dx = player.getX() - c.x;
                    double dz = player.getZ() - c.z;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    // a onda só é desenhada perto de cada jogador (com Cegueira ele não vê mais longe que isso)
                    if (Math.abs(d - r) < 6.0) {
                        double base = Math.atan2(dz, dx);
                        for (int i = -6; i <= 6; i++) {
                            double a = base + i * 0.9 / Math.max(2.0, r);
                            level.sendParticles(player, ParticleTypes.SPLASH, true, false, c.x + Math.cos(a) * r, c.y + 0.2,
                                    c.z + Math.sin(a) * r, 3, 0.1, 0.15, 0.1, 0.0);
                            level.sendParticles(player, ParticleTypes.BUBBLE_POP, true, false, c.x + Math.cos(a) * r, c.y + 0.5,
                                    c.z + Math.sin(a) * r, 1, 0.1, 0.2, 0.1, 0.0);
                        }
                        if (Math.abs(d - r) < 3.0 && Math.abs(d - r) > 2.5) {
                            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.5F, 0.6F);
                        }
                    }
                    if (Math.abs(d - r) < 0.8 && grounded(player) && player.getY() < c.y + 0.7 && hit.add(player.getUUID())) {
                        if (player.hurtServer(level, this.damageSources().mobAttack(this), TIDE_DAMAGE)) {
                            Vec3 in = new Vec3(-dx, 0.0, -dz);
                            push(player, in.lengthSqr() < 1.0E-4 ? Vec3.ZERO : in.normalize().scale(0.5), 0.3);
                            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
                        }
                    }
                }
            });
        }
        this.schedule(warn + travel + 1, () -> {
            if (this.mode() != BOAZ) {
                return;
            }
            for (ServerPlayer player : this.players(level)) {
                if (!hit.contains(player.getUUID())) {
                    this.addIntuition(level, player);
                }
            }
        });
    }

    // ----- Silêncio -----

    private void omenSilence(ServerLevel level, List<ServerPlayer> targets) {
        int grace = 20;
        int length = 50;
        for (ServerPlayer player : targets) {
            PriestessOmenPayload.send(player, PriestessOmenPayload.SILENCE, 0.0F, grace + length, grace);
            player.sendOverlayMessage(Component.translatable("message.mistech.priestess.silence").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.6F, 0.5F);
        }
        Map<UUID, Vec3> last = new HashMap<>();
        Set<UUID> moved = new HashSet<>();
        for (int t = 0; t <= grace + length; t += 2) {
            final int tick = t;
            this.schedule(t, () -> {
                if (this.mode() != BOAZ) {
                    return;
                }
                for (ServerPlayer player : this.players(level)) {
                    double a = tick * 0.25;
                    for (int i = 0; i < 3; i++) {
                        double b = a + i * Math.PI * 2.0 / 3.0;
                        level.sendParticles(ParticleTypes.SOUL, player.getX() + Math.cos(b) * 1.2, player.getY() + 1.0,
                                player.getZ() + Math.sin(b) * 1.2, 1, 0.0, 0.05, 0.0, 0.0);
                    }
                    if (tick < grace) {
                        last.put(player.getUUID(), player.position());
                        continue;
                    }
                    if (tick % 6 != 0) {
                        continue;
                    }
                    Vec3 before = last.put(player.getUUID(), player.position());
                    if (before != null) {
                        Vec3 delta = player.position().subtract(before);
                        boolean moving = delta.x * delta.x + delta.z * delta.z > 0.12 * 0.12 || delta.y > 0.25;
                        if (moving) {
                            moved.add(player.getUUID());
                            player.hurtServer(level, this.damageSources().magic(), SILENCE_DAMAGE);
                            level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
                            level.playSound(null, player.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.HOSTILE, 1.5F, 0.6F);
                        }
                    }
                }
                if (tick == grace + length) {
                    for (ServerPlayer player : this.players(level)) {
                        if (!moved.contains(player.getUUID())) {
                            this.addIntuition(level, player);
                        }
                    }
                }
            });
        }
    }

    // ================================================================== JACHIN

    private void startJachin(ServerLevel level) {
        this.entityData.set(DATA_MODE, JACHIN);
        this.setAction(IDLE);
        this.modeTicks = 0;
        this.tasks.clear();
        this.jachinStartHealth = this.getHealth();
        this.hitsSinceShuffle = 0;
        this.boltTimer = 30;
        this.shuffleTimer = this.shuffleInterval();
        if (this.arenaCenter != null) {
            PriestessArena.setMode(level, this.arenaCenter, false);
        }
        Vec3 throne = this.position();
        level.sendParticles(ParticleTypes.END_ROD, throne.x, throne.y + 1.5, throne.z, 40, 0.5, 1.0, 0.5, 0.15);
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 1.2F);
        level.playSound(null, this.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.0F, 1.4F);
        for (ServerPlayer player : this.players(level)) {
            player.removeEffect(MobEffects.BLINDNESS);
            PriestessOmenPayload.send(player, PriestessOmenPayload.JACHIN, 0.0F, 60, this.intuition(player));
        }
        // ela e os reflexos aparecem espalhados
        int count = this.phase >= 3 ? 4 : 3;
        for (int i = 0; i < count; i++) {
            PriestessReflectionEntity reflection = PriestessContent.REFLECTION.get().create(level, EntitySpawnReason.EVENT);
            if (reflection != null) {
                reflection.setOwner(this);
                reflection.snapTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
                level.addFreshEntity(reflection);
                this.reflections.add(reflection.getUUID());
            }
        }
        this.shuffle(level, false);
    }

    private int shuffleInterval() {
        return this.phase >= 3 ? 70 : this.phase == 2 ? 80 : 95;
    }

    private List<PriestessReflectionEntity> reflections(ServerLevel level) {
        List<PriestessReflectionEntity> list = new ArrayList<>();
        for (Iterator<UUID> it = this.reflections.iterator(); it.hasNext(); ) {
            if (level.getEntity(it.next()) instanceof PriestessReflectionEntity reflection && reflection.isAlive()) {
                list.add(reflection);
            } else {
                it.remove();
            }
        }
        return list;
    }

    /** Todas as figuras (a verdadeira + os reflexos vivos). */
    private List<Mob> figures(ServerLevel level) {
        List<Mob> list = new ArrayList<>(this.reflections(level));
        list.add(this);
        return list;
    }

    /** Ela e os reflexos trocam de lugar (lugares novos, sorteados, longe uns dos outros). */
    private void shuffle(ServerLevel level, boolean announce) {
        List<Mob> figures = this.figures(level);
        java.util.Collections.shuffle(figures, new java.util.Random(this.random.nextLong()));
        List<Vec3> spots = this.spots(figures.size());
        for (int i = 0; i < figures.size(); i++) {
            Mob figure = figures.get(i);
            level.sendParticles(ParticleTypes.POOF, figure.getX(), figure.getY() + 1.5, figure.getZ(), 10, 0.3, 0.8, 0.3, 0.02);
            Vec3 spot = spots.get(i);
            figure.teleportTo(spot.x, spot.y, spot.z);
            figure.setDeltaMovement(Vec3.ZERO);
            level.sendParticles(ParticleTypes.END_ROD, spot.x, spot.y + 1.5, spot.z, 12, 0.3, 0.8, 0.3, 0.05);
        }
        level.playSound(null, BlockPos.containing(this.center()), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 2.0F, 0.9F);
        if (announce) {
            for (ServerPlayer player : this.players(level)) {
                player.sendOverlayMessage(Component.translatable("message.mistech.priestess.shuffle").withStyle(ChatFormatting.GRAY));
            }
        }
        this.hitsSinceShuffle = 0;
        this.shuffleTimer = this.shuffleInterval();
    }

    private List<Vec3> spots(int count) {
        Vec3 c = this.center();
        double maxR = this.arenaCenter != null ? 13.0 : 8.0;
        List<Vec3> spots = new ArrayList<>();
        int tries = 0;
        while (spots.size() < count && tries++ < 400) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double r = 5.0 + this.random.nextDouble() * (maxR - 5.0);
            Vec3 p = new Vec3(c.x + Math.cos(a) * r, this.floorY(), c.z + Math.sin(a) * r);
            if (this.arenaCenter != null && p.z < this.arenaCenter.getZ() - 9.0) {
                continue; // longe do estrado do trono
            }
            boolean ok = true;
            for (Vec3 other : spots) {
                if (other.distanceToSqr(p) < 4.5 * 4.5) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                spots.add(p);
            }
        }
        while (spots.size() < count) {
            double a = spots.size() * Math.PI * 2.0 / count;
            spots.add(new Vec3(c.x + Math.cos(a) * 8.0, this.floorY(), c.z + Math.sin(a) * 8.0));
        }
        return spots;
    }

    private void tickJachin(ServerLevel level) {
        List<ServerPlayer> players = this.players(level);
        List<Mob> figures = this.figures(level);
        // todas olham para o jogador mais perto (igualzinho)
        for (Mob figure : figures) {
            ServerPlayer near = this.nearest(level, figure);
            this.face(figure, near == null ? null : near.position());
            figure.setDeltaMovement(Vec3.ZERO);
        }
        // a sombra: só a verdadeira deixa cair sombra no chão
        if (this.modeTicks % 5 == 0) {
            DustParticleOptions shadow = new DustParticleOptions(0x0A0A14, 1.6F);
            for (int i = 0; i < 3; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                level.sendParticles(shadow, this.getX() + Math.cos(a) * 0.6, this.getY() + 0.05, this.getZ() + Math.sin(a) * 0.6,
                        1, 0.05, 0.0, 0.05, 0.0);
            }
        }
        // golpes rápidos e fracos, de figuras sorteadas (a verdadeira não ataca mais nem menos que os reflexos)
        if (--this.boltTimer <= 0 && !players.isEmpty()) {
            this.boltTimer = this.phase >= 3 ? 12 : this.phase == 2 ? 17 : 22;
            Mob caster = figures.get(this.random.nextInt(figures.size()));
            ServerPlayer target = players.get(this.random.nextInt(players.size()));
            castBolt(level, caster, target, BOLT_DAMAGE, this.phase >= 3 ? 1.05 : 0.9);
        }
        if (--this.shuffleTimer <= 0 || this.hitsSinceShuffle >= 3) {
            if (this.hitsSinceShuffle >= 3) {
                this.hitsSinceShuffle = 0;
                this.shuffleTimer = 12;
                this.schedule(10, () -> {
                    if (this.mode() == JACHIN) this.shuffle(level, true);
                });
            } else {
                this.shuffle(level, true);
            }
        }
        boolean hurtEnough = this.jachinStartHealth - this.getHealth() >= this.getMaxHealth() * JACHIN_DAMAGE_CAP;
        if (this.modeTicks >= JACHIN_TICKS || hurtEnough) {
            if (hurtEnough) {
                this.say(level, "message.mistech.priestess.retreat");
            }
            this.startBoaz(level);
        }
    }

    /** Lança uma gota de água rápida da figura no jogador. */
    static void castBolt(ServerLevel level, Mob caster, ServerPlayer target, float damage, double speed) {
        if (caster instanceof PriestessEntity priestess) {
            priestess.setAction(CAST);
        } else if (caster instanceof PriestessReflectionEntity reflection) {
            reflection.setAction(CAST);
        }
        Vec3 from = caster.position().add(0.0, 2.0, 0.0);
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 velocity = to.subtract(from).normalize().scale(speed);
        MagusSpellEntity bolt = MagusSpellEntity.create(level, MagusSpellEntity.Kind.WATER_BOLT, caster, from, velocity, damage);
        bolt.life(60);
        level.addFreshEntity(bolt);
        level.playSound(null, caster.blockPosition(), SoundEvents.DROWNED_SHOOT, SoundSource.HOSTILE, 1.0F, 1.4F);
    }

    /** Um reflexo foi estourado: contra-ataque rápido em volta e na direção de quem bateu. */
    void onReflectionShattered(ServerLevel level, PriestessReflectionEntity reflection, @Nullable ServerPlayer attacker) {
        this.reflections.remove(reflection.getUUID());
        Vec3 p = reflection.position();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
                p.x, p.y + 1.5, p.z, 60, 0.4, 0.9, 0.4, 0.2);
        level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 1.0, p.z, 40, 0.8, 0.6, 0.8, 0.1);
        level.playSound(null, reflection.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.8F);
        level.playSound(null, reflection.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.5F, 1.2F);
        for (ServerPlayer player : this.players(level)) {
            if (player.distanceToSqr(p) < 3.5 * 3.5) {
                if (player.hurtServer(level, this.damageSources().indirectMagic(reflection, this), SHATTER_DAMAGE)) {
                    Vec3 away = player.position().subtract(p).multiply(1.0, 0.0, 1.0);
                    push(player, away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.9), 0.35);
                }
            }
        }
        if (attacker != null) {
            attacker.sendOverlayMessage(Component.translatable("message.mistech.priestess.reflection").withStyle(ChatFormatting.GRAY));
            for (int i = 0; i < 3; i++) {
                Vec3 from = p.add(0.0, 2.0, 0.0);
                Vec3 to = attacker.position().add(0.0, 1.0, 0.0);
                Vec3 velocity = to.subtract(from).normalize().yRot((i - 1) * 0.25F).scale(0.8);
                MagusSpellEntity bolt = MagusSpellEntity.create(level, MagusSpellEntity.Kind.ARCANE_BOLT, this, from, velocity, BOLT_DAMAGE)
                        .target(attacker);
                bolt.life(60);
                level.addFreshEntity(bolt);
            }
        }
    }

    private void clearReflections(ServerLevel level) {
        for (PriestessReflectionEntity reflection : this.reflections(level)) {
            level.sendParticles(ParticleTypes.POOF, reflection.getX(), reflection.getY() + 1.5, reflection.getZ(), 10, 0.3, 0.8, 0.3, 0.02);
            reflection.discard();
        }
        this.reflections.clear();
    }

    static void push(LivingEntity e, Vec3 horizontal, double up) {
        e.setDeltaMovement(horizontal.x, up, horizontal.z);
        e.hurtMarked = true;
        if (e instanceof ServerPlayer sp) {
            sp.resetFallDistance();
        }
    }

    // ------------------------------------------------------------------ dano

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.DROWN)) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof PriestessFigure) {
            return false;
        }
        if (this.mode() == BOAZ) {
            // velada: nada a fere enquanto está no trono
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.5F, 0.5F);
            level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.5, this.getZ(), 6, 0.3, 0.6, 0.3, 0.02);
            if (attacker instanceof ServerPlayer player) {
                long now = level.getGameTime();
                if (now - this.lastVeiledMessage.getOrDefault(player.getUUID(), -100L) > 40) {
                    this.lastVeiledMessage.put(player.getUUID(), now);
                    player.sendOverlayMessage(Component.translatable("message.mistech.priestess.veiled").withStyle(ChatFormatting.DARK_AQUA));
                }
            }
            return false;
        }
        if (attacker instanceof ServerPlayer player) {
            int stacks = this.intuition(player);
            if (stacks > 0) {
                damage *= 1.0F + INTUITION_BONUS * stacks;
                level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.8, this.getZ(), 3 + stacks, 0.3, 0.6, 0.3, 0.05);
            }
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            this.hitsSinceShuffle++;
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            this.clearReflections(level);
            for (ServerPlayer player : this.players(level)) {
                player.removeEffect(MobEffects.BLINDNESS);
            }
            if (this.arenaCenter != null) {
                PriestessArena.setMode(level, this.arenaCenter, false);
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (this.level() instanceof ServerLevel level) {
            this.clearReflections(level);
        }
        super.remove(reason);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
            // gotas e brilho lunar em volta
            this.level().addParticle(this.seated() ? ParticleTypes.DRIPPING_WATER : ParticleTypes.END_ROD,
                    this.getRandomX(0.9), this.getY() + 0.5 + this.random.nextDouble() * 2.5, this.getRandomZ(0.9), 0.0, -0.01, 0.0);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }
}
