package com.camera.camera_video;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.flutter.plugin.common.MessageCodec;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class XChatCameraFactory extends PlatformViewFactory {
    private MethodChannel mChannel;
    public XChatCameraPlatform mXChatCameraPlatform;

    public XChatCameraFactory(MessageCodec<Object> createArgsCodec, MethodChannel channel) {
        super(createArgsCodec);
        mChannel = channel;
    }

    @NonNull
    @Override
    public PlatformView create(Context context, int viewId, @Nullable Object args) {
        mXChatCameraPlatform = new XChatCameraPlatform(context, mChannel);
        return mXChatCameraPlatform;
    }
}
