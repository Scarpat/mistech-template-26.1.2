package com.mistech.mistechmod.realm.dream;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.DreamLayout;
import com.mistech.mistechmod.realm.GnomeRealm;
import com.mistech.mistechmod.registry.DreamRealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regras do Sonho em anéis:
 * <ul>
 * <li><b>Borda</b>: passou de {@link DreamLayout#EDGE} blocos do centro, o sonho se desfaz e o jogador acorda em casa.</li>
 * <li><b>Mar de Nuvens</b>: gravidade 40% menor.</li>
 * <li><b>Abismos de Queda</b>: cair fundo numa fenda = susto — volta para a beira com Escuridão e Lentidão, sem dano.</li>
 * <li><b>Flor-Olho</b>: a 6 blocos de uma, o jogador brilha e os monstros em volta (24 blocos) vêm atrás dele.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class DreamRealmEvents {
    public static final ResourceKey<Biome> CLOUD_SEA = biome("cloud_sea");
    public static final ResourceKey<Biome> FALLING_CHASMS = biome("falling_chasms");
    public static final Identifier LOW_GRAVITY = Identifier.fromNamespaceAndPath(Mistech.MODID, "dream_low_gravity");
    public static final double GRAVITY_CUT = -0.4;
    public static final int SCARE_FALL = 18;
    public static final int EYE_COOLDOWN = 12 * 20;

    private static final Map<UUID, BlockPos> SAFE = new HashMap<>();
    private static final Map<UUID, Long> EYE_READY = new HashMap<>();

    private DreamRealmEvents() {}

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mistech.MODID, name));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean realm = GnomeRealm.isRealm(level);
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (!realm) {
            if (gravity != null && gravity.getModifier(LOW_GRAVITY) != null) {
                gravity.removeModifier(LOW_GRAVITY);
            }
            return;
        }
        // borda
        if (DreamLayout.plainDistance(player.getX(), player.getZ()) > DreamLayout.EDGE) {
            player.sendSystemMessage(Component.translatable("message.mistech.gnome_realm.edge"));
            GnomeRealm.sendHome(player);
            return;
        }
        BlockPos pos = player.blockPosition();
        // susto da queda (todo tick: a queda é rápida)
        if (player.onGround() && !player.isSpectator()) {
            if (player.getY() > 50) {
                SAFE.put(player.getUUID(), pos);
            }
        } else if (player.fallDistance > SCARE_FALL && !player.getAbilities().flying && level.getBiome(pos).is(FALLING_CHASMS)) {
            BlockPos safe = SAFE.get(player.getUUID());
            if (safe != null && safe.distSqr(pos) < 64 * 64) {
                player.teleportTo(safe.getX() + 0.5, safe.getY() + 0.1, safe.getZ() + 0.5);
                player.setDeltaMovement(0, 0, 0);
                player.hurtMarked = true;
                player.resetFallDistance();
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
                level.playSound(null, safe, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 1.4F);
                player.sendSystemMessage(Component.translatable("message.mistech.gnome_realm.startle"));
                return;
            }
        }
        if (player.tickCount % 10 != 5) {
            return;
        }
        // camadas visitadas (para o Apanha-Sonho Lúcido)
        if (player.tickCount % 40 == 5) {
            int ring = Math.min(DreamLayout.RING_REM, DreamLayout.ring(player.getX(), player.getZ()));
            String tag = com.mistech.mistechmod.realm.dream.item.LucidDreamcatcherItem.VISITED_TAG;
            int seen = player.getPersistentData().getIntOr(tag, 0);
            if ((seen & (1 << ring)) == 0) {
                player.getPersistentData().putInt(tag, seen | (1 << ring));
            }
        }
        // gravidade do Mar de Nuvens
        if (gravity != null) {
            boolean low = level.getBiome(pos).is(CLOUD_SEA);
            boolean has = gravity.getModifier(LOW_GRAVITY) != null;
            if (low && !has) {
                gravity.addTransientModifier(new AttributeModifier(LOW_GRAVITY, GRAVITY_CUT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!low && has) {
                gravity.removeModifier(LOW_GRAVITY);
            }
        }
        // Flor-Olho
        if (player.tickCount % 40 == 5 && DreamLayout.ring(player.getX(), player.getZ()) == DreamLayout.RING_NIGHTMARE
                && !player.isCreative() && !player.isSpectator() && !player.isCrouching()
                && level.getGameTime() >= EYE_READY.getOrDefault(player.getUUID(), 0L)) {
            BlockPos flower = findEyeFlower(level, pos);
            if (flower != null) {
                EYE_READY.put(player.getUUID(), level.getGameTime() + EYE_COOLDOWN);
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
                level.sendParticles(ParticleTypes.SCULK_SOUL, flower.getX() + 0.5, flower.getY() + 0.8, flower.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
                level.playSound(null, flower, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 1.0F, 0.5F);
                for (Monster monster : level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(24.0))) {
                    if (monster.getTarget() == null) {
                        monster.setTarget(player);
                    }
                }
            }
        }
    }

    private static BlockPos findEyeFlower(ServerLevel level, BlockPos center) {
        int r = EyeFlowerBlock.RANGE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -3, -r), center.offset(r, 3, r))) {
            if (level.getBlockState(p).is(DreamRealmContent.EYE_FLOWER.get())) {
                return p.immutable();
            }
        }
        return null;
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SAFE.remove(event.getEntity().getUUID());
        EYE_READY.remove(event.getEntity().getUUID());
    }
}
