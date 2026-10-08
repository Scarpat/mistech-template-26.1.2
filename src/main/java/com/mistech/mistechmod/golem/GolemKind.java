package com.mistech.mistechmod.golem;

/** Os golems de trabalho (a estátua de cada material vira um). */
public enum GolemKind {
    /** Terracota: leva da origem para os baús em volta do destino que já têm aquele item (sobra vai para o destino). */
    SORTER("sorter", 32, 0.25),
    /** Ametista: recolhe itens do chão em volta do destino e guarda nele. */
    COLLECTOR("collector", 32, 0.28),
    /** Mitril: transporte rápido; com Runa do Espaço, teleporta entre os pontos. */
    MESSENGER("messenger", 64, 0.38),
    /** Pedra-negra: faz uma receita (aprendida ao mostrar o item) com ingredientes da origem e guarda no destino. */
    CRAFTER("crafter", 64, 0.25),
    /** Oricalco: mantém um estoque no destino buscando no Arquivo Espacial. */
    KEEPER("keeper", 64, 0.3);

    public final String id;
    public final int carry;
    public final double speed;

    GolemKind(String id, int carry, double speed) {
        this.id = id;
        this.carry = carry;
        this.speed = speed;
    }

    public static GolemKind byId(int ordinal) {
        GolemKind[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }
}
