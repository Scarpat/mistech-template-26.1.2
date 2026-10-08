package com.mistech.mistechmod.tarot.priestess;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * O Santuário de Bolso do Portal da Sacerdotisa: a dimensão {@code mistech:pocket_sanctum} (vazia), onde cada jogador
 * tem a sua sala de <b>tamanho fixo</b> ({@link #SIZE}×{@link #SIZE}, {@link #HEIGHT} de altura), a
 * {@link #SPACING} blocos uma da outra.
 * <ul>
 * <li>Entra e sai com o Portal (botão direito). A saída fica guardada no jogador (dimensão, posição e rotação).</li>
 * <li>Saída fixa: a <b>magnetita</b> entre os pilares, no fundo da sala — clicar nela leva de volta (para quem perdeu
 * o item). Ela não quebra e volta se sumir.</li>
 * <li>As paredes, o chão e o teto não quebram e não dá para colocar blocos fora do espaço de dentro.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class PocketSanctum {
    public static final int SIZE = 15;
    public static final int HEIGHT = 7;
    public static final int SPACING = 512;
    public static final int FLOOR_Y = 64;
    private static final String RETURN_TAG = "mistech_pocket_return";

    private PocketSanctum() {}

    // ------------------------------------------------------------------ dados salvos (quem tem qual sala)

    public static class Data extends SavedData {
        private record Room(UUID owner, int index, boolean built) {
            static final Codec<Room> CODEC = RecordCodecBuilder.create(i -> i.group(
                    UUIDUtil.CODEC.fieldOf("owner").forGetter(Room::owner),
                    Codec.INT.fieldOf("index").forGetter(Room::index),
                    Codec.BOOL.optionalFieldOf("built", false).forGetter(Room::built)
            ).apply(i, Room::new));
        }

        public static final Codec<Data> CODEC = Room.CODEC.listOf().optionalFieldOf("rooms", java.util.List.of()).codec()
                .xmap(Data::new, data -> java.util.List.copyOf(data.rooms.values()));
        public static final SavedDataType<Data> TYPE = new SavedDataType<>(
                Identifier.fromNamespaceAndPath(Mistech.MODID, "pocket_sanctum"), Data::new, CODEC);

        private final Map<UUID, Room> rooms = new HashMap<>();

        public Data() {}

        private Data(java.util.List<Room> list) {
            for (Room room : list) {
                this.rooms.put(room.owner(), room);
            }
        }

        static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        /** Índice da sala do jogador (cria se ainda não tiver). */
        int index(UUID owner) {
            Room room = this.rooms.get(owner);
            if (room == null) {
                int next = 0;
                for (Room other : this.rooms.values()) {
                    next = Math.max(next, other.index() + 1);
                }
                room = new Room(owner, next, false);
                this.rooms.put(owner, room);
                this.setDirty();
            }
            return room.index();
        }

        boolean built(UUID owner) {
            Room room = this.rooms.get(owner);
            return room != null && room.built();
        }

        void markBuilt(UUID owner) {
            Room room = this.rooms.get(owner);
            if (room != null && !room.built()) {
                this.rooms.put(owner, new Room(owner, room.index(), true));
                this.setDirty();
            }
        }
    }

    // ------------------------------------------------------------------ geometria

    public static boolean isPocket(Level level) {
        return level.dimension() == PriestessContent.POCKET_SANCTUM;
    }

    /** Canto (x, z mínimos) do espaço de dentro da sala {@code index}; o chão fica em y = {@link #FLOOR_Y}. */
    static BlockPos origin(int index) {
        return new BlockPos(index * SPACING, FLOOR_Y, 0);
    }

    /** Dentro do espaço livre de alguma sala (onde se pode construir)? */
    static boolean inInterior(BlockPos pos) {
        int lx = Math.floorMod(pos.getX(), SPACING);
        return lx < SIZE && pos.getZ() >= 0 && pos.getZ() < SIZE && pos.getY() > FLOOR_Y && pos.getY() <= FLOOR_Y + HEIGHT
                && !isExit(pos);
    }

    static BlockPos exit(BlockPos origin) {
        return origin.offset(SIZE / 2, 1, 1);
    }

    static boolean isExit(BlockPos pos) {
        int lx = Math.floorMod(pos.getX(), SPACING);
        return lx == SIZE / 2 && pos.getZ() == 1 && pos.getY() == FLOOR_Y + 1;
    }

    static Vec3 arrival(BlockPos origin) {
        return new Vec3(origin.getX() + SIZE / 2 + 0.5, FLOOR_Y + 1, origin.getZ() + SIZE - 2.5);
    }

    // ------------------------------------------------------------------ entrar / sair

    /** Usar o Portal: entra (fora do santuário) ou volta (dentro). */
    public static void use(ServerPlayer player) {
        if (isPocket(player.level())) {
            leave(player);
        } else {
            enter(player);
        }
    }

    private static void enter(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel pocket = server.getLevel(PriestessContent.POCKET_SANCTUM);
        if (pocket == null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.priestess_portal.no_dimension"));
            return;
        }
        Data data = Data.get(pocket);
        BlockPos origin = origin(data.index(player.getUUID()));
        if (!data.built(player.getUUID())) {
            build(pocket, origin);
            data.markBuilt(player.getUUID());
        }
        ensureExit(pocket, origin);

        CompoundTag back = new CompoundTag();
        back.putString("Dim", player.level().dimension().identifier().toString());
        back.putDouble("X", player.getX());
        back.putDouble("Y", player.getY());
        back.putDouble("Z", player.getZ());
        back.putFloat("Yaw", player.getYRot());
        player.getPersistentData().put(RETURN_TAG, back);

        effects(player.level(), player.position());
        Vec3 arrival = arrival(origin);
        player.teleport(new TeleportTransition(pocket, arrival, Vec3.ZERO, 180.0F, 0.0F, TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        effects(pocket, arrival);
    }

    public static void leave(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel target = null;
        Vec3 pos = null;
        float yaw = player.getYRot();
        CompoundTag back = player.getPersistentData().getCompoundOrEmpty(RETURN_TAG);
        if (back.contains("Dim")) {
            Identifier dimId = Identifier.tryParse(back.getStringOr("Dim", "minecraft:overworld"));
            ServerLevel level = dimId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
            if (level != null && !isPocket(level)) {
                target = level;
                pos = new Vec3(back.getDoubleOr("X", 0.0), back.getDoubleOr("Y", 64.0), back.getDoubleOr("Z", 0.0));
                yaw = back.getFloatOr("Yaw", yaw);
            }
        }
        if (target == null) {
            // sem saída guardada: ponto de nascer do mundo
            target = server.overworld();
            BlockPos spawn = target.getRespawnData().pos();
            pos = Vec3.atBottomCenterOf(target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn));
        }
        player.getPersistentData().remove(RETURN_TAG);
        effects(player.level(), player.position());
        player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, yaw, 0.0F, TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        effects(target, pos);
    }

    private static void effects(Level level, Vec3 at) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 40, 0.4, 0.8, 0.4, 0.05);
            server.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 1.0, at.z, 10, 0.3, 0.6, 0.3, 0.02);
            server.playSound(null, BlockPos.containing(at), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 0.8F);
            server.playSound(null, BlockPos.containing(at), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25F, 1.6F);
        }
    }

    // ------------------------------------------------------------------ a sala

    /**
     * Constrói a sala: chão em xadrez (preto Boaz / branco Jachin) com um tapete azul, paredes de ladrilhos de ardósia
     * com faixa de prismarinho, teto de prismarinho escuro com lanternas do mar, os dois pilares no fundo e a magnetita
     * (saída) entre eles.
     */
    static void build(ServerLevel level, BlockPos origin) {
        int flags = Block.UPDATE_CLIENTS;
        for (int x = -1; x <= SIZE; x++) {
            for (int z = -1; z <= SIZE; z++) {
                boolean wallX = x == -1 || x == SIZE;
                boolean wallZ = z == -1 || z == SIZE;
                for (int y = 0; y <= HEIGHT + 1; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state;
                    if (y == 0) {
                        state = wallX || wallZ ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                                : ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
                    } else if (y == HEIGHT + 1) {
                        state = !wallX && !wallZ && x % 4 == 3 && z % 4 == 3 ? Blocks.SEA_LANTERN.defaultBlockState()
                                : Blocks.DARK_PRISMARINE.defaultBlockState();
                    } else if (wallX || wallZ) {
                        state = y == 3 ? Blocks.PRISMARINE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(pos, state, flags);
                }
            }
        }
        // tapete do meio até a saída
        for (int z = 2; z < SIZE; z++) {
            level.setBlock(origin.offset(SIZE / 2, 1, z), Blocks.BLUE_CARPET.defaultBlockState(), flags);
        }
        // pilares Boaz (preto) e Jachin (branco)
        for (int y = 1; y <= HEIGHT; y++) {
            level.setBlock(origin.offset(SIZE / 2 - 3, y, 1), y == HEIGHT ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.POLISHED_BASALT.defaultBlockState(), flags);
            level.setBlock(origin.offset(SIZE / 2 + 3, y, 1), y == HEIGHT ? Blocks.SEA_LANTERN.defaultBlockState()
                    : Blocks.QUARTZ_PILLAR.defaultBlockState(), flags);
        }
        // o véu atrás da saída
        for (int x = SIZE / 2 - 2; x <= SIZE / 2 + 2; x++) {
            for (int y = 1; y <= HEIGHT; y++) {
                level.setBlock(origin.offset(x, y, 0), (x + y) % 3 == 0 ? Blocks.RED_STAINED_GLASS.defaultBlockState()
                        : Blocks.BLUE_STAINED_GLASS.defaultBlockState(), flags);
            }
        }
        level.setBlock(origin.offset(SIZE / 2 - 1, 1, 1), Blocks.QUARTZ_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST), flags);
        level.setBlock(origin.offset(SIZE / 2 + 1, 1, 1), Blocks.QUARTZ_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST), flags);
    }

    static void ensureExit(ServerLevel level, BlockPos origin) {
        BlockPos exit = exit(origin);
        if (!level.getBlockState(exit).is(Blocks.LODESTONE)) {
            level.setBlock(exit, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ proteção e saída fixa

    @SubscribeEvent
    static void onUseExit(PlayerInteractEvent.RightClickBlock event) {
        if (!isPocket(event.getLevel()) || !isExit(event.getPos()) || !event.getLevel().getBlockState(event.getPos()).is(Blocks.LODESTONE)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof Level level && isPocket(level) && !inInterior(event.getPos())
                && !event.getPlayer().hasInfiniteMaterials()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && isPocket(level) && !inInterior(event.getPos())
                && !(event.getEntity() instanceof Player player && player.hasInfiniteMaterials())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onExplosion(ExplosionEvent.Detonate event) {
        if (isPocket(event.getLevel())) {
            event.getAffectedBlocks().removeIf(pos -> !inInterior(pos));
        }
    }
}
