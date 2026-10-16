package com.mistech.mistechmod.realm.dream;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.mistech.mistechmod.registry.DreamRealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;

/**
 * Árvores Fractais do Jardim Fractal (e da Muda Fractal). Dois formatos:
 * <ul>
 * <li><b>Fractal</b> (65%): tronco com raízes, que se divide em 3 galhos, cada galho em 2-3 menores (×0,7 de comprimento),
 * três gerações; cada ponta termina numa bola de Folhas Fractais.</li>
 * <li><b>Bulbo</b> (35%): tronco fino em curva com uma copa achatada grande e uma menor em cima, e galhos curtos saindo
 * para os lados com bolinhas de folhas.</li>
 * </ul>
 * As folhas recebem a distância certa até o tronco (decaem normalmente quando cortam a árvore); as que ficam a mais de 6
 * blocos viram persistentes.
 */
public class FractalTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final double MAX_REACH = 13.0;

    public FractalTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** Planta em construção: troncos (com eixo) e folhas, gravados só no fim. */
    private static final class Plan {
        final BlockPos origin;
        final Map<BlockPos, Direction.Axis> logs = new HashMap<>();
        final Set<BlockPos> leaves = new LinkedHashSet<>();

        Plan(BlockPos origin) {
            this.origin = origin;
        }

        boolean inReach(BlockPos p) {
            double dx = p.getX() - this.origin.getX(), dz = p.getZ() - this.origin.getZ();
            return dx * dx + dz * dz <= MAX_REACH * MAX_REACH;
        }

        void log(BlockPos p, Direction.Axis axis) {
            if (inReach(p)) {
                this.logs.put(p, axis);
                this.leaves.remove(p);
            }
        }

        void leaf(BlockPos p) {
            if (inReach(p) && !this.logs.containsKey(p)) {
                this.leaves.add(p);
            }
        }

        /** Linha de troncos de {@code from} até {@code to}, sempre ligada pelas faces. */
        BlockPos line(Vec3 from, Vec3 to) {
            Vec3 d = to.subtract(from);
            Direction.Axis axis = Math.abs(d.y) >= Math.max(Math.abs(d.x), Math.abs(d.z)) ? Direction.Axis.Y
                    : Math.abs(d.x) >= Math.abs(d.z) ? Direction.Axis.X : Direction.Axis.Z;
            int steps = Math.max(1, (int) Math.ceil(d.length() * 2.0));
            BlockPos last = BlockPos.containing(from);
            log(last, axis);
            for (int i = 1; i <= steps; i++) {
                BlockPos p = BlockPos.containing(from.add(d.scale(i / (double) steps)));
                if (p.equals(last)) {
                    continue;
                }
                // liga pelas faces quando o passo muda mais de um eixo
                if (p.getX() != last.getX() && (p.getY() != last.getY() || p.getZ() != last.getZ())) {
                    log(new BlockPos(p.getX(), last.getY(), last.getZ()), axis);
                }
                if (p.getY() != last.getY() && p.getZ() != last.getZ()) {
                    log(new BlockPos(p.getX(), p.getY(), last.getZ()), axis);
                }
                log(p, axis);
                last = p;
            }
            return last;
        }

        void blob(BlockPos center, double rx, double ry, RandomSource random) {
            int ix = Mth.ceil(rx), iy = Mth.ceil(ry);
            for (int dx = -ix; dx <= ix; dx++) {
                for (int dy = -iy; dy <= iy; dy++) {
                    for (int dz = -ix; dz <= ix; dz++) {
                        double d = (dx * dx + dz * dz) / (rx * rx) + dy * dy / (ry * ry);
                        if (d <= 1.0 && (d < 0.7 || random.nextFloat() < 0.75F)) {
                            leaf(center.offset(dx, dy, dz));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!DreamFeatures.soil(level.getBlockState(origin.below()))) {
            return false;
        }
        Plan plan = new Plan(origin);
        if (random.nextFloat() < 0.65F) {
            fractal(plan, random);
        } else {
            bulb(plan, random);
        }
        // o tronco precisa de espaço
        for (BlockPos p : plan.logs.keySet()) {
            if (!p.equals(origin) && !free(level, p)) {
                return false;
            }
        }
        write(level, plan);
        return true;
    }

    private static boolean free(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES);
    }

    // ------------------------------------------------------------------ formatos

    private static void fractal(Plan plan, RandomSource random) {
        Vec3 base = Vec3.atBottomCenterOf(plan.origin);
        int height = 5 + random.nextInt(4);
        Vec3 top = base.add((random.nextDouble() - 0.5) * 1.5, height, (random.nextDouble() - 0.5) * 1.5);
        plan.line(base, top);
        // raízes
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.75F) {
                plan.log(plan.origin.relative(side), side.getAxis());
            }
        }
        double first = random.nextDouble() * Math.PI * 2.0;
        int branches = 3;
        for (int i = 0; i < branches; i++) {
            double yaw = first + i * Math.PI * 2.0 / branches + (random.nextDouble() - 0.5) * 0.5;
            Vec3 dir = direction(yaw, 0.85 + random.nextDouble() * 0.2);
            branch(plan, random, top, dir, 5.0 + random.nextDouble() * 1.5, 2);
        }
        plan.blob(BlockPos.containing(top.add(0, 1.5, 0)), 2.2, 1.6, random);
    }

    /** Galho recursivo: anda {@code length} na direção e se divide; na última geração vira uma bola de folhas. */
    private static void branch(Plan plan, RandomSource random, Vec3 start, Vec3 dir, double length, int depth) {
        Vec3 end = start.add(dir.scale(length));
        BlockPos tip = plan.line(start, end);
        if (depth <= 0) {
            plan.blob(tip.above(), 2.4 + random.nextDouble() * 0.6, 1.8, random);
            return;
        }
        int children = random.nextFloat() < 0.4F ? 3 : 2;
        double yaw0 = Math.atan2(dir.z, dir.x);
        for (int i = 0; i < children; i++) {
            double spread = (i - (children - 1) / 2.0) * 0.9 + (random.nextDouble() - 0.5) * 0.3;
            double pitch = Math.asin(Mth.clamp(dir.y, -1.0, 1.0)) * 0.7 + 0.35;
            Vec3 child = direction(yaw0 + spread, Math.sin(Mth.clamp(pitch, 0.2, 1.3)));
            branch(plan, random, end, child, length * 0.7, depth - 1);
        }
        if (depth == 1) {
            plan.blob(tip, 1.6, 1.3, random);
        }
    }

    /** Vetor unitário com o rumo {@code yaw} e componente vertical {@code up} (0..1). */
    private static Vec3 direction(double yaw, double up) {
        double flat = Math.sqrt(Math.max(0.0, 1.0 - up * up));
        return new Vec3(Math.cos(yaw) * flat, up, Math.sin(yaw) * flat);
    }

    private static void bulb(Plan plan, RandomSource random) {
        Vec3 base = Vec3.atBottomCenterOf(plan.origin);
        int height = 6 + random.nextInt(4);
        double bendYaw = random.nextDouble() * Math.PI * 2.0;
        Vec3 last = base;
        for (int y = 1; y <= height; y++) {
            double bend = Math.sin(y / (double) height * Math.PI) * 1.6;
            Vec3 p = base.add(Math.cos(bendYaw) * bend, y, Math.sin(bendYaw) * bend);
            plan.line(last, p);
            last = p;
        }
        BlockPos top = BlockPos.containing(last);
        plan.blob(top.above(), 4.2 + random.nextDouble(), 2.2, random);
        plan.blob(top.above(3), 2.6, 1.6, random);
        plan.log(top.above(), Direction.Axis.Y);
        int arms = 2 + random.nextInt(2);
        for (int i = 0; i < arms; i++) {
            double yaw = random.nextDouble() * Math.PI * 2.0;
            Vec3 from = base.add(0, height * (0.45 + random.nextDouble() * 0.2), 0)
                    .add(Math.cos(bendYaw) * 1.2, 0, Math.sin(bendYaw) * 1.2);
            Vec3 to = from.add(direction(yaw, 0.5).scale(3.5));
            BlockPos tip = plan.line(from, to);
            plan.blob(tip.above(), 1.8, 1.3, random);
        }
    }

    // ------------------------------------------------------------------ gravar

    private static void write(WorldGenLevel level, Plan plan) {
        BlockState log = DreamRealmContent.FRACTAL_LOG.get().defaultBlockState();
        for (Map.Entry<BlockPos, Direction.Axis> e : plan.logs.entrySet()) {
            if (free(level, e.getKey()) || e.getKey().equals(plan.origin)) {
                level.setBlock(e.getKey(), log.setValue(RotatedPillarBlock.AXIS, e.getValue()), Block.UPDATE_CLIENTS);
            }
        }
        // distância das folhas até o tronco (como as árvores do jogo)
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos p : plan.logs.keySet()) {
            distance.put(p, 0);
            queue.add(p);
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = distance.get(p);
            if (d >= LeavesBlock.DECAY_DISTANCE - 1) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (plan.leaves.contains(n) && !distance.containsKey(n)) {
                    distance.put(n, d + 1);
                    queue.add(n);
                }
            }
        }
        BlockState leaves = DreamRealmContent.FRACTAL_LEAVES.get().defaultBlockState();
        for (BlockPos p : plan.leaves) {
            if (!level.getBlockState(p).isAir()) {
                continue;
            }
            Integer d = distance.get(p);
            BlockState state = d == null ? leaves.setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, LeavesBlock.DECAY_DISTANCE)
                    : leaves.setValue(LeavesBlock.DISTANCE, Math.max(1, d));
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }
}
