package net.myitian.roughlyenoughinputmethods.inputmethods;


import dev.architectury.platform.Platform;
import dev.architectury.utils.value.BooleanValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import me.shedaniel.rei.api.client.favorites.FavoriteMenuEntry;
import me.shedaniel.rei.api.client.search.method.CharacterUnpackingInputMethod;
import me.shedaniel.rei.api.client.search.method.InputMethod;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import net.minecraft.text.Text;
import net.myitian.roughlyenoughinputmethods.UniHanManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static net.myitian.roughlyenoughinputmethods.RoughlyEnoughInputMethodsClient.MOD_ID;

public class PinyinInputMethod extends UniHanInputMethod implements CharacterUnpackingInputMethod {
    protected static final Map<IntList, List<IntList>> incompleteInitialsFinals = new HashMap<>();
    protected static final Map<IntList, List<IntList>> vuFuzzyWithIncomplete = new HashMap<>();
    protected static final Map<IntList, IntList> vuFuzzy = new HashMap<>();
    protected static final String[] finals = {
            "a",
            "e",
            "i",
            "o",
            "u",
            "v",
            "ai",
            "ei",
            "ao",
            "ou",
            "ia",
            "ie",
            "ua",
            "uo",
            "ve",
            "iao",
            "iou",
            "uai",
            "uei",
            "an",
            "ian",
            "uan",
            "van",
            "en",
            "in",
            "uen",
            "vn",
            "ang",
            "iang",
            "uang",
            "eng ",
            "ing",
            "ueng",
            "ong",
            "iong",
    };

    static {
        incompleteInitialsFinals.put(IntList.of("zh".codePoints().toArray()), List.of(IntList.of("z".codePoints().toArray())));
        incompleteInitialsFinals.put(IntList.of('z', 'h'), List.of(IntList.of('z')));
        incompleteInitialsFinals.put(IntList.of('s', 'h'), List.of(IntList.of('s')));
        incompleteInitialsFinals.put(IntList.of('c', 'h'), List.of(IntList.of('c')));
        for (String srcFinal : finals) {
            IntList src = IntList.of(srcFinal.codePoints().toArray());
            List<IntList> inc = new ArrayList<>();
            List<IntList> vuf = new ArrayList<>();
            for (int i = srcFinal.length() - 1; i > 0; i--) {
                String f = srcFinal.substring(0, i);
                inc.add(IntList.of(f.codePoints().toArray()));
                if (f.contains("v")) {
                    vuf.add(IntList.of(f.replace('v', 'u').codePoints().toArray()));
                }
            }
            incompleteInitialsFinals.put(src, inc);
            if (!vuf.isEmpty()) {
                vuFuzzyWithIncomplete.put(src, vuf);
            }
            if (srcFinal.contains("v")) {
                vuFuzzy.put(src, IntList.of(srcFinal.replace('v', 'u').codePoints().toArray()));
            }
        }
    }

    protected final Map<IntList, IntList> fuzzyMap = new LinkedHashMap<>();
    protected final Int2ObjectMap<ToneEntry> toneMap;
    protected final Set<IntList> fuzzySet = new HashSet<>();
    protected boolean allowIncompleteInitialsFinals = false;
    protected boolean allowVUFuzzy = false;

    public PinyinInputMethod(UniHanManager manager) {
        super(manager);
        toneMap = new Int2ObjectOpenHashMap<>();
        addTone('ā', "a1");
        addTone('á', "a2");
        addTone('ǎ', "a3");
        addTone('à', "a4");
        addTone('ē', "e1");
        addTone('é', "e2");
        addTone('ě', "e3");
        addTone('è', "e4");
        addTone('ī', "i1");
        addTone('í', "i2");
        addTone('ǐ', "i3");
        addTone('ì', "i4");
        addTone('ō', "o1");
        addTone('ó', "o2");
        addTone('ǒ', "o3");
        addTone('ò', "o4");
        addTone('ū', "u1");
        addTone('ú', "u2");
        addTone('ǔ', "u3");
        addTone('ù', "u4");
        addTone('ǖ', "v1");
        addTone('ǘ', "v2");
        addTone('ǚ', "v3");
        addTone('ǜ', "v4");

        addFuzzy("z", "zh");
        addFuzzy("s", "sh");
        addFuzzy("c", "ch");
        addFuzzy("an", "ang");
        addFuzzy("en", "eng");
        addFuzzy("in", "ing");
        addFuzzy("ian", "iang");
        addFuzzy("uan", "uang");
        addFuzzy("n", "l");
        addFuzzy("r", "l");
        addFuzzy("h", "f");
        read();
    }

    private void addFuzzy(String original, String to) {
        this.fuzzyMap.put(IntList.of(original.codePoints().toArray()), IntList.of(to.codePoints().toArray()));
    }

    private void addTone(char c, String s) {
        this.toneMap.put(c, new ToneEntry(s.charAt(0), Character.digit(s.charAt(1), 10)));
    }

