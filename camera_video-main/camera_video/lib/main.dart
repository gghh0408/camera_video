import 'dart:io';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:device_info_plus/device_info_plus.dart';

import 'camera_picker/camera_platform_view.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // TRY THIS: Try running your application with "flutter run". You'll see
        // the application has a purple toolbar. Then, without quitting the app,
        // try changing the seedColor in the colorScheme below to Colors.green
        // and then invoke "hot reload" (save your changes or press the "hot
        // reload" button in a Flutter-supported IDE, or press "r" if you used
        // the command line to start the app).
        //
        // Notice that the counter didn't reset back to zero; the application
        // state is not lost during the reload. To reset the state, use hot
        // restart instead.
        //
        // This works for code too, not just values: Most code changes can be
        // tested with just a hot reload.
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  Future<bool> requestPermission(Permission p) async {
    var status = await p.status;
    if (status == PermissionStatus.granted || status == PermissionStatus.limited) {
      return true;
    } else if (status == PermissionStatus.permanentlyDenied) {
      return false;
    } else {
      status = await p.request();
      if (status == PermissionStatus.granted || status == PermissionStatus.limited) {
        return true;
      } else {
        return false;
      }
    }
  }

  Future<bool> requestStoragePermission({bool isNeedAudioOrVideo = true}) async {
    // For Android 12 and before, use Permission.storage.request() for External Storage access
    // For Android 13 and above, use following Granular media permissions:
    //
    // Permission.photos.request() : Read image files from external storage
    // Permission.videos.request() : Read video files from external storage
    // Permission.audio.request() : Read audio files from external storage

    bool permissionResult = false;
    if (Platform.isAndroid) {
      final androidInfo = await DeviceInfoPlugin().androidInfo;
      if (androidInfo.version.sdkInt <= 32) {
        permissionResult = await requestPermission(Permission.storage);
      } else {
        bool request1 = await requestPermission(Permission.photos);
        bool request2 = await requestPermission(Permission.videos);
        bool request3 = await requestPermission(Permission.audio);
        permissionResult = request1 || request2 || request3;
      }
    }
    return permissionResult;
  }

  _goRecord() async {
    bool permissionResult = await requestPermission(Permission.camera);
    if (!permissionResult) {
      return;
    }
    permissionResult = await requestPermission(Permission.microphone);
    if (!permissionResult) {
      return;
    }
    permissionResult = await requestStoragePermission();
    if (!permissionResult) {
      return;
    }
    CameraPlatformPage.pushToViewer(context, useRootNavigator: false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(backgroundColor: Theme.of(context).colorScheme.inversePrimary, title: Text(widget.title)),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            GestureDetector(
              onTap: () {
                _goRecord();
              },
              child: Container(
                color: Colors.white,
                width: 200,
                height: 200,
                alignment: Alignment.center,
                child: Text('Click to record and play'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
