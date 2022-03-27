package moe.mewore.rabbit.backend.editor;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EditorVersionHandler implements Handler {

    private final String externalStaticLocation;

    private final Function<File, File @Nullable []> fileChildGetter;

    private final BiConsumer<Context, Collection<EditorVersion>> resultSender;

    private static final String EDITOR_DIR_PATH = "editors";

    private static @Nullable File getContainedFileMatching(final File directory, final FilenameFilter filter) {
        final File[] children = directory.listFiles(filter);
        return children == null || children.length == 0 ? null : children[0];
    }

    private static @Nullable String getFileUrl(final @Nullable File file, final String baseUrl) {
        return file == null ? null : "/" + baseUrl + "/" + file.getName();
    }

    @Override
    public void handle(final @NonNull Context ctx) {
        final File @Nullable [] subdirectories = fileChildGetter.apply(
                Path.of(externalStaticLocation, EDITOR_DIR_PATH).toFile());
        if (subdirectories == null) {
            resultSender.accept(ctx, Collections.emptySet());
            return;
        }

        final List<EditorVersion> versionSet = Arrays.stream(subdirectories).map(directory -> {
                    if (!directory.isDirectory()) {
                        return EditorVersion.INVALID_EDITOR_VERSION;
                    }

                final int version;
                try {
                    version = Integer.parseInt(directory.getName());
                } catch (final NumberFormatException e) {
                    return EditorVersion.INVALID_EDITOR_VERSION;
                }

                final @Nullable File windowsZip = getContainedFileMatching(directory,
                    (dir, name) -> name.contains("win64") && name.endsWith(".zip"));
                final @Nullable File jar = getContainedFileMatching(directory, (dir, name) -> name.endsWith(".jar"));
                final @Nullable File linuxTarball = getContainedFileMatching(directory,
                    (dir, name) -> name.contains("lin64") && name.endsWith(".tar.gz"));
                if (windowsZip == null && jar == null && linuxTarball == null) {
                    return EditorVersion.INVALID_EDITOR_VERSION;
                }

                final String baseUrl = EDITOR_DIR_PATH + "/" + directory.getName();
                final long lastModified = jar != null ? jar.lastModified() : directory.lastModified();
                return new EditorVersion(version, lastModified, getFileUrl(windowsZip, baseUrl),
                    getFileUrl(linuxTarball, baseUrl), getFileUrl(jar, baseUrl));
            })
            .filter(version -> version != EditorVersion.INVALID_EDITOR_VERSION)
            .sorted((v1, v2) -> v2.getId() - v1.getId())
            .collect(Collectors.toUnmodifiableList());

        resultSender.accept(ctx, versionSet);
    }
}
