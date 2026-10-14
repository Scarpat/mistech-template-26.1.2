package com.mistech.mistechmod.celestial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.purity.block.TaintedSoilBlock;
import com.mistech.mistechmod.registry.OreContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * O que cada Evento Celeste faz no mundo, mais o Chamariz do Ermo (monstros extras em volta de um altar) e a Vigília
 * (chunks carregados em volta de um altar).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class CelestialEffects {
    public static final String BLOOD_TAG = "mistech_blood_moon";
    public static final String LURED_TAG = "mistech_lured";
    public static final Identifier BLOOD_HEALTH = Identifier.fromNamespaceAndPath(Mistech.MODID, "blood_moon_health");
    public static final Identifier BLOOD_DAMAGE = Identifier.fromNamespaceAndPath(Mistech.MODID, "blood_moon_damage");
    public static final float BLOOD_BUFF = 0.5F;
    public static final float LUNAR_BLOOD_CHANCE = 0.35F;
    public static final int LURE_RADIUS = 48;
    public static final int MAX_NEARBY_MONSTERS = 40;
    public static final int VIGIL_CHUNK_RADIUS = 1;

    private static final List<Meteor> METEORS = new ArrayList<>();

    private CelestialEffects() {}

    static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        long gameTime = overworld.getGameTime();
        tickMeteors(overworld);
        if (gameTime % 20 == 0) {
            naturalBloodMoon(server, overworld);
            for (ServerPlayer player : overworld.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                perPlayerSecond(overworld, player);
            }
        }
        if (gameTime % 100 == 37) {
            for (ServerPlayer player : overworld.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                if (Celestial.isActive(overworld, CelestialEvent.BLOOD_MOON)) {
                    for (int i = 0; i < 2; i++) {
                        spawnMonster(overworld, player, false, overworld.getRandom().nextInt(8) == 0
                                ? com.mistech.mistechmod.folklore.FolkloreContent.WEREWOLF.get() : null);
                    }
                } else if (canFullMoonWerewolfSpawn(overworld, player)) {
                    // lua cheia: raramente um lobisomem, e só rondando vilas
                    spawnMonster(overworld, player, false, com.mistech.mistechmod.folklore.FolkloreContent.WEREWOLF.get());
                }
            }
            tickLures(server, gameTime);
            tickVigils(server, gameTime);
        }
    }

    /** Chance (1 em N, a cada 5 s por jogador) de um lobisomem na lua cheia fora da Lua de Sangue. */
    public static final int FULL_MOON_WEREWOLF_ODDS = 150;
    /** Distância (em seções de 16 blocos) da vila em que o lobisomem da lua cheia pode aparecer. */
    public static final int WEREWOLF_VILLAGE_SECTIONS = 3;
    /** Raio sem outro lobisomem para nascer um novo na lua cheia. */
    public static final double WEREWOLF_SPACING = 96.0;

    /**
     * Fora da Lua de Sangue o lobisomem só aparece em noite de lua cheia, perto de vilas, bem raramente e um de cada vez
     * (nunca se já houver outro por perto).
     */
    private static boolean canFullMoonWerewolfSpawn(ServerLevel overworld, ServerPlayer player) {
        if (!overworld.isDarkOutside() || overworld.getRandom().nextInt(FULL_MOON_WEREWOLF_ODDS) != 0
                || overworld.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, player.blockPosition())
                        != net.minecraft.world.level.MoonPhase.FULL_MOON
                || !overworld.isCloseToVillage(player.blockPosition(), WEREWOLF_VILLAGE_SECTIONS)) {
            return false;
        }
        return overworld.getEntitiesOfClass(com.mistech.mistechmod.folklore.WerewolfEntity.class,
                player.getBoundingBox().inflate(WEREWOLF_SPACING), net.minecraft.world.entity.Entity::isAlive).isEmpty();
    }

    private static void perPlayerSecond(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        // Lua de Prata: mortos-vivos a céu aberto pegam fogo sob a lua
        if (Celestial.isActive(level, CelestialEvent.SILVER_MOON)) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48, 24, 48),
                    m -> m.is(EntityTypeTags.UNDEAD) && !m.isInWaterOrRain() && level.canSeeSky(BlockPos.containing(m.getEyePosition())))) {
                if (random.nextInt(6) == 0) {
                    mob.igniteForSeconds(4.0F);
                    level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1.0, mob.getZ(), 4, 0.2, 0.4, 0.2, 0.02);
                }
            }
        }
        // Aurora: Sorte e Regeneração a céu aberto
        if (Celestial.isActive(level, CelestialEvent.AURORA) && level.canSeeSky(eye)) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 7 * 20, 0, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 7 * 20, 0, true, true));
            if (random.nextInt(3) == 0) {
                level.sendParticles(player, ParticleTypes.GLOW, true, false, player.getX() + random.nextGaussian() * 8, player.getY() + 12 + random.nextDouble() * 6,
                        player.getZ() + random.nextGaussian() * 8, 6, 2.0, 0.5, 2.0, 0.0);
            }
        }
        // Solstício: o sol limpa o triplo e as plantas em volta crescem mais
        if (Celestial.isActive(level, CelestialEvent.SOLSTICE)) {
            if (Taint.inSunlight(level, eye)) {
                Taint.cleanse(player, 0.04F); // + 0,02 normal = 3×
            }
            for (int i = 0; i < 10; i++) {
                BlockPos p = player.blockPosition().offset(random.nextInt(25) - 12, random.nextInt(9) - 4, random.nextInt(25) - 12);
                BlockState state = level.getBlockState(p);
                if (state.getBlock() instanceof CropBlock || (state.getBlock() instanceof BonemealableBlock && state.isRandomlyTicking())) {
                    state.randomTick(level, p, random);
                }
            }
        }
        // Chuva Bendita: a chuva lava a Mácula (até zero) e o Solo Maculado
        if (Celestial.isActive(level, CelestialEvent.BLESSED_RAIN)) {
            if (level.isRainingAt(eye)) {
                Taint.purify(player, 0.2F);
            }
            for (int i = 0; i < 6; i++) {
                int x = player.getBlockX() + random.nextInt(49) - 24;
                int z = player.getBlockZ() + random.nextInt(49) - 24;
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
                if (level.getBlockState(top).is(PurityContent.TAINTED_SOIL.get())) {
                    TaintedSoilBlock.cleanse(level, top);
                    level.sendParticles(ParticleTypes.END_ROD, top.getX() + 0.5, top.getY() + 1.1, top.getZ() + 0.5, 5, 0.3, 0.1, 0.3, 0.01);
                }
            }
        }
        // Chuva de Estrelas: de vez em quando cai um meteoro por perto
        if (Celestial.isActive(level, CelestialEvent.STARFALL) && random.nextInt(8) == 0) {
            launchMeteor(level, player);
        }
    }

    // ----- Lua de Sangue natural -----

    public static final float NATURAL_BLOOD_BASE = 0.04F;
    public static final float NATURAL_BLOOD_TAINT = 0.26F;
    private static long lastNaturalDay = Long.MIN_VALUE;

    /** No começo de cada noite de lua cheia: 4% de chance de Lua de Sangue, até 30% se alguém estiver Profano. */
    private static void naturalBloodMoon(MinecraftServer server, ServerLevel overworld) {
        long t = Celestial.now(server);
        int tod = Celestial.timeOfDay(t);
        long day = Math.floorDiv(t, Celestial.DAY);
        if (tod < 12500 || tod >= 13500 || day == lastNaturalDay) {
            return;
        }
        lastNaturalDay = day;
        if (overworld.players().isEmpty() || Celestial.isScheduled(server, CelestialEvent.BLOOD_MOON)
                || Celestial.isScheduled(server, CelestialEvent.SILVER_MOON)
                || overworld.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE,
                        overworld.getRespawnData().pos()) != net.minecraft.world.level.MoonPhase.FULL_MOON) {
            return;
        }
        float worst = 0.0F;
        for (ServerPlayer player : overworld.players()) {
            worst = Math.max(worst, Taint.get(player));
        }
        float chance = NATURAL_BLOOD_BASE + NATURAL_BLOOD_TAINT * (worst / Taint.MAX);
        if (overworld.getRandom().nextFloat() < chance) {
            Celestial.start(server, CelestialEvent.BLOOD_MOON, Celestial.tonight(t));
            net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.translatable("message.mistech.celestial.start.blood_moon")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED);
            for (ServerPlayer player : overworld.players()) {
                player.sendSystemMessage(msg);
                overworld.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.5F, 0.6F);
            }
        }
    }

    // ----- monstros extras (Lua de Sangue e Chamariz) -----

    private static final List<EntityType<? extends Mob>> WILD = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
            EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH);

    private static void spawnMonster(ServerLevel level, Player near, boolean lured) {
        spawnMonster(level, near, lured, null);
    }

    private static void spawnMonster(ServerLevel level, Player near, boolean lured, @org.jspecify.annotations.Nullable EntityType<? extends Mob> forced) {
        RandomSource random = level.getRandom();
        if (level.getEntitiesOfClass(Monster.class, near.getBoundingBox().inflate(48, 24, 48)).size() >= MAX_NEARBY_MONSTERS) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 16 + random.nextDouble() * 16;
        int x = (int) Math.floor(near.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(near.getZ() + Math.sin(angle) * dist);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return;
        }
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (Math.abs(pos.getY() - near.getY()) > 24) {
            return;
        }
        if (forced == com.mistech.mistechmod.folklore.FolkloreContent.WEREWOLF.get()
                && com.mistech.mistechmod.folklore.SilverIncenseItem.isWarded(level, near, pos)) {
            // fumaça de Incenso de Prata: nada de lobisomem (na Lua de Sangue vem um monstro comum no lugar)
            if (!Celestial.isActive(level, CelestialEvent.BLOOD_MOON)) {
                return;
            }
            forced = null;
        }
        @SuppressWarnings("unchecked")
        EntityType<Mob> type = (EntityType<Mob>) (forced != null ? forced : WILD.get(random.nextInt(WILD.size())));
        if (!SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)
                || !level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
            return;
        }
        Mob mob = type.create(level, EntitySpawnReason.NATURAL);
        if (mob == null) {
            return;
        }
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        if (lured) {
            mob.getPersistentData().putBoolean(LURED_TAG, true);
        }
        level.addFreshEntity(mob);
    }

    /** Monstros que nascem na Lua de Sangue ficam mais fortes. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof Monster mob) || !(event.getLevel() instanceof ServerLevel level)
                || !Celestial.isActive(level, CelestialEvent.BLOOD_MOON) || mob.getPersistentData().getBooleanOr(BLOOD_TAG, false)) {
            return;
        }
        mob.getPersistentData().putBoolean(BLOOD_TAG, true);
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && !health.hasModifier(BLOOD_HEALTH)) {
            health.addPermanentModifier(new AttributeModifier(BLOOD_HEALTH, BLOOD_BUFF, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && !damage.hasModifier(BLOOD_DAMAGE)) {
            damage.addPermanentModifier(new AttributeModifier(BLOOD_DAMAGE, BLOOD_BUFF, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Sangue Lunar dos monstros da Lua de Sangue; o Chamariz dobra parte dos drops. */
    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        var entity = event.getEntity();
        RandomSource random = level.getRandom();
        if (entity.getPersistentData().getBooleanOr(BLOOD_TAG, false) && random.nextFloat() < LUNAR_BLOOD_CHANCE) {
            event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(CelestialContent.LUNAR_BLOOD.get())));
        }
        if (entity instanceof Enemy && isLured(level, entity.blockPosition(), entity.getPersistentData().getBooleanOr(LURED_TAG, false))) {
            List<ItemEntity> extra = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                if (random.nextFloat() < 0.3F) {
                    extra.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
                }
            }
            event.getDrops().addAll(extra);
        }
    }

    @SubscribeEvent
    static void onExperience(LivingExperienceDropEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || event.getAttackingPlayer() == null) {
            return;
        }
        if (entity instanceof Enemy && isLured(level, entity.blockPosition(), entity.getPersistentData().getBooleanOr(LURED_TAG, false))) {
            event.setDroppedExperience(event.getDroppedExperience() * 2);
        } else if (entity.getPersistentData().getBooleanOr(BLOOD_TAG, false)) {
            event.setDroppedExperience(Math.round(event.getDroppedExperience() * 1.5F));
        }
    }

    /** Lua de Prata: metade dos monstros nem chega a nascer. */
    @SubscribeEvent
    static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity() instanceof Enemy && event.getSpawnType() == EntitySpawnReason.NATURAL
                && event.getEntity().level() instanceof ServerLevel level && Celestial.isActive(level, CelestialEvent.SILVER_MOON)
                && level.getRandom().nextBoolean()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    // ----- Chamariz do Ermo -----

    public static void addLure(ServerLevel level, BlockPos altar, long ticks) {
        CelestialData data = CelestialData.get(level.getServer());
        data.lures.removeIf(z -> z.pos().equals(altar) && z.dimension().equals(level.dimension().identifier().toString()));
        data.lures.add(new CelestialData.Zone(level.dimension().identifier().toString(), altar.immutable(), level.getGameTime() + ticks));
        data.setDirty();
    }

    private static boolean isLured(ServerLevel level, BlockPos pos, boolean tagged) {
        if (tagged) {
            return true;
        }
        String dim = level.dimension().identifier().toString();
        long now = level.getGameTime();
        for (CelestialData.Zone z : CelestialData.get(level.getServer()).lures) {
            if (z.until() > now && z.dimension().equals(dim) && z.pos().distSqr(pos) <= (double) LURE_RADIUS * LURE_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static void tickLures(MinecraftServer server, long overworldTime) {
        CelestialData data = CelestialData.get(server);
        if (data.lures.isEmpty()) {
            return;
        }
        for (Iterator<CelestialData.Zone> it = data.lures.iterator(); it.hasNext();) {
            CelestialData.Zone zone = it.next();
            ServerLevel level = levelOf(server, zone.dimension());
            if (level == null || zone.until() <= level.getGameTime()) {
                it.remove();
                data.setDirty();
                continue;
            }
            if (!level.isDarkOutside() && level.dimension() == Level.OVERWORLD) {
                continue; // de dia o chamariz só espera a noite
            }
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && player.blockPosition().distSqr(zone.pos()) <= (double) LURE_RADIUS * LURE_RADIUS) {
                    spawnMonster(level, player, true);
                    spawnMonster(level, player, true);
                }
            }
        }
    }

    // ----- Vigília -----

    public static void addVigil(ServerLevel level, BlockPos altar, long ticks) {
        CelestialData data = CelestialData.get(level.getServer());
        String dim = level.dimension().identifier().toString();
        data.vigils.removeIf(z -> z.pos().equals(altar) && z.dimension().equals(dim));
        data.vigils.add(new CelestialData.Zone(dim, altar.immutable(), level.getGameTime() + ticks));
        data.setDirty();
        forceAround(level, altar, true);
    }

    private static void forceAround(ServerLevel level, BlockPos altar, boolean forced) {
        int cx = altar.getX() >> 4;
        int cz = altar.getZ() >> 4;
        for (int dx = -VIGIL_CHUNK_RADIUS; dx <= VIGIL_CHUNK_RADIUS; dx++) {
            for (int dz = -VIGIL_CHUNK_RADIUS; dz <= VIGIL_CHUNK_RADIUS; dz++) {
                level.setChunkForced(cx + dx, cz + dz, forced);
            }
        }
    }

    private static void tickVigils(MinecraftServer server, long overworldTime) {
        CelestialData data = CelestialData.get(server);
        for (Iterator<CelestialData.Zone> it = data.vigils.iterator(); it.hasNext();) {
            CelestialData.Zone zone = it.next();
            ServerLevel level = levelOf(server, zone.dimension());
            if (level == null) {
                it.remove();
                data.setDirty();
            } else if (zone.until() <= level.getGameTime()) {
                forceAround(level, zone.pos(), false);
                it.remove();
                data.setDirty();
            }
        }
    }

    private static ServerLevel levelOf(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    // ----- meteoros (Chuva de Estrelas) -----

    private static final class Meteor {
        Vec3 pos;
        final Vec3 velocity;
        int age;

        Meteor(Vec3 pos, Vec3 velocity) {
            this.pos = pos;
            this.velocity = velocity;
        }
    }

    public static void launchMeteor(ServerLevel level, Player near) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 12 + random.nextDouble() * 36;
        double tx = near.getX() + Math.cos(angle) * dist;
        double tz = near.getZ() + Math.sin(angle) * dist;
        Vec3 start = new Vec3(tx - 30 + random.nextDouble() * 60, near.getY() + 90, tz - 30 + random.nextDouble() * 60);
        Vec3 target = new Vec3(tx, near.getY(), tz);
        Vec3 velocity = target.subtract(start).normalize().scale(1.6);
        METEORS.add(new Meteor(start, velocity));
        level.playSound(null, BlockPos.containing(target), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 2.0F, 0.5F);
    }

    private static void tickMeteors(ServerLevel level) {
        if (METEORS.isEmpty()) {
            return;
        }
        for (Iterator<Meteor> it = METEORS.iterator(); it.hasNext();) {
            Meteor m = it.next();
            m.age++;
            Vec3 next = m.pos.add(m.velocity);
            BlockPos block = BlockPos.containing(next);
            if (m.age > 200 || !level.hasChunkAt(block)) {
                it.remove();
                continue;
            }
            level.sendParticles(ParticleTypes.END_ROD, true, true, next.x, next.y, next.z, 2, 0.1, 0.1, 0.1, 0.01);
            level.sendParticles(ParticleTypes.FLAME, true, true, next.x, next.y, next.z, 3, 0.15, 0.15, 0.15, 0.02);
            if (m.age % 2 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, true, true, m.pos.x, m.pos.y, m.pos.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
            if (!level.getBlockState(block).isAir() || next.y <= level.getMinY()) {
                impact(level, m.pos);
                it.remove();
                continue;
            }
            m.pos = next;
        }
    }

    private static void impact(ServerLevel level, Vec3 at) {
        RandomSource random = level.getRandom();
        level.explode(null, at.x, at.y, at.z, 1.5F, false, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 40, 0.6, 0.6, 0.6, 0.15);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 10, 0.5, 0.3, 0.5, 0.0);
        level.playSound(null, BlockPos.containing(at), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.AMBIENT, 3.0F, 0.6F);
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(CelestialContent.STAR_FRAGMENT.get()));
        if (random.nextFloat() < 0.35F) {
            loot.add(new ItemStack(OreContent.RAW_ADAMANTINE.get()));
        }
        if (random.nextFloat() < 0.35F) {
            loot.add(new ItemStack(OreContent.RAW_ORICHALCUM.get()));
        }
        if (random.nextFloat() < 0.5F) {
            loot.add(new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(5)));
        }
        if (random.nextFloat() < 0.25F) {
            loot.add(new ItemStack(Items.GLOWSTONE_DUST, 1 + random.nextInt(3)));
        }
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
            item.setDeltaMovement(random.nextGaussian() * 0.1, 0.3, random.nextGaussian() * 0.1);
            item.setGlowingTag(true);
            level.addFreshEntity(item);
        }
    }
}
