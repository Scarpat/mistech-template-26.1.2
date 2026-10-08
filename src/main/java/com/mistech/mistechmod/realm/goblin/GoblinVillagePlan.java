package com.mistech.mistechmod.realm.goblin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.mistech.mistechmod.block.goblin.BoneLockBlock;
import com.mistech.mistechmod.block.goblin.GoblinTotemBlock;
import com.mistech.mistechmod.block.goblin.RunePanelBlock;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;
import com.mistech.mistechmod.registry.GoblinContent;
import com.mistech.mistechmod.registry.MurkyWood;
import com.mistech.mistechmod.registry.RealmContent;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Planta da Vila dos Duendes. Tudo sai da semente, então cada chunk reconstrói a mesma planta.
 *
 * <pre>
 * Superfície (y0): clareira de lama batida cercada por paliçada, 8 cabanas, torre de vigia, fogueiras e,
 *                  no centro, o FOSSO: poço redondo com escada em caracol em volta de um pilar.
 * Fundo (catY):    túnel do fosso até o LABIRINTO (9x9 células de 3 blocos, paredes de 3).
 *                  Centro = ARENA da Rainha (porta selada pela Fechadura de Ossos, 3 chaves).
 *                  Atrás do trono = sala do tesouro (selada até a Rainha morrer).
 *                  Três salas de chave presas nas bordas do labirinto:
 *                    norte = Totens (girar até todos olharem para o centro),
 *                    sul   = Painel de Runas (acender todas as pedras),
 *                    leste = corredor com armadilhas de flecha + Troll guardião.
 * </pre>
 */
public final class GoblinVillagePlan {
    static final int N = 9;          // células por lado
    static final int U = 3;          // tamanho de uma unidade (célula ou parede)
    static final int VILLAGE_R = 31; // raio da paliçada
    static final int FLAT_R = 33;    // raio do chão plano
    static final int BLEND = 10;     // faixa de transição para o terreno natural

    private final Blueprint bp = new Blueprint();
    private final Random rnd;
    private final int cx, y0, cz, catY, mx, mz;

