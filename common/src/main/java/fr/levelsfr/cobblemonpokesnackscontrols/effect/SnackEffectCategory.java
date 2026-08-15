package fr.levelsfr.cobblemonpokesnackscontrols.effect;

/**
 * Every native Cobblemon 1.7.3 Poké Snack influence family targeted by the V1 rule engine.
 * The adapter that maps Cobblemon bait effect identifiers to these categories is intentionally
 * deferred to the gameplay-hook phase.
 */
public enum SnackEffectCategory {
    TYPE_ATTRACTION,
    EGG_GROUP_ATTRACTION,
    EV_YIELD_FILTER,
    RARITY_BUCKET,
    NATURE,
    IV,
    SHINY,
    HIDDEN_ABILITY,
    GENDER,
    LEVEL,
    FRIENDSHIP,
    DROPS,
    BITE_TIME
}
