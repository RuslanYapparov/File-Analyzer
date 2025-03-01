package com.exam.fileanalyzer.in;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * The handler of exceptions thrown during the FileAnalyzer-microservice work.
 */
@Slf4j
@RestControllerAdvice
public class FileAnalyzerExceptionHandler
{
        /**
         * Handles exceptions returning 400 Bad Request response.
         *
         * @param exception  exception to be handled.
         * @return exception DTO.
         */
        @ExceptionHandler(value = { IllegalArgumentException.class,
                MethodArgumentTypeMismatchException.class,
                MissingServletRequestPartException.class,
                FileSizeLimitExceededException.class,
                MaxUploadSizeExceededException.class
        })
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ExceptionDto handleBadRequestException(Exception exception)
        {
                return handleException(exception);
        }

        /**
         * Handles exceptions returning 500 Internal Server Error response.
         *
         * @param exception  exception to be handled.
         * @return exception DTO.
         */
        @ExceptionHandler(value = { IOException.class, RuntimeException.class })
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ExceptionDto handleInternalServerErrorException(Exception exception)
        {
                return handleException(exception);
        }

        /**
         * Log the exception and return exception DTO.
         *
         * @param exception exception to be handled.
         * @return exception DTO.
         */
        private ExceptionDto handleException(Exception exception)
        {
                log.error("Exception occurred: {} ({})", exception.getClass().getSimpleName(), exception.getMessage());
                return new ExceptionDto(
                        exception.getClass().getSimpleName(),
                        exception.getMessage(),
                        LocalDateTime.now()
                );
        }

        /**
        * DTO with minimal information about the exception to be returned from the service.
        */
        @Value
        public static class ExceptionDto
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