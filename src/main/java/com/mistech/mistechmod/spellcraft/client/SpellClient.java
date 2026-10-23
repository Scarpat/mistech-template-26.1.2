package com.mistech.mistechmod.spellcraft.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.ManaService;
import com.mistech.mistechmod.spellcraft.OpenFocusPayload;
import com.mistech.mistechmod.spellcraft.OpenInscriptionPayload;
import com.mistech.mistechmod.spellcraft.Spell;
import com.mistech.mistechmod.spellcraft.Spells;
import com.mistech.mistechmod.spellcraft.SpellFocusItem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Cliente do sistema de magias: tela da Mesa de Inscrição e a barra de mana. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class SpellClient {
    private static final Identifier MANA_HUD = Identifier.fromNamespaceAndPath(Mistech.MODID, "mana_bar");
    private static final Identifier SLOTS_HUD = Identifier.fromNamespaceAndPath(Mistech.MODID, "spell_slots");
    private static final Identifier CAST_HUD = Identifier.fromNamespaceAndPath(Mistech.MODID, "cast_progress");
    private static final int BAR_W = 81;
    private static final int BAR_H = 9;
    /** Mods com barra de mana própria: se algum estiver instalado, a nossa barra não aparece (evita duas barras sobrepostas). */
    private static final java.util.List<String> OTHER_MANA_MODS = java.util.List.of("irons_spellbooks", "ars_nouveau", "ars_elemental",
            "electroblobs_wizardry", "botania", "mna", "manaweave", "eidolon", "hexcasting", "occultism", "thaumcraft");
    private static Boolean otherManaBar;

    /** Mana na última vez que mudou, para mostrar a barra por uns segundos depois de lançar. */
    private static float lastMana = -1.0F;
    private static long lastChange;

    private SpellClient() {}

    @SubscribeEvent
    static void onPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenInscriptionPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new InscriptionScreen(payload.tablePos())));
        event.register(OpenFocusPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new FocusScreen(payload.hand())));
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, MANA_HUD, SpellClient::renderMana);
        event.registerAbove(VanillaGuiLayers.HOTBAR, SLOTS_HUD, SpellClient::renderSlots);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, CAST_HUD, SpellClient::renderCast);
    }

    private static boolean otherManaBar() {
        if (otherManaBar == null) {
            otherManaBar = OTHER_MANA_MODS.stream().anyMatch(net.neoforged.fml.ModList.get()::isLoaded);
        }
        return otherManaBar;
    }

    /** Barra de mana: logo acima da fome (à direita da hotbar); sobe 10 px debaixo d'água para não cobrir as bolhas. */
    private static void renderMana(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || otherManaBar()) {
            return;
        }
        float mana = ManaService.get(player);
        float max = ManaService.max(player);
        long now = System.currentTimeMillis();
        if (Math.abs(mana - lastMana) > 0.01F) {
            lastMana = mana;
            lastChange = now;
        }
        boolean holding = SpellFocusItem.is(player.getMainHandItem()) || SpellFocusItem.is(player.getOffhandItem());
        // só aparece segurando um Tomo, ou por 4 s depois de a mana mudar (regenerando ou gastando)
        if (!holding && (now - lastChange > 4000L || mana >= max - 0.01F)) {
            return;
        }
        int x = g.guiWidth() / 2 + 10;
        int y = g.guiHeight() - 39 - BAR_H - 2 - (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply() ? 10 : 0);
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xFF0A1424);
        g.fill(x, y, x + BAR_W, y + BAR_H, 0xFF1A2E4A);
        int filled = Math.round(BAR_W * Math.max(0.0F, Math.min(1.0F, mana / max)));
        g.fillGradient(x, y, x + filled, y + BAR_H, 0xFF6FC4FF, 0xFF2F6FE0);
        g.fill(x, y, x + filled, y + 1, 0x60FFFFFF);
        Component text = Component.literal((int) mana + "/" + (int) max);
        g.text(mc.font, text, x + BAR_W / 2 - mc.font.width(text) / 2, y + 1, 0xFFFFFFFF, true);
        // reserva do Cajado: faixa dourada logo acima da barra
        ItemStack staff = ManaService.reserveStack(player);
        if (!staff.isEmpty()) {
            int reserve = ManaService.reserveOf(staff);
            int cap = ManaService.reserveMax(staff);
            int ry = y - 6;
            g.fill(x - 1, ry - 1, x + BAR_W + 1, ry + 4, 0xFF0A1424);
            g.fill(x, ry, x + BAR_W, ry + 3, 0xFF2A1E40);
            int rf = Math.round(BAR_W * Math.max(0.0F, Math.min(1.0F, reserve / (float) cap)));
            g.fillGradient(x, ry, x + rf, ry + 3, 0xFFFFE08A, 0xFFC088FF);
            Component rt = Component.literal(reserve + "/" + cap);
            g.text(mc.font, rt, x + BAR_W - mc.font.width(rt), ry - 10, 0xFFE8D0FF, true);
        }
    }

    /** Barra de carga (magias carregadas) ou de conjuração (rituais) logo abaixo da mira. */
    private static void renderCast(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || !player.isUsingItem() || !SpellFocusItem.is(player.getUseItem())) {
            return;
        }
        Spell spell = SpellFocusItem.selected(player.getUseItem());
        if (spell == null || (spell.kind() != Spell.Kind.CHARGE && spell.kind() != Spell.Kind.RITUAL)) {
            return;
        }
        float progress = Math.min(1.0F, player.getTicksUsingItem() / (float) spell.castTicks());
        int w = 62;
        int x = g.guiWidth() / 2 - w / 2;
        int y = g.guiHeight() / 2 + 22;
        boolean full = progress >= 1.0F;
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 90.0);
        int border = full ? (0xFF000000 | (0xFF << 16) | ((int) (0xC0 + 0x3F * pulse) << 8) | 0x60) : 0xFF000000;
        g.fill(x - 2, y - 2, x + w + 2, y + 8, border);
        g.fill(x - 1, y - 1, x + w + 1, y + 7, 0xFF1A1A1A);
        int color = 0xFF000000 | spell.element().color();
        int filled = Math.round(w * progress);
        g.fillGradient(x, y, x + filled, y + 6, color, 0xFF000000 | ((spell.element().color() & 0xFEFEFE) >> 1));
        g.fill(x, y, x + filled, y + 1, 0x70FFFFFF);
        Component label = spell.kind() == Spell.Kind.CHARGE
                ? Component.literal(String.format(java.util.Locale.ROOT, "×%.1f", 1.0F + progress))
                : Component.literal((int) (progress * 100) + "%");
        g.text(mc.font, label, g.guiWidth() / 2 - mc.font.width(label) / 2, y + 10, 0xFFFFFFFF, true);
    }

    /** HUD do Tomo/Varinha: coluna de casas no estilo do inventário à direita da hotbar; a escolhida ganha moldura branca, brilho do elemento e o nome numa dica roxa. */
    private static void renderSlots(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        ItemStack stack = SpellFocusItem.held(player);
        if (stack.isEmpty()) {
            return;
        }
        java.util.List<String> slots = SpellFocusItem.slots(stack);
        int selected = SpellFocusItem.selectedIndex(stack);
        int size = 18;
        int x = g.guiWidth() / 2 + 91 + 34;
        int top = g.guiHeight() - 4 - slots.size() * (size + 1);
        float mana = ManaService.get(player);
        // marca de ressonância ativa: ícone do elemento com uma barrinha do tempo restante, acima da coluna
        long now = player.level().getGameTime();
        com.mistech.mistechmod.item.RuneElement mark = com.mistech.mistechmod.spellcraft.Resonance.mark(player, now);
        if (mark != null) {
            int my = top - size - 5;
            MysticUi.inset(g, x, my, size, size, 0xFF2A1E40);
            MysticUi.glow(g, x, my, size, mark.color());
            ElementIcon.draw(g, mark, x + 4, my + 4, 1);
            float left = Math.max(0.0F, (com.mistech.mistechmod.spellcraft.Resonance.markEnd(player) - now) / (float) (player.getData(com.mistech.mistechmod.spellcraft.SpellcraftContent.CIRCLE_BLESSED.get()) ? com.mistech.mistechmod.spellcraft.Resonance.DURATION_BLESSED : com.mistech.mistechmod.spellcraft.Resonance.DURATION));
            g.fill(x, my + size + 1, x + Math.round(size * left), my + size + 3, 0xFF000000 | mark.color());
        }
        for (int i = 0; i < slots.size(); i++) {
            int y = top + i * (size + 1);
            boolean sel = i == selected;
            Spell spell = Spells.byId(slots.get(i));
            if (spell == null) {
                MysticUi.inset(g, x, y, size, size, 0xFF6E6E6E);
                g.fill(x + 8, y + 8, x + 10, y + 10, 0xFF5A5A5A);
                if (sel) {
                    MysticUi.selection(g, x, y, size);
                }
                continue;
            }
            double mult = SpellFocusItem.manaMult(stack, spell.element());
            int cost = ManaService.cost(player, (int) Math.ceil(spell.mana() * mult * com.mistech.mistechmod.spellcraft.SpellCaster.mastery(player, spell.element())));
            boolean locked = !spell.hasAffinity(player) && !SpellFocusItem.forbiddenHeld(player);
            boolean noMana = !player.hasInfiniteMaterials() && mana < cost;
            MysticUi.spellTile(g, spell, x, y, size, locked, noMana, i);
            if (sel) {
                MysticUi.glow(g, x, y, size, spell.element().color());
                MysticUi.selection(g, x, y, size);
                Component name = Component.translatable(spell.nameKey());
                int w = mc.font.width(name);
                MysticUi.tooltipBox(g, x + size + 6, y + 3, w + 8, 14);
                g.text(mc.font, name, x + size + 10, y + 6, 0xFF000000 | spell.element().color(), true);
            }
        }
    }
}
