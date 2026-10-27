package ink.astrius.driftward.climate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.astrius.driftward.Driftward;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.Optional;

// Per-biome corrections for the climate model, as a data map (`data/driftward/data_maps/worldgen/biome/climate.json`).
//
// - offset: added to the annual mean, e.g. geothermal basins or ice-filled chasms
// - mean: replaces the noise-derived annual mean, for biomes placed by something other than the
//   climate noise (e.g. Terralith's islands, which are placed by weirdness)
// - lapse: whether the biome cools with altitude; off for volcanic biomes
// - glacier: permanent ice. Snow and ice there follow a temperature capped below the snow line, so
//   they never melt; the air (what players feel) is unchanged, since glaciers persist by their own
//   mass, not because summer air is below freezing
public record BiomeClimate(double offset, Optional<Double> mean, boolean lapse, boolean glacier) {
    public static final Codec<BiomeClimate> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.DOUBLE.optionalFieldOf("offset", 0.0).forGetter(BiomeClimate::offset),
        Codec.DOUBLE.optionalFieldOf("mean").forGetter(BiomeClimate::mean),
        Codec.BOOL.optionalFieldOf("lapse", true).forGetter(BiomeClimate::lapse),
        Codec.BOOL.optionalFieldOf("glacier", false).forGetter(BiomeClimate::glacier)
    ).apply(i, BiomeClimate::new));

    public static final DataMapType<Biome, BiomeClimate> DATA_MAP = DataMapType.builder(
        ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "climate"),
        Registries.BIOME,
        CODEC
    ).build();
}
