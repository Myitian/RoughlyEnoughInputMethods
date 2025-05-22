package net.myitian.roughlyenoughinputmethods.inputmethods;

// Based on me.shedaniel.rei.impl.client.search.method.unihan.UniHanInputMethod
// MIT License

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import me.shedaniel.rei.api.client.search.method.CharacterUnpackingInputMethod;
import me.shedaniel.rei.api.client.search.method.InputMethod;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import net.myitian.roughlyenoughinputmethods.UniHanManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class UniHanInputMethod implements InputMethod<IntList> {
    protected final UniHanManager manager;
    public Int2ObjectMap<List<CharacterUnpackingInputMethod.ExpendedChar>> dataMap = new Int2ObjectOpenHashMap<>();

    public UniHanInputMethod(UniHanManager manager) {
        this.manager = manager;
    }

    @Override
    public CompletableFuture<Void> prepare(Executor executor) {
        return dispose(executor)
                .thenRunAsync(() -> manager.download(p->{}), executor)
                .thenRunAsync(this::load, executor);
    }

    public abstract void load();

    protected List<CharacterUnpackingInputMethod.ExpendedChar> asExpendedChars(String string) {
        return List.of(new CharacterUnpackingInputMethod.ExpendedChar(CollectionUtils.map(IntList.of(string.codePoints().toArray()), IntList::of)));
    }

    @Override
    public CompletableFuture<Void> dispose(Executor executor) {
        return CompletableFuture.runAsync(dataMap::clear, executor);
    }
}