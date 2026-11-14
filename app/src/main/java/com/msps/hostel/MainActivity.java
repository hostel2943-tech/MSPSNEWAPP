
package com.msps.hostel;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

    private byte[] pendingBackup;
    private String pendingBackupFilename;

    private static final int CREATE_BACKUP_REQUEST = 501;

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

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Connect website printing to Android printing.
                view.evaluateJavascript(
                    "(function(){"
                    + "window.print=function(){"
                    + "if(window.AMUAndroidPrint){"
                    + "window.AMUAndroidPrint.print();"
                    + "}"
                    + "};"
                    + "})();",
                    null
                );
            }
        });

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
                            "MSPS Hostel Report",
                            webView.createPrintDocumentAdapter(
                                "MSPS Hostel Report"
                            ),
                            new PrintAttributes.Builder().build()
                        );
                    } else {
                        showMessage("Printing service unavailable");
                    }

                } catch (Exception e) {
                    showMessage("Unable to open print preview");
                }
            });
        }

        @JavascriptInterface
        public void saveBackup(String base64, String filename) {

            runOnUiThread(() -> {
                try {
                    byte[] bytes = android.util.Base64.decode(
                        base64,
                        android.util.Base64.DEFAULT
                    );

                    String safeFilename = filename == null
                        ? "MSPS_Backup.json"
                        : filename.replaceAll("[^a-zA-Z0-9._-]", "_");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveBackupModern(bytes, safeFilename);
                    } else {
                        saveBackupOlderAndroid(bytes, safeFilename);
                    }

                } catch (Exception e) {
                    notifyBackupResult(false);
                    showMessage("Backup saving failed");
                }
            });
        }
    }

    private void saveBackupModern(byte[] bytes, String filename)
            throws Exception {

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
                throw new Exception("Unable to open backup file");
            }

            output.write(bytes);
            output.flush();
        }

        notifyBackupResult(true);
        showMessage("Backup saved in Downloads");
    }

    private void saveBackupOlderAndroid(byte[] bytes, String filename) {

        pendingBackup = bytes;
        pendingBackupFilename = filename;

        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("application/json");

            intent.putExtra(
                Intent.EXTRA_TITLE,
                filename
            );

            startActivityForResult(
                intent,
                CREATE_BACKUP_REQUEST
            );

        } catch (Exception e) {
            pendingBackup = null;
            pendingBackupFilename = null;

            notifyBackupResult(false);
            showMessage("Unable to open backup location");
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_BACKUP_REQUEST) {

            if (resultCode == RESULT_OK && data != null
                    && data.getData() != null
                    && pendingBackup != null) {

                try {
                    Uri uri = data.getData();

                    try (OutputStream output =
                        getContentResolver().openOutputStream(uri)) {

                        if (output == null) {
                            throw new Exception("Unable to save backup");
                        }

                        output.write(pendingBackup);
                        output.flush();
                    }

                    notifyBackupResult(true);
                    showMessage("Backup saved successfully");

                } catch (Exception e) {
                    notifyBackupResult(false);
                    showMessage("Backup saving failed");
                }

            } else {
                notifyBackupResult(false);
                showMessage("Backup saving cancelled");
            }

            pendingBackup = null;
            pendingBackupFilename = null;
        }
    }

    private void notifyBackupResult(boolean success) {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(
                "if(window.__mspsBackupSaved){"
                + "window.__mspsBackupSaved("
                + (success ? "true" : "false")
                + ");}",
                null
            ));
        }
    }

    private void showMessage(String message) {
        Toast.makeText(
            MainActivity.this,
            message,
            Toast.LENGTH_LONG
        ).show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {

        if (webView != null) {

            webView.evaluateJavascript(
                "(function(){"

                + "var m=[...document.querySelectorAll('.mini-modal.show')];"

                + "if(m.length){"
                + "m[m.length-1].classList.remove('show');"
                + "return 'closed';"
                + "}"

                + "if(document.querySelector('#panel.show')){"

                + "if(history.state&&history.state.amuMiniModal){"
                + "history.back();"
                + "return 'history';"
                + "}"

                + "if(history.state&&history.state.amuPanel){"
                + "history.back();"
                + "return 'history';"
                + "}"

                + "if(typeof closeTopPanel==='function'){"
                + "closeTopPanel();"
                + "return 'closed';"
                + "}"
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

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
