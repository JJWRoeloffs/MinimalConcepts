package edu.vuamsterdam.MinimalConcepts;

import java.util.concurrent.*;

public class TimeoutHelper {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static <T> T runWithTimeout(Callable<T> task, long timeoutSeconds) throws Exception {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Task timed out after " + timeoutSeconds + " seconds");
        }
    }
}