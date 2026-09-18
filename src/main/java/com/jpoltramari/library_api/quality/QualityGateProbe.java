package com.jpoltramari.library_api.quality;

public final class QualityGateProbe {

    private QualityGateProbe() {
    }

    public static int calculate(int value) {
        int result = value;

        if (value > 0) {
            result += 10;
        }

        if (value > 10) {
            result += 20;
        }

        if (value > 20) {
            result += 30;
        }

        if (value > 30) {
            result += 40;
        }

        if (value > 40) {
            result += 50;
        }

        return result;
    }

    public static String classify(int value) {
        if (value < 0) {
            return "negative";
        }

        if (value == 0) {
            return "zero";
        }

        if (value < 10) {
            return "small";
        }

        return "positive";
    }
}
