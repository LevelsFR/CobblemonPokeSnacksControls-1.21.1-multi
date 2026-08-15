package fr.levelsfr.cobblemonpokesnackscontrols.snack;

@FunctionalInterface
public interface SnackStateListener {
    void onStateChanged(SnackRuntimeState previousState, SnackRuntimeState newState);
}
