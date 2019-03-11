package makholin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class EntityLockerImpl<T> implements EntityLocker<T> {

    private final Map<T, Lock> locks = new HashMap<>();

    @Override
    public void lock(T entityId) {
        validate(entityId);
        getLockByEntityId(entityId).lock();
    }

    @Override
    public void lock(T entityId, long timeoutMs) throws TimeoutException, InterruptedException {
        validate(entityId);
        boolean locked = getLockByEntityId(entityId).tryLock(timeoutMs, MILLISECONDS);
        if (!locked) {
            throw new TimeoutException("Can't acquire lock caused by timeout expiration");
        }
    }

    @Override
    public void unlock(T entityId) {
        validate(entityId);
        Lock lock = locks.get(entityId);
        if (lock == null || !((ReentrantLock) lock).isHeldByCurrentThread()) {
            throw new IllegalStateException("Entity should be locked before unlocking in the same thread");
        }
        lock.unlock();
    }

    private synchronized Lock getLockByEntityId(T entityId) {
        Lock lock = locks.get(entityId);
        if (lock != null) {
            return lock;
        }
        lock = new ReentrantLock(true);
        locks.put(entityId, lock);
        return lock;
    }

    private void validate(T entityId) {
        if (entityId == null) {
            throw new IllegalArgumentException("Entity id is null");
        }
    }
}
