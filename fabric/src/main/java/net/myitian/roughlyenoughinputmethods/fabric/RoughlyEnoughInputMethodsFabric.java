package net.myitian.roughlyenoughinputmethods.fabric;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.search.method.InputMethodRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.myitian.roughlyenoughinputmethods.RoughlyEnoughInputMethods;

public class RoughlyEnoughInputMethodsFabric implements ClientModInitializer, REIClientPlugin {
    @Override
    public void onInitializeClient() {
        RoughlyEnoughInputMethods.init();
    }

    @Override
    public void registerInputMethods(InputMethodRegistry registry) {
        RoughlyEnoughInputMethods.registerInputMethods(registry);
    }
}
