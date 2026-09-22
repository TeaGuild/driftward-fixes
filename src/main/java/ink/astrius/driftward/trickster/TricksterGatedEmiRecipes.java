package ink.astrius.driftward.trickster;

import de.dafuqs.spectrum.compat.emi.recipes.AnvilCrushingEmiRecipeGated;
import de.dafuqs.spectrum.compat.emi.recipes.SpectrumWorldInteractionRecipe;
import de.dafuqs.spectrum.recipe.anvil_crushing.AnvilCrushingRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Optional;

public class TricksterGatedEmiRecipes {
    public static void register(EmiRegistry registry) {
        if (!ModList.get().isLoaded("trickster")) {
            return;
        }
        final var tormentCoreShell = BuiltInRegistries.ITEM.get(ResourceLocation.parse(
            "trickster:inactive_spawner_spell_core"
        ));
        final var tormentCore = BuiltInRegistries.ITEM.get(ResourceLocation.parse(
            "trickster:spawner_spell_core"
        ));
        final var inertSpawner = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
            "trickster:inert_spawner"
        ));

        registry.addRecipe(SpectrumWorldInteractionRecipe.customBuilder()
            .id(ResourceLocation.parse("driftward:/world/block_interaction/trickster/spawner"))
            .leftInput(EmiStack.of(Blocks.SPAWNER))
            .rightInput(EmiStack.of(tormentCoreShell), false)
            .output(EmiStack.of(inertSpawner))
            .output(EmiStack.of(tormentCore))
            .requiredAdvancement(ResourceLocation.parse("trickster:spawner_spell_core"))
            .build());

        for (final var knotType : List.of("amethyst", "emerald", "diamond", "astral", "quartz", "prismatic", "echo")) {
            final var knot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                "trickster", knotType + "_knot"
            ));
            final var crackedKnot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                "trickster", "cracked_" + knotType + "_knot"
            ));
            registry.addRecipe(new AnvilCrushingEmiRecipeGated(
                new RecipeHolder<>(
                    ResourceLocation.parse("driftward:/anvil_crushing/trickster/" + knotType + "_knot_crushing"),
                    new AnvilCrushingRecipe(
                        "",
                        Optional.of(ResourceLocation.parse("trickster:find_cracked_knot")),
                        Optional.empty(),
                        List.of(),
                        Ingredient.of(knot),
                        new ItemStack(crackedKnot),
                        1,
                        1,
                        Optional.empty(),
                        0,
                        null
                    )
                )
            ));
        }
    }
}
