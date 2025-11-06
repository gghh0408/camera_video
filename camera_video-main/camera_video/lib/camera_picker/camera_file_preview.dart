import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';
import 'package:path/path.dart' as path;
import 'dart:math' as math;

enum XCameraPickerViewType { image, video }

class XCameraPickerViewer extends StatefulWidget {
  XCameraPickerViewer._({Key? key, required this.filePath, this.finishText = '发送', this.createViewerState})
    : super(key: key);

  /// The [XFile] of the preview file.
  /// 预览文件的 [XFile] 实例
  final String filePath;

  /// Creates a customized [CameraPickerViewerState].
  /// 构建一个自定义的 [CameraPickerViewerState]。
  final XCameraPickerViewerState Function()? createViewerState;
  String finishText = "send";

  /// Static method to push with the navigator.
  /// 跳转至选择预览的静态方法
  static Future pushToViewer(
    BuildContext context, {
    required String filePath,
    String finishText = '',
    XCameraPickerViewerState Function()? createViewerState,
    bool useRootNavigator = true,
  }) {
    if (finishText == '') {
      finishText = 'send';
    }
    return Navigator.of(context, rootNavigator: useRootNavigator).push(
      PageRouteBuilder(
        pageBuilder: (_, __, ___) =>
            XCameraPickerViewer._(filePath: filePath, finishText: finishText, createViewerState: createViewerState),
        transitionsBuilder:
            (BuildContext context, Animation<double> animation, Animation<double> secondaryAnimation, Widget child) {
              return FadeTransition(opacity: animation, child: child);
            },
      ),
    );
  }

  @override
  XCameraPickerViewerState createState() =>
      // ignore: no_logic_in_create_state
      createViewerState?.call() ?? XCameraPickerViewerState();
}

class XCameraPickerViewerState extends State<XCameraPickerViewer> {
  /// Whether the player is playing.
  /// 播放器是否在播放
  final ValueNotifier<bool> isPlaying = ValueNotifier<bool>(false);

  // late final ThemeData theme =
  //     widget.pickerConfig.theme ?? CameraPicker.themeData(wechatThemeColor);

  /// 通过 [filePath] 构建 [File] 实例。
  late final File previewFile = File(widget.filePath);

  /// Controller for the video player.
  /// 视频播放的控制器
  late final VideoPlayerController videoController = VideoPlayerController.file(previewFile);

  /// Whether the controller is playing.
  /// 播放控制器是否在播放
  bool get isControllerPlaying => videoController.value.isPlaying;

  /// Whether the controller has initialized.
  /// 控制器是否已初始化
  late bool hasLoaded = false;

  /// Whether there's any error when initialize the video controller.
  /// 初始化视频控制器时是否发生错误
  bool hasErrorWhenInitializing = false;

  /// Whether the saving process is ongoing.
  bool isSavingEntity = false;

  // CameraErrorHandler? get onError => widget.pickerConfig.onError;

  @override
  void initState() {
    super.initState();
    initializeVideoPlayerController();
  }

  @override
  void dispose() {
    videoController
      ..removeListener(videoControllerListener)
      ..pause()
      ..dispose();
    super.dispose();
  }

  Future<void> initializeVideoPlayerController() async {
    // try {
    await videoController.initialize();
    videoController.addListener(videoControllerListener);
    hasLoaded = true;
    // if (widget.pickerConfig.shouldAutoPreviewVideo) {
    // videoController.play();
    // }
    // } catch (e, s) {
    //   hasErrorWhenInitializing = true;
    //   // realDebugPrint('Error when initializing video controller: $e');
    //   // handleErrorWithHandler(e, onError, s: s);
    // } finally {
    if (mounted) {
      setState(() {});
    }
    // }
  }

  /// Listener for the video player.
  /// 播放器的监听方法
  void videoControllerListener() {
    // if (isControllerPlaying == false && videoController.value.duration == videoController.value.position) {
    //   videoController.seekTo(Duration.zero);
    // }
    if (isControllerPlaying != isPlaying.value) {
      isPlaying.value = isControllerPlaying;
    }
  }

  /// Callback for the play button.
  /// 播放按钮的回调
  ///
  /// Normally it only switches play state for the player. If the video reaches
  /// the end, then click the button will make the video replay.
  /// 一般来说按钮只切换播放暂停。当视频播放结束时，点击按钮将从头开始播放。
  Future<void> playButtonCallback() async {
    debugPrint(
      '播放按钮的回调isPlaying=${isPlaying.value}, duration=${videoController.value.duration}, position=${videoController.value.position}',
    );
    if (isPlaying.value) {
      videoController.pause();
    } else {
      if (videoController.value.duration == videoController.value.position) {
        videoController
          ..seekTo(Duration.zero)
          ..play();
      } else {
        videoController.play();
      }
    }
  }

