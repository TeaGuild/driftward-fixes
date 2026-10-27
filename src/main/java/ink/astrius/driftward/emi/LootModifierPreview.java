package ink.astrius.driftward.emi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

// EMI Loot only reads loot tables, so anything a global loot modifier adds is invisible in it. Most
// modifiers in the pack are declarative ("when table X rolls, also roll table Y / add item Z with
// chance C"), so translate those into extra pools on a copy of the loot table registry that is only
// handed to EMI Loot. The real tables are untouched, so nothing drops twice.
public class LootModifierPreview {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation GLOBAL_LIST =
        ResourceLocation.fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json");

    public static Registry<LootTable> withModifiers(
        ResourceManager resources,
        Registry<LootTable> tables,
        RegistryOps<JsonElement> ops
    ) {
        final Map<ResourceLocation, List<JsonObject>> extraPools = new HashMap<>();
        for (final var id : activeModifiers(resources)) {
            try {
                readModifier(resources, ops, id, extraPools);
            } catch (Exception e) {
                LOGGER.debug("Skipping loot modifier {} in EMI preview", id, e);
            }
        }
        if (extraPools.isEmpty()) {
            return tables;
        }

        final var copy = new MappedRegistry<>(Registries.LOOT_TABLE, Lifecycle.experimental());
        var augmented = 0;
        for (final var entry : tables.entrySet()) {
            var table = entry.getValue();
            final var pools = extraPools.get(entry.getKey().location());
            if (pools != null) {
                final var withPools = addPools(table, pools, ops);
                if (withPools != null) {
                    table = withPools;
                    augmented++;
                }
            }
            copy.register(entry.getKey(), table, RegistrationInfo.BUILT_IN);
        }
        copy.freeze();
        LOGGER.info("Showing loot modifiers in EMI for {} loot tables", augmented);
        return copy;
    }

    // `global_loot_modifiers.json` is merged across data packs, and `replace` drops earlier entries
    private static List<ResourceLocation> activeModifiers(ResourceManager resources) {
        final var ids = new LinkedHashSet<ResourceLocation>();
        for (final var resource : resources.getResourceStack(GLOBAL_LIST)) {
            try (Reader reader = resource.openAsReader()) {
                final var json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("replace") && json.get("replace").getAsBoolean()) {
                    ids.clear();
                }
                for (final var entry : json.getAsJsonArray("entries")) {
                    ids.add(ResourceLocation.parse(entry.getAsString()));
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to read {}", GLOBAL_LIST, e);
            }
        }
        return new ArrayList<>(ids);
    }

    private static void readModifier(
        ResourceManager resources,
        RegistryOps<JsonElement> ops,
        ResourceLocation id,
        Map<ResourceLocation, List<JsonObject>> extraPools
    ) throws Exception {
        final var file = id.withPath(path -> "loot_modifiers/" + path + ".json");
        final var resource = resources.getResource(file);
        if (resource.isEmpty()) {
            return;
        }
        final JsonObject json;
        try (Reader reader = resource.get().openAsReader()) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        // Modifiers disabled through `neoforge:conditions` (mod_loaded, config flags) don't load in game
        if (!ICondition.conditionsMatched(ops, json)) {
            return;
        }

        // Conditions that pick the table become targets; the rest (tools, killer, chance, ...) stay on
        // the pool, so EMI Loot shows them
        final var targets = new ArrayList<ResourceLocation>();
        final var poolConditions = new JsonArray();
        if (json.has("conditions")) {
            for (final var condition : json.getAsJsonArray("conditions")) {
                final var obj = condition.getAsJsonObject();
                if (!collectTargets(obj, targets) || keepWithTarget(obj)) {
                    poolConditions.add(obj);
                }
            }
        }
        if (json.has("target")) {
            targets.add(ResourceLocation.parse(json.get("target").getAsString()));
        }
        if (json.has("targets")) {
            for (final var target : json.getAsJsonArray("targets")) {
                targets.add(ResourceLocation.parse(target.getAsString()));
            }
        }
        if (json.has("chance")) {
            final var randomChance = new JsonObject();
            randomChance.addProperty("condition", "minecraft:random_chance");
            randomChance.addProperty("chance", json.get("chance").getAsDouble());
            poolConditions.add(randomChance);
        }
        if (targets.isEmpty()) {
            // Targets that depend on entity tags, damage sources and such can't be shown per table
            return;
        }

        final var entries = new JsonArray();
        for (final var key : List.of("table", "lootTable")) {
            if (json.has(key)) {
                entries.add(tableEntry(json.get(key).getAsString(), 1));
            }
        }
        if (json.has("tables")) {
            for (final var table : json.getAsJsonArray("tables")) {
                final var obj = table.getAsJsonObject();
                entries.add(tableEntry(obj.get("data").getAsString(), weight(obj)));
            }
        }
        if (json.has("item")) {
            final var item = json.get("item");
            entries.add(item.isJsonPrimitive() ? itemEntry(item.getAsString()) : itemEntry(item.getAsJsonObject(), 1));
        }
        if (json.has("items")) {
            for (final var item : json.getAsJsonArray("items")) {
                final var obj = item.getAsJsonObject();
                entries.add(itemEntry(obj.has("data") ? obj.getAsJsonObject("data") : obj, weight(obj)));
            }
        }
        if (entries.isEmpty()) {
            return;
        }

        final var pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", entries);
        if (!poolConditions.isEmpty()) {
            pool.add("conditions", poolConditions);
        }
        for (final var target : targets) {
            extraPools.computeIfAbsent(target, k -> new ArrayList<>()).add(pool);
        }
    }

