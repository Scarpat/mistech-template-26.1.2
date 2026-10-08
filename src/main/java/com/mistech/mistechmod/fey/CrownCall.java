package com.mistech.mistechmod.fey;

import java.util.function.Consumer;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.entity.goblin.CorruptedGoblinEntity;
import com.mistech.mistechmod.registry.GoblinContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Chamado da Rainha: com uma coroa equipada, a tecla (G) chama {@link #COUNT} Duendes Corrompidos aliados por
 * {@link #DURATION_TICKS}. Coroa Corrompida: 1 vez por dia de jogo; Coroa Purificada: a cada meio dia, e os duendes vêm "limpos".
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class CrownCall {
    public static final int COUNT = 2;
    /** 3 minutos. */
    public static final int DURATION_TICKS = 3 * 60 * 20;
    public static final long CORRUPTED_COOLDOWN = 24000L;
    public static final long PURIFIED_COOLDOWN = 12000L;

    private CrownCall() {}

    /** Cliente -> servidor: tecla do Chamado da Rainha. */
    public record CrownCallPayload() implements CustomPacketPayload {
        public static final CrownCallPayload INSTANCE = new CrownCallPayload();
        public static final CustomPacketPayload.Type<CrownCallPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "crown_call"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CrownCallPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<CrownCallPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(CrownCallPayload.TYPE, CrownCallPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                tryCall(player);
            }
        });
    }

    public static void tryCall(ServerPlayer player) {
        boolean pure = Accessories.has(player, Accessory.PURIFIED_CROWN);
        boolean corrupted = Accessories.has(player, Accessory.CORRUPTED_CROWN);
        if (!pure && !corrupted) {
            player.sendOverlayMessage(Component.translatable("message.mistech.crown_call.no_crown").withStyle(ChatFormatting.GRAY));
            return;
        }
        ServerLevel level = player.level();
        long now = level.getServer().overworld().getGameTime();
        long ready = player.getData(FeyContent.CROWN_CALL_READY);
        if (now < ready && !player.getAbilities().instabuild) {
            long seconds = (ready - now + 19) / 20;
            player.sendOverlayMessage(Component.translatable("message.mistech.crown_call.cooldown",
                    seconds / 60, String.format("%02d", seconds % 60)).withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        int spawned = 0;
        for (int i = 0; i < COUNT; i++) {
            float angle = player.getRandom().nextFloat() * Mth.TWO_PI;
            double x = player.getX() + Mth.cos(angle) * 2.5;
            double z = player.getZ() + Mth.sin(angle) * 2.5;
            BlockPos column = BlockPos.containing(x, player.getY() + 2, z);
            double y = player.getY();
            // procura chão perto da altura do jogador (cavernas): desce até 4 blocos
            for (int dy = 2; dy >= -4; dy--) {
                BlockPos feet = column.offset(0, dy - 2, 0);
                if (level.getBlockState(feet.below()).isSolid() && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                    y = feet.getY();
                    break;
                }
            }
            if (y == player.getY() && !level.getBlockState(BlockPos.containing(x, y - 1, z)).isSolid()) {
                y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
                if (Math.abs(y - player.getY()) > 6) {
                    x = player.getX();
                    z = player.getZ();
                    y = player.getY();
                }
            }
            CorruptedGoblinEntity goblin = GoblinContent.CORRUPTED_GOBLIN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (goblin == null) {
                continue;
            }
            goblin.snapTo(x, y, z, player.getYRot(), 0.0F);
            goblin.setSummoner(player, pure, DURATION_TICKS);
            if (pure) {
                goblin.setCustomName(Component.translatable("entity.mistech.corrupted_goblin.pure"));
            }
            level.addFreshEntity(goblin);
            level.sendParticles(pure ? ParticleTypes.END_ROD : ParticleTypes.WITCH, x, y + 0.6, z, 20, 0.3, 0.5, 0.3, 0.05);
            spawned++;
        }
        if (spawned == 0) {
            return;
        }
        player.setData(FeyContent.CROWN_CALL_READY, now + (pure ? PURIFIED_COOLDOWN : CORRUPTED_COOLDOWN));
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, pure ? 1.4F : 1.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.PIGLIN_CELEBRATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        player.sendOverlayMessage(Component.translatable(pure ? "message.mistech.crown_call.pure" : "message.mistech.crown_call.done")
                .withStyle(pure ? ChatFormatting.GOLD : ChatFormatting.DARK_PURPLE));
    }

    /** Linhas extras do tooltip das coroas. */
    public static void appendTooltip(Accessory crown, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("tooltip.mistech.crown_call", Component.keybind("key.mistech.crown_call"))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (crown == Accessory.CORRUPTED_CROWN) {
            tooltip.accept(Component.translatable("tooltip.mistech.corrupted_crown.taint").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
