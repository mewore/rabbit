package moe.mewore.rabbit.world.editor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.noise.CompositeNoise;
import moe.mewore.rabbit.noise.DiamondSquareNoise;
import moe.mewore.rabbit.noise.Noise;
import moe.mewore.rabbit.world.MazeMap;
import moe.mewore.rabbit.world.WorldProperties;

@RequiredArgsConstructor
public class WorldEditor {

    private static final int NOISE_SHARPNESS_INPUT_MULTIPLIER = 10;

    private static final int NOISE_VISIBILITY_INPUT_MULTIPLIER = 10;

    private final MazeMapCanvas canvas = new MazeMapCanvas();

    private final JTextField seedField = new JTextField("11", 8);

    private final JSpinner columnCountField = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 5));

    private final JSpinner rowCountField = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 5));

    private final JSpinner cellSizeField = new JSpinner(new SpinnerNumberModel(80.0, 1.0, 1000.0, 1.0));

    private final JSpinner smoothingPassesField = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));

    private final JSpinner noiseResolutionField = new JSpinner(new SpinnerNumberModel(7, 1, 10, 1));

    private final JSpinner noiseSharpnessField = new JSpinner(
        new SpinnerNumberModel(5.0, 0, NOISE_SHARPNESS_INPUT_MULTIPLIER, 1));

    private final JSpinner noiseVisibilityField = new JSpinner(
        new SpinnerNumberModel(0f, 0, NOISE_VISIBILITY_INPUT_MULTIPLIER, 1));

    private final JCheckBox visibleFertilityCheckbox = new JCheckBox();

    private final File worldPropertiesFile;

    public static void main(final String[] args) throws IOException {
        new WorldEditor(new File(args.length >= 1 ? args[0] : WorldProperties.FILENAME)).open();
    }

    private void open() throws IOException {
        final JFrame frame = new JFrame("World Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        final JMenuBar mb = new JMenuBar();
        frame.getContentPane().add(BorderLayout.NORTH, mb);

        final File propertiesFile = new File(WorldProperties.FILENAME);
        final WorldProperties worldProperties = propertiesFile.canRead()
            ? WorldProperties.getFromFile(propertiesFile)
            : WorldProperties.getFromClasspath();
        seedField.setText(worldProperties.getSeed());

        final JPanel upperPanel = new JPanel();

        seedField.setText(worldProperties.getSeed());
        addField(upperPanel, "Seed", seedField);

        columnCountField.setValue(worldProperties.getColumnCount());
        addField(upperPanel, "Columns", columnCountField);

        rowCountField.setValue(worldProperties.getRowCount());
        addField(upperPanel, "Rows", rowCountField);

        cellSizeField.setValue(worldProperties.getCellSize());
        addField(upperPanel, "Cell size", cellSizeField);
        cellSizeField.setToolTipText("Changing this doesn't make anything in the editor look different at the moment.");

        final JPanel lowerPanel = new JPanel();

        smoothingPassesField.setValue(worldProperties.getSmoothingPasses());
        addField(lowerPanel, "Smoothing", smoothingPassesField);

        noiseResolutionField.setValue(worldProperties.getNoiseResolution());
        addField(lowerPanel, "Noise resolution", noiseResolutionField);

        noiseSharpnessField.setValue(worldProperties.getNoiseSharpness() * 10);
        addField(lowerPanel, "Noise sharpness", noiseSharpnessField);


        lowerPanel.add(new JLabel("Noise visibility"));
        lowerPanel.add(noiseVisibilityField);
        noiseVisibilityField.addChangeListener(a -> this.canvas.setNoiseVisibility(getNoiseVisibility()));

        lowerPanel.add(new JLabel("Visible fertility"));
        lowerPanel.add(visibleFertilityCheckbox);
        visibleFertilityCheckbox.addChangeListener(
            a -> this.canvas.setFertilityVisible(visibleFertilityCheckbox.isSelected()));

        canvas.setFlippedCells(worldProperties.getFlippedCellSet());

        final JButton saveButton = new JButton("Save");
        upperPanel.add(saveButton);
        saveButton.setToolTipText("Will save to: " + worldPropertiesFile.getAbsolutePath());
        saveButton.addActionListener(a -> {
            try {
                makeWorldProperties().save(worldPropertiesFile);
            } catch (final IOException e) {
                System.out.println("Failed to save to: " + worldPropertiesFile.getAbsolutePath());
                e.printStackTrace();
            }
        });

        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        panel.add(upperPanel);
        panel.add(lowerPanel);
        frame.getContentPane().add(BorderLayout.SOUTH, panel);
        frame.setVisible(true);

        frame.getContentPane().add(BorderLayout.CENTER, canvas);

        updateCanvasMap();
    }

    private void addField(final JPanel panel, final String label, final JComponent component) {
        panel.add(new JLabel(label));
        panel.add(component);
        if (component != cellSizeField) {
            if (component instanceof JSpinner) {
                ((JSpinner) component).addChangeListener((a) -> this.updateCanvasMap());
            } else if (component instanceof JTextField) {
                ((JTextField) component).addActionListener((a) -> this.updateCanvasMap());
            }
        }
    }

    private void updateCanvasMap() {
        final WorldProperties worldProperties = makeWorldProperties();
        final long seed = worldProperties.getSeedAsLong();
        final int resolution = worldProperties.getNoiseResolution();
        final double sharpness = worldProperties.getNoiseSharpness();
        final Noise opennessNoise = new CompositeNoise(
            DiamondSquareNoise.createSeamless(resolution, new Random(seed), 1.0, sharpness),
            DiamondSquareNoise.createSeamless(resolution, new Random(seed + 1), 1.0, sharpness),
            CompositeNoise.XNOR_BLENDING);
        final MazeMap map = MazeMap.createSeamless(worldProperties, new Random(seed), opennessNoise);
        canvas.setNoiseVisibility(getNoiseVisibility());
        canvas.setFertilityVisible(visibleFertilityCheckbox.isSelected());
        canvas.setUp(map, worldProperties.getFlippedCellSet(), opennessNoise);
        canvas.update(canvas.getGraphics());
    }

    private float getNoiseVisibility() {
        return (float) (double) noiseVisibilityField.getValue() / NOISE_VISIBILITY_INPUT_MULTIPLIER;
    }

    private WorldProperties makeWorldProperties() {
        return new WorldProperties(seedField.getText(), (int) columnCountField.getValue(),
            (int) rowCountField.getValue(), (double) cellSizeField.getValue(),
            (double) noiseSharpnessField.getValue() / NOISE_SHARPNESS_INPUT_MULTIPLIER,
            (int) noiseResolutionField.getValue(), (int) smoothingPassesField.getValue(),
            canvas.getFlippedCells().stream().map(Object::toString).collect(Collectors.joining(",")));
    }
}
