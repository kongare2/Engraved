package com.kongare;

import net.minecraft.Util;
import net.minecraft.util.HttpUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Ocelot
 */
public class RawAudioSource implements AudioSource {

    private final CompletableFuture<AsyncInputStream.InputStreamSupplier> locationFuture;
    private CompletableFuture<InputStream> stream;

    public RawAudioSource(String hash, URL url, @Nullable DownloadProgressListener listener, boolean temporary, AudioFileType type) throws IOException {
        Path location = SoundCache.resolveFilePath(hash, temporary);
        this.locationFuture = CompletableFuture.supplyAsync(() -> AudioSource.downloadTo(location, url, listener, type), HttpUtil.DOWNLOAD_EXECUTOR);
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        if (this.stream != null)
            return this.stream;
        return this.stream = this.locationFuture.thenApplyAsync(stream -> {
            try {
                return stream.get();
            } catch (Exception e) {
                throw new CompletionException("Failed to open stream", e);
            }
        }, Util.ioPool());
    }
}
