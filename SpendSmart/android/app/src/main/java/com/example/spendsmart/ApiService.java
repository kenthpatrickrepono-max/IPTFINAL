package com.example.spendsmart;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("api/register")
    Call<AuthResponse> register(@Body Map<String, String> body);

    @POST("api/login")
    Call<AuthResponse> login(@Body Map<String, String> body);

    @GET("api/categories")
    Call<CategoriesResponse> getCategories(@Header("Authorization") String token);

    @GET("api/expenses")
    Call<ExpensesResponse> getExpenses(@Header("Authorization") String token);

    @POST("api/expenses")
    Call<MessageResponse> addExpense(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @DELETE("api/expenses/{id}")
    Call<MessageResponse> deleteExpense(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @PUT("api/expenses/{id}")
    Call<MessageResponse> updateExpense(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );
}
