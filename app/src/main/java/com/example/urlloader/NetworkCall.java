package com.example.urlloader;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    public void checkIfRedirected(URL url, CallbackOnNetworkResponse caller) {
        String initialUrl = url.toString(); // Replace with actual URL
        AtomicReference<String> finalUrl = new AtomicReference<>();
        ExecutorService exec = Executors.newSingleThreadExecutor();

            exec.execute(
                    () ->{
                        finalUrl.set(getFinalUrl(initialUrl));
                        new Handler(Looper.getMainLooper()).postDelayed(
                                ()->makeNetworkCall(finalUrl.get(), caller), 2000
                        );
                    }
            );
    }
    public void makeNetworkCall(String url, CallbackOnNetworkResponse caller) {

        request = new Request.Builder()
                .url(url == null ? "https://google.com": url)
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

    public String getFinalUrl(String url) {
        Request request = new Request.Builder().url(url).build();
        String html = "";
        try {
            Response response = client.newCall(request).execute();
            html = response.body().string();
        }
        catch (Exception e) {
            html = "";
        }
        // Check for <meta http-equiv="refresh" content="3;url=new_url">
        Document doc = Jsoup.parse(html);
        Element metaRefresh = doc.selectFirst("meta[http-equiv=refresh]");
        if (metaRefresh != null) {
            String content = metaRefresh.attr("content");
            Pattern pattern = Pattern.compile("\\d+;\\s*url=(.+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        // Check for JavaScript redirects (window.location.href = 'new_url')
        Pattern jsPattern = Pattern.compile(
                "window\\.location\\.(href|replace|assign)\\s*=\\s*['\"](.*?)['\"]",
                Pattern.CASE_INSENSITIVE);
        Matcher jsMatcher = jsPattern.matcher(html);
        if (jsMatcher.find()) {
            return jsMatcher.group(2).trim();
        }

        // If no redirect is found, return the original URL
        return url;
    }

    public interface CallbackOnNetworkResponse{
        void onSuccess(String val);
        void onFailure(Exception e);
    }
}
