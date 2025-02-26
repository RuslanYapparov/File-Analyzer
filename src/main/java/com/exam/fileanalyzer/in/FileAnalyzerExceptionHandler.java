package com.exam.fileanalyzer.in;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * The handler of exceptions thrown during the FileAnalyzer-microservice work.
 */
@RestControllerAdvice
public class FileAnalyzerExceptionHandler
{

        /**
        * DTO with minimal information about the exception to be returned from the service.
        */
        @Value
        private static class ExceptionDto
        {
                /** A simple name of the exception class. */
                String errorType;
                /** A message of the exception. */
                String errorMessage;
                /** Date and time of the exception handling. */
                @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
                LocalDateTime errorTime;
        }

}