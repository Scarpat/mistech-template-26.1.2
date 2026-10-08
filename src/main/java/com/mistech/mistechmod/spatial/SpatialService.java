package com.mistech.mistechmod.spatial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mistech.mistechmod.network.OpenAnchorsPayload;
import com.mistech.mistechmod.registry.SpatialContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Lista de âncoras conhecidas e teletransporte (servidor). */
public final class SpatialService {
    /** Distância máxima até a âncora de onde se abriu a lista. */
    public static final double USE_DISTANCE = 8.0;
    /** 1 nível a cada tantos blocos (mesma dimensão). */
    public static final int BLOCKS_PER_LEVEL = 1000;
    public static final int MAX_COST = 5;
    /** Para outra dimensão. */
    public static final int CROSS_DIMENSION_COST = 5;

    private SpatialService() {}

    public static List<String> known(ServerPlayer player) {
        return player.getData(SpatialContent.KNOWN_ANCHORS);
    }

    public static void learn(ServerPlayer player, String key) {
        List<String> known = known(player);
        if (!known.contains(key)) {
            List<String> updated = new ArrayList<>(known);
            updated.add(key);
            player.setData(SpatialContent.KNOWN_ANCHORS, List.copyOf(updated));
            SpatialAnchors.Anchor anchor = SpatialAnchors.get(player.level().getServer()).get(key);
            if (anchor != null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.spatial_anchor.learned", anchor.name())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    private static void forget(ServerPlayer player, String key) {
        List<String> known = new ArrayList<>(known(player));
        if (known.remove(key)) {
            player.setData(SpatialContent.KNOWN_ANCHORS, List.copyOf(known));
        }
    }

    public static int cost(ServerPlayer player, SpatialAnchors.Anchor target) {
        if (player.hasInfiniteMaterials()) {
            return 0;
        }
        if (!player.level().dimension().identifier().equals(target.dimension())) {
            return CROSS_DIMENSION_COST;
        }
        double dist = Math.sqrt(player.blockPosition().distSqr(target.pos()));
        return Math.min(MAX_COST, (int) Math.ceil(dist / BLOCKS_PER_LEVEL));
    }

    /** Manda a lista para o cliente. {@code sourceKey} vazio = aberta pelo Orbe. */
    public static void openList(ServerPlayer player, String sourceKey) {
        SpatialAnchors anchors = SpatialAnchors.get(player.level().getServer());
        List<OpenAnchorsPayload.Entry> entries = new ArrayList<>();
        // aberta pelo Anel Espacial (sem Orbe na mão): paga em energia, a lista mostra "grátis" em níveis
        boolean ring = sourceKey.isEmpty() && !player.getMainHandItem().is(SpatialContent.TRANSLOCATION_ORB.get())
                && !player.getOffhandItem().is(SpatialContent.TRANSLOCATION_ORB.get());
        for (String key : known(player)) {
            SpatialAnchors.Anchor anchor = anchors.get(key);
            if (anchor != null) {
                entries.add(new OpenAnchorsPayload.Entry(key, anchor.name(), anchor.dimension(), anchor.pos(), ring ? 0 : cost(player, anchor)));
            }
        }
        entries.sort(Comparator.comparing(OpenAnchorsPayload.Entry::name, String.CASE_INSENSITIVE_ORDER));
        PacketDistributor.sendToPlayer(player, new OpenAnchorsPayload(sourceKey, entries));
    }

    public static final int MAX_NAME = 32;

    /** Pedido do cliente para renomear a âncora em que ele está (como uma waystone). */
    public static void tryRename(ServerPlayer player, String key, String rawName) {
        SpatialAnchors anchors = SpatialAnchors.get(player.level().getServer());
        SpatialAnchors.Anchor anchor = anchors.get(key);
        if (anchor == null || !anchor.dimension().equals(player.level().dimension().identifier())
                || player.distanceToSqr(anchor.pos().getCenter()) > USE_DISTANCE * USE_DISTANCE
                || !SpatialAnchorBlock.isAnchor(player.level(), anchor.pos())) {
            return;
        }
        String name = net.minecraft.util.StringUtil.filterText(rawName.replace("\u00a7", "")).strip();
        if (name.length() > MAX_NAME) {
            name = name.substring(0, MAX_NAME);
        }
        if (name.isEmpty() || name.equals(anchor.name())) {
            return;
        }
        anchors.add(new SpatialAnchors.Anchor(anchor.dimension(), anchor.pos(), name));
        player.sendOverlayMessage(Component.translatable("message.mistech.spatial_anchor.renamed", name).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, anchor.pos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        openList(player, key);
    }

    /** Pedido do cliente para ir até {@code targetKey}. */
    public static void tryTeleport(ServerPlayer player, String targetKey, String sourceKey) {
        SpatialAnchors anchors = SpatialAnchors.get(player.level().getServer());
        boolean fromOrb = sourceKey.isEmpty();
        InteractionHand orbHand = null;
        ItemStack ring = ItemStack.EMPTY;
        if (fromOrb) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.is(SpatialContent.TRANSLOCATION_ORB.get()) && !player.getCooldowns().isOnCooldown(stack)) {
                    orbHand = hand;
                    break;
                }
            }
            if (orbHand == null) {
                // Anel Espacial (acessório): paga em energia no lugar dos níveis
                SpatialAnchors.Anchor wanted = anchors.get(targetKey);
                boolean otherDim = wanted != null && !wanted.dimension().equals(player.level().dimension().identifier());
                ring = com.mistech.mistechmod.artifact.ArtifactEvents.readyRing(player, otherDim);
                if (ring.isEmpty()) {
                    return;
                }
            }
        } else {
            SpatialAnchors.Anchor source = anchors.get(sourceKey);
            if (source == null || !source.dimension().equals(player.level().dimension().identifier())
                    || player.distanceToSqr(source.pos().getCenter()) > USE_DISTANCE * USE_DISTANCE) {
                return;
            }
        }
        if (targetKey.equals(sourceKey) || !known(player).contains(targetKey)) {
            return;
        }
        SpatialAnchors.Anchor target = anchors.get(targetKey);
        ServerLevel level = target == null ? null : player.level().getServer().getLevel(target.levelKey());
        if (target == null || level == null || !SpatialAnchorBlock.isAnchor(level, target.pos())) {
            if (target != null) {
                anchors.remove(targetKey);
            }
            forget(player, targetKey);
            player.sendOverlayMessage(Component.translatable("message.mistech.spatial_anchor.gone").withStyle(ChatFormatting.RED));
            return;
        }
        int cost = ring.isEmpty() ? cost(player, target) : 0;
        if (player.experienceLevel < cost) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spatial_anchor.no_xp", cost).withStyle(ChatFormatting.RED));
            return;
        }
        Vec3 landing = landing(level, target.pos());
        if (landing == null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spatial_anchor.blocked").withStyle(ChatFormatting.RED));
            return;
        }
        if (cost > 0) {
            player.giveExperienceLevels(-cost);
        }
        if (!ring.isEmpty()) {
            com.mistech.mistechmod.artifact.ArtifactEvents.spendRing(player, ring,
                    !player.level().dimension().identifier().equals(target.dimension()));
        }
        if (orbHand != null) {
            ItemStack orb = player.getItemInHand(orbHand);
            player.getCooldowns().addCooldown(orb, TranslocationOrbItem.COOLDOWN_TICKS);
            orb.hurtAndBreak(1, player, orbHand);
        }
        ServerLevel from = player.level();
        Vec3 start = player.position();
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1.0, start.z, 40, 0.3, 0.8, 0.3, 0.05);
        from.playSound(null, start.x, start.y, start.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
        float yaw = (float) Math.toDegrees(Math.atan2(-(target.pos().getX() + 0.5 - landing.x), target.pos().getZ() + 0.5 - landing.z));
        player.teleport(new TeleportTransition(level, landing, Vec3.ZERO, yaw, 0.0F, TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.PORTAL, landing.x, landing.y + 1.0, landing.z, 60, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, landing.x, landing.y, landing.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, target.pos(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    /** Um lugar livre (2 blocos de ar com chão) ao lado do pilar; senão em cima dele. */
    private static Vec3 landing(ServerLevel level, BlockPos base) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos feet = base.relative(dir).above(dy);
                if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                    return Vec3.atBottomCenterOf(feet);
                }
            }
        }
        BlockPos top = base.above(2);
        if (level.getBlockState(top).getCollisionShape(level, top).isEmpty()
                && level.getBlockState(top.above()).getCollisionShape(level, top.above()).isEmpty()) {
            return new Vec3(top.getX() + 0.5, top.getY() - 0.0625, top.getZ() + 0.5);
        }
        return null;
    }
}
