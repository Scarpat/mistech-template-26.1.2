package com.mistech.mistechmod.client.guide;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mistech.mistechmod.Mistech;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Conteúdo do Grimório Mistech, lido de assets/mistech/guide/book.json.
 * Os textos são chaves de tradução (guide.mistech.*) nos arquivos de lang.
 *
 * <p>Cada entrada é um nó do mapa da jornada: tem posição (x, y em células), dependências (outras entradas que
 * precisam estar concluídas antes) e uma tarefa (ler a entrada ou ter um item no inventário).
 */
public final class GuideBook {
    public static final Identifier LOCATION = Identifier.fromNamespaceAndPath(Mistech.MODID, "guide/book.json");

    /** Tarefa de uma entrada: "read" (basta abrir) ou "item" (ter qualquer um dos itens no inventário). */
    public record Task(boolean read, List<String> items) {
        public static final Task READ = new Task(true, List.of());
    }

    public record Entry(String id, String icon, String name, List<JsonObject> pages, List<String> deps, Task task,
                        float x, float y, boolean big) {}

    public record Category(String id, String icon, String name, String description, List<Entry> entries) {}

    public final String title;
    public final String subtitle;
    public final String intro;
    /** Capítulos da organização atual (ver {@link #setView}). */
    public final List<Category> categories = new ArrayList<>();
    private final Map<String, Entry> byId = new LinkedHashMap<>();
    private final Map<String, Integer> categoryOf = new HashMap<>();
    /** Formas de organizar o livro: "type" (por assunto, os capítulos de sempre), "progress", "dimension"... */
    private final Map<String, List<Category>> views = new LinkedHashMap<>();
    private String view = TYPE_VIEW;
    public static final String TYPE_VIEW = "type";
    /** Para cada entrada, as entradas que dependem dela. */
    private final Map<String, List<String>> dependents = new HashMap<>();

    private GuideBook(JsonObject json) {
        this.title = json.get("title").getAsString();
        this.subtitle = json.get("subtitle").getAsString();
        this.intro = json.get("intro").getAsString();
        for (JsonElement c : json.getAsJsonArray("categories")) {
            JsonObject cat = c.getAsJsonObject();
            List<Entry> entries = new ArrayList<>();
            int index = 0;
            for (JsonElement e : cat.getAsJsonArray("entries")) {
                JsonObject entry = e.getAsJsonObject();
                List<JsonObject> pages = new ArrayList<>();
                for (JsonElement p : entry.getAsJsonArray("pages")) {
                    pages.add(p.getAsJsonObject());
                }
                List<String> deps = new ArrayList<>();
                if (entry.has("deps")) {
                    for (JsonElement d : entry.getAsJsonArray("deps")) {
                        deps.add(d.getAsString());
                    }
                }
                Task task = Task.READ;
                if (entry.has("task")) {
                    JsonObject t = entry.getAsJsonObject("task");
                    if (t.has("type") && t.get("type").getAsString().equals("item") && t.has("items")) {
                        List<String> items = new ArrayList<>();
                        for (JsonElement i : t.getAsJsonArray("items")) {
                            items.add(i.getAsString());
                        }
                        task = items.isEmpty() ? Task.READ : new Task(false, List.copyOf(items));
                    }
                }
                // sem posição: uma fila simples, para o livro nunca quebrar
                float x = entry.has("x") ? entry.get("x").getAsFloat() : (index % 6) * 1.6F;
                float y = entry.has("y") ? entry.get("y").getAsFloat() : index / 6;
                boolean big = entry.has("size") && entry.get("size").getAsString().equals("big");
                entries.add(new Entry(entry.get("id").getAsString(), entry.get("icon").getAsString(),
                        entry.get("name").getAsString(), pages, List.copyOf(deps), task, x, y, big));
                index++;
            }
            categories.add(new Category(cat.get("id").getAsString(), cat.get("icon").getAsString(),
                    cat.get("name").getAsString(), cat.get("description").getAsString(), entries));
        }
        this.views.put(TYPE_VIEW, List.copyOf(categories));
        this.index();
        // outras organizações: os mesmos nós, em capítulos e posições diferentes
        if (json.has("views")) {
            for (Map.Entry<String, JsonElement> v : json.getAsJsonObject("views").entrySet()) {
                List<Category> list = new ArrayList<>();
                for (JsonElement c : v.getValue().getAsJsonArray()) {
                    JsonObject cat = c.getAsJsonObject();
                    List<Entry> refs = new ArrayList<>();
                    for (JsonElement e : cat.getAsJsonArray("entries")) {
                        JsonObject ref = e.getAsJsonObject();
                        Entry base = byId.get(ref.get("id").getAsString());
                        if (base == null) {
                            Mistech.LOGGER.warn("Grimório: a organização {} cita a entrada {}, que não existe", v.getKey(), ref.get("id"));
                            continue;
                        }
                        refs.add(new Entry(base.id(), base.icon(), base.name(), base.pages(), base.deps(), base.task(),
                                ref.has("x") ? ref.get("x").getAsFloat() : base.x(), ref.has("y") ? ref.get("y").getAsFloat() : base.y(),
                                ref.has("size") && ref.get("size").getAsString().equals("big")));
                    }
                    list.add(new Category(cat.get("id").getAsString(), cat.get("icon").getAsString(), cat.get("name").getAsString(),
                            cat.get("description").getAsString(), refs));
                }
                if (!list.isEmpty()) {
                    this.views.put(v.getKey(), List.copyOf(list));
                }
            }
        }
        for (Entry entry : byId.values()) {
            for (String dep : entry.deps()) {
                if (!byId.containsKey(dep)) {
                    Mistech.LOGGER.warn("Grimório: a entrada {} depende de {}, que não existe", entry.id(), dep);
                }
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(entry.id());
            }
        }
    }

