package com.mistech.mistechmod.tarot.magus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.tarot.ArcanePlane;
import com.mistech.mistechmod.tarot.ArenaLayout;
import com.mistech.mistechmod.tarot.ArenaTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * O Salão Arcano: arena vasta (raio {@link #RADIUS}) de pedra-negra polida com um ∞ dourado gravado no chão, círculos de
 * ametista e quatro cantos, um de cada elemento (pilares musgosos, pilares de calcita com nuvens, pilares de prismarinho
 * e braseiros). A Mesa do Mago fica no norte.
 * <p>
 * <b>A arena se molda</b> ({@link Field}): ao trocar de modo o chão vira o elemento em ondas que saem do centro
 * (poucas centenas de blocos por tick) e volta ao normal no fim. Blocos temporários (espinhos, muralhas, poças de magma)
 * somem sozinhos. Na Fase IV surge o plano de cima (vidro, a {@link #UPPER} blocos) — "assim em cima, como embaixo".
 * <p>
 * Cair do salão não é derrota: o Mago devolve o jogador com um pouco de dano.
 */
public class MagusArena implements ArenaLayout {
    public static final MagusArena INSTANCE = new MagusArena();

    public static final int RADIUS = 30;
    /** Raio que se transforma (os cantos decorados ficam de fora). */
    public static final int PAINT_RADIUS = 26;
    /** Altura do plano de cima (chão de vidro) em relação ao chão de baixo. */
    public static final int UPPER = 26;
    public static final int UPPER_RADIUS = 22;
    public static final float FALL_DAMAGE = 4.0F;

    private static final Map<BlockPos, Field> FIELDS = new HashMap<>();

    public static @Nullable Field field(@Nullable BlockPos center) {
        return center == null ? null : FIELDS.get(center);
    }

    @Override
    public double leashRadius() {
        return RADIUS + 4.0;
    }

    @Override
    public double leashBelow() {
        return -8.0;
    }

    @Override
    public int clearRadius() {
        return ArenaTools.MAX_CLEAR_RADIUS;
    }

    @Override
    public double fallLimit() {
        return -14.0;
    }

    @Override
    public Vec3 arrival(BlockPos center, int index) {
        double angle = Math.toRadians(90 + (index - 1.5) * 14);
        double dist = 20.0;
        return new Vec3(center.getX() + 0.5 + Math.cos(angle) * dist, center.getY() + 1.0, center.getZ() + 0.5 + Math.sin(angle) * dist);
    }

    @Override
    public Vec3 bossSpawn(BlockPos center) {
        return new Vec3(center.getX() + 0.5, center.getY() + 1.0, center.getZ() - 6.5);
    }

    // ----------------------------------------------------------------- construção

    @Override
    public void build(ServerLevel level, BlockPos center) {
        ArenaTools.clear(level, center, this);
        FIELDS.put(center.immutable(), new Field(center.immutable()));
        int flags = Block.UPDATE_CLIENTS;
        BlockState floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState floor2 = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState ring = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState inner = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState gold = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        boolean[][] lemniscate = lemniscateMask(11.0);
        int r = RADIUS;
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) {
                    continue;
                }
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state;
                if (d > r - 1.5) {
                    state = ring;
                } else if (Math.abs(d - 24) < 0.55) {
                    state = ring;
                } else if (Math.abs(d - 17) < 0.5) {
                    state = inner;
                } else if (lemniscate[dx + 40][dz + 40]) {
                    state = gold;
                } else if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                } else {
                    state = ArenaTools.noise(dx, dz, 3) < 0.3 ? floor2 : floor;
                }
                level.setBlock(pos, state, flags);
                level.setBlock(pos.below(), Blocks.BLACKSTONE.defaultBlockState(), flags);
                if (d > r - 4 && ArenaTools.noise(dx, dz, 9) < 0.6) {
                    level.setBlock(pos.below(2), Blocks.BLACKSTONE.defaultBlockState(), flags);
                }
                if (d > r - 0.5) {
                    level.setBlock(pos.above(), wall, flags);
                }
            }
        }
        // os quatro cantos (NO = Terra, NE = Ar, SE = Água, SO = Fogo)
        for (int i = 0; i < 4; i++) {
            MagusElement element = MagusElement.CLASSIC[i];
            double base = Math.toRadians(225 + i * 90);
            for (int k = -1; k <= 1; k++) {
                double a = base + k * 0.28;
                BlockPos p = center.offset((int) Math.round(Math.cos(a) * (r - 3)), 1, (int) Math.round(Math.sin(a) * (r - 3)));
                corner(level, element, p, k == 0);
            }
        }
        // Mesa do Mago (norte), com as quatro ferramentas
        BlockPos table = center.offset(0, 1, -(r - 4));
        for (int dx = -2; dx <= 2; dx++) {
            level.setBlock(table.offset(dx, 0, 0), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), flags);
            level.setBlock(table.offset(dx, 1, 0), Blocks.DARK_OAK_SLAB.defaultBlockState(), flags);
        }
        level.setBlock(table.offset(-2, 2, 0), Blocks.MOSS_CARPET.defaultBlockState(), flags);        // pentáculo (terra)
        level.setBlock(table.offset(-1, 2, 0), Blocks.LIGHTNING_ROD.defaultBlockState(), flags);       // espada (ar)
        level.setBlock(table.offset(0, 2, 0), Blocks.ENCHANTING_TABLE.defaultBlockState(), flags);
        level.setBlock(table.offset(1, 2, 0), Blocks.DECORATED_POT.defaultBlockState(), flags);        // taça (água)
        level.setBlock(table.offset(2, 2, 0), Blocks.END_ROD.defaultBlockState(), flags);              // bastão (fogo)
    }

    /** Desenho do ∞ no chão (lemniscata), máscara 81×81 com o centro em [40][40]. */
    private static boolean[][] lemniscateMask(double a) {
        boolean[][] mask = new boolean[81][81];
        for (int i = 0; i < 720; i++) {
            double t = i / 720.0 * Math.PI * 2.0;
            double den = 1 + Math.sin(t) * Math.sin(t);
            int x = (int) Math.round(a * Math.cos(t) / den);
            int z = (int) Math.round(a * Math.sin(t) * Math.cos(t) / den * 1.3);
            mask[x + 40][z + 40] = true;
        }
        return mask;
    }

    private static void corner(ServerLevel level, MagusElement element, BlockPos p, boolean main) {
        int flags = Block.UPDATE_CLIENTS;
        int h = main ? 6 : 4;
        switch (element) {
            case EARTH -> {
                for (int y = 0; y < h; y++) {
                    level.setBlock(p.above(y), (y % 3 == 1 ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSSY_STONE_BRICKS).defaultBlockState(), flags);
                }
                level.setBlock(p.above(h), Blocks.AMETHYST_CLUSTER.defaultBlockState(), flags);
                level.setBlock(p.below(), Blocks.ROOTED_DIRT.defaultBlockState(), flags);
            }
            case AIR -> {
                for (int y = 0; y < h; y++) {
                    level.setBlock(p.above(y), Blocks.CALCITE.defaultBlockState(), flags);
                }
                level.setBlock(p.above(h), Blocks.END_ROD.defaultBlockState(), flags);
                if (main) {
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (Math.abs(dx) + Math.abs(dz) < 3) {
                                level.setBlock(p.offset(dx, h + 3, dz), Blocks.WHITE_WOOL.defaultBlockState(), flags);
                            }
                        }
                    }
                }
            }
            case WATER -> {
                for (int y = 0; y < h; y++) {
                    level.setBlock(p.above(y), (y == h - 1 ? Blocks.SEA_LANTERN : Blocks.DARK_PRISMARINE).defaultBlockState(), flags);
                }
                level.setBlock(p.above(h), Blocks.PRISMARINE_WALL.defaultBlockState(), flags);
            }
            case FIRE -> {
                for (int y = 0; y < h - 1; y++) {
                    level.setBlock(p.above(y), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), flags);
                }
                level.setBlock(p.above(h - 1), Blocks.MAGMA_BLOCK.defaultBlockState(), flags);
                level.setBlock(p.above(h), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), flags);
            }
            default -> {}
        }
    }

    // ----------------------------------------------------------------- durante a luta

    @Override
    public void tick(ServerLevel level, ArcanePlane.Fight fight, List<ServerPlayer> players) {
        Field field = FIELDS.get(fight.center());
        if (field == null) {
            return;
        }
        field.tick(level);
        // ∞ de partículas no céu
        if (fight.ticks() % 6 == 0) {
            BlockPos c = fight.center();
            for (int i = 0; i < 6; i++) {
                double t = (fight.ticks() * 0.02 + i / 6.0) * Math.PI * 2.0;
                double den = 1 + Math.sin(t) * Math.sin(t);
                level.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5 + 14 * Math.cos(t) / den, c.getY() + 38,
                        c.getZ() + 0.5 + 14 * Math.sin(t) * Math.cos(t) / den, 1, 0, 0, 0, 0);
            }
        }
        // efeitos do chão transformado
        if (field.has(MagusElement.AIR)) {
            double a = fight.ticks() * 0.004;
            Vec3 wind = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(0.035);
            for (ServerPlayer player : players) {
                if (player.isSpectator() || player.isShiftKeyDown() || !field.onPaintedFloor(player.position())) {
                    continue;
                }
                player.push(wind.x, 0.0, wind.z);
                player.hurtMarked = true;
            }
            if (fight.ticks() % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    Vec3 p = field.randomFloorPoint(level);
                    level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.3, p.z, 0, wind.x * 20, 0.0, wind.z * 20, 0.6);
                }
            }
        }
        if (field.has(MagusElement.FIRE) && fight.ticks() % 4 == 0) {
            for (int i = 0; i < 3; i++) {
                Vec3 p = field.randomFloorPoint(level);
                level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.2, p.z, 1, 0.2, 0.0, 0.2, 0.0);
            }
        }
        if (field.has(MagusElement.EARTH) && fight.ticks() % 5 == 0) {
            Vec3 p = field.randomFloorPoint(level);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y + 1.5, p.z, 2, 2.0, 1.0, 2.0, 0.0);
        }
        // queda de alto (do plano de cima) não machuca tanto: o Mago segura no ar
        if (field.upperBuilt) {
            for (ServerPlayer player : players) {
                if (player.fallDistance > 8 && !player.hasEffect(MobEffects.SLOW_FALLING)) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
                }
            }
        }
    }

    /** Caiu do salão: o Mago devolve o jogador (com um pouco de dano), para o plano onde ele estava. */
    @Override
    public boolean catchFall(ServerLevel level, ArcanePlane.Fight fight, ServerPlayer player) {
        BlockPos center = fight.center();
        Field field = FIELDS.get(center);
        boolean upper = field != null && field.upperBuilt && field.playersUpper;
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        Vec3 back = new Vec3(center.getX() + 0.5 + Math.cos(angle) * 8.0, center.getY() + (upper ? UPPER : 0) + 4.0,
                center.getZ() + 0.5 + Math.sin(angle) * 8.0);
        player.teleportTo(back.x, back.y, back.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, false, false, true));
        player.hurtServer(level, level.damageSources().magic(), FALL_DAMAGE);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, back.x, back.y, back.z, 40, 0.4, 1.0, 0.4, 0.05);
        level.playSound(null, BlockPos.containing(back), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.8F);
        player.sendOverlayMessage(Component.translatable("message.mistech.magus.caught"));
        return true;
    }

    // ================================================================= o chão que se molda

    /**
     * Estado da arena de uma luta: o que foi pintado (transformação do chão), blocos temporários e o plano de cima.
     * Guarda o bloco original de cada posição mexida para desfazer depois.
     */
    public static final class Field {
        public final BlockPos center;
        private final Map<BlockPos, BlockState> base = new HashMap<>();
        private final Map<BlockPos, BlockState> painted = new HashMap<>();
        private final Map<BlockPos, Long> temps = new HashMap<>();
        private final ArrayDeque<Change> queue = new ArrayDeque<>();
        private final List<MagusElement> active = new ArrayList<>();
        private int paintedY;
        private long time;
        public boolean upperBuilt;
        /** Fase IV: os jogadores estão no plano de cima? */
        public boolean playersUpper;

        private record Change(BlockPos pos, @Nullable BlockState state, boolean permanent) {}

        Field(BlockPos center) {
            this.center = center;
            this.paintedY = center.getY();
        }

        public boolean has(MagusElement element) {
            return this.active.contains(element);
        }

        public List<MagusElement> active() {
            return this.active;
        }

        public int floorY(boolean upper) {
            return this.center.getY() + (upper ? UPPER : 0);
        }

        boolean onPaintedFloor(Vec3 pos) {
            double dx = pos.x - (this.center.getX() + 0.5), dz = pos.z - (this.center.getZ() + 0.5);
            return pos.y - this.paintedY < 3.0 && pos.y - this.paintedY > 0.0 && dx * dx + dz * dz < PAINT_RADIUS * PAINT_RADIUS;
        }

        Vec3 randomFloorPoint(ServerLevel level) {
            double a = level.getRandom().nextDouble() * Math.PI * 2.0;
            double d = Math.sqrt(level.getRandom().nextDouble()) * PAINT_RADIUS;
            return new Vec3(this.center.getX() + 0.5 + Math.cos(a) * d, this.paintedY + 1.0, this.center.getZ() + 0.5 + Math.sin(a) * d);
        }

        public void tick(ServerLevel level) {
            this.time++;
            int budget = 420;
            while (budget-- > 0 && !this.queue.isEmpty()) {
                Change c = this.queue.poll();
                if (c.permanent) {
                    level.setBlock(c.pos, c.state == null ? Blocks.AIR.defaultBlockState() : c.state, Block.UPDATE_CLIENTS);
                } else if (c.state == null) {
                    this.unpaintAt(level, c.pos);
                } else {
                    this.paintAt(level, c.pos, c.state);
                }
            }
            if (!this.temps.isEmpty() && this.time % 2 == 0) {
                Iterator<Map.Entry<BlockPos, Long>> it = this.temps.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<BlockPos, Long> e = it.next();
                    if (e.getValue() <= this.time) {
                        it.remove();
                        BlockPos pos = e.getKey();
                        BlockState back = this.painted.containsKey(pos) ? this.painted.get(pos) : this.base.get(pos);
                        level.setBlock(pos, back == null ? Blocks.AIR.defaultBlockState() : back, Block.UPDATE_CLIENTS);
                        if (!this.painted.containsKey(pos)) {
                            this.base.remove(pos);
                        }
                        if (back != null && back.isAir() || back == null) {
                            level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
                        }
                    }
                }
            }
        }

        private void paintAt(ServerLevel level, BlockPos pos, BlockState state) {
            this.base.computeIfAbsent(pos, level::getBlockState);
            this.painted.put(pos, state);
            if (!this.temps.containsKey(pos)) {
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        }

        private void unpaintAt(ServerLevel level, BlockPos pos) {
            if (this.painted.remove(pos) == null) {
                return;
            }
            if (this.temps.containsKey(pos)) {
                return; // o temporário devolve o original quando acabar
            }
            BlockState orig = this.base.remove(pos);
            if (orig != null) {
                level.setBlock(pos, orig, Block.UPDATE_CLIENTS);
            }
        }

        private BlockState orig(ServerLevel level, BlockPos pos) {
            BlockState b = this.base.get(pos);
            return b != null ? b : level.getBlockState(pos);
        }

        /** Bloco temporário (some em {@code ticks}). Só ocupa ar, ou o próprio chão quando {@code floor}. */
        public boolean temp(ServerLevel level, BlockPos pos, BlockState state, int ticks, boolean floor) {
            BlockState now = level.getBlockState(pos);
            if (!floor && !now.isAir() && !now.is(Blocks.WATER) && !this.temps.containsKey(pos)) {
                return false;
            }
            if (floor && now.isAir()) {
                return false;
            }
            this.base.computeIfAbsent(pos, p -> now);
            this.temps.put(pos.immutable(), this.time + ticks);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            return true;
        }

        /** Transforma o chão do plano (de baixo ou de cima) nos elementos dados, em ondas a partir do centro. */
        public void paint(ServerLevel level, List<MagusElement> elements, boolean upper) {
            this.unpaintAll();
            this.active.clear();
            this.active.addAll(elements);
            if (elements.isEmpty()) {
                return;
            }
            int y = this.floorY(upper);
            this.paintedY = y;
            int radius = upper ? UPPER_RADIUS - 2 : PAINT_RADIUS;
            List<BlockPos> spots = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        spots.add(new BlockPos(this.center.getX() + dx, y, this.center.getZ() + dz));
                    }
                }
            }
            spots.sort(Comparator.comparingDouble(p -> p.distSqr(new BlockPos(this.center.getX(), y, this.center.getZ()))));
            int seed = (int) (this.time % 997);
            for (BlockPos pos : spots) {
                int dx = pos.getX() - this.center.getX(), dz = pos.getZ() - this.center.getZ();
                MagusElement e = elements.size() == 1 || ArenaTools.noise(dx / 3, dz / 3, seed + 17) < 0.5 ? elements.get(0) : elements.get(1);
                double n = ArenaTools.noise(dx, dz, seed);
                if (this.orig(level, pos).isAir()) {
                    continue;
                }
                BlockState above = this.orig(level, pos.above());
                this.queue.add(new Change(pos, floorFor(e, n, dx, dz), false));
                if (e == MagusElement.WATER && above.isAir() && dx * dx + dz * dz < (radius - 1) * (radius - 1)) {
                    this.queue.add(new Change(pos.above(), Blocks.WATER.defaultBlockState(), false));
                }
                // Terra: alguns pilares (cobertura para todos)
                if (e == MagusElement.EARTH && ArenaTools.noise(dx * 7, dz * 7, seed + 3) < 0.012 && dx * dx + dz * dz > 25 && above.isAir()) {
                    int h = 2 + (int) (ArenaTools.noise(dx, dz, seed + 5) * 3);
                    for (int k = 1; k <= h; k++) {
                        this.queue.add(new Change(pos.above(k), (k == h ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSSY_STONE_BRICKS).defaultBlockState(), false));
                    }
                }
            }
            level.playSound(null, this.center.atY(y), switch (elements.get(0)) {
                case EARTH -> SoundEvents.ROOTED_DIRT_PLACE;
                case AIR -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value();
                case WATER -> SoundEvents.AMBIENT_UNDERWATER_ENTER;
                case FIRE -> SoundEvents.BLAZE_SHOOT;
                default -> SoundEvents.AMETHYST_BLOCK_CHIME;
            }, SoundSource.HOSTILE, 3.0F, 0.6F);
        }

        /** Volta o chão ao normal (também em ondas). */
        public void unpaintAll() {
            this.active.clear();
            if (this.painted.isEmpty()) {
                return;
            }
            List<BlockPos> list = new ArrayList<>(this.painted.keySet());
            list.sort(Comparator.comparingDouble(p -> -p.distSqr(this.center.atY(p.getY()))));
            // primeiro o que está em cima (água, pilares), depois o chão
            list.sort(Comparator.comparingInt(p -> -p.getY()));
            for (BlockPos pos : list) {
                this.queue.add(new Change(pos, null, false));
            }
        }

        private static BlockState floorFor(MagusElement e, double n, int dx, int dz) {
            return switch (e) {
                case EARTH -> (n < 0.35 ? Blocks.COARSE_DIRT : n < 0.55 ? Blocks.ROOTED_DIRT : n < 0.82 ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSS_BLOCK).defaultBlockState();
                case AIR -> (n < 0.5 ? Blocks.CALCITE : n < 0.78 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA).defaultBlockState();
                case WATER -> (n < 0.45 ? Blocks.PRISMARINE_BRICKS : n < 0.75 ? Blocks.DARK_PRISMARINE : Blocks.PRISMARINE).defaultBlockState();
                case FIRE -> (n < 0.4 ? Blocks.BLACKSTONE : n < 0.68 ? Blocks.MAGMA_BLOCK : Blocks.NETHERRACK).defaultBlockState();
                case QUINTESSENCE -> (n < 0.5 ? Blocks.AMETHYST_BLOCK : Blocks.PURPUR_BLOCK).defaultBlockState();
            };
        }

        /** Fase IV: constrói o plano de cima (chão de vidro colorido com borda de ametista e mureta). */
        public void buildUpper(ServerLevel level) {
            if (this.upperBuilt) {
                return;
            }
            this.upperBuilt = true;
            int y = this.floorY(true);
            List<BlockPos> spots = new ArrayList<>();
            int r = UPPER_RADIUS;
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    if (dx * dx + dz * dz <= (r + 0.5) * (r + 0.5)) {
                        spots.add(new BlockPos(this.center.getX() + dx, y, this.center.getZ() + dz));
                    }
                }
            }
            spots.sort(Comparator.comparingDouble(p -> p.distSqr(this.center.atY(y))));
            boolean[][] lem = lemniscateMask(9.0);
            for (BlockPos pos : spots) {
                int dx = pos.getX() - this.center.getX(), dz = pos.getZ() - this.center.getZ();
                double d = Math.sqrt(dx * dx + dz * dz);
                BlockState s;
                if (d > r - 1.0) {
                    s = Blocks.AMETHYST_BLOCK.defaultBlockState();
                    this.queue.add(new Change(pos.above(), Blocks.PURPLE_STAINED_GLASS_PANE.defaultBlockState(), true));
                } else if (Math.abs(dx) < 40 && Math.abs(dz) < 40 && lem[dx + 40][dz + 40]) {
                    s = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
                } else {
                    s = ((dx + dz) & 3) == 0 ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState() : Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                }
                this.queue.add(new Change(pos, s, true));
            }
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                BlockPos lamp = new BlockPos(this.center.getX() + (int) Math.round(Math.cos(a) * (r - 3)), y - 1,
                        this.center.getZ() + (int) Math.round(Math.sin(a) * (r - 3)));
                this.queue.add(new Change(lamp, Blocks.SEA_LANTERN.defaultBlockState(), true));
            }
        }

        /** Partículas de aviso (círculo) no chão. */
        public static void warnCircle(ServerLevel level, Vec3 at, double radius, int color) {
            int n = (int) (radius * 8);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2.0 / n;
                level.sendParticles(new DustParticleOptions(color, 1.2F), at.x + Math.cos(a) * radius, at.y + 0.1, at.z + Math.sin(a) * radius,
                        1, 0, 0, 0, 0);
            }
        }
    }
}
