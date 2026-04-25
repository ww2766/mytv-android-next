package top.yogiczy.mytv.tv.ui.screens.webview;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AssetUtil {

    public static boolean fileExistsInAssets(Context context, String fileName) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] files = assetManager.list("");
            if (files != null) {
                for (String file : files) {
                    if (file.equals(fileName)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static String readStringFromAssets(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}