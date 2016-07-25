package com.awarmisland.d.carouselfigure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 轮播图
 * @author  d
 */
public class CarouselFigure extends View {

    private float MIN_ANGLE_TAN_VALUE=0.2f; //横向手势最大可接受角度 正切值
    private float MIN_WIDTH_OFFSET_VALUE=0.2f; //宽度与偏移量的差值 临界值
    private long SLID_IMG_INTERVALS=10;//滑动时间间隔
    private float SLID_IMG_INTERVAL_OFFSET=40f;//滑动间隔偏移量
    private float ALLOW_SLID_IMG_OFFSET = 50f;//允许滑动图片偏移量
    private Context mContext;
    private List<Bitmap> imgList;//图片集合
    private Set<Integer> handleIndexSet;//记录处理图片集合
    private int showIndex,pre_show_index,next_show_index;//显示图片索引值
    private Paint mPaint; //画笔
    private int mWidth; //整体宽度
    private int mHeight;//整体高度
    private PointF startPoint;//开始触碰坐标值
    private PointF nowPoint;//实时坐标
    private float offset;//偏移距离
    private float old_offset;//记录上一次偏移距离
    private boolean isAllowAutoSlid;//是否允许自动轮播
    private boolean isAutoSliding;//正在自动轮播
    private boolean isGestureSliding;//手势拨动滑动
    private boolean isNextCycle;//是否轮播下一个周期
    private boolean isRefreshing;//是否正在刷新数据

    private OnTopImageClickListeners onTopImageClickListeners; //点击图片监听

    public CarouselFigure(Context context) {
        super(context);
        init(context);
    }

