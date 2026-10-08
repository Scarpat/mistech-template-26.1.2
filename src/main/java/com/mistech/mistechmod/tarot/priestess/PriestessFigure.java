package com.mistech.mistechmod.tarot.priestess;

/**
 * O que a Sacerdotisa e os seus reflexos têm em comum para o desenho (o mesmo modelo e a mesma textura — por fora não
 * dá para diferenciar; só a verdadeira tem sombra) e para as magias não acertarem umas às outras.
 */
public interface PriestessFigure {
    int IDLE = 0, CAST = 1, OMEN = 2;

    /** Ação atual (para a animação): {@link #IDLE}, {@link #CAST} ou {@link #OMEN}. */
    int figureAction();

    /** Ticks (com fração) desde que a ação começou — só no cliente. */
    float figureActionTime(float partialTick);

    /** Sentada no trono (modo Boaz). */
    boolean seated();
}
