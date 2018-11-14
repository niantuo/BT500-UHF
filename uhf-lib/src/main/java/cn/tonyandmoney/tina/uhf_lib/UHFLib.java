package cn.tonyandmoney.tina.uhf_lib;

import android.content.Context;
import android.content.res.Resources;

public class UHFLib {

    private static Context mAppContext;

    public static Context context() {
        return mAppContext;
    }

    public static void setContext(Context application) {
        UHFLib.mAppContext = application;
    }

    public static Resources getResources(){
        return mAppContext.getResources();
    }
}
