/*
 * @FilePath: camera_platform_view.dart
 * @Author: linhuaqin
 * @Date: 2023-04-13 20:55:12
 * @LastEditors: Please set LastEditors
 * @LastEditTime: 2023-08-17 17:05:55
 * Copyright: 2023 XhorseTech CO.,LTD. All Rights Reserved.
 * @Descripttion: 
 */
import 'package:flutter/gestures.dart';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'camera_file_preview.dart';

class CameraPlatformPage extends StatefulWidget {
  CameraPlatformPage({
    super.key,
  });

  static Future<void> pushToViewer(
    BuildContext context, {
    bool useRootNavigator = true,
  }) {
    return Navigator.of(
      context,
      rootNavigator: useRootNavigator,
    ).push(MaterialPageRoute(
        fullscreenDialog: true,
        builder: (context) {
          return CameraPlatformPage();
        }));
  }

  @override
  State<CameraPlatformPage> createState() => _CameraPlatformPageState();
}

class _CameraPlatformPageState extends State<CameraPlatformPage> {
  @override
  void initState() {
    super.initState();
    initChannel();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Container(
        width: double.infinity,
        height: double.infinity,
        child: AndroidView(
                viewType: "android/cameraView",
                creationParamsCodec: StandardMessageCodec(),
                gestureRecognizers: Set()..add(Factory<LongPressGestureRecognizer>(() => LongPressGestureRecognizer())),
              )
      ),
    );
  }

  initChannel() async {
    MethodChannel channel = MethodChannel('xchat.flutter.cameraPlatformView');
    channel.setMethodCallHandler((call) async {
      debugPrint('传回来的数据=$call');
      if (call.method == 'back') {
        Navigator.pop(context);
      } else if (call.method == 'takeVideoEnd') {
        Map dataMap = call.arguments;
        String path = dataMap['path'];
        pushToViewer(filePath: path);
      }
    });
  }

  Future pushToViewer({required String filePath}) async {
    XCameraPickerViewer.pushToViewer(
      context,
      filePath: filePath,
      useRootNavigator: false,
    );
  }
}
