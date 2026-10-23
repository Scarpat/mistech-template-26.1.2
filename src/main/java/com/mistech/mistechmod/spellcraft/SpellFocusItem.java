package com.mistech.mistechmod.spellcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Item que lança magias: Tomos, Varinhas (e, depois, Cajados). Tem um número de <b>espaços</b> onde o jogador escolhe
 * quais magias conhecidas levar e um <b>tier máximo</b> (os melhores itens aceitam magias melhores). Pode ter bônus ou
 * penalidades por elemento no gasto de mana (ex.: Tomo das Chamas: Fogo −10%, Gelo +10%) e na recarga.
 * <ul>
 * <li>Botão direito: lança a magia do espaço escolhido · Shift + botão direito: abre a tela dos espaços.</li>
 * <li>Shift + roda: próximo espaço com magia. Funciona em qualquer mão. A barra (HUD) mostra os espaços.</li>
 * </ul>
 */
public class SpellFocusItem extends Item {
    /** Características do item. {@code manaMods}: id do elemento → multiplicador do custo de mana. */
    public record Focus(int slots, int maxTier, Map<String, Double> manaMods, double cooldownMult, int reserve, double power, double channelMult, String element) {
        public Focus(int slots, int maxTier) {
            this(slots, maxTier, Map.of(), 1.0, 0, 0.0, 1.0, "");
        }

        public Focus(int slots, int maxTier, Map<String, Double> manaMods, double cooldownMult) {
            this(slots, maxTier, manaMods, cooldownMult, 0, 0.0, 1.0, "");
        }

        /** Cajado: reserva de mana, poder extra (0,1 = +10%) e pulsos contínuos mais baratos. */
        public static Focus staff(int slots, int maxTier, int reserve, double power, double channelMult) {
            return new Focus(slots, maxTier, Map.of(), 1.0, reserve, power, channelMult, "");
        }

        /** Tomo de elemento: −20% de mana no seu elemento, +20% no oposto; reforça a ressonância que termina nele. */
        public static Focus tome(int slots, int maxTier, RuneElement element) {
            RuneElement opposite = com.mistech.mistechmod.affinity.Affinity.opposite(element);
            Map<String, Double> mods = opposite == element ? Map.of(element.id(), 0.8) : Map.of(element.id(), 0.8, opposite.id(), 1.2);
            return new Focus(slots, maxTier, mods, 1.0, 0, 0.0, 1.0, element.id());
        }

        public double manaMult(RuneElement element) {
            return this.manaMods.getOrDefault(element.id(), 1.0);
        }

        public double powerMult() {
            return 1.0 + this.power;
        }
    }

    /** Onde o jogador estava ao começar um ritual (se sair do lugar, o ritual quebra). */
    private static final Map<java.util.UUID, net.minecraft.world.phys.Vec3> RITUAL_START = new java.util.HashMap<>();
    private static final java.util.Set<java.util.UUID> RITUAL_DONE = new java.util.HashSet<>();

    private final Focus focus;

    public SpellFocusItem(Item.Properties properties, Focus focus) {
        super(properties);
        this.focus = focus;
    }

    public Focus focus() {
        return this.focus;
    }

    /** Marca do Tomo Proibido no campo {@code element} do Focus. */
    public static final String FORBIDDEN = "forbidden";

    public static boolean forbiddenHeld(Player player) {
        return held(player).getItem() instanceof SpellFocusItem item && FORBIDDEN.equals(item.focus.element());
    }

    // ----- engastes: runas gravadas no item (cada uma: −5% de mana e +5% de poder nas magias do seu elemento) -----
    public static final double GEM_MANA = 0.95;
    public static final double GEM_POWER = 0.05;

    public static List<String> gems(ItemStack stack) {
        return stack.getOrDefault(SpellcraftContent.FOCUS_GEMS.get(), List.<String>of());
    }

    public static int maxGems(ItemStack stack) {
        return stack.getItem() instanceof SpellFocusItem item ? (item.focus.slots() + 1) / 2 : 0;
    }

    private static int gemCount(ItemStack stack, RuneElement element) {
        int n = 0;
        for (String id : gems(stack)) {
            if (id.equals(element.id())) {
                n++;
            }
        }
        return n;
    }

    /** Multiplicador de mana deste item (com engastes) para o elemento. */
    public static double manaMult(ItemStack stack, RuneElement element) {
        return stack.getItem() instanceof SpellFocusItem item ? item.focus.manaMult(element) * Math.pow(GEM_MANA, gemCount(stack, element)) : 1.0;
    }

