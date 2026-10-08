package com.mistech.mistechmod.item.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Cajado do Gnomo: bateria de energia (FE) + runas.
 * <ul>
 * <li>Runa na outra mão + botão direito: o cajado consome a runa e ela entra na lista (cada elemento uma vez).</li>
 * <li>Shift + roda do mouse: troca a runa escolhida (a cor do orbe muda). Posição 0 = modo padrão (não faz nada).</li>
 * <li>Segurar botão direito: conjura a magia da runa escolhida (ver {@link StaffSpells}); todas gastam energia.</li>
 * </ul>
 */
public class GnomeStaffItem extends Item {
    public static final int CAPACITY = 50_000;
    public static final int MAX_CHARGE_PER_TICK = 1_000;
    /** Cor do orbe no modo padrão (cinza claro). */
    public static final int DEFAULT_ORB_COLOR = 0xC8C4D0;

    public GnomeStaffItem(Item.Properties properties) {
        super(properties);
    }

    // ----- dados no item -----

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.STAFF_ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.set(MistechComponents.STAFF_ENERGY.get(), Mth.clamp(energy, 0, CAPACITY));
    }

    /** Gasta energia se tiver o bastante (criativo não gasta). */
    public static boolean tryUse(Player player, ItemStack stack, int amount) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        amount = cost(player, amount);
        int energy = getEnergy(stack);
        if (energy < amount) {
            return false;
        }
        setEnergy(stack, energy - amount);
        return true;
    }

    public static boolean hasEnergy(Player player, ItemStack stack, int amount) {
        return player.hasInfiniteMaterials() || getEnergy(stack) >= cost(player, amount);
    }

    /** Custo real: Amuleto de Essência (-25%) e Surto Arcano (-50%). */
    public static int cost(Player player, int amount) {
        amount = com.mistech.mistechmod.accessory.Accessories.staffCost(player, amount);
        if (player.hasEffect(com.mistech.mistechmod.registry.MistechEffects.ARCANE_SURGE)) {
            amount = (int) Math.ceil(amount * 0.5);
        }
        return amount;
    }

    public static List<RuneElement> getRunes(ItemStack stack) {
        List<RuneElement> out = new ArrayList<>();
        for (String id : stack.getOrDefault(MistechComponents.STAFF_RUNES.get(), List.<String>of())) {
            RuneElement element = RuneElement.byId(id);
            if (element != null) {
                out.add(element);
            }
        }
        return out;
    }

    /** Runa escolhida, ou null no modo padrão. */
    public static @Nullable RuneElement getSelected(ItemStack stack) {
        List<RuneElement> runes = getRunes(stack);
        int selected = stack.getOrDefault(MistechComponents.STAFF_SELECTED.get(), 0);
        return selected >= 1 && selected <= runes.size() ? runes.get(selected - 1) : null;
    }

    /** Pinta o orbe com a cor da runa escolhida (o modelo lê a cor do custom_model_data). */
    public static void refreshOrb(ItemStack stack) {
        RuneElement selected = getSelected(stack);
        int color = selected == null ? DEFAULT_ORB_COLOR : selected.color();
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(color)));
    }

    /** Itens que guardam runas e trocam com Shift + roda: o cajado e o Cachimbo Rúnico. */
    public static boolean holdsRunes(ItemStack stack) {
        return stack.is(GnomeContent.GNOME_STAFF.get()) || stack.is(com.mistech.mistechmod.Mistech.RUNIC_PIPE.get());
    }

    /**
     * Runa na outra mão: guarda no item (cada elemento uma vez) e já escolhe ela.
     * Usado pelo cajado e pelo Cachimbo Rúnico. Consome a runa.
     */
    public static InteractionResult learnRune(Level level, Player player, ItemStack stack, ItemStack other, RuneElement rune, String addedKey) {
        List<RuneElement> runes = getRunes(stack);
        if (runes.contains(rune)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.mistech.gnome_staff.already",
                        Component.translatable(rune.translationKey())).withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            List<String> ids = new ArrayList<>(stack.getOrDefault(MistechComponents.STAFF_RUNES.get(), List.<String>of()));
            ids.add(rune.id());
            stack.set(MistechComponents.STAFF_RUNES.get(), ids);
            stack.set(MistechComponents.STAFF_SELECTED.get(), ids.size()); // já escolhe a nova
            refreshOrb(stack);
            other.consume(1, player);
            player.sendOverlayMessage(Component.translatable(addedKey,
                    Component.translatable(rune.translationKey()).withColor(rune.color())));
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Shift + roda: próxima/anterior runa (o 0 é o modo padrão). */
    public static void scroll(ServerPlayer player, int direction) {
        ItemStack stack = holdsRunes(player.getMainHandItem()) ? player.getMainHandItem()
                : holdsRunes(player.getOffhandItem()) ? player.getOffhandItem() : ItemStack.EMPTY;
        if (stack.isEmpty() || player.isUsingItem()) {
            return;
        }
        int count = getRunes(stack).size() + 1;
        int selected = stack.getOrDefault(MistechComponents.STAFF_SELECTED.get(), 0);
        selected = Math.floorMod(selected + (direction > 0 ? 1 : -1), count);
        stack.set(MistechComponents.STAFF_SELECTED.get(), selected);
        refreshOrb(stack);
        player.sendOverlayMessage(selectedName(stack));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    public static Component selectedName(ItemStack stack) {
        RuneElement selected = getSelected(stack);
        Component name = selected == null ? Component.translatable("item.mistech.gnome_staff.mode_default")
                : Component.translatable(selected.translationKey()).withColor(selected.color());
        return Component.translatable("item.mistech.gnome_staff.selected", name);
    }

    // ----- uso -----

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

        // runa na outra mão: guardar no cajado
        RuneElement rune = RuneElement.ofRune(other);
        if (rune != null) {
            List<RuneElement> runes = getRunes(stack);
            if (runes.contains(rune)) {
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(Component.translatable("item.mistech.gnome_staff.already",
                            Component.translatable(rune.translationKey())).withStyle(ChatFormatting.GRAY));
                }
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                List<String> ids = new ArrayList<>(stack.getOrDefault(MistechComponents.STAFF_RUNES.get(), List.<String>of()));
                ids.add(rune.id());
                stack.set(MistechComponents.STAFF_RUNES.get(), ids);
                stack.set(MistechComponents.STAFF_SELECTED.get(), ids.size()); // já escolhe a nova
                refreshOrb(stack);
                other.consume(1, player);
                player.sendOverlayMessage(Component.translatable("item.mistech.gnome_staff.added",
                        Component.translatable(rune.translationKey()).withColor(rune.color())));
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }

        RuneElement selected = getSelected(stack);
        if (selected == null) {
            return InteractionResult.PASS; // modo padrão: nada acontece
        }
        if (!hasEnergy(player, stack, StaffSpells.minEnergy(selected))) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.mistech.gnome_staff.no_energy").withStyle(ChatFormatting.RED));
                level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (entity instanceof Player player) {
            RuneElement selected = getSelected(stack);
            if (selected != null) {
                int held = this.getUseDuration(stack, entity) - ticksRemaining;
                StaffSpells.channel(level, player, stack, selected, held);
                if (StaffSpells.isChanneled(selected) && held > 0 && held % 40 == 0) {
                    com.mistech.mistechmod.affinity.Affinity.grant(player, selected, 1); // afinidade: 1 ponto a cada 2 s canalizando
                }
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        RuneElement selected = getSelected(stack);
        int held = this.getUseDuration(stack, entity) - remainingTime;
        if (selected == null || StaffSpells.isChanneled(selected) || held < StaffSpells.CHARGE_TICKS) {
            return false;
        }
        boolean cast = StaffSpells.release(level, player, stack, selected);
        if (cast) {
            com.mistech.mistechmod.affinity.Affinity.grant(player, selected, 2); // afinidade: 2 pontos por magia
        }
        return cast;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    // ----- barra de energia e tooltip -----

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        RuneElement selected = getSelected(stack);
        return selected == null ? DEFAULT_ORB_COLOR : selected.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.gnome_staff.energy", getEnergy(stack), CAPACITY).withStyle(ChatFormatting.GRAY));
        builder.accept(selectedName(stack).copy().withStyle(ChatFormatting.GRAY));
        List<RuneElement> runes = getRunes(stack);
        if (runes.isEmpty()) {
            builder.accept(Component.translatable("item.mistech.gnome_staff.no_runes").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (RuneElement rune : runes) {
                builder.accept(Component.literal(" • ").append(Component.translatable(rune.translationKey())).withColor(rune.color()));
            }
        }
        builder.accept(Component.translatable("item.mistech.gnome_staff.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
