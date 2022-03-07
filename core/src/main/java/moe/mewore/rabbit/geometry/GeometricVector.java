package moe.mewore.rabbit.geometry;

public interface GeometricVector {

    int store(final double[] source, int position);

    int load(final double[] source, int position);
}
