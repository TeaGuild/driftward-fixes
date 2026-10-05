package ink.astrius.driftward.kube;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import ink.astrius.driftward.Driftward;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Arrays;

@EventBusSubscriber(modid = Driftward.MOD_ID)
public class DriftwardKubeJSPlugin implements KubeJSPlugin {
    public static EventGroup GROUP = EventGroup.of("Driftward");
    public static EventHandler EMI = GROUP.server("emi", () -> DriftwardEmiEventJS.class);

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GROUP);
    }

    @SubscribeEvent
    public static void registerPackets(final RegisterPayloadHandlersEvent event) {
        final var registry = event.registrar("1");
        registry.playToClient(DriftwardServerData.TYPE, DriftwardServerData.STREAM_CODEC, DriftwardServerData::handle);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // Reload is in progress OR a new player join is in progress, and we don't have prepared data
        if (event.getPlayer() == null || DriftwardServerData.preparedInstance == null) {
            final var inst = DriftwardServerData.empty();
            if (EMI.hasListeners()) {
                EMI.post(ScriptType.SERVER, new DriftwardEmiEventJS(inst));
            }
            DriftwardServerData.preparedInstance = inst.freeze();
        }
        final var payload = DriftwardServerData.preparedInstance;
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    public static void initEmi(EmiInitRegistry registry) {
        final var data = DriftwardServerData.receivedInstance;
        if (data == null) {
            return;
        }
        final var removedItems = data.removedItems().stream().flatMap(i -> Arrays.stream(i.getItems())).toList();
        for (final var item : removedItems) {
            registry.disableStack(EmiStack.of(item));
        }
    }
}
