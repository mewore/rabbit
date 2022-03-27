package moe.mewore.rabbit.backend.editor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.javalin.http.Context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditorVersionHandlerTest {

    private File[] files;

    private Context context;

    private EditorVersionHandler handler;

    @Mock
    private BiConsumer<Context, Collection<EditorVersion>> resultSender;

    @Captor
    private ArgumentCaptor<Collection<EditorVersion>> resultCaptor;

    private static File makeFileWithName(final String name) {
        final var file = mock(File.class);
        when(file.getName()).thenReturn(name);
        return file;
    }

    @BeforeEach
    void setUp() {
        context = mock(Context.class, Answers.RETURNS_SELF);
        files = null;
        handler = new EditorVersionHandler("editor-location", file -> files, resultSender);
    }

    @Test
    void handle() {
        final File dir = mock(File.class);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.getName()).thenReturn("10");
        when(dir.listFiles(any(FilenameFilter.class))).thenAnswer(invocation -> {
            final var filter = (FilenameFilter) invocation.getArgument(0);
            return Stream.of("win64.zip", "ween.zip", "lin64.tar.gz", "editor.jar", "a.jar", ".")
                .map(EditorVersionHandlerTest::makeFileWithName)
                .filter(file -> filter.accept(file, file.getName()))
                .toArray(File[]::new);
        });
        final File otherDir = mock(File.class);
        when(otherDir.isDirectory()).thenReturn(true);
        when(otherDir.getName()).thenReturn("2");
        when(otherDir.listFiles(any(FilenameFilter.class))).thenAnswer(invocation -> {
            final var filter = (FilenameFilter) invocation.getArgument(0);
            return Stream.of("screaming", "asd", "lin64.tar.gz", "", "a.jar", ".")
                .map(EditorVersionHandlerTest::makeFileWithName)
                .filter(file -> filter.accept(file, file.getName()))
                .toArray(File[]::new);
        });

        files = new File[]{dir, otherDir};
        handler.handle(context);
        verify(resultSender).accept(same(context), resultCaptor.capture());

        final List<EditorVersion> result = new ArrayList<>(resultCaptor.getValue());
        assertEquals(2, result.size());

        final EditorVersion latest = result.get(0);
        assertEquals(10, latest.getId());
        assertEquals(0L, latest.getLastModified());
        assertEquals("/editors/10/win64.zip", latest.getWindowsZipUrl());
        assertEquals("/editors/10/lin64.tar.gz", latest.getLinuxTarballUrl());
        assertEquals("/editors/10/editor.jar", latest.getJarUrl());

        final EditorVersion oldest = result.get(1);
        assertEquals(2, oldest.getId());
        assertEquals(0L, oldest.getLastModified());
        assertNull(oldest.getWindowsZipUrl());
        assertEquals("/editors/2/lin64.tar.gz", oldest.getLinuxTarballUrl());
        assertEquals("/editors/2/a.jar", oldest.getJarUrl());
    }

    @Test
    void handle_null() {
        handler.handle(context);
        verify(resultSender).accept(same(context), same(Collections.emptySet()));
    }

    @Test
    void handle_nonDirectory() {
        files = new File[]{mock(File.class)};
        handler.handle(context);
        verify(resultSender).accept(same(context), eq(Collections.emptyList()));
    }

    @Test
    void handle_invalidName() {
        final File dir = mock(File.class);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.getName()).thenReturn("not a number");

        files = new File[]{dir};
        handler.handle(context);
        verify(resultSender).accept(same(context), eq(Collections.emptyList()));
    }

    @Test
    void handle_noMatching() {
        final File dir = mock(File.class);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.getName()).thenReturn("5");
        when(dir.listFiles(any(FilenameFilter.class))).thenAnswer(invocation -> {
            final var filter = (FilenameFilter) invocation.getArgument(0);
            if (!filter.accept(mock(File.class), ".zip")) {
                return null;
            }
            if (!filter.accept(mock(File.class), ".tar.gz")) {
                return new File[0];
            }
            return null;
        });

        files = new File[]{dir};
        handler.handle(context);
        verify(resultSender).accept(same(context), eq(Collections.emptyList()));
    }
}