    protected void read() {
        Path path = Platform.getConfigFolder().resolve(MOD_ID + "/pinyin.properties");
        this.fuzzySet.clear();
        if (Files.exists(path)) {
            try {
                Properties properties = new Properties();
                try (InputStream stream = Files.newInputStream(path)) {
                    properties.load(stream);
                }
                allowIncompleteInitialsFinals = properties.getOrDefault(
                        "AllowIncompleteInitialsFinals", "false").equals("true");
                allowVUFuzzy = properties.getOrDefault(
                        "AllowVUFuzzy", "false").equals("true");
                for (IntList key : this.fuzzyMap.keySet()) {
                    if (properties.getOrDefault("Fuzzy:" + new String(key.toIntArray(), 0, key.size()), "false").equals("true")) {
                        this.fuzzySet.add(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        this.write();
    }

    protected void write() {
        Path path = Platform.getConfigFolder().resolve(MOD_ID + "/pinyin.properties");
        Properties properties = new Properties();
        properties.put("AllowIncompleteInitialsFinals", allowIncompleteInitialsFinals ? "true" : "false");
        properties.put("AllowVUFuzzy", allowVUFuzzy ? "true" : "false");
        for (IntList key : this.fuzzyMap.keySet()) {
            if (this.fuzzySet.contains(key)) {
                properties.put("Fuzzy_" + new String(key.toIntArray(), 0, key.size()), "true");
            }
        }
        try (OutputStream stream = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            properties.store(stream, "Pinyin Options");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<IntList> expendFilter(String filter) {
        return Collections.singletonList(IntList.of(filter.codePoints().toArray()));
    }

    @Override
    public List<ExpendedChar> expendSourceChar(int codePoint) {
        List<ExpendedChar> sequences = dataMap.get(codePoint);
        if (sequences != null && !sequences.isEmpty()) return sequences;
        return List.of(new ExpendedChar(List.of(IntList.of(codePoint))));
    }

    @Override
    public List<Locale> getMatchingLocales() {
        return CollectionUtils.filterToList(InputMethod.getAllLocales(), locale -> locale.code().startsWith("zh_"));
    }

    @Override
    public Text getName() {
        return Text.translatable("text.roughlyenoughinputmethods.input.methods.pinyin",
                Text.translatable("text.rei.input.methods.pinyin"));
    }

    @Override
    public Text getDescription() {
        return Text.translatable("text.rei.input.methods.pinyin.description");
    }

    @Override
    public List<FavoriteMenuEntry> getOptionsMenuEntries() {
        List<FavoriteMenuEntry> innerEntries = new ArrayList<>();
        innerEntries.add(FavoriteMenuEntry.createToggle(
                Text.translatable("text.roughlyenoughinputmethods.input.methods.pinyin.allow-incomplete"),
                new BooleanValue() {
                    @Override
                    public void accept(boolean b) {
                        PinyinInputMethod.this.allowIncompleteInitialsFinals = b;
                        PinyinInputMethod.this.write();
                        PinyinInputMethod.this.dataMap.clear();
                        PinyinInputMethod.this.load();
                    }

                    @Override
                    public boolean getAsBoolean() {
                        return PinyinInputMethod.this.allowIncompleteInitialsFinals;
                    }
                }));
        innerEntries.add(FavoriteMenuEntry.createToggle(
                Text.translatable("text.roughlyenoughinputmethods.input.methods.pinyin.allow-vu-fuzzy"),
                new BooleanValue() {
                    @Override
                    public void accept(boolean b) {
                        PinyinInputMethod.this.allowVUFuzzy = b;
                        PinyinInputMethod.this.write();
                        PinyinInputMethod.this.dataMap.clear();
                        PinyinInputMethod.this.load();
                    }

                    @Override
                    public boolean getAsBoolean() {
                        return PinyinInputMethod.this.allowVUFuzzy;
                    }
                }));
        this.fuzzyMap.forEach((from, to) -> {
            innerEntries.add(FavoriteMenuEntry.createToggle(Text.literal("%s -> %s".formatted(new String(from.toIntArray(), 0, from.size()), new String(to.toIntArray(), 0, to.size()))),
                    new BooleanValue() {
                        @Override
                        public boolean getAsBoolean() {
                            return PinyinInputMethod.this.fuzzySet.contains(from);
                        }

                        @Override
                        public void accept(boolean t) {
                            if (t) {
                                PinyinInputMethod.this.fuzzySet.add(from);
                            } else {
                                PinyinInputMethod.this.fuzzySet.remove(from);
                            }
                            PinyinInputMethod.this.write();
                            PinyinInputMethod.this.dataMap.clear();
                            PinyinInputMethod.this.load();
                        }
                    }));
        });
        return List.of(FavoriteMenuEntry.createSubMenu(Text.translatable("text.rei.input.methods.pinyin.fuzzy.matching"),
                innerEntries));
    }

    @Override
    protected List<CharacterUnpackingInputMethod.ExpendedChar> asExpendedChars(String string) {
        List<IntList>[] codepoints = new List[3];
        int skip = 2;
        int tone = -1;
        char[] chars = string.toCharArray();
        if (chars[0] == 's' && chars[1] == 'h') {
            codepoints[0] = this.expendInitials("sh");
        } else if (chars[0] == 'c' && chars[1] == 'h') {
            codepoints[0] = this.expendInitials("ch");
        } else if (chars[0] == 'z' && chars[1] == 'h') {
            codepoints[0] = this.expendInitials("zh");
        } else {
            skip = 1;
            ToneEntry toneEntry = toneMap.get(chars[0]);
            if (toneEntry == null) {
                codepoints[0] = this.expendInitials(chars[0] + "");
            } else {
                codepoints[0] = this.expendInitials(((char) toneEntry.codepoint()) + "");
                tone = toneEntry.tone();
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = skip; i < chars.length; i++) {
            char c = chars[i];
            if (c == 'ü') {
                builder.append('v');
            } else {
                ToneEntry toneEntry = toneMap.get(c);
                if (toneEntry == null) {
                    builder.append(c);
                } else {
                    builder.append((char) toneEntry.codepoint());
                    tone = toneEntry.tone();
                }
            }
        }
        int length = 2;
        if (builder.isEmpty()) {
            List<IntList>[] expendSingles = this.expendSingles(codepoints[0]);
            codepoints[0] = expendSingles[0];
            if (expendSingles.length > 1) {
                codepoints[1] = expendSingles[1];
            } else {
                length = 1;
            }
        } else {
            codepoints[1] = this.expendFinals(builder.toString());
        }
        if (tone != -1) {
            codepoints[++length - 1] = List.of(IntList.of(Character.forDigit(tone, 10)));
        }
        int combinations = 1;
        for (int i = 0; i < length; i++) {
            combinations *= codepoints[i].size();
        }
        List<IntList>[] results = new List[combinations];
        int[] current = new int[length];
        for (int i = 0; i < combinations; i++) {
            results[i] = new ArrayList<>();
            for (int k = 0; k < length; k++) {
                results[i].add(codepoints[k].get(current[k]));
            }

            for (int k = 0; k < length; k++) {
                if (current[k] + 1 < codepoints[k].size()) {
                    current[k]++;
                    break;
                } else {
                    current[k] = 0;
                }
            }
        }

        return CollectionUtils.map(results, ExpendedChar::new);
    }

    protected List<IntList>[] expendSingles(List<IntList> codepoint) {
        return new List[]{codepoint};
    }

    protected List<IntList> expendSimple(String string) {
        IntList codepoints = IntList.of(string.codePoints().toArray());
        List<IntList> result = new ArrayList<>();
        result.add(codepoints);
        if (allowIncompleteInitialsFinals) {
            List<IntList> l = incompleteInitialsFinals.get(codepoints);
            if (l != null) {
                result.addAll(l);
            }
        }
        if (allowVUFuzzy) {
            IntList l = vuFuzzy.get(codepoints);
            if (l != null) {
                result.add(l);
            }
        }
        if (allowVUFuzzy && allowIncompleteInitialsFinals) {
            List<IntList> l = vuFuzzyWithIncomplete.get(codepoints);
            if (l != null) {
                result.addAll(l);
            }
        }
        if (fuzzySet.contains(codepoints)) {
            result.add(fuzzyMap.get(codepoints));
        }
        return result;
    }

    protected List<IntList> expendInitials(String string) {
        return this.expendSimple(string);
    }

    protected List<IntList> expendFinals(String string) {
        return this.expendSimple(string);
    }

    @Override
    public void load() {
        try {
            manager.load((codepoint, fieldKey, data) -> {
                HashSet<String> strSet = new HashSet<>(4);
                int start, end;
                switch (fieldKey) {
                    case "kMandarin":
                        start = 0;
                        while ((end = data.indexOf(' ', start)) >= 0) {
                            strSet.add(data.substring(start, end));
                            start = end + 1;
                        }
                        strSet.add(data.substring(start));
                        break;
                    case "kHanyuPinyin":
                        start = data.indexOf(':') + 1;
                        while ((end = data.indexOf(',', start)) >= 0) {
                            strSet.add(data.substring(start, end));
                            start = end + 1;
                        }
                        strSet.add(data.substring(start));
                        break;
                    case "kTGHZ2013":
                    case "kXHC1983":
                        start = 0;
                        while (true) {
                            start = data.indexOf(':', start) + 1;
                            end = data.indexOf(' ', start);
                            if (end < 0) {
                                strSet.add(data.substring(start));
                                break;
                            }
                            strSet.add(data.substring(start, end));
                            start = end + 1;
                        }
                        break;
                }
                List<CharacterUnpackingInputMethod.ExpendedChar> sequences = dataMap.computeIfAbsent(codepoint, value -> new ArrayList<>(strSet.size()));
                for (String string : strSet) {
                    sequences.addAll(asExpendedChars(string));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected record ToneEntry(int codepoint, int tone) {
    }
}
