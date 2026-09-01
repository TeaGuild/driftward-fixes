package ink.astrius.driftward.mixin.tags;

import io.wispforest.lmft.LMFTCommon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LMFTCommon.class)
public interface LMFTCommonAccessor {
    @Accessor("CURRENT_LOADING_TAG_ENTRY")
    static ThreadLocal<String> driftward$getCurrentLoadingTagEntry() {
        throw new AssertionError();
    }
}
