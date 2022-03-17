package moe.mewore.rabbit.geometry;

public interface GeometricVector {

    void store(final double[] source, int position);

    void load(final double[] source, int position);
}