    // ---------- paletas ----------
    private static BlockState st(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    private final Palette wall = Palette.mix(11, st(Blocks.DEEPSLATE_BRICKS), 55, st(Blocks.CRACKED_DEEPSLATE_BRICKS), 20,
            st(Blocks.DEEPSLATE_TILES), 15, st(Blocks.TUFF_BRICKS), 9, st(Blocks.CRYING_OBSIDIAN), 1);
    private final Palette floor = Palette.mix(12, st(Blocks.DEEPSLATE_TILES), 50, st(Blocks.POLISHED_DEEPSLATE), 25,
            st(Blocks.CRACKED_DEEPSLATE_TILES), 25);
    private final Palette arenaFloor = Palette.mix(13, st(Blocks.POLISHED_BLACKSTONE_BRICKS), 70,
            st(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS), 30);
    private final Palette ground = Palette.mix(14, st(Blocks.PACKED_MUD), 60, st(Blocks.COARSE_DIRT), 0, st(Blocks.GRAVEL), 15,
            st(Blocks.MUD_BRICKS), 10, st(Blocks.TUFF), 15);
    private final Palette hutWall = Palette.mix(15, st(Blocks.MOSSY_COBBLESTONE), 40, st(MurkyWood.PLANKS.get()), 30,
            st(Blocks.MUD_BRICKS), 30);
    private final BlockState log = MurkyWood.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    private final BlockState planks = st(MurkyWood.PLANKS.get());
    private final BlockState fence = st(MurkyWood.FENCE.get());
    private final BlockState seal = st(GoblinContent.GOBLIN_SEAL.get());

    // ---------- labirinto ----------
    private final boolean[][] reserved = new boolean[N][N];
    private final boolean[][] openEast = new boolean[N][N];  // passagem (i,j) -> (i+1,j)
    private final boolean[][] openSouth = new boolean[N][N]; // passagem (i,j) -> (i,j+1)

    public GoblinVillagePlan(long seed, int cx, int y0, int cz) {
        this.rnd = new Random(seed);
        this.cx = cx;
        this.y0 = y0;
        this.cz = cz;
        this.catY = Math.max(y0 - 36, -44);
        this.mx = cx + 8;
        this.mz = cz - (N * 2 + 1) * U / 2; // a linha do meio (j = 4) fica no z do fosso

        this.surfaceGround();
        this.catacombShell();
        this.maze();
        this.arena();
        this.totemRoom();
        this.runeRoom();
        this.trapRoom();
        this.pit();
        this.village();
    }

    public Blueprint blueprint() {
        return this.bp;
    }

    public BoundingBox bounds() {
        return this.bp.bounds();
    }

    private int cellX(int i) {
        return this.mx + (2 * i + 1) * U;
    }

    private int cellZ(int j) {
        return this.mz + (2 * j + 1) * U;
    }

    private static int[] cell(int i, int j) {
        return new int[] {i, j};
    }

    // ================================================================== superfície

    /** Aplaina a clareira: chão de lama batida no raio da vila e rampa suave até o terreno natural. */
    private void surfaceGround() {
        int r = FLAT_R + BLEND;
        final int top = this.y0 - 1;
        BoundingBox box = new BoundingBox(this.cx - r, this.catY, this.cz - r, this.cx + r, this.y0 + 40, this.cz + r);
        this.bp.custom(box, ctx -> {
            int ax = Math.max(box.minX(), ctx.chunk.minX()), bx = Math.min(box.maxX(), ctx.chunk.maxX());
            int az = Math.max(box.minZ(), ctx.chunk.minZ()), bz = Math.min(box.maxZ(), ctx.chunk.maxZ());
            for (int x = ax; x <= bx; x++) {
                for (int z = az; z <= bz; z++) {
                    double d = Math.hypot(x - this.cx, z - this.cz);
                    if (d > r) {
                        continue;
                    }
                    int natural = Blueprint.surface(ctx, x, z);
                    int target;
                    boolean village = d <= FLAT_R;
                    if (village) {
                        target = top;
                    } else {
                        double s = (d - FLAT_R) / BLEND;
                        s = s * s * (3 - 2 * s);
                        target = (int) Math.round(top + (natural - top) * s);
                    }
                    if (natural > target) {
                        for (int y = target + 1; y <= Math.min(natural, target + 40); y++) {
                            ctx.set(x, y, z, Blocks.AIR.defaultBlockState());
                        }
                    } else {
                        for (int y = natural + 1; y < target; y++) {
                            ctx.set(x, y, z, y < target - 3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                        }
                    }
                    BlockState surfaceBlock = village ? this.ground.pick(x, target, z) : RealmContent.WITHERED_GRASS.get().defaultBlockState();
                    ctx.set(x, target, z, surfaceBlock);
                    if (village) {
                        ctx.set(x, target - 1, z, Blocks.DIRT.defaultBlockState());
                    }
                }
            }
        });
    }

    // ================================================================== catacumbas

    private int mazeSize() {
        return (2 * N + 1) * U;
    }

    private void catacombShell() {
        int s = this.mazeSize();
        this.bp.fill(this.mx, this.catY - 1, this.mz, this.mx + s - 1, this.catY + 6, this.mz + s - 1, this.wall);
        // chão das células e das passagens é trocado depois; aqui só o bloco do chão
        this.bp.fill(this.mx, this.catY, this.mz, this.mx + s - 1, this.catY, this.mz + s - 1, this.floor);
    }

    private void maze() {
        // reserva a arena (3x3 no centro) e a sala do tesouro (à leste da arena)
        for (int i = 3; i <= 5; i++) {
            for (int j = 3; j <= 5; j++) {
                this.reserved[i][j] = true;
            }
        }
        this.reserved[6][4] = true;

        // labirinto perfeito (busca em profundidade) a partir da entrada (0,4)
        boolean[][] seen = new boolean[N][N];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(cell(0, 4));
        seen[0][4] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int[] d : dirs) {
                int ni = c[0] + d[0], nj = c[1] + d[1];
                if (ni >= 0 && nj >= 0 && ni < N && nj < N && !seen[ni][nj] && !this.reserved[ni][nj]) {
                    options.add(cell(ni, nj));
                }
            }
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] next = options.get(this.rnd.nextInt(options.size()));
            this.open(c[0], c[1], next[0], next[1]);
            seen[next[0]][next[1]] = true;
            stack.push(next);
        }
        // alguns atalhos extras para virar labirinto de verdade (vários caminhos)
        for (int k = 0; k < 9; k++) {
            int i = this.rnd.nextInt(N - 1), j = this.rnd.nextInt(N);
            if (!this.reserved[i][j] && !this.reserved[i + 1][j]) {
                this.open(i, j, i + 1, j);
            }
            int a = this.rnd.nextInt(N), b = this.rnd.nextInt(N - 1);
            if (!this.reserved[a][b] && !this.reserved[a][b + 1]) {
                this.open(a, b, a, b + 1);
            }
        }

