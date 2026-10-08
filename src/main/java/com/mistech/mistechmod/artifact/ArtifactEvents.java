package com.mistech.mistechmod.artifact;

import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.accessory.AccessoryItem;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** O que os Artefatos Rúnicos fazem (os que não cabem na própria classe do item). */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ArtifactEvents {
    // Botas do Vento
    public static final int JUMP_COST = 200;
    public static final double JUMP_VELOCITY = 0.6;
    // Patins Gélidos
    public static final int FREEZE_RADIUS = 3;
    public static final double ICE_SPEED = 0.6;
    public static final int SKATE_COST_PER_SECOND = 10;
    private static final Identifier ICE_SPEED_ID = Identifier.fromNamespaceAndPath(Mistech.MODID, "frost_skates_ice");
    // Manopla da Tempestade
    public static final int GAUNTLET_COST = 400;
    public static final double CHAIN_RADIUS = 6.0;
    public static final int CHAIN_TARGETS = 3;
    public static final float CHAIN_DAMAGE = 5.0F;
    // Máscara das Marés / Coroa do Farol
    public static final int MASK_COST_PER_SECOND = 20;
    public static final int CROWN_COST_PER_SECOND = 20;
    public static final double CROWN_RADIUS = 24.0;
    // Manto das Sombras
    public static final int CLOAK_COST_PER_SECOND = 10;
    public static final double CLOAK_DODGE_PER_RUNE = 0.15;
    public static final double CLOAK_DODGE_MAX = 0.45;
    private static final String STILL_TAG = "mistech_shadow_still";
    // Ímã do Vazio
    public static final double MAGNET_RADIUS = 8.0;
    public static final int MAGNET_COST_PER_ITEM = 2;
    // Ampulheta de Bolso
    public static final int HOURGLASS_RADIUS = 5;
    public static final int HOURGLASS_RANDOM_TICKS = 12;
    public static final int HOURGLASS_COST_PER_SECOND = 20;
    // Anel Espacial
    public static final int RING_COST = 5_000;
    public static final int RING_COST_OTHER_DIMENSION = 10_000;

    private ArtifactEvents() {}

    // ------------------------------------------------------------------ energia

    /** Gasta {@code amount} FE se tiver (criativo não gasta). */
    static boolean spend(Player player, ItemStack stack, int amount) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        int energy = MithrilGear.getEnergy(stack);
        if (energy < amount) {
            return false;
        }
        MithrilGear.setEnergy(stack, energy - amount);
        return true;
    }

    private static ItemStack worn(Player player, EquipmentSlot slot, net.minecraft.world.item.Item item) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.is(item) && MithrilGear.isCharged(stack) ? stack : ItemStack.EMPTY;
    }

    /** O acessório equipado (vazio se não tiver). */
    public static ItemStack accessory(Player player, Accessory accessory) {
        return Accessories.find(player, accessory); // espaços do Mistech ou do Curios
    }

    // ------------------------------------------------------------------ Botas do Vento

    public static int maxExtraJumps(ItemStack boots) {
        return Boolean.TRUE.equals(boots.get(ArtifactContent.CLOUD_STEP.get())) ? 2 : 1;
    }

    /** O cliente já deu o impulso; aqui o servidor cobra, zera a queda e faz o efeito. */
    public static void doubleJump(ServerPlayer player, int index) {
        ItemStack boots = worn(player, EquipmentSlot.FEET, ArtifactContent.WIND_BOOTS.get());
        if (boots.isEmpty() || index < 1 || index > maxExtraJumps(boots) || player.isPassenger() || !spend(player, boots, JUMP_COST)) {
            return;
        }
        player.resetFallDistance();
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.3, 0.05, 0.3, 0.03);
        level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.5F, 1.4F + index * 0.1F);
        if (Boolean.TRUE.equals(boots.get(ArtifactContent.CLOUD_STEP.get()))) {
            com.mistech.mistechmod.sky.Clouds.placeTemporary(level, BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ()), player);
        }
    }

    // ------------------------------------------------------------------ ticks do jogador

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.level();
        frostSkates(player, level);
        voidMagnet(player, level);
        pocketHourglass(player, level);
        if (player.tickCount % 5 == 0) {
            shadowCloak(player, level);
        }
        if (player.tickCount % 20 == 0) {
            tideMask(player);
            beaconCrown(player, level);
        }
    }

    private static void frostSkates(ServerPlayer player, ServerLevel level) {
        ItemStack skates = worn(player, EquipmentSlot.FEET, ArtifactContent.FROST_SKATES.get());
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean onIce = !skates.isEmpty() && player.onGround()
                && level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 0.1, player.getZ())).is(BlockTags.ICE);
        if (speed != null) {
            boolean has = speed.getModifier(ICE_SPEED_ID) != null;
            if (onIce && !has) {
                speed.addTransientModifier(new AttributeModifier(ICE_SPEED_ID, ICE_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!onIce && has) {
                speed.removeModifier(ICE_SPEED_ID);
            }
        }
        if (skates.isEmpty() || !player.onGround()) {
            return;
        }
        if (player.tickCount % 20 == 0 && !spend(player, skates, SKATE_COST_PER_SECOND)) {
            return;
        }
        BlockPos feet = player.blockPosition();
        BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-FREEZE_RADIUS, -1, -FREEZE_RADIUS), feet.offset(FREEZE_RADIUS, -1, FREEZE_RADIUS))) {
            if (!pos.closerToCenterThan(player.position(), FREEZE_RADIUS + 0.5)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && level.getFluidState(pos).isSourceOfType(Fluids.WATER) && level.getBlockState(pos.above()).isAir()
                    && frosted.canSurvive(level, pos) && level.isUnobstructed(frosted, pos, CollisionContext.empty())) {
                level.setBlockAndUpdate(pos, frosted);
                level.scheduleTick(pos, Blocks.FROSTED_ICE, 80 + player.getRandom().nextInt(80));
            }
        }
    }

    private static void tideMask(ServerPlayer player) {
        ItemStack mask = worn(player, EquipmentSlot.HEAD, ArtifactContent.TIDE_MASK.get());
        if (mask.isEmpty() || !player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) || !spend(player, mask, MASK_COST_PER_SECOND)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 40, 0, true, false, true));
        player.setAirSupply(player.getMaxAirSupply());
        if (GearRunes.count(GearRunes.tally(mask), RuneElement.WATER) > 0.0) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false, true));
        }
    }

    private static void beaconCrown(ServerPlayer player, ServerLevel level) {
        ItemStack crown = worn(player, EquipmentSlot.HEAD, ArtifactContent.BEACON_CROWN.get());
        if (crown.isEmpty() || !spend(player, crown, CROWN_COST_PER_SECOND)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        boolean throughWalls = GearRunes.count(GearRunes.tally(crown), RuneElement.ECHO) > 0.0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(CROWN_RADIUS),
                e -> e instanceof Enemy && e.isAlive() && e.distanceTo(player) <= CROWN_RADIUS)) {
            if (throughWalls || player.hasLineOfSight(living)) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false), player);
            }
        }
    }

    private static void shadowCloak(ServerPlayer player, ServerLevel level) {
        ItemStack cloak = worn(player, EquipmentSlot.CHEST, ArtifactContent.SHADOW_CLOAK.get());
        Vec3 motion = player.getDeltaMovement();
        boolean still = !cloak.isEmpty() && player.isShiftKeyDown() && player.onGround() && motion.x * motion.x + motion.z * motion.z < 0.0004;
        int count = still ? player.getPersistentData().getIntOr(STILL_TAG, 0) + 1 : 0;
        player.getPersistentData().putInt(STILL_TAG, count);
        if (count < 4) { // 1 s parado
            return;
        }
        if (player.tickCount % 20 == 0 && !spend(player, cloak, CLOAK_COST_PER_SECOND)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 15, 0, true, false, true));
        if (count == 4) {
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.8, player.getZ(), 12, 0.3, 0.5, 0.3, 0.01);
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0), m -> m.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    private static void voidMagnet(ServerPlayer player, ServerLevel level) {
        if (player.tickCount % 2 != 0) {
            return;
        }
        ItemStack magnet = accessory(player, Accessory.VOID_MAGNET);
        if (magnet.isEmpty() || !ArtifactAccessoryItem.active(magnet) || !MithrilGear.isCharged(magnet) || player.isShiftKeyDown()) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(MAGNET_RADIUS),
                e -> e.isAlive() && !e.hasPickUpDelay() && e.distanceTo(player) <= MAGNET_RADIUS)) {
            if (ArtifactAccessoryItem.filters(magnet, item.getItem())) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, item.getX(), item.getY() + 0.2, item.getZ(), 6, 0.1, 0.1, 0.1, 0.02);
                item.discard();
                continue;
            }
            if (!spend(player, magnet, MAGNET_COST_PER_ITEM)) {
                return;
            }
            Vec3 pull = player.position().add(0.0, 0.5, 0.0).subtract(item.position());
            item.setDeltaMovement(pull.normalize().scale(Math.min(0.6, pull.length() * 0.3)));
            item.hurtMarked = true;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void pocketHourglass(ServerPlayer player, ServerLevel level) {
        ItemStack hourglass = accessory(player, Accessory.POCKET_HOURGLASS);
        if (hourglass.isEmpty() || !ArtifactAccessoryItem.active(hourglass) || !MithrilGear.isCharged(hourglass)) {
            return;
        }
        if (player.tickCount % 20 == 0 && !spend(player, hourglass, HOURGLASS_COST_PER_SECOND)) {
            return;
        }
        BlockPos center = player.blockPosition();
        RandomSource random = player.getRandom();
        // plantas: ticks aleatórios extras
        if (player.tickCount % 2 == 0) {
            for (int i = 0; i < HOURGLASS_RANDOM_TICKS; i++) {
                BlockPos pos = center.offset(random.nextInt(HOURGLASS_RADIUS * 2 + 1) - HOURGLASS_RADIUS,
                        random.nextInt(5) - 2, random.nextInt(HOURGLASS_RADIUS * 2 + 1) - HOURGLASS_RADIUS);
                BlockState state = level.getBlockState(pos);
                if (state.isRandomlyTicking() && !state.is(Blocks.GRASS_BLOCK)
                        && (state.getBlock() instanceof BonemealableBlock || state.is(BlockTags.CROPS) || state.is(Blocks.SUGAR_CANE)
                        || state.is(Blocks.CACTUS) || state.is(Blocks.NETHER_WART))) {
                    state.randomTick(level, pos, random);
                }
            }
        }
        // fornalhas: um tick a mais por tick (2× mais rápidas)
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) {
                    continue;
                }
                for (BlockEntity be : java.util.List.copyOf(level.getChunk(cx + dx, cz + dz).getBlockEntities().values())) {
                    if (be instanceof AbstractFurnaceBlockEntity && be.getBlockPos().closerThan(center, HOURGLASS_RADIUS + 0.5)) {
                        BlockState state = be.getBlockState();
                        BlockEntityTicker ticker = state.getTicker(level, (net.minecraft.world.level.block.entity.BlockEntityType) be.getType());
                        if (ticker != null) {
                            ticker.tick(level, be.getBlockPos(), state, be);
                        }
                    }
                }
            }
        }
        if (player.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.6, 0.4, 0.6, 0.2);
        }
    }

    // ------------------------------------------------------------------ Anel Espacial

    /** Tecla do anel: abre a lista de Âncoras como o Orbe de Translocação. */
    public static void openSpatialRing(ServerPlayer player) {
        ItemStack ring = accessory(player, Accessory.SPATIAL_RING);
        if (ring.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("item.mistech.spatial_ring.missing").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!player.hasInfiniteMaterials() && MithrilGear.getEnergy(ring) < RING_COST) {
            player.sendOverlayMessage(Component.translatable("item.mistech.spatial_ring.no_energy", RING_COST).withStyle(ChatFormatting.RED));
            return;
        }
        com.mistech.mistechmod.spatial.SpatialService.openList(player, "");
    }

    /** Anel equipado com energia para ir até {@code otherDimension ? 10.000 : 5.000} FE (vazio se não). */
    public static ItemStack readyRing(ServerPlayer player, boolean otherDimension) {
        ItemStack ring = accessory(player, Accessory.SPATIAL_RING);
        int cost = otherDimension ? RING_COST_OTHER_DIMENSION : RING_COST;
        return !ring.isEmpty() && (player.hasInfiniteMaterials() || MithrilGear.getEnergy(ring) >= cost) ? ring : ItemStack.EMPTY;
    }

    public static void spendRing(ServerPlayer player, ItemStack ring, boolean otherDimension) {
        spend(player, ring, otherDimension ? RING_COST_OTHER_DIMENSION : RING_COST);
    }

    // ------------------------------------------------------------------ dano

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        // Manto das Sombras + Runa do Vazio: chance de sumir e reaparecer perto
        if (target instanceof ServerPlayer player && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            ItemStack cloak = worn(player, EquipmentSlot.CHEST, ArtifactContent.SHADOW_CLOAK.get());
            double voids = cloak.isEmpty() ? 0.0 : GearRunes.count(GearRunes.tally(cloak), RuneElement.VOID);
            if (voids > 0.0 && !player.getCooldowns().isOnCooldown(cloak)
                    && player.getRandom().nextDouble() < Math.min(CLOAK_DODGE_MAX, CLOAK_DODGE_PER_RUNE * voids) && blink(player)) {
                player.getCooldowns().addCooldown(cloak, 100);
                event.setCanceled(true);
                return;
            }
        }
        // Manopla da Tempestade: raio em cadeia
        Entity direct = source.getDirectEntity();
        if (direct instanceof ServerPlayer player && direct == source.getEntity() && player != target) {
            ItemStack gauntlet = player.getMainHandItem();
            if (gauntlet.is(ArtifactContent.STORM_GAUNTLET.get()) && MithrilGear.isCharged(gauntlet) && !player.getCooldowns().isOnCooldown(gauntlet)
                    && spend(player, gauntlet, GAUNTLET_COST)) {
                player.getCooldowns().addCooldown(gauntlet, 20);
                chainLightning(level, player, target);
            }
        }
    }

    private static boolean blink(ServerPlayer player) {
        double x = player.getX(), y = player.getY(), z = player.getZ();
        for (int i = 0; i < 16; i++) {
            double tx = x + (player.getRandom().nextDouble() - 0.5) * 12.0;
            double ty = y + player.getRandom().nextInt(7) - 3;
            double tz = z + (player.getRandom().nextDouble() - 0.5) * 12.0;
            if (player.randomTeleport(tx, ty, tz, true)) {
                player.level().sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 20, 0.3, 0.6, 0.3, 0.05);
                player.level().playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
                return true;
            }
        }
        return false;
    }

    private static void chainLightning(ServerLevel level, ServerPlayer player, LivingEntity first) {
        LivingEntity from = first;
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        hit.add(first);
        for (int i = 0; i < CHAIN_TARGETS; i++) {
            final LivingEntity source = from;
            Optional<LivingEntity> next = level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(CHAIN_RADIUS),
                    e -> e.isAlive() && e != player && !hit.contains(e) && !(e instanceof Player)
                            && !(e instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerReference() == null ? null
                                    : own.getOwnerReference().getUUID()))
                            && e.distanceTo(source) <= CHAIN_RADIUS)
                    .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(source)));
            if (next.isEmpty()) {
                break;
            }
            LivingEntity to = next.get();
            spark(level, source.getBoundingBox().getCenter(), to.getBoundingBox().getCenter());
            to.hurtServer(level, level.damageSources().lightningBolt(), CHAIN_DAMAGE);
            hit.add(to);
            from = to;
        }
        spark(level, player.getEyePosition(), first.getBoundingBox().getCenter());
        level.playSound(null, first.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    private static void spark(ServerLevel level, Vec3 a, Vec3 b) {
        Vec3 step = b.subtract(a);
        int n = Math.max(2, (int) (step.length() * 3));
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.add(step.scale(i / (double) n));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    // ------------------------------------------------------------------ Marreta Vulcânica: derreter

    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!tool.is(ArtifactContent.VOLCANIC_HAMMER.get()) || !VolcanicHammerItem.autosmelt(tool) || !MithrilGear.isCharged(tool)) {
            return;
        }
        ServerLevel level = event.getLevel();
        float xp = 0;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack));
                if (!result.isEmpty()) {
                    xp += recipe.get().value().experience() * stack.getCount();
                    drop.setItem(result.copyWithCount(result.getCount() * stack.getCount()));
                }
            }
        }
        if (xp > 0) {
            ExperienceOrb.award(level, event.getPos().getCenter(), (int) Math.ceil(xp));
            level.sendParticles(ParticleTypes.FLAME, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                    4, 0.2, 0.2, 0.2, 0.01);
        }
    }
}
