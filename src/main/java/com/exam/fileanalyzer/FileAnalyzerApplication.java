package com.exam.fileanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class for the log analysis microservice starting.
 */
@SpringBootApplication
public class FileAnalyzerApplication
{
		/**
	 	* Runs the server for processing log analysis queries.
		*
	 	* @param args command line arguments.
	 	*/
		public static void main(String[] args)
		{
				SpringApplication.run(FileAnalyzerApplication.class, args);
		}

}