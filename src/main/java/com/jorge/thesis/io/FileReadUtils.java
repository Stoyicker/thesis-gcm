package com.jorge.thesis.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class FileReadUtils {

    public static List<String> readCSVFile(Path filePath) throws FileNotFoundException {
        if (!Files.exists(filePath))
            throw new FileNotFoundException(filePath.toAbsolutePath() + " does not exist or has not been found.");

        try {

            Charset charset;

            try {
                charset = Charset.forName(IOUtils.toString(FileReadUtils.class.getResourceAsStream
                        ("/charset")));
            } catch (IOException e) {
                e.printStackTrace(System.err);
                throw new IllegalStateException("Resource /charset not properly loaded.");
            }
            final CSVParser parser = CSVParser.parse(filePath.toFile(), charset, CSVFormat
                    .DEFAULT);
            final List<CSVRecord> records = parser.getRecords();
            final List<String> ret = new ArrayList<>();
            records.stream().forEach(v -> v.iterator().forEachRemaining(ret::add));
            return ret;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return Collections.<String>emptyList();
        }
    }
}
