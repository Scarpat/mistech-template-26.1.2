package com.mistech.mistechmod.realm;

import com.mistech.mistechmod.entity.GnomeEntity;
import com.mistech.mistechmod.entity.GnomeKingEntity;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.RealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Constrói (uma vez) a Casa do Rei no centro da Dimensão dos Gnomos: casa grande com telhado de chapéu de gnomo,
 * sala do trono com o Rei, camas (para ir embora dormindo), escada em caracol até a mina dos gnomos
 * (quatro túneis com trilhos, paredes cheias de minério e baús), e gnomos em volta e na mina.
 * Marca de "já construído": uma pedra-ímã escondida em (0, 2, 0).
 */
public final class GnomeRealmHouse {
    private static final BlockPos MARKER = new BlockPos(0, 2, 0);
    private static final int CLEAR = 22;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private GnomeRealmHouse() {}

    /** Garante que a casa existe e devolve onde o jogador chega (na frente da porta). */
    public static BlockPos ensureBuilt(ServerLevel level) {
        // gera os chunks em volta do centro
        // (um anel a mais de chunks para as árvores dos vizinhos não caírem dentro da obra depois)
        for (int cx = -4; cx <= 3; cx++) {
            for (int cz = -4; cz <= 3; cz++) {
                level.getChunk(cx, cz);
            }
        }
        int ground = groundY(level);
        if (!level.getBlockState(MARKER).is(Blocks.LODESTONE)) {
            build(level, ground, level.getRandom());
            level.setBlock(MARKER, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            // sementes/galhos que caíram da grama e das árvores cortadas na obra
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(-64, 0, -64, 64, 256, 64)).forEach(net.minecraft.world.entity.Entity::discard);
            // gnomos que nasceram com o mundo e ficaram enterrados quando a clareira foi aterrada: sobem para a superfície
            unburyGnomes(level, new net.minecraft.world.phys.AABB(-96, 0, -96, 96, 256, 96));
        }
        return new BlockPos(0, ground, R_BODY + 5);
    }

