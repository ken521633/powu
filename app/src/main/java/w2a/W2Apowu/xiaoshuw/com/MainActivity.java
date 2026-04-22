package w2a.W2Apowu.xiaoshuw.com;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.model.PAGErrorModel;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionCallback;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadCallback;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AdLog";
    private static final String DEFAULT_PANGLE_APP_ID = "8813818";
    private static final String DEFAULT_TEST_PANGLE_APP_ID = "8025677";
    private static final String DEFAULT_TEST_REWARDED_SLOT_ID = "980088192";

    private WebView webView;
    private TextView debugOverlayView;
    private boolean isPangleInitInProgress;
    private boolean isPangleInitSucceeded;
    private String pendingRewardSlotId;
    private String lastDebugSlotId = "-";
    private String lastDebugEvent = "app_start";
    private String lastDebugDetail = "created";
    private String lastDebugTimestamp = "-";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化环境
        SWVContext.setContext(this);
        
        // 创建 WebView
        webView = new WebView(this);
        setupContentView();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // 注入桥接对象
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidAdBridge");

        String url = SWVContext.getSwvProperties().getProperty("app.url");
        updateDebugOverlay("webview_prepare", "-", "url=" + url);

        // 1. 初始化穿山甲 SDK
        initPangleSDK();
        webView.loadUrl(url);
    }

    private void setupContentView() {
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.addView(
                webView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        if (shouldShowDebugOverlay()) {
            debugOverlayView = new TextView(this);
            debugOverlayView.setTextColor(Color.WHITE);
            debugOverlayView.setBackgroundColor(0xCC111111);
            debugOverlayView.setTextSize(11);
            debugOverlayView.setPadding(24, 24, 24, 24);
            debugOverlayView.setClickable(false);
            debugOverlayView.setFocusable(false);

            FrameLayout.LayoutParams overlayLayoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            overlayLayoutParams.gravity = Gravity.TOP;
            rootLayout.addView(debugOverlayView, overlayLayoutParams);
            updateDebugOverlay("overlay_ready", "-", "version=" + BuildConfig.VERSION_NAME);
        }

        setContentView(rootLayout);
    }

    private void initPangleSDK() {
        if (isPangleInitSucceeded || isPangleInitInProgress) {
            return;
        }

        isPangleInitInProgress = true;
        String appId = getPangleAppId();
        PAGConfig config = new PAGConfig.Builder()
                .appId(appId)
                .useTextureView(true)
                .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
                .debugLog(true)
                .build();

        Log.d(TAG, "初始化穿山甲 SDK, appId=" + appId + ", testMode=" + isPangleTestMode());
        updateDebugOverlay("sdk_init_start", "-", "appId=" + appId);
        
        PAGSdk.init(this, config, new PAGSdk.PAGInitCallback() {
            @Override
            public void success() {
                isPangleInitInProgress = false;
                isPangleInitSucceeded = true;
                Log.d(TAG, "穿山甲 SDK 启动成功");
                updateDebugOverlay("sdk_init_success", "-", "init=true");

                if (pendingRewardSlotId != null && !pendingRewardSlotId.isEmpty()) {
                    String slotIdToLoad = pendingRewardSlotId;
                    pendingRewardSlotId = null;
                    runOnUiThread(() -> loadRewardAd(slotIdToLoad));
                }
            }

            @Override
            public void fail(int code, String msg) {
                isPangleInitInProgress = false;
                Log.e(TAG, "穿山甲 SDK 启动失败 code=" + code + ", message=" + msg);
                updateDebugOverlay("sdk_init_fail", "-", code + " / " + msg);
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "广告初始化失败: " + code + " / " + msg,
                        Toast.LENGTH_SHORT
                ).show());
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardVideo(String slotId) {
            Log.d(TAG, "JS 呼叫安卓成功，请求激励广告 ID: " + slotId);
            updateDebugOverlay("js_request_reward", slotId, "from_web");
            runOnUiThread(() -> requestRewardAd(slotId));
        }
    }

    private void requestRewardAd(String slotId) {
        String resolvedSlotId = resolveRewardSlotId(slotId);
        if (resolvedSlotId.isEmpty()) {
            Log.e(TAG, "广告位为空，无法加载激励广告");
            updateDebugOverlay("reward_slot_missing", slotId, "resolved_slot_empty");
            Toast.makeText(this, "广告位未配置", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPangleInitSucceeded) {
            pendingRewardSlotId = resolvedSlotId;
            Log.d(TAG, "SDK 尚未初始化完成，等待初始化后再加载广告，slotId=" + resolvedSlotId);
            updateDebugOverlay("reward_wait_init", resolvedSlotId, "sdk_not_ready");
            initPangleSDK();
            Toast.makeText(this, "广告初始化中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }

        loadRewardAd(resolvedSlotId);
    }

    private void loadRewardAd(String slotId) {
        Log.d(TAG, "开始加载激励广告，slotId=" + slotId + ", testMode=" + isPangleTestMode());
        updateDebugOverlay("reward_load_start", slotId, "testMode=" + isPangleTestMode());
        PAGRewardedRequest request = new PAGRewardedRequest();
        PAGRewardedAd.loadAd(slotId, request, new PAGRewardedAdLoadCallback() {
            @Override
            public void onError(PAGErrorModel error) {
                int code = error != null ? error.getErrorCode() : -1;
                String message = error != null ? error.getErrorMessage() : "unknown error";
                Log.e(TAG, "广告加载失败 code=" + code + ", message=" + message);
                updateDebugOverlay("reward_load_fail", slotId, code + " / " + message);
                Toast.makeText(
                        MainActivity.this,
                        "广告加载失败: " + code + " / " + message,
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onAdLoaded(PAGRewardedAd ad) {
                Log.d(TAG, "广告加载成功，开始播放");
                updateDebugOverlay("reward_load_success", slotId, "showing");
                ad.setAdInteractionCallback(new PAGRewardedAdInteractionCallback() {
                    @Override
                    public void onUserEarnedReward(PAGRewardItem rewardItem) {
                        Log.d(TAG, "看完广告，通知 JS 发奖");
                        updateDebugOverlay("reward_earned", slotId, "callback=reward");
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "window.AD && typeof window.AD.reward === 'function' && window.AD.reward();",
                                null
                        ));
                    }

                    @Override
                    public void onAdShowFailed(PAGErrorModel error) {
                        int code = error != null ? error.getErrorCode() : -1;
                        String message = error != null ? error.getErrorMessage() : "unknown error";
                        Log.e(TAG, "广告播放失败 code=" + code + ", message=" + message);
                        updateDebugOverlay("reward_show_fail", slotId, code + " / " + message);
                        Toast.makeText(
                                MainActivity.this,
                                "广告播放失败: " + code + " / " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onUserEarnedRewardFail(PAGErrorModel error) {
                        int code = error != null ? error.getErrorCode() : -1;
                        String message = error != null ? error.getErrorMessage() : "unknown error";
                        Log.e(TAG, "奖励发放失败 code=" + code + ", message=" + message);
                        updateDebugOverlay("reward_earn_fail", slotId, code + " / " + message);
                    }
                });
                runOnUiThread(() -> ad.show(MainActivity.this));
            }
        });
    }

    private void updateDebugOverlay(String event, String slotId, String detail) {
        lastDebugEvent = sanitizeDebugValue(event, "-");
        lastDebugSlotId = sanitizeDebugValue(slotId, "-");
        lastDebugDetail = sanitizeDebugValue(detail, "-");
        lastDebugTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date());

        if (debugOverlayView == null) {
            return;
        }

        StringBuilder debugText = new StringBuilder();
        debugText.append("Pangle Debug").append('\n');
        debugText.append("version=").append(BuildConfig.VERSION_NAME)
                .append(" (").append(BuildConfig.VERSION_CODE).append(")").append('\n');
        debugText.append("testMode=").append(isPangleTestMode()).append('\n');
        debugText.append("appId=").append(getPangleAppId()).append('\n');
        debugText.append("slotId=").append(lastDebugSlotId).append('\n');
        debugText.append("event=").append(lastDebugEvent).append('\n');
        debugText.append("time=").append(lastDebugTimestamp).append('\n');
        debugText.append("detail=").append(lastDebugDetail);
        runOnUiThread(() -> {
            if (debugOverlayView != null) {
                debugOverlayView.setText(debugText.toString());
            }
        });
    }

    private String sanitizeDebugValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private boolean shouldShowDebugOverlay() {
        String defaultValue = isPangleTestMode() ? "true" : "false";
        return Boolean.parseBoolean(getSwvProperties().getProperty("ad.pangle.debug.overlay", defaultValue));
    }

    private boolean isPangleTestMode() {
        return Boolean.parseBoolean(getSwvProperties().getProperty("ad.pangle.test.mode", "false"));
    }

    private String getPangleAppId() {
        if (isPangleTestMode()) {
            return getSwvProperties().getProperty("ad.pangle.test.app.id", DEFAULT_TEST_PANGLE_APP_ID).trim();
        }
        return getSwvProperties().getProperty("ad.pangle.app.id", DEFAULT_PANGLE_APP_ID).trim();
    }

    private String resolveRewardSlotId(String slotId) {
        if (isPangleTestMode()) {
            return getSwvProperties().getProperty(
                    "ad.pangle.test.slot.rewarded",
                    DEFAULT_TEST_REWARDED_SLOT_ID
            ).trim();
        }
        return slotId == null ? "" : slotId.trim();
    }

    private Properties getSwvProperties() {
        return SWVContext.getSwvProperties();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) { webView.goBack(); } 
        else { super.onBackPressed(); }
    }
}
