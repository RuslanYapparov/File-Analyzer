package com.exam.fileanalyzer.in;

import com.exam.fileanalyzer.service.LogsAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import static com.exam.fileanalyzer.service.LogsAnalyzer.CountEntriesParamHolder;

/**
 * Controller for processing REST requests of the log analysis.
 */
@Slf4j
@RestController
@RequestMapping("/api/analyze/logs")
public class LogsAnalyzerController
{
        /** Service bean to proceed the log analysis. */
        private final LogsAnalyzer logsAnalyzer;

        /**
         * LogsAnalyzerController's constructor with spring bean injection.
         *
         * @param logsAnalyzer service bean to proceed the log analysis.
         */
        @Autowired
        public LogsAnalyzerController(LogsAnalyzer logsAnalyzer)
        {
                this.logsAnalyzer = logsAnalyzer;
        }

        /**
         * The controller's method representing POST endpoint for the log analysis request.
         *
         * @param zipFile MultipartFile-object representing the zip file.
         * @param searchQuery text to be searched in the entries of the zip file.
         * @param startDate date to filter the entries of the zip file by date.
         * @param numberOfDays another parameter to filter the entries of the zip file by date.
         * @return map with the result of the log analysis.
         * @throws IOException if I/O problem was occurred during the log analysis.
         */
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Integer> countEntriesInZipFile(
                @RequestParam("file") MultipartFile zipFile,
                @RequestParam(name = "text", required = false) String searchQuery,
                @RequestParam(name = "date", required = false) LocalDate startDate,
                @RequestParam(name = "days", required = false) Integer numberOfDays)
                throws IOException
        {
                log.info("Received new http request for the log files analysis with parameters: " +
                        "searchQuery = {}, startDate = {}, numberOfDays = {}", searchQuery, startDate, numberOfDays);
                CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                        .searchQuery(searchQuery)
                        .zipMultipartFile(zipFile)
                        .startDate(startDate)
                        .numberOfDays(numberOfDays)
                        .build();
                Map<String, Integer> result = logsAnalyzer.countEntriesInZipFile(paramHolder);
                log.info("Http request processed successfully. Sending result map with {} entries", result.size());
                return result;
        }

}