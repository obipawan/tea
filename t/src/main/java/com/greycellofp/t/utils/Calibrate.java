package com.greycellofp.t.utils;

import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.signum;

/**
 * Created by pawan.kumar1 on 25/04/15.
 */
public class Calibrate {
    private int previousDirection = 0;
    private double directionChanges = 0;
    private int iteration = 0;

    public double calibrate(double maxVolumeRatio, int leftBandwidth, int rightBandwidth) {
        int difference = leftBandwidth - rightBandwidth;
        int direction = (int) signum(difference);

        if (previousDirection != direction) {
            directionChanges++;
            previousDirection = direction;
        }

        int ITERATION_CYCLES = 20;
        iteration = ++iteration % ITERATION_CYCLES;
        if (iteration == 0) {
            int UP_THRESHOLD = 5;
            if (directionChanges >= UP_THRESHOLD) {
                double UP_AMOUNT = 1.1;
                maxVolumeRatio = maxVolumeRatio * UP_AMOUNT;
            }
            int DOWN_THRESHOLD = 0;
            if (directionChanges == DOWN_THRESHOLD) {
                double DOWN_AMOUNT = 0.9;
                maxVolumeRatio = maxVolumeRatio * DOWN_AMOUNT;
            }

            double MAX_RATIO = 0.95;
            maxVolumeRatio = min(MAX_RATIO, maxVolumeRatio);
            double MIN_RATIO = 0.0001;
            maxVolumeRatio = max(MIN_RATIO, maxVolumeRatio);
            directionChanges = 0;
        }

        return maxVolumeRatio;
    }
}
