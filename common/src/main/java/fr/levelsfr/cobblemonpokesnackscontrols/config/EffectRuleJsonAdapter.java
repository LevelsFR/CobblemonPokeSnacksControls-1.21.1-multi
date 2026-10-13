package fr.levelsfr.cobblemonpokesnackscontrols.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

@SuppressWarnings("deprecation")
final class EffectRuleJsonAdapter implements JsonSerializer<EffectRule>, JsonDeserializer<EffectRule> {
    @Override
    public JsonElement serialize(EffectRule rule, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", rule.enabled);
        json.addProperty("valueMultiplier", rule.valueMultiplier);
        json.addProperty("chanceMultiplier", rule.chanceMultiplier);
        json.add("maximumValue", rule.maximumValue == null ? JsonNull.INSTANCE : context.serialize(rule.maximumValue));
        json.add("maximumChance", rule.maximumChance == null ? JsonNull.INSTANCE : context.serialize(rule.maximumChance));
        return json;
    }

    @Override
    public EffectRule deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        EffectRule rule = new EffectRule();
        if (!json.isJsonObject()) {
            return rule;
        }

        JsonObject object = json.getAsJsonObject();
        if (object.has("enabled") && object.get("enabled").isJsonPrimitive()) {
            rule.enabled = object.get("enabled").getAsBoolean();
        }
        if (object.has("valueMultiplier") && object.get("valueMultiplier").isJsonPrimitive()) {
            rule.valueMultiplier = object.get("valueMultiplier").getAsDouble();
        }
        if (object.has("chanceMultiplier") && object.get("chanceMultiplier").isJsonPrimitive()) {
            rule.chanceMultiplier = object.get("chanceMultiplier").getAsDouble();
        }
        if (object.has("maximumValue") && object.get("maximumValue").isJsonPrimitive()) {
            rule.maximumValue = object.get("maximumValue").getAsDouble();
        }
        if (object.has("maximumChance") && object.get("maximumChance").isJsonPrimitive()) {
            rule.maximumChance = object.get("maximumChance").getAsDouble();
        }
        if (object.has("multiplier") && object.get("multiplier").isJsonPrimitive()) {
            rule.multiplier = object.get("multiplier").getAsDouble();
        }
        return rule;
    }
}
