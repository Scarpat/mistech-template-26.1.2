package com.mistech.mistechmod.archive;

import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Pacotes da Mesa do Arquivo. O handler do cliente fica em {@code archive.client.ArchiveClient}. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ArchiveNetwork {
    private ArchiveNetwork() {}

    public record Entry(ItemResource resource, long count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemResource.STREAM_CODEC, Entry::resource, ByteBufCodecs.VAR_LONG, Entry::count, Entry::new);
    }

    /** Servidor -> cliente: tudo o que está no Arquivo. */
    public record SyncPayload(int containerId, List<Entry> entries, boolean powered) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "archive_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncPayload::containerId, Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncPayload::entries,
                ByteBufCodecs.BOOL, SyncPayload::powered, SyncPayload::new);

        @Override
        public CustomPacketPayload.Type<SyncPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: clique na grade do Arquivo. */
    public record ActionPayload(int containerId, int action, net.minecraft.world.item.ItemStack stack) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ActionPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "archive_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ActionPayload::containerId, ByteBufCodecs.VAR_INT, ActionPayload::action,
                net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC, ActionPayload::stack, ActionPayload::new);

        @Override
        public CustomPacketPayload.Type<ActionPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC);
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ArchiveTerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleAction(payload.action(), ItemResource.of(payload.stack()));
            }
        });
    }
}
