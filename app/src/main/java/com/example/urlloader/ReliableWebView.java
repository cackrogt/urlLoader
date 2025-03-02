package com.example.urlloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ReliableWebView {
    private WebView webView;
    private Context context;
    private ResultCallback callback;
    private String lastUrl = "";
    private boolean jsRedirectDetected = false;

    public ReliableWebView(Context context) {
        this.context = context;
    }

    public void loadUrl(String url, ResultCallback callback) {
        this.callback = callback;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (webView == null) {
                webView = new WebView(context);
            }
            setupWebView();
            webView.loadUrl(url);
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);  // Disable caching

        webView.addJavascriptInterface(new JsRedirectDetector(), "JSBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                lastUrl = request.getUrl().toString();
                Log.d("ReliableWebView", "HTTP Redirect: " + lastUrl);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("ReliableWebView", "Page Loaded: " + url);
                new Handler().postDelayed(() -> {
                    if (!jsRedirectDetected) {
                        detectJavaScriptRedirect(view);
                    } else {
                        callback.onResult(lastUrl.isEmpty() ? url : lastUrl);
                        destroyWebView();
                    }
                }, 2000);  // Wait 2 seconds for JS redirects
            }
        });
    }

    private void detectJavaScriptRedirect(WebView view) {
        Log.d("ReliableWebView", "Checking for JS redirects...");
        view.evaluateJavascript(
                "(function() {" +
                        "  let observer = new MutationObserver((mutations) => {" +
                        "    JSBridge.onRedirect(document.location.href);" +
                        "  });" +
                        "  observer.observe(document.body, { childList: true, subtree: true });" +
                        "})();",
                null
        );
    }

    private class JsRedirectDetector {
        @JavascriptInterface
        public void onRedirect(String newUrl) {
            if (!newUrl.equals(lastUrl)) {
                jsRedirectDetected = true;
                lastUrl = newUrl;
                Log.d("ReliableWebView", "JS Redirect detected: " + newUrl);
                new Handler(Looper.getMainLooper()).post(() -> webView.loadUrl(newUrl));
            }
        }
    }

    private void destroyWebView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

    public interface ResultCallback {
        void onResult(String finalUrl);
    }
}

