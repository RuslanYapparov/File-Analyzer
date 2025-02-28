package com.exam.fileanalyzer.service.impl;

import com.exam.fileanalyzer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * LogsAnalyzer interface implementation - bean responsible for the log file analysis.
 */
@Slf4j
@Service
public class LogsAnalyzerImpl implements LogsAnalyzer
{
		private static final int AVAILABLE_THREADS = Runtime.getRuntime().availableProcessors();
		private static final int MIN_PATHS_FOR_MULTITHREADING = 5;
		private final ZipFileManager zipFileManager;

		@Autowired
		public LogsAnalyzerImpl(ZipFileManager zipFileManager)
		{
				this.zipFileManager = zipFileManager;
		}

		/**
		 * Counts the number of occurrences of the search query in each file inside the zip file given with paramHolder.
		 *
		 * @param paramHolder object containing the search query, zip file, start date, and number of days.
		 * @return A map of file names and the number of occurrences of the search query in the file.
		 * @throws IOException if the zip file or its entries cannot be read.
		 */
		public Map<String, Integer> countEntriesInZipFile(@NonNull CountEntriesParamHolder paramHolder)
			throws IOException
		{
				List<Path> paths = zipFileManager.getPathsOfLogFilesForAnalysis(paramHolder, null);
				Map<String, Integer> result;
				if (AVAILABLE_THREADS > 2 && paths.size() >= MIN_PATHS_FOR_MULTITHREADING)
				{
					result = countEntriesInUnzippedLogFiles(paths, paramHolder.getSearchQuery());
				} else
				{
					result = countEntriesInUnzippedLogFilesWithThreads(new ConcurrentLinkedQueue<>(paths),
						paramHolder.getSearchQuery());
				}
				zipFileManager.deleteTempLogFiles(paths);
				return result;
		}

		private Map<String, Integer> countEntriesInUnzippedLogFiles(List<Path> paths, String searchQuery)
		{
				boolean withoutSearch = Objects.isNull(searchQuery);
				Map<String, Integer> result = new LinkedHashMap<>();
				paths.stream()
						.filter(Files::isRegularFile)
						.forEach(path -> result.put(path.getFileName().toString(),
								countOccurrencesInFile(path, searchQuery, withoutSearch)));
				return result;
		}

		private Map<String, Integer> countEntriesInUnzippedLogFilesWithThreads(Queue<Path> paths,
																			   String searchQuery)
		{
				boolean withoutSearch = Objects.isNull(searchQuery);
				Map<String, Integer> result = new ConcurrentSkipListMap<>();
				ExecutorService executor = Executors.newFixedThreadPool(Math.min(paths.size(), AVAILABLE_THREADS));
				List<Future<?>> tasks = new ArrayList<>();
				while (!paths.isEmpty())
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
						tasks.add(executor.submit(() -> result.put(path.getFileName().toString(),
								countOccurrencesInFile(path, searchQuery, withoutSearch))));
				}
				awaitAllThreadsAndCloseExecutor(executor, tasks);
				return result;
		}

		private int countOccurrencesInFile(Path path, String searchQuery, boolean withoutSearch)
		{
				try (Stream<String> lines = Files.lines(path))
				{
						return (int) lines.filter(line -> withoutSearch || line.contains(searchQuery)).count();
				} catch (IOException exception)
				{
						throw new IllegalStateException(String.format("Cannot read log file '%s' during analysing.",
							path.getFileName()));
				}
		}

		private void awaitAllThreadsAndCloseExecutor(ExecutorService executor, List<Future<?>> tasks)
		{
				for (Future<?> task : tasks)
				{
						try
						{
								task.get(); // Блок до завершения задачи
						} catch (InterruptedException | ExecutionException exception)
						{
								throw new IllegalStateException(String.format("MultiThread analysing error: %s (%s)",
									exception.getClass().getSimpleName(), exception.getMessage()));
						}
				}
				executor.shutdown();
				try
				{
						if (!executor.awaitTermination(7, TimeUnit.SECONDS))
						{
								executor.shutdownNow();
						}
				} catch (InterruptedException exception)
				{
						executor.shutdownNow();
						Thread.currentThread().interrupt();
				}
		}

}