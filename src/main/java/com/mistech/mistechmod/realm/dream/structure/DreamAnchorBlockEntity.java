package com.mistech.mistechmod.realm.dream.structure;

import com.mistech.mistechmod.registry.DreamMobs;
import com.mistech.mistechmod.registry.DreamStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Vigia uma caixa (relativa ao bloco). Quando um jogador entra nela:
 * <ul>
 * <li>{@link #LOOP}: as primeiras {@code loops} vezes, joga o jogador de volta {@code (dx,dy,dz)} — num lugar igualzinho,
 * então parece que o corredor/escada não acaba. Depois deixa passar.</li>
 * <li>{@link #BOGEYMAN}: uma vez só, solta {@code loops} Bichos-Papões no ponto {@code (dx,dy,dz)} (embaixo das camas).</li>
 * </ul>
 * A contagem fica nos dados do jogador (chave pela posição do bloco).
 */
public class DreamAnchorBlockEntity extends BlockEntity {
    public static final int LOOP = 0, BOGEYMAN = 1;

    private int mode = LOOP;
    private int x0, y0, z0, x1, y1, z1;
    private int dx, dy, dz;
    private int loops = 3;
    private String message = "";
    private boolean used;

    public DreamAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(DreamStructures.ANCHOR_BE.get(), pos, state);
    }

    /** Configura (na geração). Caixa e destino são relativos ao bloco. */
    public void configure(int mode, int x0, int y0, int z0, int x1, int y1, int z1, int dx, int dy, int dz, int loops, String message) {
        this.mode = mode;
        this.x0 = Math.min(x0, x1);
        this.y0 = Math.min(y0, y1);
        this.z0 = Math.min(z0, z1);
        this.x1 = Math.max(x0, x1);
        this.y1 = Math.max(y0, y1);
        this.z1 = Math.max(z0, z1);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.loops = loops;
        this.message = message;
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DreamAnchorBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 2 != 0 || (be.mode == BOGEYMAN && be.used)) {
            return;
        }
        AABB zone = new AABB(pos.getX() + be.x0, pos.getY() + be.y0, pos.getZ() + be.z0, pos.getX() + be.x1 + 1, pos.getY() + be.y1 + 1,
                pos.getZ() + be.z1 + 1);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, zone)) {
            if (player.isSpectator()) {
                continue;
            }
            if (be.mode == BOGEYMAN) {
                be.releaseBogeymen(server, pos, player);
                return;
            }
            be.loop(server, pos, player);
        }
    }

    private void loop(ServerLevel level, BlockPos pos, ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        String key = "mistech_dream_loop_" + pos.asLong();
        int done = data.getIntOr(key, 0);
        if (done >= this.loops || level.getGameTime() < data.getLongOr("mistech_dream_loop_cd", 0L)) {
            return;
        }
        data.putInt(key, done + 1);
        data.putLong("mistech_dream_loop_cd", level.getGameTime() + 10);
        player.teleportTo(player.getX() + this.dx, player.getY() + this.dy, player.getZ() + this.dz);
        player.resetFallDistance();
        if (!this.message.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(this.message + (done + 1 >= this.loops ? ".last" : "")));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.6F, 0.5F + 0.15F * done);
    }

    private void releaseBogeymen(ServerLevel level, BlockPos pos, ServerPlayer player) {
        this.used = true;
        this.setChanged();
        BlockPos at = pos.offset(this.dx, this.dy, this.dz);
        for (int i = 0; i < Math.max(1, this.loops); i++) {
            var mob = DreamMobs.BOGEYMAN.get().create(level, EntitySpawnReason.STRUCTURE);
            if (mob == null) {
                continue;
            }
            mob.snapTo(at.getX() + 0.5 + i * 2 - (this.loops - 1), at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            mob.setTarget(player);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.SQUID_INK, mob.getX(), mob.getY() + 0.5, mob.getZ(), 20, 0.4, 0.3, 0.4, 0.02);
        }
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        level.playSound(null, at, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.0F, 1.4F);
        if (!this.message.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(this.message));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Mode", this.mode);
        output.putIntArray("Zone", new int[] {this.x0, this.y0, this.z0, this.x1, this.y1, this.z1});
        output.putIntArray("Offset", new int[] {this.dx, this.dy, this.dz});
        output.putInt("Loops", this.loops);
        output.putString("Message", this.message);
        output.putBoolean("Used", this.used);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.mode = input.getIntOr("Mode", LOOP);
        int[] z = input.getIntArray("Zone").orElse(new int[6]);
        if (z.length == 6) {
            this.x0 = z[0];
            this.y0 = z[1];
            this.z0 = z[2];
            this.x1 = z[3];
            this.y1 = z[4];
            this.z1 = z[5];
        }
        int[] o = input.getIntArray("Offset").orElse(new int[3]);
        if (o.length == 3) {
            this.dx = o[0];
            this.dy = o[1];
            this.dz = o[2];
        }
        this.loops = input.getIntOr("Loops", 3);
        this.message = input.getStringOr("Message", "");
        this.used = input.getBooleanOr("Used", false);
    }
}