    private static int groundY(ServerLevel level) {
        // a casa fica em cima de uma plataforma na altura do chão original no centro
        int y = 70;
        for (int yy = 200; yy > 20; yy--) {
            BlockState state = level.getBlockState(new BlockPos(0, yy, 0));
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.STONE)) {
                y = yy + 1;
                break;
            }
        }
        BlockPos probe = new BlockPos(0, 0, R_BODY + 5);
        // se a casa já existe, a entrada é a referência
        if (level.getBlockState(MARKER).is(Blocks.LODESTONE)) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe.getX(), probe.getZ());
        }
        return Math.max(60, Math.min(110, y));
    }

    /** Cama vermelha com o pé em (x, z) e a cabeceira para o leste. */
    private static void placeBed(ServerLevel level, int x, int y, int z) {
        BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT);
        set(level, x, y, z, foot);
        set(level, x + 1, y, z, foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
    }

    // ----------------------------------------------------------------------------------------------------
    // Casa-cogumelo: base de pedra, corpo branco (caule de cogumelo) com vigas de madeira, raízes em volta,
    // um chapéu vermelho de pintas brancas enorme, uma torrezinha com chapéu pequeno, jardim, lagos e caminhos.
    // ----------------------------------------------------------------------------------------------------
    private static final int R_BODY = 7;      // raio do corpo
    private static final int R_CAP = 12;      // raio do chapéu
    private static final int BODY_H = 10;     // altura das paredes

    private static boolean inCircle(int x, int z, double r) {
        return x * x + z * z <= r * r;
    }

    private static boolean onCircle(int x, int z, double r) {
        return inCircle(x, z, r + 0.5) && !inCircle(x, z, r - 0.5);
    }

    private static BlockState capBlock(boolean underside) {
        BlockState cap = Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
        return underside ? cap.setValue(net.minecraft.world.level.block.HugeMushroomBlock.DOWN, false) : cap;
    }

    /** Pintas brancas do chapéu: manchas de 2x2 espalhadas numa grade de 4x4 (mesma receita sempre). */
    private static boolean spot(int dx, int dy, int dz, double r) {
        int ox = Math.floorMod(dx, 4), oz = Math.floorMod(dz, 4);
        if (ox > 1 || oz > 1) {
            return false;
        }
        int h = Math.floorDiv(dx, 4) * 73856093 ^ Math.floorDiv(dz, 4) * 19349663 ^ Math.floorDiv(dy, 3) * 83492791 ^ (int) r;
        return Math.floorMod(h, 5) < 3;
    }

    /** Domo de chapéu de cogumelo (casca de ~1 bloco) com o centro em (cx, y0, cz). */
    private static void mushroomCap(ServerLevel level, int cx, int y0, int cz, double r, double h) {
        for (int dx = (int) -r - 1; dx <= r + 1; dx++) {
            for (int dz = (int) -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) {
                    continue;
                }
                // altura da casca nessa distância (meia elipse) e a borda caindo um pouco
                double top = h * Math.sqrt(Math.max(0.0, 1.0 - (d / (r + 0.5)) * (d / (r + 0.5))));
                int yTop = (int) Math.round(top);
                int yBottom = d > r - 1.5 ? -1 : 0; // aba caída na borda
                for (int dy = yBottom; dy <= yTop; dy++) {
                    boolean shell = dy >= yTop - 1 || dy == yBottom || d > r - 1.0;
                    if (shell) {
                        boolean outer = dy != yBottom && (dy == yTop || d > r - 1.0);
                        set(level, cx + dx, y0 + dy, cz + dz,
                                outer && spot(dx, dy == yTop ? 0 : dy, dz, r) ? Blocks.WHITE_CONCRETE.defaultBlockState() : capBlock(dy == yBottom));
                    } else {
                        set(level, cx + dx, y0 + dy, cz + dz, AIR);
                    }
                }
            }
        }
    }

    private static void build(ServerLevel level, int g, RandomSource random) {
        int clear = CLEAR + 4;
        // 1) clareira nivelada
        for (int x = -clear; x <= clear; x++) {
            for (int z = -clear; z <= clear; z++) {
                for (int y = g; y <= g + 50; y++) {
                    set(level, x, y, z, AIR);
                }
                for (int y = g - 6; y < g - 1; y++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        set(level, x, y, z, Blocks.DIRT.defaultBlockState());
                    }
                }
                set(level, x, g - 1, z, Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }

        int f = g + 2; // nível do piso da casa (em cima da base de pedra)

        // 2) base de pedra
        for (int x = -R_BODY - 1; x <= R_BODY + 1; x++) {
            for (int z = -R_BODY - 1; z <= R_BODY + 1; z++) {
                if (!inCircle(x, z, R_BODY + 1.3)) {
                    continue;
                }
                for (int y = g - 1; y <= f - 1; y++) {
                    BlockState stone = random.nextInt(4) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                            : random.nextInt(6) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                    set(level, x, y, z, stone);
                }
                if (inCircle(x, z, R_BODY - 0.5)) {
                    set(level, x, f - 1, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
                }
            }
        }
        // escadinha até a porta (sul)
        for (int x = -1; x <= 1; x++) {
            set(level, x, g, R_BODY + 3, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            set(level, x, g + 1, R_BODY + 2, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            set(level, x, g, R_BODY + 2, Blocks.STONE_BRICKS.defaultBlockState());
        }

        // 3) corpo: parede redonda branca com vigas
        BlockState frame = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        for (int x = -R_BODY; x <= R_BODY; x++) {
            for (int z = -R_BODY; z <= R_BODY; z++) {
                if (!onCircle(x, z, R_BODY)) {
                    continue;
                }
                double ang = Math.toDegrees(Math.atan2(z, x));
                boolean post = Math.abs(((ang + 360) % 45) - 22.5) > 18.5;
                for (int y = f; y < f + BODY_H; y++) {
                    int rel = y - f;
                    BlockState wall;
                    if (post || rel == 0 || rel == 5 || rel == BODY_H - 1) {
                        wall = rel == 5 && !post ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : frame;
                    } else if ((rel == 2 || rel == 3 || rel == 7) && Math.abs(((ang + 360) % 45) - 22.5) < 6) {
                        wall = Blocks.GLASS.defaultBlockState();
                    } else {
                        wall = Blocks.MUSHROOM_STEM.defaultBlockState();
                    }
                    set(level, x, y, z, wall);
                }
            }
        }
        // porta (arco) + lanterna em cima
        for (int x = -1; x <= 1; x++) {
            for (int y = f; y <= f + 2; y++) {
                for (int z = R_BODY - 1; z <= R_BODY + 1; z++) {
                    set(level, x, y, z, AIR);
                }
            }
        }
        set(level, 0, f + 3, R_BODY, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        set(level, 0, f + 3, R_BODY + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(level, 0, f + 4, R_BODY + 1, Blocks.DARK_OAK_PLANKS.defaultBlockState());

        // raízes grossas saindo da base
        for (int i = 0; i < 7; i++) {
            double ang = i * (Math.PI * 2 / 7) + 0.3;
            if (Math.abs(Math.sin(ang) - 1) < 0.35) {
                continue; // não na frente da porta
            }
            for (int r = R_BODY; r <= R_BODY + 3; r++) {
                int x = (int) Math.round(Math.cos(ang) * r), z = (int) Math.round(Math.sin(ang) * r);
                int top = f + 1 - (r - R_BODY);
                for (int y = g - 1; y <= top; y++) {
                    set(level, x, y, z, Blocks.SPRUCE_WOOD.defaultBlockState());
                }
            }
        }

        // 4) teto e chapéu gigante
        int roof = f + BODY_H;
        for (int x = -R_BODY; x <= R_BODY; x++) {
            for (int z = -R_BODY; z <= R_BODY; z++) {
                if (inCircle(x, z, R_BODY + 0.5)) {
                    set(level, x, roof, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                }
            }
        }
        mushroomCap(level, 0, roof + 1, 0, R_CAP, 7.5);
        // lanternas penduradas na aba do chapéu
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4 + Math.PI / 8;
            int x = (int) Math.round(Math.cos(ang) * (R_CAP - 2)), z = (int) Math.round(Math.sin(ang) * (R_CAP - 2));
            set(level, x, roof - 1, z, Blocks.IRON_CHAIN.defaultBlockState());
            set(level, x, roof - 2, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        // 5) torrezinha com chapéu pequeno (noroeste)
        int tx = -7, tz = -5, tr = 2, th = BODY_H + 5;
        for (int x = tx - tr; x <= tx + tr; x++) {
            for (int z = tz - tr; z <= tz + tr; z++) {
                if (!inCircle(x - tx, z - tz, tr + 0.5)) {
                    continue;
                }
                for (int y = g - 1; y < f + th; y++) {
                    boolean edge = !inCircle(x - tx, z - tz, tr - 0.5);
                    BlockState b = edge ? ((y - f) % 4 == 0 ? frame : Blocks.MUSHROOM_STEM.defaultBlockState()) : AIR;
                    if (y < f) {
                        b = Blocks.STONE_BRICKS.defaultBlockState();
                    }
                    if (edge && (y - f) % 5 == 2 && (x == tx - tr || z == tz - tr)) {
                        b = Blocks.GLASS.defaultBlockState();
                    }
                    set(level, x, y, z, b);
                }
            }
        }
        mushroomCap(level, tx, f + th, tz, tr + 3, 3.5);

        // 6) interior
        int fl = f; // onde se pisa
        // tapete do trono até a porta
        for (int z = -3; z <= R_BODY - 1; z++) {
            set(level, 0, fl, z, Blocks.RED_CARPET.defaultBlockState());
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -6; z <= -4; z++) {
                if (inCircle(x, z, R_BODY - 0.5)) {
                    set(level, x, fl, z, Blocks.GOLD_BLOCK.defaultBlockState());
                }
            }
        }
        set(level, 0, fl + 1, -6, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        set(level, -2, fl + 1, -5, Blocks.LANTERN.defaultBlockState());
        set(level, 2, fl + 1, -5, Blocks.LANTERN.defaultBlockState());
        for (int[] bed : new int[][] {{-5, -2}, {-5, 1}, {-3, 3}}) {
            placeBed(level, bed[0], fl, bed[1]);
        }
        set(level, -4, fl, -4, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, -3, fl, -5, Blocks.BARREL.defaultBlockState());
        set(level, 4, fl, 4, Blocks.BOOKSHELF.defaultBlockState());
        set(level, 5, fl, 3, Blocks.BOOKSHELF.defaultBlockState());
        set(level, 4, fl + 1, 4, Blocks.POTTED_RED_MUSHROOM.defaultBlockState());
        set(level, -2, fl, 5, Blocks.POTTED_FERN.defaultBlockState());
        // lustre no meio
        for (int y = roof - 1; y >= roof - 3; y--) {
            set(level, 0, y, 0, Blocks.IRON_CHAIN.defaultBlockState());
        }
        set(level, 0, roof - 4, 0, Blocks.SHROOMLIGHT.defaultBlockState());

        // 7) escada até a mina (lances que vão e voltam, dentro de um túnel de pedra)
        int bottom = buildStairs(level, fl, random);

        // 8) jardim, caminhos, lagos, cogumelos-luminária
        buildGarden(level, g, random);

        // 9) o Rei no trono e os gnomos em volta
        GnomeKingEntity king = RealmContent.GNOME_KING.get().create(level, EntitySpawnReason.STRUCTURE);
        if (king != null) {
            king.snapTo(0.5, fl + 1, -4.5, 180.0F, 0.0F);
            king.setYHeadRot(180.0F);
            level.addFreshEntity(king);
        }
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = R_CAP + 3 + random.nextDouble() * 5;
            int x = (int) (Math.cos(angle) * dist), z = (int) (Math.sin(angle) * dist);
            spawnGnome(level, x + 0.5, g, z + 0.5, ItemStack.EMPTY);
        }
    }

    /** Lances de escada de 12 degraus indo para leste e voltando para oeste até a altura da mina. Devolve a altura final. */
    private static int buildStairs(ServerLevel level, int floor, RandomSource random) {
        int mineY = Math.max(18, floor - 44);
        int x0 = 1, x1 = 12;               // começo e fim de cada lance
        int[][] lanes = {{-2, 0}, {-6, -4}}; // faixa (z) de ida e de volta
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        int y = floor - 1;                 // bloco do primeiro degrau
        int flight = 0;
        int landingX = x1 + 1;
        while (y - 12 >= mineY) {
            int[] lane = lanes[flight % 2];
            boolean east = flight % 2 == 0;
            for (int i = 0; i < 12; i++) {
                int x = east ? x0 + i : x1 - i;
                int sy = y - 1 - i;
                for (int z = lane[0] - 1; z <= lane[1] + 1; z++) {
                    boolean side = z < lane[0] || z > lane[1];
                    for (int yy = sy - 1; yy <= sy + 5; yy++) {
                        boolean inside = !side && yy >= sy && yy <= sy + 4;
                        if (!inside) {
                            // paredes/teto só onde não é piso da casa nem outro lance
                            if (yy >= floor - 1) {
                                continue;
                            }
                            set(level, x, yy, z, wall);
                        } else if (yy == sy) {
                            set(level, x, yy, z, Blocks.SPRUCE_STAIRS.defaultBlockState()
                                    .setValue(StairBlock.FACING, east ? Direction.WEST : Direction.EAST));
                        } else {
                            set(level, x, yy, z, AIR);
                        }
                    }
                }
                if (i % 4 == 2 && sy + 4 < floor - 1) {
                    set(level, x, sy + 4, lane[0], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                }
            }
            y -= 12;
            // patamar ligando as duas faixas: piso no mesmo nível do último degrau (1 degrau abaixo), parede do lado de fora
            int lx = east ? x1 + 1 : x0 - 1;
            int outer = east ? lx + 1 : lx - 1;
            landingX = lx;
            for (int z = -7; z <= 1; z++) {
                boolean inLane = z >= -6 && z <= 0;
                for (int yy = y - 2; yy <= y + 5; yy++) {
                    if (inLane && yy >= y && yy <= y + 3) {
                        set(level, lx, yy, z, AIR);
                    } else if (inLane && yy == y - 1) {
                        set(level, lx, yy, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
                    } else {
                        set(level, lx, yy, z, wall);
                    }
                    set(level, outer, yy, z, wall);
                }
            }
            set(level, lx, y + 3, -3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            y -= 1;
            flight++;
        }
        // abertura no piso da casa + grade em volta
        for (int x = x0; x <= x0 + 2; x++) {
            for (int z = lanes[0][0]; z <= lanes[0][1]; z++) {
                set(level, x, floor - 1, z, AIR);
            }
        }
        for (int x = x0 - 1; x <= x0 + 3; x++) {
            for (int z = lanes[0][0] - 1; z <= lanes[0][1] + 1; z++) {
                boolean edge = x == x0 + 3 || z == lanes[0][0] - 1 || z == lanes[0][1] + 1;
                if (edge && x != x0 - 1) {
                    set(level, x, floor, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                }
            }
        }
        // a mina começa logo depois do último patamar (o salão abre a parede de fora); sem túnel voltando para a escada
        boolean lastEast = (flight - 1) % 2 == 0;
        int hubX = landingX + (lastEast ? 5 : -5);
        buildMine(level, hubX, y + 1, -3, random, lastEast ? Direction.WEST : Direction.EAST);
        return y;
    }

    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.PINK_TULIP, Blocks.RED_TULIP, Blocks.BLUE_ORCHID};

    private static void buildGarden(ServerLevel level, int g, RandomSource random) {
        int ring = R_CAP + 2;
        // caminho principal até o sul e um anel em volta da casa
        for (int z = R_BODY + 4; z <= CLEAR + 4; z++) {
            for (int x = -1; x <= 1; x++) {
                set(level, x, g - 1, z, Blocks.DIRT_PATH.defaultBlockState());
            }
        }
        for (int x = -ring - 1; x <= ring + 1; x++) {
            for (int z = -ring - 1; z <= ring + 1; z++) {
                if (onCircle(x, z, ring) && level.getBlockState(new BlockPos(x, g - 1, z)).is(Blocks.GRASS_BLOCK)) {
                    set(level, x, g - 1, z, random.nextInt(5) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
                }
            }
        }
        // postes com lanterna ao longo do caminho
        for (int z = R_BODY + 6; z <= CLEAR + 2; z += 5) {
            for (int x : new int[] {-2, 2}) {
                set(level, x, g, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                set(level, x, g + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                set(level, x, g + 2, z, Blocks.LANTERN.defaultBlockState());
            }
        }
        // lagos com vitórias-régias, cana e juncos
        pond(level, 17, g, 9, 4.2, random);
        pond(level, -16, g, 12, 3.2, random);
        pond(level, 15, g, -15, 3.0, random);
        // canteiros de flores e moitas
        int clear = CLEAR + 4;
        for (int x = -clear; x <= clear; x++) {
            for (int z = -clear; z <= clear; z++) {
                BlockPos ground = new BlockPos(x, g - 1, z);
                if (inCircle(x, z, ring + 0.6) || !level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                        || !level.getBlockState(ground.above()).isAir()) {
                    continue;
                }
                double patch = Math.sin(x * 0.35) * Math.cos(z * 0.31) + Math.sin((x + z) * 0.17);
                if (patch > 0.7 && random.nextInt(3) != 0) {
                    set(level, x, g, z, FLOWERS[Math.floorMod(x / 3 + z / 4, FLOWERS.length)].defaultBlockState());
                } else if (patch < -1.1 && random.nextInt(3) == 0) {
                    set(level, x, g, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
                } else if (random.nextInt(6) == 0) {
                    set(level, x, g, z, Blocks.SHORT_GRASS.defaultBlockState());
                }
            }
        }
        // cogumelos-luminária
        for (int[] m : new int[][] {{-12, 17}, {12, 18}, {-19, -6}, {20, -3}, {-6, -19}}) {
            lampMushroom(level, m[0], g, m[1]);
        }
    }

    private static void pond(ServerLevel level, int cx, int g, int cz, double r, RandomSource random) {
        for (int x = (int) (cx - r - 2); x <= cx + r + 2; x++) {
            for (int z = (int) (cz - r - 2); z <= cz + r + 2; z++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (d <= r) {
                    int depth = d < r - 1.5 ? 3 : 2;
                    for (int y = g - depth; y <= g - 1; y++) {
                        set(level, x, y, z, Blocks.WATER.defaultBlockState());
                    }
                    set(level, x, g - depth - 1, z, random.nextBoolean() ? Blocks.CLAY.defaultBlockState() : Blocks.SAND.defaultBlockState());
                    set(level, x, g, z, AIR);
                    if (random.nextInt(7) == 0) {
                        set(level, x, g, z, Blocks.LILY_PAD.defaultBlockState());
                    }
                } else if (d <= r + 1.2) {
                    // margem: pedrinhas, cana e flores
                    int roll = random.nextInt(10);
                    if (roll < 2) {
                        set(level, x, g - 1, z, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                    } else if (roll < 4) {
                        set(level, x, g - 1, z, Blocks.GRASS_BLOCK.defaultBlockState());
                        set(level, x, g, z, Blocks.SUGAR_CANE.defaultBlockState());
                        set(level, x, g + 1, z, Blocks.SUGAR_CANE.defaultBlockState());
                    } else if (roll == 4) {
                        set(level, x, g, z, Blocks.FIREFLY_BUSH.defaultBlockState());
                    }
                }
            }
        }
    }

    /** Cogumelo gigante pequeno com uma lanterna pendurada embaixo do chapéu. */
    private static void lampMushroom(ServerLevel level, int x, int g, int z) {
        for (int y = g; y < g + 4; y++) {
            set(level, x, y, z, Blocks.MUSHROOM_STEM.defaultBlockState());
        }
        mushroomCap(level, x, g + 4, z, 2.4, 1.5);
        set(level, x + 1, g + 3, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private static final BlockState[] ORES = {
            Blocks.COAL_ORE.defaultBlockState(), Blocks.COAL_ORE.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState(),
            Blocks.IRON_ORE.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState(), Blocks.COPPER_ORE.defaultBlockState(),
            Blocks.COPPER_ORE.defaultBlockState(), Blocks.GOLD_ORE.defaultBlockState(), Blocks.GOLD_ORE.defaultBlockState(),
            Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.LAPIS_ORE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState(),
            Blocks.EMERALD_ORE.defaultBlockState()};

    private static void buildMine(ServerLevel level, int cx, int y, int cz, RandomSource random, Direction back) {
        // salão de 9x9 em volta da saída da escada
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                for (int dy = 0; dy <= 4; dy++) {
                    set(level, x, y + dy, z, AIR);
                }
                set(level, x, y - 1, z, Blocks.COBBLESTONE.defaultBlockState());
                if (Math.max(Math.abs(x - cx), Math.abs(z - cz)) == 4) {
                    set(level, x, y + 5, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
                }
            }
        }
        set(level, cx - 3, y, cz - 3, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, cx + 3, y, cz + 3, Blocks.BARREL.defaultBlockState());
        for (int[] c : new int[][] {{-3, 3}, {3, -3}}) {
            set(level, cx + c[0], y + 4, cz + c[1], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        // quatro túneis
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir == back) {
                continue;
            }
            Direction side = dir.getClockWise();
            int length = 30 + random.nextInt(8);
            BlockPos start = new BlockPos(cx, y, cz).relative(dir, 5);
            for (int i = 0; i < length; i++) {
                BlockPos c = start.relative(dir, i);
                for (int w = -1; w <= 1; w++) {
                    BlockPos col = c.relative(side, w);
                    for (int dy = 0; dy <= 2; dy++) {
                        level.setBlock(col.above(dy), AIR, Block.UPDATE_CLIENTS);
                    }
                    level.setBlock(col.below(), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlock(col.above(3), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                // paredes cheias de minério
                for (int dy = 0; dy <= 2; dy++) {
                    for (int s : new int[] {-2, 2}) {
                        BlockPos wall = c.relative(side, s).above(dy);
                        level.setBlock(wall, random.nextInt(3) == 0 ? ORES[random.nextInt(ORES.length)] : Blocks.STONE.defaultBlockState(),
                                Block.UPDATE_CLIENTS);
                    }
                }
                // trilho no meio
                RailShape shape = dir.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
                level.setBlock(c, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape), Block.UPDATE_CLIENTS);
                // escoras de madeira a cada 5 blocos
                if (i % 5 == 2) {
                    for (int dy = 0; dy <= 2; dy++) {
                        level.setBlock(c.relative(side, -1).above(dy), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                        level.setBlock(c.relative(side, 1).above(dy), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    for (int w = -1; w <= 1; w++) {
                        level.setBlock(c.relative(side, w).above(2), Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    level.setBlock(c.above(1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), Block.UPDATE_CLIENTS);
                }
            }
            // baú no fim do túnel
            BlockPos end = start.relative(dir, length - 1).relative(side, 1);
            level.setBlock(end, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            RandomizableContainer.setBlockEntityLootTable(level, random, end,
                    ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/abandoned_mineshaft")));
            // dois gnomos mineiros por túnel
            for (int g = 0; g < 2; g++) {
                BlockPos p = start.relative(dir, 6 + random.nextInt(length - 8));
                spawnGnome(level, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, new ItemStack(Items.IRON_PICKAXE));
            }
        }
    }

    /** Tira de dentro da terra os gnomos que ficaram presos em blocos (manda para o topo daquela coluna). */
    public static void unburyGnomes(ServerLevel level, net.minecraft.world.phys.AABB area) {
        for (GnomeEntity gnome : level.getEntitiesOfClass(GnomeEntity.class, area)) {
            if (!level.noCollision(gnome)) {
                int x = gnome.getBlockX(), z = gnome.getBlockZ();
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                gnome.snapTo(x + 0.5, top, z + 0.5, gnome.getYRot(), 0.0F);
            }
        }
    }

    private static void spawnGnome(ServerLevel level, double x, double y, double z, ItemStack tool) {
        GnomeEntity gnome = GnomeContent.GNOME.get().create(level, EntitySpawnReason.STRUCTURE);
        if (gnome == null) {
            return;
        }
        gnome.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!tool.isEmpty()) {
            gnome.setItemSlot(EquipmentSlot.MAINHAND, tool);
            gnome.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        gnome.setPersistenceRequired();
        level.addFreshEntity(gnome);
    }
}
