package org.caworks.adbanner;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String path = Environment.getExternalStorageDirectory()
            + File.separator + "AdBannerExtra";
    private Context mContext;
    private List<BannerItem> bannerItems = new ArrayList<>();
    private MyAdBanner banner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        banner = new MyAdBanner(mContext);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        addContentView(banner, params);
        LinearLayout ll_container = (LinearLayout) findViewById(R.id.ll_container);
        ll_container.addView(banner);


        new Thread() {
            @Override
            public void run() {
                AssetManager assets = getAssets();
                try {
                    String[] list = assets.list("");
                    File directory = new File(path);
                    directory.mkdirs();
                    for (String fileName : list) {
                        Log.e(TAG, "fileName: " + fileName);




                        if (fileName.endsWith(".jpg")) {
                            BannerItem bannerItem = new BannerItem();
                            bannerItem.imgUrl = path + File.separator + fileName;
                            bannerItems.add(bannerItem);

                            InputStream is = assets.open(fileName);
                            File file = new File(directory, fileName);
                            FileOutputStream fos = new FileOutputStream(file, false);
                            int len;

//                            BufferedOutputStream bos = new BufferedOutputStream(fos);
//                            while ((len = is.read()) != -1) {
//                                bos.write(len);
//                            }
//                            bos.flush();
//                            bos.close();

                            // 比BOS快N多倍
                            byte b[] = new byte[1024 * 2];
                            while ((len = is.read(b)) != -1) {

                                fos.write(b, 0, len);
                            }
                            fos.flush();
                            fos.close();

                            is.close();

                            if (fileName.endsWith("352.jpg")) break;
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            banner.setSource(bannerItems).startScroll();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

//        BannerItem bannerItem = new BannerItem();
//        bannerItem.imgUrl =
//                banner.setSource()
    }
//    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
//    RandomAccessFile这个类，你看看他的SEEK（）方法
}