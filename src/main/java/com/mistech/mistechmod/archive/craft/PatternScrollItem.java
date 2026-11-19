package com.mistech.mistechmod.archive.craft;

import java.util.function.Consumer;

import com.mistech.mistechmod.golem.GolemCrafting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Pergaminho de Padrão (Fase 8). Com ele na mão principal:
 * <ul>
 * <li>Item na outra mão + clique no ar = padrão de <b>mesa</b> para aquele item (a receita de mesa de trabalho que o faz).</li>
 * <li>Agachado + clique numa máquina (qualquer bloco com inventário), com a entrada na outra mão = padrão de
 * <b>processamento</b>: a entrada (com a quantidade) vai para aquela máquina.</li>
 * <li>Depois, agachado + clique no ar com a saída na outra mão = o que a máquina devolve.</li>
 * <li>Agachado + clique no ar com a outra mão vazia = apaga o padrão.</li>
 * </ul>
 * Os padrões prontos vão numa Estante de Padrões ligada ao Arquivo.
 */
public class PatternScrollItem extends Item {
    public PatternScrollItem(Item.Properties properties) {
        super(properties);
    }

    public static Pattern get(ItemStack stack) {
        return stack.get(CraftContent.PATTERN.get());
    }

    public static boolean isComplete(ItemStack stack) {
        Pattern p = get(stack);
        return p != null && p.complete();
    }

