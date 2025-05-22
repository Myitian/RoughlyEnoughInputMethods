package net.myitian.roughlyenoughinputmethods.inputmethods;

import net.myitian.roughlyenoughinputmethods.UniHanManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PinyinInputMethodNew extends PinyinInputMethod {
    public PinyinInputMethodNew(UniHanManager manager) {
        super(manager);
    }

    @Override
    public CompletableFuture<Void> prepare(Executor executor, ProgressCallback progressCallback) {
        return dispose(executor)
                .thenRunAsync(() -> manager.download(p -> progressCallback.onProgress(p * 0.99)), executor)
                .thenRunAsync(this::load, executor)
                .whenComplete((aVoid, throwable) -> progressCallback.onProgress(1.0));
    }
}
