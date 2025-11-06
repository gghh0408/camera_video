package com.camera.camera_video;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;

public class MainActivity extends FlutterActivity {
    public static Context mContext;
    private FlutterEngine curEngine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        curEngine = getFlutterEngine();
        curEngine.getPlugins().add(new XChatCameraPlugin());
    }
}
