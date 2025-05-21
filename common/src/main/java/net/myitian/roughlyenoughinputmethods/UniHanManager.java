package net.myitian.roughlyenoughinputmethods;

// Based on me.shedaniel.rei.impl.client.search.method.unihan.UniHanManager
// MIT License

import me.shedaniel.rei.impl.common.InternalLogger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipInputStream;

public final class UniHanManager {
    private final Path unihanPath;

    public UniHanManager(Path unihanPath) {
        this.unihanPath = unihanPath;
    }

    public boolean downloaded() {
        return Files.exists(unihanPath);
    }

    public void download(ProgressCallback progressCallback) {
        try {
            download("https://shedaniel.moe/qntrML0EraNB.zip", progressCallback);
        } catch (Exception e) {
            download("https://www.unicode.org/Public/UCD/latest/ucd/Unihan.zip", progressCallback);
        }
    }

    public void download(String URL, ProgressCallback progressCallback) {
        if (downloaded()) return;
        try {
            java.net.URL url = new URL(URL);
            Files.deleteIfExists(unihanPath);
            Path parent = unihanPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            long completeFileSize = httpConnection.getContentLength();
            BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedOutputStream bufferedStream = new BufferedOutputStream(outputStream, 1024);
            byte[] data = new byte[1024];
            long downloadedFileSize = 0;
            int x;
            int lastPercent = 0;
            while ((x = inputStream.read(data, 0, 1024)) >= 0) {
                downloadedFileSize += x;
                double progress = (double) downloadedFileSize / (double) completeFileSize;
                int percent = (int) (progress * 100);
                if (percent > lastPercent) {
                    lastPercent = percent;
                    InternalLogger.getInstance().debug("Downloading UniHan Progress: %d%%".formatted(percent));
                }
                progressCallback.onProgress(progress);
                bufferedStream.write(data, 0, x);
            }
            bufferedStream.close();
            inputStream.close();
            Files.write(unihanPath, outputStream.toByteArray(), StandardOpenOption.CREATE);
            InternalLogger.getInstance().debug("Downloaded UniHan");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void load(DataConsumer consumer) throws IOException {
        try (ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(unihanPath))) {
            while (inputStream.getNextEntry() != null) {
                read(IOUtils.lineIterator(inputStream, StandardCharsets.UTF_8), consumer);
            }
        }
    }

    private void read(LineIterator lines, DataConsumer consumer) {
        int i = 0;
        while (lines.hasNext()) {
            i++;
            String line = lines.nextLine();
            if (line.startsWith("#") || line.isEmpty()) continue;
            if (!line.startsWith("U+")) {
                throw new IllegalArgumentException("Invalid line: " + i + ", " + line);
            }
            int firstTab = line.indexOf('\t');
            String code = line.substring(2, firstTab);
            int codePoint = Integer.parseInt(code, 16);
            int secondTab = line.indexOf('\t', firstTab + 1);
            String fieldKey = line.substring(firstTab + 1, secondTab);
            String data = line.substring(secondTab + 1);
            consumer.read(codePoint, fieldKey, data);
        }
    }

    @FunctionalInterface
    public interface DataConsumer {
        void read(int codepoint, String fieldKey, String data);
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(double progress);
    }
}
