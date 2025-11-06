package com.camera.camera_video;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StandardMessageCodec;

public class XChatCameraPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {

    private MethodChannel mChannel;
    private XChatCameraFactory mXChatCameraFactory;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        //创建通道对象，通道名称与Flutter端通道的名称一致
        mChannel = new MethodChannel(binding.getBinaryMessenger(), "xchat.flutter.cameraPlatformView");
        //注册此通道的监听
        mChannel.setMethodCallHandler(this);
        //创建PlatformView的工厂对象
        mXChatCameraFactory = new XChatCameraFactory(StandardMessageCodec.INSTANCE, mChannel);
        //在Flutter引擎上注册PlatformView的工厂对象
        binding.getPlatformViewRegistry()
                .registerViewFactory("android/cameraView", mXChatCameraFactory);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        //注销通道的监听
        mChannel.setMethodCallHandler(null);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        //获取监听到的函数名称
        String methodName = call.method;

    }
}

