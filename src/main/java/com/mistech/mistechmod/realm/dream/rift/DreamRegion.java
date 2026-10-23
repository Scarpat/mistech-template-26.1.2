package com.mistech.mistechmod.realm.dream.rift;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.DreamRealmContent;
import com.mistech.mistechmod.registry.RealmContent;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Regiões do Sonho que um Fragmento de Sonho consegue trazer para a Superfície: o bioma (com as criaturas dele) e o chão
 * que a fenda espalha. Os blocos imitam as regras de superfície do {@code gnome_realm.json}.
 */
public enum DreamRegion {
    // ----- Sonho Comum
    GNOME_MEADOW("gnome_meadow", 0x7AD870, () -> Blocks.GRASS_BLOCK.defaultBlockState(), DreamRegion::dirt),
    SWEET_MUSHROOM_GROVE("sweet_mushroom_grove", 0xF08AC8, () -> DreamRealmContent.SWEET_MYCELIUM.get().defaultBlockState(), DreamRegion::dirt),
    PILLOW_HILLS("pillow_hills", 0xF0F0FA, () -> DreamRealmContent.CLOUD_WOOL.get().defaultBlockState(),
            () -> DreamRealmContent.CLOUD_WOOL.get().defaultBlockState()),
    // ----- Pesadelo
    MURKY_WASTES("murky_wastes", 0x6A6A50, DreamRegion::withered, DreamRegion::dirt, 0.15F, () -> Blocks.COARSE_DIRT.defaultBlockState()),
    HAND_FOREST("hand_forest", 0x9A5048, DreamRegion::withered, DreamRegion::dirt),
    FALLING_CHASMS("falling_chasms", 0x30305A, DreamRegion::withered, DreamRegion::dirt, 0.2F, () -> Blocks.GRAVEL.defaultBlockState()),
    WRONG_NEIGHBORHOOD("wrong_neighborhood", 0xD8C060, DreamRegion::withered, DreamRegion::dirt, 0.3F,
            () -> Blocks.GRAVEL.defaultBlockState(), () -> Blocks.COARSE_DIRT.defaultBlockState()),
    // ----- Sonho Profundo
    CLOUD_SEA("cloud_sea", 0xA0D4F8, () -> Blocks.GRASS_BLOCK.defaultBlockState(), DreamRegion::dirt, 0.2F,
            () -> DreamRealmContent.CLOUD_WOOL.get().defaultBlockState()),
    FRACTAL_GARDEN("fractal_garden", 0xF060D8, () -> DreamRealmContent.PRISMATIC_GRASS.get().defaultBlockState(), DreamRegion::dirt),
    INVERTED_FOREST("inverted_forest", 0x80D8C8, () -> Blocks.GRASS_BLOCK.defaultBlockState(), DreamRegion::dirt),
    // ----- Sono REM
    STARRY_VOID("starry_void", 0x7A8AE0, () -> DreamRealmContent.STAR_MOSS.get().defaultBlockState(),
            () -> DreamRealmContent.STAR_STONE.get().defaultBlockState(), 0.3F, () -> DreamRealmContent.STAR_STONE.get().defaultBlockState()),
    MEMORY_QUILT("memory_quilt", 0xE8D0A0, () -> Blocks.GRASS_BLOCK.defaultBlockState(), DreamRegion::dirt, 0.55F,
            () -> Blocks.SAND.defaultBlockState(), () -> Blocks.SNOW_BLOCK.defaultBlockState(), () -> Blocks.PODZOL.defaultBlockState(),
            () -> Blocks.RED_SAND.defaultBlockState()),
    ABYSSAL_GAZE("abyssal_gaze", 0xD06AB0, () -> Blocks.GRASS_BLOCK.defaultBlockState(), DreamRegion::dirt, 0.25F,
            () -> Blocks.COARSE_DIRT.defaultBlockState());

    private final String id;
    private final int color;
    private final Supplier<BlockState> top;
    private final Supplier<BlockState> filler;
    /** Fração do chão coberta pelos remendos (blocos alternativos em manchas de 3×3). */
    private final float patchChance;
    private final Supplier<BlockState>[] patches;
    private final ResourceKey<Biome> biome;

    @SafeVarargs
    DreamRegion(String id, int color, Supplier<BlockState> top, Supplier<BlockState> filler, float patchChance, Supplier<BlockState>... patches) {
        this.id = id;
        this.color = color;
        this.top = top;
        this.filler = filler;
        this.patchChance = patchChance;
        this.patches = patches;
        this.biome = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mistech.MODID, id));
    }

    DreamRegion(String id, int color, Supplier<BlockState> top, Supplier<BlockState> filler) {
        this(id, color, top, filler, 0.0F);
    }

    private static BlockState dirt() {
        return Blocks.DIRT.defaultBlockState();
    }

    private static BlockState withered() {
        return RealmContent.WITHERED_GRASS.get().defaultBlockState();
    }

    public String id() {
        return this.id;
    }

    /** Cor das partículas da fenda e do chão sendo convertido. */
    public int color() {
        return this.color;
    }

    public ResourceKey<Biome> biome() {
        return this.biome;
    }

    public String translationKey() {
        return "biome.mistech." + this.id;
    }

    /** Bloco do topo do chão em (x, z): o principal ou um remendo (manchas fixas de 3×3, sem depender da semente). */
    public BlockState topAt(int x, int z) {
        if (this.patches.length > 0) {
            long h = Mth.getSeed(Math.floorDiv(x, 3), 17, Math.floorDiv(z, 3));
            float u = (float) ((h >>> 16) & 0xFFFF) / 65536.0F;
            if (u < this.patchChance) {
                return this.patches[(int) ((h >>> 40) & 0xFFFF) % this.patches.length].get();
            }
        }
        return this.top.get();
    }

    public BlockState filler() {
        return this.filler.get();
    }

    /** O bloco é um chão que alguma região do sonho espalha (para poder re-sonhar uma área já convertida). */
    public static boolean isDreamGround(BlockState state) {
        for (DreamRegion region : values()) {
            if (state.is(region.top.get().getBlock()) || state.is(region.filler.get().getBlock())) {
                return true;
            }
            for (Supplier<BlockState> patch : region.patches) {
                if (state.is(patch.get().getBlock())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static DreamRegion random(RandomSource random) {
        DreamRegion[] all = values();
        return all[random.nextInt(all.length)];
    }

    public static @Nullable DreamRegion byId(String id) {
        for (DreamRegion region : values()) {
            if (region.id.equals(id)) {
                return region;
            }
        }
        return null;
    }
}
