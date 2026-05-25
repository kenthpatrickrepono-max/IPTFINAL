package com.example.spendsmart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackingFragment extends Fragment {

    private static final int[] BAR_COLORS = {
            Color.parseColor("#1976D2"),
            Color.parseColor("#43A047"),
            Color.parseColor("#FB8C00"),
            Color.parseColor("#8E24AA"),
            Color.parseColor("#E53935"),
            Color.parseColor("#00ACC1"),
            Color.parseColor("#FDD835"),
            Color.parseColor("#6D4C41")
    };

    private SessionManager sessionManager;
    private TextView tvTrackingTotal, tvTrackingEmpty;
    private ProgressBar trackingProgress;
    private LinearLayout categoryContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        tvTrackingTotal = view.findViewById(R.id.tvTrackingTotal);
        tvTrackingEmpty = view.findViewById(R.id.tvTrackingEmpty);
        trackingProgress = view.findViewById(R.id.trackingProgress);
        categoryContainer = view.findViewById(R.id.categoryContainer);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTracking();
    }

    private void loadTracking() {
        trackingProgress.setVisibility(View.VISIBLE);
        tvTrackingEmpty.setVisibility(View.GONE);
        categoryContainer.removeAllViews();

        RetrofitClient.getApiService().getExpenses(sessionManager.getToken())
                .enqueue(new Callback<ExpensesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ExpensesResponse> call, @NonNull Response<ExpensesResponse> response) {
                        if (!isAdded()) return;
                        trackingProgress.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            renderTracking(response.body());
                        } else if (response.code() == 401) {
                            sessionManager.clearSession();
                            Intent intent = new Intent(requireContext(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ExpensesResponse> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        trackingProgress.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed to load tracking data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderTracking(ExpensesResponse data) {
        tvTrackingTotal.setText(String.format(Locale.US, "Total: $%.2f", data.total));

        if (data.expenses == null || data.expenses.isEmpty()) {
            tvTrackingEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // Aggregate amounts by category
        Map<String, Double> byCategory = new HashMap<>();
        for (Expense e : data.expenses) {
            String cat = e.category == null ? "Uncategorized" : e.category;
            Double prev = byCategory.get(cat);
            byCategory.put(cat, (prev == null ? 0.0 : prev) + e.amount);
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(byCategory.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
                return Double.compare(b.getValue(), a.getValue());
            }
        });

        double maxAmount = 0.0;
        for (Map.Entry<String, Double> e : entries) {
            if (e.getValue() > maxAmount) maxAmount = e.getValue();
        }
        double total = data.total > 0 ? data.total : 1.0;

        int colorIdx = 0;
        for (Map.Entry<String, Double> e : entries) {
            addCategoryRow(e.getKey(), e.getValue(), total, maxAmount, BAR_COLORS[colorIdx % BAR_COLORS.length]);
            colorIdx++;
        }
    }

    private void addCategoryRow(String name, double amount, double total, double maxAmount, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(12);
        row.setLayoutParams(rowLp);

        // Label line: category name + amount + percentage
        LinearLayout labelRow = new LinearLayout(requireContext());
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvName = new TextView(requireContext());
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameLp);
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#424242"));
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);

        TextView tvAmount = new TextView(requireContext());
        double pct = (amount / total) * 100.0;
        tvAmount.setText(String.format(Locale.US, "$%.2f  (%.1f%%)", amount, pct));
        tvAmount.setTextColor(Color.parseColor("#616161"));
        tvAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        labelRow.addView(tvName);
        labelRow.addView(tvAmount);

        // Bar background
        LinearLayout barBg = new LinearLayout(requireContext());
        LinearLayout.LayoutParams barBgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(14));
        barBgLp.topMargin = dp(4);
        barBg.setLayoutParams(barBgLp);
        barBg.setBackgroundColor(Color.parseColor("#E0E0E0"));

        // Filled bar
        View filled = new View(requireContext());
        float fraction = maxAmount > 0 ? (float) (amount / maxAmount) : 0f;
        if (fraction < 0.02f) fraction = 0.02f; // ensure visible sliver
        LinearLayout.LayoutParams filledLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, fraction);
        filled.setLayoutParams(filledLp);
        filled.setBackgroundColor(color);

        View spacer = new View(requireContext());
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(0f, 1f - fraction));
        spacer.setLayoutParams(spacerLp);

        barBg.addView(filled);
        barBg.addView(spacer);

        row.addView(labelRow);
        row.addView(barBg);

        categoryContainer.addView(row);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
