package com.exam.fileanalyzer.service.impl;

import com.exam.fileanalyzer.service.LogsAnalyzer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * LogsAnalyzer interface implementation - bean responsible for the log file analysis.
 */
@Service
public class LogsAnalyzerImpl implements LogsAnalyzer
{
		/**
		 * Counts the number of occurrences of the search query in each file inside the zip file given with paramHolder.
		 *
		 * @param paramHolder object containing the search query, zip file, start date, and number of days.
		 * @return A map of file names and the number of occurrences of the search query in the file.
		 * @throws IOException if the zip file or its entries cannot be read.
		 */
		public Map<String, Integer> countEntriesInZipFile(LogsAnalyzer.CountEntriesParamHolder paramHolder)
			throws IOException
		{
				throw new UnsupportedEncodingException("Need to implement!");
		}

}