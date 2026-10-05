package ink.astrius.driftward.kube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record DriftwardServerData(
    List<Ingredient> removedItems
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DriftwardServerData> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("driftward", "sync_server_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DriftwardServerData> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), DriftwardServerData::removedItems,
        DriftwardServerData::new
    );

    @Nullable
    public static DriftwardServerData receivedInstance;
    @Nullable
    public static DriftwardServerData preparedInstance;

    public static DriftwardServerData empty() {
        return new DriftwardServerData(new ArrayList<>());
    }

    public DriftwardServerData freeze() {
        return new DriftwardServerData(Collections.unmodifiableList(removedItems));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        receivedInstance = this;
    }
}
