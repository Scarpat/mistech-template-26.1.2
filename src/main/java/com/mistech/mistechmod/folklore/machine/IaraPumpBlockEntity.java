package com.mistech.mistechmod.folklore.machine;

import java.util.List;

import com.mistech.mistechmod.block.FluidTank;
import com.mistech.mistechmod.celestial.MachineSky;
import com.mistech.mistechmod.lore.LoreContent;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.PurityContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Bomba da Iara: com uma Escama da Iara no espaço (não é gasta), puxa blocos de fonte de qualquer fluido num raio de
 * {@link #RADIUS} (e até {@link #DEPTH} para baixo) para um tanque de {@link #TANK} mB: 1 bloco a cada {@link #INTERVAL} ticks,
 * {@link #FE_PER_BLOCK} FE cada. Num bioma de rio ou oceano, dá água sem fim ({@link #RIVER_PER_TICK} mB/t, sem secar nada).
 * Perto de uma <b>fonte infinita</b> de água (poça 3×1 ou 2×2, como no vanilla) também dá água sem fim, {@link #INFINITE_PER_TICK} mB/t.
 * Balde vazio no espaço de cima sai cheio embaixo. À noite, quem estiver a até 8 blocos sem Arruda ouve o canto (Náusea leve).
 * info0 = situação ({@link #STATUS}), info1 = mB no tanque.
 */
public class IaraPumpBlockEntity extends MachineBlockEntity {
    public static final int TANK = 16_000;
    public static final int RADIUS = 16, DEPTH = 16;
    public static final int INTERVAL = 10;
    public static final int FE_PER_BLOCK = 50;
    public static final int RIVER_PER_TICK = 50;
    public static final int INFINITE_PER_TICK = 25;
    /** Onde procurar uma fonte infinita em volta da bomba (blocos para os lados / para baixo). */
    public static final int INFINITE_RADIUS = 4, INFINITE_DEPTH = 4;
    public static final int SLOT_BUCKET = 0, SLOT_FILLED = 1, SLOT_SCALE = 2;
    /** Posições olhadas por tentativa de achar uma fonte. */
    private static final int SCAN_PER_TRY = 2048;
    public static final String[] STATUS = {"pumping", "river", "no_scale", "no_energy", "dry", "full", "infinite"};
    public static final int ST_PUMPING = 0, ST_RIVER = 1, ST_NO_SCALE = 2, ST_NO_ENERGY = 3, ST_DRY = 4, ST_FULL = 5, ST_INFINITE = 6;

    private final FluidTank tank = new FluidTank(TANK, this::setChanged);
    private int cursor;
    private int timer;
    private int songTimer;
    /** Há fonte infinita em volta? (procurada a cada {@link #INFINITE_CHECK} ticks) */
    private boolean infinite;
    private int infiniteTimer;
    private static final int INFINITE_CHECK = 100;

    public IaraPumpBlockEntity(BlockPos pos, BlockState state) {
        this(FolkMachines.IARA_PUMP_BE.get(), pos, state, FolkMachines.PUMP_LAYOUT);
    }

    /** Para a Bomba do Boitatá (lava). */
    protected IaraPumpBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state,
            com.mistech.mistechmod.machine.MachineLayout layout) {
        super(type, pos, state, layout, 500, 0);
    }

    @Override
    protected boolean autoFluidInput() {
        return false;
    }

    // ----- ganchos (a Bomba do Boitatá muda) -----

    /** O catalisador no espaço (não é gasto). */
    protected boolean hasCatalyst() {
        return isScale(this.items.get(SLOT_SCALE));
    }

    /** Fluidos que esta bomba puxa. */
    protected boolean pumps(Fluid fluid) {
        return true;
    }

    /** Fluxo sem fim deste lugar (mB/t; 0 = nenhum) e o status mostrado. */
    protected int endlessFlow(ServerLevel level, BlockPos pos) {
        if (level.getBiome(pos).is(BiomeTags.IS_RIVER) || level.getBiome(pos).is(BiomeTags.IS_OCEAN)) {
            this.endlessStatus = ST_RIVER;
            return RIVER_PER_TICK;
        }
        if (this.infinite) {
            this.endlessStatus = ST_INFINITE;
            return INFINITE_PER_TICK;
        }
        return 0;
    }

    protected Fluid endlessFluid() {
        return Fluids.WATER;
    }

    protected int endlessStatus = ST_RIVER;

    /** Procura uma fonte infinita: fonte com 2+ fontes vizinhas na horizontal e chão firme (ou fonte) embaixo. */
    protected boolean findInfinite(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-INFINITE_RADIUS, -INFINITE_DEPTH, -INFINITE_RADIUS),
                pos.offset(INFINITE_RADIUS, 0, INFINITE_RADIUS))) {
            if (level.isLoaded(p) && isInfiniteWater(level, p)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInfiniteWater(ServerLevel level, BlockPos p) {
        FluidState here = level.getFluidState(p);
        if (!here.isSource() || !here.is(net.minecraft.tags.FluidTags.WATER)) {
            return false;
        }
        int sources = 0;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            FluidState n = level.getFluidState(p.relative(d));
            if (n.isSource() && n.is(net.minecraft.tags.FluidTags.WATER)) {
                sources++;
            }
        }
        if (sources < 2) {
            return false;
        }
        BlockPos below = p.below();
        FluidState under = level.getFluidState(below);
        return level.getBlockState(below).isSolid() || under.isSource() && under.is(net.minecraft.tags.FluidTags.WATER);
    }

    /** Efeito enquanto trabalha (a Iara canta). */
    protected void working(ServerLevel level, BlockPos pos) {
        this.song(level, pos);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public static boolean isScale(ItemStack stack) {
        return stack.is(LoreContent.IARA_SCALE.get());
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    private boolean accepts(Fluid fluid) {
        return this.tank.isEmpty() || this.tank.getFluid().getFluid().isSame(fluid);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        int status;
        if (--this.infiniteTimer <= 0) {
            this.infiniteTimer = INFINITE_CHECK;
            this.infinite = this.findInfinite(level, pos);
        }
        int endless = this.hasCatalyst() ? this.endlessFlow(level, pos) : 0;
        if (!this.hasCatalyst()) {
            status = ST_NO_SCALE;
        } else if (this.tank.getAmount() + 1000 > TANK) {
            status = ST_FULL;
        } else if (endless > 0 && this.accepts(this.endlessFluid())) {
            // rio, oceano, fonte infinita (ou mar de lava, no Boitatá): fluido sem fim
            int amount = Math.round(endless * MachineSky.iara(this.ownerMask));
            int fe = this.energyStep(FE_PER_BLOCK / 20);
            if (!this.hasEnergy(fe)) {
                status = ST_NO_ENERGY;
            } else {
                this.drainEnergy(fe);
                this.tank.fill(FluidResource.of(this.endlessFluid()), amount, false);
                status = this.endlessStatus;
            }
        } else {
            status = this.info0 == ST_DRY ? ST_DRY : ST_PUMPING;
            int interval = Math.max(1, Math.round(INTERVAL * 2.0F / Math.max(1, this.workStep()) / MachineSky.iara(this.ownerMask)));
            if (++this.timer >= interval) {
                this.timer = 0;
                if (!this.hasEnergy(FE_PER_BLOCK)) {
                    status = ST_NO_ENERGY;
                } else {
                    status = this.pumpOne(level, pos) ? ST_PUMPING : ST_DRY;
                }
            } else if (!this.hasEnergy(FE_PER_BLOCK)) {
                status = ST_NO_ENERGY;
            }
        }
        this.fillBuckets();
        boolean working = status == ST_PUMPING || status == ST_RIVER || status == ST_INFINITE;
        if (working) {
            this.working(level, pos);
        }
        this.info0 = status;
        this.info1 = this.tank.getAmount();
        this.setLit(level, pos, state, working);
    }

    /** Procura a próxima fonte (cursor pela área) e puxa. */
    private boolean pumpOne(ServerLevel level, BlockPos pos) {
        int side = RADIUS * 2 + 1;
        int total = side * side * (DEPTH + 1);
        for (int i = 0; i < SCAN_PER_TRY; i++) {
            if (this.cursor >= total) {
                this.cursor = 0;
                return false; // deu a volta sem achar (o próximo tick recomeça)
            }
            int c = this.cursor++;
            int dy = -(c / (side * side));
            int rest = c % (side * side);
            BlockPos p = pos.offset(rest % side - RADIUS, dy, rest / side - RADIUS);
            if (!level.isLoaded(p)) {
                continue;
            }
            FluidState fluid = level.getFluidState(p);
            if (fluid.isEmpty() || !fluid.isSource() || !this.pumps(fluid.getType()) || !this.accepts(fluid.getType())) {
                continue;
            }
            BlockState there = level.getBlockState(p);
            Fluid type = fluid.getType();
            if (there.getBlock() instanceof LiquidBlock) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else if (there.getBlock() instanceof BucketPickup pickup) {
                if (pickup.pickupBlock(null, level, p, there).isEmpty()) {
                    continue;
                }
            } else {
                continue;
            }
            this.tank.fill(FluidResource.of(type), 1000, false);
            this.drainEnergy(FE_PER_BLOCK);
            level.sendParticles(ParticleTypes.SPLASH, p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.05);
            this.cursor--; // olha a mesma casa de novo (pode ter outra fonte se o fluido voltar)
            return true;
        }
        return true; // ainda procurando
    }

    private void fillBuckets() {
        ItemStack bucket = this.items.get(SLOT_BUCKET);
        if (bucket.isEmpty() || !isBucket(bucket) || this.tank.getAmount() < 1000) {
            return;
        }
        Item filled = this.tank.getFluid().getFluid().getBucket();
        if (filled == Items.AIR) {
            return;
        }
        ItemStack out = new ItemStack(filled);
        if (this.canOutput(out)) {
            bucket.shrink(1);
            this.tank.drain(FluidResource.of(this.tank.getFluid()), 1000, false);
            this.insertOutput(out);
        }
    }

    /** O canto da Iara: à noite, Náusea leve em quem está perto sem Arruda. */
    private void song(ServerLevel level, BlockPos pos) {
        if (++this.songTimer < 100 || !level.isDarkOutside()) {
            return;
        }
        this.songTimer = 0;
        boolean sang = false;
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8), p -> !p.isSpectator() && !hasRue(p))) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, true, false, true));
            sang = true;
        }
        if (sang) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.6F, 0.6F + level.getRandom().nextFloat() * 0.6F);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.4F, 1.2F);
        }
    }

    public static boolean hasRue(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(PurityContent.RUE_LEAF.get()) || s.is(PurityContent.RUE_INCENSE.get()) || s.is(PurityContent.RUE_CIGAR.get())
                    || s.is(com.mistech.mistechmod.drink.DrinkContent.RUE_CACHACA.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addInfo(List<Component> lines) {
        FluidStack fluid = this.tank.getFluid();
        if (!fluid.isEmpty()) {
            lines.add(Component.translatable("info.mistech.tank", fluid.getHoverName(), fluid.getAmount(), TANK));
        }
        lines.add(Component.translatable(this.statusPrefix() + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.info0))]));
        super.addInfo(lines);
    }

    protected String statusPrefix() {
        return "gui.mistech.iara_pump.status.";
    }

    /** Linhas da tela (cliente). */
    public static List<Component> infoLines(int status) {
        return List.of(Component.translatable("gui.mistech.iara_pump.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]));
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.deserialize(input);
        this.cursor = input.getIntOr("Cursor", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        this.tank.serialize(output);
        output.putInt("Cursor", this.cursor);
    }
}
