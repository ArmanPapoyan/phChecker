package arman.papoyan.phchecker;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class ColorAnalyzer {

    private static final List<PHColor> PH_COLORS = new ArrayList<>();

    static {
        PH_COLORS.add(new PHColor(1, "#c75b51"));
        PH_COLORS.add(new PHColor(2, "#d0374c"));
        PH_COLORS.add(new PHColor(3, "#d094b9"));
        PH_COLORS.add(new PHColor(4, "#a87cbb"));
        PH_COLORS.add(new PHColor(5, "#a773bc"));
        PH_COLORS.add(new PHColor(6, "#b698d0"));
        PH_COLORS.add(new PHColor(7, "#5a719d"));
        PH_COLORS.add(new PHColor(8, "#4187a9"));
        PH_COLORS.add(new PHColor(9, "#206f36"));
        PH_COLORS.add(new PHColor(10, "#58ad50"));
        PH_COLORS.add(new PHColor(11, "#82ba4a"));
        PH_COLORS.add(new PHColor(12, "#8dc357"));
        PH_COLORS.add(new PHColor(13, "#dce85d"));
        PH_COLORS.add(new PHColor(14, "#d8e77f"));
    }

    private static float[] whiteReference = new float[]{255, 255, 255};
    private static boolean isCalibrated = false;

    public static void calibrateWithWhite(Bitmap bitmap, int x, int y, int width, int height) {
        float[] avgColor = getAverageColor(bitmap, x, y, width, height);
        whiteReference = new float[]{avgColor[0], avgColor[1], avgColor[2]};
        isCalibrated = true;

        android.util.Log.d("ColorAnalyzer", "White reference: R=" + whiteReference[0] +
                " G=" + whiteReference[1] + " B=" + whiteReference[2]);
    }
    public static void calibrateWithWhiteFromBitmap(Bitmap bitmap) {
        float[] avgColor = getAverageColorFromBitmap(bitmap);
        whiteReference = new float[]{avgColor[0], avgColor[1], avgColor[2]};
        isCalibrated = true;
        android.util.Log.d("ColorAnalyzer", "White reference: R=" + whiteReference[0] +
                " G=" + whiteReference[1] + " B=" + whiteReference[2]);
    }
    public static boolean isCalibrated() {
        return isCalibrated;
    }

    public static float estimatePH(Bitmap bitmap, int x, int y, int width, int height) {
        if (!isCalibrated) {
            return -1;
        }

        float[] measuredColor = getAverageColor(bitmap, x, y, width, height);
        float[] correctedColor = correctColor(measuredColor);

        return interpolatePH(correctedColor);
    }
    public static float estimatePHFromBitmap(Bitmap bitmap) {
        if (!isCalibrated) return -1;
        float[] measuredColor = getAverageColorFromBitmap(bitmap);
        float[] correctedColor = correctColor(measuredColor);
        return interpolatePH(correctedColor);
    }
    private static float[] correctColor(float[] measuredColor) {
        float refAvg = (whiteReference[0] + whiteReference[1] + whiteReference[2]) / 3f;

        float scale = 255f / refAvg;
        scale = Math.min(scale, 1.5f);

        float r = measuredColor[0] * scale;
        float g = measuredColor[1] * scale;
        float b = measuredColor[2] * scale;

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        android.util.Log.d("ColorAnalyzer", "Corrected: R=" + r + " G=" + g + " B=" + b);
        return new float[]{r, g, b};
    }

    private static float interpolatePH(float[] color) {
        PHColor best = null;
        PHColor second = null;
        float bestDist = Float.MAX_VALUE;
        float secondDist = Float.MAX_VALUE;

        for (PHColor phColor : PH_COLORS) {
            float distance = calculateColorDistance(color, phColor.rgb);
            android.util.Log.d("ColorAnalyzer", "Distance to pH " + phColor.pH + ": " + distance);

            if (distance < bestDist) {
                second = best;
                secondDist = bestDist;
                best = phColor;
                bestDist = distance;
            } else if (distance < secondDist) {
                second = phColor;
                secondDist = distance;
            }
        }

        if (best == null) {
            return 7.0f;
        }
        if (second == null || (bestDist + secondDist) == 0f) {
            return best.pH;
        }

        float weightBest = secondDist / (bestDist + secondDist);
        float weightSecond = bestDist / (bestDist + secondDist);

        return best.pH * weightBest + second.pH * weightSecond;
    }

    private static float calculateColorDistance(float[] rgb1, float[] rgb2) {
        float[] hsv1 = new float[3];
        float[] hsv2 = new float[3];
        android.graphics.Color.RGBToHSV(
                Math.round(rgb1[0]), Math.round(rgb1[1]), Math.round(rgb1[2]), hsv1);
        android.graphics.Color.RGBToHSV(
                Math.round(rgb2[0]), Math.round(rgb2[1]), Math.round(rgb2[2]), hsv2);

        float dh = Math.abs(hsv1[0] - hsv2[0]);
        if (dh > 180f) dh = 360f - dh;
        dh /= 180f;

        float ds = Math.abs(hsv1[1] - hsv2[1]);  // 0..1
        float dv = Math.abs(hsv1[2] - hsv2[2]);  // 0..1

        float weightH = 0.7f;
        float weightS = 0.2f;
        float weightV = 0.1f;

        return (float) Math.sqrt(
                weightH * dh * dh +
                        weightS * ds * ds +
                        weightV * dv * dv
        ) * 255f;
    }

    public static float[] getAverageColor(Bitmap bitmap, int x, int y, int width, int height) {
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(bitmap.getWidth(), x + width);
        int endY = Math.min(bitmap.getHeight(), y + height);

        int totalPixels = (endX - startX) * (endY - startY);
        if (totalPixels <= 0) return new float[]{0, 0, 0};

        final int QLEVELS = 8; // 8x8x8 = 512 корзин
        final int BIN_COUNT = QLEVELS * QLEVELS * QLEVELS;

        long[] sumR = new long[BIN_COUNT];
        long[] sumG = new long[BIN_COUNT];
        long[] sumB = new long[BIN_COUNT];
        int[] count = new int[BIN_COUNT];

        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                int pixel = bitmap.getPixel(i, j);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                int qr = r * QLEVELS / 256;
                int qg = g * QLEVELS / 256;
                int qb = b * QLEVELS / 256;
                int binIdx = qr * QLEVELS * QLEVELS + qg * QLEVELS + qb;

                sumR[binIdx] += r;
                sumG[binIdx] += g;
                sumB[binIdx] += b;
                count[binIdx]++;   // ← теперь точно есть
            }
        }

        int bestBin = -1;
        int bestCount = 0;
        for (int i = 0; i < BIN_COUNT; i++) {
            if (count[i] > bestCount) {
                bestCount = count[i];
                bestBin = i;
            }
        }

        if (bestBin < 0 || bestCount == 0) {
            return new float[]{0, 0, 0};
        }

        float r = sumR[bestBin] / (float) bestCount;
        float g = sumG[bestBin] / (float) bestCount;
        float b = sumB[bestBin] / (float) bestCount;

        android.util.Log.d("ColorAnalyzer",
                "Dominant bin: count=" + bestCount + "/" + totalPixels +
                        " color=#" + String.format("%02X%02X%02X",
                        Math.round(r), Math.round(g), Math.round(b)));

        return new float[]{r, g, b};
    }
    public static String getHexColor(float[] rgb) {
        int r = Math.round(rgb[0]);
        int g = Math.round(rgb[1]);
        int b = Math.round(rgb[2]);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static class PHColor {
        float pH;
        float[] rgb;

        PHColor(float pH, String hex) {
            this.pH = pH;
            this.rgb = hexToRgb(hex);
        }
    }

    private static float[] hexToRgb(String hex) {
        int color = Color.parseColor(hex);
        return new float[]{
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        };
    }
    public static float[] getAverageColorFromBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= 0 || h <= 0) return new float[]{0, 0, 0};

        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int pixel = bitmap.getPixel(i, j);
                sumR += android.graphics.Color.red(pixel);
                sumG += android.graphics.Color.green(pixel);
                sumB += android.graphics.Color.blue(pixel);
                count++;
            }
        }
        if (count == 0) return new float[]{0, 0, 0};
        return new float[]{sumR / (float) count, sumG / (float) count, sumB / (float) count};
    }
}