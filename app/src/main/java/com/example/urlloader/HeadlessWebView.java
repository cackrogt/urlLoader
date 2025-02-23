package com.example.urlloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HeadlessWebView {
    private WebView webView;
    private Context context;
    private ResultCallback callback;

    public HeadlessWebView(Context context) {
        this.context = context;
    }

    public void loadUrl(String url, ResultCallback callback) {
        this.callback = callback;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            webView = new WebView(context);
            setupWebView();
            webView.loadUrl(url);
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String newUrl = request.getUrl().toString();
                Log.i("HeadlessWebView", "Redirect detected: " + newUrl);
                view.loadUrl(newUrl);  // Follow JavaScript-based redirects
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.i("HeadlessWebView", "Final URL: " + url);

                // Extract text from the page
                view.evaluateJavascript(
                        "(function() { return document.body.innerText; })();",
                        text -> {
                            Log.i("ExtractedText", "Page Text: " + text);
                            callback.onResult(url, text);
                            destroyWebView();
                        }
                );
            }
        });
    }

    private void destroyWebView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

    public interface ResultCallback {
        void onResult(String finalUrl, String extractedText);
    }
}

