package com.mistech.mistechmod.folklore.machine;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.folklore.FolkloreContent;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Moinho do Saci: gerador de vento. Precisa de um Saci Engarrafado (não é gasto) e de ar livre nos 3×3 blocos na frente,
 * onde giram as pás. FE pela altura: {@link #FE_LOW} FE/t até Y 64, sobe em linha reta até {@link #FE_HIGH} em Y 200.
 * Chuva +25%, tempestade +50%. Uma Folha de Tabaco Seca a cada 10 min deixa o Saci contente (+25%); sem fumo,
 * 2% por minuto de ele aprontar: o moinho para por 30 s.
 * info0 = FE/t; info1 = situação ({@link #STATUS}).
 */
public class SaciWindmillBlockEntity extends MachineBlockEntity {
    public static final int SLOT_TOBACCO = 0, SLOT_SACI = 1;
    public static final int FE_LOW = 20, FE_HIGH = 150;
    public static final int Y_LOW = 64, Y_HIGH = 200;
    public static final int TOBACCO_TICKS = 12_000;
    public static final int PRANK_TICKS = 600;
    public static final float PRANK_CHANCE = 0.02F;
    public static final int MAX_OUTPUT = 300;
    public static final String[] STATUS = {"happy", "grumpy", "prank", "no_saci", "blocked", "full"};
    public static final int ST_HAPPY = 0, ST_GRUMPY = 1, ST_PRANK = 2, ST_NO_SACI = 3, ST_BLOCKED = 4, ST_FULL = 5;

    private int tobaccoLeft;
    private int prankLeft;
    private int minuteTimer;
    private int checkTimer;
    private boolean clear;

    /** Só no cliente (renderer das pás). */
    public float clientSpin;
    public double clientSpinClock;

    public SaciWindmillBlockEntity(BlockPos pos, BlockState state) {
        super(FolkMachines.SACI_WINDMILL_BE.get(), pos, state, FolkMachines.WINDMILL_LAYOUT, 0, MAX_OUTPUT);
    }

    public static boolean isTobacco(ItemStack stack) {
        return stack.is(Mistech.DRIED_TOBACCO_LEAF.get());
    }

    public static boolean isSaci(ItemStack stack) {
        return stack.is(FolkloreContent.SACI_JAR.get());
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    /** As 9 casas na frente do moinho (onde giram as pás) estão livres? */
    public static boolean bladesClear(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos front = pos.relative(facing);
        Direction side = facing.getClockWise();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos p = front.relative(side, a).above(b);
                BlockState there = level.getBlockState(p);
                if (!there.getCollisionShape(level, p).isEmpty() || !there.getFluidState().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** FE/t base pela altura. */
    public static double heightRate(int y) {
        if (y <= Y_LOW) {
            return FE_LOW;
        }
        if (y >= Y_HIGH) {
            return FE_HIGH;
        }
        return FE_LOW + (FE_HIGH - FE_LOW) * (y - Y_LOW) / (double) (Y_HIGH - Y_LOW);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
        if (--this.checkTimer <= 0) {
            this.checkTimer = 100;
            this.clear = bladesClear(level, pos, facing);
        }
        int generated = 0;
        int status;
        if (!isSaci(this.items.get(SLOT_SACI))) {
            status = ST_NO_SACI;
        } else if (!this.clear) {
            status = ST_BLOCKED;
        } else if (this.prankLeft > 0) {
            this.prankLeft--;
            status = ST_PRANK;
            if (this.prankLeft % 20 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.02);
            }
        } else {
            if (this.tobaccoLeft <= 0 && isTobacco(this.items.get(SLOT_TOBACCO))) {
                this.items.get(SLOT_TOBACCO).shrink(1);
                this.tobaccoLeft = TOBACCO_TICKS;
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.01);
                this.setChanged();
            }
            boolean happy = this.tobaccoLeft > 0;
            double rate = heightRate(pos.getY());
            if (level.isThundering()) {
                rate *= 1.5;
            } else if (level.isRaining()) {
                rate *= 1.25;
            }
            if (happy) {
                rate *= 1.25;
                this.tobaccoLeft--;
            }
            int space = this.layout.capacity - this.storedEnergy();
            if (space <= 0) {
                status = ST_FULL;
            } else {
                generated = Math.min(space, (int) Math.round(rate));
                int before = this.storedEnergy();
                this.addEnergy(generated);
                generated = this.storedEnergy() - before;
                status = happy ? ST_HAPPY : ST_GRUMPY;
                // sem fumo, o Saci apronta
                if (!happy && ++this.minuteTimer >= 1200) {
                    this.minuteTimer = 0;
                    if (level.getRandom().nextFloat() < PRANK_CHANCE) {
                        this.prank(level, pos);
                    }
                }
            }
        }
        this.info0 = generated;
        this.info1 = status;
        this.maxProgress = TOBACCO_TICKS;
        this.progress = this.tobaccoLeft;
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    private void prank(ServerLevel level, BlockPos pos) {
        this.prankLeft = PRANK_TICKS;
        level.playSound(null, pos, SoundEvents.WITCH_CELEBRATE, SoundSource.BLOCKS, 1.0F, 1.8F);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 15, 0.4, 0.3, 0.4, 0.03);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16))) {
            player.sendOverlayMessage(Component.translatable("message.mistech.saci_windmill.prank"));
        }
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("gui.mistech.saci_windmill.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.info1))]));
        super.addInfo(lines);
    }

    /** Linhas da tela (cliente). */
    public static List<Component> infoLines(int generating, int status) {
        String id = STATUS[Math.max(0, Math.min(STATUS.length - 1, status))];
        return List.of(Component.translatable("gui.mistech.generating", generating),
                Component.translatable("gui.mistech.saci_windmill.status." + id));
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tobaccoLeft = input.getIntOr("Tobacco", 0);
        this.prankLeft = input.getIntOr("Prank", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Tobacco", this.tobaccoLeft);
        output.putInt("Prank", this.prankLeft);
    }
}