  /// The back button for the preview section.
  /// 预览区的返回按钮
  Widget buildBackButton(BuildContext context) {
    return Semantics(
      // sortKey: const OrdinalSortKey(0),
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: GestureDetector(
          onTap: () {
            if (isSavingEntity) {
              return;
            }
            if (previewFile.existsSync()) {
              previewFile.delete();
            }
            Navigator.of(context).pop();
            Navigator.of(context).pop();
          },
          child: Image(image: AssetImage("assets/images/img_nav_vedio_close.png"), width: 56, height: 56),
        ),
      ),
    );
  }

  Widget buildPreview(BuildContext context) {
    final Widget builder = Stack(
      alignment: AlignmentDirectional.center,
      children: <Widget>[
        Center(
          child: AspectRatio(aspectRatio: videoController.value.aspectRatio, child: VideoPlayer(videoController)),
        ),
        buildPlayControlButton(context),
      ],
    );
    return builder;
  }

  /// A play control button the video playing process.
  /// 控制视频播放的按钮
  Widget buildPlayControlButton(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: isPlaying,
      builder: (_, bool value, Widget? child) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: value ? playButtonCallback : null,
        child: Center(
          child: AnimatedOpacity(
            duration: kThemeAnimationDuration,
            opacity: value ? 0 : 1,
            child: GestureDetector(
              onTap: playButtonCallback,
              child: DecoratedBox(
                decoration: const BoxDecoration(
                  boxShadow: <BoxShadow>[BoxShadow(color: Colors.black12)],
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  value ? Icons.pause_circle_outline : Icons.play_circle_filled,
                  size: 70,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (hasErrorWhenInitializing) {
      return Center(child: Text('Constants.textDelegate.loadFailed', style: const TextStyle(inherit: false)));
    }
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          Column(
            children: <Widget>[Expanded(child: Center(child: hasLoaded ? buildPreview(context) : Container()))],
          ),
        ],
      ),
    );
  }
}

class _WechatLoading extends StatefulWidget {
  const _WechatLoading({Key? key, required this.tip}) : super(key: key);

  final String tip;

  @override
  State<_WechatLoading> createState() => _WechatLoadingState();
}

class _WechatLoadingState extends State<_WechatLoading> with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(duration: const Duration(seconds: 2), vsync: this);

  @override
  void initState() {
    super.initState();
    _controller.repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Widget _buildContent(BuildContext context, double minWidth) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        SizedBox.fromSize(
          size: Size.square(minWidth / 3),
          child: AnimatedBuilder(
            animation: _controller,
            builder: (_, Widget? child) => Transform.rotate(angle: math.pi * 2 * _controller.value, child: child),
            child: CustomPaint(painter: _LoadingPainter(Theme.of(context).textTheme.bodyMedium?.color)),
          ),
        ),
        SizedBox(height: minWidth / 10),
        Text(widget.tip, style: const TextStyle(fontSize: 14), textScaleFactor: 1),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final double minWidth = MediaQuery.of(context).size.shortestSide / 3;
    return Container(
      color: Colors.black38,
      alignment: Alignment.center,
      child: RepaintBoundary(
        child: Container(
          constraints: BoxConstraints(minWidth: minWidth),
          padding: EdgeInsets.all(minWidth / 5),
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(10), color: Theme.of(context).canvasColor),
          child: _buildContent(context, minWidth),
        ),
      ),
    );
  }
}

class _LoadingPainter extends CustomPainter {
  const _LoadingPainter(this.activeColor);

  final Color? activeColor;

  @override
  void paint(Canvas canvas, Size size) {
    final Color color = activeColor ?? Colors.white;
    final Offset center = Offset(size.width / 2, size.height / 2);
    final Rect rect = Rect.fromCenter(center: center, width: size.width, height: size.height);
    final Paint paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = 4
      ..shader = SweepGradient(colors: <Color>[color.withOpacity(0), color]).createShader(rect);
    canvas.drawArc(rect, 0.1, math.pi * 2 * 0.9, false, paint);
  }

  @override
  bool shouldRepaint(_LoadingPainter oldDelegate) => false;
}
