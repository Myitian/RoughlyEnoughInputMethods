package net.myitian.roughlyenoughinputmethods;

import dev.architectury.platform.Platform;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.search.method.InputMethodRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;
import net.myitian.roughlyenoughinputmethods.inputmethods.PinyinInputMethod;

import java.io.IOException;
import java.nio.file.Files;

public class RoughlyEnoughInputMethodsClient implements ClientModInitializer, REIClientPlugin {
    public static final String MOD_ID = "roughlyenoughinputmethods";
    public static final String REI_ID = "roughlyenoughitems";
    public static final Identifier IMPinyin = new Identifier(MOD_ID, "pinyin");

    @Override
    public void onInitializeClient() {
        try {
            Files.createDirectories(Platform.getConfigFolder().resolve(MOD_ID));
            Files.createDirectories(Platform.getConfigFolder().resolve(REI_ID));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerInputMethods(InputMethodRegistry registry) {
        UniHanManager manager = new UniHanManager(Platform.getConfigFolder().resolve("roughlyenoughitems/unihan.zip"));
        registry.add(IMPinyin, new PinyinInputMethod(manager));
    }
}