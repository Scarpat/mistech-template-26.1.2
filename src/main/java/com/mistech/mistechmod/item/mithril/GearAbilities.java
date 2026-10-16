package com.mistech.mistechmod.item.mithril;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Habilidades das ferramentas e armas acima do mitril (só com energia; cada bloco extra gasta energia normalmente).
 * <ul>
 * <li><b>Adamantina+:</b> picareta e pá cavam 3×3 (Oricalco Puro: 5×5); machado derruba a árvore inteira.
 * Agachado desliga as duas coisas.</li>
 * <li><b>Oricalco+:</b> ímã — drops de blocos e de criaturas vão direto para o inventário; espada/lança com golpe
 * carregado solta uma onda que acerta quem está em volta do alvo (50%; Puro 75% do dano).</li>
 * <li><b>Oricalco Puro:</b> golpe carregado de espada/lança também solta um raio de luz na direção do olhar.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GearAbilities {
    public static final int TREE_LIMIT = 192;
    public static final int WAVE_COST = 100;
    public static final double WAVE_RADIUS = 3.0;
    public static final float WAVE_FRACTION = 0.5F;
    public static final float WAVE_FRACTION_PURE = 0.75F;
    public static final int RAY_COST = 300;
    public static final double RAY_RANGE = 16.0;
    public static final float RAY_DAMAGE = 6.0F;
    public static final int RAY_COOLDOWN = 20;

    /** Face do bloco que o jogador começou a quebrar (para saber o plano do 3×3). */
    private static final Map<UUID, Direction> LAST_FACE = new HashMap<>();
    private static final Map<UUID, Long> RAY_READY = new HashMap<>();
    /** Evita que os blocos quebrados pela área disparem outra área. */
    private static boolean breaking;

    private GearAbilities() {}

    // ------------------------------------------------------------------ mineração

    /** Chamado pelo {@code mineBlock} das ferramentas revestidas, depois que o bloco já quebrou. */
    public static void afterMine(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (breaking || !(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server)
                || player.isCrouching() || !MithrilGear.isCharged(stack)) {
            return;
        }
        GearTier tier = MithrilGear.tier(stack);
        if (!tier.atLeast(GearTier.ADAMANTINE)) {
            return;
        }
        breaking = true;
        try {
            if (stack.is(ItemTags.AXES) && state.is(BlockTags.LOGS)) {
                fellTree(server, player, stack, pos);
            } else if ((stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)) && stack.getDestroySpeed(state) > 1.0F) {
                int radius = tier == GearTier.PURE_ORICHALCUM ? 2 : 1;
                mineArea(server, player, stack, state, pos, radius);
            }
        } finally {
            breaking = false;
        }
    }

    private static void mineArea(ServerLevel level, ServerPlayer player, ItemStack stack, BlockState origin, BlockPos pos, int radius) {
        Direction face = LAST_FACE.getOrDefault(player.getUUID(), lookFace(player));
        Direction.Axis axis = face.getAxis();
        float originHardness = Math.max(0.5F, origin.getDestroySpeed(level, pos));
        float maxHardness = originHardness * 2.0F + 1.5F;
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos p = switch (axis) {
                    case X -> pos.offset(0, a, b);
                    case Y -> pos.offset(a, 0, b);
                    case Z -> pos.offset(a, b, 0);
                };
                if (!MithrilGear.isCharged(stack) || player.getMainHandItem() != stack) {
                    return;
                }
                BlockState state = level.getBlockState(p);
                if (state.isAir() || state.hasBlockEntity()) {
                    continue;
                }
                float hardness = state.getDestroySpeed(level, p);
                if (hardness < 0.0F || hardness > maxHardness || stack.getDestroySpeed(state) <= 1.0F
                        || state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) {
                    continue;
                }
                player.gameMode.destroyBlock(p);
            }
        }
    }

    /** Face pelo olhar (quando não deu para gravar o clique). */
    private static Direction lookFace(Player player) {
        if (player.getXRot() > 50.0F) {
            return Direction.UP;
        }
        if (player.getXRot() < -50.0F) {
            return Direction.DOWN;
        }
        return player.getDirection().getOpposite();
    }

    private static void fellTree(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos origin) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        boolean leaves = false;
        while (!queue.isEmpty() && logs.size() < TREE_LIMIT) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos p = current.offset(dx, dy, dz);
                        if (p.getY() < origin.getY() || !seen.add(p)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(p);
                        if (state.is(BlockTags.LOGS)) {
                            logs.add(p);
                            queue.add(p);
                        } else if (state.is(BlockTags.LEAVES)
                                && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT))) {
                            leaves = true;
                        }
                    }
                }
            }
        }
        if (!leaves) {
            return; // sem folhas naturais: provavelmente é construção, não árvore
        }
        for (BlockPos p : logs) {
            if (!MithrilGear.isCharged(stack) || player.getMainHandItem() != stack) {
                return;
            }
            player.gameMode.destroyBlock(p);
        }
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getFace() != null && !event.getLevel().isClientSide()) {
            LAST_FACE.put(event.getEntity().getUUID(), event.getFace());
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_FACE.remove(event.getEntity().getUUID());
        RAY_READY.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ ímã

    private static boolean magnet(ItemStack tool) {
        return MithrilGear.isMithril(tool) && MithrilGear.isCharged(tool) && MithrilGear.tier(tool).atLeast(GearTier.ORICHALCUM);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player && magnet(event.getTool())) {
            pull(player, event.getDrops().iterator());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && magnet(player.getMainHandItem())) {
            pull(player, event.getDrops().iterator());
        }
    }

    private static void pull(Player player, Iterator<ItemEntity> drops) {
        while (drops.hasNext()) {
            ItemEntity drop = drops.next();
            ItemStack stack = drop.getItem();
            player.getInventory().add(stack);
            if (stack.isEmpty()) {
                drops.remove();
            } else {
                drop.setItem(stack);
            }
        }
    }

    // ------------------------------------------------------------------ armas

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!MithrilGear.isMithril(weapon) || !MithrilGear.isCharged(weapon)
                || !(weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.SPEARS))) {
            return;
        }
        GearTier tier = MithrilGear.tier(weapon);
        if (!tier.atLeast(GearTier.ORICHALCUM) || player.getAttackStrengthScale(0.5F) < 0.9F) {
            return;
        }
        boolean free = player.hasInfiniteMaterials();
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        // onda
        float waveDamage = base * (tier == GearTier.PURE_ORICHALCUM ? WAVE_FRACTION_PURE : WAVE_FRACTION);
        List<LivingEntity> around = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(WAVE_RADIUS, 1.0, WAVE_RADIUS),
                e -> e != player && e != target && e.isAlive() && canHit(player, e));
        if (!around.isEmpty()) {
            for (LivingEntity e : around) {
                e.hurtServer(level, level.damageSources().playerAttack(player), waveDamage);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    4, WAVE_RADIUS * 0.5, 0.1, WAVE_RADIUS * 0.5, 0);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
            if (!free) {
                MithrilGear.setEnergy(weapon, MithrilGear.getEnergy(weapon) - WAVE_COST);
            }
        }

        // raio de luz
        if (tier == GearTier.PURE_ORICHALCUM && level.getGameTime() >= RAY_READY.getOrDefault(player.getUUID(), 0L)
                && MithrilGear.isCharged(weapon)) {
            RAY_READY.put(player.getUUID(), level.getGameTime() + RAY_COOLDOWN);
            lightRay(level, player, target);
            if (!free) {
                MithrilGear.setEnergy(weapon, MithrilGear.getEnergy(weapon) - RAY_COST);
            }
        }
    }

    private static boolean canHit(Player player, LivingEntity e) {
        return !(e instanceof Player) && !(e instanceof ArmorStand) && !player.isAlliedTo(e)
                && !(e instanceof TamableAnimal tame && tame.isOwnedBy(player));
    }

    private static void lightRay(ServerLevel level, Player player, LivingEntity skip) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(RAY_RANGE));
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        end = hit.getLocation();
        double length = start.distanceTo(end);
        for (double d = 1.0; d < length; d += 0.5) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        AABB box = new AABB(start, end).inflate(1.0);
        for (Entity e : level.getEntities(player, box, e -> e instanceof LivingEntity && e != skip && e.isAlive())) {
            LivingEntity living = (LivingEntity) e;
            if (!canHit(player, living) || living.getBoundingBox().inflate(0.4).clip(start, end).isEmpty()) {
                continue;
            }
            float damage = living.is(EntityTypeTags.UNDEAD) ? RAY_DAMAGE * 2.0F : RAY_DAMAGE;
            living.hurtServer(level, level.damageSources().indirectMagic(player, player), damage);
            if (living.is(EntityTypeTags.UNDEAD)) {
                living.igniteForSeconds(3.0F);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 2.0F);
    }
}
