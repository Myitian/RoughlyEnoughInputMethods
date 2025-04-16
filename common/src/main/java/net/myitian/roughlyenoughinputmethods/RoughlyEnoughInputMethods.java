package net.myitian.roughlyenoughinputmethods;

import dev.architectury.platform.Platform;
import me.shedaniel.rei.api.client.search.method.InputMethodRegistry;
import net.minecraft.util.Identifier;
import net.myitian.roughlyenoughinputmethods.inputmethods.PinyinInputMethod;

import java.io.IOException;
import java.nio.file.Files;

public final class RoughlyEnoughInputMethods {
    public static final String MOD_ID = "roughlyenoughinputmethods";
    public static final String REI_ID = "roughlyenoughitems";
    public static final Identifier IMPinyin = Identifier.of(MOD_ID, "pinyin");

    public static void init() {
        try {
            Files.createDirectories(Platform.getConfigFolder().resolve(MOD_ID));
            Files.createDirectories(Platform.getConfigFolder().resolve(REI_ID));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerInputMethods(InputMethodRegistry registry) {
        UniHanManager manager = new UniHanManager(Platform.getConfigFolder().resolve(REI_ID + "/unihan.zip"));
        registry.add(IMPinyin, new PinyinInputMethod(manager));
    }
}
