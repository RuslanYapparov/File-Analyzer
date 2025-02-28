package com.exam.fileanalyzer.service.impl;

import com.exam.fileanalyzer.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;

import static com.exam.fileanalyzer.service.LogsAnalyzer.CountEntriesParamHolder;

/**
 * ZipFileManager interface implementation - bean responsible for unzip necessary log files from zip file and delete
 * them after work.
 */
@Slf4j
@Service
public class ZipFileManagerImpl implements ZipFileManager
{
        /** Constant with pattern for log file name. */
        private static final Pattern LOG_FILE_NAME_PATTERN =
                Pattern.compile("logs_\\d{4}-\\d{2}-\\d{2}-access\\.log");
        /** Constant with date format in log file name. */
        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        /** Path of temp directory for operated log files. */
        private final Path tempDir;

        /**
         * Bean's constructor with property value injection.
         *
         * @param tempDirPath path of temp directory for operated log files (property value).
         */
        @Autowired
        public ZipFileManagerImpl(@Value("${temp.dir.path}") String tempDirPath)
        {
                tempDir = Paths.get(tempDirPath);
        }

        /**
         * Creates temp directory for operated log files if it not exists.
         *
         * @throws IOException if meets problems with creating temp directory.
         */
        @PostConstruct
        public void createTempDirectoryIfNotExists() throws IOException
        {
                if (Files.exists(tempDir))
                {
                        log.info("Temp directory for log files already exists.");
                } else
                {
                        log.info("There is no temp directory for log files. Creating...");
                        Files.createDirectory(tempDir);
                        log.info("Temp directory for log files successfully created.");
                }
        }

        /**
         * Unzips necessary log files from the zip file.
         *
         * @param paramHolder object containing the search query, zip file, start date, and number of days.
         * @param charset (optional) charset used to read the zip file.
         * @return a list of paths to the log files.
         * @throws IOException if the zip file or its entries cannot be read.
         */
        @Override
        public List<Path> getPathsOfLogFilesForAnalysis(@NonNull CountEntriesParamHolder paramHolder,
                                                        @Nullable Charset charset) throws IOException
        {
                if (Objects.isNull(paramHolder.getZipMultipartFile()) ||
                        Objects.isNull(paramHolder.getZipMultipartFile().getOriginalFilename()) ||
                        !paramHolder.getZipMultipartFile().getOriginalFilename().endsWith(".zip"))
                {
                        throw new IllegalArgumentException("There is no file to open or it is not a zip file.");
                }
                log.debug("Getting paths of necessary log files from zip file{}",
                        Objects.isNull(charset) ? "." : " with charset '" + charset + "'.");
                Set<Path> paths = new LinkedHashSet<>();
                Path unzipDir = createUnzipDir();
                paths.add(unzipDir);
                try (ZipInputStream zipStream = new ZipInputStream(paramHolder.getZipMultipartFile().getInputStream(),
                        Objects.isNull(charset) ? StandardCharsets.UTF_8 : charset))
                {
                        ZipEntry entry;
                        while ((entry = zipStream.getNextEntry()) != null)
                        {
                                TempFileCreatingParams params = new TempFileCreatingParams(zipStream, entry, unzipDir,
                                        paramHolder.getStartDate(), paramHolder.getNumberOfDays());
                                if (!entry.isDirectory())
                                {
                                        createAppropriateLogFileInTempDir(params).ifPresent(paths::add);
                                }
                                zipStream.closeEntry();
                        }
                } catch (IllegalArgumentException exception)
                {
                       deleteTempLogFiles(new ArrayList<>(paths));
                       return handleIllegalArgumentExceptionWithZipFileEntries(exception, paramHolder);
                }
                log.debug("Found {} paths of necessary log files in zip file.", paths.size());
                return new ArrayList<>(paths);
        }

        /**
         * Deletes the temporary log files and their parent directory.
         *
         * @param logFilePaths a list of paths to the log files.
         * @throws IOException if meets the problem on deleting some file.
         * @throws IllegalStateException if gets null or empty list.
         */
        @Override
        public void deleteTempLogFiles(List<Path> logFilePaths) throws IOException
        {
                log.debug("Deleting temporary log files and their parent directory...");
                if (Objects.isNull(logFilePaths) || logFilePaths.isEmpty())
                {
                        throw new IllegalStateException("The log file paths list for deleting is null or empty. " +
                                "There is must be at least one path (created temp directory) to delete.");
                }
                int size = logFilePaths.size();
                Path parentDir = logFilePaths.get(0);
                for (int i = 1; i < logFilePaths.size(); i++)
                {
                        Files.deleteIfExists(logFilePaths.get(i));
                }
                Files.deleteIfExists(parentDir);
                try
                {
                        logFilePaths.clear();
                } catch (UnsupportedOperationException exception)
                {
                        log.warn("Can't clear the list of log file paths because it is unmodifiable!");
                }
                log.debug("Temporary log files and their parent directory successfully deleted ({} files).", size);
        }