    /** Multiplicador de poder deste item (com engastes) para o elemento. */
    public static double powerMult(ItemStack stack, RuneElement element) {
        return stack.getItem() instanceof SpellFocusItem item ? item.focus.powerMult() + GEM_POWER * gemCount(stack, element) : 1.0;
    }

    /** Runa no off-hand: grava (sem Shift) ou arranca (com Shift). Devolve se tratou. */
    private static boolean handleGem(ServerPlayer player, ItemStack focusStack, ItemStack rune) {
        RuneElement element = RuneElement.ofRune(rune);
        if (element == null) {
            return false;
        }
        List<String> gems = new ArrayList<>(gems(focusStack));
        if (player.isShiftKeyDown()) {
            if (!gems.remove(element.id())) {
                player.sendOverlayMessage(Component.translatable("message.mistech.gem.none", Component.translatable(element.translationKey())).withStyle(ChatFormatting.GRAY));
                return true;
            }
            focusStack.set(SpellcraftContent.FOCUS_GEMS.get(), gems);
            if (!player.getInventory().add(new ItemStack(element.rune()))) {
                player.drop(new ItemStack(element.rune()), false);
            }
            player.sendOverlayMessage(Component.translatable("message.mistech.gem.removed", Component.translatable(element.translationKey())).withColor(element.color()));
            return true;
        }
        if (gems.size() >= maxGems(focusStack)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.gem.full", maxGems(focusStack)).withStyle(ChatFormatting.RED));
            return true;
        }
        gems.add(element.id());
        focusStack.set(SpellcraftContent.FOCUS_GEMS.get(), gems);
        if (!player.getAbilities().instabuild) {
            rune.shrink(1);
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.gem.added", Component.translatable(element.translationKey())).withColor(element.color()));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof SpellFocusItem;
    }

    /** O item de lançar que o jogador segura (principal primeiro), ou vazio. */
    public static ItemStack held(Player player) {
        return is(player.getMainHandItem()) ? player.getMainHandItem() : is(player.getOffhandItem()) ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    /** Ids das magias nos espaços (completa com "" até o número de espaços do item). */
    public static List<String> slots(ItemStack stack) {
        List<String> list = new ArrayList<>(stack.getOrDefault(SpellcraftContent.SPELL_SLOTS.get(), List.<String>of()));
        int count = stack.getItem() instanceof SpellFocusItem item ? item.focus.slots() : list.size();
        while (list.size() < count) {
            list.add("");
        }
        return list.size() > count ? new ArrayList<>(list.subList(0, count)) : list;
    }

    public static int selectedIndex(ItemStack stack) {
        List<String> slots = slots(stack);
        return slots.isEmpty() ? 0 : Math.floorMod(stack.getOrDefault(SpellcraftContent.TOME_SELECTED.get(), 0), slots.size());
    }

    public static @Nullable Spell selected(ItemStack stack) {
        List<String> slots = slots(stack);
        return slots.isEmpty() ? null : Spells.byId(slots.get(selectedIndex(stack)));
    }

    /** O jogador pode pôr esta magia neste item? (conhecida e dentro do tier máximo) */
    public static boolean canHold(Player player, ItemStack stack, Spell spell) {
        return stack.getItem() instanceof SpellFocusItem item && spell.tier() <= item.focus.maxTier() && SpellBook.of(player).knows(spell.id());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && handleGem(serverPlayer, stack, player.getOffhandItem())) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenFocusPayload(hand == InteractionHand.MAIN_HAND ? 0 : 1));
            return InteractionResult.SUCCESS;
        }
        Spell spell = selected(stack);
        if (spell == null) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.spell.empty_slot").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (spell.tier() > this.focus.maxTier()) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.spell.too_strong").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (serverPlayer.isUsingItem()) {
            return InteractionResult.CONSUME;
        }
        double mana = manaMult(stack, spell.element());
        if (spell.kind() == Spell.Kind.CHARGE || spell.kind() == Spell.Kind.RITUAL) {
            if (SpellCaster.check(serverPlayer, spell, mana) != SpellCaster.Result.OK) {
                return InteractionResult.FAIL;
            }
            RITUAL_DONE.remove(serverPlayer.getUUID());
            RITUAL_START.put(serverPlayer.getUUID(), serverPlayer.position());
            if (spell.kind() == Spell.Kind.CHARGE) {
                com.mistech.mistechmod.spellcraft.fx.Vfx.circle(serverPlayer.level(), serverPlayer.position(), spell.element(), 1.5, spell.castTicks() + 40);
            }
            serverPlayer.startUsingItem(hand);
            level.playSound(null, serverPlayer.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.7F);
            return InteractionResult.SUCCESS;
        }
        boolean ok = SpellCaster.tryCast(serverPlayer, spell, mana, this.focus.cooldownMult(), 0.0, powerMult(stack, spell.element())) == SpellCaster.Result.OK;
        if (ok && spell.channelTicks() > 0) {
            serverPlayer.startUsingItem(hand);
        }
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        Spell spell = selected(stack);
        if (spell == null || spell.kind() == Spell.Kind.INSTANT) {
            player.stopUsingItem();
            return;
        }
        int elapsed = 72000 - ticksRemaining;
        switch (spell.kind()) {
            case CHANNEL -> {
                if (elapsed > 0 && elapsed % spell.channelTicks() == 0
                        && !SpellCaster.pulse(player, spell, manaMult(stack, spell.element()) * this.focus.channelMult(), powerMult(stack, spell.element()))) {
                    player.stopUsingItem();
                }
            }
            case CHARGE -> {
                double factor = Math.min(1.0, elapsed / (double) spell.castTicks());
                if (elapsed % 2 == 0) {
                    SpellFx.gather(player.level(), player, spell.element(), factor);
                }
                if (elapsed == spell.castTicks()) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
                    SpellFx.ring(player.level(), player.position().add(0, 1.0, 0), 1.0, SpellFx.dust(spell.element(), 1.6F), 24);
                    com.mistech.mistechmod.spellcraft.fx.Vfx.flare(player.level(), player.position().add(0, 1.2, 0), spell.element(), 1.6F, 8);
                }
            }
            case RITUAL -> {
                net.minecraft.world.phys.Vec3 start = RITUAL_START.get(player.getUUID());
                if (start != null && player.position().subtract(start).horizontalDistanceSqr() > 0.5) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.spell.ritual_broken").withStyle(ChatFormatting.RED));
                    RITUAL_DONE.add(player.getUUID());
                    player.stopUsingItem();
                    return;
                }
                SpellFx.ritualRise(player.level(), player, spell.element(), elapsed, spell.castTicks());
                if (elapsed >= spell.castTicks()) {
                    RITUAL_DONE.add(player.getUUID());
                    SpellCaster.tryCast(player, spell, manaMult(stack, spell.element()), this.focus.cooldownMult(), 0.0, powerMult(stack, spell.element()));
                    player.stopUsingItem();
                }
            }
            default -> { }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTime) {
        Spell spell = selected(stack);
        if (!(user instanceof ServerPlayer player) || spell == null) {
            return false;
        }
        int elapsed = 72000 - remainingTime;
        switch (spell.kind()) {
            case CHARGE -> {
                if (elapsed >= 6) {
                    double factor = Math.min(1.0, elapsed / (double) spell.castTicks());
                    SpellCaster.tryCast(player, spell, manaMult(stack, spell.element()), this.focus.cooldownMult(), factor, powerMult(stack, spell.element()));
                }
            }
            case RITUAL -> {
                if (!RITUAL_DONE.contains(player.getUUID())) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.spell.ritual_cancelled").withStyle(ChatFormatting.GRAY));
                }
            }
            default -> { }
        }
        return false;
    }

    /**
     * Chamado sempre que o uso para — soltando o botão, trocando de item ou ao terminar um ritual. As contínuas começam a
     * recarga aqui (antes, trocar de espaço na hotbar pulava a recarga) e o estado do ritual é limpo.
     */
    @Override
    public void onStopUsing(ItemStack stack, LivingEntity user, int remainingTime) {
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        RITUAL_START.remove(player.getUUID());
        RITUAL_DONE.remove(player.getUUID());
        Spell spell = selected(stack);
        if (spell != null && spell.kind() == Spell.Kind.CHANNEL) {
            VineGrapple.release(player, false);
            SpellCaster.finishChannel(player, spell, this.focus.cooldownMult());
        }
    }

    /** Shift + roda: próximo ou anterior espaço com magia. */
    public static void scroll(ServerPlayer player, int direction) {
        ItemStack stack = held(player);
        if (stack.isEmpty()) {
            return;
        }
        List<String> slots = slots(stack);
        int n = slots.size();
        int index = selectedIndex(stack);
        int found = -1;
        for (int i = 1; i <= n; i++) {
            int candidate = Math.floorMod(index + (direction > 0 ? i : -i), n);
            if (Spells.byId(slots.get(candidate)) != null) {
                found = candidate;
                break;
            }
        }
        if (found < 0) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spell.no_slots_filled").withStyle(ChatFormatting.GRAY));
            return;
        }
        stack.set(SpellcraftContent.TOME_SELECTED.get(), found);
        Spell spell = Spells.byId(slots.get(found));
        SpellFocusItem item = (SpellFocusItem) stack.getItem();
        int cost = ManaService.cost(player, (int) Math.ceil(spell.mana() * manaMult(stack, spell.element())));
        player.sendOverlayMessage(Component.translatable("message.mistech.spell.selected",
                Component.translatable(spell.nameKey()).withColor(spell.element().color()), cost,
                spell.hasAffinity(player) ? Component.empty() : Component.translatable("message.mistech.spell.locked", spell.requiredLevel()).withStyle(ChatFormatting.RED)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    /** Servidor: põe (ou tira, com id vazio) uma magia num espaço do item da mão indicada. */
    public static void assign(ServerPlayer player, int hand, int slot, String spellId) {
        ItemStack stack = hand == 0 ? player.getMainHandItem() : player.getOffhandItem();
        if (!is(stack)) {
            return;
        }
        List<String> slots = slots(stack);
        if (slot < 0 || slot >= slots.size()) {
            return;
        }
        if (spellId.isEmpty()) {
            slots.set(slot, "");
        } else {
            Spell spell = Spells.byId(spellId);
            if (spell == null || !canHold(player, stack, spell)) {
                player.sendOverlayMessage(Component.translatable("message.mistech.spell.too_strong").withStyle(ChatFormatting.RED));
                return;
            }
            // a mesma magia não fica em dois espaços
            for (int i = 0; i < slots.size(); i++) {
                if (spellId.equals(slots.get(i))) {
                    slots.set(i, "");
                }
            }
            slots.set(slot, spellId);
        }
        stack.set(SpellcraftContent.SPELL_SLOTS.get(), List.copyOf(slots));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.mistech.focus.slots", this.focus.slots(), this.focus.maxTier()).withStyle(ChatFormatting.GRAY));
        for (String id : slots(stack)) {
            Spell spell = Spells.byId(id);
            builder.accept(spell == null ? Component.literal(" • ").append(Component.translatable("tooltip.mistech.focus.empty")).withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal(" • ").append(Component.translatable(spell.nameKey())).withColor(spell.element().color()));
        }
        for (Map.Entry<String, Double> entry : this.focus.manaMods().entrySet()) {
            RuneElement element = RuneElement.byId(entry.getKey());
            if (element != null) {
                int percent = (int) Math.round((entry.getValue() - 1.0) * 100.0);
                builder.accept(Component.translatable("tooltip.mistech.focus.mod", Component.translatable(element.translationKey()), (percent > 0 ? "+" : "") + percent)
                        .withStyle(percent < 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
        if (this.focus.cooldownMult() != 1.0) {
            int percent = (int) Math.round((this.focus.cooldownMult() - 1.0) * 100.0);
            builder.accept(Component.translatable("tooltip.mistech.focus.cooldown", (percent > 0 ? "+" : "") + percent)
                    .withStyle(percent < 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (this.focus.reserve() > 0) {
            builder.accept(Component.translatable("tooltip.mistech.focus.reserve", ManaService.reserveOf(stack), this.focus.reserve()).withStyle(ChatFormatting.AQUA));
        }
        if (this.focus.power() > 0) {
            builder.accept(Component.translatable("tooltip.mistech.focus.power", Math.round(this.focus.power() * 100)).withStyle(ChatFormatting.GREEN));
        }
        if (this.focus.channelMult() < 1.0) {
            builder.accept(Component.translatable("tooltip.mistech.focus.channel", Math.round((1.0 - this.focus.channelMult()) * 100)).withStyle(ChatFormatting.GREEN));
        }
        if (FORBIDDEN.equals(this.focus.element())) {
            builder.accept(Component.translatable("tooltip.mistech.focus.forbidden").withStyle(ChatFormatting.DARK_RED));
        }
        if (maxGems(stack) > 0) {
            List<String> gems = gems(stack);
            net.minecraft.network.chat.MutableComponent line = Component.translatable("tooltip.mistech.focus.gems", gems.size(), maxGems(stack)).withStyle(ChatFormatting.LIGHT_PURPLE);
            for (String id : gems) {
                RuneElement ge = RuneElement.byId(id);
                if (ge != null) {
                    line.append(Component.literal(" ◆").withColor(ge.color()));
                }
            }
            builder.accept(line);
        }
        builder.accept(Component.translatable("tooltip.mistech.focus.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
