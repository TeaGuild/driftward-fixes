package ink.astrius.driftward.kube;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.rhino.Context;

public class DriftwardEmiEventJS implements KubeEvent {
    private final DriftwardServerData data;

    public DriftwardEmiEventJS(DriftwardServerData data) {
        this.data = data;
    }

    public void nuke(Context cx, Object filter) {
        data.removedItems().add(IngredientWrapper.wrap(cx, filter));
    }
}
