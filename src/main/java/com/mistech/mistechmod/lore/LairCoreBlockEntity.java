package com.mistech.mistechmod.lore;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Guarda a arena de um chefe do folclore. Estados: dormindo → (jogador chega a {@link #radius()} blocos) acordado,
 * com o chefe nascendo no ponto marcado da arena → derrotado (o chefe morreu) → depois de {@link #RESPAWN_TICKS}
 * volta a dormir e o chefe pode aparecer de novo. Se o chefe some sem morrer (descarregou longe), volta a dormir em 30 s.
 */
public class LairCoreBlockEntity extends BlockEntity {
    /** Marca no chefe ({@code getPersistentData}) com a posição do coração da toca, para avisar quando ele morre. */
    public static final String LAIR_TAG = "mistech_lair";
    public static final int RESPAWN_TICKS = 24000 * 5;
    private static final int DORMANT = 0, AWAKE = 1, DEFEATED = 2;

    private String kind = "cuca";
    private int state = DORMANT;
    private int timer;
    private int sx, sy, sz;
    private @Nullable UUID boss;

    public LairCoreBlockEntity(BlockPos pos, BlockState state) {
        super(LoreContent.LAIR_CORE_BE.get(), pos, state);
    }

    /** Chamado na geração da estrutura: qual chefe e onde ele nasce (relativo ao coração). */
    public void setup(String kind, int dx, int dy, int dz) {
        this.kind = kind;
        this.sx = dx;
        this.sy = dy;
        this.sz = dz;
        this.setChanged();
    }

    private boolean isMapinguari() {
        return "mapinguari".equals(this.kind);
    }

    private int radius() {
        return this.isMapinguari() ? 15 : 11;
    }

    public void onBossDefeated() {
        this.state = DEFEATED;
        this.timer = RESPAWN_TICKS;
        this.boss = null;
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LairCoreBlockEntity be) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0) {
            return;
        }
        switch (be.state) {
            case DEFEATED -> {
                be.timer -= 20;
                if (be.timer <= 0) {
                    be.state = DORMANT;
                    be.timer = 0;
                }
                be.setChanged();
            }
            case AWAKE -> {
                Entity e = be.boss == null ? null : server.getEntity(be.boss);
                if (e != null && e.isAlive()) {
                    be.timer = 0;
                    return;
                }
                be.timer += 20;
                if (be.timer >= 600) {
                    be.state = DORMANT;
                    be.timer = 0;
                    be.boss = null;
                    be.setChanged();
                }
            }
            default -> be.tryAwaken(server, pos);
        }
    }

    private void tryAwaken(ServerLevel level, BlockPos pos) {
        int r = this.radius();
        BlockPos spawn = pos.offset(this.sx, this.sy, this.sz);
        AABB area = new AABB(spawn).inflate(r, 10, r);
        List<Player> players = level.getEntitiesOfClass(Player.class, area, p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
        if (players.isEmpty()) {
            return;
        }
        Class<? extends Mob> cls = this.isMapinguari() ? MapinguariEntity.class : CucaEntity.class;
        if (!level.getEntitiesOfClass(cls, new AABB(spawn).inflate(64)).isEmpty()) {
            return; // já tem um (de ritual ou natural) por perto
        }
        EntityType<? extends Mob> type = this.isMapinguari() ? LoreContent.MAPINGUARI.get() : LoreContent.CUCA.get();
        Mob mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) {
            return;
        }
        Player target = players.get(0);
        mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                (float) Math.toDegrees(Math.atan2(target.getX() - spawn.getX(), spawn.getZ() - target.getZ())), 0.0F);
        mob.setPersistenceRequired();
        mob.getPersistentData().putLong(LAIR_TAG, pos.asLong());
        mob.setTarget(target);
        level.addFreshEntity(mob);
        this.boss = mob.getUUID();
        this.state = AWAKE;
        this.timer = 0;
        this.setChanged();

        double x = spawn.getX() + 0.5, y = spawn.getY() + 1.5, z = spawn.getZ() + 0.5;
        if (this.isMapinguari()) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 60, 1.5, 1.5, 1.5, 0.02);
            level.playSound(null, spawn, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4.0F, 0.5F);
        } else {
            level.sendParticles(ParticleTypes.WITCH, x, y, z, 120, 1.0, 1.5, 1.0, 0.2);
            level.playSound(null, spawn, SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        }
        String key = this.isMapinguari() ? "message.mistech.mapinguari.cave" : "message.mistech.cuca.lair";
        ChatFormatting color = this.isMapinguari() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_PURPLE;
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(spawn).inflate(48))) {
            p.sendSystemMessage(Component.translatable(key).withStyle(color));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Kind", this.kind);
        output.putInt("State", this.state);
        output.putInt("Timer", this.timer);
        output.putInt("SX", this.sx);
        output.putInt("SY", this.sy);
        output.putInt("SZ", this.sz);
        if (this.boss != null) {
            output.store("Boss", UUIDUtil.CODEC, this.boss);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.kind = input.getStringOr("Kind", "cuca");
        this.state = input.getIntOr("State", DORMANT);
        this.timer = input.getIntOr("Timer", 0);
        this.sx = input.getIntOr("SX", 0);
        this.sy = input.getIntOr("SY", 0);
        this.sz = input.getIntOr("SZ", 0);
        this.boss = input.read("Boss", UUIDUtil.CODEC).orElse(null);
    }
}
