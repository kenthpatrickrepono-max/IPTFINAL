package com.example.spendsmart;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;
import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText etAmount, etDescription, etDate;
    private Spinner spinnerCategory;
    private Button btnSave;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private final List<Category> categoryList = new ArrayList<>();
    private int selectedCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        sessionManager = new SessionManager(this);

        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Set today's date as default
        Calendar cal = Calendar.getInstance();
        etDate.setText(String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)));

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dp, y, m, d) ->
                    etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        loadCategories();

        btnSave.setOnClickListener(v -> saveExpense());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Expense");
        }
    }

    private void loadCategories() {
        RetrofitClient.getApiService().getCategories(sessionManager.getToken())
                .enqueue(new Callback<CategoriesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CategoriesResponse> call, @NonNull Response<CategoriesResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            categoryList.clear();
                            categoryList.addAll(response.body().categories);

                            ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                                    AddExpenseActivity.this,
                                    android.R.layout.simple_spinner_item,
                                    categoryList
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerCategory.setAdapter(adapter);

                            spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                                    selectedCategoryId = categoryList.get(pos).id;
                                }
                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CategoriesResponse> call, @NonNull Throwable t) {
                        Toast.makeText(AddExpenseActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (amountStr.isEmpty() || date.isEmpty() || selectedCategoryId == -1) {
            Toast.makeText(this, "Please fill in amount, category and date", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid positive amount", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("category_id", selectedCategoryId);
        body.put("description", description);
        body.put("expense_date", date);

        RetrofitClient.getApiService().addExpense(sessionManager.getToken(), body)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(AddExpenseActivity.this, "Expense added!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddExpenseActivity.this, "Failed to add expense", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(AddExpenseActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
