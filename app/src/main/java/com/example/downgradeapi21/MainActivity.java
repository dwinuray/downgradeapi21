package com.example.downgradeapi21;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.provider.Settings;
import android.view.View;
import android.webkit.GeolocationPermissions;
// import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private int webViewPreviousState;
    private final int PAGE_STARTED = 0x1;
    private final int PAGE_REDIRECTED = 0x2;
    private ConstraintLayout rootView;
    private WebView webView;

    //	private Button restart, exit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webview);
        rootView = (ConstraintLayout) findViewById(R.id.root_view);

        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ Permission APIs
            MarshMallowsetting();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
        webView.setInitialScale(1);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.setWebViewClient(new GeoWebViewClient());
        // Below required for geolocation
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.setWebChromeClient(new GeoWebChromeClient());

        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.getSettings().setGeolocationDatabasePath(getFilesDir().getPath());

        webView.loadUrl("http://192.168.1.14:8000/mobile/login");
//        createWebPagePrint(browser);
    }

    public  void createWebPagePrint(WebView browser) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter printAdapter = browser.createPrintDocumentAdapter();
            String jobName = "Print" + " Document";
            PrintAttributes.Builder builder = new PrintAttributes.Builder();
            builder.setMediaSize(PrintAttributes.MediaSize.ISO_A5);
            PrintJob printJob = printManager.print(jobName, printAdapter, builder.build());
            if(printJob.isCompleted()){
                Toast.makeText(getApplicationContext(), "print_complete", Toast.LENGTH_LONG).show();
            }
            else if(printJob.isFailed()){
                Toast.makeText(getApplicationContext(), "print_failed", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * WebChromeClient subclass handles UI-related calls
     * Note: think chrome as in decoration, not the Chrome browser
     */
    public class GeoWebChromeClient extends android.webkit.WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin,
                                                       final GeolocationPermissions.Callback callback) {
            // Always grant permission since the app itself requires location
            // permission and the user has therefore already granted it
            callback.invoke(origin, true, false);
        }
    }

    /**
     * WebViewClient subclass loads all hyperlinks in the existing WebView
     */
    public class GeoWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        Dialog loadingDialog = new Dialog(MainActivity.this);
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            webViewPreviousState = PAGE_STARTED;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (isConnected()) {
                final Snackbar snackBar = Snackbar.make(rootView, "onReceivedError : " + error.getDescription(), Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("Reload", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        webView.loadUrl("javascript:window.location.reload( true )");
                    }
                });
                snackBar.show();
            } else {
                final Snackbar snackBar = Snackbar.make(rootView, "No Internet Connection ", Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("Enable Data", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), 0);
                        webView.loadUrl("javascript:window.location.reload( true )");
                        snackBar.dismiss();
                    }
                });
                snackBar.show();
            }
            super.onReceivedError(view, request, error);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (isConnected()) {
                final Snackbar snackBar = Snackbar.make(rootView, "HttpError : " + errorResponse.getReasonPhrase(), Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("Reload", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        webView.loadUrl("javascript:window.location.reload( true )");
                    }
                });
                snackBar.show();
            } else {
                final Snackbar snackBar = Snackbar.make(rootView, "No Internet Connection ", Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("Enable Data", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), 0);
                        webView.loadUrl("javascript:window.location.reload( true )");
                        snackBar.dismiss();
                    }
                });
                snackBar.show();
            }
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (webViewPreviousState == PAGE_STARTED) {
                if (null != loadingDialog) {
                    loadingDialog.dismiss();
                    loadingDialog = null;
                }
            }
        }
    }


    /**
     * Check if there is any connectivity
     *
     * @return is Device Connected
     */
    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null != cm) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return (info != null && info.isConnected());
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

                ) {
                    Toast.makeText(MainActivity.this, "All Permission GRANTED !! Thank You :)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "One or More Permissions are DENIED Exiting App :(", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void MarshMallowsetting() {
        List<String> permissionsNeeded = new ArrayList<String>();
        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("Show Location");
        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                String message = "App need access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }
        Toast.makeText(MainActivity.this, "No new Permission Required- Launching App .You are Awesome!!", Toast.LENGTH_SHORT).show();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}