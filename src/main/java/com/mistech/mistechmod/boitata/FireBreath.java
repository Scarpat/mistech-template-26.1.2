package com.mistech.mistechmod.boitata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Sopro de Fogo (tecla R): montado num Boitatá adulto, segurar a tecla solta um jato contínuo (com fôlego, ver
 * {@link BoitataMountEntity#MAX_STAMINA}); com a Armadura do Boitatá completa, o próprio jogador sopra uma vez por minuto.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class FireBreath {
    public static final long ARMOR_COOLDOWN = 1200L;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private FireBreath() {}

    /** Cliente -> servidor: tecla do sopro apertada (true) ou solta (false). */
    public record BreathPayload(boolean held) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BreathPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "fire_breath"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BreathPayload> STREAM_CODEC =
                net.minecraft.network.codec.ByteBufCodecs.BOOL.map(BreathPayload::new, BreathPayload::held).cast();

        @Override
        public CustomPacketPayload.Type<BreathPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(BreathPayload.TYPE, BreathPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> use(player, payload.held()));
            }
        });
    }

    private static void use(ServerPlayer player, boolean held) {
        ServerLevel level = player.level();
        if (player.getVehicle() instanceof BoitataMountEntity mount && mount.isOwnedBy(player)) {
            mount.setBreathing(player, held);
            return;
        }
        if (!held || !BoitataGear.fullSet(player)) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < ARMOR_COOLDOWN && !player.isCreative()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.fire_breath.cooldown", (ARMOR_COOLDOWN - (now - last) + 19) / 20)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        LAST.put(player.getUUID(), now);
        cone(level, player, player.getEyePosition(), player.getLookAngle(), 8.0, 8.0F, true);
    }

    /** Cone de fogo: queima e machuca quem estiver na frente (menos o dono e os bichos dele). */
    public static void cone(ServerLevel level, LivingEntity source, Vec3 from, Vec3 dir, double range, float damage, boolean burst) {
        dir = dir.normalize();
        if (burst) {
            level.playSound(null, source.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.6F);
            level.playSound(null, source.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        for (int i = 0; i < (burst ? 40 : 0); i++) {
            double d = 1.0 + level.getRandom().nextDouble() * range;
            double spread = d * 0.22;
            Vec3 at = from.add(dir.scale(d)).add((level.getRandom().nextDouble() - 0.5) * spread, (level.getRandom().nextDouble() - 0.5) * spread,
                    (level.getRandom().nextDouble() - 0.5) * spread);
            level.sendParticles(i % 4 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0, 0, 0, 0.02);
        }
        Entity owner = source instanceof OwnableEntity ownable && ownable.getOwner() != null ? ownable.getOwner() : source;
        AABB box = new AABB(from, from.add(dir.scale(range))).inflate(range * 0.3);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (target == source || target == owner || target.isPassengerOfSameVehicle(source) || target.getVehicle() == source) {
                continue;
            }
            if (target instanceof OwnableEntity pet && pet.getOwner() == owner) {
                continue;
            }
            Vec3 to = target.getBoundingBox().getCenter().subtract(from);
            double along = to.dot(dir);
            if (along < 0 || along > range || to.subtract(dir.scale(along)).length() > 1.0 + along * 0.3) {
                continue;
            }
            target.igniteForSeconds(6.0F);
            var source2 = owner instanceof net.minecraft.world.entity.player.Player p ? level.damageSources().playerAttack(p)
                    : owner instanceof LivingEntity living ? level.damageSources().mobAttack(living) : level.damageSources().inFire();
            target.hurtServer(level, source2, damage);
        }
    }
}
