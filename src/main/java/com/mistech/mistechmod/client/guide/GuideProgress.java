package com.mistech.mistechmod.client.guide;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mistech.mistechmod.Mistech;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Progresso da jornada do Grimório (só no cliente).
 *
 * <p>Uma entrada fica <b>revelada</b> quando todas as dependências estão concluídas, e é <b>concluída</b> quando a
 * tarefa dela é cumprida: abrir a entrada (tarefa "read") ou ter um dos itens no inventário (tarefa "item").
 * O progresso é salvo por mundo/servidor em {@code .minecraft/mistech/grimoire/<mundo>.json}.
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class GuideProgress {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCAN_INTERVAL = 10;

    public enum State { LOCKED, AVAILABLE, DONE }

    private static String worldKey = null;
    private static final Set<String> done = new HashSet<>();
    private static final Set<String> read = new HashSet<>();
    private static boolean freeMode = false;
    private static boolean dirty = false;
    private static int ticks = 0;

    private GuideProgress() {}

    // ================================================================= consulta

    public static boolean isDone(String id) {
        return done.contains(id);
    }

    public static boolean wasRead(String id) {
        return read.contains(id);
    }

    public static boolean freeMode() {
        return freeMode;
    }

    public static void setFreeMode(boolean free) {
        freeMode = free;
        dirty = true;
        save();
    }

    /** Todas as dependências concluídas (ou modo livre ligado). */
    public static boolean isUnlocked(GuideBook book, GuideBook.Entry entry) {
        if (freeMode) {
            return true;
        }
        return depsDone(book, entry);
    }

    private static boolean depsDone(GuideBook book, GuideBook.Entry entry) {
        for (String dep : entry.deps()) {
            if (book.entry(dep) != null && !done.contains(dep)) {
                return false;
            }
        }
        return true;
    }

    public static State state(GuideBook book, GuideBook.Entry entry) {
        if (done.contains(entry.id())) {
            return State.DONE;
        }
        return isUnlocked(book, entry) ? State.AVAILABLE : State.LOCKED;
    }

    /** Um capítulo é visível se tiver ao menos uma entrada revelada ou concluída. */
    public static boolean chapterOpen(GuideBook book, GuideBook.Category category) {
        for (GuideBook.Entry entry : category.entries()) {
            if (state(book, entry) != State.LOCKED) {
                return true;
            }
        }
        return category.entries().isEmpty();
    }

    public static int doneCount(GuideBook.Category category) {
        int count = 0;
        for (GuideBook.Entry entry : category.entries()) {
            if (done.contains(entry.id())) {
                count++;
            }
        }
        return count;
    }

    public static int doneCount(GuideBook book) {
        int count = 0;
        for (GuideBook.Entry entry : book.allEntries()) {
            if (done.contains(entry.id())) {
                count++;
            }
        }
        return count;
    }

    // ================================================================= mudanças

    /** Chamado quando o jogador abre uma entrada. */
    public static void markRead(GuideBook book, GuideBook.Entry entry) {
        ensureWorld();
        if (read.add(entry.id())) {
            dirty = true;
        }
        if (entry.task().read() && !done.contains(entry.id()) && depsDone(book, entry)) {
            List<GuideBook.Entry> completed = new ArrayList<>();
            completed.add(entry);
            done.add(entry.id());
            dirty = true;
            // ler pode revelar entradas cujo item o jogador já tem
            completed.addAll(scanInventory(book));
            announce(book, completed);
        }
        save();
    }

    // ================================================================= tick

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (worldKey != null) {
                save();
                worldKey = null;
                done.clear();
                read.clear();
                freeMode = false;
            }
            return;
        }
        if (++ticks % SCAN_INTERVAL != 0) {
            return;
        }
        ensureWorld();
        GuideBook book = GuideBook.get();
        List<GuideBook.Entry> completed = scanInventory(book);
        if (!completed.isEmpty()) {
            announce(book, completed);
        }
        if (dirty && ticks % (SCAN_INTERVAL * 6) == 0) {
            save();
        }
    }

    /** Conclui as tarefas de item que estão no inventário. Repete até estabilizar (cadeias se resolvem juntas). */
    private static List<GuideBook.Entry> scanInventory(GuideBook book) {
        List<GuideBook.Entry> completed = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return completed;
        }
        Inventory inventory = minecraft.player.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        stacks.add(minecraft.player.containerMenu.getCarried());
        boolean changed = true;
        for (int pass = 0; changed && pass < 32; pass++) {
            changed = false;
            for (GuideBook.Entry entry : book.allEntries()) {
                if (entry.task().read() || done.contains(entry.id()) || !depsDone(book, entry)) {
                    continue;
                }
                if (hasAny(entry.task().items(), stacks)) {
                    done.add(entry.id());
                    completed.add(entry);
                    changed = true;
                    dirty = true;
                }
            }
        }
        return completed;
    }

    private static boolean hasAny(List<String> items, List<ItemStack> stacks) {
        for (String id : items) {
            for (ItemStack stack : stacks) {
                if (GuideBook.matches(id, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Mostra o aviso de entradas concluídas e quantas foram reveladas por causa delas. */
    private static void announce(GuideBook book, List<GuideBook.Entry> completed) {
        int revealed = 0;
        Set<String> seen = new HashSet<>();
        for (GuideBook.Entry entry : completed) {
            for (String next : book.dependents(entry.id())) {
                GuideBook.Entry dependent = book.entry(next);
                if (dependent != null && seen.add(next) && !done.contains(next) && depsDone(book, dependent)) {
                    revealed++;
                }
            }
        }
        GrimoireToast.show(completed, revealed);
    }

    // ================================================================= arquivo

    private static String currentKey() {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            Path name = root.getFileName();
            return "sp_" + (name != null ? name.toString() : "world");
        }
        ServerData data = minecraft.getCurrentServer();
        if (data != null && data.ip != null) {
            return "mp_" + data.ip;
        }
        return "local";
    }

    private static Path file(String key) {
        String safe = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        return Minecraft.getInstance().gameDirectory.toPath().resolve("mistech").resolve("grimoire").resolve(safe + ".json");
    }

    private static void ensureWorld() {
        String key = currentKey();
        if (key.equals(worldKey)) {
            return;
        }
        if (worldKey != null) {
            save();
        }
        worldKey = key;
        done.clear();
        read.clear();
        freeMode = false;
        dirty = false;
        Path path = file(key);
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("done")) {
                for (JsonElement e : json.getAsJsonArray("done")) {
                    done.add(e.getAsString());
                }
            }
            if (json.has("read")) {
                for (JsonElement e : json.getAsJsonArray("read")) {
                    read.add(e.getAsString());
                }
            }
            freeMode = json.has("free") && json.get("free").getAsBoolean();
        } catch (Exception e) {
            Mistech.LOGGER.warn("Não consegui ler o progresso do Grimório em {}", path, e);
        }
    }

    public static void save() {
        if (worldKey == null || !dirty) {
            return;
        }
        Path path = file(worldKey);
        JsonObject json = new JsonObject();
        JsonArray doneArray = new JsonArray();
        done.stream().sorted().forEach(doneArray::add);
        JsonArray readArray = new JsonArray();
        read.stream().sorted().forEach(readArray::add);
        json.add("done", doneArray);
        json.add("read", readArray);
        json.addProperty("free", freeMode);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            dirty = false;
        } catch (Exception e) {
            Mistech.LOGGER.warn("Não consegui salvar o progresso do Grimório em {}", path, e);
        }
    }

    /** Usado pela tela: garante que o progresso do mundo atual está carregado. */
    public static void prepare() {
        ensureWorld();
        GuideBook book = GuideBook.get();
        List<GuideBook.Entry> completed = scanInventory(book);
        if (!completed.isEmpty()) {
            announce(book, completed);
        }
    }
}
