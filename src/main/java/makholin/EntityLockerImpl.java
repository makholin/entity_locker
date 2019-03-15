package makholin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class EntityLockerImpl<T> implements EntityLocker<T> {

    private final Map<T, Lock> locks = new WeakHashMap<>();
    private final Set<T> lockedIds = new HashSet<>();

    @Override
    public void lock(T entityId) {
        validate(entityId);
        getLockByEntityId(entityId).lock();
        lockedIds.add(entityId);
    }

    @Override
    public void lock(T entityId, long timeoutMs) throws TimeoutException, InterruptedException {
        validate(entityId);
        boolean locked = getLockByEntityId(entityId).tryLock(timeoutMs, MILLISECONDS);
        if (!locked) {
            throw new TimeoutException("Can't acquire lock caused by timeout expiration");
        }
        lockedIds.add(entityId);
    }

    @Override
    public void unlock(T entityId) {
        validate(entityId);
        Lock lock = locks.get(entityId);
        if (lock == null || !((ReentrantLock) lock).isHeldByCurrentThread()) {
            throw new IllegalStateException("Entity should be locked before unlocking in the same thread");
        }
        lockedIds.remove(entityId);
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
