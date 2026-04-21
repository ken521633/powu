package w2a.W2Apowu.xiaoshuw.com;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;

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
        TTAdConfig config = new TTAdConfig.Builder()
                .appId("8813818") 
                .useTextureView(true)
                .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
                .allowShowNotify(true)
                .debug(true) 
                .directDownloadNetworkType(TTAdConstant.NETWORK_STATE_WIFI)
                .build();
        
        TTAdSdk.init(this, config);
        TTAdSdk.start(new TTAdSdk.Callback() {
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
        TTAdNative adNative = TTAdSdk.getAdManager().createAdNative(this);
        AdSlot adSlot = new AdSlot.Builder().setCodeId(slotId).build();

        adNative.loadRewardVideoAd(adSlot, new TTAdNative.RewardVideoAdListener() {
            @Override
            public void onError(int code, String message) {
                Log.e("AdLog", "加载失败: " + message);
                Toast.makeText(MainActivity.this, "广告加载失败: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRewardVideoAdLoad(TTRewardVideoAd ad) {
                Log.d("AdLog", "加载成功，开始播放");
                ad.showRewardVideoAd(MainActivity.this);
                
                ad.setRewardAdInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {
                    @Override
                    public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName, int errorCode, String errorMsg) {
                        if (rewardVerify) {
                            Log.d("AdLog", "看完广告，通知 JS 发奖");
                            // 关键：这里直接调用你 JS 里 window.AD 的 reward 方法
                            runOnUiThread(() -> webView.evaluateJavascript("javascript:window.AD.reward();", null));
                        }
                    }
                    @Override public void onAdShow() {}
                    @Override public void onAdVideoBarClick() {}
                    @Override public void onAdClose() {}
                    @Override public void onVideoComplete() {}
                    @Override public void onVideoError() {}
                    @Override public void onSkippedVideo() {}
                });
            }
            @Override public void onRewardVideoCached() {}
            @Override public void onRewardVideoCached(TTRewardVideoAd ad) {}
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) { webView.goBack(); } 
        else { super.onBackPressed(); }
    }
}