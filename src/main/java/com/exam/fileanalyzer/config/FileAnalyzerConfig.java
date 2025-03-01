package com.exam.fileanalyzer.config;

import com.exam.fileanalyzer.service.*;
import com.exam.fileanalyzer.service.impl.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

/**
 * The Spring bean for some another bean's configured creating.
 */
@Configuration
public class FileAnalyzerConfig {

        /**
         * Configuration method for the {@link LogsAnalyzer} interface implementation
         * (looks up for the available number of processors on the current machine).
         *
         * @param zipFileManager            bean responsible for managing zip files.
         * @param minPathsForMultiThreading the minimum number of log files to use multithreading.
         * @return the configured bean for the {@link LogsAnalyzer} interface implementation.
         */
        @Bean
        public LogsAnalyzer logsAnalyzer(ZipFileManager zipFileManager,
                                         @Value("${min.paths.for.multithreading}") int minPathsForMultiThreading) {
                return (Runtime.getRuntime().availableProcessors() > 2) ?
                        new MultiThreadLogsAnalyzerImpl(zipFileManager, minPathsForMultiThreading) :
                        new SingleThreadLogsAnalyzerImpl(zipFileManager);
        }

}