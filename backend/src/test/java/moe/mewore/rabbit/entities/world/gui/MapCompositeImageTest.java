package moe.mewore.rabbit.entities.world.gui;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.entities.world.MazeMap;
import moe.mewore.rabbit.noise.FakeNoise;
import moe.mewore.rabbit.noise.Noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MapCompositeImageTest {

    @Test
    void drawFromScratch() {
        final MazeMap map = mock(MazeMap.class);
        final MapCompositeImage compositeImage = new MapCompositeImage(20, 20, map, new FakeNoise());
        compositeImage.drawFromScratch(Collections.emptySet());
        verify(map, never()).applyFertilityToImage(any(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(map).applyEverythingElseToImage(any());
    }

    @Test
    void testRedrawFlippedCells() {
        final MazeMap map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(2);
        when(map.getHeight()).thenReturn(25);
        final MapCompositeImage compositeImage = new MapCompositeImage(20, 20, map, new FakeNoise());
        compositeImage.redrawFlippedCells(List.of(0), List.of(0));
        verify(map, never()).applyFertilityToImage(any(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(map).applyEverythingElseToImage(any());
    }

    @Test
    void testRedrawFlippedCells_drawFertility() {
        final MazeMap map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(5000);
        when(map.getHeight()).thenReturn(5);
        final MapCompositeImage compositeImage = new MapCompositeImage(20, 20, map, new FakeNoise());
        compositeImage.setFertilityVisible(true);
        compositeImage.redrawFlippedCells(List.of(0), List.of(0));
        verify(map).applyFertilityToImage(any(), eq(0), eq(-4), eq(0), eq(7));
        verify(map).applyEverythingElseToImage(any());
    }

    @Test
    void testRedrawRegion() {
        final MazeMap map = mock(MazeMap.class);
        final MapCompositeImage compositeImage = new MapCompositeImage(20, 20, map, new FakeNoise());
        compositeImage.redrawRegion(0, 0, 0, 0, Collections.emptySet());
        verify(map).applyEverythingElseToImage(any());
    }

    @Test
    void testRedrawRegion_withNoise() {
        final MazeMap map = mock(MazeMap.class);
        final MapCompositeImage compositeImage = new MapCompositeImage(20, 20, map, new FakeNoise() {
            @Override
            public double get(final double x, final double y) {
                return .7;
            }
        });
        compositeImage.setNoiseVisibility(.5f);
        compositeImage.redrawRegion(0, 0, 0, 0, Collections.emptySet());
        verify(map).applyEverythingElseToImage(any());
    }

    @Test
    void testRedrawNoise() {
        final MazeMap map = mock(MazeMap.class);
        final Noise noise = mock(Noise.class);
        final MapCompositeImage compositeImage = new MapCompositeImage(20, 20, map, noise);
        compositeImage.redrawNoise();
        verify(noise, never()).get(anyDouble(), anyDouble());
    }

    @Test
    void testRedrawNoise_withDrawNoiseEnabled() {
        final MazeMap map = mock(MazeMap.class);
        final Noise noise = mock(Noise.class);
        final MapCompositeImage compositeImage = new MapCompositeImage(20, 20, map, noise);
        compositeImage.setNoiseVisibility(.4f);
        compositeImage.redrawNoise();
        verify(noise, times(400)).get(anyDouble(), anyDouble());
    }

    @Test
    void updateUiIndicators() {
        new MapCompositeImage(20, 20, mock(MazeMap.class), mock(Noise.class)).updateUiIndicators(null, null, null);
    }

    @Test
    void updateUiIndicators_withPaintedCells() {
        new MapCompositeImage(20, 20, mock(MazeMap.class), mock(Noise.class)).updateUiIndicators(Set.of(0), null, null);
    }

    @Test
    void updateUiIndicators_withPaint() {
        new MapCompositeImage(20, 20, mock(MazeMap.class), mock(Noise.class)).updateUiIndicators(null, true, null);
    }

    @Test
    void updateUiIndicators_withHoveredCell() {
        final MazeMap map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(1);
        when(map.getHeight()).thenReturn(2);
        new MapCompositeImage(20, 20, map, mock(Noise.class)).updateUiIndicators(null, null, 0);
    }

    @Test
    void updateUiIndicators_withHoveredCell_multiple() {
        final MazeMap map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(1);
        when(map.getHeight()).thenReturn(2);
        new MapCompositeImage(20, 20, map, mock(Noise.class)).updateUiIndicators(null, null, 0);
        new MapCompositeImage(20, 20, map, mock(Noise.class)).updateUiIndicators(null, null, 0);
    }


    @Test
    void updateUiIndicators_withAll() {
        final MazeMap map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(2);
        when(map.getHeight()).thenReturn(1);
        new MapCompositeImage(20, 20, map, mock(Noise.class)).updateUiIndicators(Set.of(0), false, 0);
    }

    @Test
    void testGetHoveredCell() {
        final MazeMap map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(3);
        when(map.getHeight()).thenReturn(2);
        final MapCompositeImage mapCompositeImage = new MapCompositeImage(30, 20, map, mock(Noise.class));
        assertEquals(0, mapCompositeImage.getHoveredCell(0, 0));
        assertEquals(5, mapCompositeImage.getHoveredCell(-5, -5));
        assertEquals(0, mapCompositeImage.getHoveredCell(5, 5));
        assertEquals(2, mapCompositeImage.getHoveredCell(25, 5));
    }
}
