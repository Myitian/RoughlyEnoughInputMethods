package net.myitian.roughlyenoughinputmethods.forge;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.search.method.InputMethodRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraftforge.fml.common.Mod;
import net.myitian.roughlyenoughinputmethods.RoughlyEnoughInputMethods;

@Mod(RoughlyEnoughInputMethods.MOD_ID)
@REIPluginClient
public class RoughlyEnoughInputMethodsForge implements REIClientPlugin {
    public RoughlyEnoughInputMethodsForge() {
        RoughlyEnoughInputMethods.init();
    }

    @Override
    public void registerInputMethods(InputMethodRegistry registry) {
        RoughlyEnoughInputMethods.registerInputMethods(registry);
    }
}
