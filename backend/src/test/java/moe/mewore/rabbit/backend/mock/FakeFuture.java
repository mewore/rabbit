package moe.mewore.rabbit.backend.mock;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FakeFuture<T> implements Future<T> {

    private final T result;

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() {
        return result;
    }

    @Override
    public T get(final long timeout, @NotNull final TimeUnit unit) {
        return result;
    }
}
