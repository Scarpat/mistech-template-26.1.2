package com.mistech.mistechmod.beyond;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Jardim Pálido e Nether:
 * <ul>
 *   <li><b>Vigília</b>: quem tem o efeito trava (Vigiado) toda criatura que olha; o <b>Olhar Pesado</b> deixa lenta e fraca;
 *       a <b>Guarda Vigilante</b> deixa lento quem mira em você; Runa da Vigília engastada = olhar pesado mais fraco;</li>
 *   <li><b>Vigiado</b>: não anda, não voa e não causa dano;</li>
 *   <li><b>Véu das Almas</b> e <b>Pacto Piglin</b>: mobs do Nether / piglins não te escolhem como alvo;</li>
 *   <li><b>Toque das Almas</b>: golpes dão Murchar II;</li>
 *   <li><b>Âncora de Portal</b>: voltando do Nether, o jogador vinculado sai na âncora.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class PaleNetherEvents {
    public static final double VIGIL_RANGE = 24.0;
    public static final double GAZE_RANGE = 16.0;
    public static final double WARD_RANGE = 16.0;

    private PaleNetherEvents() {}

    /** Chefes e jogadores não são travados. */
    public static boolean canBeWatched(LivingEntity entity) {
        return !(entity instanceof Player) && entity.isAlive() && !entity.is(Tags.EntityTypes.BOSSES);
    }

    /** Criaturas que o jogador está olhando (dentro de um cone; {@code cone} = 1 − cosseno). */
    public static List<LivingEntity> looked(ServerLevel level, Player player, double range, double cone) {
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && canBeWatched(e) && e.distanceToSqr(player) <= range * range
                        && e.isLookingAtMe(player, cone, false, true, e.getEyeY(), e.getY() + e.getBbHeight() * 0.5));
    }

    public static void watch(LivingEntity target, int ticks, @org.jspecify.annotations.Nullable Entity source) {
        target.addEffect(new MobEffectInstance(MistechEffects.WATCHED, ticks, 0, false, false, true), source);
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 4 != 0) {
            return;
        }
        if (player.hasEffect(MistechEffects.VIGIL_GAZE)) {
            for (LivingEntity target : looked(level, player, VIGIL_RANGE, 0.15)) {
                if (!target.hasEffect(MistechEffects.WATCHED)) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                            6, 0.3, 0.2, 0.3, 0.0);
                    level.playSound(null, target.blockPosition(), SoundEvents.CREAKING_FREEZE, SoundSource.PLAYERS, 0.6F, 1.2F);
                }
                watch(target, 12, player);
            }
        }
        double gearVigil = player.tickCount % 20 == 0 ? GearRunes.count(GearRunes.tallyEquipped(player), RuneElement.VIGIL) : 0.0;
        boolean heavy = player.hasEffect(MistechEffects.HEAVY_GAZE);
        if (heavy || gearVigil > 0.0) {
            int slow = heavy ? 2 : Math.min(3, (int) Math.ceil(gearVigil)) - 1;
            for (LivingEntity target : looked(level, player, GAZE_RANGE, 0.05)) {
                if (!heavy && !(target instanceof Enemy)) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, Math.max(0, slow), false, true, true), player);
                if (heavy) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, true, true), player);
                }
            }
        }
        if (player.tickCount % 20 == 0 && player.hasEffect(MistechEffects.VIGIL_WARD)) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(WARD_RANGE), m -> m.getTarget() == player)) {
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, true, true), player);
            }
        }
    }

    /** Vigiado: fica parado no ar/chão. */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide() && living.hasEffect(MistechEffects.WATCHED)) {
            Vec3 v = living.getDeltaMovement();
            living.setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
            if (living instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        }
    }

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living && living.hasEffect(MistechEffects.WATCHED)) {
            event.setCanceled(true);
            return;
        }
        // Toque das Almas
        if (attacker instanceof Player player && event.getSource().getDirectEntity() == player && player.hasEffect(MistechEffects.SOUL_TOUCH)
                && event.getEntity() != player) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), player);
        }
    }

    // ------------------------------------------------------------------ Véu das Almas / Pacto Piglin

    public static boolean hasPiglinPact(Player player) {
        return player.getData(PaleNetherContent.PIGLIN_PACT) > player.level().getGameTime();
    }

    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity attacker = event.getEntity();
        boolean soulVeil = player.hasEffect(MistechEffects.SOUL_VEIL) && attacker.is(PaleNetherContent.SOUL_VEIL_IGNORES);
        boolean pact = attacker instanceof AbstractPiglin && hasPiglinPact(player);
        if ((soulVeil || pact) && attacker.getLastHurtByMob() != player) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ Pacto Piglin: escambo extra

    public static final String BARTER_TAG = "mistech_bartering";
    public static final float PACT_BARTER_CHANCE = 0.5F;

    /**
     * Pacto Piglin: quando um piglin termina de admirar o ouro (o lingote some da mão secundária sem ele ter apanhado),
     * 50% de chance de jogar um escambo extra para o jogador com pacto mais perto (até 8 blocos).
     */
    @SubscribeEvent
    static void onPiglinTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.piglin.Piglin piglin) || !(piglin.level() instanceof ServerLevel level)
                || !piglin.isAdult()) {
            return;
        }
        boolean holdingGold = piglin.getOffhandItem().is(net.minecraft.world.entity.monster.piglin.PiglinAi.BARTERING_ITEM);
        net.minecraft.nbt.CompoundTag data = piglin.getPersistentData();
        if (holdingGold) {
            if (!data.getBooleanOr(BARTER_TAG, false)) {
                data.putBoolean(BARTER_TAG, true);
            }
            return;
        }
        if (!data.getBooleanOr(BARTER_TAG, false)) {
            return;
        }
        data.remove(BARTER_TAG);
        // admirar interrompido por um golpe não é escambo
        if (!piglin.isAlive() || piglin.hurtTime > 0 || piglin.tickCount - piglin.getLastHurtByMobTimestamp() < 40) {
            return;
        }
        Player lucky = level.getNearestPlayer(piglin.getX(), piglin.getY(), piglin.getZ(), 8.0,
                p -> p instanceof Player player && hasPiglinPact(player));
        if (lucky == null || level.getRandom().nextFloat() >= PACT_BARTER_CHANCE) {
            return;
        }
        net.minecraft.world.level.storage.loot.LootTable table = level.getServer().reloadableRegistries()
                .getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.PIGLIN_BARTERING);
        List<net.minecraft.world.item.ItemStack> items = table.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, piglin)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.PIGLIN_BARTER));
        Vec3 toward = lucky.position().subtract(piglin.position()).normalize().scale(0.3);
        for (net.minecraft.world.item.ItemStack stack : items) {
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level,
                    piglin.getX(), piglin.getEyeY() - 0.3, piglin.getZ(), stack);
            drop.setDeltaMovement(toward.x, 0.25, toward.z);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        level.sendParticles(ParticleTypes.WAX_ON, piglin.getX(), piglin.getEyeY(), piglin.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
    }

    // ------------------------------------------------------------------ Âncora de Portal

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getFrom() != Level.NETHER || event.getTo() != Level.OVERWORLD) {
            return;
        }
        String anchor = player.getData(PaleNetherContent.PORTAL_ANCHOR);
        BlockPos pos = PortalAnchorBlock.parse(anchor);
        ServerLevel overworld = player.level().getServer().overworld();
        if (pos == null) {
            return;
        }
        if (!overworld.getBlockState(pos).is(PaleNetherContent.PORTAL_ANCHOR_BLOCK.get())) {
            player.setData(PaleNetherContent.PORTAL_ANCHOR, "");
            player.sendSystemMessage(Component.translatable("message.mistech.portal_anchor.lost").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.teleportTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        player.setPortalCooldown();
        overworld.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.05);
        overworld.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.mistech.portal_anchor.arrived").withColor(0xB05CFF));
    }
}
