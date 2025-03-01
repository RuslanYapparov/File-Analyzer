package com.exam.fileanalyzer.service.impl;

import com.exam.fileanalyzer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * LogsAnalyzer interface implementation - bean responsible for the single-thread only log file analysis.
 */
@Slf4j
@Service
public class SingleThreadLogsAnalyzerImpl implements LogsAnalyzer
{
        /** Bean responsible for managing zip files. */
        protected final ZipFileManager zipFileManager;

        /**
         * Constructor with bean injection.
         *
         * @param zipFileManager bean responsible for managing zip files.
         */
        @Autowired
        public SingleThreadLogsAnalyzerImpl(ZipFileManager zipFileManager) {
                this.zipFileManager = zipFileManager;
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
                Map<String, Integer> result = countEntriesInUnzippedLogFiles(paths, paramHolder.getSearchQuery());
                zipFileManager.deleteTempLogFiles(paths);
                return result;
        }

        /**
         * Starts the single-thread log file processing.
         *
         * @param paths the paths of the log files in list.
         * @param searchQuery the search query to count lines.
         * @return the map of file names and the number of occurrences of the search query in lines of the file.
         */
        protected Map<String, Integer> countEntriesInUnzippedLogFiles(List<Path> paths, String searchQuery)
        {
                Map<String, Integer> result = new LinkedHashMap<>();
                paths.stream()
                        .filter(Files::isRegularFile)
                        .forEach(path -> result.put(path.getFileName().toString(),
                                countOccurrencesInFile(path, searchQuery)));
                return result;
        }

        /**
         * Counts the number of occurrences of the search query in the given log file.
         *
         * @param path the path of the log file.
         * @param searchQuery the search query to count lines in the log file.
         * @return the number of occurrences of the search query in lines of the log file or the number of lines
         * if the search query is null.
         */
        protected int countOccurrencesInFile(Path path, String searchQuery)
        {
                boolean withoutSearch = Objects.isNull(searchQuery);
                try (Stream<String> lines = Files.lines(path))
                {
                        return (int) lines.filter(line -> withoutSearch || line.contains(searchQuery)).count();
                } catch (IOException exception)
                {
                        throw new IllegalStateException(String.format("Cannot read log file '%s' during analysing.",
                                path.getFileName()));
                }
        }

}