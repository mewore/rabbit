package moe.mewore.rabbit.entities.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.noise.DiamondSquareNoise;
import moe.mewore.rabbit.noise.Noise;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Forest extends BinaryEntity {

    /**
     * The number of points to check (sample) before choosing the most fertile location for a plant. A value of 0
     * means all plants are at the same position. A value of 1 means the plants are distributed randomly regardless
     * of the ground fertility. An infinite value means that all plants are bunched up at the single most fertile
     * point. More generally, the (not normalized) probability of a plant being at a point is equal to the fertility
     * at that point raised to the same power as the number of samples minus 1. And since the exponential function
     * grows very quickly, even a few samples are enough to make the trees bunch up into the regions with a higher
     * fertility.
     */
    private static final int SAMPLE_COUNT_PER_PLANT = 2;

    private static final int PLANT_COUNT = 1024;

    private static final double TINY_CHANCE = 0.2;

    private final @NonNull Plant @NonNull [] plants;

    public static Forest generate(final Random random) {
        final Noise fertilityNoise = DiamondSquareNoise.createSeamless(6, random, true);

        final List<@NonNull Plant> plants = new ArrayList<>(PLANT_COUNT);
        double bestX = 0.0;
        double bestY = 0.0;
        double bestFertility;
        double x;
        double y;
        double fertility;
        for (int i = 0; i < PLANT_COUNT; i++) {
            bestFertility = 0;
            for (int j = 0; j < SAMPLE_COUNT_PER_PLANT; j++) {
                x = random.nextDouble();
                y = random.nextDouble();
                fertility = fertilityNoise.get(x, y);
                if (fertility >= bestFertility) {
                    bestX = x;
                    bestY = y;
                    bestFertility = fertility;
                }
            }
            plants.add(new Plant(bestX, bestY, random.nextDouble() < TINY_CHANCE ? 0.0 : bestFertility));
        }
        return new Forest(plants.toArray(new Plant[0]));
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(plants.length);
        for (final Plant plant : plants) {
            plant.appendToBinaryOutput(output);
        }
    }
}
