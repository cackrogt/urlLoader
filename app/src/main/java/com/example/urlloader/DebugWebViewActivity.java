package com.example.urlloader;

import static java.lang.Math.min;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class DebugWebViewActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
//        setContentView(webView); // Show WebView

        setupWebView();

        String url = getIntent().getStringExtra("URL_TO_LOAD");
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d("Glimpse", "intent DebugWebView Redirect to: " + request.getUrl());
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("Glimpse", "intent DebugWebView Page Loaded: " + url);
                injectJavaScript();
            }
        });
        webView.addJavascriptInterface(new JavaScriptInterface(), "JSBridge");
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
            Log.d("WebViewActivity", " intent Extracted Text: " + text);

            // Update UI with extracted text
            runOnUiThread(() -> Log.i("Glimpse", "intent extracted text is: " +
                    text.substring(0, min(text.length(), 5000))));
        }
    }
}

