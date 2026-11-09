package dev.kikugie.elytratrims.compat;

//? if fabric {
import dev.kikugie.fletching_table.fabric.Entrypoint;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;

/** Called by LambDynamicLights only, when it is installed. */
@Entrypoint(DynamicLightsInitializer.ENTRYPOINT_KEY)
public class LambDynamicLightsEntrypoint implements DynamicLightsInitializer {
    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        GlowingWingsLight.INSTANCE.init(context);
    }
}
//?}
