package cn.valorin.dueltime4.util;

public final class FormatUtil {

    private FormatUtil() {}

    public static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static String distinguishPositiveNumber(double value) {
        return value > 0 ? "+" + round(value, 1) : String.valueOf(round(value, 1));
    }
}
