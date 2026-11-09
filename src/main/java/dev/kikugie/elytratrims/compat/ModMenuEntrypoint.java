package dev.kikugie.elytratrims.compat;

//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.kikugie.elytratrims.cosmetic.gui.ElytraCosmeticScreen;
import dev.kikugie.fletching_table.fabric.Entrypoint;

/** Called by ModMenu only, when it is installed: its config button opens the cosmetic editor. */
@Entrypoint("modmenu")
public class ModMenuEntrypoint implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ElytraCosmeticScreen::new;
    }
}
//?}
