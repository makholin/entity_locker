package makholin;

import java.util.concurrent.TimeoutException;

public interface EntityLocker<T> {

    void lock(T entityId);

    void lock(T entityId, long timeoutMs) throws TimeoutException, InterruptedException;

    void unlock(T entityId);
}

