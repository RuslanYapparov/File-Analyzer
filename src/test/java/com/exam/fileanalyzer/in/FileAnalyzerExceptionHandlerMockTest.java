package com.exam.fileanalyzer.in;

import com.exam.fileanalyzer.service.LogsAnalyzer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.exam.fileanalyzer.service.LogsAnalyzer.CountEntriesParamHolder;

@WebMvcTest(controllers = { LogsAnalyzerController.class, FileAnalyzerExceptionHandler.class })
public class FileAnalyzerExceptionHandlerMockTest
{
        private static final MockMultipartFile TEST_FILE = LogsAnalyzerControllerMockTest.createMockMultipartFile();
        private static final String URL_TEMPLATE = "/api/analyze/logs";
        @MockBean
        private LogsAnalyzer logAnalyzer;
        @Autowired
        private MockMvc mockMvc;

        @Test
        void countEntriesInZipFile_whenServiceThrowIllegalArgumentException_thenReturnExceptionDtoWith400Status()
                throws Exception
        {
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenThrow(new IllegalArgumentException("You are not right!"));

                mockMvc.perform(multipart(URL_TEMPLATE)
                                .file(TEST_FILE)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorType").value("IllegalArgumentException"))
                        .andExpect(jsonPath("$.errorMessage").value("You are not right!"));

                verify(logAnalyzer, Mockito.times(1))
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

        @Test
        void countEntriesInZipFile_whenServiceThrowRuntimeException_thenReturnExceptionDtoWith500Status()
                throws Exception
        {
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenThrow(new ArithmeticException("Wtf!"));

                mockMvc.perform(multipart(URL_TEMPLATE)
                                .file(TEST_FILE)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.errorType").value("ArithmeticException"))
                        .andExpect(jsonPath("$.errorMessage").value("Wtf!"));

                verify(logAnalyzer, Mockito.times(1))
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

}