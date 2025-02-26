package com.exam.fileanalyzer.in;

import com.exam.fileanalyzer.service.LogsAnalyzer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.exam.fileanalyzer.service.LogsAnalyzer.CountEntriesParamHolder;

@WebMvcTest(controllers = { LogsAnalyzerController.class, FileAnalyzerExceptionHandler.class })
public class FileAnalyzerExceptionHandlerMockTest
{
        private static final byte[] realFileByteArray = LogsAnalyzerControllerMockTest.createRealFileByteArray();
        private final MockMvc mockMvc;

        @MockBean
        private LogsAnalyzer logAnalyzer;

        @Autowired
        public FileAnalyzerExceptionHandlerMockTest(MockMvc mockMvc)
        {
                this.mockMvc = mockMvc;
        }

        @Test
        void countEntriesInZipFile_whenServiceThrowIllegalArgumentException_thenReturnExceptionDtoWith400Status() throws Exception
        {
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenThrow(new IllegalArgumentException("You are not right!"));

                mockMvc.perform(post("/api/analyze/logs")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                .content(realFileByteArray)
                                .characterEncoding(StandardCharsets.UTF_8))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.errorType").value("IllegalArgumentException"))
                        .andExpect(jsonPath("$.errorMessage").value("You are not right!"));

                verify(logAnalyzer, Mockito.times(1))
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

        @Test
        void countEntriesInZipFile_whenServiceThrowRuntimeException_thenReturnExceptionDtoWith500Status() throws Exception
        {
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenThrow(new ArithmeticException("Wtf!"));

                mockMvc.perform(post("/api/analyze/logs")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                .content(realFileByteArray)
                                .characterEncoding(StandardCharsets.UTF_8))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.errorType").value("ArithmeticException"))
                        .andExpect(jsonPath("$.errorMessage").value("Wtf!"));

                verify(logAnalyzer, Mockito.times(1))
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

        @Test
        void countEntriesInZipFile_whenServiceThrowError_thenReturnExceptionDtoWith500Status() throws Exception
        {
                when(logAnalyzer.countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class)))
                        .thenThrow(new StackOverflowError("Wtf!"));

                mockMvc.perform(post("/api/analyze/logs")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                .content(realFileByteArray)
                                .characterEncoding(StandardCharsets.UTF_8))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.errorType").value("StackOverflowError"))
                        .andExpect(jsonPath("$.errorMessage").value("Wtf!"));

                verify(logAnalyzer, Mockito.times(1))
                        .countEntriesInZipFile(Mockito.any(CountEntriesParamHolder.class));
        }

}