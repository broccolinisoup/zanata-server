package org.zanata.limits;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Test(groups = "unit-tests")
@Slf4j
public class RestCallLimiterTest {
    private RestCallLimiter limiter;
    private int maxConcurrent = 4;
    private int maxActive = 2;
    private Logger testLogger = LogManager.getLogger(getClass());
    private Logger testeeLogger = LogManager.getLogger(RestCallLimiter.class);

    @Mock
    private Runnable runnable;
    private CountBlockingSemaphore countBlockingSemaphore;

    @BeforeClass
    public void beforeClass() {
        // set logging to debug
//        testeeLogger.setLevel(Level.DEBUG);
//        testLogger.setLevel(Level.DEBUG);
    }

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
        limiter = new RestCallLimiter(maxConcurrent, maxActive);
    }

    @Test
    public void canOnlyHaveMaximumNumberOfConcurrentRequest()
            throws InterruptedException, ExecutionException {
        // we don't limit active requests
        limiter = new RestCallLimiter(maxConcurrent, maxConcurrent);

        // to ensure threads are actually running concurrently
        runnableWillTakeTime(20);

        Callable<Boolean> task = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return limiter.tryAcquireAndRun(runnable);
            }
        };
        int numOfThreads = maxConcurrent + 1;
        List<Boolean> result =
                submitConcurrentTasksAndGetResult(task, numOfThreads);
        log.debug("result: {}", result);
        // requests that are within the max concurrent limit should get permit
        Iterable<Boolean> successRequest =
                Iterables.filter(result, new Predicate<Boolean>() {
                    @Override
                    public boolean apply(Boolean input) {
                        return input;
                    }
                });
        assertThat(successRequest,
                Matchers.<Boolean>iterableWithSize(maxConcurrent));
        // last request which exceeds the limit will fail to get permit
        assertThat(result, Matchers.hasItem(false));
    }

    static <T> List<T> submitConcurrentTasksAndGetResult(Callable<T> task,
            int numOfThreads) throws InterruptedException, ExecutionException {
        List<Callable<T>> tasks = Collections.nCopies(numOfThreads, task);

        ExecutorService service = Executors.newFixedThreadPool(numOfThreads);

        List<Future<T>> futures = service.invokeAll(tasks);
        return Lists.transform(futures, new Function<Future<T>, T>() {
            @Override
            public T apply(Future<T> input) {
                try {
                    return input.get();
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        });
    }

    @Test
    public void canOnlyHaveMaxActiveConcurrentRequest()
            throws InterruptedException, ExecutionException {
        countBlockingSemaphore = new CountBlockingSemaphore(maxActive);
        limiter =
                new RestCallLimiter(maxConcurrent, maxActive)
                        .changeActiveSemaphore(countBlockingSemaphore);

        // Given: each thread will take some time to do its job
        final int timeSpentDoingWork = 20;
        runnableWillTakeTime(timeSpentDoingWork);

        // When: max concurrent threads are accessing simultaneously
        Callable<Boolean> callable = new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                return limiter.tryAcquireAndRun(runnable);
            }
        };

        // Then: only max active threads will be served immediately while others
        // will block until them finish
        List<Boolean> requests =
                submitConcurrentTasksAndGetResult(callable, maxConcurrent);
        log.debug("result: {}", requests);

        assertThat(countBlockingSemaphore.numOfBlockedThreads(),
                Matchers.equalTo(maxConcurrent - maxActive));
    }

    void runnableWillTakeTime(final int timeSpentDoingWork) {
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Uninterruptibles.sleepUninterruptibly(timeSpentDoingWork,
                        TimeUnit.MILLISECONDS);
                return null;
            }
        }).when(runnable).run();
    }

    @Test
    public void changeMaxConcurrentLimitWillTakeEffectImmediately()
            throws ExecutionException, InterruptedException {
        runnableWillTakeTime(10);

        // we start off with only 1 concurrent permit
        limiter = new RestCallLimiter(1, 10);
        Callable<Boolean> task = new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                return limiter.tryAcquireAndRun(runnable);
            }
        };

        int numOfThreads = 2;
        List<Boolean> result =
                submitConcurrentTasksAndGetResult(task, numOfThreads);
        assertThat(result, Matchers.containsInAnyOrder(true, false));
        assertThat(limiter.availableConcurrentPermit(), Matchers.is(1));

        // change permit to match number of threads
        limiter.setMaxConcurrent(numOfThreads);

        List<Boolean> resultAfterChange =
                submitConcurrentTasksAndGetResult(task, numOfThreads);
        assertThat(resultAfterChange, Matchers.contains(true, true));

        assertThat(limiter.availableConcurrentPermit(),
                Matchers.is(numOfThreads));
    }

    @Test
    public void changeMaxActiveLimitWhenNoBlockedThreads() {
        limiter = new RestCallLimiter(3, 3);
        limiter.tryAcquireAndRun(runnable);

        limiter.setMaxActive(2);
        // change won't happen until next request comes in
        limiter.tryAcquireAndRun(runnable);
        assertThat(limiter.availableActivePermit(), Matchers.is(2));

        limiter.setMaxActive(1);

        limiter.tryAcquireAndRun(runnable);
        assertThat(limiter.availableActivePermit(), Matchers.is(1));
    }

    @Test
    public void changeMaxActiveLimitWhenHasBlockedThreads()
            throws InterruptedException {
        // Given: only 2 active requests allowed
        limiter = new RestCallLimiter(10, 2);

        // When: below requests are fired simultaneously
        // 3 requests (each takes 20ms) and 1 request should block
        final int timeSpentDoingWork = 20;
        runnableWillTakeTime(timeSpentDoingWork);
        Callable<Long> callable = taskToAcquireAndMeasureTime();
        List<Callable<Long>> requests = Collections.nCopies(3, callable);
        // 1 task to update the active permit with 5ms delay
        // (so that it will happen while there is a blocked request)
        Callable<Long> changeTask = new Callable<Long>() {

            @Override
            public Long call() throws Exception {
                // to ensure it happens when there is a blocked request
                Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS);
                limiter.setMaxActive(3);
                return -10L;
            }
        };
        // 2 delayed request that will try to acquire after the change
        // (while there is still a request blocking)
        Callable<Long> delayRequest = new Callable<Long>() {

            @Override
            public Long call() throws Exception {
                // ensure this happen after change limit took place
                Uninterruptibles
                        .sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                return tryAcquireAndMeasureTime();
            }
        };
        List<Callable<Long>> delayedRequests =
                Collections.nCopies(2, delayRequest);
        List<Callable<Long>> allTasks = Lists.newArrayList(requests);
        allTasks.add(changeTask);
        allTasks.addAll(delayedRequests);
        ExecutorService executorService =
                Executors.newFixedThreadPool(allTasks.size());
        List<Future<Long>> futures = executorService.invokeAll(allTasks);

        // Then: at the beginning 1 request should be blocked meanwhile change
        // active limit will happen
        // the update request will change the semaphore so new requests will be
        // operating on new semaphore object
        List<Long> timeUsedInMillis =
                getTimeUsedInMillisRoundedUpToTens(futures);

        log.info("result: {}", timeUsedInMillis);
        // initial blocked thread's release will operate on old semaphore which
        // was thrown away
        assertThat(limiter.availableActivePermit(), Matchers.is(3));
    }

    @Test
    public void willReleaseSemaphoreWhenThereIsException() throws IOException,
            ServletException {
        doThrow(new RuntimeException("bad")).when(runnable).run();

        try {
            limiter.tryAcquireAndRun(runnable);
        } catch (Exception e) {
            // I know
        }

        assertThat(limiter.availableConcurrentPermit(),
                Matchers.equalTo(maxConcurrent));
        assertThat(limiter.availableActivePermit(), Matchers.equalTo(maxActive));
    }

    @Test
    public void zeroPermitMeansNoLimit() {
        limiter = new RestCallLimiter(0, 0);

        assertThat(limiter.tryAcquireAndRun(runnable), Matchers.is(true));
        assertThat(limiter.tryAcquireAndRun(runnable), Matchers.is(true));
        assertThat(limiter.tryAcquireAndRun(runnable), Matchers.is(true));
    }

    /**
     * it will measure acquire blocking time and return it.
     */
    private Callable<Long> taskToAcquireAndMeasureTime() {
        return new Callable<Long>() {

            @Override
            public Long call() throws Exception {
                return tryAcquireAndMeasureTime();
            }
        };
    }

    private long tryAcquireAndMeasureTime() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        limiter.tryAcquireAndRun(runnable);
        stopwatch.stop();
        long timeSpent = stopwatch.elapsedMillis();
        log.debug("real time try acquire and run task takes: {}", timeSpent);
        return roundToTens(timeSpent);
    }

    private static List<Long> getTimeUsedInMillisRoundedUpToTens(
            List<Future<Long>> futures) {
        return Lists.transform(futures, new Function<Future<Long>, Long>() {
            @Override
            public Long apply(Future<Long> input) {
                try {
                    return roundToTens(input.get());
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        });
    }

    private static long roundToTens(long arg) {
        return arg / 10 * 10;
    }

    private static class CountBlockingSemaphore extends Semaphore {
        private static final long serialVersionUID = 1L;
        private AtomicInteger blockCounter = new AtomicInteger(0);

        public CountBlockingSemaphore(Integer permits) {
            super(permits);
        }

        @Override
        public boolean tryAcquire(long timeout, TimeUnit unit)
                throws InterruptedException {
            boolean got = tryAcquire();
            if (!got) {
                blockCounter.incrementAndGet();
            }
            // we don't care the result
            return true;
        }

        public int numOfBlockedThreads() {
            return blockCounter.get();
        }
    }
}
