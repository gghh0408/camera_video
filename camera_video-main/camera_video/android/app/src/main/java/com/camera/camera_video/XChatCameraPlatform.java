package com.camera.camera_video;

import android.content.Context;
import android.os.Build;
import android.view.View;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

public class XChatCameraPlatform implements PlatformView {
    private XChatCameraView mCameraView;
    private XChatAPi30CameraView mApi30CameraView;

    public XChatCameraPlatform(Context context, MethodChannel channel) {
        if (Build.VERSION.SDK_INT >= 30) {
            mApi30CameraView = new XChatAPi30CameraView(context);
            mApi30CameraView.setMethodChannel(channel);
        } else {
            mCameraView = new XChatCameraView(context);
            mCameraView.setMethodChannel(channel);
        }
    }

    @Override
    public View getView() {
        if (Build.VERSION.SDK_INT >= 30) {
            return mApi30CameraView;
        } else {
            return mCameraView;
        }
    }

    @Override
    public void dispose() {
        if (Build.VERSION.SDK_INT >= 30) {
            mApi30CameraView.onFinishCapture();
        } else {
            mCameraView.onFinishCapture();
        }
    }
}
