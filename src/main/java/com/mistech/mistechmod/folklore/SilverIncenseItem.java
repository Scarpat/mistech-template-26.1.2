package com.mistech.mistechmod.folklore;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.IncenseItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Incenso de Prata (4 incensos + 1 lingote de mitril). Queima como o incenso comum (Véu de Incenso, 2 min) e a fumaça de
 * prata <b>espanta lobisomens</b>:
 * <ul>
 *   <li>lobisomens a até {@link #SCARE_RADIUS} blocos de quem carrega aceso (ou 2× o raio do Porta-Incenso) fogem da fumaça e
 *       largam o alvo;</li>
 *   <li>enquanto queima, nenhum lobisomem novo aparece perto (nem na lua cheia, nem na Lua de Sangue): quem carrega fica
 *       protegido, e o Porta-Incenso protege {@link #WARD_RADIUS} blocos em volta.</li>
 * </ul>
 */
public class SilverIncenseItem extends IncenseItem {
    /** Raio em que lobisomens fogem de quem carrega o incenso aceso. */
    public static final double SCARE_RADIUS = 16.0;
    /** Raio protegido contra o surgimento de lobisomens em volta de um Porta-Incenso com Incenso de Prata. */
    public static final int WARD_RADIUS = 48;
    /** Por quanto tempo (ticks) uma proteção dura sem ser renovada (renovada a cada segundo enquanto queima). */
    private static final int WARD_TICKS = 60;
    public static final String PLAYER_WARD_TAG = "mistech_silver_ward";

    /** Porta-Incensos queimando Incenso de Prata no Mundo Normal → até quando (game time) protegem. */
    private static final Map<BlockPos, Long> HOLDER_WARDS = new ConcurrentHashMap<>();

    public SilverIncenseItem(Properties properties) {
        super(properties);
    }

    public static boolean isSilver(ItemStack stack) {
        return stack.getItem() instanceof SilverIncenseItem;
    }

    /** Porta-Incenso: se {@code stack} for Incenso de Prata, espanta os lobisomens e protege a área. */
    public static void applyAny(ItemStack stack, ServerLevel level, BlockPos pos, double radius) {
        if (!isSilver(stack)) {
            return;
        }
        Vec3 center = pos.getCenter();
        scare(level, center, radius * 2);
        if (level.dimension() == Level.OVERWORLD) {
            HOLDER_WARDS.put(pos.immutable(), level.getGameTime() + WARD_TICKS);
        }
        if (level.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.6, center.z, 1, 0.3, 0.3, 0.3, 0.01);
        }
    }

    /** Lobisomens na fumaça fogem dela. */
    public static void scare(ServerLevel level, Vec3 center, double radius) {
        for (WerewolfEntity werewolf : level.getEntitiesOfClass(WerewolfEntity.class, new AABB(BlockPos.containing(center)).inflate(radius),
                e -> e.isAlive() && e.position().distanceToSqr(center) <= radius * radius)) {
            werewolf.scareFrom(center);
        }
    }

    /**
     * Um lobisomem pode surgir em {@code pos} (perto de {@code player})? Não, se o jogador estiver carregando Incenso de Prata
     * aceso ou se houver um Porta-Incenso com Incenso de Prata queimando a até {@link #WARD_RADIUS} blocos.
     */
    public static boolean isWarded(ServerLevel level, Player player, BlockPos pos) {
        long now = level.getGameTime();
        if (player.getPersistentData().getLongOr(PLAYER_WARD_TAG, 0L) > now) {
            return true;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        long maxSqr = (long) WARD_RADIUS * WARD_RADIUS;
        Iterator<Map.Entry<BlockPos, Long>> it = HOLDER_WARDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> ward = it.next();
            if (ward.getValue() <= now) {
                it.remove();
            } else if (ward.getKey().distSqr(pos) <= maxSqr || ward.getKey().distSqr(player.blockPosition()) <= maxSqr) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        boolean lit = isLit(stack);
        super.inventoryTick(stack, level, entity, slot);
        if (lit && entity instanceof Player player && level.getGameTime() % 20 == 0) {
            player.getPersistentData().putLong(PLAYER_WARD_TAG, level.getGameTime() + WARD_TICKS);
            scare(level, player.position(), SCARE_RADIUS);
            if (level.getRandom().nextInt(2) == 0) {
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.3, 0.4, 0.3, 0.01);
            }
        }
    }
}
