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

    public static boolean isCalibrated() {
        return isCalibrated;
    }

    public static float estimatePH(Bitmap bitmap, int x, int y, int width, int height) {
        if (!isCalibrated) {
            return -1;
        }

        float[] measuredColor = getAverageColor(bitmap, x, y, width, height);

        float[] correctedColor = correctColor(measuredColor);

        return findClosestPH(correctedColor);
    }

    private static float[] correctColor(float[] measuredColor) {
        float rScale = 255.0f / whiteReference[0];
        float gScale = 255.0f / whiteReference[1];
        float bScale = 255.0f / whiteReference[2];

        float r = measuredColor[0] * rScale;
        float g = measuredColor[1] * gScale;
        float b = measuredColor[2] * bScale;

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        float[] corrected = new float[]{r, g, b};

        android.util.Log.d("ColorAnalyzer", "Corrected: R=" + r + " G=" + g + " B=" + b);

        return corrected;
    }

    private static float findClosestPH(float[] color) {
        float minDistance = Float.MAX_VALUE;
        float closestPH = 7.0f;

        for (PHColor phColor : PH_COLORS) {
            float distance = calculateColorDistance(color, phColor.rgb);
            android.util.Log.d("ColorAnalyzer", "Distance to pH " + phColor.pH + ": " + distance);

            if (distance < minDistance) {
                minDistance = distance;
                closestPH = phColor.pH;
            }
        }

        return closestPH;
    }

    private static float calculateColorDistance(float[] color1, float[] color2) {
        float dr = color1[0] - color2[0];
        float dg = color1[1] - color2[1];
        float db = color1[2] - color2[2];

        float weightR = 0.3f;
        float weightG = 0.59f;
        float weightB = 0.11f;

        return (float) Math.sqrt(
                weightR * dr * dr +
                        weightG * dg * dg +
                        weightB * db * db
        );
    }

    public static float[] getAverageColor(Bitmap bitmap, int x, int y, int width, int height) {
        int totalPixels = 0;
        long sumR = 0, sumG = 0, sumB = 0;

        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(bitmap.getWidth(), x + width);
        int endY = Math.min(bitmap.getHeight(), y + height);

        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                int pixel = bitmap.getPixel(i, j);
                sumR += Color.red(pixel);
                sumG += Color.green(pixel);
                sumB += Color.blue(pixel);
                totalPixels++;
            }
        }

        if (totalPixels > 0) {
            return new float[]{
                    sumR / (float) totalPixels,
                    sumG / (float) totalPixels,
                    sumB / (float) totalPixels
            };
        }
        return new float[]{0, 0, 0};
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
}