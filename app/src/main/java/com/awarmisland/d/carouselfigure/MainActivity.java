package com.awarmisland.d.carouselfigure;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
        CarouselFigure carouselFigure = (CarouselFigure) findViewById(R.id.carouselFigure);
        List<Bitmap> imgList = new ArrayList<Bitmap>();
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test1));
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test2));
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test3));
        imgList.add(BitmapFactory.decodeResource(getResources(),R.drawable.test4));
        carouselFigure.setImgList(imgList);
        carouselFigure.setShowIndex(0);
        carouselFigure.setOnTopImageClickListeners(new CarouselFigure.OnTopImageClickListeners() {
            @Override
            public void onClick(int showIndex) {
                Log.i("tag","index: "+showIndex);
            }
        });
    }
}
