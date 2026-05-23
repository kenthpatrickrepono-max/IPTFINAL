package com.example.spendsmart;

import com.google.gson.annotations.SerializedName;

public class Expense {
    public int id;
    public double amount;
    public String description;
    public String category;

    @SerializedName("expense_date")
    public String expenseDate;

    @SerializedName("created_at")
    public String createdAt;
}
