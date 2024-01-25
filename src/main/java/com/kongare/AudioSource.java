package com.kongare;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Sources of raw audio data to be played.
 *
 * @author Ocelot
 */
public interface AudioSource {

    Logger LOGGER = LogManager.getLogger();

    static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = SoundDownloadSource.getDownloadHeaders();
        map.put("X-Minecraft-Username", Minecraft.getInstance().getUser().getName());
        map.put("X-Minecraft-UUID", Minecraft.getInstance().getUser().getProfileId().toString());
        return map;
    }

    static AsyncInputStream.InputStreamSupplier downloadTo(Path file, URL url, @Nullable DownloadProgressListener progressListener, AudioFileType type) {
        if (progressListener != null)
            progressListener.progressStartRequest(Component.translatable("resourcepack.requesting"));

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            getDownloadHeaders().forEach(connection::setRequestProperty);

            long contentLength = connection.getContentLengthLong();

            // Indicates a cache of "forever"
            long cacheTime = Long.MAX_VALUE;
            int cachePriority = 0;
            boolean cache = true;

            String cacheControl = connection.getHeaderField("Cache-Control");
            if (cacheControl != null) {
                String[] parts = cacheControl.split(",");
                for (String part : parts) {
                    try {
                        String[] entry = part.split("=");
                        String name = entry[0].trim();
                        String value = entry.length > 1 ? entry[1].trim() : null;
                        switch (name) {
                            case "max-age": {
                                if (cachePriority > 0)
                                    break;
                                try {
                                    cacheTime = Integer.parseInt(value);
                                } catch (NumberFormatException e) {
                                    LOGGER.error("Invalid max-age: " + value);
                                }
                                break;
                            }
                            case "s-maxage": {
                                cachePriority = 1;
                                try {
                                    cacheTime = Integer.parseInt(value);
                                } catch (NumberFormatException e) {
                                    LOGGER.error("Invalid s-maxage: " + value);
                                }
                                break;
                            }
                            // Skip must-revalidate
                            // Skip no-cache because "hidden" files are already in the temp directory
                            case "no-store": {
                                cache = false;
                                break;
                            }
                            // Skip private
                            // Skip public
                            // Skip no-transform
                            // Skip immutable
                            // Skip stale-while-revalidate
                            // Skip stale-if-error
                        }
                    } catch (Exception e) {
                        LOGGER.error("Invalid response header: {}", part, e);
                    }
                }
            }

            String ageHeader = connection.getHeaderField("Age");
            if (ageHeader != null) {
                try {
                    cacheTime -= Integer.parseInt(ageHeader);
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid Age: " + ageHeader);
                }
            }

            if (contentLength <= 0 || cacheTime <= 0 || !cache) {
                if (!type.isStream())
                    throw new IOException("The provided URL is a stream, but that is not supported");
                Files.deleteIfExists(file);
                return () -> new AsyncInputStream(url::openStream, 8192, 8, HttpUtil.DOWNLOAD_EXECUTOR);
            }

            if (!type.isFile())
                throw new IOException("The provided URL is a file, but that is not supported");
            if (SoundCache.isValid(file, file.getFileName().toString()))
                return () -> Files.newInputStream(file.toFile().toPath());
            if (contentLength > 104857600)
                throw new IOException("Filesize is bigger than maximum allowed (file is " + contentLength + ", limit is 104857600)");

            SoundCache.updateCache(file, file.getFileName().toString(), cacheTime, TimeUnit.SECONDS, new ProgressTrackingInputStream(connection.getInputStream(), contentLength, progressListener) {
                @Override
                public int read() throws IOException {
                    int value = super.read();
                    if (this.getRead() > 104857600)
                        throw new IOException("Filesize was bigger than maximum allowed (got >= " + this.getRead() + ", limit was 104857600)");
                    return value;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int value = super.read(b, off, len);
                    if (this.getRead() > 104857600)
                        throw new IOException("Filesize was bigger than maximum allowed (got >= " + this.getRead() + ", limit was 104857600)");
                    return value;
                }
            });
        } catch (Throwable e) {
            throw new CompletionException(e);
        }
        return () -> Files.newInputStream(file);
    }

    /**
     * @return A future to a resource that will exist at some point in the future
     */
    CompletableFuture<InputStream> openStream();

    enum AudioFileType {
        FILE(true, false),
        STREAM(false, true),
        BOTH(true, true);

        private final boolean file;
        private final boolean stream;


        AudioFileType(boolean file, boolean stream) {
            this.file = file;
            this.stream = stream;
        }

        public boolean isFile() {
            return file;
        }

        public boolean isStream() {
            return stream;
        }
    }
}