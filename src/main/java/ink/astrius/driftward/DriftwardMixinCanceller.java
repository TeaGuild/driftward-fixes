package ink.astrius.driftward;

import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

public class DriftwardMixinCanceller implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        return mixinClassName.equals("org.betterx.betterend.mixin.common.ServerPlayerGameModeMixin");
    }
}
