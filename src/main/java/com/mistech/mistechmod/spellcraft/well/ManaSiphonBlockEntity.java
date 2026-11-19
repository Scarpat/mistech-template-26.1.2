package com.mistech.mistechmod.spellcraft.well;

import java.util.List;
import java.util.UUID;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.spellcraft.ManaService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Sangria Arcana: mana do jogador → FE. Segurar o botão direito no bloco drena {@link Config#siphonManaPerSecond()} de mana por
 * segundo e gera {@link Config#siphonFePerMana()} FE por ponto; para sozinha em {@link Config#siphonFloor()} da mana máxima.
 * Corrompido ou pior rende 2×, mas cada 100 de mana drenada dá {@link Config#siphonTaint()} de Mácula.
 * Buffer 20.000 FE; manda até {@link #MAX_OUTPUT} FE/t para cada vizinho.
 * info0 = FE/t gerando agora, info1 = situação ({@link #STATUS}).
 */
public class ManaSiphonBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int MAX_OUTPUT = 500;
    /** Ticks depois do último "uso" em que o jogador ainda conta como segurando (o jogo repete o clique a cada 4). */
    public static final int HOLD_WINDOW = 6;
    public static final double REACH = 5.0;
    public static final String[] STATUS = {"idle", "draining", "low_mana", "full", "tainted"};
    public static final int ST_IDLE = 0, ST_DRAINING = 1, ST_LOW = 2, ST_FULL = 3, ST_TAINTED = 4;

    private UUID user;
    private long lastUse = -100;
    private boolean warned;

    public ManaSiphonBlockEntity(BlockPos pos, BlockState state) {
        super(WellContent.MANA_SIPHON_BE.get(), pos, state, WellContent.SIPHON_LAYOUT, 0, MAX_OUTPUT);
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    /** O jogador "usou" o bloco (chega a cada 4 ticks enquanto o botão fica apertado). */
    public void use(ServerLevel level, Player player) {
        if (this.user == null || !this.user.equals(player.getUUID()) || level.getGameTime() - this.lastUse > HOLD_WINDOW) {
            this.warned = false;
        }
        this.user = player.getUUID();
        this.lastUse = level.getGameTime();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        int generated = 0;
        int status = ST_IDLE;
        Player player = this.user == null ? null : level.getPlayerByUUID(this.user);
        if (player != null && level.getGameTime() - this.lastUse <= HOLD_WINDOW && player.isAlive()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH * REACH) {
            float floor = ManaService.max(player) * Config.siphonFloor();
            float mana = ManaService.get(player);
            float drain = Math.min(Config.siphonManaPerSecond() / 20.0F, mana - floor);
            int space = this.layout.capacity - this.storedEnergy();
            if (drain <= 0.001F) {
                status = ST_LOW;
                if (!this.warned) {
                    this.warned = true;
                    player.sendOverlayMessage(Component.translatable("message.mistech.mana_siphon.low").withStyle(ChatFormatting.GRAY));
                }
            } else if (space <= 0) {
                status = ST_FULL;
            } else {
                boolean tainted = Taint.stage(player).ordinal() >= Taint.Stage.CORRUPTED.ordinal();
                ManaService.set(player, mana - drain);
                double fe = drain * Config.siphonFePerMana() * (tainted ? 2.0 : 1.0);
                int before = this.storedEnergy();
                this.addEnergyExact(fe);
                generated = this.storedEnergy() - before;
                if (tainted) {
                    Taint.add(player, drain / 100.0F * Config.siphonTaint());
                    status = ST_TAINTED;
                } else {
                    status = ST_DRAINING;
                }
                if (level.getGameTime() % 2 == 0) {
                    Vec3 from = player.position().add(0, 1.1, 0);
                    Vec3 to = Vec3.atCenterOf(pos).add(0, 0.5, 0);
                    Vec3 p = from.lerp(to, level.getRandom().nextDouble());
                    Vec3 dir = to.subtract(from).normalize();
                    level.sendParticles(tainted ? ParticleTypes.WITCH : ParticleTypes.ENCHANT, p.x, p.y, p.z, 0, dir.x, dir.y, dir.z, 0.3);
                }
                if (level.getGameTime() % 20 == 0) {
                    level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5F, tainted ? 0.7F : 1.4F);
                }
            }
        }
        this.info0 = generated;
        this.info1 = status;
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    private double carry;

    /** Soma FE com casas decimais (a drenagem por tick é fracionária); passa pelo config dos geradores e pelos engastes. */
    private void addEnergyExact(double amount) {
        double total = amount + this.carry;
        int whole = (int) Math.floor(total);
        this.carry = total - whole;
        if (whole > 0) {
            this.addEnergy(whole);
        }
    }

    /** Poção de Mana na Sangria: a mana do pote vira FE na hora (devolve o pote). @return false se não coube */
    public boolean pour(ServerLevel level, Player player, net.minecraft.world.InteractionHand hand) {
        double fe = ManaPotionItem.MANA * Config.siphonFePerMana();
        int space = this.layout.capacity - this.storedEnergy();
        if (space < fe * com.mistech.mistechmod.Config.generatorOutput()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.mana_siphon.full").withStyle(ChatFormatting.GRAY));
            return false;
        }
        this.addEnergyExact(fe);
        com.mistech.mistechmod.purity.block.PurityFluids.swap(player, hand, new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.GLASS_BOTTLE));
        BlockPos pos = this.worldPosition;
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.6);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.6F);
        this.setChanged();
        return true;
    }

    @Override
    public void addInfo(List<Component> lines) {
        if (this.info0 > 0) {
            lines.add(Component.translatable("gui.mistech.generating", this.info0));
        }
        super.addInfo(lines);
    }

    /** Linhas da tela (cliente). */
    public static List<Component> infoLines(int generating, int status) {
        String id = STATUS[Math.max(0, Math.min(STATUS.length - 1, status))];
        return List.of(Component.translatable("gui.mistech.mana_siphon.status." + id),
                Component.translatable("gui.mistech.generating", generating),
                Component.translatable("gui.mistech.mana_siphon.hint"));
    }
}
