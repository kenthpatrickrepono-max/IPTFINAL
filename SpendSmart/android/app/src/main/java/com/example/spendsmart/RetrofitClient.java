package com.example.spendsmart;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // ⚠️ REPLACE this with the HTTPS URL of your deployed Render web service.
    // After deploying (see SpendSmart/backend/DEPLOY.md) Render gives you a URL
    // like https://spendsmart-api.onrender.com — keep the trailing slash.
    private static final String BASE_URL = "https://spendsmart-api-uz2n.onrender.com/";

    // Local testing alternative (Flask running on your machine):
    //   - Android emulator:  "http://10.0.2.2:5000/"
    //   - Real device:       "http://<your-computer-LAN-IP>:5000/"
    // Note: plain http requires android:usesCleartextTraffic="true" in the manifest.

    private static Retrofit retrofitInstance = null;
    private static ApiService apiServiceInstance = null;

    public static Retrofit getInstance() {
        if (retrofitInstance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitInstance;
    }

    public static ApiService getApiService() {
        if (apiServiceInstance == null) {
            apiServiceInstance = getInstance().create(ApiService.class);
        }
        return apiServiceInstance;
    }
}
