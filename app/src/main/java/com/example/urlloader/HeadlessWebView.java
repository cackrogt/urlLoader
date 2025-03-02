package com.example.urlloader;

import static java.lang.Math.min;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HeadlessWebView {
    private WebView webView;
    private Context context;
    private ResultCallback callback;
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_MS = 10000;
    private int retryCount = 0;
    private boolean pageLoaded = false;

    public HeadlessWebView(Context context) {
        this.context = context;
    }

    public void loadUrl(String url, ResultCallback callback) {
        pageLoaded = false;

        // Start a timeout for retry

        this.callback = callback;
        Handler handler = new Handler(Looper.getMainLooper());
        Log.i("Glimpse", "we start the load for url");
        handler.postDelayed(() ->{
//            if(webView == null) {
//                webView = new WebView(context);
//            }
            webView = ((Activity)context).findViewById(R.id.myWebView);
            webView.setVisibility(WebView.INVISIBLE);
            setupWebView();
            webView.loadUrl(url);
            webView.onResume();
        }, 100);
        handler.postDelayed(() -> {
            if (!pageLoaded && retryCount < MAX_RETRIES) {
                retryCount++;
                Log.i("Glimpse", "Retrying... Attempt: " + retryCount);
                loadUrl(url, callback);
            } else if (!pageLoaded) {
                Log.i("Glimpse", "Page failed to load after " + MAX_RETRIES + " retries.");
            }
        }, TIMEOUT_MS);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);


        webView.setWebChromeClient(new WebChromeClient());
        Log.i("Glimpse", "we start the load for url");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.i("Glimpse", "Redirect detected: " + view.getUrl());
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i("Glimpse", "Page Loaded: " + url);
                pageLoaded = true;
                injectJavaScript();
            }
        });
        webView.addJavascriptInterface(new JavaScriptInterface(), "JSBridge");
    }

    private void destroyWebView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

    private void injectJavaScript() {
        String jsCode = "(function () {" +
                "function extractTextFromElement(element) {" +
                "  if (!element) return '';" +
                "  let text = '';" +
                "  if (element.nodeType === Node.TEXT_NODE) {" +
                "    text = element.nodeValue.trim();" +
                "  } else if (element.nodeType === Node.ELEMENT_NODE && element.tagName !== 'SCRIPT' && element.tagName !== 'STYLE') {" +
                "    let style = window.getComputedStyle(element);" +
                "    if (style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0') {" +
                "      text = Array.from(element.childNodes).map(extractTextFromElement).join(' ');" +
                "    }" +
                "  }" +
                "  return text;" +
                "}" +
                "function getTextFromMainDocument() {" +
                "  return extractTextFromElement(document.body);" +
                "}" +
                "async function getTextFromIframes() {" +
                "  let iframeTexts = await Promise.all(Array.from(document.querySelectorAll('iframe')).map(async (iframe) => {" +
                "    try {" +
                "      let doc = iframe.contentDocument || iframe.contentWindow.document;" +
                "      if (doc) {" +
                "        return extractTextFromElement(doc.body);" +
                "      }" +
                "    } catch (e) { return ''; }" +
                "    return '';" +
                "  }));" +
                "  return iframeTexts.join(' ');" +
                "}" +
                "async function getAllTextContent() {" +
                "  await new Promise(resolve => setTimeout(resolve, 3000));" + // Delay to allow dynamic content
                "  let mainText = getTextFromMainDocument();" +
                "  let iframeText = await getTextFromIframes();" +
                "  return mainText + ' ' + iframeText;" +
                "}" +
                "getAllTextContent().then(text => {" +
                "  console.log('Extracted Text:', text);" +
                "  JSBridge.onExtractedText(text);" +  // Pass text to Android
                "});" +
                "})();";

        webView.evaluateJavascript(jsCode, null);
    }

    private class JavaScriptInterface {
        @android.webkit.JavascriptInterface
        public void onExtractedText(String text) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(
                    () -> {
                        Log.i("Glimpse", "Extracted Text: "
                                + text.substring(0, min(text.length(), 5000)));

                        callback.onResult("https://google.com",
                                text.substring(0, min(text.length(), 5000)));
                        destroyWebView();
                    }, 100
            );

            //destroyWebView();
        }
    }

    public interface ResultCallback {
        void onResult(String finalUrl, String extractedText);
    }
}

