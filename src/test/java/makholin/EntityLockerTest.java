package makholin;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class EntityLockerTest {

    private final long timoutMs = 1000;

    private EntityLocker<Long> entityLocker;
    private Entity entity;

    @Before
    public void setup() {
        entityLocker = new EntityLockerImpl<>();
        entity = new Entity(1);
    }

    @Test
    public void testExecutionProtectedCodeInSingleThread() {
        long originalValue = entity.getCounter();

        entityLocker.lock(entity.getId());
        try {
            entity.increment();
        } finally {
            entityLocker.unlock(entity.getId());
        }

        assertEquals("Increment should be completed successfully", originalValue + 1, entity.getCounter());
    }

    @Test
    public void testExecutionProtectedCodeInMultithreadingEnvironment() throws InterruptedException {
        long originalCounter = entity.getCounter();
        int threadsAmount = 1000;

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadsAmount; ++i) {
            Thread thread = new Thread(() -> {
                entityLocker.lock(entity.getId());
                try {
                    entity.increment();
                } finally {

                    entityLocker.unlock(entity.getId());

                }
            });
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals("Entity's counter should be incremented in each thread", originalCounter + threadsAmount, entity.getCounter());
    }

    @Test()
    public void testFailedLockByTimeout() throws InterruptedException {
        long originalCounter = entity.getCounter();
        long insufficientTimeoutMs = 1;
        List<Thread> threads = new ArrayList<>();

        Thread firstThread = new Thread(() -> {
            entityLocker.lock(entity.getId());
            try {
                Thread.sleep(timoutMs);
                entity.increment();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                entityLocker.unlock(entity.getId());
            }
        });
        firstThread.start();
        threads.add(firstThread);

        Thread secondThread = new Thread(() -> {
            try {
                entityLocker.lock(entity.getId(), insufficientTimeoutMs);
                try {
                    entity.increment();
                } finally {
                    entityLocker.unlock(entity.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        secondThread.start();
        threads.add(secondThread);

        for (Thread t : threads) {
            t.join();
        }

        assertEquals("Only one thread should increment counter, second is stopped by TimeoutException",
                originalCounter + 1, entity.getCounter());
    }

    @Test()
    public void testSuccessfulLockByTimeout() throws InterruptedException {
        long originalCounter = entity.getCounter();
        long sufficientTimeoutMs = timoutMs * 2;
        List<Thread> threads = new ArrayList<>();

        Thread firstThread = new Thread(() -> {
            entityLocker.lock(entity.getId());
            try {
                Thread.sleep(timoutMs);
                entity.increment();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                entityLocker.unlock(entity.getId());
            }
        });
        firstThread.start();
        threads.add(firstThread);

        Thread secondThread = new Thread(() -> {
            try {

                entityLocker.lock(entity.getId(), sufficientTimeoutMs);
                try {
                    entity.increment();
                } finally {
                    entityLocker.unlock(entity.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        secondThread.start();
        threads.add(secondThread);

        for (Thread t : threads) {
            t.join();
        }

        assertEquals("Both threads should increment counter", originalCounter + 2, entity.getCounter());
    }

    @Test()
    public void testUnlockWhenOtherThreadHasLock() throws InterruptedException {
        List<Exception> thrownExceptions = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        Thread firstThread = new Thread(() -> {
            entityLocker.lock(entity.getId());
            try {
                Thread.sleep(timoutMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                entityLocker.unlock(entity.getId());
            }
        });
        firstThread.start();
        threads.add(firstThread);

        Thread secondThread = new Thread(() -> {
            try {
                entityLocker.unlock(entity.getId());
            } catch (Exception e) {
                thrownExceptions.add(e);
            }
        });
        secondThread.start();
        threads.add(secondThread);

        for (Thread t : threads) {
            t.join();
        }

        assertEquals("Exception should be thrown in second thread", 1, thrownExceptions.size());
        assertEquals("Exception should be instance of IllegalStateException", IllegalStateException.class, thrownExceptions.get(0).getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void testUnlockWithoutLockInSameThread() {
        entityLocker.unlock(entity.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLockOnNullableId() {
        entityLocker.lock(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLockWithTimeoutOnNullableId() throws TimeoutException, InterruptedException {
        entityLocker.lock(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnlockOnNullableId() {
        entityLocker.unlock(null);
    }
}
