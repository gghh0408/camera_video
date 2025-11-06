package com.camera.camera_video;

import android.content.Context;
import io.flutter.app.FlutterApplication;

public class XChatApp extends FlutterApplication {
    public static boolean portrait = true;
    public static boolean isPickUp = false;//是否为拾起状态
//    public static ChannelUtil channelUtil;
    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化bugly
        Context context = getApplicationContext();
        mContext = context;
//        channelUtil = new ChannelUtil();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
