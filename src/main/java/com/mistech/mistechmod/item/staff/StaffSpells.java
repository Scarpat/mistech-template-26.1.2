package com.mistech.mistechmod.item.staff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * As magias do Cajado do Gnomo, uma por runa. Fogo e Tempo são canalizadas (gastam energia enquanto o botão
 * estiver apertado); as outras carregam por {@link #CHARGE_TICKS} e saem ao soltar o botão.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class StaffSpells {
    /** Tempo mínimo segurando para as magias de soltar (0,5 s). */
    public static final int CHARGE_TICKS = 10;

    // custo em FE
    public static final int FIRE_PER_TICK = 25;
    public static final int TIME_PER_TICK = 30;
    public static final int WATER_COST = 1_500;
    public static final int AIR_COST = 1_500;
    public static final int EARTH_PER_BLOCK = 40;
    public static final int ENERGY_COST = 4_000;
    public static final int ICE_COST = 2_500;
    public static final int LIGHT_COST = 2_500;
    public static final int DARKNESS_COST = 3_000;
    public static final int SPACE_COST = 2_000;
    public static final int MAGMA_COST = 3_000;
    public static final int VAPOR_COST = 2_500;
    public static final int VOID_COST = 4_000;
    public static final int LUCK_COST = 3_000;
    public static final int ARCANE_COST = 2_500;
    public static final int ECHO_COST = 4_000;
    public static final int VIGIL_COST = 2_500;
    public static final int SOUL_COST = 3_000;
    public static final int CLOUD_COST = 2_000;
    /** Comprimento da ponte de nuvens do Cajado. */
    public static final int CLOUD_BRIDGE = 12;

    public static final double FIRE_RANGE = 7.0;
    public static final int EARTH_LENGTH = 10;
    public static final double LIGHTNING_RANGE = 48.0;
    public static final double ICE_RADIUS = 7.0;
    public static final double LIGHT_RADIUS = 8.0;
    public static final double DARKNESS_RANGE = 24.0;
    public static final double DARKNESS_RADIUS = 4.0;
    public static final int DARKNESS_TICKS = 160;
    public static final double SPACE_RANGE = 16.0;
    public static final double TIME_RANGE = 6.0;
    public static final double MAGMA_RANGE = 24.0;
    public static final double MAGMA_RADIUS = 3.5;
    public static final double VAPOR_RADIUS = 6.0;
    public static final double VOID_RANGE = 24.0;
    public static final double VOID_PULL_RADIUS = 7.0;
    public static final int VOID_TICKS = 100;
    public static final double LUCK_RANGE = 24.0;
    public static final double LUCK_RADIUS = 6.0;
    public static final double ARCANE_RANGE = 32.0;
    public static final double ARCANE_CHAIN_RADIUS = 6.0;
    public static final int ARCANE_CHAINS = 3;
    public static final double ECHO_RANGE = 24.0;
    public static final float ECHO_DAMAGE = 14.0F;
    public static final double VIGIL_RANGE = 12.0;
    public static final int VIGIL_TICKS = 80;
    public static final double SOUL_RANGE = 8.0;
    /** Ticks extras por tick para block entities (fornalhas, etc.) sob a magia do Tempo. */
    public static final int TIME_SPEEDUP = 4;

    private static final String WIND_TAG = "mistech_wind_until";

    private StaffSpells() {}

    public static boolean isChanneled(RuneElement element) {
        return element == RuneElement.FIRE || element == RuneElement.TIME;
    }

    /** Energia mínima para começar a conjurar. */
    public static int minEnergy(RuneElement element) {
        return switch (element) {
            case FIRE -> FIRE_PER_TICK;
            case TIME -> TIME_PER_TICK;
            case WATER -> WATER_COST;
            case AIR -> AIR_COST;
            case EARTH -> EARTH_PER_BLOCK;
            case ENERGY -> ENERGY_COST;
            case ICE -> ICE_COST;
            case LIGHT -> LIGHT_COST;
            case DARKNESS -> DARKNESS_COST;
            case SPACE -> SPACE_COST;
            case MAGMA -> MAGMA_COST;
            case VAPOR -> VAPOR_COST;
            case VOID -> VOID_COST;
            case LUCK -> LUCK_COST;
            case ARCANE -> ARCANE_COST;
            case ECHO -> ECHO_COST;
            case VIGIL -> VIGIL_COST;
            case SOUL -> SOUL_COST;
            case CLOUD -> CLOUD_COST;
            case LIFE -> com.mistech.mistechmod.life.LifeEvents.FOUNTAIN_COST;
        };
    }

    // ================= canalizadas =================

    public static void channel(Level level, Player player, ItemStack stack, RuneElement element, int held) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // partículas da cor da runa saindo do cajado enquanto carrega
        if (level.isClientSide()) {
            if (!isChanneled(element) && held <= CHARGE_TICKS) {
                Vec3 tip = eye.add(look.scale(0.8)).add(0, -0.3, 0);
                level.addParticle(ParticleTypes.ENCHANT, tip.x, tip.y, tip.z, (Math.random() - 0.5) * 0.4, 0.1, (Math.random() - 0.5) * 0.4);
            }
            return;
        }
        ServerLevel server = (ServerLevel) level;
        if (element == RuneElement.FIRE) {
            if (!GnomeStaffItem.tryUse(player, stack, FIRE_PER_TICK)) {
                player.stopUsingItem();
                return;
            }
            fireBreath(server, player, eye, look, held);
        } else if (element == RuneElement.TIME) {
            if (!GnomeStaffItem.tryUse(player, stack, TIME_PER_TICK)) {
                player.stopUsingItem();
                return;
            }
            accelerate(server, player, held);
        } else if (held == CHARGE_TICKS) {
            // pronto para soltar
            server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.8F);
        }
    }

    /** Fogo: bafo de fogo num cone à frente; incendeia e machuca. */
    private static void fireBreath(ServerLevel level, Player player, Vec3 eye, Vec3 look, int held) {
        Vec3 start = eye.add(look.scale(0.6)).add(0, -0.2, 0);
        for (int i = 0; i < 4; i++) {
            Vec3 dir = look.add(level.getRandom().nextGaussian() * 0.08, level.getRandom().nextGaussian() * 0.08, level.getRandom().nextGaussian() * 0.08);
            // count 0 = a "velocidade" vira direção
            level.sendParticles(i == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME, start.x, start.y, start.z, 0, dir.x, dir.y, dir.z, 0.45);
        }
        if (held % 6 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 0.7F + level.getRandom().nextFloat() * 0.3F);
        }
        AABB area = player.getBoundingBox().expandTowards(look.scale(FIRE_RANGE)).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist <= FIRE_RANGE && to.normalize().dot(look) > 0.8) {
                target.igniteForSeconds(4.0F);
                if (held % 10 == 0) {
                    target.hurtServer(level, level.damageSources().playerAttack(player), 3.0F);
                }
            }
        }
    }

    /** Tempo: o bloco mirado recebe ticks extras (plantas crescem, fornalhas aceleram...). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void accelerate(ServerLevel level, Player player, int held) {
        HitResult hit = player.pick(TIME_RANGE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isRandomlyTicking() && held % 4 == 0) {
            state.randomTick(level, pos, level.getRandom());
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            BlockEntityTicker ticker = state.getTicker(level, be.getType());
            if (ticker != null) {
                for (int i = 0; i < TIME_SPEEDUP; i++) {
                    ticker.tick(level, pos, level.getBlockState(pos), be);
                }
            }
        }
        if (held % 5 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.35, 0.2, 0.35, 0.0);
        }
        if (held % 20 == 0) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 0.6F);
        }
    }

    // ================= soltar =================

    public static boolean release(Level level, Player player, ItemStack stack, RuneElement element) {
        return switch (element) {
            case WATER -> water(level, player, stack);
            case AIR -> air(level, player, stack);
            default -> {
                if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
                    boolean cast = switch (element) {
                        case EARTH -> earth(server, serverPlayer, stack);
                        case ENERGY -> energy(server, serverPlayer, stack);
                        case ICE -> ice(server, serverPlayer, stack);
                        case LIGHT -> light(server, serverPlayer, stack);
                        case DARKNESS -> darkness(server, serverPlayer, stack);
                        case SPACE -> space(server, serverPlayer, stack);
                        case MAGMA -> magma(server, serverPlayer, stack);
                        case VAPOR -> vapor(server, serverPlayer, stack);
                        case VOID -> voidRift(server, serverPlayer, stack);
                        case LUCK -> luck(server, serverPlayer, stack);
                        case ARCANE -> arcane(server, serverPlayer, stack);
                        case ECHO -> echo(server, serverPlayer, stack);
                        case VIGIL -> vigil(server, serverPlayer, stack);
                        case SOUL -> soul(server, serverPlayer, stack);
                        case CLOUD -> cloudBridge(server, serverPlayer, stack);
                        case LIFE -> com.mistech.mistechmod.life.LifeEvents.fountain(server, serverPlayer, stack);
                        default -> false;
                    };
                    if (cast) {
                        player.getCooldowns().addCooldown(stack, element == RuneElement.LIFE ? 160 : element == RuneElement.ECHO ? 80
                                : element == RuneElement.ENERGY || element == RuneElement.VOID || element == RuneElement.LUCK ? 60 : 20);
                    }
                    yield cast;
                }
                yield true;
            }
        };
    }

    /** Água: na água ou na chuva, lança o jogador como o tridente com Correnteza. Roda nos dois lados (o cliente mexe o jogador). */
    private static boolean water(Level level, Player player, ItemStack stack) {
        if (!player.isInWaterOrRain() || player.isPassenger()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("item.mistech.gnome_staff.needs_water"));
            }
            return false;
        }
        if (!GnomeStaffItem.hasEnergy(player, stack, WATER_COST)) {
            return false;
        }
        Vec3 push = player.getLookAngle().scale(3.0);
        player.push(push.x, push.y, push.z);
        player.startAutoSpinAttack(20, 8.0F, stack);
        if (player.onGround()) {
            player.move(MoverType.SELF, new Vec3(0.0, 1.2, 0.0));
        }
        if (!level.isClientSide()) {
            GnomeStaffItem.tryUse(player, stack, WATER_COST);
            level.playSound(null, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(stack, 20);
        }
        return true;
    }

    /** Vento: impulso forte para o alto e para frente; a próxima queda não machuca. */
    private static boolean air(Level level, Player player, ItemStack stack) {
        if (!GnomeStaffItem.hasEnergy(player, stack, AIR_COST)) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        player.push(look.x * 1.4, 1.3 + Math.max(0.0, look.y), look.z * 1.4);
        player.resetFallDistance();
        if (level instanceof ServerLevel server) {
            GnomeStaffItem.tryUse(player, stack, AIR_COST);
            player.getPersistentData().putLong(WIND_TAG, server.getGameTime() + 600);
            server.sendParticles(ParticleTypes.GUST, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.5, 0.1, 0.5, 0.05);
            server.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(stack, 20);
        }
        return true;
    }

    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && player.level() instanceof ServerLevel level
                && player.getPersistentData().contains(WIND_TAG)) {
            long until = player.getPersistentData().getLongOr(WIND_TAG, 0L);
            player.getPersistentData().remove(WIND_TAG);
            if (level.getGameTime() <= until) {
                event.setDamageMultiplier(0.0F);
            }
        }
    }

    /** Terra: túnel 3x3 na direção do olhar, quebrando tudo que uma picareta de ferro consegue. */
    private static boolean earth(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Direction dir = Direction.getApproximateNearest(player.getLookAngle());
        Direction.Axis axis = dir.getAxis();
        Direction side1 = axis == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction side2 = axis == Direction.Axis.Z ? Direction.EAST : axis == Direction.Axis.X ? Direction.SOUTH : Direction.SOUTH;
        BlockPos origin = BlockPos.containing(player.getEyePosition());
        int broken = 0;
        for (int i = 1; i <= EARTH_LENGTH; i++) {
            BlockPos center = origin.relative(dir, i);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos pos = center.relative(side1, a).relative(side2, b);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                        continue;
                    }
                    if (state.getDestroySpeed(level, pos) < 0 || state.is(BlockTags.INCORRECT_FOR_IRON_TOOL)
                            || level.getBlockEntity(pos) != null || !level.mayInteract(player, pos)) {
                        continue;
                    }
                    if (!GnomeStaffItem.tryUse(player, stack, EARTH_PER_BLOCK)) {
                        return broken > 0;
                    }
                    level.destroyBlock(pos, true, player);
                    broken++;
                }
            }
        }
        if (broken > 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        return broken > 0;
    }

    /** Energia: vários raios em volta do ponto mirado. */
    private static boolean energy(ServerLevel level, ServerPlayer player, ItemStack stack) {
        HitResult hit = player.pick(LIGHTNING_RANGE, 0.0F, false);
        if (hit.getType() == HitResult.Type.MISS || !GnomeStaffItem.tryUse(player, stack, ENERGY_COST)) {
            return false;
        }
        Vec3 center = hit.getLocation();
        for (int i = 0; i < 5; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                double ox = i == 0 ? 0 : (level.getRandom().nextDouble() - 0.5) * 7.0;
                double oz = i == 0 ? 0 : (level.getRandom().nextDouble() - 0.5) * 7.0;
                BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        BlockPos.containing(center.x + ox, center.y, center.z + oz));
                double y = Math.abs(ground.getY() - center.y) < 6 ? ground.getY() : center.y;
                bolt.snapTo(center.x + ox, y, center.z + oz);
                bolt.setCause(player);
                level.addFreshEntity(bolt);
            }
        }
        return true;
    }

    /** Gelo: congela as criaturas em volta e a água vira gelo. */
    private static boolean ice(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!GnomeStaffItem.tryUse(player, stack, ICE_COST)) {
            return false;
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ICE_RADIUS),
                e -> e != player && e.isAlive() && e.distanceTo(player) <= ICE_RADIUS)) {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 200));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 4), player);
            target.hurtServer(level, level.damageSources().freeze(), 3.0F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    12, 0.3, 0.5, 0.3, 0.02);
        }
        int r = (int) ICE_RADIUS;
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -3, -r), center.offset(r, 2, r))) {
            if (pos.distSqr(center) <= r * r && level.getFluidState(pos).is(Fluids.WATER)
                    && level.getBlockState(pos).is(Blocks.WATER) && level.mayInteract(player, pos)) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 80, ICE_RADIUS / 2, 0.5, ICE_RADIUS / 2, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    /** Luz: explosão de luz que machuca as criaturas em volta (mortos-vivos sofrem mais) e as deixa brilhando. */
    private static boolean light(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!GnomeStaffItem.tryUse(player, stack, LIGHT_COST)) {
            return false;
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(LIGHT_RADIUS),
                e -> e != player && e.isAlive() && e.distanceTo(player) <= LIGHT_RADIUS && !(e instanceof Player))) {
            boolean undead = target.is(net.minecraft.tags.EntityTypeTags.UNDEAD);
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), undead ? 12.0F : 7.0F);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), player);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), player);
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 120, 0.2, 0.2, 0.2, 0.45);
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1, player.getZ(), 40, LIGHT_RADIUS / 2, 1.0, LIGHT_RADIUS / 2, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
        return true;
    }

    // ----- Escuridão: área que prende e dá Wither -----

    private record DarkZone(ResourceKey<Level> dimension, Vec3 center, java.util.UUID caster, int[] ticksLeft) {}

    private static final List<DarkZone> DARK_ZONES = new ArrayList<>();

    private static boolean darkness(ServerLevel level, ServerPlayer player, ItemStack stack) {
        HitResult hit = player.pick(DARKNESS_RANGE, 0.0F, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(8.0)) : hit.getLocation();
        if (!GnomeStaffItem.tryUse(player, stack, DARKNESS_COST)) {
            return false;
        }
        DARK_ZONES.add(new DarkZone(level.dimension(), center, player.getUUID(), new int[] {DARKNESS_TICKS}));
        level.playSound(null, BlockPos.containing(center), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        tickVoidRifts(event);
        Iterator<DarkZone> it = DARK_ZONES.iterator();
        while (it.hasNext()) {
            DarkZone zone = it.next();
            ServerLevel level = event.getServer().getLevel(zone.dimension());
            if (level == null || --zone.ticksLeft()[0] <= 0) {
                it.remove();
                continue;
            }
            Vec3 c = zone.center();
            if (zone.ticksLeft()[0] % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = level.getRandom().nextDouble() * Math.PI * 2;
                    double r = level.getRandom().nextDouble() * DARKNESS_RADIUS;
                    level.sendParticles(i % 2 == 0 ? ParticleTypes.SQUID_INK : ParticleTypes.SMOKE,
                            c.x + Math.cos(angle) * r, c.y + 0.2, c.z + Math.sin(angle) * r, 1, 0, 0.05, 0, 0.0);
                }
            }
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(DARKNESS_RADIUS, 3, DARKNESS_RADIUS),
                    e -> e.isAlive() && !e.getUUID().equals(zone.caster()) && e.position().distanceTo(c) <= DARKNESS_RADIUS + 0.5)) {
                // presas: não andam nem pulam
                target.setDeltaMovement(0.0, Math.min(0.0, target.getDeltaMovement().y), 0.0);
                target.hurtMarked = true;
                if (zone.ticksLeft()[0] % 10 == 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 9));
                }
            }
        }
    }

    /** Espaço: teleporta até 16 blocos na direção do olhar (para antes de bater num bloco). */
    private static boolean space(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(SPACE_RANGE)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getLocation().subtract(look.scale(0.8));
        Vec3 feet = target.subtract(0, player.getEyeHeight() * 0.5, 0);
        if (feet.distanceTo(player.position()) < 2.0 || !GnomeStaffItem.hasEnergy(player, stack, SPACE_COST)) {
            return false;
        }
        Vec3 from = player.position();
        boolean ok = player.randomTeleport(feet.x, feet.y, feet.z, false);
        if (!ok) {
            return false;
        }
        GnomeStaffItem.tryUse(player, stack, SPACE_COST);
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 40, 0.3, 0.6, 0.3, 0.3);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.3, 0.6, 0.3, 0.1);
        level.playSound(null, BlockPos.containing(from), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    // ================= runas avançadas =================

    /** Magma: erupção no ponto mirado — queima e joga para cima as criaturas, e a pedra em volta vira magma. */
    private static boolean magma(ServerLevel level, ServerPlayer player, ItemStack stack) {
        HitResult hit = player.pick(MAGMA_RANGE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !GnomeStaffItem.tryUse(player, stack, MAGMA_COST)) {
            return false;
        }
        BlockPos center = blockHit.getBlockPos();
        Vec3 c = Vec3.atCenterOf(center).add(0, 0.5, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(MAGMA_RADIUS, 2.5, MAGMA_RADIUS),
                e -> e != player && e.isAlive() && e.position().distanceTo(c) <= MAGMA_RADIUS + 1.0)) {
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), 6.0F);
            target.igniteForSeconds(5.0F);
            target.push(0.0, 0.8, 0.0);
            target.hurtMarked = true;
        }
        int r = (int) Math.ceil(MAGMA_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            if (pos.distSqr(center) > MAGMA_RADIUS * MAGMA_RADIUS || !level.mayInteract(player, pos)
                    || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            BlockState melted = com.mistech.mistechmod.item.rune.MagmaRuneItem.meltResult(state, level.getFluidState(pos));
            if (melted != null && melted.is(Blocks.MAGMA_BLOCK) && !state.is(Blocks.MAGMA_BLOCK)) {
                level.setBlockAndUpdate(pos, melted);
            }
        }
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 30, MAGMA_RADIUS / 2, 0.3, MAGMA_RADIUS / 2, 0.0);
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 60, MAGMA_RADIUS / 2, 0.6, MAGMA_RADIUS / 2, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5, c.z, 20, 1.0, 0.8, 1.0, 0.03);
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 0.6F);
        level.playSound(null, center, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    /** Vapor: explosão de vapor em volta — escalda, cega e empurra as criaturas, e deixa uma névoa no lugar. */
    private static boolean vapor(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!GnomeStaffItem.tryUse(player, stack, VAPOR_COST)) {
            return false;
        }
        Vec3 c = player.position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(VAPOR_RADIUS),
                e -> e != player && e.isAlive() && e.distanceTo(player) <= VAPOR_RADIUS)) {
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), 3.0F);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), player);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), player);
            Vec3 away = target.position().subtract(c).multiply(1, 0, 1).normalize().scale(1.2);
            target.push(away.x, 0.35, away.z);
            target.hurtMarked = true;
        }
        player.clearFire();
        net.minecraft.world.entity.AreaEffectCloud cloud = new net.minecraft.world.entity.AreaEffectCloud(level, c.x, c.y, c.z);
        cloud.setOwner(player);
        cloud.setRadius((float) VAPOR_RADIUS);
        cloud.setWaitTime(0);
        cloud.setDuration(20 * 10);
        cloud.setCustomParticle(ParticleTypes.CLOUD);
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 1.0, c.z, 150, VAPOR_RADIUS / 2, 1.0, VAPOR_RADIUS / 2, 0.06);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.4F, 0.5F);
        return true;
    }

    // ----- Vazio: fenda que puxa as criaturas e machuca quem chega no meio -----

    private record VoidRift(ResourceKey<Level> dimension, Vec3 center, java.util.UUID caster, int[] ticksLeft) {}

    private static final List<VoidRift> VOID_RIFTS = new ArrayList<>();

    private static boolean voidRift(ServerLevel level, ServerPlayer player, ItemStack stack) {
        HitResult hit = player.pick(VOID_RANGE, 0.0F, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(10.0))
                : hit.getLocation().add(0, 1.0, 0);
        if (!GnomeStaffItem.tryUse(player, stack, VOID_COST)) {
            return false;
        }
        VOID_RIFTS.add(new VoidRift(level.dimension(), center, player.getUUID(), new int[] {VOID_TICKS}));
        level.playSound(null, BlockPos.containing(center), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    private static void tickVoidRifts(ServerTickEvent.Post event) {
        Iterator<VoidRift> it = VOID_RIFTS.iterator();
        while (it.hasNext()) {
            VoidRift rift = it.next();
            ServerLevel level = event.getServer().getLevel(rift.dimension());
            if (level == null || --rift.ticksLeft()[0] <= 0) {
                it.remove();
                continue;
            }
            Vec3 c = rift.center();
            int left = rift.ticksLeft()[0];
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 6, 0.6, 0.6, 0.6, 0.02);
            if (left % 2 == 0) {
                level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 2, 0.15, 0.15, 0.15, 0.0);
            }
            if (left % 20 == 0) {
                level.playSound(null, BlockPos.containing(c), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.4F, 1.6F);
            }
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(VOID_PULL_RADIUS),
                    e -> e.isAlive() && !e.getUUID().equals(rift.caster()) && !e.isSpectator()
                            && e.getBoundingBox().getCenter().distanceTo(c) <= VOID_PULL_RADIUS)) {
                Vec3 to = c.subtract(target.getBoundingBox().getCenter());
                double dist = to.length();
                if (dist > 0.3) {
                    Vec3 pull = to.normalize().scale(Math.min(0.35, 0.12 + 0.6 / (dist + 1.0)));
                    target.setDeltaMovement(target.getDeltaMovement().scale(0.6).add(pull));
                    target.hurtMarked = true;
                }
                if (dist <= 1.6 && left % 10 == 0) {
                    ServerPlayer caster = level.getServer().getPlayerList().getPlayer(rift.caster());
                    target.hurtServer(level, caster != null ? level.damageSources().indirectMagic(caster, caster)
                            : level.damageSources().magic(), 3.0F);
                }
            }
        }
    }

    // ================= runas complexas =================

    /**
     * Sorte: mirando uma criatura (até 24 blocos) ela fica marcada pela sorte por 60 s (drops e XP dobrados ao morrer)
     * e brilha. Sem alvo: abençoa o jogador e quem estiver a até 6 blocos com Sorte II (60 s) e Fortuna (30 s).
     */
    private static boolean luck(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(LUCK_RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, player, eye, limit,
                player.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1.0),
                e -> e instanceof LivingEntity && !(e instanceof Player) && e.isAlive(), 0.3F);
        if (!GnomeStaffItem.tryUse(player, stack, LUCK_COST)) {
            return false;
        }
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            com.mistech.mistechmod.event.FortuneEvents.mark(target, 20 * 60);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60, 0), player);
            trail(level, eye, target.getBoundingBox().getCenter(), ParticleTypes.HAPPY_VILLAGER);
            com.mistech.mistechmod.item.rune.LuckRuneItem.fx(level, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("item.mistech.luck_rune.marked", target.getDisplayName()));
        } else {
            for (Player ally : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(LUCK_RADIUS),
                    p -> p.isAlive() && p.distanceTo(player) <= LUCK_RADIUS)) {
                ally.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 60, 1), player);
                ally.addEffect(new MobEffectInstance(com.mistech.mistechmod.registry.MistechEffects.FORTUNE, 20 * 30, 0), player);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 60, LUCK_RADIUS / 2, 0.8, LUCK_RADIUS / 2, 0.0);
            com.mistech.mistechmod.item.rune.LuckRuneItem.fx(level, player.getX(), player.getY() + 1.0, player.getZ());
        }
        return true;
    }

    /**
     * Energia Mágica: raio arcano até 32 blocos — 8 de dano mágico no primeiro alvo, que salta para até 3 criaturas
     * próximas (6 blocos) com 5 de dano cada. Tira a Absorção dos alvos.
     */
    /** Eco: grito do Warden carregado — 24 blocos, atravessa paredes e todas as criaturas na linha, 14 de dano que ignora armadura. */
    private static boolean echo(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!GnomeStaffItem.tryUse(player, stack, ECHO_COST)) {
            return false;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0F, 1.4F);
        com.mistech.mistechmod.beyond.Sonic.boom(level, player, ECHO_RANGE, ECHO_DAMAGE, true);
        return true;
    }

    /** Vigília: congela por 4 s todas as criaturas num cone de 12 blocos à frente (chefes não). */
    private static boolean vigil(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!GnomeStaffItem.tryUse(player, stack, VIGIL_COST)) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(VIGIL_RANGE),
                e -> e != player && com.mistech.mistechmod.beyond.PaleNetherEvents.canBeWatched(e)
                        && e.distanceTo(player) <= VIGIL_RANGE
                        && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.7)) {
            com.mistech.mistechmod.beyond.PaleNetherEvents.watch(target, VIGIL_TICKS, player);
            level.sendParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
        }
        for (int i = 1; i <= VIGIL_RANGE; i += 2) {
            Vec3 p = eye.add(look.scale(i));
            level.sendParticles(ParticleTypes.WHITE_ASH, p.x, p.y, p.z, 6, i * 0.12, i * 0.08, i * 0.12, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.CREAKING_FREEZE, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    /** Almas: cone de chamas azuis de 8 blocos — 4 de dano, fogo 4 s e Murchar II 5 s. */
    private static boolean soul(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!GnomeStaffItem.tryUse(player, stack, SOUL_COST)) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SOUL_RANGE),
                e -> e != player && e.isAlive() && e.distanceTo(player) <= SOUL_RANGE
                        && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.75)) {
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), 4.0F);
            target.igniteForSeconds(4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), player);
        }
        for (int i = 1; i <= SOUL_RANGE; i++) {
            Vec3 p = eye.add(look.scale(i)).add(0, -0.3, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 5, i * 0.1, i * 0.07, i * 0.1, 0.01);
        }
        level.sendParticles(ParticleTypes.SOUL, eye.x, eye.y - 0.3, eye.z, 6, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    private static boolean arcane(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(ARCANE_RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, player, eye, limit,
                player.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && e != player, 0.3F);
        if (!GnomeStaffItem.tryUse(player, stack, ARCANE_COST)) {
            return false;
        }
        Vec3 from = eye.add(player.getLookAngle().scale(0.8)).add(0, -0.2, 0);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity first)) {
            trail(level, from, limit, ParticleTypes.ELECTRIC_SPARK);
            level.sendParticles(ParticleTypes.ENCHANT, limit.x, limit.y, limit.z, 20, 0.3, 0.3, 0.3, 0.5);
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.6F);
            return true;
        }
        List<LivingEntity> struck = new ArrayList<>();
        LivingEntity current = first;
        Vec3 prev = from;
        for (int i = 0; i <= ARCANE_CHAINS && current != null; i++) {
            Vec3 at = current.getBoundingBox().getCenter();
            trail(level, prev, at, ParticleTypes.ELECTRIC_SPARK);
            trail(level, prev, at, ParticleTypes.WITCH);
            current.removeEffect(MobEffects.ABSORPTION);
            current.hurtServer(level, level.damageSources().indirectMagic(player, player), i == 0 ? 8.0F : 5.0F);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.2);
            struck.add(current);
            prev = at;
            final Vec3 center = at;
            current = level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(ARCANE_CHAIN_RADIUS),
                            e -> e.isAlive() && e != player && !struck.contains(e) && !(e instanceof Player)
                                    && e.getBoundingBox().getCenter().distanceTo(center) <= ARCANE_CHAIN_RADIUS)
                    .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(center))).orElse(null);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.6F);
        level.playSound(null, first.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.8F);
        return true;
    }

    /** Linha de partículas entre dois pontos. */
    private static void trail(ServerLevel level, Vec3 from, Vec3 to, net.minecraft.core.particles.ParticleOptions particle) {
        double dist = from.distanceTo(to);
        int steps = Math.max(2, (int) (dist * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** Nuvens: ponte de nuvens (3 de largura × {@link #CLOUD_BRIDGE}) na direção em que o jogador está virado, na altura dos pés. Some em 30 s. */
    private static boolean cloudBridge(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!GnomeStaffItem.hasEnergy(player, stack, CLOUD_COST)) {
            return false;
        }
        net.minecraft.core.BlockPos feet = net.minecraft.core.BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
        int placed = com.mistech.mistechmod.sky.Clouds.bridge(level, feet, player.getDirection(), CLOUD_BRIDGE, player);
        if (placed == 0) {
            return false;
        }
        GnomeStaffItem.tryUse(player, stack, CLOUD_COST);
        return true;
    }
}
