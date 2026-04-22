package w2a.W2Apowu.xiaoshuw.com;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.webkit.JavascriptInterface;
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

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. 初始化穿山甲 SDK
        initPangleSDK();
        
        // 初始化环境
        SWVContext.setContext(this);
        
        // 创建 WebView
        webView = new WebView(this);
        setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // 注入桥接对象
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidAdBridge");

        String url = SWVContext.getSwvProperties().getProperty("app.url");
        webView.loadUrl(url);
    }

    private void initPangleSDK() {
        PAGConfig config = new PAGConfig.Builder()
                .appId("8813818") 
                .useTextureView(true)
                .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
                .debugLog(true)
                .build();
        
        PAGSdk.init(this, config, new PAGSdk.PAGInitCallback() {
            @Override
            public void success() { Log.d("AdLog", "穿山甲 SDK 启动成功"); }
            @Override
            public void fail(int code, String msg) { Log.e("AdLog", "启动失败: " + msg); }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardVideo(String slotId) {
            Log.d("AdLog", "JS 呼叫安卓成功，准备加载 ID: " + slotId);
            // 切换到主线程去加载广告
            runOnUiThread(() -> loadRewardAd(slotId));
        }
    }

    private void loadRewardAd(String slotId) {
        PAGRewardedRequest request = new PAGRewardedRequest();
        PAGRewardedAd.loadAd(slotId, request, new PAGRewardedAdLoadCallback() {
            @Override
            public void onError(PAGErrorModel error) {
                String message = error != null ? error.getErrorMessage() : "unknown error";
                Log.e("AdLog", "加载失败: " + message);
                Toast.makeText(MainActivity.this, "广告加载失败: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdLoaded(PAGRewardedAd ad) {
                Log.d("AdLog", "加载成功，开始播放");
                ad.setAdInteractionCallback(new PAGRewardedAdInteractionCallback() {
                    @Override
                    public void onUserEarnedReward(PAGRewardItem rewardItem) {
                        Log.d("AdLog", "看完广告，通知 JS 发奖");
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "window.AD && typeof window.AD.reward === 'function' && window.AD.reward();",
                                null
                        ));
                    }

                    @Override
                    public void onAdShowFailed(PAGErrorModel error) {
                        String message = error != null ? error.getErrorMessage() : "unknown error";
                        Log.e("AdLog", "播放失败: " + message);
                        Toast.makeText(MainActivity.this, "广告播放失败: " + message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onUserEarnedRewardFail(PAGErrorModel error) {
                        String message = error != null ? error.getErrorMessage() : "unknown error";
                        Log.e("AdLog", "奖励发放失败: " + message);
                    }
                });
                runOnUiThread(() -> ad.show(MainActivity.this));
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) { webView.goBack(); } 
        else { super.onBackPressed(); }
    }
}
