package arman.papoyan.phchecker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * First screen the user sees: pick a food from the fresh/spoiled pH reference
 * table. Only after a product is chosen does the camera / measurement screen
 * (MainActivity) open.
 */
public class ProductSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_NAME = "arman.papoyan.phchecker.FOOD_NAME";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> allRows = new ArrayList<>();   // full list, grouped by category
    private final List<Object> shownRows = new ArrayList<>(); // currently visible (filtered) rows
    private RowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_selection);

        ListView listView = findViewById(R.id.foodListView);
        EditText searchBox = findViewById(R.id.searchBox);

        buildGroupedRows();

        adapter = new RowAdapter();
        listView.setAdapter(adapter);
        applyFilter("");

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Object row = shownRows.get(position);
            if (row instanceof FoodItem) {
                openMeasurement((FoodItem) row);
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void buildGroupedRows() {
        String currentCategory = null;
        for (FoodItem item : FoodDatabase.ALL) {
            if (!item.category.equals(currentCategory)) {
                currentCategory = item.category;
                allRows.add(currentCategory); // header row
            }
            allRows.add(item);
        }
    }

    private void applyFilter(String query) {
        shownRows.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            shownRows.addAll(allRows);
        } else {
            // while searching, show a flat matching list without headers
            for (FoodItem item : FoodDatabase.ALL) {
                if (item.name.toLowerCase(Locale.ROOT).contains(q)) {
                    shownRows.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void openMeasurement(FoodItem item) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_FOOD_NAME, item.name);
        startActivity(intent);
    }

    private class RowAdapter extends BaseAdapter {

        @Override public int getCount() { return shownRows.size(); }
        @Override public Object getItem(int position) { return shownRows.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public int getViewTypeCount() { return 2; }

        @Override
        public int getItemViewType(int position) {
            return (shownRows.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == TYPE_ITEM;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object row = shownRows.get(position);

            if (getItemViewType(position) == TYPE_HEADER) {
                TextView headerView = (TextView) (convertView != null
                        ? convertView
                        : LayoutInflater.from(ProductSelectionActivity.this)
                        .inflate(R.layout.item_food_header, parent, false));
                headerView.setText((String) row);
                return headerView;
            }

            View itemView = convertView != null
                    ? convertView
                    : LayoutInflater.from(ProductSelectionActivity.this)
                    .inflate(R.layout.item_food, parent, false);

            FoodItem food = (FoodItem) row;
            TextView nameView = itemView.findViewById(R.id.foodNameText);
            TextView rangeView = itemView.findViewById(R.id.foodRangeText);

            nameView.setText(food.name);
            String rangeText = "Fresh: " + food.freshRangeLabel() + "   Spoiled: " + food.spoiledRangeLabel();
            if (food.pHUnreliable) {
                rangeText += "  ⚠️";
            }
            rangeView.setText(rangeText);

            return itemView;
        }
    }
}