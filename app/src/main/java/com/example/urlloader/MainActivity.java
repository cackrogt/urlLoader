package com.example.urlloader;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    private TextView outputText;
    private ScrollView scrollView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextInputEditText inputText = findViewById(R.id.inputText);
        outputText = findViewById(R.id.outputText);
        outputText.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        scrollView = findViewById(R.id.scrollView);
        Button actionButton = findViewById(R.id.actionButton);
        Context context = this;
        NetworkCall.CallbackOnNetworkResponse caller = new NetworkCall.CallbackOnNetworkResponse() {
            @Override
            public void onSuccess(String val) {
                outputText.setText(val);
                scrollToBottom();
            }

            @Override
            public void onFailure(Exception e) {
                outputText.setText("Error "+ e.getMessage());
            }
        };
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Empty onClickListener
                String inputLink = inputText.getText().toString().trim();
                outputText.setText(inputLink);
                NetworkCall net = new NetworkCall();
                URL url = null;
                try {
                    url = new URL(inputLink);
                } catch (MalformedURLException e) {
                    Log.i("MainAct", "we throw error during url creation");
                }
                AtomicReference<String> finUrl = new AtomicReference<>();
                HeadlessWebView headlessWebView = new HeadlessWebView(context);
                headlessWebView.loadUrl(url.toString(), (finalUrl, extractedText) -> {
                    Log.i("Glimpse FinalResult", "Final URL: " + finalUrl);
                    Log.i("Glimpse FinalResult", "Extracted Text: " + extractedText);
                    finUrl.set(finalUrl);
                    try {
                        net.checkIfRedirected(new URL(finUrl.get()), caller);
                    } catch (MalformedURLException e) {
                        Log.i("Glimpse", "did not work");
                    }
                });


            }
        });
    }

    private void scrollToBottom() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
