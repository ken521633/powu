package w2a.W2Apowu.xiaoshuw.com;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化环境
        SWVContext.setContext(this);
        
        // 创建一个全屏 WebView
        webView = new WebView(this);
        setContentView(webView);

        // 设置浏览器参数
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // 加载你在 swv.properties 里设置好的网址
        String url = SWVContext.getSwvProperties().getProperty("app.url");
        webView.loadUrl(url);
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
