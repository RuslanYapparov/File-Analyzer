package com.exam.fileanalyzer.in;

import com.exam.fileanalyzer.service.LogsAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.mockito.Mockito.*;
import static com.exam.fileanalyzer.service.LogsAnalyzer.CountEntriesParamHolder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { LogsAnalyzerController.class, FileAnalyzerExceptionHandler.class })
public class LogsAnalyzerControllerMockTest
{
        private static final MockMultipartFile TEST_FILE = createMockMultipartFile();
        @MockBean
        private LogsAnalyzer logAnalyzer;
        @Autowired
        private MockMvc mockMvc;

        @ParameterizedTest
        @CsvSource(value = {
                "Mozilla, 27.02.2018, 4",
                "null, 27.02.2018, 3",
                "Mozilla, null, 2",
                "Mozilla, 27.02.2018, null",
                "null, null, null"
        }, nullValues = "null")
        void countEntriesInZipFile_whenGetOrDoNotGetParameters_thenCallsServiceBeanAndReturnMapDto(
                String searchQuery, String startDate, String numberOfDays) throws Exception
        {
                String urlTemplate = "/api/analyze/logs" +
                        buildQueryParams(searchQuery, startDate, numberOfDays);
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenReturn(Map.of("firstElement", 1, "secondElement", -7));

                mockMvc.perform(multipart(urlTemplate)
                                .file(TEST_FILE)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.firstElement").value(1))
                        .andExpect(jsonPath("$.secondElement").value(-7));

                verify(logAnalyzer, Mockito.times(1))
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));

                if (Objects.nonNull(startDate)) {
                        CountEntriesParamHolder expectedParamHolder = CountEntriesParamHolder.builder()
                                .searchQuery(searchQuery)
                                .zipMultipartFile(TEST_FILE)
                                .numberOfDays(numberOfDays != null ? Integer.parseInt(numberOfDays) : null)
                                .startDate(LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                                .build();

                        verify(logAnalyzer, Mockito.times(1))
                                .countEntriesInZipFile(eq(expectedParamHolder));
                }
        }

        @ParameterizedTest
        @CsvSource(value = {
                "Сарделька, 3",
                "27.02.2018, Колбаска"
        })
        void countEntriesInZipFile_whenGetIncorrectParameters_thenDoNotCallsServiceBeanAndReturn400(
                String startDate, String numberOfDays) throws Exception
        {
                String urlTemplate = "/api/analyze/logs" +
                        buildQueryParams("Сосиска", startDate, numberOfDays);

                mockMvc.perform(multipart(urlTemplate)
                                .file(TEST_FILE)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorType")
                                .value("MethodArgumentTypeMismatchException"));

                verify(logAnalyzer, Mockito.never())
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

        @Test
        void countEntriesInZipFile_whenGetNullFile_thenDoNotCallsServiceBeanAndReturn400() throws Exception
        {
                String urlTemplate = "/api/analyze/logs";

                mockMvc.perform(multipart(urlTemplate)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorType")
                                .value("MissingServletRequestPartException"));

                verify(logAnalyzer, Mockito.never())
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

        @Test
        void countEntriesInZipFile_whenGetIncorrectParameter_thenDoNotCallsServiceBeanAndReturn404() throws Exception
        {
                String urlTemplate = "/api/analyze/logs/foo=1";

                mockMvc.perform(multipart(urlTemplate)
                                .file(TEST_FILE)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNotFound());

                verify(logAnalyzer, Mockito.never())
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

        static MockMultipartFile createMockMultipartFile()
        {
                try
                {
                        return new MockMultipartFile(
                                "file",
                                "logs-27_02_2018-03_03_2018.zip",
                                "application/zip",
                                Files.readAllBytes(Paths.get("src/test/resources/logs-27_02_2018-03_03_2018.zip"))
                        );
                } catch (IOException exception)
                {
                        throw new IllegalStateException(String.format("Cannot create file for use in tests: %s (%s)",
                                exception.getClass().getSimpleName(), exception.getMessage()));
                }
        }

        private String buildQueryParams(String searchQuery, String startDate, String numberOfDays)
        {
                StringBuilder queryParams = new StringBuilder();
                if (searchQuery != null || startDate != null || numberOfDays != null)
                {
                        queryParams.append("?");
                        if (searchQuery != null)
                        {
                                queryParams.append("text=").append(searchQuery);
                                if (startDate != null || numberOfDays != null) queryParams.append("&");
                        }
                        if (startDate != null)
                        {
                                queryParams.append("date=").append(startDate);
                                if (numberOfDays != null) queryParams.append("&");
                        }
                        if (numberOfDays != null)
                        {
                                queryParams.append("days=").append(numberOfDays);
                        }
                }
                return queryParams.toString();
        }

}