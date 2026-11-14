package com.msps.hostel;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
            android.graphics.Color.rgb(16, 36, 58)
        );

        getWindow().setNavigationBarColor(
            android.graphics.Color.rgb(16, 36, 58)
        );

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
            new AndroidBridge(),
            "AMUAndroidPrint"
        );

        webView.loadUrl("file:///android_asset/index.html");
    }

    private class AndroidBridge {

        @JavascriptInterface
        public void print() {
            runOnUiThread(() -> {
                try {
                    PrintManager manager =
                        (PrintManager) getSystemService(
                            Context.PRINT_SERVICE
                        );

                    if (manager != null) {
                        manager.print(
                            "MSPS Report",
                            webView.createPrintDocumentAdapter(
                                "MSPS Report"
                            ),
                            new PrintAttributes.Builder().build()
                        );
                    }
                } catch (Exception e) {
                    Toast.makeText(
                        MainActivity.this,
                        "Unable to open print preview",
                        Toast.LENGTH_LONG
                    ).show();
                }
            });
        }

        @JavascriptInterface
        public void saveBackup(String base64, String filename) {
            runOnUiThread(() -> {
                try {
                    byte[] bytes =
                        android.util.Base64.decode(
                            base64,
                            android.util.Base64.DEFAULT
                        );

                    ContentValues values = new ContentValues();
                    values.put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        filename
                    );
                    values.put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/json"
                    );
                    values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    );

                    Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    );

                    if (uri == null) {
                        throw new Exception("Unable to create backup file");
                    }

                    try (OutputStream output =
                        getContentResolver().openOutputStream(uri)) {

                        if (output == null) {
                            throw new Exception("Unable to save backup");
                        }

                        output.write(bytes);
                    }

                    webView.evaluateJavascript(
                        "if(window.__mspsBackupSaved)window.__mspsBackupSaved(true)",
                        null
                    );

                    Toast.makeText(
                        MainActivity.this,
                        "Backup saved in Downloads",
                        Toast.LENGTH_LONG
                    ).show();

                } catch (Exception e) {

                    webView.evaluateJavascript(
                        "if(window.__mspsBackupSaved)window.__mspsBackupSaved(false)",
                        null
                    );

                    Toast.makeText(
                        MainActivity.this,
                        "Backup saving failed",
                        Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null) {
            webView.evaluateJavascript(
                "(function(){"
                + "var m=[...document.querySelectorAll('.mini-modal.show')];"
                + "if(m.length){m[m.length-1].classList.remove('show');return 'closed';}"
                + "if(document.querySelector('#panel.show')){"
                + "if(history.state&&history.state.amuMiniModal){history.back();return 'history';}"
                + "if(history.state&&history.state.amuPanel){history.back();return 'history';}"
                + "if(typeof closeTopPanel==='function'){closeTopPanel();return 'closed';}"
                + "}"
                + "return 'none';"
                + "})()",
                value -> {
                    if ("\"none\"".equals(value)) {
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            MainActivity.super.onBackPressed();
                        }
                    }
                }
            );
        } else {
            super.onBackPressed();
        }
    }
}
