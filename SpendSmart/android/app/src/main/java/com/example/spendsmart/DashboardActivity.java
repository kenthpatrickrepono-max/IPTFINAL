package com.example.spendsmart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseDeleteListener {

    private TextView tvWelcome, tvTotal;
    private RecyclerView rvExpenses;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SessionManager sessionManager;
    private ExpenseAdapter adapter;
    private List<Expense> expenseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvTotal = findViewById(R.id.tvTotal);
        rvExpenses = findViewById(R.id.rvExpenses);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        tvWelcome.setText("Welcome, " + sessionManager.getUsername() + "!");

        adapter = new ExpenseAdapter(expenseList, this);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(adapter);

        findViewById(R.id.btnAddExpense).setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void loadExpenses() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        RetrofitClient.getApiService().getExpenses(sessionManager.getToken())
                .enqueue(new Callback<ExpensesResponse>() {
                    @Override
                    public void onResponse(Call<ExpensesResponse> call, Response<ExpensesResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            ExpensesResponse data = response.body();
                            expenseList.clear();
                            expenseList.addAll(data.expenses);
                            adapter.notifyDataSetChanged();

                            tvTotal.setText(String.format("Total Spent: $%.2f", data.total));

                            if (expenseList.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            }
                        } else if (response.code() == 401) {
                            // Token expired — redirect to login
                            sessionManager.clearSession();
                            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<ExpensesResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(DashboardActivity.this, "Failed to load expenses", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDelete(Expense expense, int position) {
        RetrofitClient.getApiService().deleteExpense(sessionManager.getToken(), expense.id)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(DashboardActivity.this, "Expense deleted", Toast.LENGTH_SHORT).show();
                            loadExpenses(); // Refresh list and total
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Toast.makeText(DashboardActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
