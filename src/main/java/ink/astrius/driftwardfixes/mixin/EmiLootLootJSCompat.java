package ink.astrius.driftwardfixes.mixin;

import com.google.gson.JsonElement;
import fzzyhmstrs.emi_loot.EMILoot;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// From https://github.com/mosharky/EMI_Loot_Fix/blob/95f7a23db709cd45b041974dbe0148bb88a69e5d/src/main/java/dev/momo/emi_loot_fix/mixin/ReloadableServerRegistriesMixin.java
@Restriction(
    require = {
        @Condition("emi_loot"),
        @Condition("lootjs")
    }
)
@Mixin(value = ReloadableServerRegistries.class, priority = 900)
public class EmiLootLootJSCompat {
    @Unique
    private static ResourceManager remix_core$resourceManager;
    @Unique
    private static RegistryOps<JsonElement> remix_core$registryOps;

    @Inject(
        method = "lambda$scheduleElementParse$4",
        at = @At("RETURN")
    )
    private static <T> void remix_core$captureLootTableLoadContext(
        LootDataType<T> lootDataType,
        ResourceManager resourceManager,
        RegistryOps<JsonElement> registryOps,
        CallbackInfoReturnable<WritableRegistry<?>> cir
    ) {
        if (lootDataType == LootDataType.TABLE) {
            remix_core$resourceManager = resourceManager;
            remix_core$registryOps = registryOps;
        }
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private static void remix_core$reparseEmiLootAfterLootJs(
        LayeredRegistryAccess<RegistryLayer> registries,
        List<WritableRegistry<?>> writableRegistries,
        CallbackInfoReturnable<LayeredRegistryAccess<RegistryLayer>> cir
    ) {
        if (remix_core$resourceManager == null || remix_core$registryOps == null) return;
        writableRegistries.stream()
            .filter(registry ->
                registry.key().equals(Registries.LOOT_TABLE)
            )
            .findFirst()
            .ifPresent(registry ->
                EMILoot.parseTables(
                    remix_core$resourceManager,
                    (Registry<LootTable>) registry,
                    remix_core$registryOps
                )
            );
    }
}
