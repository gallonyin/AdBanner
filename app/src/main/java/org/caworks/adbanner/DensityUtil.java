package org.caworks.adbanner;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Created by Gallon on 2017/3/5.
 */

public class DensityUtil {
    private static final String TAG = "DensityUtil";
    private static final DisplayMetrics metric = new DisplayMetrics();

    public static int dp2px(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * @param context-activity
     * @return 屏幕宽度
     */
    public static int screenWidth(Context context) {
        // 获得屏幕宽高(px)
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        Log.e(TAG, "displayMetrics.widthPixels: " + displayMetrics.widthPixels);
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels; // 屏幕宽度
//            screenHeight = metric.heightPixels; // 屏幕高度
    }

    /**
     * @param context-activity
     * @return 屏幕高度
     */
    public static int screenHeight(Context context) {
        // 获得屏幕宽高(px)
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        Log.e(TAG, "displayMetrics.heightPixels: " + displayMetrics.heightPixels);
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.heightPixels; // 屏幕高度
    }
}
