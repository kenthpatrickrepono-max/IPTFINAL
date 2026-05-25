package com.example.spendsmart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public interface OnExpenseDeleteListener {
        void onDelete(Expense expense, int position);
    }

    private final List<Expense> expenses;
    private final OnExpenseDeleteListener listener;

    public ExpenseAdapter(List<Expense> expenses, OnExpenseDeleteListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.tvAmount.setText(String.format(Locale.US, "$%.2f", expense.amount));
        holder.tvCategory.setText(expense.category);
        holder.tvDescription.setText(expense.description == null || expense.description.isEmpty()
                ? "No description" : expense.description);
        holder.tvDate.setText(expense.expenseDate);

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onDelete(expense, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvCategory, tvDescription, tvDate;
        ImageButton btnDelete;

        ViewHolder(View view) {
            super(view);
            tvAmount = view.findViewById(R.id.tvAmount);
            tvCategory = view.findViewById(R.id.tvCategory);
            tvDescription = view.findViewById(R.id.tvDescription);
            tvDate = view.findViewById(R.id.tvDate);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }
}
