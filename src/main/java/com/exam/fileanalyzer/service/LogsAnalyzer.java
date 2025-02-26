package com.exam.fileanalyzer.service;

import lombok.*;

import java.io.*;
import java.time.LocalDate;
import java.util.Map;

/**
 * Interface of the log analysing service.
 */
public interface LogsAnalyzer
{
        /**
         * Counts the number of occurrences of the search query in each file inside the zip file given with paramHolder.
         *
         * @param paramHolder object containing the search query, zip file, start date, and number of days.
         * @return A map of file names and the number of occurrences of the search query in the file.
         * @throws IOException if the zip file or its entries cannot be read.
         */
        Map<String, Integer> countEntriesInZipFile(CountEntriesParamHolder paramHolder) throws IOException;

        /**
         * An object containing data used in the method of calculating the number of lines in the log files.
         */
        @Value
        @Builder
        class CountEntriesParamHolder
        {
                /** The string to search for in the file. */
                String searchQuery;
                /** The zip file to search in. */
                File zipFile;
                /** The start date of the search. */
                LocalDate startDate;
                /** The number of days to search for from the start date. */
                Integer numberOfDays;
        }

}