/*
 *  Copyright (c) 2015 The CCP project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a Beijing Speedtong Information Technology Co.,Ltd license
 *  that can be found in the LICENSE file in the root of the web site.
 *
 *   http://www.yuntongxun.com
 *
 *  An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */package com.yuntongxun.ecdemo.common.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.yuntongxun.ecdemo.ECApplication;

/**
 * Created by Jorstin on 2015/3/18.
 */
public class DensityUtil {

    // 根据屏幕密度转换
    private static float mPixels = 0.0F;
    private static float density = -1.0F;

    /**
     *
     * @param context
     * @param pixels
     * @return
     */
    public static int getDisplayMetrics(Context context, float pixels) {
        if (mPixels == 0.0F)
            mPixels = context.getResources().getDisplayMetrics().density;
        return (int) (0.5F + pixels * mPixels);
    }


    public static int getImageWeidth(Context context , float pixels) {
        LogUtil.e("screen width " + context.getResources().getDisplayMetrics().widthPixels);
        return context.getResources().getDisplayMetrics().widthPixels - 66 - getDisplayMetrics(context, pixels);
    }

    /**
     * 像素转化dip
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue){

        final float scale = context.getResources().getDisplayMetrics().density;

        return (int)(pxValue / scale + 0.5f);

    }

    /**
     * dip转化像素
     * @param dipValue
     * @return
     */
    public static int dip2px(float dipValue){
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);

    }

    /**
     * @param context
     * @param height
     * @return
     */
    public static int getMetricsDensity(Context context , float height) {
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(localDisplayMetrics);
        return Math.round(height * localDisplayMetrics.densityDpi / 160.0F);
    }


    public static int fromDPToPix(Context context, int dp) {
        return Math.round(getDensity(context) * dp);
    }

    /**
     * @param context
     * @return
     */
    public static float getDensity(Context context) {
        if (context == null)
            context = ECApplication.getInstance().getApplicationContext();
        if (density < 0.0F)
            density = context.getResources().getDisplayMetrics().density;
        return density;
    }

    public static int round(Context context, int paramInt) {
        return Math.round(paramInt / getDensity(context));
    }
}
