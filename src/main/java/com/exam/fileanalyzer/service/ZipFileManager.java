package com.exam.fileanalyzer.service;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface of the service that can unzips necessary log files from the zip file and delete the temporary files.
 */
public interface ZipFileManager
{

        /**
         * Unzips necessary log files from the zip file.
         *
         * @param paramHolder object containing the search query, zip file, start date, and number of days.
         * @param charset (optional) charset used to read the zip file.
         * @return a list of paths to the log files.
         * @throws IOException if the zip file or its entries cannot be read.
         */
        List<Path> getPathsOfLogFilesForAnalysis(@NonNull LogsAnalyzer.CountEntriesParamHolder paramHolder,
                                                 @Nullable Charset charset) throws IOException;

        /**
         * Deletes the temporary log files and their parent directory.
         *
         * @param logFilePaths a list of paths to the log files.
         * @throws IOException if meets the problem on deleting some file.
         * @throws IllegalStateException if gets null or empty list.
         */
        void deleteTempLogFiles(List<Path> logFilePaths) throws IOException;

}