    private static void set(Player player, InteractionHand hand, ItemStack stack, Pattern pattern) {
        if (stack.getCount() > 1) {
            // separa um pergaminho da pilha
            ItemStack one = stack.copyWithCount(1);
            one.set(CraftContent.PATTERN.get(), pattern);
            stack.shrink(1);
            if (!player.getInventory().add(one)) {
                player.drop(one, false);
            }
        } else if (pattern == null) {
            stack.remove(CraftContent.PATTERN.get());
        } else {
            stack.set(CraftContent.PATTERN.get(), pattern);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getOffhandItem();
        Pattern pattern = get(stack);
        if (player.isShiftKeyDown()) {
            if (other.isEmpty()) {
                if (pattern != null) {
                    set(player, hand, stack, null);
                    player.sendOverlayMessage(Component.translatable("message.mistech.pattern.cleared"));
                }
                return InteractionResult.SUCCESS;
            }
            if (pattern != null && !pattern.crafting()) {
                set(player, hand, stack, pattern.withOutput(other));
                player.sendOverlayMessage(Component.translatable("message.mistech.pattern.processing_done", pattern.input().getCount(),
                        pattern.input().getHoverName(), other.getCount(), other.getHoverName()).withStyle(ChatFormatting.GOLD));
                server.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.2F);
                return InteractionResult.SUCCESS;
            }
            player.sendOverlayMessage(Component.translatable("message.mistech.pattern.need_machine").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        if (other.isEmpty()) {
            if (pattern != null && pattern.complete()) {
                // padrão pronto e mão secundária vazia: troca a prioridade
                Pattern next = pattern.nextPriority();
                set(player, hand, stack, next);
                player.sendOverlayMessage(Component.translatable("message.mistech.pattern.priority",
                        Component.translatable("item.mistech.pattern_scroll.priority." + priorityKey(next.priority()))).withStyle(ChatFormatting.GOLD));
                server.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.5F);
                return InteractionResult.SUCCESS;
            }
            player.sendOverlayMessage(Component.translatable("message.mistech.pattern.help").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        java.util.List<ResourceKey<Recipe<?>>> all = GolemCrafting.findAll(server, other);
        if (all.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.golem.no_recipe", other.getHoverName()).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        // mesmo item de novo: passa para a próxima receita dele (várias maneiras de fazer)
        ResourceKey<Recipe<?>> recipe = all.get(0);
        if (pattern != null && pattern.crafting() && pattern.recipe().isPresent() && pattern.output().is(other.getItem())) {
            int at = all.indexOf(pattern.recipe().get());
            recipe = all.get((at + 1) % all.size());
        }
        int index = all.indexOf(recipe) + 1;
        ItemStack output = GolemCrafting.result(server, recipe);
        Pattern made = Pattern.crafting(recipe, output.isEmpty() ? other.copyWithCount(1) : output);
        if (pattern != null) {
            made = new Pattern(made.crafting(), made.recipe(), made.input(), made.output(), made.machine(), pattern.priority());
        }
        set(player, hand, stack, made);
        player.sendOverlayMessage(Component.translatable("message.mistech.pattern.crafting_n", other.getHoverName(), index, all.size(),
                ingredients(server, recipe)).withStyle(ChatFormatting.GOLD));
        server.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    public static String priorityKey(int priority) {
        return priority >= 1 ? "high" : priority == 0 ? "normal" : "low";
    }

    /** "2× Tábua, 1× Graveto": resumo dos ingredientes da receita (o primeiro item de cada um). */
    public static Component ingredients(ServerLevel level, ResourceKey<Recipe<?>> key) {
        net.minecraft.world.item.crafting.CraftingRecipe recipe = GolemCrafting.get(level, key);
        java.util.List<java.util.Optional<net.minecraft.world.item.crafting.Ingredient>> slots = recipe == null ? null : GolemCrafting.slots(recipe);
        if (slots == null) {
            return Component.empty();
        }
        java.util.Map<net.minecraft.world.item.Item, Integer> count = new java.util.LinkedHashMap<>();
        for (var slot : slots) {
            slot.flatMap(i -> i.items().findFirst()).ifPresent(h -> count.merge(h.value(), 1, Integer::sum));
        }
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        boolean first = true;
        for (var e : count.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(e.getValue() + "× ").append(new ItemStack(e.getKey()).getHoverName());
        }
        return out;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        ItemStack other = player.getOffhandItem();
        if (server.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP) == null
                || server.getBlockState(pos).getBlock() instanceof PatternShelfBlock) {
            player.sendOverlayMessage(Component.translatable("message.mistech.pattern.not_machine").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (other.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.pattern.need_input").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        set(player, context.getHand(), context.getItemInHand(), Pattern.processing(pos, other));
        player.sendOverlayMessage(Component.translatable("message.mistech.pattern.processing_input", other.getCount(), other.getHoverName(),
                server.getBlockState(pos).getBlock().getName()).withStyle(ChatFormatting.GOLD));
        server.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Pattern p = get(stack);
        if (p == null) {
            builder.accept(Component.translatable("item.mistech.pattern_scroll.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (p.crafting()) {
            builder.accept(Component.translatable("item.mistech.pattern_scroll.crafting", p.output().getCount(), p.output().getHoverName())
                    .withStyle(ChatFormatting.GOLD));
        } else {
            builder.accept(Component.translatable("item.mistech.pattern_scroll.processing").withStyle(ChatFormatting.AQUA));
            builder.accept(Component.translatable("item.mistech.pattern_scroll.in", p.input().getCount(), p.input().getHoverName())
                    .withStyle(ChatFormatting.GRAY));
            if (!p.output().isEmpty()) {
                builder.accept(Component.translatable("item.mistech.pattern_scroll.out", p.output().getCount(), p.output().getHoverName())
                        .withStyle(ChatFormatting.GRAY));
            } else {
                builder.accept(Component.translatable("item.mistech.pattern_scroll.no_output").withStyle(ChatFormatting.RED));
            }
            p.machine().ifPresent(m -> builder.accept(Component.translatable("item.mistech.pattern_scroll.machine", m.getX(), m.getY(), m.getZ())
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
        builder.accept(Component.translatable("item.mistech.pattern_scroll.priority",
                Component.translatable("item.mistech.pattern_scroll.priority." + priorityKey(p.priority()))).withStyle(ChatFormatting.DARK_AQUA));
        if (p.crafting() && p.recipe().isPresent()) {
            builder.accept(Component.literal(p.recipe().get().identifier().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isComplete(stack);
    }
}