    // Returns whether the condition only selects which table the modifier applies to
    private static boolean collectTargets(JsonObject condition, List<ResourceLocation> out) {
        final var type = stripMinecraft(condition.get("condition").getAsString());
        switch (type) {
            case "neoforge:loot_table_id" -> {
                out.add(ResourceLocation.parse(condition.get("loot_table_id").getAsString()));
                return true;
            }
            case "block_state_property" -> {
                out.add(ResourceLocation.parse(condition.get("block").getAsString()).withPrefix("blocks/"));
                return true;
            }
            case "entity_properties" -> {
                final var predicate = condition.getAsJsonObject("predicate");
                final var isThis = !condition.has("entity") || condition.get("entity").getAsString().equals("this");
                if (isThis && predicate != null && predicate.size() == 1 && predicate.has("type")
                    && predicate.get("type").isJsonPrimitive()
                    && !predicate.get("type").getAsString().startsWith("#")) {
                    out.add(ResourceLocation.parse(predicate.get("type").getAsString()).withPrefix("entities/"));
                    return true;
                }
                return false;
            }
            case "any_of" -> {
                final var found = new ArrayList<ResourceLocation>();
                for (final var term : condition.getAsJsonArray("terms")) {
                    if (!collectTargets(term.getAsJsonObject(), found)) {
                        return false;
                    }
                }
                out.addAll(found);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // A block condition with state properties ("fully grown") is worth showing even though it also
    // picks the table
    private static boolean keepWithTarget(JsonObject condition) {
        return stripMinecraft(condition.get("condition").getAsString()).equals("block_state_property")
            && condition.has("properties");
    }

    private static String stripMinecraft(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private static int weight(JsonObject obj) {
        return obj.has("weight") ? obj.get("weight").getAsInt() : 1;
    }

    private static JsonObject tableEntry(String table, int weight) {
        final var entry = new JsonObject();
        entry.addProperty("type", "minecraft:loot_table");
        entry.addProperty("value", table);
        entry.addProperty("weight", weight);
        return entry;
    }

    private static JsonObject itemEntry(String id) {
        final var item = new JsonObject();
        item.addProperty("id", id);
        return itemEntry(item, 1);
    }

    private static JsonObject itemEntry(JsonObject item, int weight) {
        final var entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", item.get("id").getAsString());
        entry.addProperty("weight", weight);
        final var countKey = item.has("count") ? "count" : item.has("Count") ? "Count" : null;
        if (countKey != null && item.get(countKey).getAsInt() != 1) {
            final var functions = new JsonArray();
            final var setCount = new JsonObject();
            setCount.addProperty("function", "minecraft:set_count");
            setCount.addProperty("count", item.get(countKey).getAsInt());
            functions.add(setCount);
            entry.add("functions", functions);
        }
        return entry;
    }

    private static LootTable addPools(LootTable table, List<JsonObject> pools, RegistryOps<JsonElement> ops) {
        try {
            final var json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();
            final var allPools = json.has("pools") ? json.getAsJsonArray("pools") : new JsonArray();
            pools.forEach(allPools::add);
            json.add("pools", allPools);
            return LootTable.DIRECT_CODEC.parse(ops, json).getOrThrow();
        } catch (Exception e) {
            LOGGER.debug("Could not add modifier pools to a loot table for EMI", e);
            return null;
        }
    }
}
