package ink.astrius.driftward;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import me.fallenbreath.conditionalmixin.api.mixin.RestrictiveMixinConfigPlugin;

import java.util.List;
import java.util.Set;

public class DriftwardMixinPlugin extends RestrictiveMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        MixinCancellerRegistrar.register(new DriftwardMixinCanceller());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }
}
