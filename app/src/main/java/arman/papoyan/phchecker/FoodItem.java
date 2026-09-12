package arman.papoyan.phchecker;

import java.util.List;
import java.util.Locale;

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
    public final List<float[]> spoiledRanges;
    public final boolean pHUnreliable;
    public final String reliabilityNote;

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