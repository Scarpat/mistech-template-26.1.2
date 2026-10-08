package com.mistech.mistechmod.tarot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Dados da Fenda Arcana (na metade de baixo): qual arcano, quem abriu e quanto tempo falta. */
public class ArcanaRiftBlockEntity extends BlockEntity {
    /** 60 s aberta: o grupo inteiro pode entrar (qualquer jogador). Fecha antes se a luta acabar. */
    public static final int LIFETIME = 1200;

    /** Fendas abertas por jogador (só no servidor, só enquanto carregadas) — uma por vez. */
    private static final Map<UUID, GlobalPos> OPEN = new HashMap<>();

    private @Nullable TarotArcana arcana;
    private @Nullable UUID owner;
    private int age;
    /** Alguém já entrou (a carta não volta mais). */
    private boolean entered;

    public ArcanaRiftBlockEntity(BlockPos pos, BlockState state) {
        super(TarotContent.ARCANA_RIFT_BE.get(), pos, state);
    }

    public static boolean hasOpenRift(ServerPlayer player) {
        GlobalPos pos = OPEN.get(player.getUUID());
        if (pos == null) {
            return false;
        }
        ServerLevel level = player.level().getServer().getLevel(pos.dimension());
        if (level != null && level.isLoaded(pos.pos()) && level.getBlockEntity(pos.pos()) instanceof ArcanaRiftBlockEntity) {
            return true;
        }
        OPEN.remove(player.getUUID());
        return false;
    }

    void setup(TarotArcana arcana, ServerPlayer owner) {
        this.arcana = arcana;
        this.owner = owner.getUUID();
        this.age = 0;
        if (this.level != null) {
            OPEN.put(owner.getUUID(), GlobalPos.of(this.level.dimension(), this.worldPosition));
        }
        this.setChanged();
    }

    public @Nullable TarotArcana arcana() {
        return this.arcana;
    }

    private GlobalPos globalPos() {
        return GlobalPos.of(this.level.dimension(), this.worldPosition);
    }

    /** Qualquer jogador pode entrar: todos que passam pela mesma fenda lutam juntos na mesma arena. */
    void tryEnter(ServerPlayer player) {
        if (this.arcana == null || !(this.level instanceof ServerLevel)) {
            return;
        }
        if (ArcanePlane.isFighting(player)) {
            return;
        }
        if (this.entered && !ArcanePlane.hasFightAt(this.globalPos())) {
            return; // a luta já acabou; a fenda está fechando
        }
        this.entered = true;
        this.setChanged();
        ArcanePlane.enterNextTick(player, this.arcana, this.globalPos(), this.owner != null ? this.owner : player.getUUID());
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, ArcanaRiftBlockEntity rift) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        rift.age++;
        if (rift.arcana == null || rift.age >= LIFETIME) {
            rift.close(server, true);
            return;
        }
        // a luta acabou (vitória, todos caíram ou o servidor reiniciou): fecha
        if (rift.entered && rift.age % 10 == 0 && !ArcanePlane.hasFightAt(rift.globalPos())) {
            rift.close(server, false);
            return;
        }
        if (rift.age % 10 == 0) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.25, 0.7, 0.25, 0.02);
        }
        if (rift.age == LIFETIME - 100 && rift.owner != null && server.getPlayerByUUID(rift.owner) instanceof ServerPlayer owner) {
            owner.sendOverlayMessage(Component.translatable("message.mistech.tarot.rift_closing"));
        }
    }

    /** Fecha a fenda. @param refund devolve a Carta Invertida se ninguém entrou. */
    private void close(ServerLevel level, boolean refund) {
        if (refund && this.arcana != null && !this.entered) {
            Containers.dropItemStack(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                    new ItemStack(TarotContent.invertedCard(this.arcana)));
            this.entered = true;
        }
        if (this.owner != null) {
            OPEN.remove(this.owner);
        }
        level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.sendParticles(ParticleTypes.PORTAL, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0,
                this.worldPosition.getZ() + 0.5, 40, 0.3, 0.8, 0.3, 0.3);
        level.setBlock(this.worldPosition.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(this.worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.arcana = TarotArcana.byId(input.getStringOr("Arcana", ""));
        Optional<UUID> owner = input.read("Owner", UUIDUtil.CODEC);
        this.owner = owner.orElse(null);
        this.age = input.getIntOr("Age", 0);
        this.entered = input.getBooleanOr("Entered", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.arcana != null) {
            output.putString("Arcana", this.arcana.id());
        }
        if (this.owner != null) {
            output.store("Owner", UUIDUtil.CODEC, this.owner);
        }
        output.putInt("Age", this.age);
        output.putBoolean("Entered", this.entered);
    }
}
