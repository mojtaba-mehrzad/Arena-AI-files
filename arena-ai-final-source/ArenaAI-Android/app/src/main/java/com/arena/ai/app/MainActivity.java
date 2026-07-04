package com.arena.ai.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.Manifest;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String ARENA_BATTLE_URL = "https://arena.ai/battle";
    private static final String ARENA_AGENT_URL = "https://arena.ai/agent";
    private static final String ARENA_SIDE_BY_SIDE_URL = "https://arena.ai/side-by-side";
    private static final String ARENA_DIRECT_URL = "https://arena.ai/direct";

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout fullscreenContainer;
    private BottomNavigationView bottomNav;
    private ImageView btnAccount;
    private ImageView btnRefresh;
    private View waveOverlay;
    private SharedPreferences prefs;
    private AccountManager accountManager;

    private boolean isNavUpdateFromWebView = false;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;
    private static final int DOWNLOAD_PERMISSION_REQUEST_CODE = 1004;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        accountManager = new AccountManager(this);

        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        initViews();
        setupWebView();
        setupBottomNavigation();
        setupAccountButton();
        setupRefreshButton();
        setupSwipeRefresh();
        setupBackPressHandler();

        webView.loadUrl(ARENA_BATTLE_URL);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "arena_ai_notifications",
                    "Arena AI Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications from Arena.ai");
            channel.enableLights(true);
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        bottomNav = findViewById(R.id.bottom_navigation);
        btnAccount = findViewById(R.id.btn_account);
        btnRefresh = findViewById(R.id.btn_refresh);
        waveOverlay = findViewById(R.id.wave_overlay);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " ArenaAI/1.0.0 (Android)");

        webView.setWebViewClient(new ArenaWebViewClient());
        webView.setWebChromeClient(new ArenaWebChromeClient());
        setupDownloadHandling();
        setupNavigationSyncBridge();
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            if (isNavUpdateFromWebView) return true;

            int id = item.getItemId();
            String targetUrl = null;
            if (id == R.id.nav_battle) targetUrl = ARENA_BATTLE_URL;
            else if (id == R.id.nav_agent) targetUrl = ARENA_AGENT_URL;
            else if (id == R.id.nav_side_by_side) targetUrl = ARENA_SIDE_BY_SIDE_URL;
            else if (id == R.id.nav_direct) targetUrl = ARENA_DIRECT_URL;

            if (targetUrl != null) {
                // Play scale animation on the selected icon
                animateNavItem(item);
                // Play wave across the nav bar
                playNavWave();

                String currentUrl = webView.getUrl();
                if (currentUrl == null || !currentUrl.equals(targetUrl)) {
                    webView.loadUrl(targetUrl);
                }
            }
            return true;
        });
    }

    /**
     * Scale bounce animation on the selected nav item icon.
     */
    private void animateNavItem(MenuItem item) {
        View itemView = findViewById(item.getItemId());
        if (itemView == null) return;

        // Find the icon ImageView inside the nav item
        if (itemView instanceof FrameLayout) {
            View iconView = ((FrameLayout) itemView).getChildAt(0);
            if (iconView != null) {
                ScaleAnimation scaleUp = new ScaleAnimation(
                        1.0f, 1.3f, 1.0f, 1.3f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                scaleUp.setDuration(150);
                scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());

                ScaleAnimation scaleDown = new ScaleAnimation(
                        1.3f, 1.0f, 1.3f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                scaleDown.setDuration(200);
                scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());

                AnimationSet set = new AnimationSet(true);
                set.addAnimation(scaleUp);
                scaleUp.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation a) {}
                    @Override public void onAnimationRepeat(Animation a) {}
                    @Override public void onAnimationEnd(Animation a) {
                        iconView.startAnimation(scaleDown);
                    }
                });
                iconView.startAnimation(set);
            }
        }
    }

    /**
     * Play a golden wave across the navigation bar.
     */
    private void playNavWave() {
        waveOverlay.setVisibility(View.VISIBLE);
        waveOverlay.setAlpha(0f);
        waveOverlay.setScaleX(0.1f);
        waveOverlay.setPivotX(0f);

        // Fade in + scale out
        waveOverlay.animate()
                .alpha(0.6f)
                .scaleX(1f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // Fade out
                    waveOverlay.animate()
                            .alpha(0f)
                            .setDuration(350)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> waveOverlay.setVisibility(View.INVISIBLE))
                            .start();
                })
                .start();
    }

    private void setupAccountButton() {
        btnAccount.setOnClickListener(v -> showAccountSheet());
    }

    private void setupRefreshButton() {
        btnRefresh.setOnClickListener(v -> {
            flashRefreshButton();
            webView.reload();
        });
    }

    private void flashRefreshButton() {
        btnRefresh.setImageTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.arena_primary)));
        btnRefresh.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(120)
                .withEndAction(() -> btnRefresh.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160)
                        .withEndAction(() -> btnRefresh.setImageTintList(
                                android.content.res.ColorStateList.valueOf(getColor(R.color.arena_text_hint))))
                        .start())
                .start();
    }

    private void showAccountSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_accounts, null);
        sheet.setContentView(view);

        RecyclerView recycler = view.findViewById(R.id.recycler_accounts);
        View emptyState = view.findViewById(R.id.empty_state);
        ImageView btnAdd = view.findViewById(R.id.btn_add_account);

        List<AccountManager.Account> accounts = accountManager.getAccounts();

        if (accounts.isEmpty()) {
            recycler.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            recycler.setLayoutManager(new LinearLayoutManager(this));
            AccountAdapter adapter = new AccountAdapter(accounts, accountManager.getActiveAccountId(),
                    new AccountAdapter.Listener() {
                        @Override
                        public void onSwitch(AccountManager.Account account) {
                            sheet.dismiss();
                            confirmSwitchAccount(account);
                        }

                        @Override
                        public void onDelete(AccountManager.Account account) {
                            confirmDeleteAccount(account, () -> {
                                accountManager.deleteAccount(account.id);
                                updateAccountButton();
                                // Refresh the sheet
                                sheet.dismiss();
                                showAccountSheet();
                            });
                        }
                    });
            recycler.setAdapter(adapter);
        }

        btnAdd.setOnClickListener(v -> {
            showAddAccountDialog(() -> {
                updateAccountButton();
                sheet.dismiss();
                showAccountSheet();
            });
        });

        sheet.show();
    }

    private void showAddAccountDialog(Runnable onSuccess) {
        EditText input = new EditText(this);
        input.setHint(R.string.add_account_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getColor(R.color.arena_text_primary));
        input.setHintTextColor(getColor(R.color.arena_text_hint));
        input.setPadding(48, 24, 48, 24);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(32, 0, 32, 0);
        input.setLayoutParams(lp);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 16, 24, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_account_dialog_title)
                .setView(container)
                .setPositiveButton(R.string.add_account_save, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Account " + (accountManager.getAccounts().size() + 1);
                    accountManager.saveCurrentSession(name);
                    updateAccountButton();
                    Toast.makeText(this, "Account saved!", Toast.LENGTH_SHORT).show();
                    if (onSuccess != null) onSuccess.run();
                })
                .setNegativeButton(R.string.add_account_cancel, null)
                .show();
    }

    private void confirmSwitchAccount(AccountManager.Account account) {
        String activeId = accountManager.getActiveAccountId();
        if (account.id.equals(activeId)) {
            Toast.makeText(this, "Already on this account", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.switch_account_title)
                .setMessage(getString(R.string.switch_account_message))
                .setPositiveButton(R.string.switch_account_confirm, (dialog, which) -> {
                    accountManager.switchToAccount(account.id);
                    webView.reload();
                    updateAccountButton();
                })
                .setNegativeButton(R.string.exit_cancel, null)
                .show();
    }

    private void confirmDeleteAccount(AccountManager.Account account, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_message)
                .setPositiveButton(R.string.delete_account_confirm, (dialog, which) -> {
                    onConfirm.run();
                })
                .setNegativeButton(R.string.exit_cancel, null)
                .show();
    }

    private void updateAccountButton() {
        AccountManager.Account active = accountManager.getActiveAccount();
        if (active != null) {
            btnAccount.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.arena_primary)));
        } else {
            btnAccount.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.arena_text_hint)));
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.arena_primary,
                R.color.arena_accent
        );
        // Pull-to-refresh by drag is disabled so it never interferes with page scrolling.
        // Manual refresh is available from the refresh button in the bottom bar.
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    showExitDialog();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void showExitDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exit_dialog_title)
                .setMessage(R.string.exit_dialog_message)
                .setPositiveButton(R.string.exit_confirm, (dialog, which) -> finish())
                .setNegativeButton(R.string.exit_cancel, null)
                .show();
    }


    private void setupDownloadHandling() {
        webView.addJavascriptInterface(new BlobDownloadInterface(), "ArenaAndroidDownloader");
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            if (url != null && url.startsWith("blob:")) {
                downloadBlobUrl(url, fileName, mimeType);
                return;
            }
            if (url != null && url.startsWith("data:")) {
                saveDataUrl(url, fileName);
                return;
            }
            downloadHttpUrl(url, userAgent, contentDisposition, mimeType);
        });
    }

    private void setupNavigationSyncBridge() {
        webView.addJavascriptInterface(new NavigationBridge(), "ArenaNavigationBridge");
    }

    private void injectNavigationSyncScript() {
        String script = "(function(){" +
                "if(window.__arenaAndroidNavSyncInstalled)return;" +
                "window.__arenaAndroidNavSyncInstalled=true;" +
                "function notify(){try{ArenaNavigationBridge.onUrlChanged(location.href);}catch(e){}}" +
                "var pushState=history.pushState;history.pushState=function(){var r=pushState.apply(this,arguments);setTimeout(notify,0);return r;};" +
                "var replaceState=history.replaceState;history.replaceState=function(){var r=replaceState.apply(this,arguments);setTimeout(notify,0);return r;};" +
                "window.addEventListener('popstate',function(){setTimeout(notify,0);});" +
                "window.addEventListener('hashchange',function(){setTimeout(notify,0);});" +
                "setInterval(function(){if(window.__arenaLastHref!==location.href){window.__arenaLastHref=location.href;notify();}},500);" +
                "notify();" +
                "})();";
        webView.evaluateJavascript(script, null);
    }

    private boolean ensureDownloadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                DOWNLOAD_PERMISSION_REQUEST_CODE);
        Toast.makeText(this, "Storage permission is required for downloads", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void downloadHttpUrl(String url, String userAgent, String contentDisposition, String mimeType) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Invalid download link", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ensureDownloadPermission()) return;
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) request.addRequestHeader("Cookie", cookies);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            request.addRequestHeader("Accept", "*/*");
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
            request.setDescription("Downloading file…");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Download manager is unavailable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadBlobUrl(String blobUrl, String fileName, String mimeType) {
        if (!ensureDownloadPermission()) return;
        try {
            String safeBlobUrl = JSONObject.quote(blobUrl);
            String safeFileName = JSONObject.quote(sanitizeFileName(fileName));
            String safeMimeType = JSONObject.quote(mimeType == null ? "application/octet-stream" : mimeType);
            String script = "(async function(){" +
                    "try{" +
                    "const response=await fetch(" + safeBlobUrl + ");" +
                    "const blob=await response.blob();" +
                    "const reader=new FileReader();" +
                    "reader.onloadend=function(){ArenaAndroidDownloader.saveBase64File(reader.result," + safeFileName + ",blob.type||" + safeMimeType + ");};" +
                    "reader.onerror=function(){ArenaAndroidDownloader.onDownloadError('Could not read downloaded file');};" +
                    "reader.readAsDataURL(blob);" +
                    "}catch(e){ArenaAndroidDownloader.onDownloadError(e&&e.message?e.message:String(e));}" +
                    "})();";
            webView.evaluateJavascript(script, null);
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveDataUrl(String dataUrl, String fileName) {
        if (!ensureDownloadPermission()) return;
        new Thread(() -> {
            try {
                saveBase64DataUrl(dataUrl, sanitizeFileName(fileName));
                runOnUiThread(() -> Toast.makeText(this, "File saved to Downloads", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void saveBase64DataUrl(String dataUrl, String fileName) throws Exception {
        if (dataUrl == null || !dataUrl.startsWith("data:")) throw new IllegalArgumentException("Invalid file data");
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) throw new IllegalArgumentException("Invalid file data");
        String metadata = dataUrl.substring(5, commaIndex);
        String base64Part = dataUrl.substring(commaIndex + 1);
        String mimeType = "application/octet-stream";
        int semicolonIndex = metadata.indexOf(';');
        if (semicolonIndex > 0) mimeType = metadata.substring(0, semicolonIndex);
        else if (!metadata.isEmpty()) mimeType = metadata;
        byte[] bytes = metadata.contains("base64")
                ? Base64.decode(base64Part, Base64.DEFAULT)
                : java.net.URLDecoder.decode(base64Part, "UTF-8").getBytes("UTF-8");
        saveBytesToDownloads(bytes, fileName, mimeType);
    }

    private void saveBytesToDownloads(byte[] bytes, String fileName, String mimeType) throws Exception {
        fileName = sanitizeFileName(fileName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Could not create download file");
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("Could not open download file");
                out.write(bytes);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Storage permission is required");
            }
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not open Downloads folder");
            File file = new File(dir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
            }
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) fileName = "arena-download";
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return fileName.isEmpty() ? "arena-download" : fileName;
    }

    private class NavigationBridge {
        @JavascriptInterface
        public void onUrlChanged(String url) {
            if (url == null) return;
            runOnUiThread(() -> syncBottomNavSelection(url));
        }
    }

    private class BlobDownloadInterface {
        @JavascriptInterface
        public void saveBase64File(String dataUrl, String fileName, String mimeType) {
            new Thread(() -> {
                try {
                    saveBase64DataUrl(dataUrl, fileName);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "File saved to Downloads", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }
        @JavascriptInterface
        public void onDownloadError(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "Download failed: " + message, Toast.LENGTH_LONG).show());
        }
    }

    // ---- WebView Client ----
    private class ArenaWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.contains("arena.ai")) return false;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            syncBottomNavSelection(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            syncBottomNavSelection(url);
            injectNavigationSyncScript();
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            syncBottomNavSelection(url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) showErrorPage();
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }
    }

    // ---- WebChrome Client ----
    private class ArenaWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            if (newProgress == 100) progressBar.setVisibility(View.GONE);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) { return true; }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                callback.invoke(origin, true, false);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            MainActivity.this.filePathCallback = filePathCallback;
            try {
                startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_REQUEST_CODE);
            } catch (Exception e) {
                MainActivity.this.filePathCallback = null;
                Toast.makeText(MainActivity.this, R.string.file_chooser_error, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            fullscreenContainer.setVisibility(View.VISIBLE);
            fullscreenContainer.addView(view);
            webView.setVisibility(View.GONE);
            bottomNav.setVisibility(View.GONE);
            super.onShowCustomView(view, callback);
        }

        @Override
        public void onHideCustomView() {
            fullscreenContainer.setVisibility(View.GONE);
            fullscreenContainer.removeAllViews();
            webView.setVisibility(View.VISIBLE);
            bottomNav.setVisibility(View.VISIBLE);
            super.onHideCustomView();
        }
    }

    private void syncBottomNavSelection(String url) {
        isNavUpdateFromWebView = true;
        if (url.contains("/agent")) bottomNav.setSelectedItemId(R.id.nav_agent);
        else if (url.contains("/side-by-side")) bottomNav.setSelectedItemId(R.id.nav_side_by_side);
        else if (url.contains("/direct")) bottomNav.setSelectedItemId(R.id.nav_direct);
        else bottomNav.setSelectedItemId(R.id.nav_battle);
        isNavUpdateFromWebView = false;
    }

    private void showErrorPage() {
        String errorHtml = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1.0'>" +
                "<style>*{margin:0;padding:0;box-sizing:border-box}body{display:flex;justify-content:center;" +
                "align-items:center;height:100vh;background:#0A0A0A;color:#8E8E93;font-family:-apple-system,sans-serif;" +
                "text-align:center;padding:32px}.container{max-width:300px}h2{color:#F0F0F0;font-size:20px;" +
                "margin-bottom:8px;font-weight:600}p{font-size:14px;line-height:1.5;margin-bottom:24px}" +
                "button{padding:12px 32px;background:#D4A853;color:#000;border:none;border-radius:12px;" +
                "font-size:15px;font-weight:600;cursor:pointer}button:active{opacity:0.7}</style></head><body>" +
                "<div class='container'><h2>No Connection</h2>" +
                "<p>Check your internet connection and try again.</p>" +
                "<button onclick='window.location.reload()'>Retry</button></div></body></html>";
        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    // ---- Account Adapter ----
    static class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.VH> {
        interface Listener {
            void onSwitch(AccountManager.Account account);
            void onDelete(AccountManager.Account account);
        }

        private final List<AccountManager.Account> accounts;
        private final String activeId;
        private final Listener listener;

        AccountAdapter(List<AccountManager.Account> accounts, String activeId, Listener listener) {
            this.accounts = accounts;
            this.activeId = activeId;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_account, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AccountManager.Account acc = accounts.get(pos);
            boolean isActive = acc.id.equals(activeId);

            h.avatar.setText(String.valueOf(acc.name.charAt(0)).toUpperCase());
            h.name.setText(acc.name);
            h.status.setText(isActive ? R.string.account_active : R.string.account_inactive);
            h.activeIcon.setVisibility(isActive ? View.VISIBLE : View.GONE);

            h.itemView.setOnClickListener(v -> listener.onSwitch(acc));
            h.btnDelete.setOnClickListener(v -> listener.onDelete(acc));
        }

        @Override
        public int getItemCount() { return accounts.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView avatar, name, status;
            ImageView activeIcon, btnDelete;
            VH(View v) {
                super(v);
                avatar = v.findViewById(R.id.account_avatar);
                name = v.findViewById(R.id.account_name);
                status = v.findViewById(R.id.account_status);
                activeIcon = v.findViewById(R.id.account_active_icon);
                btnDelete = v.findViewById(R.id.btn_delete_account);
            }
        }
    }

    // ---- Lifecycle ----
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String ds = data.getDataString();
                if (ds != null) results = new Uri[]{Uri.parse(ds)};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            webView.reload();
        } else if (requestCode == DOWNLOAD_PERMISSION_REQUEST_CODE) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Tap download again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override protected void onResume() { super.onResume(); webView.onResume(); updateAccountButton(); }
    @Override protected void onPause() { super.onPause(); webView.onPause(); }
    @Override protected void onDestroy() { webView.destroy(); super.onDestroy(); }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) { webView.reload(); return true; }
        if (id == R.id.action_share) { shareCurrentPage(); return true; }
        if (id == R.id.action_open_browser) { openInBrowser(); return true; }
        if (id == R.id.action_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void shareCurrentPage() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Arena.ai");
        i.putExtra(Intent.EXTRA_TEXT, "Check this out on Arena: " + webView.getUrl());
        startActivity(Intent.createChooser(i, getString(R.string.share_via)));
    }

    private void openInBrowser() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl())));
    }
}
