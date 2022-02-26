package moe.mewore.rabbit.backend.editor;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
class EditorVersion {

    public static final EditorVersion INVALID_EDITOR_VERSION = new EditorVersion(-1, -1, null, null, null);

    private final int id;

    private final long lastModified;

    private final @Nullable String windowsZipUrl;

    private final @Nullable String linuxTarballUrl;

    private final @Nullable String jarUrl;
}
