package com.exam.fileanalyzer.config;

import org.springframework.context.annotation.*;
import org.springframework.format.datetime.*;
import org.springframework.format.support.*;
import org.springframework.lang.NonNull;

@Configuration
public class DateTimeFormatterConfig {

        @Bean
        public @NonNull FormattingConversionService dateFormatConversionService() {
                DefaultFormattingConversionService conversionService =
                        new DefaultFormattingConversionService(false);
                DateFormatterRegistrar dateRegistrar = new DateFormatterRegistrar();
                dateRegistrar.setFormatter(new DateFormatter("dd.MM.yyyy"));
                dateRegistrar.registerFormatters(conversionService);
                return conversionService;
        }

}