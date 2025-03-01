package com.exam.fileanalyzer.service;

import com.exam.fileanalyzer.service.impl.MultiThreadLogsAnalyzerImpl;
import com.exam.fileanalyzer.service.impl.SingleThreadLogsAnalyzerImpl;
import com.exam.fileanalyzer.service.impl.ZipFileManagerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

import static com.exam.fileanalyzer.service.LogsAnalyzer.CountEntriesParamHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = { MultiThreadLogsAnalyzerImpl.class, ZipFileManagerImpl.class },
        properties = { "temp.dir.path=src/test/resources/tmp" })
public class LogsAnalyzerTest
{
        private final static String FILES_DIRECTORY = "src/test/resources/";
        private LogsAnalyzer logsAnalyzer;
        @SpyBean
        private ZipFileManager zipFileManager;

        @Autowired
        public LogsAnalyzerTest(LogsAnalyzer logsAnalyzer)
        {
                this.logsAnalyzer = logsAnalyzer;
        }

        @ParameterizedTest
        @ValueSource(strings = { "logs-27_02_2018-03_03_2018.zip", "logs_in_directories.zip", "проблемный зип.zip" })
        void countEntriesInZipFile_whenGetCorrectNonNullParamsWithCorrectTestFile_thenReturnCorrectMap(String fileName)
                throws IOException
        {
                CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                        .searchQuery("Mozilla")
                        .zipMultipartFile(createMockFile(fileName))
                        .startDate(LocalDate.of(2018, 2, 27))
                        .numberOfDays(3)
                        .build();
                Map<String, Integer> result = logsAnalyzer.countEntriesInZipFile(paramHolder);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(3);
                assertThat(result.get("logs_2018-02-27-access.log")).isEqualTo(40);
                assertThat(result.get("logs_2018-02-28-access.log")).isEqualTo(18);
                assertThat(result.get("logs_2018-03-01-access.log")).isEqualTo(23);
        }

        @Test
        void countEntriesInZipFile_whenGetNullSearchQueryWith2018TestFile_thenReturnCorrectMap() throws IOException
        {
                CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                        .zipMultipartFile(createMockFile("logs-27_02_2018-03_03_2018.zip"))
                        .startDate(LocalDate.of(2018, 2, 28))
                        .numberOfDays(4)
                        .build();
                Map<String, Integer> result = logsAnalyzer.countEntriesInZipFile(paramHolder);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(4);
                assertThat(result.get("logs_2018-02-28-access.log")).isEqualTo(19);
                assertThat(result.get("logs_2018-03-01-access.log")).isEqualTo(23);
                assertThat(result.get("logs_2018-03-02-access.log")).isEqualTo(23);
                assertThat(result.get("logs_2018-03-03-access.log")).isEqualTo(30);
        }

        @Test
        void countEntriesInZipFile_whenGetNullStartDateWith2018TestFile_thenReturnEmptyMap() throws IOException
        {
                CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                        .searchQuery("Mozilla")
                        .zipMultipartFile(createMockFile("logs-27_02_2018-03_03_2018.zip"))
                        .numberOfDays(3)
                        .build();
                Map<String, Integer> result = logsAnalyzer.countEntriesInZipFile(paramHolder);

                assertThat(result).isNotNull();
                assertThat(result).isEmpty();
        }

        @Test
        void countEntriesInZipFile_whenGetNullNumberOfDaysWith2018TestFile_thenReturnCorrectMap() throws IOException
        {
                CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                        .searchQuery("Mozilla")
                        .zipMultipartFile(createMockFile("logs-27_02_2018-03_03_2018.zip"))
                        .startDate(LocalDate.of(2018, 2, 27))
                        .build();
                Map<String, Integer> result = logsAnalyzer.countEntriesInZipFile(paramHolder);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                assertThat(result.get("logs_2018-02-27-access.log")).isEqualTo(40);
        }

        @ParameterizedTest
        @ValueSource(strings = { "SuYo.jpg" })
        @NullSource
        void countEntriesInZipFile_whenGetNullOrNotZipFileWith2018TestFile_thenThrowsIllegalArgumentException(
                String fileName) throws IOException
        {
                CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                        .searchQuery("Mozilla")
                        .zipMultipartFile(Objects.isNull(fileName) ? null : createMockFile(fileName))
                        .startDate(LocalDate.of(2018, 2, 27))
                        .build();

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> logsAnalyzer.countEntriesInZipFile(paramHolder));
                assertThat(exception.getMessage()).isEqualTo("There is no file to open or it is not a zip file.");
        }

        @ParameterizedTest
        @ValueSource(strings = { "empty.zip", "incorrect_pattern_logs+file.zip" })
        void countEntriesInZipFile_whenGetEmptyOrIncorrectPatterLogsZipFileWithIncorrectZipTestFile_thenReturnEmptyMap(
                String fileName) throws IOException
        {
                CountEntriesParamHolder paramHolder = CountEntriesParamHolder.builder()
                        .searchQuery("Mozilla")
                        .zipMultipartFile(createMockFile(fileName))
                        .startDate(LocalDate.of(2018, 2, 27))
                        .numberOfDays(3)
                        .build();
                Map<String, Integer> result = logsAnalyzer.countEntriesInZipFile(paramHolder);

                assertThat(result).isNotNull();
                assertThat(result).isEmpty();
        }

        @Test
        void singleThreadLogsAnalyzerImplTests() throws IOException {
                logsAnalyzer = new SingleThreadLogsAnalyzerImpl(zipFileManager);
                countEntriesInZipFile_whenGetCorrectNonNullParamsWithCorrectTestFile_thenReturnCorrectMap(
                        "проблемный зип.zip");
                countEntriesInZipFile_whenGetNullOrNotZipFileWith2018TestFile_thenThrowsIllegalArgumentException(
                        "SuYo.jpg");
        }

        private MultipartFile createMockFile(String fileName) throws IOException
        {
                return new MockMultipartFile("MockFile", fileName, null,
                        new FileInputStream(FILES_DIRECTORY + fileName));
        }

}