        // cava as células e as passagens
        int y1 = this.catY + 1, y2 = this.catY + 4;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (this.reserved[i][j]) {
                    continue;
                }
                int x = this.cellX(i), z = this.cellZ(j);
                this.bp.air(x, y1, z, x + U - 1, y2, z + U - 1);
                if (this.openEast[i][j]) {
                    this.bp.air(x + U, y1, z, x + 2 * U - 1, y2, z + U - 1);
                }
                if (this.openSouth[i][j]) {
                    this.bp.air(x, y1, z + U, x + U - 1, y2, z + 2 * U - 1);
                }
            }
        }
        // entrada oeste da célula (0,4)
        this.bp.air(this.mx, y1, this.cellZ(4), this.mx + U - 1, y2, this.cellZ(4) + U - 1);

        this.decorateMaze();
    }

    private void open(int i, int j, int ni, int nj) {
        if (ni == i + 1) {
            this.openEast[i][j] = true;
        } else if (ni == i - 1) {
            this.openEast[ni][nj] = true;
        } else if (nj == j + 1) {
            this.openSouth[i][j] = true;
        } else {
            this.openSouth[ni][nj] = true;
        }
    }

    private int exits(int i, int j) {
        int n = 0;
        if (this.openEast[i][j]) n++;
        if (i > 0 && this.openEast[i - 1][j]) n++;
        if (this.openSouth[i][j]) n++;
        if (j > 0 && this.openSouth[i][j - 1]) n++;
        return n;
    }

    /** Lanternas, teias, ossos, baús, spawners, armadilhas e criaturas pelo labirinto. */
    private void decorateMaze() {
        List<int[]> free = new ArrayList<>();
        List<int[]> deadEnds = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (this.reserved[i][j] || (i == 0 && j == 4) || (i == 2 && j == 4)
                        || (i == 1 && j == 0) || (i == 7 && j == 8) || (i == 8 && j == 1)) {
                    continue;
                }
                free.add(cell(i, j));
                if (this.exits(i, j) == 1) {
                    deadEnds.add(cell(i, j));
                }
            }
        }
        Collections.shuffle(free, this.rnd);
        Collections.shuffle(deadEnds, this.rnd);
        int top = this.catY + 4;

        for (int[] c : free) {
            int x = this.cellX(c[0]), z = this.cellZ(c[1]);
            int roll = this.rnd.nextInt(10);
            if (roll < 2) {
                this.bp.block(x + 1, top, z + 1, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            if (this.rnd.nextInt(6) == 0) {
                this.bp.block(x + (this.rnd.nextBoolean() ? 0 : 2), top, z + (this.rnd.nextBoolean() ? 0 : 2), Blocks.COBWEB.defaultBlockState());
            }
            if (this.rnd.nextInt(8) == 0) {
                this.bp.block(x + this.rnd.nextInt(3), this.catY + 1, z + this.rnd.nextInt(3), Blocks.BONE_BLOCK.defaultBlockState());
            }
        }
        // baús nos becos sem saída, com um crânio do lado
        int chests = 0;
        for (int[] c : deadEnds) {
            if (chests >= 4) {
                break;
            }
            int x = this.cellX(c[0]) + 1, z = this.cellZ(c[1]) + 1;
            this.bp.chest(x, this.catY + 1, z, Direction.from2DDataValue(this.rnd.nextInt(4)), Blueprint.loot("chests/goblin_catacomb"));
            this.bp.block(x + 1, this.catY + 1, z + 1, Blocks.SKELETON_SKULL.defaultBlockState());
            chests++;
        }
        // spawners (2 de aberração, 1 de duende) longe da entrada
        int placed = 0;
        for (int[] c : free) {
            if (placed >= 3 || c[0] < 2) {
                continue;
            }
            int x = this.cellX(c[0]) + 1, z = this.cellZ(c[1]) + 1;
            if (placed < 2) {
                this.bp.spawner(x, this.catY + 1, z, GoblinContent.ABERRATION::get);
            } else {
                this.bp.spawner(x, this.catY + 1, z, GoblinContent.CORRUPTED_GOBLIN::get);
            }
            placed++;
            free.set(free.indexOf(c), cell(-1, -1));
        }
        // criaturas espalhadas
        int goblins = 0, aberrations = 0, trolls = 0;
        for (int[] c : free) {
            if (c[0] < 0) {
                continue;
            }
            double x = this.cellX(c[0]) + 1.5, z = this.cellZ(c[1]) + 1.5;
            if (goblins < 6 && this.rnd.nextInt(3) == 0) {
                this.bp.mob(x, this.catY + 1, z, GoblinContent.CORRUPTED_GOBLIN::get, null);
                goblins++;
            } else if (aberrations < 5 && this.rnd.nextInt(3) == 0) {
                this.bp.mob(x, this.catY + 1, z, GoblinContent.ABERRATION::get, null);
                aberrations++;
            } else if (trolls < 2 && c[0] >= 3 && this.rnd.nextInt(5) == 0) {
                this.bp.mob(x, this.catY + 1, z, GoblinContent.TROLL::get, null);
                trolls++;
            }
        }
        // armadilhas: placa de pressão numa passagem leste-oeste e um dispenser na parede apontando para ela
        int traps = 0;
        for (int i = 1; i < N - 1 && traps < 4; i++) {
            for (int j = 0; j < N && traps < 4; j++) {
                if (this.openEast[i][j] && !this.reserved[i][j] && !this.reserved[i + 1][j] && this.rnd.nextInt(4) == 0) {
                    int x = this.cellX(i) + U + 1, z = this.cellZ(j);
                    this.bp.block(x, this.catY + 1, z, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState());
                    this.bp.dispenser(x, this.catY + 1, z - 1, Direction.SOUTH, Blueprint.loot("dispensers/goblin_trap"));
                    traps++;
                }
            }
        }
    }

    // ================================================================== arena

    private void arena() {
        int ax = this.mx + 28, az = this.cz, fy = this.catY;
        double r = 9.5;
        // casca da arena: só a área reservada (unidades 6..12) na altura do labirinto, e mais alta por cima
        this.bp.fill(ax - 10, fy - 1, az - 10, ax + 10, fy + 6, az + 10, this.wall);
        this.bp.fill(ax - 11, fy + 6, az - 11, ax + 11, fy + 13, az + 11, this.wall);
        this.bp.shape(ax - 10, fy, az - 10, ax + 10, fy, az + 10, (x, y, z) -> Math.hypot(x - ax, z - az) < r, this.arenaFloor);
        // cúpula
        this.bp.shape(ax - 10, fy + 1, az - 10, ax + 10, fy + 11, az + 10, (x, y, z) -> {
            double d = Math.hypot(x - ax, z - az);
            return d < r && y <= fy + 11 - (int) (d * d / (r * r) * 4);
        }, Palette.of(Blocks.CAVE_AIR.defaultBlockState()));
        // anel de blackstone dourada e círculo interno
        this.bp.shape(ax - 10, fy, az - 10, ax + 10, fy, az + 10, (x, y, z) -> {
            double d = Math.hypot(x - ax, z - az);
            return (d >= 7.0 && d < 7.9) || d < 1.6;
        }, Palette.mix(21, st(Blocks.GILDED_BLACKSTONE), 35, st(Blocks.POLISHED_BLACKSTONE), 65));
        // 4 pilares nas diagonais (dá para se esconder das magias)
        for (int[] d : new int[][] {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            int px = ax + d[0] * 5, pz = az + d[1] * 5;
            this.bp.fill(px, fy + 1, pz, px + (d[0] > 0 ? 1 : -1), fy + 11, pz + (d[1] > 0 ? 1 : -1),
                    Palette.mix(22, st(Blocks.POLISHED_BLACKSTONE_BRICKS), 80, st(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS), 20));
        }
        // braseiros de fogo das almas
        for (int k = 0; k < 8; k++) {
            if (k == 0 || k == 4) {
                continue; // não na frente da porta nem do trono
            }
            double a = k * Math.PI / 4;
            int bx = ax + (int) Math.round(Math.cos(a) * 8), bz = az + (int) Math.round(Math.sin(a) * 8);
            this.bp.block(bx, fy + 1, bz, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        // lustre: lanternas penduradas no alto
        for (int[] d : new int[][] {{0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
            this.bp.block(ax + d[0], fy + 10, az + d[1], Blocks.IRON_CHAIN.defaultBlockState());
            this.bp.block(ax + d[0], fy + 9, az + d[1], Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // trono (leste), de frente para a porta
        this.bp.block(ax + 7, fy + 1, az, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        this.bp.fill(ax + 8, fy + 1, az, ax + 8, fy + 3, az, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        this.bp.block(ax + 8, fy + 4, az, Blocks.GOLD_BLOCK.defaultBlockState());
        this.bp.block(ax + 7, fy + 1, az - 1, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        this.bp.block(ax + 7, fy + 1, az + 1, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());

        // porta oeste: selada; a Fechadura de Ossos fica em cima, no meio
        int dx = ax - 10;
        this.bp.fill(dx, fy + 1, az - 1, dx, fy + 3, az + 1, this.seal);
        this.bp.fill(dx, fy + 4, az - 1, dx, fy + 4, az + 1, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        this.bp.block(dx, fy + 4, az, GoblinContent.BONE_LOCK.get().defaultBlockState().setValue(BoneLockBlock.FACING, Direction.WEST));
        this.bp.block(dx - 1, fy + 4, az - 1, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

        // sala do tesouro (atrás do trono), selada até a Rainha morrer
        int tx = ax + 11;
        this.bp.air(tx, fy + 1, az - 2, tx + 2, fy + 3, az + 2);
        this.bp.fill(tx, fy, az - 2, tx + 2, fy, az + 2, Blocks.GILDED_BLACKSTONE.defaultBlockState());
        this.bp.fill(ax + 10, fy + 1, az - 1, ax + 10, fy + 3, az + 1, this.seal);
        this.bp.chest(tx + 2, fy + 1, az - 2, Direction.WEST, Blueprint.loot("chests/goblin_queen_treasure"));
        this.bp.chest(tx + 2, fy + 1, az + 2, Direction.WEST, Blueprint.loot("chests/goblin_queen_treasure"));
        this.bp.block(tx + 2, fy + 1, az, Blocks.GOLD_BLOCK.defaultBlockState());
        this.bp.block(tx + 2, fy + 2, az, Blocks.SOUL_LANTERN.defaultBlockState());
        this.bp.block(tx, fy + 1, az - 2, Blocks.GOLD_BLOCK.defaultBlockState());
        this.bp.block(tx, fy + 1, az + 2, Blocks.GOLD_BLOCK.defaultBlockState());

        // a Rainha
        this.bp.mob(ax + 0.5, fy + 1, az + 0.5, GoblinContent.GOBLIN_QUEEN::get, q -> q.setArena(new net.minecraft.core.BlockPos(ax, fy + 1, az)));
    }

    // ================================================================== salas das chaves

    /** Norte: 4 totens em cruz em volta de um braseiro; girar até todos olharem para o centro abre a alcova da chave. */
    private void totemRoom() {
        int x0 = this.cellX(1), fy = this.catY;
        int rcx = x0 + 1, rcz = this.mz - 8;
        this.bp.fill(rcx - 5, fy - 1, this.mz - 17, rcx + 5, fy + 7, this.mz - 1, this.wall);
        this.bp.air(x0, fy + 1, this.mz, x0 + 2, fy + 4, this.mz + 2);          // atravessa a borda do labirinto
        this.bp.air(x0, fy + 1, this.mz - 3, x0 + 2, fy + 4, this.mz - 1);      // corredor
        this.room(rcx - 4, rcz - 4, rcx + 4, rcz + 4, 5);
        this.bp.block(rcx, fy + 1, rcz, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        Direction[] toCenter = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        int[][] spots = {{0, -2}, {0, 2}, {-2, 0}, {2, 0}};
        Direction[] facings = new Direction[4];
        boolean allRight;
        do {
            allRight = true;
            for (int k = 0; k < 4; k++) {
                facings[k] = Direction.from2DDataValue(this.rnd.nextInt(4));
                allRight &= facings[k] == toCenter[k];
            }
        } while (allRight);
        for (int k = 0; k < 4; k++) {
            int tx = rcx + spots[k][0], tz = rcz + spots[k][1];
            this.bp.block(tx, fy + 1, tz, GoblinContent.GOBLIN_TOTEM.get().defaultBlockState().setValue(GoblinTotemBlock.FACING, facings[k]));
            this.bp.block(tx, fy + 2, tz, this.log);
            this.bp.block(tx, fy + 3, tz, Blocks.SKELETON_SKULL.defaultBlockState());
        }
        // alcova da chave (norte), selada
        this.bp.air(x0, fy + 1, this.mz - 16, x0 + 2, fy + 3, this.mz - 14);
        this.bp.fill(x0, fy + 1, this.mz - 13, x0 + 2, fy + 3, this.mz - 13, this.seal);
        this.bp.chest(rcx, fy + 1, this.mz - 16, Direction.SOUTH, Blueprint.loot("chests/goblin_key_room"));
        this.bp.mob(rcx + 3.5, fy + 1, rcz + 3.5, GoblinContent.CORRUPTED_GOBLIN::get, null);
        this.bp.mob(rcx - 2.5, fy + 1, rcz + 3.5, GoblinContent.CORRUPTED_GOBLIN::get, null);
    }

    /** Sul: painel 3x3 de Pedras Rúnicas na parede do fundo; acender todas abre a alcova da chave (leste). */
    private void runeRoom() {
        int x0 = this.cellX(7), fy = this.catY, s = this.mazeSize();
        int zEdge = this.mz + s;          // primeiro z depois da borda sul
        int rcx = x0 + 1, rcz = zEdge + 9;
        this.bp.fill(rcx - 5, fy - 1, zEdge, rcx + 9, fy + 7, zEdge + 15, this.wall);
        this.bp.air(x0, fy + 1, zEdge - U, x0 + 2, fy + 4, zEdge - 1);          // borda do labirinto
        this.bp.air(x0, fy + 1, zEdge, x0 + 2, fy + 4, zEdge + 4);              // corredor
        this.room(rcx - 4, rcz - 4, rcx + 4, rcz + 4, 5);
        // painel no fundo (parede sul), virado para o norte; começa todo aceso e leva alguns toques aleatórios
        int pz = rcz + 5;
        boolean[][] lit = new boolean[3][3];
        do {
            for (boolean[] row : lit) {
                java.util.Arrays.fill(row, true);
            }
            int presses = 3 + this.rnd.nextInt(3);
            for (int p = 0; p < presses; p++) {
                int a = this.rnd.nextInt(3), b = this.rnd.nextInt(3);
                int[][] cross = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] d : cross) {
                    int na = a + d[0], nb = b + d[1];
                    if (na >= 0 && nb >= 0 && na < 3 && nb < 3) {
                        lit[na][nb] = !lit[na][nb];
                    }
                }
            }
        } while (allLit(lit));
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                this.bp.block(rcx - 1 + a, fy + 2 + b, pz, GoblinContent.RUNE_PANEL.get().defaultBlockState()
                        .setValue(RunePanelBlock.FACING, Direction.NORTH).setValue(RunePanelBlock.LIT, lit[a][b]));
            }
        }
        // alcova da chave (leste), selada
        int ex = rcx + 5;
        this.bp.air(ex + 1, fy + 1, rcz - 1, ex + 3, fy + 3, rcz + 1);
        this.bp.fill(ex, fy + 1, rcz - 1, ex, fy + 3, rcz + 1, this.seal);
        this.bp.chest(ex + 3, fy + 1, rcz, Direction.WEST, Blueprint.loot("chests/goblin_key_room"));
        this.bp.mob(rcx - 3.5, fy + 1, rcz - 2.5, GoblinContent.ABERRATION::get, null);
        this.bp.mob(rcx + 2.5, fy + 1, rcz - 3.5, GoblinContent.CORRUPTED_GOBLIN::get, null);
    }

    private static boolean allLit(boolean[][] lit) {
        for (boolean[] row : lit) {
            for (boolean b : row) {
                if (!b) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Leste: corredor comprido com armadilhas de flecha e uma sala guardada por um Troll (chave num baú). */
    private void trapRoom() {
        int z0 = this.cellZ(1), fy = this.catY, s = this.mazeSize();
        int xEdge = this.mx + s;
        int rcx = xEdge + 14, rcz = z0 + 1;
        this.bp.fill(xEdge, fy - 1, rcz - 6, rcx + 6, fy + 7, rcz + 6, this.wall);
        this.bp.air(xEdge - U, fy + 1, z0, xEdge - 1, fy + 4, z0 + 2);          // borda do labirinto
        this.bp.air(xEdge, fy + 1, z0, rcx - 5, fy + 4, z0 + 2);                // corredor
        this.bp.fill(xEdge, fy, z0, rcx - 5, fy, z0 + 2, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int k = 0; k < 3; k++) {
            int x = xEdge + 2 + k * 3;
            boolean north = k % 2 == 0;
            int pz = north ? z0 : z0 + 2;
            this.bp.block(x, fy + 1, pz, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState());
            this.bp.dispenser(x, fy + 1, north ? z0 - 1 : z0 + 3, north ? Direction.SOUTH : Direction.NORTH,
                    Blueprint.loot("dispensers/goblin_trap"));
        }
        this.room(rcx - 4, rcz - 4, rcx + 4, rcz + 4, 5);
        this.bp.chest(rcx + 4, fy + 1, rcz, Direction.WEST, Blueprint.loot("chests/goblin_key_room"));
        this.bp.spawner(rcx + 3, fy + 1, rcz - 3, GoblinContent.ABERRATION::get);
        this.bp.fill(rcx - 1, fy + 1, rcz + 3, rcx + 1, fy + 1, rcz + 4, Blocks.BONE_BLOCK.defaultBlockState());
        this.bp.mob(rcx + 0.5, fy + 1, rcz + 0.5, GoblinContent.TROLL::get, null);
    }

    /** Sala retangular com chão, colunas nos cantos e lanternas. */
    private void room(int x0, int z0, int x1, int z1, int height) {
        int fy = this.catY;
        this.bp.air(x0, fy + 1, z0, x1, fy + height, z1);
        this.bp.fill(x0, fy, z0, x1, fy, z1, this.floor);
        for (int[] c : new int[][] {{x0, z0}, {x0, z1}, {x1, z0}, {x1, z1}}) {
            this.bp.fill(c[0], fy + 1, c[1], c[0], fy + height, c[1], Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        }
        this.bp.block((x0 + x1) / 2, fy + height, (z0 + z1) / 2 - 2, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        this.bp.block((x0 + x1) / 2, fy + height, (z0 + z1) / 2 + 2, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    // ================================================================== fosso

    /** Poço redondo da vila até as catacumbas, com escada em caracol em volta de um pilar central. */
    private void pit() {
        int fy = this.catY, top = this.y0 - 1;
        double inner = 1.5, outer = 4.6, shell = 6.2;
        // parede do poço
        this.bp.shape(this.cx - 7, fy - 1, this.cz - 7, this.cx + 7, top - 1, this.cz + 7, (x, y, z) -> {
            double d = Math.hypot(x - this.cx, z - this.cz);
            return d >= outer && d < shell;
        }, this.wall);
        // chão
        this.bp.shape(this.cx - 7, fy, this.cz - 7, this.cx + 7, fy, this.cz + 7,
                (x, y, z) -> Math.hypot(x - this.cx, z - this.cz) < outer, this.floor);
        // ar + degraus + pilar
        final double step = Math.PI * 2 / 16;
        this.bp.shape(this.cx - 5, fy + 1, this.cz - 5, this.cx + 5, top + 12, this.cz + 5, (x, y, z) ->
                Math.hypot(x - this.cx, z - this.cz) < outer, (x, y, z) -> {
                    double d = Math.hypot(x - this.cx, z - this.cz);
                    if (d < inner) {
                        return y > top ? Blocks.AIR.defaultBlockState()
                                : (y - fy) % 5 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : this.wall.pick(x, y, z);
                    }
                    if (y > top) {
                        return Blocks.AIR.defaultBlockState();
                    }
                    double a = Math.atan2(z - this.cz, x - this.cx);
                    if (a < 0) {
                        a += Math.PI * 2;
                    }
                    double stepA = ((y - fy) % 16) * step;
                    double diff = a - stepA;
                    if (diff < 0) {
                        diff += Math.PI * 2;
                    }
                    return diff < step * 1.15 ? this.floor.pick(x, y, z) : Blocks.CAVE_AIR.defaultBlockState();
                });
        // túnel do fundo do poço até a entrada do labirinto
        this.bp.fill(this.cx + 5, fy - 1, this.cz - 3, this.mx - 1, fy + 6, this.cz + 3, this.wall);
        this.bp.fill(this.cx + 3, fy, this.cz - 1, this.mx - 1, fy, this.cz + 1, this.floor);
        this.bp.air(this.cx + 3, fy + 1, this.cz - 1, this.mx - 1, fy + 4, this.cz + 1);
        this.bp.block(this.mx - 2, fy + 4, this.cz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // beirada na superfície (com uma abertura onde está o degrau de cima)
        double topA = ((top - fy) % 16) * step;
        this.bp.shape(this.cx - 7, this.y0, this.cz - 7, this.cx + 7, this.y0, this.cz + 7, (x, y, z) -> {
            double d = Math.hypot(x - this.cx, z - this.cz);
            if (d < outer || d >= outer + 1.0) {
                return false;
            }
            double a = Math.atan2(z - this.cz, x - this.cx);
            if (a < 0) {
                a += Math.PI * 2;
            }
            double diff = Math.abs(a - topA);
            diff = Math.min(diff, Math.PI * 2 - diff);
            return diff > 0.45;
        }, Palette.mix(31, st(Blocks.COBBLED_DEEPSLATE), 60, st(Blocks.MOSSY_COBBLESTONE), 40));
        // estacas com crânios em volta
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            int x = this.cx + (int) Math.round(Math.cos(a) * 7), z = this.cz + (int) Math.round(Math.sin(a) * 7);
            this.bp.fill(x, this.y0, z, x, this.y0 + 1, z, this.fence);
            this.bp.block(x, this.y0 + 2, z, Blocks.SKELETON_SKULL.defaultBlockState());
        }
    }

    // ================================================================== vila

    private void village() {
        int y = this.y0;
        // paliçada de troncos com 4 portões
        this.bp.shape(this.cx - VILLAGE_R - 1, y, this.cz - VILLAGE_R - 1, this.cx + VILLAGE_R + 1, y + 6, this.cz + VILLAGE_R + 1, (x, yy, z) -> {
            double d = Math.hypot(x - this.cx, z - this.cz);
            if (d < VILLAGE_R - 0.5 || d >= VILLAGE_R + 0.5) {
                return false;
            }
            if (Math.abs(x - this.cx) <= 2 || Math.abs(z - this.cz) <= 2) {
                return false; // portões
            }
            int h = 4 + (int) Math.floorMod(Blueprint.hash(x, 0, z, 41), 3L);
            return yy < y + h;
        }, Palette.of(this.log));
        this.bp.shape(this.cx - VILLAGE_R - 1, y + 4, this.cz - VILLAGE_R - 1, this.cx + VILLAGE_R + 1, y + 7, this.cz + VILLAGE_R + 1, (x, yy, z) -> {
            double d = Math.hypot(x - this.cx, z - this.cz);
            if (d < VILLAGE_R - 0.5 || d >= VILLAGE_R + 0.5 || Math.abs(x - this.cx) <= 2 || Math.abs(z - this.cz) <= 2) {
                return false;
            }
            int h = 4 + (int) Math.floorMod(Blueprint.hash(x, 0, z, 41), 3L);
            return yy == y + h && Math.floorMod(x + z, 2) == 0;
        }, Palette.of(this.fence));

        // caminhos de cascalho dos portões até o fosso
        Palette path = Palette.mix(32, st(Blocks.GRAVEL), 50, st(Blocks.COARSE_DIRT), 30, st(Blocks.PACKED_MUD), 20);
        this.bp.fill(this.cx - 1, y - 1, this.cz - VILLAGE_R, this.cx + 1, y - 1, this.cz - 7, path);
        this.bp.fill(this.cx - 1, y - 1, this.cz + 7, this.cx + 1, y - 1, this.cz + VILLAGE_R, path);
        this.bp.fill(this.cx - VILLAGE_R, y - 1, this.cz - 1, this.cx - 7, y - 1, this.cz + 1, path);
        this.bp.fill(this.cx + 7, y - 1, this.cz - 1, this.cx + VILLAGE_R, y - 1, this.cz + 1, path);

        // 8 cabanas em anel
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 8 + k * Math.PI / 4;
            int hx = this.cx + (int) Math.round(Math.cos(a) * 19), hz = this.cz + (int) Math.round(Math.sin(a) * 19);
            this.hut(hx, hz, k);
        }

        // torre de vigia (noroeste, entre cabanas)
        int tx = this.cx - 27, tz = this.cz - 6;
        this.watchtower(tx, tz);

        // fogueiras das almas com bancos de toras
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            int fx = this.cx + (int) Math.round(Math.cos(a) * 11), fz = this.cz + (int) Math.round(Math.sin(a) * 11);
            this.bp.block(fx, y, fz, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            this.bp.block(fx + 2, y, fz, MurkyWood.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            this.bp.block(fx, y, fz + 2, MurkyWood.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            this.bp.mob(fx + 1.5, y, fz - 0.5, GoblinContent.CORRUPTED_GOBLIN::get, null);
        }
        // mais duendes andando pela vila
        for (int k = 0; k < 4; k++) {
            double a = this.rnd.nextDouble() * Math.PI * 2, d = 9 + this.rnd.nextDouble() * 18;
            this.bp.mob(this.cx + Math.cos(a) * d, y, this.cz + Math.sin(a) * d, GoblinContent.CORRUPTED_GOBLIN::get, null);
        }
    }

    private void hut(int hx, int hz, int index) {
        int y = this.y0;
        double r = 3.6;
        boolean ruined = index % 3 == 2;
        // chão, paredes e teto em cone
        this.bp.shape(hx - 4, y - 1, hz - 4, hx + 4, y - 1, hz + 4, (x, yy, z) -> Math.hypot(x - hx, z - hz) < r, Palette.of(st(Blocks.MUD_BRICKS)));
        this.bp.shape(hx - 4, y, hz - 4, hx + 4, y + 2, hz + 4, (x, yy, z) -> {
            double d = Math.hypot(x - hx, z - hz);
            return d < r;
        }, (x, yy, z) -> {
            double d = Math.hypot(x - hx, z - hz);
            if (d < r - 1.0) {
                return Blocks.AIR.defaultBlockState();
            }
            if (ruined && Blueprint.hash(x, yy, z, 51) % 4 == 0) {
                return Blocks.AIR.defaultBlockState();
            }
            return this.hutWall.pick(x, yy, z);
        });
        for (int k = 0; k < 4; k++) {
            double rr = 4.3 - k;
            final int layer = k;
            this.bp.shape(hx - 5, y + 3 + k, hz - 5, hx + 5, y + 3 + k, hz + 5, (x, yy, z) -> {
                double d = Math.hypot(x - hx, z - hz);
                if (ruined && layer < 2 && Blueprint.hash(x, yy, z, 52) % 5 == 0) {
                    return false;
                }
                return d < rr;
            }, Palette.mix(53, this.planks, 70, st(Blocks.DARK_OAK_PLANKS), 30));
        }
        this.bp.block(hx, y + 7, hz, this.fence);
        // porta voltada para o centro da vila
        int dx = Integer.signum(this.cx - hx), dz = Integer.signum(this.cz - hz);
        int doorX, doorZ;
        if (Math.abs(this.cx - hx) > Math.abs(this.cz - hz)) {
            doorX = hx + dx * 3;
            doorZ = hz;
        } else {
            doorX = hx;
            doorZ = hz + dz * 3;
        }
        this.bp.fill(doorX, y, doorZ, doorX, y + 1, doorZ, Blocks.AIR.defaultBlockState());
        // dentro: baú no fundo, e alguma coisa de duende
        int backX = hx - (doorX - hx) / 3 * 2, backZ = hz - (doorZ - hz) / 3 * 2;
        Direction facing = Direction.getApproximateNearest(doorX - hx, 0, doorZ - hz);
        this.bp.chest(backX, y, backZ, facing, Blueprint.loot("chests/goblin_village_hut"));
        switch (index % 4) {
            case 0 -> this.bp.block(hx + (backZ != hz ? 1 : 0), y, hz + (backX != hx ? 1 : 0), Blocks.CAULDRON.defaultBlockState());
            case 1 -> this.bp.block(hx, y, hz, Blocks.BONE_BLOCK.defaultBlockState());
            case 2 -> this.bp.block(hx, y + 2, hz, Blocks.COBWEB.defaultBlockState());
            default -> this.bp.block(hx, y + 2, hz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        this.bp.mob(hx + 0.5 + (doorX - hx) / 3.0, y, hz + 0.5 + (doorZ - hz) / 3.0, GoblinContent.CORRUPTED_GOBLIN::get, null);
    }

    private void watchtower(int tx, int tz) {
        int y = this.y0;
        for (int[] c : new int[][] {{0, 0}, {3, 0}, {0, 3}, {3, 3}}) {
            this.bp.fill(tx + c[0], y, tz + c[1], tx + c[0], y + 8, tz + c[1], this.log);
        }
        this.bp.fill(tx - 1, y + 7, tz - 1, tx + 4, y + 7, tz + 4, this.planks);
        this.bp.shape(tx - 1, y + 8, tz - 1, tx + 4, y + 8, tz + 4,
                (x, yy, z) -> x == tx - 1 || x == tx + 4 || z == tz - 1 || z == tz + 4, Palette.of(this.fence));
        this.bp.fill(tx + 1, y, tz + 1, tx + 1, y + 6, tz + 1, Blocks.LADDER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH));
        this.bp.fill(tx + 1, y, tz, tx + 1, y + 6, tz, this.log);
        this.bp.block(tx + 1, y + 7, tz + 1, Blocks.AIR.defaultBlockState());
        this.bp.block(tx + 2, y + 8, tz + 2, Blocks.SOUL_LANTERN.defaultBlockState());
        this.bp.mob(tx + 2.5, y + 8, tz + 2.5, GoblinContent.CORRUPTED_GOBLIN::get, null);
    }
}
