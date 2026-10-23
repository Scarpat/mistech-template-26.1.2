package com.mistech.mistechmod.item.scroll;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Porta-Pergaminhos: guarda até 9 runas (config) e usa a escolhida como se ela estivesse na mão.
 * <ul>
 * <li>Botão direito: usa a runa escolhida (no ar, num bloco ou numa criatura).</li>
 * <li>Agachado + botão direito: abre o estojo.</li>
 * <li>Tecla Z (ou agachado + roda do mouse): troca de runa.</li>
 * <li>Na outra mão com a Runa de Fogo escolhida, acende incenso, charuto e baseado (gasta da runa).</li>
 * </ul>
 * As runas ficam no componente {@code minecraft:container}; a escolhida em {@code mistech:scroll_selected}.
 */
public class ScrollCaseItem extends Item {
    public static final TagKey<Item> RUNES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "runes"));
    public static final int MAX_SLOTS = 9;

    public ScrollCaseItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean isRune(ItemStack stack) {
        return stack.is(RUNES);
    }

    public static int slots() {
        return Math.max(1, Math.min(MAX_SLOTS, Config.scrollCaseSlots()));
    }

    public static NonNullList<ItemStack> contents(ItemStack scrollCase) {
        NonNullList<ItemStack> list = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        scrollCase.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
        return list;
    }

    public static void setContents(ItemStack scrollCase, NonNullList<ItemStack> list) {
        scrollCase.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        normalize(scrollCase, list, 1);
    }

    public static int selectedIndex(ItemStack scrollCase) {
        return Math.floorMod(scrollCase.getOrDefault(MistechComponents.SCROLL_SELECTED.get(), 0), MAX_SLOTS);
    }

    /** Se a escolhida está vazia, pula para a próxima runa (na direção dada). */
    private static void normalize(ItemStack scrollCase, NonNullList<ItemStack> list, int direction) {
        int index = selectedIndex(scrollCase);
        if (!list.get(index).isEmpty()) {
            return;
        }
        for (int i = 1; i <= MAX_SLOTS; i++) {
            int j = Math.floorMod(index + i * direction, MAX_SLOTS);
            if (!list.get(j).isEmpty()) {
                scrollCase.set(MistechComponents.SCROLL_SELECTED.get(), j);
                return;
            }
        }
    }

    /** A runa escolhida (cópia; vazia se não houver nenhuma). */
    public static ItemStack selected(ItemStack scrollCase) {
        if (!(scrollCase.getItem() instanceof ScrollCaseItem)) {
            return ItemStack.EMPTY;
        }
        return contents(scrollCase).get(selectedIndex(scrollCase));
    }

    public static boolean selectedIs(ItemStack scrollCase, Item rune) {
        return selected(scrollCase).is(rune);
    }

    /** Troca a escolhida para a próxima (+1) ou anterior (-1) que tenha runa. Devolve a nova runa. */
    public static ItemStack cycle(ItemStack scrollCase, int direction) {
        NonNullList<ItemStack> list = contents(scrollCase);
        int index = selectedIndex(scrollCase);
        for (int i = 1; i <= MAX_SLOTS; i++) {
            int j = Math.floorMod(index + i * direction, MAX_SLOTS);
            if (!list.get(j).isEmpty()) {
                scrollCase.set(MistechComponents.SCROLL_SELECTED.get(), j);
                return list.get(j);
            }
        }
        return list.get(index);
    }

    /** Gasta usos da runa escolhida (ex.: acender incenso). A runa some quando acaba. */
    public static void damageSelected(ItemStack scrollCase, int amount, @Nullable ServerLevel level, @Nullable Player player) {
        NonNullList<ItemStack> list = contents(scrollCase);
        int index = selectedIndex(scrollCase);
        ItemStack rune = list.get(index);
        if (rune.isEmpty() || player != null && player.getAbilities().instabuild) {
            return;
        }
        if (rune.isDamageableItem()) {
            int damage = rune.getDamageValue() + amount;
            if (damage >= rune.getMaxDamage()) {
                list.set(index, ItemStack.EMPTY);
                if (level != null && player != null) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.7F);
                }
            } else {
                rune.setDamageValue(damage);
            }
        } else {
            rune.shrink(amount);
        }
        setContents(scrollCase, list);
    }

    // ----- uso -----

    /** Põe a runa escolhida na mão, roda a ação e guarda de volta o que sobrou dela. */
    private InteractionResult delegate(Player player, InteractionHand hand, Function<ItemStack, InteractionResult> action) {
        ItemStack scrollCase = player.getItemInHand(hand);
        NonNullList<ItemStack> list = contents(scrollCase);
        int index = selectedIndex(scrollCase);
        ItemStack rune = list.get(index);
        if (rune.isEmpty()) {
            if (!player.level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.scroll_case.empty"));
            }
            return InteractionResult.FAIL;
        }
        if (player.getCooldowns().isOnCooldown(rune)) {
            return InteractionResult.PASS;
        }
        InteractionResult result;
        ItemStack after;
        player.setItemInHand(hand, rune);
        try {
            result = action.apply(rune);
        } finally {
            after = player.getItemInHand(hand);
            player.setItemInHand(hand, scrollCase);
        }
        if (result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null) {
            after = success.heldItemTransformedTo();
        }
        list.set(index, isRune(after) ? after : ItemStack.EMPTY);
        if (!after.isEmpty() && !isRune(after) && !player.level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(after);
        }
        setContents(scrollCase, list);
        return result instanceof InteractionResult.Success ? InteractionResult.SUCCESS : result;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                open(serverPlayer, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return this.delegate(player, hand, rune -> rune.use(level, player, hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                open(serverPlayer, context.getHand());
            }
            return InteractionResult.SUCCESS;
        }
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        InteractionResult result = this.delegate(player, context.getHand(),
                rune -> rune.useOn(new UseOnContext(context.getLevel(), player, context.getHand(), rune, hit)));
        // runas que só funcionam no ar (ex.: Ar, Luz) também valem mirando um bloco
        if (result == InteractionResult.PASS) {
            result = this.delegate(player, context.getHand(), rune -> rune.use(context.getLevel(), player, context.getHand()));
        }
        return result;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return this.delegate(player, hand, rune -> rune.interactLivingEntity(player, target, hand));
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        ItemStack scrollCase = player.getItemInHand(hand);
        int locked = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : -1;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ScrollCaseMenu(id, inventory,
                new ScrollCaseContainer(scrollCase, p), locked), scrollCase.getHoverName()));
    }

    // ----- visual -----

    @Override
    public boolean isBarVisible(ItemStack stack) {
        ItemStack rune = selected(stack);
        return !rune.isEmpty() && rune.isDamageableItem();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        ItemStack rune = selected(stack);
        return rune.isEmpty() ? 0 : rune.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB78CF0;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return selected(stack).hasFoil();
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack rune = selected(stack);
        Component base = super.getName(stack);
        return rune.isEmpty() ? base : Component.translatable("item.mistech.scroll_case.named", base, rune.getHoverName());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        NonNullList<ItemStack> list = contents(stack);
        int selected = selectedIndex(stack);
        int count = 0;
        for (int i = 0; i < MAX_SLOTS; i++) {
            ItemStack rune = list.get(i);
            if (rune.isEmpty()) {
                continue;
            }
            count++;
            int uses = rune.isDamageableItem() ? rune.getMaxDamage() - rune.getDamageValue() : rune.getCount();
            Component line = Component.literal(i == selected ? "▶ " : "   ").append(rune.getHoverName())
                    .append(Component.literal(" (" + uses + ")").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(i == selected ? line.copy().withStyle(ChatFormatting.LIGHT_PURPLE) : line.copy().withStyle(ChatFormatting.GRAY));
        }
        if (count == 0) {
            tooltip.accept(Component.translatable("message.mistech.scroll_case.empty").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("item.mistech.scroll_case.help").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canFitInsideContainerItems() {
        return true;
    }
}
