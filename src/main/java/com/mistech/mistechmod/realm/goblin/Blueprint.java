package com.mistech.mistechmod.realm.goblin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Lista de operações de construção em coordenadas do mundo. A estrutura é grande (várias chunks):
 * cada chunk aplica só a parte que cai dentro dela ({@link #apply}), então a mesma planta é
 * reaplicada chunk a chunk sem nada escapar para fora.
 */
public final class Blueprint {
    /** Escolhe um bloco para uma posição (sempre o mesmo para a mesma posição). */
    public interface Palette {
        BlockState pick(int x, int y, int z);

        static Palette of(BlockState state) {
            return (x, y, z) -> state;
        }

        /** Mistura por peso, estável por posição. */
        static Palette mix(long salt, Object... stateAndWeight) {
            int n = stateAndWeight.length / 2;
            BlockState[] states = new BlockState[n];
            int[] cumulative = new int[n];
            int total = 0;
            for (int i = 0; i < n; i++) {
                states[i] = (BlockState) stateAndWeight[i * 2];
                total += (Integer) stateAndWeight[i * 2 + 1];
                cumulative[i] = total;
            }
            final int sum = total;
            return (x, y, z) -> {
                int r = (int) Math.floorMod(hash(x, y, z, salt), (long) sum);
                for (int i = 0; i < n; i++) {
                    if (r < cumulative[i]) {
                        return states[i];
                    }
                }
                return states[n - 1];
            };
        }
    }

    public interface Shape {
        boolean contains(int x, int y, int z);
    }

    /** Contexto de uma chunk sendo decorada. */
    public static final class Ctx {
        public final WorldGenLevel level;
        public final BoundingBox chunk;
        public final RandomSource random;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        Ctx(WorldGenLevel level, BoundingBox chunk, RandomSource random) {
            this.level = level;
            this.chunk = chunk;
            this.random = random;
        }

        public boolean inside(int x, int y, int z) {
            return this.chunk.isInside(x, y, z);
        }

        public void set(int x, int y, int z, BlockState state) {
            if (!this.chunk.isInside(x, y, z)) {
                return;
            }
            this.cursor.set(x, y, z);
            this.level.setBlock(this.cursor, state, 2);
            var block = state.getBlock();
            if (block instanceof FenceBlock || block instanceof WallBlock || block instanceof IronBarsBlock || block instanceof StairBlock) {
                this.level.getChunk(this.cursor).markPosForPostprocessing(this.cursor);
            }
        }

        public BlockState get(int x, int y, int z) {
            return this.level.getBlockState(this.cursor.set(x, y, z));
        }
    }

    private interface Op {
        BoundingBox box();

        void apply(Ctx ctx);
    }

    private final List<Op> ops = new ArrayList<>();
    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    public static long hash(int x, int y, int z, long salt) {
        long h = salt * 0x9E3779B97F4A7C15L + x * 0x632BE59BD9B4E019L + y * 0x85157AF5L + z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return h;
    }

    public static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mistech.MODID, path));
    }

    private void grow(BoundingBox b) {
        this.minX = Math.min(this.minX, b.minX());
        this.minY = Math.min(this.minY, b.minY());
        this.minZ = Math.min(this.minZ, b.minZ());
        this.maxX = Math.max(this.maxX, b.maxX());
        this.maxY = Math.max(this.maxY, b.maxY());
        this.maxZ = Math.max(this.maxZ, b.maxZ());
    }

    private void add(Op op) {
        this.ops.add(op);
        this.grow(op.box());
    }

    public BoundingBox bounds() {
        return new BoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    // ------------------------------------------------------------------ operações

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Palette palette) {
        this.shape(x0, y0, z0, x1, y1, z1, (x, y, z) -> true, palette);
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        this.fill(x0, y0, z0, x1, y1, z1, Palette.of(state));
    }

    public void air(int x0, int y0, int z0, int x1, int y1, int z1) {
        this.fill(x0, y0, z0, x1, y1, z1, Blocks.CAVE_AIR.defaultBlockState());
    }

    /** Preenche só onde {@code shape} diz, dentro da caixa. */
    public void shape(int x0, int y0, int z0, int x1, int y1, int z1, Shape shape, Palette palette) {
        BoundingBox box = new BoundingBox(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1));
        this.add(new Op() {
            @Override
            public BoundingBox box() {
                return box;
            }

            @Override
            public void apply(Ctx ctx) {
                int ax = Math.max(box.minX(), ctx.chunk.minX()), bx = Math.min(box.maxX(), ctx.chunk.maxX());
                int ay = Math.max(box.minY(), ctx.chunk.minY()), by = Math.min(box.maxY(), ctx.chunk.maxY());
                int az = Math.max(box.minZ(), ctx.chunk.minZ()), bz = Math.min(box.maxZ(), ctx.chunk.maxZ());
                for (int x = ax; x <= bx; x++) {
                    for (int z = az; z <= bz; z++) {
                        for (int y = ay; y <= by; y++) {
                            if (shape.contains(x, y, z)) {
                                ctx.set(x, y, z, palette.pick(x, y, z));
                            }
                        }
                    }
                }
            }
        });
    }

    public void block(int x, int y, int z, BlockState state) {
        this.fill(x, y, z, x, y, z, state);
    }

    /** Operação livre numa caixa (para coisas que precisam ler o mundo, como o aplainamento do terreno). */
    public void custom(BoundingBox box, Consumer<Ctx> action) {
        this.add(new Op() {
            @Override
            public BoundingBox box() {
                return box;
            }

            @Override
            public void apply(Ctx ctx) {
                action.accept(ctx);
            }
        });
    }

    public void chest(int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        this.custom(new BoundingBox(x, y, z, x, y, z), ctx -> {
            if (!ctx.inside(x, y, z)) {
                return;
            }
            ctx.set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
            RandomizableContainer.setBlockEntityLootTable(ctx.level, ctx.random, new BlockPos(x, y, z), loot);
        });
    }

    public void dispenser(int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        this.custom(new BoundingBox(x, y, z, x, y, z), ctx -> {
            if (!ctx.inside(x, y, z)) {
                return;
            }
            ctx.set(x, y, z, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing));
            RandomizableContainer.setBlockEntityLootTable(ctx.level, ctx.random, new BlockPos(x, y, z), loot);
        });
    }

    public void spawner(int x, int y, int z, Supplier<? extends EntityType<?>> type) {
        this.custom(new BoundingBox(x, y, z, x, y, z), ctx -> {
            if (!ctx.inside(x, y, z)) {
                return;
            }
            ctx.set(x, y, z, Blocks.SPAWNER.defaultBlockState());
            if (ctx.level.getBlockEntity(new BlockPos(x, y, z)) instanceof SpawnerBlockEntity spawner) {
                spawner.setEntityId(type.get(), ctx.random);
            }
        });
    }

    /** Criatura persistente que nasce com a estrutura. */
    public <T extends Mob> void mob(double x, double y, double z, Supplier<EntityType<T>> type, @Nullable Consumer<T> setup) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        this.custom(new BoundingBox(bx, by, bz, bx, by, bz), ctx -> {
            if (!ctx.inside(bx, by, bz)) {
                return;
            }
            T mob = type.get().create(ctx.level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (mob == null) {
                return;
            }
            mob.snapTo(x, y, z, ctx.random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            mob.finalizeSpawn(ctx.level, ctx.level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.STRUCTURE, null);
            if (setup != null) {
                setup.accept(mob);
            }
            ctx.level.addFreshEntityWithPassengers(mob);
        });
    }

    /** Altura do topo sólido do terreno natural nesta coluna (só vale dentro da chunk que está sendo gerada). */
    public static int surface(Ctx ctx, int x, int z) {
        return ctx.level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    // ------------------------------------------------------------------ aplicar

    public void apply(WorldGenLevel level, BoundingBox chunk, RandomSource random) {
        Ctx ctx = new Ctx(level, chunk, random);
        for (Op op : this.ops) {
            if (op.box().intersects(chunk)) {
                op.apply(ctx);
            }
        }
    }
}
