package com.mistech.mistechmod.machine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Bateria Rúnica Aprimorada: 1.000.000 FE, sem fio.
 * <ul>
 * <li>Suga energia de <b>geradores</b> a até {@link #PULL_RANGE} blocos (cubo em volta), até 1.000 FE/t de cada.</li>
 * <li>Energiza <b>máquinas</b> a até {@link #PUSH_RANGE} blocos, até 1.000 FE/t para cada.</li>
 * <li>Também recebe e entrega por qualquer face (canos, Ponto de Rede Arcana).</li>
 * </ul>
 * Gerador = bloco que só deixa tirar energia (não aceita). Máquina = bloco que aceita energia.
 * Um bloco que já aceitou energia alguma vez nunca é tratado como gerador (máquina cheia não é drenada).
 * Outras Baterias Rúnicas são ignoradas (evita energia rodando em círculo).
 * A área é reescaneada a cada 2 s.
 *
 * Tela: info0 = FE/t entrando, info1 = FE/t saindo, info2 = geradores ligados, info3 = máquinas ligadas.
 */
public class AdvancedRunicBatteryBlockEntity extends RunicBatteryBlockEntity {
    public static final int CAPACITY = 1_000_000;
    public static final int MAX_IO = 10_000;
    public static final int PULL_RANGE = 3;
    public static final int PUSH_RANGE = 5;
    public static final int PER_LINK = 1_000;
    private static final int SCAN_INTERVAL = 40;

    private record Link(BlockPos pos, @Nullable Direction side) {}

    private final List<Link> generators = new ArrayList<>();
    private final List<Link> machines = new ArrayList<>();
    /** Posições que já aceitaram energia: continuam sendo máquinas mesmo cheias. */
    private final Set<BlockPos> knownMachines = new HashSet<>();
    private int scanCooldown;

    public AdvancedRunicBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.ADVANCED_RUNIC_BATTERY.get(), pos, state, MachineLayouts.ADVANCED_RUNIC_BATTERY, MAX_IO, MAX_IO);
    }

    /** Todas as faces recebem e entregam. */
    @Override
    public EnergyHandler handlerFor(@Nullable Direction side) {
        return this.energy;
    }

    @Override
    protected int distribute(ServerLevel level, BlockPos pos, BlockState state) {
        if (--this.scanCooldown <= 0) {
            this.scanCooldown = SCAN_INTERVAL;
            this.scan(level, pos);
        }
        boolean fx = level.getGameTime() % 20 == 0;
        boolean anyLink = false;

        // 1) suga dos geradores
        for (Link link : this.generators) {
            if (this.storedEnergy() >= this.layout.capacity) {
                break;
            }
            EnergyHandler source = level.getCapability(Capabilities.Energy.BLOCK, link.pos(), link.side());
            if (source == null) {
                continue;
            }
            int moved = EnergyHandlerUtil.move(source, this.energy, PER_LINK, null);
            if (moved > 0) {
                this.pulledThisTick += moved;
                anyLink = true;
                if (fx) {
                    spark(level, link.pos(), 3);
                }
            }
        }

        // 2) energiza as máquinas
        int out = 0;
        for (Link link : this.machines) {
            if (this.storedEnergy() <= 0) {
                break;
            }
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, link.pos(), link.side());
            if (target == null) {
                continue;
            }
            int moved = EnergyHandlerUtil.move(this.energy, target, PER_LINK, null);
            if (moved > 0) {
                out += moved;
                anyLink = true;
                if (fx) {
                    spark(level, link.pos(), 2);
                }
            }
        }
        if (fx && anyLink) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    6, 0.2, 0.1, 0.2, 0.02);
        }

        this.info2 = this.generators.size();
        this.info3 = this.machines.size();
        return out;
    }

    private static void spark(ServerLevel level, BlockPos at, int count) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5,
                count, 0.2, 0.2, 0.2, 0.05);
    }

    /** Procura geradores e máquinas em volta. */
    private void scan(Level level, BlockPos center) {
        this.generators.clear();
        this.machines.clear();
        Set<BlockPos> seenMachines = new HashSet<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-PUSH_RANGE, -PUSH_RANGE, -PUSH_RANGE),
                center.offset(PUSH_RANGE, PUSH_RANGE, PUSH_RANGE))) {
            if (p.equals(center) || !level.isLoaded(p)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(p);
            if (be == null || be instanceof RunicBatteryBlockEntity) {
                continue;
            }
            BlockPos at = p.immutable();
            Link link = findLink(level, at, center);
            if (link == null) {
                continue;
            }
            Direction side = link.side();
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, at, side);
            if (handler == null) {
                continue;
            }
            boolean accepts = canInsert(handler);
            if (accepts || this.knownMachines.contains(at)) {
                seenMachines.add(at);
                this.machines.add(link);
            } else if (withinCube(center, at, PULL_RANGE) && canExtract(handler)) {
                this.generators.add(link);
            }
        }
        this.knownMachines.clear();
        this.knownMachines.addAll(seenMachines);
    }

    /** Lado usado para falar com o bloco: sem lado, depois a face virada para a bateria, depois as outras; null = sem energia. */
    private static @Nullable Link findLink(Level level, BlockPos at, BlockPos battery) {
        if (level.getCapability(Capabilities.Energy.BLOCK, at, null) != null) {
            return new Link(at, null);
        }
        Direction toward = Direction.getApproximateNearest(battery.getX() - at.getX(), battery.getY() - at.getY(), battery.getZ() - at.getZ());
        if (level.getCapability(Capabilities.Energy.BLOCK, at, toward) != null) {
            return new Link(at, toward);
        }
        for (Direction d : Direction.values()) {
            if (level.getCapability(Capabilities.Energy.BLOCK, at, d) != null) {
                return new Link(at, d);
            }
        }
        return null;
    }

    private static boolean withinCube(BlockPos a, BlockPos b, int range) {
        return Math.abs(a.getX() - b.getX()) <= range && Math.abs(a.getY() - b.getY()) <= range
                && Math.abs(a.getZ() - b.getZ()) <= range;
    }

    /** Simula colocar 1 FE (nada muda de verdade: a transação não é confirmada). */
    private static boolean canInsert(EnergyHandler handler) {
        try (Transaction tx = Transaction.openRoot()) {
            return handler.insert(1, tx) > 0;
        }
    }

    private static boolean canExtract(EnergyHandler handler) {
        try (Transaction tx = Transaction.openRoot()) {
            return handler.extract(1, tx) > 0;
        }
    }
}
