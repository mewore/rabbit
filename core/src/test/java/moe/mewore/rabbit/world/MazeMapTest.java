package moe.mewore.rabbit.world;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.geometry.ConvexPolygon;
import moe.mewore.rabbit.geometry.Vector2;
import moe.mewore.rabbit.noise.Noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MazeMapTest {

    @Test
    void testGenerate() {
        final Random random = new Random(11L);
        final Noise opennessNoise = mock(Noise.class);
        when(opennessNoise.get(anyDouble(), anyDouble())).thenReturn(.5);
        final WorldProperties properties = new WorldProperties("", 30, 30, 2.5, 2.5, 3, 3, "");

        // ~20KB
        assertEquals(20052, MazeMap.createSeamless(properties, random, opennessNoise).encodeToBinary().length);
    }

    @Test
    void testEncode() {
        final MazeMap map = new MazeMap(2.5, new boolean[3][3], new ArrayList<>(), new int[3][3][0]);
        assertEquals(29, map.encodeToBinary().length);
    }

    @Test
    void testSetCell() {
        final MazeMap map = new MazeMap(2.5, new boolean[3][3], new ArrayList<>(), new int[3][3][0]);
        assertFalse(map.getCell(1, 1));
        map.setCell(1, 1, true);
        assertTrue(map.getCell(1, 1));
    }

    @Test
    void testGetRowCount() {
        final MazeMap map = new MazeMap(2.5, new boolean[3][3], new ArrayList<>(), new int[3][3][0]);
        assertEquals(3, map.getRowCount());
    }

    @Test
    void testGetColumnCount() {
        final MazeMap map = new MazeMap(2.5, new boolean[3][2], new ArrayList<>(), new int[3][3][0]);
        assertEquals(2, map.getColumnCount());
    }

    @Test
    void testRender() {
        final List<String> lines = new ArrayList<>();
        lines.add(" ### ");
        lines.add(" #  #");
        lines.add(" # ##");
        lines.add("#### ");
        lines.add("   ##");
        final boolean[][] cells = lines.stream().map(line -> {
            final boolean[] cellLine = new boolean[line.length()];
            for (int i = 0; i < line.length(); i++) {
                cellLine[i] = line.charAt(i) == ' ';
            }
            return cellLine;
        }).collect(Collectors.toUnmodifiableList()).toArray(new boolean[0][0]);
        final List<MazeWall> walls = new ArrayList<>();
        walls.add(new MazeWall(0, 1, 0, 3, new ConvexPolygon(
            List.of(new Vector2(0, 0), new Vector2(0, .1), new Vector2(.1, .1), new Vector2(.1, 0)))));
        walls.add(new MazeWall(1, 1, 2, 1,
            new ConvexPolygon(List.of(new Vector2(0, 0), new Vector2(0, .2), new Vector2(.3, .2)))));
        final int[][][] relevantPolygons = new int[cells.length][cells[0].length][0];
        for (int i = 0; i < relevantPolygons.length; i++) {
            for (int j = 0; j < relevantPolygons[i].length; j++) {
                relevantPolygons[i][j] = new int[]{0, 1};
            }
        }
        final MazeMap map = new MazeMap(2.5, cells, walls, relevantPolygons);

        final BufferedImage image = map.render(256, 250);
        assertEquals(256, image.getWidth());
        assertEquals(250, image.getHeight());

        System.out.println("Map image:");
        for (int i = 0; i < image.getHeight(); i += 8) {
            for (int j = 0; j < image.getWidth(); j += 8) {
                final Color color = new Color(image.getRGB(j, i));

                System.out.print((color.getRed() + color.getGreen() + color.getBlue()) / 3 > 128 ? ' ' : '#');
            }
            System.out.println();
        }
    }
}
