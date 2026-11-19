package com.mistech.mistechmod.endgame;

import java.util.List;
import java.util.UUID;

import com.mistech.mistechmod.celestial.CelestialContent;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.realm.GnomeRealm;
import com.mistech.mistechmod.registry.DreamDeepContent;
import com.mistech.mistechmod.registry.DreamMobs;
import com.mistech.mistechmod.registry.DreamRiftContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Reator Onírico (núcleo de um cubo 3×3×3, ver {@link EndgameStructures#reactor}): queima sonhos e gera {@link #RATE} FE/t.
 * <ul>
 * <li>Combustível: Fragmento de Sonho e Pó de Sonho {@link #DREAM} FE, Fragmento Amplificado {@link #AMPLIFIED} FE,
 * Estilhaço de Memória {@link #MEMORY} FE.</li>
 * <li>×2 enquanto o dono dorme a até {@link #SLEEP_RANGE} blocos (ou está no Reino dos Sonhos); ×1,5 se o reator está no
 * Reino dos Sonhos. O combustível gasta igual: o bônus é energia a mais.</li>
 * <li>Pesadelos: a cada {@link #NIGHTMARE_EVERY} ticks ligado, {@link #NIGHTMARE_CHANCE} de chance de um Bicho-Papão ou
 * uma Sombra aparecer a até 12 blocos — a não ser que haja um Apanha Sonho Purificado a até {@link #CATCHER_RANGE}.</li>
 * </ul>
 * Buffer {@link #CAPACITY} FE; envia até {@link #MAX_OUTPUT} FE/t para cada bloco encostado.
 * info0 = FE/t, info1 = situação, info2 = multiplicador (%).
 */
public class DreamReactorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 10_000_000;
    public static final int RATE = 2_000;
    public static final int MAX_OUTPUT = 10_000;
    public static final int DREAM = 100_000, AMPLIFIED = 250_000, MEMORY = 1_000_000;
    public static final int SLEEP_RANGE = 32;
    public static final int NIGHTMARE_EVERY = 12_000;
    public static final float NIGHTMARE_CHANCE = 0.15F;
    public static final int CATCHER_RANGE = 8;
    public static final int SLOT_FUEL = 0;

    public static final String[] STATUS = {"ok", "structure", "no_fuel", "full"};
    public static final int ST_OK = 0, ST_STRUCTURE = 1, ST_NO_FUEL = 2, ST_FULL = 3;

    private int burnLeft;
    private int burnTotal;
    private int nightmareTimer;
    private int missing = -1;
    private int structureTimer;
    private int status = ST_STRUCTURE;
    private double multiplier = 1.0;

    public DreamReactorBlockEntity(BlockPos pos, BlockState state) {
        super(EndgameContent.DREAM_REACTOR_BE.get(), pos, state, EndgameContent.DREAM_REACTOR_LAYOUT, 0, MAX_OUTPUT);
    }

    public static int fuelValue(ItemStack stack) {
        if (stack.is(DreamDeepContent.MEMORY_SHARD.get())) return MEMORY;
        if (stack.is(DreamRiftContent.AMPLIFIED_DREAM_FRAGMENT.get())) return AMPLIFIED;
        if (stack.is(DreamRiftContent.DREAM_FRAGMENT.get()) || stack.is(DreamDeepContent.DREAM_DUST.get())) return DREAM;
        return 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return fuelValue(stack) > 0;
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--this.structureTimer <= 0 || this.missing < 0) {
            this.structureTimer = 40;
            this.missing = EndgameStructures.missing(level, EndgameStructures.reactor(pos, state.getValue(MachineBlock.FACING)));
        }
        if (level.getGameTime() % 20 == 0) {
            this.multiplier = this.computeMultiplier(level, pos);
        }
        int generated = 0;
        int st;
        int space = this.layout.capacity - this.storedEnergy();
        if (this.missing > 0) {
            st = ST_STRUCTURE;
        } else if (space <= 0) {
            st = ST_FULL;
        } else {
            ItemStack fuel = this.items.get(SLOT_FUEL);
            if (this.burnLeft <= 0 && isFuel(fuel)) {
                this.burnLeft = this.burnTotal = fuelValue(fuel);
                fuel.shrink(1);
                this.setChanged();
            }
            if (this.burnLeft > 0) {
                int burn = Math.min(RATE, this.burnLeft);
                generated = (int) Math.min(space, Math.round(burn * this.multiplier));
                this.burnLeft -= burn;
                this.addEnergy(generated);
                st = ST_OK;
            } else {
                st = ST_NO_FUEL;
            }
        }
        if (st == ST_OK) {
            if (++this.nightmareTimer >= NIGHTMARE_EVERY) {
                this.nightmareTimer = 0;
                this.nightmare(level, pos);
            }
            if (level.getGameTime() % 10 == 0) {
                level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.6, 0.6, 0.6, 0.01);
            }
        }
        this.status = st;
        // progresso em milésimos (a chama mostra quanto falta do combustível atual)
        this.maxProgress = 1000;
        this.progress = this.burnTotal <= 0 ? 0 : (int) (1000L * this.burnLeft / this.burnTotal);
        this.info0 = generated;
        this.info1 = st;
        this.info2 = (int) Math.round(this.multiplier * 100);
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    private double computeMultiplier(ServerLevel level, BlockPos pos) {
        double f = GnomeRealm.isRealm(level) ? 1.5 : 1.0;
        UUID owner = this.owner();
        if (owner != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null && (GnomeRealm.isRealm(player.level())
                    || player.isSleeping() && player.level() == level && player.blockPosition().closerThan(pos, SLEEP_RANGE))) {
                f *= 2.0;
            }
        }
        return f;
    }

    /** Pesadelo: um Bicho-Papão ou uma Sombra sai do reator (o Apanha Sonho Purificado segura). */
    private void nightmare(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= NIGHTMARE_CHANCE || this.guarded(level, pos)) {
            return;
        }
        EntityType<?> type = random.nextBoolean() ? DreamMobs.BOGEYMAN.get() : DreamMobs.SHADE.get();
        for (int tries = 0; tries < 12; tries++) {
            BlockPos at = pos.offset(random.nextInt(25) - 12, random.nextInt(9) - 4, random.nextInt(25) - 12);
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()
                    || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
                continue;
            }
            Entity mob = type.create(level, EntitySpawnReason.EVENT);
            if (mob == null) {
                return;
            }
            mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 20, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, at, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.7F);
            return;
        }
    }

    private boolean guarded(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-CATCHER_RANGE, -CATCHER_RANGE, -CATCHER_RANGE),
                pos.offset(CATCHER_RANGE, CATCHER_RANGE, CATCHER_RANGE))) {
            if (level.getBlockState(p).is(CelestialContent.PURIFIED_DREAMCATCHER.get())) {
                return true;
            }
        }
        return false;
    }

    public static List<Component> infoLines(int fe, int status, int percent) {
        Component first = fe > 0 ? Component.translatable("gui.mistech.fe_rate", fe)
                : Component.translatable("gui.mistech.dream_reactor.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]);
        return percent > 100 ? List.of(first, Component.translatable("gui.mistech.dream_reactor.bonus", percent)) : List.of(first);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("gui.mistech.dream_reactor.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.status))]));
        if (this.status == ST_STRUCTURE) {
            lines.add(Component.translatable("info.mistech.structure_missing", Math.max(0, this.missing)));
        }
        if (this.multiplier > 1.001) {
            lines.add(Component.translatable("gui.mistech.dream_reactor.bonus", (int) Math.round(this.multiplier * 100)));
        }
        super.addInfo(lines);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.burnLeft = input.getIntOr("BurnLeft", 0);
        this.burnTotal = input.getIntOr("BurnTotal", 0);
        this.nightmareTimer = input.getIntOr("Nightmare", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("BurnLeft", this.burnLeft);
        output.putInt("BurnTotal", this.burnTotal);
        output.putInt("Nightmare", this.nightmareTimer);
    }
}
