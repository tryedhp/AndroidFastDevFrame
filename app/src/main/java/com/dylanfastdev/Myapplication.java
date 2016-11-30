package com.dylanfastdev;

import android.app.Application;

import com.orhanobut.logger.Logger;

import org.xutils.x;

import cn.bluemobi.dylan.fastdev.config.Config;

/**
 * Created by yuandl on 2016/9/1 0001.
 */
public class Myapplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        x.Ext.init(this);
        Logger.init("yanhao");
        Config.initTitleBar(R.color.colorAccent,android.R.color.white,R.drawable.arrow_back_write);
    }
}