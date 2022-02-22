package moe.mewore.rabbit.noise.composite;

import java.util.function.BiFunction;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.noise.Noise;
import moe.mewore.rabbit.noise.NoiseBase;

@RequiredArgsConstructor
public class CompositeNoise extends NoiseBase {

    public static final BiFunction<Double, Double, Double> XOR_BLENDING = (first, second) ->
        (double) Math.min((int) (Math.min(first * 1000000, 1000000)) ^ (int) (Math.min(second * 1000000, 1000000)),
            1000000) / 1000000;

    public static final BiFunction<Double, Double, Double> XNOR_BLENDING = (first, second) -> 1.0 -
        XOR_BLENDING.apply(first, second);

    private final Noise first;

    private final Noise second;

    private final BiFunction<Double, Double, Double> blending;

    @Override
    public double get(final double x, final double y) {
        return blending.apply(first.get(x, y), second.get(x, y));
    }
}
