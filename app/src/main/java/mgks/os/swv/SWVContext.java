package mgks.os.swv;

import android.content.Context;
import java.util.Properties;
import java.io.InputStream;

public class SWVContext {
    private static Context context;
    private static Properties properties;

    public static void setContext(Context ctx) {
        context = ctx;
    }

    public static Properties getSwvProperties() {
        if (properties == null) {
            properties = new Properties();
            try {
                InputStream is = context.getAssets().open("swv.properties");
                properties.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return properties;
    }
}
