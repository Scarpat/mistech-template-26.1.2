package com.mistech.mistechmod.folklore.machine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mistech.mistechmod.drink.CachacaItem;
import com.mistech.mistechmod.machine.FarmMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Toco do Curupira: fazenda de árvores numa área 9×9 à frente (mais com Alcance/Nuvens/Sagitário). Olha uma coluna por vez:
 * tronco na altura do toco = derruba a árvore inteira (tronco e folhas) e guarda tudo; chão vazio = planta uma muda.
 * {@link #FE_PER_TICK} FE/t trabalhando. O Curupira cobra: 1 Cachaça a cada {@link #TREES_PER_OFFERING} árvores.
 * Sem oferenda, ele para e deixa pegadas viradas para trás em volta.
 * info0 = lado da área, info1 = árvores que ainda pode derrubar, info2 = situação ({@link #STATUS}).
 */
public class CurupiraStumpBlockEntity extends FarmMachineBlockEntity {
    public static final int FE_PER_TICK = 30;
    public static final int TREES_PER_OFFERING = 20;
    public static final int MAX_LOGS = 256, MAX_LEAVES = 768;
    public static final int SLOT_OFFERING = 0, SLOT_SAPLING = 1;
    public static final String[] STATUS = {"working", "hungry", "no_energy", "full"};
    public static final int ST_WORKING = 0, ST_HUNGRY = 1, ST_NO_ENERGY = 2, ST_FULL = 3;

    private int credit;
    private int angryTimer;

    public CurupiraStumpBlockEntity(BlockPos pos, BlockState state) {
        super(FolkMachines.CURUPIRA_STUMP_BE.get(), pos, state, FolkMachines.STUMP_LAYOUT);
    }

    public static boolean isOffering(ItemStack stack) {
        return stack.getItem() instanceof CachacaItem;
    }

    public static boolean isSapling(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS) && stack.getItem() instanceof BlockItem;
    }

    @Override
    protected int radius() {
        return BASE_RADIUS + this.rangeBonus();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        int fe = this.energyStep(FE_PER_TICK);
        int status;
        if (this.credit <= 0 && isOffering(this.items.get(SLOT_OFFERING))) {
            this.items.get(SLOT_OFFERING).shrink(1);
            this.credit += TREES_PER_OFFERING;
            level.playSound(null, pos, SoundEvents.GENERIC_DRINK.value(), SoundSource.BLOCKS, 0.8F, 0.8F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
            this.setChanged();
        }
        if (this.credit <= 0) {
            status = ST_HUNGRY;
            if (++this.angryTimer >= 40) {
                this.angryTimer = 0;
                this.footprints(level, pos);
            }
        } else if (this.outputBlocked()) {
            status = ST_FULL;
        } else if (!this.hasEnergy(fe)) {
            status = ST_NO_ENERGY;
        } else {
            status = ST_WORKING;
            this.drainEnergy(fe);
            if (this.stepReady()) {
                BlockPos column = this.nextColumn(pos, state);
                this.work(level, column);
            }
        }
        if (this.info0 == 0) {
            this.info0 = this.radius() * 2 + 1;
        }
        this.info1 = this.credit;
        this.info2 = status;
        this.setLit(level, pos, state, status == ST_WORKING);
    }

    /** Quanto a coluna olha para baixo e para cima da altura do toco (terreno irregular). */
    public static final int SCAN_DOWN = 4, SCAN_UP = 2;

    @Override
    protected int areaBelow() {
        return SCAN_DOWN;
    }

    @Override
    protected int areaAbove() {
        return SCAN_UP;
    }

    private void work(ServerLevel level, BlockPos column) {
        for (int dy = SCAN_UP; dy >= -SCAN_DOWN; dy--) {
            BlockPos p = column.above(dy);
            if (level.isLoaded(p) && level.getBlockState(p).is(BlockTags.LOGS)) {
                // desce até o pé do tronco
                BlockPos base = p;
                while (base.getY() > column.getY() - SCAN_DOWN - 8 && level.getBlockState(base.below()).is(BlockTags.LOGS)) {
                    base = base.below();
                }
                this.fell(level, base);
                this.credit--;
                this.setChanged();
                this.plant(level, base);
                return;
            }
        }
        // sem árvore: planta no chão da coluna (acima ou abaixo da altura do toco)
        for (int dy = SCAN_UP; dy >= -SCAN_DOWN; dy--) {
            BlockPos p = column.above(dy);
            if (level.isLoaded(p) && level.getBlockState(p).isAir() && !level.getBlockState(p.below()).isAir()) {
                this.plant(level, p);
                return;
            }
        }
    }

    /** Derruba o tronco (para cima e para os lados) e as folhas em volta; guarda tudo. */
    private void fell(ServerLevel level, BlockPos start) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> logs = new ArrayList<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos p = queue.poll();
            logs.add(p);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = p.offset(dx, dy, dz);
                        if (next.getY() >= start.getY() && seen.add(next)
                                && Math.abs(next.getX() - start.getX()) <= 10 && Math.abs(next.getZ() - start.getZ()) <= 10
                                && level.getBlockState(next).is(BlockTags.LOGS)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        // folhas encostadas na copa (até 4 passos de folha em folha)
        List<BlockPos> leaves = new ArrayList<>();
        Deque<BlockPos> leafQueue = new ArrayDeque<>();
        java.util.Map<BlockPos, Integer> depth = new java.util.HashMap<>();
        for (BlockPos log : logs) {
            for (Direction d : Direction.values()) {
                BlockPos n = log.relative(d);
                if (!depth.containsKey(n) && level.getBlockState(n).is(BlockTags.LEAVES)) {
                    depth.put(n, 1);
                    leafQueue.add(n);
                }
            }
        }
        while (!leafQueue.isEmpty() && leaves.size() < MAX_LEAVES) {
            BlockPos p = leafQueue.poll();
            leaves.add(p);
            int d0 = depth.get(p);
            if (d0 >= 4) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!depth.containsKey(n) && level.getBlockState(n).is(BlockTags.LEAVES)) {
                    depth.put(n, d0 + 1);
                    leafQueue.add(n);
                }
            }
        }
        for (BlockPos p : leaves) {
            this.harvest(level, p);
        }
        for (int i = logs.size() - 1; i >= 0; i--) {
            this.harvest(level, logs.get(i));
        }
        level.playSound(null, start, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5, 8, 0.6, 0.6, 0.6, 0.0);
    }

    private void harvest(ServerLevel level, BlockPos p) {
        BlockState state = level.getBlockState(p);
        List<ItemStack> drops = Block.getDrops(state, level, p, level.getBlockEntity(p));
        level.removeBlock(p, false);
        for (ItemStack drop : drops) {
            ItemStack rest = drop;
            if (isSapling(rest)) {
                rest = this.toSaplings(rest);
            }
            if (!rest.isEmpty()) {
                rest = this.insertProduct(level, rest);
            }
            if (!rest.isEmpty()) {
                Block.popResource(level, p, rest);
            }
        }
    }

    /** Mudas que caem voltam primeiro para o espaço de mudas. */
    private ItemStack toSaplings(ItemStack stack) {
        ItemStack there = this.items.get(SLOT_SAPLING);
        if (there.isEmpty()) {
            this.items.set(SLOT_SAPLING, stack.copy());
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(there, stack)) {
            int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
            there.grow(move);
            ItemStack rest = stack.copy();
            rest.shrink(move);
            return rest;
        }
        return stack;
    }

    /** Planta uma muda do espaço de mudas em {@code pos}, se estiver vazio e o chão servir. */
    private void plant(ServerLevel level, BlockPos pos) {
        ItemStack sapling = this.items.get(SLOT_SAPLING);
        if (!isSapling(sapling) || !level.getBlockState(pos).isAir()) {
            return;
        }
        BlockState plant = ((BlockItem) sapling.getItem()).getBlock().defaultBlockState();
        if (!plant.canSurvive(level, pos)) {
            return;
        }
        level.setBlock(pos, plant, Block.UPDATE_ALL);
        sapling.shrink(1);
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
        this.setChanged();
    }

    /** Sem oferenda: pegadas viradas para trás aparecem em volta do toco. */
    private void footprints(ServerLevel level, BlockPos pos) {
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < 4; i++) {
            double r = 1.5 + i * 0.9;
            double x = pos.getX() + 0.5 + Math.cos(angle) * r + (i % 2 == 0 ? 0.15 : -0.15);
            double z = pos.getZ() + 0.5 + Math.sin(angle) * r;
            level.sendParticles(ParticleTypes.SQUID_INK, x, pos.getY() + 0.05, z, 2, 0.05, 0.0, 0.05, 0.0);
        }
        if (level.getRandom().nextInt(4) == 0) {
            level.playSound(null, pos, SoundEvents.FOX_SNIFF, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("gui.mistech.curupira_stump.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.info2))]));
        lines.add(Component.translatable("gui.mistech.curupira_stump.credit", this.credit));
        super.addInfo(lines);
    }

    /** Linhas da tela (cliente). */
    public static List<Component> infoLines(int side, int credit, int status) {
        return List.of(Component.translatable("gui.mistech.curupira_stump.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]),
                Component.translatable("gui.mistech.curupira_stump.credit", credit));
    }

    @Override
    protected void loadExtra(ValueInput input) {
        super.loadExtra(input);
        this.credit = input.getIntOr("Credit", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        super.saveExtra(output);
        output.putInt("Credit", this.credit);
    }
}
