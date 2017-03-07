package org.caworks.adbanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.caworks.adbanner.MainActivity.screenWidth;

/**
 * Created by Gallon on 2016/9/23
 *
 * 使用
 * 方式一：
 * 1.创建该对象请在代码中banner = new MyAdBanner(context); // (推荐传入 context-application)
 * 2.再加入到头布局中addHeaderView(banner);
 * 方式二：
 * 1.在xml文件中直接做节点使用
 * 建议：如果在Ultra-PTR内嵌 则仅需要在布局中把 PTR 改为 MyFixedPtrFrameLayout即可
 * 目前使用中：1.首页-关注(Fixed) 2.直播(Fixed) 3.我的-签到(非内嵌)
 * 注意：确保在Ultra-PTR内嵌时替换成 MyFixedPtrFrameLayout 不然可能出现类型转换异常
 *
 * View的生命周期
 * View的构造方法-->onFinishInflate()-->onAttachToWindow
 * -->onMeasure()-->onSizeChanged()-->onLayout() -->onDraw()
 * -->onDetachedFromWindow()
 */
public class MyAdBanner extends FrameLayout {
    private static final String TAG = "MyAdBanner";

    private ColorDrawable colorDrawable;
    private Context mContext;

    // 容器
    private FrameLayout rl_content;
    // 轮播图
    private NoTouchViewPager mViewPager;
    // 轮播图下方红点指示器
    private LinearLayout ll_point;
    // 总数，这是为实现无限滑动设置的
    private int totalCount = Integer.MAX_VALUE / 1000;
    // 每个红点指示器的大小
    private int point_size;
    // 每个红点指示器的间隔
    private int point_margin;
    // 图片集合
    private ImageView[] imageViews;
    // 数据传递接口
    private Adapter adapter;
    // 是否设置数据源
    private boolean isSetSource = false;
    // 适配器
    private TopNewsAdapter topNewsAdapter;

