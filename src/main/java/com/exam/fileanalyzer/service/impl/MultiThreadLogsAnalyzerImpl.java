package com.exam.fileanalyzer.service.impl;

import com.exam.fileanalyzer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * LogsAnalyzer interface implementation - bean responsible for the single- and multi-thread log file analysis.
 */
@Slf4j
@Service
public class MultiThreadLogsAnalyzerImpl extends SingleThreadLogsAnalyzerImpl implements LogsAnalyzer
{
        /** The number of available threads for processing the logs. */
        private static final int AVAILABLE_THREADS = Runtime.getRuntime().availableProcessors();
        /** The minimum number of paths to use multithreading. */
        private final int minPathsForMultiThreading;

        /**
         * Constructor with property value and bean injection.
         *
         * @param zipFileManager bean responsible for managing zip files.
         * @param minPathsForMultiThreading the minimum number of paths to use multithreading.
         */
        @Autowired
        public MultiThreadLogsAnalyzerImpl(ZipFileManager zipFileManager,
                                           @Value("${min.paths.for.multithreading}") int minPathsForMultiThreading)
	{
                super(zipFileManager);
                this.minPathsForMultiThreading = minPathsForMultiThreading;
        }

        /**
         * Counts the number of occurrences of the search query in each file inside the zip file given with paramHolder.
         *
         * @param paramHolder object containing the search query, zip file, start date, and number of days.
         * @return A map of file names and the number of occurrences of the search query in the file.
         * @throws IOException if the zip file or its entries cannot be read.
         */
        @Override
        public Map<String, Integer> countEntriesInZipFile(@NonNull CountEntriesParamHolder paramHolder)
                throws IOException
	{
                List<Path> paths = zipFileManager.getPathsOfLogFilesForAnalysis(paramHolder, null);
                Map<String, Integer> result;
                if (AVAILABLE_THREADS > 2 && paths.size() >= minPathsForMultiThreading)
		{
                        result = countEntriesInUnzippedLogFiles(new ConcurrentLinkedQueue<>(paths),
                                paramHolder.getSearchQuery());
                } else
		{
                        result = countEntriesInUnzippedLogFiles(paths, paramHolder.getSearchQuery());
                }
                zipFileManager.deleteTempLogFiles(paths);
                return result;
        }

	/**
	 * Starts the multi-thread file processing.
	 *
	 * @param paths the paths of the log files in blocking queue.
	 * @param searchQuery the search query to count lines.
	 * @return the map of file names and the number of occurrences of the search query in lines of the file.
	 */
        private Map<String, Integer> countEntriesInUnzippedLogFiles(Queue<Path> paths, String searchQuery)
	{
                Map<String, Integer> result = new ConcurrentSkipListMap<>();
                Runnable threadTask = createThreadTask(paths, searchQuery, result);
                int numberOfThreads = Math.min(paths.size(), AVAILABLE_THREADS);
                ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

                for (int i = 0; i < numberOfThreads; i++)
		{
                        executor.execute(threadTask);
                }
                shutdownExecutorService(executor);
                return result;
        }

	/**
	 * Creates the task for each thread to count the occurrences of the search query in each log file.
	 *
	 * @param paths the paths of the log files in blocking queue.
	 * @param searchQuery the search query to count lines.
	 * @param result the map of file names and the number of occurrences of the search query in lines of the file.
	 * @return the multithreading task.
	 */
        private Runnable createThreadTask(Queue<Path> paths, String searchQuery, Map<String, Integer> result)
	{
                return () ->
		{
                        while (true)
			{
                                Path path = paths.poll();
                                if (path == null)
				{
                                        break;
                                }
                                if (Files.isDirectory(path))
				{
                                        continue;
                                }
                                try
				{
                                        int occurrences = countOccurrencesInFile(path, searchQuery);
                                        result.put(path.getFileName().toString(), occurrences);
                                } catch (Exception exception)
				{
                                        log.warn("Exception was occurred during multithreaded file processing: " +
                                                        "{} ({}). Thread {} is going to be interrupted.",
                                                exception.getClass().getSimpleName(), exception.getMessage(),
                                                Thread.currentThread().getName());
                                        Thread.currentThread().interrupt();
                                        break;
                                }
                        }
                };
        }

	/**
	 * Shuts down the executor service with timeout for threads termination.
	 *
	 * @param executor the executor service to shut down.
	 */
        private void shutdownExecutorService(ExecutorService executor)
	{
                executor.shutdown();
                try
		{
                        if (!executor.awaitTermination(3, TimeUnit.SECONDS))
			{
                                executor.shutdownNow();
                        }
                } catch (InterruptedException e)
		{
                        Thread.currentThread().interrupt();
                }
        }

}