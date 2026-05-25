package com.example.spendsmart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements ExpenseAdapter.OnExpenseDeleteListener {

    private TextView tvWelcome, tvTotal, tvEmpty;
    private RecyclerView rvExpenses;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private ExpenseAdapter adapter;
    private final List<Expense> expenseList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvTotal = view.findViewById(R.id.tvTotal);
        rvExpenses = view.findViewById(R.id.rvExpenses);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        tvWelcome.setText("Welcome, " + sessionManager.getUsername() + "!");

        adapter = new ExpenseAdapter(expenseList, this);
        rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvExpenses.setAdapter(adapter);

        view.findViewById(R.id.btnAddExpense).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddExpenseActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void loadExpenses() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        RetrofitClient.getApiService().getExpenses(sessionManager.getToken())
                .enqueue(new Callback<ExpensesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ExpensesResponse> call, @NonNull Response<ExpensesResponse> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            ExpensesResponse data = response.body();
                            expenseList.clear();
                            expenseList.addAll(data.expenses);
                            adapter.notifyDataSetChanged();

                            tvTotal.setText(String.format(Locale.US, "Total Spent: $%.2f", data.total));

                            if (expenseList.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            }
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
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDelete(Expense expense, int position) {
        RetrofitClient.getApiService().deleteExpense(sessionManager.getToken(), expense.id)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                            loadExpenses();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
