package com.mistech.mistechmod.item.scroll;

import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/** Registro do Porta-Pergaminhos (item, tela e o pacote de trocar a runa). */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ScrollCaseContent {
    private ScrollCaseContent() {}

    public static final DeferredItem<ScrollCaseItem> SCROLL_CASE = Mistech.ITEMS.registerItem("scroll_case", ScrollCaseItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final Supplier<MenuType<ScrollCaseMenu>> MENU = MistechMenus.MENUS.register("scroll_case",
            () -> new MenuType<>(ScrollCaseMenu::new, FeatureFlags.VANILLA_SET));

    public static void init() {
        // carrega a classe (registros acima)
    }

    /** Cliente → servidor: trocar a runa do Porta-Pergaminhos da mão (+1 próxima, -1 anterior). */
    public record CyclePayload(int direction) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CyclePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "scroll_case_cycle"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CyclePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, CyclePayload::direction, CyclePayload::new);

        @Override
        public CustomPacketPayload.Type<CyclePayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(CyclePayload.TYPE, CyclePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                cycle(player, payload.direction() < 0 ? -1 : 1);
            }
        });
    }

    /** Mão principal primeiro, depois a outra. */
    public static void cycle(ServerPlayer player, int direction) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ScrollCaseItem) {
                NonNullList<ItemStack> list = ScrollCaseItem.contents(stack);
                if (list.stream().allMatch(ItemStack::isEmpty)) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.scroll_case.empty"));
                    return;
                }
                ItemStack rune = ScrollCaseItem.cycle(stack, direction);
                int uses = rune.isDamageableItem() ? rune.getMaxDamage() - rune.getDamageValue() : rune.getCount();
                player.sendOverlayMessage(Component.translatable("message.mistech.scroll_case.selected", rune.getHoverName(), uses));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.4F);
                return;
            }
        }
    }
}