    public CarouselFigure(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CarouselFigure(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取view宽高
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        //默认showindex对应图片适配后宽高，做轮播图整体宽高
        if(imgList!=null && imgList.size()>0){
            Bitmap bitmap = imgList.get(showIndex);
            float scale = (float)mWidth / bitmap.getWidth();
            mHeight = (int) (scale * bitmap.getHeight());
        }
        setMeasuredDimension(mWidth,mHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //判断偏移之后在临界值
        float min_x = mWidth-Math.abs(offset);
        //小于临界值时，改变显示图片索引值
        if(min_x<MIN_WIDTH_OFFSET_VALUE){
            //左移动
            if(offset<0 ){
                showIndex ++;
            }
            if(offset>0 ){
                showIndex--;
            }
            //回归
            reSetValue();
        }
        //初始化showIndex
        initShowIndex();
        //构画左中右三图
        handleImgWnH(pre_show_index);
        handleImgWnH(showIndex);
        handleImgWnH(next_show_index);
        canvas.drawBitmap(imgList.get(pre_show_index),-mWidth+offset,0,mPaint);
        canvas.drawBitmap(imgList.get(showIndex),offset,0,mPaint);
        canvas.drawBitmap(imgList.get(next_show_index),mWidth+offset,0,mPaint);
        drawCycle(canvas);
    }

    /**
     * view加载完成
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i("KK","onAttachedToWindow");
        //注册息屏，亮屏广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(receiver,filter);
        //自动轮播图片
        autoSlidImg();
    }

    /**
     * 进出后台  关闭开启轮播
     * @param hasWindowFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        Log.i("KK","onWindowFocusChanged");
        if(hasWindowFocus){
            isAllowAutoSlid=true;
        }else{
            isAllowAutoSlid=false;
        }
    }

    /**
     * 取消注册广播
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i("KK","onDetachedFromWindow");
        mContext.unregisterReceiver(receiver);
    }

    /**
     * 初始化参数
     */
    private void init(Context context){
        mContext = context;
        mPaint = new Paint();
        imgList = new ArrayList<>();
        handleIndexSet = new HashSet<>();
        startPoint = new PointF();
        nowPoint = new PointF();
        isAllowAutoSlid = true;
        isAutoSliding=false;
        isGestureSliding =false;
        isNextCycle = true;
        isRefreshing=false;
        //默认图片
        imgList.add(BitmapFactory.decodeResource(getResources(), R.drawable.test1));
    }

    /**
     * 滑动事件
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //正在刷新禁止触摸
        if(isRefreshing){return true;}
        //正在自动轮播时，不能触发
        if(isAutoSliding||isGestureSliding){return true;}
        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                //关闭自动轮播
                isAllowAutoSlid = false;
                startPoint.set(event.getX(),event.getY());//记录初始值
                return true;
            case MotionEvent.ACTION_MOVE:
                float min_x = Math.abs(startPoint.x - event.getX());
                float min_y = Math.abs(startPoint.y - event.getY());
                //获取正切值
                float angle_tan_value = min_y / min_x;
                if(angle_tan_value<MIN_ANGLE_TAN_VALUE){ //横向滑动
                    min_x = event.getX() - startPoint.x ;
                    offset = old_offset + min_x;
                    nowPoint.set(event.getX(),event.getY());//记录当前坐标
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                old_offset = offset;//记录上一次偏移量
                //滑动手势产生图片切换效果
                post(slidImgRunnable);
                //重新开启自动轮播
                isAllowAutoSlid = true;
                if(offset==0) {
                    onTopImageClickListeners.onClick(showIndex);
                }
                return true;
        }
        return false;

    }

    /**
     * 初始化显示图片索引
     */
    private void initShowIndex(){
        //showIndex纠正
        if(showIndex<0){
            showIndex=imgList.size()-1;
        }else if(showIndex>imgList.size()-1){
            showIndex=0;
        }
        //左图index
        if(showIndex==0) {
            pre_show_index = imgList.size() - 1;
        }else{
            pre_show_index = showIndex-1;
        }
        //右图index
        if(showIndex==imgList.size()-1){
            next_show_index = 0;
        }else{
            next_show_index=showIndex+1;
        }
    }

    /**
     * 开启自动轮播
     */
    private void autoSlidImg()  {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true){
                        Thread.sleep(4000);//每4秒轮播一次
                        if(isAllowAutoSlid){
                            post(autoSlidImgRunnable);
                        }
                    }
                }catch (Exception e ){
                }
            }
        }).start();
    }

    /**
     * 实现自动轮播效果
     */
    private Runnable autoSlidImgRunnable = new Runnable() {
        @Override
        public void run() {
            offset += -SLID_IMG_INTERVAL_OFFSET;
            if (isNextCycle) {//判断是否可以继续产生偏移，完成一次轮播后退出
                offset=0;
                isNextCycle=false;
                isAutoSliding = false;
                return;
            }else{
                isAutoSliding = true;//正在轮播滑动
            }
            invalidate();
            postDelayed(autoSlidImgRunnable, SLID_IMG_INTERVALS);
        }
    };

    //通过手势产生图片滑动效果
    private Runnable slidImgRunnable = new Runnable() {
        @Override
        public void run() {
            float abs_offset = Math.abs(offset);
            //向左移动
            if(offset<0) {
                if(abs_offset<ALLOW_SLID_IMG_OFFSET){  //允许产生滑动的偏移量，否则图片回归原位置
                    offset += SLID_IMG_INTERVAL_OFFSET;
                    if(offset>0){
                        reSetValue();
                    }
                }else {
                    offset += -SLID_IMG_INTERVAL_OFFSET;
                }
            }else if(offset>0){ //向右移动
                if(abs_offset<ALLOW_SLID_IMG_OFFSET){ //允许产生滑动的偏移量，否则图片回归原位置
                    offset += -SLID_IMG_INTERVAL_OFFSET;
                    if(offset<0){
                        reSetValue();
                    }
                }else {
                    offset += SLID_IMG_INTERVAL_OFFSET;
                }
            }
            //当滑动之后 offset重置为0 结束循环
            if(abs_offset==0){
                isGestureSliding = false;
                return;
            }
            isGestureSliding = true;
            invalidate();
            postDelayed(slidImgRunnable,SLID_IMG_INTERVALS);
        }
    };

    /**
     * 重置参数
     */
    private void reSetValue(){
        offset=0;
        old_offset=0;
        startPoint.set(nowPoint.x,nowPoint.y);
        isNextCycle = true;
        isGestureSliding =false;
    }

    /**
     * 重置回归原始
     */
    private void reInitValue(){
        offset=0;
        old_offset=0;
        isNextCycle = true;
        isGestureSliding =false;
        startPoint = new PointF();
        handleIndexSet.clear();
    }
    /**
     * 处理图片适配屏幕问题
     * @param drawIndex
     */
    private void handleImgWnH(int drawIndex) {
        if (!handleIndexSet.contains(drawIndex)) {
            Bitmap img = imgList.get(drawIndex);
            Matrix matrix = new Matrix();
            float scale_w = (float)mWidth / img.getWidth();
            float scale_h = (float)mHeight/img.getHeight();
            matrix.setScale(scale_w, scale_h);
            Bitmap bitmap = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
            imgList.set(drawIndex, bitmap);
            handleIndexSet.add(drawIndex);
        }
    }

    /**
     * 导航圆点
     * @param canvas
     */
    private void drawCycle(Canvas canvas){
        Paint paint = new Paint();
        int color_normal = Color.argb(127,248,248,255);//透明灰色
        int color_select = Color.argb(204,255,69,0);//透明橙色
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        int count = imgList.size();
        int dis = dpValue2px(12);  //圆点距离
        int radius=dpValue2px(5); //圆点大小
        for(int i=1;i<=count;i++){
            if(i==count - showIndex){
                paint.setColor(color_select);
            }else{
                paint.setColor(color_normal);
            }
            int min_x = i*dis+(i*2-1)*radius; //计算圆心偏移
            canvas.drawCircle(mWidth-min_x,mHeight-dis,radius,paint);
        }

    }

    /**
     * dp 转 px
     * @param dpValue
     * @return
     */
    private int dpValue2px(int dpValue){
        int pxValue=0;
        float scale = getResources().getDisplayMetrics().density;
        pxValue=(int)(dpValue*scale+0.5f);
        return pxValue;
    }

    /**
     * 息屏，亮屏 广播
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_SCREEN_ON)){
                Log.i("KK","亮屏");
                isAllowAutoSlid=true;
            }else if(action.equals(Intent.ACTION_SCREEN_OFF)){
                Log.i("KK","息屏");
                isAllowAutoSlid=false;
            }
        }
    };
    /**
     * 设置图片列表
     * @param imgList
     */
    public void setImgList(List<Bitmap> imgList){
        this.imgList.clear();
        this.imgList.addAll(imgList);
    }

    /**
     * 设置显示图片index
     * @param showIndex
     */
    public void setShowIndex(int showIndex){
        this.showIndex = showIndex;
    }

    /**
     * 刷新数据
     */
    public void refreshContent(){
        //正在刷新
        isRefreshing=true;
        //禁止自动轮播
        isAllowAutoSlid=false;
        reInitValue();
        isAllowAutoSlid=true;
        isRefreshing=false;
        invalidate();
    }

    /**
     *定义点击接口
     */
    public interface OnTopImageClickListeners{
         void onClick(int showIndex);
    }

    /**
     * 设置点击接口
     * @param listeners
     */
    public void setOnTopImageClickListeners(OnTopImageClickListeners listeners){
        onTopImageClickListeners = listeners;
    }


}
