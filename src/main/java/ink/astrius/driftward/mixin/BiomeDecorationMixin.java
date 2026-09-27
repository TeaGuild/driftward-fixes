package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.llamalad7.mixinextras.sugar.Share;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.function.Function;
import java.util.List;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;

// Vanilla inconsistently compares placed features by object identity vs deep equality, causing
// crashes and other issues when multiple features have the exact same definition -- which
// happens in our pack because we override multiple features to `no_op`. Jesus Christ.
@Mixin(FeatureSorter.class)
public class BiomeDecorationMixin {
    @Inject(method = "buildFeaturesPerStep", at = @At("HEAD"))
    private static <T> void createMap(
        List<T> featureSources,
        Function<T, List<HolderSet<PlacedFeature>>> featureGetter,
        boolean tryReducingError,
        CallbackInfoReturnable<List<FeatureSorter.StepFeatureData>> cir,
        @Share("map") LocalRef<Reference2IntOpenHashMap<Object>> mapRef
    ) {
        mapRef.set(new Reference2IntOpenHashMap<>());
    }

    @Redirect(
        method = "buildFeaturesPerStep",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap;computeIfAbsent(Ljava/lang/Object;Lit/unimi/dsi/fastutil/objects/Object2IntFunction;)I"
        )
    )
    private static int accessMap(
        Object2IntMap<PlacedFeature> instance,
        Object key,
        Object2IntFunction<PlacedFeature> mapping,
        @Share("map") LocalRef<Reference2IntOpenHashMap<Object>> mapRef
    ) {
        return mapRef.get().computeIfAbsent(key, mapping::getInt);
    }
}
