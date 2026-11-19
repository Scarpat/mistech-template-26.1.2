package com.mistech.mistechmod.machine;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Descrição de uma máquina do Mistech: espaços da tela (posição e tipo), capacidade de energia e o que a tela desenha.
 * Vale no servidor e no cliente (o cliente usa os filtros para aceitar ou não um item no espaço).
 *
 * Textura da tela em {@code textures/gui/container/<name>.png} (256×256): fundo 176×166 em (0,0),
 * seta cheia 24×17 em (176,0), barra de energia cheia 14×52 em (200,0) e chama cheia 14×14 em (176,17).
 */
public final class MachineLayout {
    /**
     * FILTER = espaço "fantasma": guarda só uma amostra (não gasta o item do jogador) e não sai da máquina.
     * SOCKET = engaste (qualquer runa; ver {@link com.mistech.mistechmod.machine.socket.MachineSockets}), sempre no fim da lista.
     */
    public enum Kind { INPUT, RUNE, FUEL, OUTPUT, FILTER, SOCKET }

    public record SlotSpec(int x, int y, Kind kind) {}

    public static final int BAR_X = 152, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    public static final int ARROW_W = 24, ARROW_H = 17;
    public static final int FLAME_SIZE = 14;

    public final String name;
    public final List<SlotSpec> slots;
    public final int capacity;
    public final Predicate<ItemStack> inputFilter;
    public final Predicate<ItemStack> runeFilter;
    public final Predicate<ItemStack> fuelFilter;
    /** Posição da seta de progresso (-1 = sem seta). */
    public final int arrowX, arrowY;
    /** Posição da chama (queima do combustível; -1 = sem chama). */
    public final int flameX, flameY;
    /** Linhas de texto na área da máquina (pode ser vazia) e onde começam. */
    public final Function<MachineMenu, List<Component>> infoLines;
    public final int infoX, infoY;
    /** Caixa do texto (0 = sem limite): quebra as linhas na largura e diminui a letra se passar da altura. */
    public final int infoW, infoH;
    /** Dicas dos espaços: ícone apagado e texto quando o espaço está vazio (índice → dica). */
    public final java.util.Map<Integer, Hint> hints;

    /** Dica de um espaço vazio: o ícone (item) desenhado apagado e a chave do texto do tooltip. */
    public record Hint(Supplier<ItemStack> icon, String key) {}
    private final Supplier<MenuType<MachineMenu>> menuType;
    /** Categoria e quantidade de engastes (tabela em MachineSockets). */
    public final com.mistech.mistechmod.machine.socket.MachineSockets.Spec sockets;
    /** Índice do primeiro engaste (= tamanho sem os engastes). */
    public final int firstSocket;

    private MachineLayout(Builder b) {
        this.name = b.name;
        this.sockets = com.mistech.mistechmod.machine.socket.MachineSockets.spec(b.name);
        this.firstSocket = b.slots.size();
        java.util.List<SlotSpec> all = new java.util.ArrayList<>(b.slots);
        for (int i = 0; i < this.sockets.sockets(); i++) {
            all.add(new SlotSpec(com.mistech.mistechmod.machine.socket.MachineSockets.SLOT_X,
                    com.mistech.mistechmod.machine.socket.MachineSockets.SLOT_Y + i * 18, Kind.SOCKET));
        }
        this.slots = List.copyOf(all);
        this.capacity = b.capacity;
        this.inputFilter = b.inputFilter;
        this.runeFilter = b.runeFilter;
        this.fuelFilter = b.fuelFilter;
        this.arrowX = b.arrowX;
        this.arrowY = b.arrowY;
        this.flameX = b.flameX;
        this.flameY = b.flameY;
        this.infoLines = b.infoLines;
        this.infoX = b.infoX;
        this.infoY = b.infoY;
        this.infoW = b.infoW;
        this.infoH = b.infoH;
        this.hints = java.util.Map.copyOf(b.hints);
        this.menuType = b.menuType;
    }

    public int size() {
        return this.slots.size();
    }

    public int socketCount() {
        return this.slots.size() - this.firstSocket;
    }

    public Kind kind(int slot) {
        return this.slots.get(slot).kind();
    }

    public MenuType<MachineMenu> menuType() {
        return this.menuType.get();
    }

    /** O item pode ir para o espaço (vale para jogador, funil e canos). */
    public boolean accepts(int slot, ItemStack stack) {
        return switch (this.kind(slot)) {
            case INPUT -> this.inputFilter.test(stack);
            case RUNE -> this.runeFilter.test(stack);
            case FUEL -> this.fuelFilter.test(stack);
            case SOCKET -> com.mistech.mistechmod.item.RuneElement.ofRune(stack) != null;
            case OUTPUT, FILTER -> false;
        };
    }

    public static Builder builder(String name, Supplier<MenuType<MachineMenu>> menuType) {
        return new Builder(name, menuType);
    }

    public static final class Builder {
        private final String name;
        private final Supplier<MenuType<MachineMenu>> menuType;
        private final java.util.ArrayList<SlotSpec> slots = new java.util.ArrayList<>();
        private int capacity = 10_000;
        private Predicate<ItemStack> inputFilter = s -> true;
        private Predicate<ItemStack> runeFilter = s -> false;
        private Predicate<ItemStack> fuelFilter = s -> false;
        private int arrowX = -1, arrowY = -1, flameX = -1, flameY = -1;
        private Function<MachineMenu, List<Component>> infoLines = m -> List.of();
        private int infoX = 10, infoY = 20, infoW, infoH;
        private final java.util.Map<Integer, Hint> hints = new java.util.HashMap<>();

        private Builder(String name, Supplier<MenuType<MachineMenu>> menuType) {
            this.name = name;
            this.menuType = menuType;
        }

        public Builder slot(int x, int y, Kind kind) {
            this.slots.add(new SlotSpec(x, y, kind));
            return this;
        }

        /** Grade de espaços (colunas × linhas) começando em x,y. */
        public Builder grid(int x, int y, int cols, int rows, Kind kind) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    this.slot(x + c * 18, y + r * 18, kind);
                }
            }
            return this;
        }

        /** Dica para o espaço {@code slot} (na ordem em que foram adicionados): ícone apagado + tooltip quando vazio. */
        public Builder hint(int slot, Supplier<? extends net.minecraft.world.level.ItemLike> icon, String key) {
            this.hints.put(slot, new Hint(() -> new ItemStack(icon.get()), key));
            return this;
        }

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder input(Predicate<ItemStack> filter) {
            this.inputFilter = filter;
            return this;
        }

        public Builder rune(Predicate<ItemStack> filter) {
            this.runeFilter = filter;
            return this;
        }

        public Builder fuel(Predicate<ItemStack> filter) {
            this.fuelFilter = filter;
            return this;
        }

        public Builder arrow(int x, int y) {
            this.arrowX = x;
            this.arrowY = y;
            return this;
        }

        public Builder flame(int x, int y) {
            this.flameX = x;
            this.flameY = y;
            return this;
        }

        /** Texto numa caixa: quebra na largura {@code w} e encolhe (até 60%) para caber na altura {@code h}. */
        public Builder info(int x, int y, int w, int h, Function<MachineMenu, List<Component>> lines) {
            this.infoW = w;
            this.infoH = h;
            return this.info(x, y, lines);
        }

        public Builder info(int x, int y, Function<MachineMenu, List<Component>> lines) {
            this.infoX = x;
            this.infoY = y;
            this.infoLines = lines;
            return this;
        }

        public MachineLayout build() {
            return new MachineLayout(this);
        }
    }
}
