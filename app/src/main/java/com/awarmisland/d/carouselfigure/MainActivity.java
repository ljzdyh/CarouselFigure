package com.awarmisland.d.carouselfigure;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private int i = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
        final CarouselFigure carouselFigure = (CarouselFigure) findViewById(R.id.carouselFigure);
        Button btn_refresh = (Button) findViewById(R.id.btn_refresh);
        final List<Bitmap> imgList = new ArrayList<>();
        final List<Bitmap> refresh_imgList = new ArrayList<>();
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test1));
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2));
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test3));
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test4));

        //更换数据源
        refresh_imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2_1));
//        refresh_imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2_2));
//        refresh_imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2_3));
//        refresh_imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2_4));
//        refresh_imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2_5));
//        refresh_imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2_6));

        carouselFigure.setImgList(imgList);
        carouselFigure.setShowIndex(0);
        carouselFigure.setOnTopImageClickListeners(new CarouselFigure.OnTopImageClickListeners() {
            @Override
            public void onClick(int showIndex) {
                Log.i("tag","index: "+showIndex);
            }
        });
        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(i%2==0) {
                    carouselFigure.setImgList(refresh_imgList);
                }else{
                    carouselFigure.setImgList(imgList);
                }
                i++;
                carouselFigure.refreshContent();
            }
        });
    }
}
