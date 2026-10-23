package com.mistech.mistechmod.realm.dream.rift;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamRiftContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Terraformação da Fenda Onírica. A fenda abre ({@link #OPEN_TICKS}), depois o Sonho se espalha em anéis a partir dela
 * (um anel a cada {@link #STEP_TICKS}): tira árvores e plantas da Superfície e troca o chão pelo da região. No fim o bioma
 * da área vira o bioma do Sonho (com as criaturas, o céu e a névoa dele), a vegetação da região brota, algumas criaturas
 * aparecem e a fenda fecha. A borda é ondulada e esfarrapada para não parecer um círculo perfeito.
 */
public class DreamRiftBlockEntity extends BlockEntity {
    public static final int OPEN_TICKS = 40;
    public static final int STEP_TICKS = 6;
    /** Quanto abaixo do chão mais baixo o bioma também muda (cavernas rasas). */
    private static final int BIOME_DEPTH = 12;

    private DreamRegion region = DreamRegion.GNOME_MEADOW;
    private int radius = 12;
    private int age;
    private long seed;
    private int minGround = Integer.MAX_VALUE;
    private @Nullable UUID owner;

    public DreamRiftBlockEntity(BlockPos pos, BlockState state) {
        super(DreamRiftContent.DREAM_RIFT_BE.get(), pos, state);
    }

    void setup(DreamRegion region, int radius, long seed, @Nullable UUID owner) {
        this.region = region;
        this.radius = radius;
        this.seed = seed;
        this.owner = owner;
        this.age = 0;
        this.minGround = Integer.MAX_VALUE;
        this.setChanged();
    }

    public DreamRegion region() {
        return this.region;
    }

    // ------------------------------------------------------------------ forma da área

    /** Raio relativo (≈0,84..1,16) na direção de (dx, dz): borda ondulada, fixa para esta fenda. */
    private double wobble(double dx, double dz) {
        double a = Math.atan2(dz, dx);
        double p = (this.seed & 0xFFFF) / 65536.0 * Mth.TWO_PI;
        return 1.0 + 0.10 * Math.sin(3.0 * a + p) + 0.06 * Math.sin(7.0 * a + p * 2.0);
    }

    /** Distância normalizada (0 no centro, 1 na borda) da coluna (x, z). */
    private double norm(int x, int z) {
        double dx = x + 0.5 - (this.worldPosition.getX() + 0.5);
        double dz = z + 0.5 - (this.worldPosition.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) / (this.radius * this.wobble(dx, dz));
    }

    /** A coluna entra no Sonho? Na borda (últimos 15%) só algumas, para a transição ficar esfarrapada. */
    private boolean inside(int x, int z) {
        double t = this.norm(x, z);
        if (t > 1.0) {
            return false;
        }
        if (t <= 0.85) {
            return true;
        }
        long h = Mth.getSeed(x, (int) this.seed, z);
        float u = (float) ((h >>> 16) & 0xFFFF) / 65536.0F;
        return u < (1.0 - t) / 0.15;
    }

    private int extent() {
        return Mth.ceil(this.radius * 1.17) + 1;
    }

    // ------------------------------------------------------------------ tick

    static void serverTick(Level level, BlockPos pos, BlockState state, DreamRiftBlockEntity rift) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        rift.age++;
        if (rift.age % 4 == 0) {
            int c = rift.region.color();
            server.sendParticles(new DustParticleOptions(c, 1.6F), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.4, 0.8, 0.4, 0.0);
        }
        if (rift.age <= OPEN_TICKS) {
            if (rift.age == OPEN_TICKS) {
                server.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.4F, 1.8F);
            }
            return;
        }
        int t = rift.age - OPEN_TICKS;
        if (t % STEP_TICKS != 0) {
            return;
        }
        int step = t / STEP_TICKS;
        if (step <= rift.radius) {
            rift.spreadRing(server, step);
            if (step % 3 == 0) {
                server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7F, 0.6F + step * 0.03F);
            }
            rift.setChanged();
        } else {
            rift.finish(server);
        }
    }

    /** Converte as colunas do anel {@code step} (distância normalizada entre (step-1)/R e step/R). */
    private void spreadRing(ServerLevel level, int step) {
        int e = this.extent();
        int cx = this.worldPosition.getX();
        int cz = this.worldPosition.getZ();
        RandomSource random = level.getRandom();
        for (int x = cx - e; x <= cx + e; x++) {
            for (int z = cz - e; z <= cz + e; z++) {
                double r = this.norm(x, z) * this.radius;
                int ring = Math.max(1, Mth.ceil(r));
                if (ring != step || !this.inside(x, z) || (x == cx && z == cz)) {
                    continue;
                }
                int ground = this.convertColumn(level, x, z);
                if (ground != Integer.MIN_VALUE) {
                    this.minGround = Math.min(this.minGround, ground);
                    if (random.nextInt(4) == 0) {
                        level.sendParticles(new DustParticleOptions(this.region.color(), 1.2F), x + 0.5, ground + 1.2, z + 0.5, 1,
                                0.25, 0.1, 0.25, 0.0);
                    }
                    if (random.nextInt(12) == 0) {
                        level.sendParticles(ParticleTypes.END_ROD, x + 0.5, ground + 1.3, z + 0.5, 1, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }
        }
    }

    private static boolean isPlant(BlockState s) {
        return s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.BAMBOO) || s.is(Blocks.CACTUS) || s.is(Blocks.BROWN_MUSHROOM)
                || s.is(Blocks.RED_MUSHROOM) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON)
                || s.is(BlockTags.BEEHIVES) || s.is(Blocks.COCOA) || s.is(Blocks.MOSS_CARPET);
    }

    /** Chão natural que pode virar sonho (nunca blocos de construção). */
    private static boolean isNaturalGround(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.TERRACOTTA)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.CLAY) || s.is(Blocks.POWDER_SNOW)
                || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || DreamRegion.isDreamGround(s);
    }

    /**
     * Limpa a vegetação da coluna (folhas, troncos debaixo de folhas, plantas) e troca o chão.
     * @return altura do chão, ou {@link Integer#MIN_VALUE} se a coluna é água/construção (só o bioma muda).
     */
    private int convertColumn(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int bottom = Math.max(level.getMinY(), top - 48);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        boolean underLeaves = false;
        int y = top;
        BlockState state = Blocks.AIR.defaultBlockState();
        for (; y >= bottom; y--) {
            state = level.getBlockState(m.set(x, y, z));
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                return Integer.MIN_VALUE;
            }
            if (state.is(BlockTags.LEAVES)) {
                underLeaves = true;
                level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }
            if (state.is(BlockTags.LOGS) && underLeaves) {
                level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }
            if (isPlant(state)) {
                level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }
            break;
        }
        if (y < bottom || !isNaturalGround(state)) {
            return Integer.MIN_VALUE;
        }
        level.setBlock(m.set(x, y, z), this.region.topAt(x, z), Block.UPDATE_CLIENTS);
        BlockState filler = this.region.filler();
        for (int d = 1; d <= 3; d++) {
            BlockState below = level.getBlockState(m.set(x, y - d, z));
            if (!isNaturalGround(below) || below.is(BlockTags.BASE_STONE_OVERWORLD) && d > 1) {
                break;
            }
            level.setBlock(m, filler, Block.UPDATE_CLIENTS);
        }
        return y;
    }

    // ------------------------------------------------------------------ fim

    private void finish(ServerLevel level) {
        Optional<Holder.Reference<Biome>> found = level.registryAccess().lookupOrThrow(Registries.BIOME).get(this.region.biome());
        BlockPos pos = this.worldPosition;
        if (found.isPresent()) {
            Holder<Biome> biome = found.get();
            List<LevelChunk> chunks = this.paintBiome(level, biome);
            this.growVegetation(level, biome, chunks);
            NaturalSpawner.spawnMobsForChunkGeneration(level, biome, ChunkPos.containing(pos),
                    RandomSource.create(this.seed ^ 0x5EED));
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.2F, 0.5F);
        level.playSound(null, pos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 60, 0.4, 0.9, 0.4, 0.2);
        level.sendParticles(new DustParticleOptions(this.region.color(), 2.0F), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40,
                0.6, 1.0, 0.6, 0.0);
        if (this.owner != null && level.getPlayerByUUID(this.owner) instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.translatable("message.mistech.dream_fragment.done",
                    Component.translatable(this.region.translationKey())));
        }
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Troca o bioma das células (4×4×4) cujo centro está dentro da área, do chão −{@link #BIOME_DEPTH} até o topo do mundo. */
    private List<LevelChunk> paintBiome(ServerLevel level, Holder<Biome> biome) {
        int e = this.extent();
        int cx = this.worldPosition.getX();
        int cz = this.worldPosition.getZ();
        int floor = this.minGround == Integer.MAX_VALUE ? this.worldPosition.getY() - 1 : this.minGround;
        int minQy = (floor - BIOME_DEPTH) >> 2;
        List<LevelChunk> chunks = new ArrayList<>();
        for (int chunkX = (cx - e) >> 4; chunkX <= (cx + e) >> 4; chunkX++) {
            for (int chunkZ = (cz - e) >> 4; chunkZ <= (cz + e) >> 4; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                boolean[] changed = {false};
                chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
                    int bx = (qx << 2) + 2;
                    int bz = (qz << 2) + 2;
                    if (qy >= minQy && this.norm(bx, bz) <= 0.97) {
                        changed[0] = true;
                        return biome;
                    }
                    return chunk.getNoiseBiome(qx, qy, qz);
                }, level.getChunkSource().randomState().sampler());
                if (changed[0]) {
                    chunk.markUnsaved();
                    chunks.add(chunk);
                }
            }
        }
        if (!chunks.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(new ArrayList<ChunkAccess>(chunks));
        }
        return chunks;
    }

    /**
     * Roda a vegetação do bioma do Sonho em cada chunk tocado. O filtro de bioma das features faz com que só brotem
     * dentro da área (onde o bioma já mudou). Features sem filtro de bioma (que dependem das ilhas do Sonho) ficam de fora.
     */
    private void growVegetation(ServerLevel level, Holder<Biome> biome, List<LevelChunk> chunks) {
        List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
        int index = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        if (steps.size() <= index) {
            return;
        }
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        for (LevelChunk chunk : chunks) {
            ChunkPos cp = chunk.getPos();
            RandomSource random = RandomSource.create(this.seed ^ cp.pack() * 31L);
            BlockPos origin = new BlockPos(cp.getMinBlockX(), level.getMinY(), cp.getMinBlockZ());
            for (Holder<PlacedFeature> feature : steps.get(index)) {
                if (feature.value().placement().stream().noneMatch(p -> p instanceof BiomeFilter)) {
                    continue;
                }
                try {
                    feature.value().placeWithBiomeCheck(level, generator, random, origin);
                } catch (RuntimeException ignored) {
                    // uma feature que não gosta da Superfície não pode derrubar a fenda
                }
            }
        }
    }

    // ------------------------------------------------------------------ dados

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        DreamRegion r = DreamRegion.byId(input.getStringOr("Region", ""));
        this.region = r != null ? r : DreamRegion.GNOME_MEADOW;
        this.radius = input.getIntOr("Radius", 12);
        this.age = input.getIntOr("Age", 0);
        this.seed = input.getLongOr("Seed", 0L);
        this.minGround = input.getIntOr("MinGround", Integer.MAX_VALUE);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Region", this.region.id());
        output.putInt("Radius", this.radius);
        output.putInt("Age", this.age);
        output.putLong("Seed", this.seed);
        output.putInt("MinGround", this.minGround);
        if (this.owner != null) {
            output.store("Owner", UUIDUtil.CODEC, this.owner);
        }
    }
}
