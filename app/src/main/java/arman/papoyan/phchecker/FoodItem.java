package arman.papoyan.phchecker;

import java.util.List;
import java.util.Locale;

/**
 * One row from the fresh-vs-spoiled pH reference table.
 * All numbers come directly from the reference data supplied by the user —
 * nothing here is invented or pulled from any other source.
 */
public class FoodItem {

    public enum Freshness {
        FRESH,
        SPOILED,
        UNCERTAIN
    }

    public final String name;
    public final String category;
    public final float freshMin;
    public final float freshMax;
    public final List<float[]> spoiledRanges; // each entry = {min, max}
    public final boolean pHUnreliable;         // true when the source notes pH alone can't
    // reliably tell fresh from spoiled for this food
    public final String reliabilityNote;       // shown to the user when pHUnreliable == true

    public FoodItem(String name, String category, float freshMin, float freshMax,
                    List<float[]> spoiledRanges, boolean pHUnreliable, String reliabilityNote) {
        this.name = name;
        this.category = category;
        this.freshMin = freshMin;
        this.freshMax = freshMax;
        this.spoiledRanges = spoiledRanges;
        this.pHUnreliable = pHUnreliable;
        this.reliabilityNote = reliabilityNote;
    }

    private boolean inRange(float pH, float min, float max) {
        return pH >= min && pH <= max;
    }

    public boolean isFreshRange(float pH) {
        return inRange(pH, freshMin, freshMax);
    }

    public boolean isSpoiledRange(float pH) {
        for (float[] r : spoiledRanges) {
            if (inRange(pH, r[0], r[1])) return true;
        }
        return false;
    }

    /**
     * Classifies a measured pH against THIS food's fresh/spoiled ranges only.
     * UNCERTAIN covers: falls in both ranges (common for foods where the table
     * itself shows overlapping ranges), or falls in neither.
     */
    public Freshness classify(float pH) {
        boolean fresh = isFreshRange(pH);
        boolean spoiled = isSpoiledRange(pH);
        if (fresh && !spoiled) return Freshness.FRESH;
        if (spoiled && !fresh) return Freshness.SPOILED;
        return Freshness.UNCERTAIN;
    }

    public String freshRangeLabel() {
        return String.format(Locale.US, "%.1f–%.1f", freshMin, freshMax);
    }

    public String spoiledRangeLabel() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spoiledRanges.size(); i++) {
            float[] r = spoiledRanges.get(i);
            if (i > 0) sb.append(" / ");
            if (r[1] >= 14f) {
                sb.append(String.format(Locale.US, ">%.1f", r[0]));
            } else {
                sb.append(String.format(Locale.US, "%.1f–%.1f", r[0], r[1]));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}