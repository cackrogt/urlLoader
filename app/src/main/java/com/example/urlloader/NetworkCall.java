package com.example.urlloader;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkCall {

    OkHttpClient client = new OkHttpClient();
    Request request;
    public NetworkCall() {
        client = new OkHttpClient();
    }

    public void makeNetworkCall(URL url, CallbackOnNetworkResponse caller) {
        request = new Request.Builder()
                .url(url == null ? "https://google.com": url.toString())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                String responseData = getTextData(response);
                // Process the response data
                Log.i("NetworkCall", responseData);
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> caller.onSuccess(responseData), 2000
                );
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> caller.onFailure(e), 2000
                );
            }
        });
    }

    public String getTextData(Response response) throws IOException {
        String html = response.body().string();  // Get raw HTML
        Document doc = Jsoup.parse(html);        // Parse HTML
        String text = doc.text();
        return text;
    }
    public interface CallbackOnNetworkResponse{
        void onSuccess(String val);
        void onFailure(Exception e);
    }
}
