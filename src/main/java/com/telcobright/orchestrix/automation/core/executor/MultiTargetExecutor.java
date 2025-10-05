package com.telcobright.orchestrix.automation.core.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Generic executor for running tasks against multiple targets
 * Supports both parallel and sequential execution modes
 *
 * @param <T> Type of result returned by tasks
 */
public class MultiTargetExecutor<T> {
    private static final Logger logger = Logger.getLogger(MultiTargetExecutor.class.getName());

    private final ExecutionMode mode;
    private final int threadPoolSize;

    /**
     * Create executor with specified mode
     *
     * @param mode Execution mode (PARALLEL or SEQUENTIAL)
     */
    public MultiTargetExecutor(ExecutionMode mode) {
        this(mode, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create executor with specified mode and thread pool size
     *
     * @param mode Execution mode
     * @param threadPoolSize Number of threads for parallel execution
     */
    public MultiTargetExecutor(ExecutionMode mode, int threadPoolSize) {
        this.mode = mode;
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Execute tasks against multiple targets
     *
     * @param targetNames Names of targets
     * @param tasks Tasks to execute (one per target)
     * @return List of results (one per task)
     */
    public List<TaskResult<T>> executeTasks(List<String> targetNames, List<Task<T>> tasks) {
        if (targetNames.size() != tasks.size()) {
            throw new IllegalArgumentException("Number of targets must match number of tasks");
        }

        logger.info(String.format("Executing %d tasks in %s mode", tasks.size(), mode));

        if (mode == ExecutionMode.PARALLEL) {
            return executeParallel(targetNames, tasks);
        } else {
            return executeSequential(targetNames, tasks);
        }
    }

    /**
     * Execute tasks sequentially
     */
    private List<TaskResult<T>> executeSequential(List<String> targetNames, List<Task<T>> tasks) {
        List<TaskResult<T>> results = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            String targetName = targetNames.get(i);
            Task<T> task = tasks.get(i);

            logger.info(String.format("[%d/%d] Processing target: %s", i + 1, tasks.size(), targetName));

            TaskResult<T> result = executeTask(targetName, task);
            results.add(result);

            if (!result.isSuccess()) {
                logger.warning(String.format("Task failed for target '%s': %s",
                    targetName, result.getError().getMessage()));
            }
        }

        return results;
    }

    /**
     * Execute tasks in parallel using thread pool
     */
    private List<TaskResult<T>> executeParallel(List<String> targetNames, List<Task<T>> tasks) {
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<TaskResult<T>>> futures = new ArrayList<>();

        // Submit all tasks
        for (int i = 0; i < tasks.size(); i++) {
            final String targetName = targetNames.get(i);
            final Task<T> task = tasks.get(i);
            final int taskIndex = i + 1;
            final int totalTasks = tasks.size();

            Future<TaskResult<T>> future = executorService.submit(() -> {
                logger.info(String.format("[%d/%d] Processing target: %s",
                    taskIndex, totalTasks, targetName));
                return executeTask(targetName, task);
            });

            futures.add(future);
        }

        // Collect results
        List<TaskResult<T>> results = new ArrayList<>();
        for (Future<TaskResult<T>> future : futures) {
            try {
                TaskResult<T> result = future.get();
                results.add(result);

                if (!result.isSuccess()) {
                    logger.warning(String.format("Task failed for target '%s': %s",
                        result.getTargetName(), result.getError().getMessage()));
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.severe("Error collecting task result: " + e.getMessage());
                results.add(TaskResult.failure("unknown", new Exception(e), 0));
            }
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        return results;
    }

    /**
     * Execute a single task and measure time
     */
    private TaskResult<T> executeTask(String targetName, Task<T> task) {
        long startTime = System.currentTimeMillis();

        try {
            T result = task.execute();
            long executionTime = System.currentTimeMillis() - startTime;
            return TaskResult.success(targetName, result, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return TaskResult.failure(targetName, e, executionTime);
        }
    }

    /**
     * Print summary of results
     *
     * @param results Task results
     * @return true if all tasks succeeded
     */
    public boolean printSummary(List<TaskResult<T>> results) {
        int successCount = 0;
        int failureCount = 0;
        long totalTime = 0;

        logger.info("\n========================================");
        logger.info("Execution Summary");
        logger.info("========================================");

        for (TaskResult<T> result : results) {
            if (result.isSuccess()) {
                successCount++;
                logger.info(String.format("✓ %s - SUCCESS (%dms)",
                    result.getTargetName(), result.getExecutionTimeMs()));
            } else {
                failureCount++;
                logger.severe(String.format("✗ %s - FAILED (%dms): %s",
                    result.getTargetName(), result.getExecutionTimeMs(),
                    result.getError().getMessage()));
            }
            totalTime += result.getExecutionTimeMs();
        }

        logger.info("========================================");
        logger.info(String.format("Total: %d | Success: %d | Failed: %d",
            results.size(), successCount, failureCount));
        logger.info(String.format("Total time: %dms | Avg time: %dms",
            totalTime, results.isEmpty() ? 0 : totalTime / results.size()));
        logger.info("========================================\n");

        return failureCount == 0;
    }
}