        /**
         * Creates a temporary directory for each zip file received in the http request.
         * Guaranties that each thread will work with its directory.
         *
         * @return a path to the temporary directory.
         * @throws IOException if the temporary directory cannot be created.
         */
        private Path createUnzipDir() throws IOException
        {
                String tmpDirName = String.valueOf(System.currentTimeMillis()).substring(5);
                Path unzipDir = tempDir.resolve(tmpDirName);
                if (Files.exists(unzipDir))
                {
                        tmpDirName = String.valueOf(System.currentTimeMillis() - 5000).substring(5);
                        unzipDir = tempDir.resolve(tmpDirName);
                }
                Files.createDirectory(unzipDir);
                return unzipDir;
        }

        /**
         * Checks if the log file date is appropriate for log analysis by conditions received in the http request.
         *
         * @param params parameters for creating and filtering necessary log files.
         * @return optional value with the path to the log file or empty if there is no appropriate log file.
         * @throws IOException if the temporary log file for analysis cannot be created.
         */
        private Optional<Path> createAppropriateLogFileInTempDir(TempFileCreatingParams params) throws IOException
        {
                Optional<Path> mayBeFilePath = Optional.empty();
                String fileName = params.zipEntry.getName();
                if (fileName.contains("/"))
                {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                if (LOG_FILE_NAME_PATTERN.matcher(fileName).matches() && isLogFileDateAppropriate(fileName, params))
                {
                        Path filePath = params.unzipDir.resolve(fileName);
                        Files.copy(params.zipStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                        mayBeFilePath = Optional.of(filePath);
                }
                return mayBeFilePath;
        }

        /**
         * Checks if the log file date is appropriate for log analysis by conditions received in the http request.
         *
         * @param fileName name of the log file.
         * @param params parameters for creating and filtering necessary log files.
         * @return true if the log file date is appropriate, false otherwise.
         */
        private boolean isLogFileDateAppropriate(String fileName, TempFileCreatingParams params)
        {
                String extractedDate = fileName.substring(5, 15);
                LocalDate logDate = LocalDate.parse(extractedDate, DATE_FORMAT);
                boolean isStartDateNull = Objects.isNull(params.startDate);
                boolean isNumberOfDaysNull = Objects.isNull(params.numberOfDays);
                if (isStartDateNull && isNumberOfDaysNull)
                {
                        return logDate.equals(LocalDate.now());
                }
                if (isStartDateNull)
                {
                        LocalDate point = LocalDate.now().minusDays(params.numberOfDays);
                        return !logDate.isBefore(point);
                }
                if (isNumberOfDaysNull)
                {
                        return logDate.equals(params.startDate);
                }
                LocalDate endDate = params.startDate.plusDays(params.numberOfDays);
                return !logDate.isBefore(params.startDate) && logDate.isBefore(endDate);
        }

        /**
         * Additionally launch the method of unpacking the necessary files from the archive if the first attempt meets
         * the UTF-8-charset error (IllegalArgumentException).
         * <p> This happens when obtaining a ZIP archive created in Windows and containing not ASCII characters
         * in directory and file names.
         *
         * @param exception IllegalArgumentException met in first attempt.
         * @param paramHolder parameters for creating and filtering necessary log files.
         * @return a list of paths to the log files.
         * @throws IOException if the zip file or its entries cannot be read.
         */
        private List<Path> handleIllegalArgumentExceptionWithZipFileEntries(IllegalArgumentException exception,
                                                                            CountEntriesParamHolder paramHolder)
                throws IOException
        {
                Throwable cause = exception.getCause();
                String exceptionName = Objects.isNull(cause) ? exception.getClass().getSimpleName() :
                        cause.getClass().getSimpleName();
                log.warn("There is problem with zip file - {} ({}). Trying CP437-charset instead UTF-8.",
                        exceptionName, exception.getMessage());
                try
                {
                        return getPathsOfLogFilesForAnalysis(paramHolder, Charset.forName("CP437"));
                } catch (IllegalArgumentException anotherException)
                {
                        throw new IOException(anotherException.getMessage());
                }
        }

        /**
         * The helper object providing necessary parameters for creating and filtering necessary log files.
         */
        @RequiredArgsConstructor
        private static class TempFileCreatingParams
        {
                /** Input stream of the zip file. */
                private final ZipInputStream zipStream;
                /** Entry of the zip file that can be necessary log file. */
                private final ZipEntry zipEntry;
                /** Path of the temporary directory for unzipped log files. */
                private final Path unzipDir;
                /** Start date for the log file filtering. */
                private final LocalDate startDate;
                /** Number of days for the log file filtering. */
                private final Integer numberOfDays;

        }

}