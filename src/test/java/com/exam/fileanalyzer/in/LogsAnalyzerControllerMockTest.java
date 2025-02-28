package com.exam.fileanalyzer.in;

import com.exam.fileanalyzer.service.LogsAnalyzer;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.Mockito.*;
import static com.exam.fileanalyzer.service.LogsAnalyzer.CountEntriesParamHolder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogsAnalyzerController.class)
public class LogsAnalyzerControllerMockTest
{
        private static final byte[] realFileByteArray = createRealFileByteArray();
        private final MockMvc mockMvc;

        @MockBean
        private LogsAnalyzer logAnalyzer;

        @Autowired
        public LogsAnalyzerControllerMockTest(MockMvc mockMvc)
        {
                this.mockMvc = mockMvc;
        }

        static byte[] createRealFileByteArray()
        {
                Path realFilePath = Paths.get("/src/test/resources/logs-27_02_2018-03_03_2018.zip");
                try
                {
                        return Files.readAllBytes(realFilePath);
                } catch (IOException exception)
                {
                        throw new IllegalStateException("Tests cannot be run because program cannot read real file. " +
                                "Is file 'logs-27_02_2018-03_03_2018.zip' exists in 'src/test/resources' directory?");
                }
        }

        @ParameterizedTest
        @CsvSource(value = { "Mozilla, 27-02-2018, 4",
                "null, 27-02-2018, 3",
                "Mozilla, null, 2",
                "Mozilla, 27-02-2018, null",
                "null, null, null" }, nullValues = "null")
        void countEntriesInZipFile_whenGetOrDoNotGetParameters_thenCallsServiceBeanAndReturnMapDto(
                String searchQuery, String startDate, String numberOfDays) throws Exception
        {
                String urlTemplate = "/api/analyze/logs" +
                        makeParameterizedUrlEnding(searchQuery, startDate, numberOfDays);
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenReturn(Map.of("firstElement", 1, "secondElement", -7));

                mockMvc.perform(post(urlTemplate)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                .content(realFileByteArray)
                                .characterEncoding(StandardCharsets.UTF_8))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.firstElement").value(1))
                        .andExpect(jsonPath("$.secondElement").value(-7));

                verify(logAnalyzer, Mockito.times(1))
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
                if (Objects.nonNull(startDate))
                {
                        CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                                .searchQuery(searchQuery)
                                .zipMultipartFile(new MockMultipartFile("MockFile",
                                        new FileInputStream("src/test/resources/logs-27_02_2018-03_03_2018.zip")))
                                .numberOfDays(Integer.parseInt(numberOfDays))
                                .startDate(LocalDate.now())
                                .build();

                        verify(logAnalyzer, Mockito.times(1))
                                .countEntriesInZipFile(paramHolder);
                }
        }

        @ParameterizedTest
        @CsvSource(value = { "Сарделька, 3",
                "27-02-2018, Колбаска" })
        void countEntriesInZipFile_whenGetIncorrectParameters_thenDoNotCallsServiceBeanAndReturn400(
                String startDate, String numberOfDays) throws Exception
        {
                boolean isFirstTest = "Сарделька".equals(startDate);
                String urlTemplate = "/api/analyze/logs" +
                        makeParameterizedUrlEnding("Сосиска", startDate, numberOfDays);
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenReturn(Map.of());

                mockMvc.perform(post(urlTemplate)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                .content(realFileByteArray)
                                .characterEncoding(StandardCharsets.UTF_8))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorType").value(isFirstTest ?
                                "InvalidDateFormatException" : "NumberFormatException"));

                verify(logAnalyzer, Mockito.never())
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

        private String makeParameterizedUrlEnding(String searchQuery, String startDate, String numberOfDays)
        {
                StringBuilder urlEnding = new StringBuilder((Objects.isNull(searchQuery) &&
                        Objects.isNull(startDate) && Objects.isNull(numberOfDays)) ? "" : "?");
                if (Objects.nonNull(searchQuery))
                {
                        urlEnding.append("text=").append(searchQuery).append(
                                (Objects.nonNull(startDate) || Objects.nonNull(numberOfDays)) ? "&" : "");
                }
                if (Objects.nonNull(startDate))
                {
                        urlEnding.append("date=").append(startDate).append((Objects.nonNull(numberOfDays)) ? "&" : "");
                }
                if (Objects.nonNull(numberOfDays))
                {
                        urlEnding.append("days=").append(numberOfDays);
                }
                return urlEnding.toString();
        }

}