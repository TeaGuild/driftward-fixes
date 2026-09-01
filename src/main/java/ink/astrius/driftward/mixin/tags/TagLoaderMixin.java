package ink.astrius.driftward.mixin.tags;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(TagLoader.class)
public class TagLoaderMixin {
    @WrapOperation(
        method = "build(Lnet/minecraft/tags/TagEntry$Lookup;Ljava/util/List;)Lcom/mojang/datafixers/util/Either;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        )
    )
    public boolean skipTags(List<TagLoader.EntryWithSource> instance, Object e, Operation<Boolean> original) {
        final var entry = (TagLoader.EntryWithSource) e;
        final var id = entry.entry().getId().toString();
        final var tag = LMFTCommonAccessor.driftward$getCurrentLoadingTagEntry().get();
        if (
            (
                tag.equals("minecraft:in_enchanting_table")
                    || tag.equals("minecraft:tradeable")
            )
                && id.equals("betterend:resonance")
        ) {
            return true;
        } else {
            return original.call(instance, e);
        }
    }
}