    // 数据源 Ad集合
    protected List<BannerItem> mDatas = new ArrayList<>();
    // 广告数量
    private int realAdCount = 0;
    // 轮播图数量 (<=3时优化)
    private int adCount;
    // 首次延迟
    public long mDelay = 3500;
    // 轮播间隔
    public long mPeriod = 3500;
    // 滚动状态
    public boolean scrolling = false;
    // 上次所在位置 (setSource时置为0)
    private int prePosition = -1;
    // Handler
    private final int mWhat = 31;
    // 消息处理
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
            handler.sendEmptyMessageDelayed(mWhat, mPeriod);
        }
    };
    private Toast toast;

    public MyAdBanner(Context context) {
        this(context, null);
    }

    public MyAdBanner(Context context, AttributeSet attrs) {
        super(context.getApplicationContext(), attrs); // 防泄漏处理
        mContext = context.getApplicationContext();

        colorDrawable = new ColorDrawable(Color.parseColor("#555555"));
        topNewsAdapter = new TopNewsAdapter();

        mViewPager = new NoTouchViewPager(mContext);
        ll_point = new LinearLayout(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = DensityUtil.dp2px(mContext, 7);
        ll_point.setLayoutParams(params);

        addView(mViewPager);
        addView(ll_point);
        setListener();

        setPadding(0, 5, 0, 5);
    }

    /**
     * 1.设置源
     * 必须调用此方法前判断是否为空 list != null && !list.isEmpty
     * if (advertList != null && advertList.size() > 0) {
     * isSetSource = true;
     * banner.setSource(advertList).startScroll();
     * mLiveList.addHeaderView(banner);
     * } else {
     * banner.setVisibility(View.GONE);
     * }
     *
     * 1.1 设置ptr并调用MyFixedPtr的requestDisallowInterceptTouchEvent方法请求不拦截
     */
    public MyAdBanner setSource(@NonNull List<BannerItem> list) {
        // 增加代码健壮性,建议在上层处理！
        if (list.isEmpty()) {
            setVisibility(GONE);
            try {
                ((ViewGroup) getParent()).removeView(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return this;
        }

        isSetSource = true;
        mDatas.clear();
        mDatas.addAll(list);
        adCount = mDatas.size();
        realAdCount = adCount;
        if (realAdCount == 1) { // 广告数量1不可滑动
            mViewPager.setTouchable(false);
        } else {
            mViewPager.setTouchable(true);
        }
        extraCopy();
        imageViews = new ImageView[adCount];
        Log.e(TAG, "adCount: " + adCount);
        Log.e(TAG, "realAdCount: " + realAdCount);

        for (int i = 0; i < adCount; i++) {
            final int j = i;
            final int k = (i + 3) % adCount;
            imageViews[k] = new ImageView(mContext);

            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            imageViews[k].setLayoutParams(params);
            imageViews[k].setScaleType(ImageView.ScaleType.FIT_CENTER);

            // 图片下载地址 item.imgUrl
            BannerItem item = mDatas.get(i);
            if (!TextUtils.isEmpty(item.imgUrl)) {
                if (new File(item.imgUrl).isFile()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(item.imgUrl);  // bitmap未做优化


                    imageViews[k].setImageBitmap(bitmap);
                }
            } else {
                imageViews[k].setImageDrawable(colorDrawable);
            }

            imageViews[k].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: onClick
                    BannerItem item = mDatas.get(j);
                    if (toast != null) {
                        toast.cancel();
                    }
                    toast = Toast.makeText(mContext, item.title, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }

        int margin = DensityUtil.dp2px(mContext, 1);
        LayoutParams vp_params = new LayoutParams(screenWidth, (int) (screenWidth / 3.2));
        vp_params.gravity = Gravity.CENTER;
        mViewPager.setLayoutParams(vp_params);
        mViewPager.setPadding(margin, margin, margin, margin);
        ll_point.setGravity(Gravity.CENTER);

        mViewPager.setAdapter(topNewsAdapter);
        // 设置adCount*N 总是会导致第一张空白 所以设置在adCount*2+adCount处 (如果单倍需要最少4张 并且设置首次跳到 count*n+3 )
        if (realAdCount > 1) {
            mViewPager.setCurrentItem(realAdCount * 1000 + 3, false);
        }
        prePosition = 0;
        showPoint(realAdCount);
        return this;
    }

    /**
     * 2.开始轮播 (建议 Activity onResume || Fragment setUserVisibleHint true)
     */
    public void startScroll() {
        Log.e(TAG, "startScroll(): ");
        if (!isSetSource || realAdCount == 1 || realAdCount == 0) {
            return;
        }
        if (scrolling) {
            return;
        }
        scrolling = true;
        handler.removeMessages(mWhat);
        handler.sendEmptyMessageDelayed(mWhat, mDelay);
    }

    /**
     * 2.开始轮播 (建议 Activity onResume || Fragment setUserVisibleHint true)
     *
     * @param index 从第几条广告开始滚动
     */
    public void startScroll(int index) {
        Log.e(TAG, "startScroll(index): ");
        if (!isSetSource || realAdCount == 1 || realAdCount == 0) {
            return;
        }
        if (index >= realAdCount || index < 0) { // index缺省值 -1
            return;
        }
        if (scrolling) {
            return;
        }
        scrolling = true;
        mViewPager.setCurrentItem(realAdCount * 1001 + index, false);
        changePoint(index);
        handler.removeMessages(mWhat);
        handler.sendEmptyMessageDelayed(mWhat, mDelay);
    }

    /**
     * 3.停止轮播 (建议 Activity onPause || Fragment setUserVisibleHint false)
     */
    public void stopScroll() {
        Log.e(TAG, "stopScroll(): ");
        handler.removeMessages(mWhat);
        if (!isSetSource || realAdCount == 1 || realAdCount == 0) {
            return;
        }
        if (!scrolling) {
            return;
        }
        scrolling = false;
    }

    public int getPosition() {
        return prePosition;
    }

    private int realPointSize = -1;
    private int maxSize = 9;
    /**
     * 显示轮播图下方的圆点指示器 (设置源后)
     *
     * @param size 圆点的数量 (应为Ad源数量)
     */
    private void showPoint(int size) {
        ll_point.removeAllViews();
        if (size < 1 || size > maxSize) {
            return;
        }
        realPointSize = size;
        ImageView iv;
        LinearLayout.LayoutParams params;
        point_size = DensityUtil.dp2px(mContext, 7);
        point_margin = DensityUtil.dp2px(mContext, 10);
        for (int i = 0; i < size; i++) {
            iv = new ImageView(mContext);
            iv.setImageResource(R.drawable.selector_point);
            params = new LinearLayout.LayoutParams(point_size, point_size);
            if (i != 0) {
                params.leftMargin = point_margin;
            }
            iv.setLayoutParams(params);
            iv.setEnabled(false);
            ll_point.addView(iv);
        }
        // 第一个Ad资源设置为true
        ll_point.getChildAt(prePosition).setEnabled(true);
    }

    /**
     * 轮播图适配器 内部类
     * 注意：需要在设置源之后创建此对象
     */
    class TopNewsAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return totalCount;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d(TAG, "instantiateItem: position%adCount: " + (position % adCount));
            ViewParent parent = imageViews[position % adCount].getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(imageViews[position % adCount]);
            }
            container.addView(imageViews[position % adCount]);
            return imageViews[position % adCount];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }

    /**
     * 设置轮播图的滑动监听 & 滚动监听
     */
    private void setListener() {
        mViewPager.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        stopScroll();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        startScroll();
                        break;
                }
                return false;
            }
        });

        // 滚动监听 - 轮播图的滚动监听
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                changePoint(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * 在监听广告换页时改变红点
     *
     * @param position
     */
    private void changePoint(int position) {
        int newPosition = (position + realAdCount - 3) % realAdCount;
        // 把当前的索引赋值给前一个索引变量, 方便下一次再切换.
        if (realPointSize < 1 || realPointSize > maxSize) {
            prePosition = newPosition;
            return;
        }
        try {
            ll_point.getChildAt(prePosition).setEnabled(false);
            ll_point.getChildAt(newPosition).setEnabled(true);
        } catch (Exception e) {
        } finally {
            prePosition = newPosition;
        }
    }

    /**
     * 如果广告小于等于三张要特殊处理(复制) 不然滑动过程中会出现异常 必须保证adCount >= 4
     */
    private void extraCopy() {
        while (adCount <= 3) {
            for (int i = 0; i < adCount; i++) {
                mDatas.add(mDatas.get(i));
            }
            adCount *= 2;
        }
    }

    // 定义一个接口让外部设置展示的View
    public interface Adapter {
        boolean isEmpty();

        View getView(int position);

        int getCount();
    }

}
