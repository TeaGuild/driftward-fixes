package ink.astrius.driftward;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;

public class Cobblegen {
    public static void register() {
        FluidInteractionRegistry.addInteraction(
            NeoForgeMod.LAVA_TYPE.value(),
            new FluidInteractionRegistry.InteractionInformation(
                NeoForgeMod.WATER_TYPE.value(),
                (fluidState) -> fluidState.isSource()
                    ? Blocks.OBSIDIAN.defaultBlockState()
                    : Blocks.COBBLED_DEEPSLATE.defaultBlockState()
            )
        );
    }
    public static void registerEmi(EmiRegistry registry) {
        registry.removeRecipes(ResourceLocation.fromNamespaceAndPath(
            "emi", "/world/fluid_interaction/minecraft/cobblestone"
        ));
        registry.removeRecipes(ResourceLocation.fromNamespaceAndPath(
            "emi", "/world/fluid_interaction/minecraft/stone"
        ));
        final var water = EmiStack.of(Fluids.WATER, 1000);
        final var lava = EmiStack.of(Fluids.LAVA, 1000);
        final var waterCatalyst = water.copy().setRemainder(water);
        final var lavaCatalyst = lava.copy().setRemainder(lava);

        registry.addRecipe(
            EmiWorldInteractionRecipe.builder()
                .id(ResourceLocation.fromNamespaceAndPath(
                    "driftward", "/world/fluid_interaction/cobbled_deepslate"
                ))
                .leftInput(waterCatalyst)
                .rightInput(lavaCatalyst, false)
                .output(EmiStack.of(Items.COBBLED_DEEPSLATE))
                .build()
        );
        registry.addRecipe(
            EmiWorldInteractionRecipe.builder()
                .id(ResourceLocation.fromNamespaceAndPath(
                    "driftward", "/world/fluid_interaction/deepslate"
                ))
                .leftInput(waterCatalyst)
                .rightInput(lavaCatalyst, false)
                .output(EmiStack.of(Items.DEEPSLATE))
                .build()
        );
    }
}
