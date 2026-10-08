package com.mistech.mistechmod.ritual;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.BlackCatEntity;
import com.mistech.mistechmod.entity.OwlEntity;
import com.mistech.mistechmod.registry.RitualContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Eventos dos rituais Tier I: Marca de Giz, ferramentas impuras, Sete Vidas e alerta da coruja. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class RitualEvents {
    public static final int MAX_TREE_LOGS = 64;
    public static final float INFERNAL_DOUBLE_CHANCE = 0.3F;
    private static boolean fellingTree;

    private RitualEvents() {}

    /** Giz de Cinzas no topo de um bloco → Marca de Giz em cima (gasta 1 de durabilidade). */
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        boolean impure = stack.is(Mistech.IMPURE_CHALK.get());
        if (!(impure || stack.is(Mistech.ASH_CHALK.get()) || stack.is(com.mistech.mistechmod.beyond.PaleNetherContent.PALE_CHALK.get()))
                || event.getFace() != Direction.UP) {
            return;
        }
        Level level = event.getLevel();
        if (level.getBlockEntity(event.getPos()) != null) {
            return; // não desenha em cima de mesas/máquinas (deixa abrir a tela)
        }
        BlockPos pos = event.getPos().above();
        BlockState mark = (impure ? RitualContent.IMPURE_CHALK_MARK.get() : RitualContent.CHALK_MARK.get()).defaultBlockState();
        if (!level.getBlockState(pos).canBeReplaced() || !mark.canSurvive(level, pos)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide()) {
            return;
        }
        level.setBlock(pos, mark, 3);
        level.playSound(null, pos, SoundEvents.CALCITE_HIT, SoundSource.BLOCKS, 0.8F, 1.6F);
        Player player = event.getEntity();
        stack.hurtAndBreak(1, player, event.getHand().asEquipmentSlot());
    }

    /** Picareta Impura: autofundição. Machado Impuro: derruba a árvore inteira. */
    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        ServerLevel level = event.getLevel();
        boolean infernalPick = tool.is(com.mistech.mistechmod.beyond.PaleNetherContent.INFERNAL_PICKAXE.get());
        if (tool.is(RitualContent.IMPURE_PICKAXE.get()) || infernalPick) {
            // Picareta Infernal: 30% de chance de dobrar o que vem de minério
            boolean doubled = infernalPick && event.getState().is(net.neoforged.neoforge.common.Tags.Blocks.ORES)
                    && level.getRandom().nextFloat() < INFERNAL_DOUBLE_CHANCE;
            float xp = 0;
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
                if (recipe.isPresent()) {
                    ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack));
                    if (!result.isEmpty()) {
                        xp += recipe.get().value().experience() * stack.getCount();
                        drop.setItem(result.copyWithCount(result.getCount() * stack.getCount() * (doubled ? 2 : 1)));
                    }
                }
            }
            if (xp > 0) {
                ExperienceOrb.award(level, event.getPos().getCenter(), (int) Math.ceil(xp));
                level.playSound(null, event.getPos(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.3F, 1.6F);
            }
            return;
        }
        boolean infernalAxe = tool.is(com.mistech.mistechmod.beyond.PaleNetherContent.INFERNAL_AXE.get());
        if ((tool.is(RitualContent.IMPURE_AXE.get()) || infernalAxe) && !fellingTree && event.getState().is(BlockTags.LOGS)
                && event.getBreaker() instanceof ServerPlayer player && !player.isShiftKeyDown()) {
            fellTree(level, event.getPos(), event.getState(), player, tool, infernalAxe ? MAX_TREE_LOGS * 2 : MAX_TREE_LOGS);
        }
    }

    private static void fellTree(ServerLevel level, BlockPos origin, BlockState originState, ServerPlayer player, ItemStack tool, int maxLogs) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        int broken = 0;
        fellingTree = true;
        try {
            while (!queue.isEmpty() && broken < maxLogs && !tool.isEmpty()) {
                BlockPos pos = queue.poll();
                for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
                    if (seen.add(next.immutable()) && level.getBlockState(next).is(originState.getBlock())) {
                        queue.add(next.immutable());
                    }
                }
                if (!pos.equals(origin)) {
                    level.destroyBlock(pos, true, player);
                    tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    broken++;
                }
            }
        } finally {
            fellingTree = false;
        }
    }

    /** Adaga Ritual: 25% de Resíduo Mágico ao matar. */
    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity killer
                && (killer.getMainHandItem().is(RitualContent.RITUAL_DAGGER.get()) && killer.getRandom().nextFloat() < 0.25F
                    || killer.getMainHandItem().is(com.mistech.mistechmod.beyond.PaleNetherContent.INFERNAL_DAGGER.get())
                        && killer.getRandom().nextFloat() < 0.4F)) {
            LivingEntity dead = event.getEntity();
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), new ItemStack(Mistech.MAGIC_RESIDUE.get())));
        }
    }

    /** Sete Vidas do Gato Preto. */
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && BlackCatEntity.trySave(player.level(), player)) {
            event.setCanceled(true);
        }
    }

    /** Coruja: avisa o dono quando um mob hostil mira nele. */
    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player target) || !(event.getEntity() instanceof Enemy)
                || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        List<OwlEntity> owls = level.getEntitiesOfClass(OwlEntity.class, new AABB(target.blockPosition()).inflate(32),
                owl -> target.getUUID().equals(OwlEntity.ownerId(owl)));
        if (!owls.isEmpty()) {
            owls.get(0).alert(event.getEntity());
        }
    }
}