    private void index() {
        byId.clear();
        categoryOf.clear();
        for (int i = 0; i < categories.size(); i++) {
            for (Entry entry : categories.get(i).entries()) {
                byId.put(entry.id(), entry);
                categoryOf.put(entry.id(), i);
            }
        }
    }

    /** Ids das organizações disponíveis, na ordem do livro ("type" primeiro). */
    public List<String> viewIds() {
        return List.copyOf(views.keySet());
    }

    public String view() {
        return this.view;
    }

    /** Troca a organização: {@link #categories} passa a ter os capítulos dela. */
    public void setView(String id) {
        List<Category> list = views.get(id);
        if (list == null) {
            id = TYPE_VIEW;
            list = views.get(TYPE_VIEW);
        }
        this.view = id;
        categories.clear();
        categories.addAll(list);
        this.index();
    }

    /** A entrada com esse id, ou null. */
    public Entry entry(String id) {
        return byId.get(id);
    }

    public Iterable<Entry> allEntries() {
        return byId.values();
    }

    public int size() {
        return byId.size();
    }

    /** Índice do capítulo da entrada, ou -1. */
    public int categoryIndex(String entryId) {
        return categoryOf.getOrDefault(entryId, -1);
    }

    public List<String> dependents(String entryId) {
        return dependents.getOrDefault(entryId, Collections.emptyList());
    }

    // ------------------------------------------------------------------ carregamento

    private static GuideBook cached;

    /** O livro carregado (lê na primeira vez). */
    public static GuideBook get() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    /** Lê o livro de novo dos recursos (pega mudanças de resource pack). */
    public static GuideBook reload() {
        cached = load();
        return cached;
    }

    private static GuideBook load() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(LOCATION);
            if (resource.isPresent()) {
                try (Reader reader = resource.get().openAsReader()) {
                    return new GuideBook(JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
            Mistech.LOGGER.error("Grimório Mistech não encontrado em {}", LOCATION);
        } catch (Exception e) {
            Mistech.LOGGER.error("Erro ao ler o Grimório Mistech", e);
        }
        JsonObject empty = new JsonObject();
        empty.addProperty("title", "guide.mistech.title");
        empty.addProperty("subtitle", "guide.mistech.subtitle");
        empty.addProperty("intro", "guide.mistech.intro");
        empty.add("categories", new JsonArray());
        return new GuideBook(empty);
    }

    // ------------------------------------------------------------------ itens

    private static final Map<String, List<ItemStack>> CACHE = new HashMap<>();

    /**
     * "" = vazio; "mod:item" = item; "#mod:tag" = alterna entre os itens da tag;
     * "@water_bottle" = garrafa de água.
     */
    public static ItemStack stack(String id) {
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> options = CACHE.computeIfAbsent(id, GuideBook::resolve);
        if (options.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = (int) ((Util.getMillis() / 1000L) % options.size());
        return options.get(index);
    }

    public static ItemStack stack(String id, int count) {
        ItemStack stack = stack(id);
        return stack.isEmpty() ? stack : stack.copyWithCount(count);
    }

    /** O item de uma tarefa bate com este stack? Aceita "mod:item" e "#mod:tag". */
    public static boolean matches(String id, ItemStack stack) {
        if (stack.isEmpty() || id.isEmpty()) {
            return false;
        }
        try {
            if (id.startsWith("#")) {
                return stack.is(TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1))));
            }
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Identifier.parse(id));
        } catch (Exception e) {
            return false;
        }
    }

    private static List<ItemStack> resolve(String id) {
        List<ItemStack> list = new ArrayList<>();
        try {
            if (id.equals("@water_bottle")) {
                list.add(PotionContents.createItemStack(Items.POTION, Potions.WATER));
            } else if (id.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)));
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    list.add(new ItemStack(holder));
                }
            } else {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
                if (item != null && item != Items.AIR) {
                    list.add(new ItemStack(item));
                }
            }
        } catch (Exception e) {
            Mistech.LOGGER.warn("Item inválido no Grimório: {}", id);
        }
        return list;
    }

    /** Limpa o cache (as tags só existem depois de entrar num mundo). */
    public static void clearCache() {
        CACHE.clear();
    }
}
