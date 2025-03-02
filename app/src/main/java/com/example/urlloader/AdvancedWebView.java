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

public class AdvancedWebView {
    private WebView webView;
    private Context context;
    private ResultCallback callback;
    private String lastUrl = "";
    private boolean jsRedirectDetected = false;

    public AdvancedWebView(Context context) {
        this.context = context;
    }

    public void loadUrl(String url, ResultCallback callback) {
        this.callback = callback;
        new Handler(Looper.getMainLooper()).post(() -> {
            webView = new WebView(context);
            setupWebView();
            webView.loadUrl(url);
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.addJavascriptInterface(new JsRedirectDetector(), "JSBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                lastUrl = request.getUrl().toString();
                Log.d("Glimpse", "HTTP Redirect: " + lastUrl);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!jsRedirectDetected) {
                    detectJavaScriptRedirect(view);
                } else {
                    callback.onResult(lastUrl.isEmpty() ? url : lastUrl);
                    destroyWebView();
                }
            }
        });
    }

    private void detectJavaScriptRedirect(WebView view) {
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

