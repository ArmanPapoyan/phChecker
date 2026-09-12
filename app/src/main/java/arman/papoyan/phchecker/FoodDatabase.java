package arman.papoyan.phchecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FoodDatabase {

    public static final String CAT_FISH = "Fish 🐟";
    public static final String CAT_BREAD = "Bread 🍞";
    public static final String CAT_DAIRY = "Dairy Products 🧀";
    public static final String CAT_MEAT = "Meat 🥩";
    public static final String CAT_VEG = "Vegetables 🥦";
    public static final String CAT_FRUIT = "Fruits 🍎";

    public static final List<FoodItem> ALL = new ArrayList<>();

    private static final float POINT_TOLERANCE = 0.25f;
    private static final float SCALE_MAX = 14f;

    private static float[] r(float min, float max) { return new float[]{min, max}; }
    private static float[] pt(float value) { return new float[]{value - POINT_TOLERANCE, value + POINT_TOLERANCE}; }
    private static float[] above(float min) { return new float[]{min, SCALE_MAX}; }
    private static List<float[]> ranges(float[]... rs) { return Arrays.asList(rs); }

    private static void add(String name, String category, float freshMin, float freshMax,
                            List<float[]> spoiled, boolean unreliable, String note) {
        ALL.add(new FoodItem(name, category, freshMin, freshMax, spoiled, unreliable, note));
    }

    private static void addPointFresh(String name, String category, float freshPoint,
                                      List<float[]> spoiled, boolean unreliable, String note) {
        float[] fr = pt(freshPoint);
        add(name, category, fr[0], fr[1], spoiled, unreliable, note);
    }

    private static final String MOLD_NOTE =
            "pH does not indicate mold spoilage — check visually and by smell.";
    private static final String GENERIC_NOTE =
            "In this table, the fresh and spoiled ranges overlap or are very close — " +
                    "pH alone is unreliable here, rely also on smell, texture, and appearance.";

    static {
        // Fish
        add("🐟 Fish (most species)", CAT_FISH, 6.6f, 6.8f, ranges(r(7f, 7.8f)), false, null);

        // Bread
        add("🍞 White bread", CAT_BREAD, 5f, 6.2f, ranges(r(6.5f, 7.5f)), true, MOLD_NOTE);
        add("🍞 Whole grain bread", CAT_BREAD, 5.47f, 5.85f, ranges(r(6.5f, 7.5f)), true, MOLD_NOTE);

        // Dairy Products
        add("🧀 Hard cheeses (Cheddar, Parmesan, Swiss)", CAT_DAIRY, 5.2f, 5.9f, ranges(above(7f), r(4f, 4.8f)), false, null);
        add("🧀 Soft cheeses (Ricotta, Cottage cheese)", CAT_DAIRY, 4.1f, 5.02f, ranges(above(6.5f)), false, null);
        add("🧈 Butter", CAT_DAIRY, 6.1f, 6.4f, ranges(r(4f, 5f), pt(7f)), false, null);
        add("🥛 Milk (cow and goat)", CAT_DAIRY, 6.4f, 6.8f, ranges(r(4f, 4.5f)), false, null);
        addPointFresh("🥛 Peptonized milk", CAT_DAIRY, 7.1f, ranges(r(7.5f, 8.5f)), false, null);
        addPointFresh("🥛 Condensed milk", CAT_DAIRY, 6.3f, ranges(r(4.2f, 5f), above(7f)), false, null);

        // Meat
        add("🥩 Beef", CAT_MEAT, 5.1f, 6.2f, ranges(r(7f, 7.8f)), false, null);
        add("🍗 Chicken", CAT_MEAT, 6.2f, 6.4f, ranges(r(7f, 7.8f)), false, null);
        addPointFresh("🥓 Pork", CAT_MEAT, 5.7f, ranges(r(7f, 7.8f)), false, null);
        addPointFresh("🐟 White fish", CAT_MEAT, 5.5f, ranges(r(7f, 7.8f)), false, null);

        // Vegetables
        add("🌱 Asparagus (shoots and stalks)", CAT_VEG, 5.7f, 6.1f, ranges(r(6.5f, 7.5f)), false, null);
        add("🫘 Beans (green and lima)", CAT_VEG, 4.6f, 6.5f, ranges(r(6.5f, 7.5f)), true, GENERIC_NOTE);
        add("🪴 Sugar beets", CAT_VEG, 4.2f, 4.4f, ranges(pt(3.5f), r(6.5f, 7.2f)), false, null);
        addPointFresh("🥦 Broccoli", CAT_VEG, 6.5f, ranges(r(7f, 7.8f)), false, null);
        addPointFresh("🥬 Brussels sprouts", CAT_VEG, 6.3f, ranges(r(7f, 7.8f), r(4.5f, 5.5f)), false, null);
        add("🥬 Cabbage (white)", CAT_VEG, 5.4f, 6.0f, ranges(r(6.5f, 7.5f), r(3.8f, 4.5f)), false, null);
        add("🥕 Carrot", CAT_VEG, 5.88f, 6.4f, ranges(r(6.5f, 7.5f), pt(4f)), true, GENERIC_NOTE);
        add("🥕 Cooked carrot", CAT_VEG, 5.58f, 6.03f, ranges(r(6.5f, 7.5f), pt(4f)), false, null);
        add("🥬 Celery", CAT_VEG, 5.7f, 6.0f, ranges(r(6.5f, 7.5f), r(4f, 5f)), false, null);
        addPointFresh("🥒 Cucumbers", CAT_VEG, 3.8f, ranges(r(5.5f, 6.8f)), false, null);
        addPointFresh("🍆 Eggplant", CAT_VEG, 4.5f, ranges(r(6f, 7.2f)), false, null);
        addPointFresh("🥬 Lettuce", CAT_VEG, 6.0f, ranges(r(7f, 7.5f)), false, null);
        add("🫒 Olives", CAT_VEG, 3.6f, 3.8f, ranges(above(3f)), false, null);
        add("🧅 Red onion", CAT_VEG, 5.3f, 5.8f, ranges(r(6.5f, 7.5f)), false, null);
        add("🌿 Parsley", CAT_VEG, 5.7f, 6.0f, ranges(r(6.5f, 7.5f)), false, null);
        addPointFresh("🥕 Parsnip", CAT_VEG, 5.3f, ranges(r(6.5f, 7.5f)), false, null);
        add("🥔 Potatoes (regular and sweet)", CAT_VEG, 5.3f, 5.6f, ranges(r(6.5f, 7.8f)), false, null);
        add("🎃 Pumpkin", CAT_VEG, 4.8f, 5.2f, ranges(r(6.2f, 7.2f)), false, null);
        add("🌱 Rhubarb", CAT_VEG, 3.1f, 3.4f, ranges(r(3f, 3.8f)), true, GENERIC_NOTE);
        addPointFresh("🥔 Rutabaga", CAT_VEG, 6.3f, ranges(r(6.5f, 7.5f)), true, GENERIC_NOTE);
        add("🥬 Spinach", CAT_VEG, 5.5f, 6.0f, ranges(r(6.5f, 7.5f)), false, null);
        add("🥒 Squash / Zucchini", CAT_VEG, 5.0f, 5.4f, ranges(r(6.2f, 7.2f)), false, null);
        add("🍅 Tomatoes (whole)", CAT_VEG, 4.2f, 4.3f, ranges(r(4f, 4.8f)), true, GENERIC_NOTE);
        add("🧅 Turnip", CAT_VEG, 5.2f, 5.5f, ranges(r(6.5f, 7.5f), r(4f, 5f)), false, null);

        // Fruits
        add("🍎 Apples", CAT_FRUIT, 2.9f, 3.3f, ranges(above(3.8f)), true, GENERIC_NOTE);
        add("🍉 Watermelons", CAT_FRUIT, 5.2f, 5.6f, ranges(r(6f, 7.2f), r(4f, 4.8f)), false, null);
        add("🍑 Plums", CAT_FRUIT, 2.8f, 4.6f, ranges(r(2.8f, 3.8f)), true, GENERIC_NOTE);
        add("🍊 Oranges (juice)", CAT_FRUIT, 3.6f, 4.3f, ranges(r(3f, 3.8f)), true, GENERIC_NOTE);
        add("🍈 Honeydew melon", CAT_FRUIT, 6.3f, 6.7f, ranges(r(6.5f, 7.5f)), true, GENERIC_NOTE);
        add("🍋 Limes", CAT_FRUIT, 1.8f, 2.0f, ranges(r(1.8f, 2f)), true, GENERIC_NOTE);
        add("🍇 Grapes", CAT_FRUIT, 3.4f, 4.5f, ranges(above(3f)), true, GENERIC_NOTE);
        addPointFresh("🍊 Grapefruit (juice)", CAT_FRUIT, 3.0f, ranges(r(2.5f, 3f)), true, GENERIC_NOTE);
        add("🍌 Bananas", CAT_FRUIT, 4.5f, 4.7f, ranges(r(4f, 4.8f)), true, GENERIC_NOTE);
        add("🧃 Apple juice", CAT_FRUIT, 3.3f, 4.1f, ranges(r(2.8f, 3.5f)), true, GENERIC_NOTE);
        add("🧃 Apple cider", CAT_FRUIT, 3.6f, 3.8f, ranges(r(3f, 3.6f)), true, GENERIC_NOTE);
    }

    public static FoodItem findByName(String name) {
        if (name == null) return null;
        for (FoodItem item : ALL) {
            if (item.name.equals(name)) return item;
        }
        return null;
    }
}