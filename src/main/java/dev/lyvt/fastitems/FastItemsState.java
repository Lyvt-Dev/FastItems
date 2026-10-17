package dev.lyvt.fastitems;

/** Per-entity state, never a global flag shared between deferred render submissions. */
public interface FastItemsState {
    boolean fastitems$isBillboard();
    void fastitems$setBillboard(boolean value);
}
