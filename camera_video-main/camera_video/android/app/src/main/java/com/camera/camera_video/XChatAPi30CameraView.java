package com.camera.camera_video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Range;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import java.math.BigDecimal;

import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import io.flutter.plugin.common.MethodChannel;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaCodec;

public class XChatAPi30CameraView extends FrameLayout implements TextureView.SurfaceTextureListener {
    private MethodChannel mMethodChannel;

    public XChatAPi30CameraView(@NonNull Context context) {
        this(context, null);
    }

    public XChatAPi30CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XChatAPi30CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.activity_camera2_record, this);
    }

    public void setMethodChannel(MethodChannel channel) {
        mMethodChannel = channel;
        initViews();
        initTextureView();
    }

    private boolean isForceLandscape = false;
    //views
    private AutoFitTextureView mTextureView;
    private CameraBtn ivTakePhoto;//拍照&录像按钮
    private ImageView ivClose;//关闭
    //拍照方向
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    //constant
    private static final String TAG = "XChatCameraView";
    private static final int CAPTURE_OK = 0;//拍照完成回调
    private String mCameraId;//后置摄像头ID
    private String mCameraIdFront;//前置摄像头ID
    private Size mPreviewSize;//预览的Size
    private Size mVideoPreviewSize;//预览的Size
    private boolean isCameraFront = false;//当前是否是前置摄像头

    //Camera2
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mPreviewSession;
    private CameraCharacteristics characteristics;
    private ImageReader mImageReader;
    private String picSavePath;//图片保存路径
    private String videoSavePath;//视频保存路径

    //handler
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private LongPressRunnable longPressRunnable;//长按后处理的逻辑Runnable

    //录像
    private static final int MAX_RECORD_TIME = Camera2Config.RECORD_MAX_TIME;//最大录制时长,默认10S
    private static final int MIN_RECORD_TIME = Camera2Config.RECORD_MIN_TIME;//最小录制时长，默认1S
    private boolean isRecording = false;//是否正在录制视频
    private boolean isStop = false;//是否停止过了MediaRecorder
    private float currentTime;
    private MediaRecorder mMediaRecorder;

    /**
     * **************************************初始化相关**********************************************
     */
    //初始化TextureView
    private void initTextureView() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        mTextureView.setSurfaceTextureListener(this);
    }

    //初始化视图控件
    private void initViews() {
        mTextureView = findViewById(R.id.textureView);
        ivTakePhoto = findViewById(R.id.iv_takePhoto);
        ivClose = findViewById(R.id.iv_close);
        ivClose.setOnClickListener(clickListener);
        //触摸事件
        onTouchListner();
        initScaleBtnListener();
    }

    OnClickListener clickListener = view -> {
        int i = view.getId();
        if (i == R.id.iv_close) {
            //关闭Activity
            mMethodChannel.invokeMethod("back", null);
        }
    };
    private float downY = 0;
    private float curMoveY = 0;

    //拍照按钮触摸事件
    private void onTouchListner() {
        longPressRunnable = new LongPressRunnable();
        ivTakePhoto.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downY = event.getRawY();
                    isRecording = false;
                    mCameraHandler.postDelayed(longPressRunnable, 250);//同时延长400启动长按后处理的逻辑Runnable
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isRecording) {
                        if (curMoveY == 0) {
                            curMoveY = downY;
                        }
                        float moveY = event.getRawY();
                        if (moveY + 18 < curMoveY) {//往上移动，放大
                            curMoveY = moveY;
                            handleZoom(true);
                        } else if (moveY - 18 > curMoveY) {//往上移动，缩小
                            curMoveY = moveY;
                            handleZoom(false);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    curMoveY = 0;
                    //根据当前按钮的状态进行相应的处理
                    handlerUnpressByState();
                    break;
            }
            return true;
        });

        mTextureView.setOnTouchListener((view, event) -> {
            //两指缩放
            changeZoom(event);
            return true;
        });
    }

    //长按线程
    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            //判断是否需要录像功能
            if (Camera2Config.ENABLE_RECORD) {
                prepareMediaRecorder();
                startButtonAnima();
                isRecording = true; //如果按下后经过500毫秒则会修改当前状态为长按状态，标记为正在录制中
                startMediaRecorder();//开始录制
            }
        }
    }

    //当手指松开按钮时候处理的逻辑
    private void handlerUnpressByState() {
        mCameraHandler.removeCallbacks(longPressRunnable);//移除长按逻辑的Runnable
        //根据当前状态处理
        if (isRecording) {
            stopButtonAnima();
            isRecording = false;
            mCameraHandler.post(() -> {
                //停止录制，先判断是不是停止过了
                if (!isStop) {
                    stopMediaRecorder();
                }
            });
        } else {
            isRecording = false;
            //判断是否需要拍照功能
            if (Camera2Config.ENABLE_CAPTURE) {
                capture();
            }
        }
    }

    //开始按下按钮动画
    public void startButtonAnima() {
        AnimatorSet animatorSet = new AnimatorSet();//组合动画
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivTakePhoto, "scaleX", 1f, 1.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivTakePhoto, "scaleY", 1f, 1.3f);

        animatorSet.setDuration(100);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.play(scaleX).with(scaleY);//两个动画同时开始
        animatorSet.start();
    }

    //停止按下按钮动画
    public void stopButtonAnima() {
        AnimatorSet animatorSet = new AnimatorSet();//组合动画
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivTakePhoto, "scaleX", 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivTakePhoto, "scaleY", 1.3f, 1f);

        animatorSet.setDuration(100);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.play(scaleX).with(scaleY);//两个动画同时开始
        animatorSet.start();
    }

    /**
     * ******************************SurfaceTextureListener*****************************************
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        //当SurefaceTexture可用的时候，设置相机参数并打开相机
        setupCamera();//配置相机参数
        openCamera(mCameraId);//打开相机
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
//        configureTransform(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    /**
     * ******************************SetupCamera(配置Camera)*****************************************
     */
    private void setupCamera() {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) MainActivity.mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            //0表示后置摄像头,1表示前置摄像头
            mCameraId = cameraIdList[0];
            mCameraIdFront = cameraIdList[1];

            //前置摄像头和后置摄像头的参数属性不同，所以这里要做下判断
            Range zoomRange = null;
            if (isCameraFront) {
                characteristics = manager.getCameraCharacteristics(mCameraIdFront);
            } else {
                characteristics = manager.getCameraCharacteristics(mCameraId);
            }
            if (Build.VERSION.SDK_INT >= 30) {
                zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
            }
            if (zoomRange != null) {
                SCALE_MIN_ZOOM = (float) zoomRange.getLower();
                SCALE_MAX_ZOOM = (float) zoomRange.getUpper();
            }
            //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //选择预览尺寸
            WindowManager mWindowManager = (WindowManager) MainActivity.mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = mWindowManager.getDefaultDisplay();
            Size tempSize = Camera2Util.getOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class), display.getWidth(), display.getHeight());
            if (tempSize == null) {
                tempSize = Collections.max(Arrays.asList(map.getOutputSizes(SurfaceTexture.class)), new CompareSizeByArea());
            }
            mPreviewSize = tempSize;
            mCurZoom = 1.0f;
            mTextureView.setAspectRatio(mPreviewSize);

//            Size tempVideoSize = Camera2Util.getVideoPreviewSize(map.getOutputSizes(SurfaceTexture.class), display.getWidth(), display.getHeight());
//            if (tempVideoSize == null) {
//                tempVideoSize = tempSize;
//            }
//            mVideoPreviewSize = tempVideoSize;
            //此ImageReader用于拍照所需
            setupImageReader();

            //MediaRecorder用于录像所需
            mMediaRecorder = new MediaRecorder();
            changeScaleBtnState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            //确保乘法不会溢出范围
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    //配置ImageReader
    private void setupImageReader() {
        //2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(reader -> {
            Image mImage = reader.acquireNextImage();
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Camera2Util.createSavePath(Camera2Config.PATH_SAVE_PIC);//判断有没有这个文件夹，没有的话需要创建
            picSavePath = Camera2Config.PATH_SAVE_PIC + "IMG_" + timeStamp + ".jpg";
            FileOutputStream fos = null;
            try {
                if (isCameraFront) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Matrix matrix = new Matrix();
                    matrix.postScale(-1, 1);
                    if (XChatApp.portrait) {
                        matrix.postRotate(90);
                    }
                    Bitmap tempBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                            bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    tempBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    data = byteArrayOutputStream.toByteArray();
                    bitmap.recycle();
                    tempBitmap.recycle();
                    bitmap = null;
                    tempBitmap = null;
                }
                fos = new FileOutputStream(picSavePath);
                fos.write(data, 0, data.length);

                Message msg = new Message();
                msg.what = CAPTURE_OK;
                msg.obj = picSavePath;
                mCameraHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            mImage.close();
        }, mCameraHandler);

        mCameraHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case CAPTURE_OK:
                        //这里拍照保存完成
                        HashMap<String, String> map = new HashMap<>();
                        map.put("path", picSavePath);
                        mMethodChannel.invokeMethod("takePhotoEnd", map);
                        break;
                }
            }
        };
    }

    /**
     * ******************************openCamera(打开Camera)*****************************************
     */
    private void openCamera(String CameraId) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) MainActivity.mContext.getSystemService(Context.CAMERA_SERVICE);
        //检查权限
        try {
            if (ActivityCompat.checkSelfPermission(MainActivity.mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            manager.openCamera(CameraId, mStateCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    /**
     * ******************************Camera2成功打开，开始预览(startPreview)*************************
     */
    @SuppressLint("NewApi")
    public void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();//获取TextureView的SurfaceTexture，作为预览输出载体

        if (mSurfaceTexture == null) {
            return;
        }

        try {
            closePreviewSession();
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());//设置TextureView的缓冲区大小
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);//创建CaptureRequestBuilder，TEMPLATE_PREVIEW比表示预览请求
            Surface mSurface = new Surface(mSurfaceTexture);//获取Surface显示预览数据
            mPreviewBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, mCurZoom);
            //默认预览不开启闪光灯
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            mPreviewBuilder.addTarget(mSurface);//设置Surface作为预览数据的显示界面
            setCaptureRequestParams(mPreviewBuilder, 0);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        //创建捕获请求
                        mCaptureRequest = mPreviewBuilder.build();
                        mPreviewSession = session;
                        //不停的发送获取图像请求，完成连续预览
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ********************************************拍照*********************************************
     */
    @SuppressLint("NewApi")
    private void capture() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            //获取屏幕方向
            int rotation = ((Activity) MainActivity.mContext).getWindowManager().getDefaultDisplay().getRotation();
            CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            if (isCameraFront) {
                mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, !isForceLandscape && XChatApp.portrait ? ORIENTATION.get(Surface.ROTATION_180) : 180);
            } else {
                mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, !isForceLandscape && XChatApp.portrait ? ORIENTATION.get(rotation) : 0);
            }
            mCaptureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            mCaptureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, mCurZoom);
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            // 设置3A参数
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            // 设置中心对焦区域
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,
                    new MeteringRectangle[]{getCenterFocusRegion()});
            CameraCaptureSession.CaptureCallback mCallBack = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    //拍完照unLockFocus
                    unLockFocus();
                }
            };
            mPreviewSession.stopRepeating();
            mPreviewSession.abortCaptures();
            setCaptureRequestParams(mCaptureBuilder, 2);
            //咔擦拍照
            mPreviewSession.capture(mCaptureBuilder.build(), mCallBack, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCaptureRequestParams(CaptureRequest.Builder builder, int type) {
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
//        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
    }

    @SuppressLint("NewApi")
    private void unLockFocus() {
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            //闪光灯重置为未开启状态
            mCurZoom = 1.0f;
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            //继续开启预览
            mPreviewBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, mCurZoom);
            setCaptureRequestParams(mPreviewBuilder, 0);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
            changeScaleBtnState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ********************************************录像*********************************************
     */
    private void setUpMediaRecorder() {
        try {
            mMediaRecorder.reset();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            if (isCameraFront) {
                mMediaRecorder.setOrientationHint(XChatApp.portrait ? 270 : 0);
            } else {
                mMediaRecorder.setOrientationHint(XChatApp.portrait ? 90 : 0);
            }
            //判断有没有配置过视频地址了
            Camera2Util.createSavePath(Camera2Config.PATH_SAVE_VIDEO);//判断有没有这个文件夹，没有的话需要创建
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            videoSavePath = Camera2Config.PATH_SAVE_VIDEO + "VIDEO_" + timeStamp + ".mp4";
            mMediaRecorder.setOutputFile(videoSavePath);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioChannels(1);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setCaptureRate(30);
            mMediaRecorder.setAudioSamplingRate(44100);
            mMediaRecorder.setAudioEncodingBitRate(48000);
            mMediaRecorder.setVideoEncodingBitRate(2 * 1280 * 720);
            mMediaRecorder.setVideoSize(1280, 720);
            mMediaRecorder.setPreviewDisplay(getPreviewSurface());
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Surface getPreviewSurface() {
        return new Surface(mTextureView.getSurfaceTexture());
    }

    //预览录像
    @SuppressLint("NewApi")
    private void prepareMediaRecorder() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            //判断预览之前有没有开闪光灯
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            mPreviewBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, mCurZoom);
            // 设置防抖
            mPreviewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
            // 设置3A参数
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            // 设置中心对焦区域
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,
                    new MeteringRectangle[]{getCenterFocusRegion()});
            setCaptureRequestParams(mPreviewBuilder, 1);
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        //创建捕获请求
                        mCaptureRequest = mPreviewBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.mContext, "onConfigured Failed e=" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(MainActivity.mContext, "onConfigureFailed", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.mContext, "prepareMediaRecorder Failed e=" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //开始录像
    private void startMediaRecorder() {
        try {
            mMediaRecorder.start();
            //开始计时，判断是否已经超过录制时间了
            mCameraHandler.post(recordRunnable);
            isStop = false;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.mContext, "startMediaRecorder Failed e=" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            currentTime += 0.1;
            //开始显示进度条
            ivTakePhoto.setCurrentNum(currentTime);
            //如果超过最大录制时长则自动结束
            if (currentTime > MAX_RECORD_TIME) {
                stopMediaRecorder();
            } else {
                try {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                    mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCameraHandler.postDelayed(this, 100);
            }
        }
    };

    //停止录像
    private void stopMediaRecorder() {
        if (TextUtils.isEmpty(videoSavePath)) {
            return;
        }
        try {
            mCameraHandler.removeCallbacks(recordRunnable);
            mMediaRecorder.setOnInfoListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            //结束ProgressView
            ivTakePhoto.setCurrentNum(0);
            mMediaRecorder.reset();
            isStop = true;

            //判断录制时常是不是小于指定秒数
            if (currentTime <= MIN_RECORD_TIME) {
                Toast.makeText(MainActivity.mContext, "Too short to create video file!", Toast.LENGTH_LONG).show();
                showResetCameraLayout();
            } else {
                flipVideo();
            }
        } catch (Exception e) {
            e.printStackTrace();
            //这里抛出的异常是由于MediaRecorder开关时间过于短暂导致，直接按照录制时间短处理
            Toast.makeText(MainActivity.mContext, "Too short to create video file!", Toast.LENGTH_LONG).show();
            showResetCameraLayout();
        }

        currentTime = 0;
    }

    private void flipVideo() {
        //正常录制结束
        HashMap<String, String> map = new HashMap<>();
        map.put("path", videoSavePath);
        mMethodChannel.invokeMethod("takeVideoEnd", map);
        showResetCameraLayout();
    }

    public void showResetCameraLayout() {
        resetCamera();
        ivTakePhoto.setCurrentNum(0);
    }

    /**
     * **********************************************切换摄像头**************************************
     */
    public void switchCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (isCameraFront) {
            isCameraFront = false;
            setupCamera();
            openCamera(mCameraId);
        } else {
            isCameraFront = true;
            setupCamera();
            openCamera(mCameraIdFront);
        }
    }

    /**
     * *********************************放大或者缩小**********************************
     */
    //手指按下的点为(x1, y1)手指离开屏幕的点为(x2, y2)
    float finger_spacing;

    public void changeZoom(MotionEvent event) {
        try {
            int action = event.getAction();
            float current_finger_spacing;
            //判断当前屏幕的手指数
            if (event.getPointerCount() > 1) {
                //计算两个触摸点的距离
                current_finger_spacing = getFingerSpacing(event);
                if (finger_spacing != 0) {
                    if (current_finger_spacing > finger_spacing) {
                        if (current_finger_spacing > finger_spacing + 10) {
                            handleZoom(true);
                            finger_spacing = current_finger_spacing;
                        }
                    } else if (current_finger_spacing < finger_spacing) {
                        if (current_finger_spacing < finger_spacing - 10) {
                            handleZoom(false);
                            finger_spacing = current_finger_spacing;
                        }
                    }
                } else {
                    finger_spacing = current_finger_spacing;
                }
            } else {
                if (action == MotionEvent.ACTION_UP) {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                    refreshPreview();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("can not access camera.", e);
        }
    }

    //计算两个触摸点的距离
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float SCALE_MAX_ZOOM = 10.0f; // 放大的最大值，用于计算每次放大/缩小操作改变的大小
    private float mCurZoom = 1.0f; // 缩放

    private float SCALE_MIN_ZOOM = 0.6f; // 最小缩放

    @SuppressLint("NewApi")
    public void handleZoom(boolean isZoomIn) {
        if (mCameraDevice == null || characteristics == null || mPreviewBuilder == null) {
            return;
        }
        float tempZoom = 0.0f;
        if (isZoomIn && mCurZoom < SCALE_MAX_ZOOM) { // 放大
            BigDecimal a = new BigDecimal(String.valueOf(mCurZoom));
            BigDecimal b = new BigDecimal(String.valueOf(0.1));
            tempZoom = a.add(b).floatValue();
        } else if (mCurZoom > SCALE_MIN_ZOOM) { // 缩小
            BigDecimal a = new BigDecimal(String.valueOf(mCurZoom));
            BigDecimal b = new BigDecimal(String.valueOf(0.1));
            tempZoom = a.subtract(b).floatValue();
        } else {
            return;
        }
        if (tempZoom != mCurZoom) {
            mCurZoom = tempZoom;
            changeScaleBtnState();
            refreshPreview();
        }
    }

    /**
     * **************************************清除操作************************************************
     */
    public void onFinishCapture() {
        try {
            if (mPreviewSession != null) {
                mPreviewSession.close();
                mPreviewSession = null;
            }

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }

            if (mMediaRecorder != null) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

            if (mCameraHandler != null) {
                mCameraHandler.removeCallbacksAndMessages(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //清除预览Session
    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    //重新配置打开相机
    public void resetCamera() {
        if (TextUtils.isEmpty(mCameraId)) {
            return;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
        }

        setupCamera();
        openCamera(isCameraFront ? mCameraIdFront : mCameraId);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            isCameraFront = false;
            resetCamera();
        } else {
            onFinishCapture();//释放资源
        }
    }

    @SuppressLint("NewApi")
    private void initScaleBtnListener() {
        changeScaleBtnState();
    }

    private void startScale(View view) {
        RotateAnimation mRotateAnimation;
        if (curPhoneDirection == 3 && lastPhoneDirection == 0) {
            mRotateAnimation = new RotateAnimation(0, -90,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        } else if (curPhoneDirection == 0 && lastPhoneDirection == 3) {
            mRotateAnimation = new RotateAnimation(-90, 0,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        } else {
            mRotateAnimation = new RotateAnimation(lastPhoneDirection * 90, curPhoneDirection * 90,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        }
        // 设置动画时长
        mRotateAnimation.setDuration(500);
        mRotateAnimation.setFillAfter(true);
        // 启动动画
        view.startAnimation(mRotateAnimation);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private int curPhoneDirection = 0; // 0 正竖屏，顺时针旋转 1 2 3
    private int lastPhoneDirection = 0; // 0 正竖屏，顺时针旋转 1 2 3

    @SuppressLint("NewApi")
    private void refreshPreview() {
        mPreviewBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, mCurZoom);
        setCaptureRequestParams(mPreviewBuilder, 2);
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeScaleBtnState() {

    }

    private MeteringRectangle getCenterFocusRegion() {
        // 获取相机传感器的活动区域大小
        Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        if (activeArraySize == null) {
            // 如果无法获取传感器区域，返回默认的中心区域
            return new MeteringRectangle(
                    new Rect(0, 0, getWidth(), getHeight()),
                    MeteringRectangle.METERING_WEIGHT_MAX
            );
        }

        // 计算中心点坐标
        int centerX = activeArraySize.width() / 2;
        int centerY = activeArraySize.height() / 2;

        // 设置对焦区域大小（通常为传感器区域的10-15%）
        int regionWidth = activeArraySize.width() / 10;
        int regionHeight = activeArraySize.height() / 10;

        // 创建对焦区域矩形
        Rect focusRect = new Rect(
                centerX - regionWidth / 2,  // 左
                centerY - regionHeight / 2, // 上
                centerX + regionWidth / 2,  // 右
                centerY + regionHeight / 2  // 下
        );

        // 确保矩形在有效范围内
        focusRect.left = Math.max(focusRect.left, activeArraySize.left);
        focusRect.top = Math.max(focusRect.top, activeArraySize.top);
        focusRect.right = Math.min(focusRect.right, activeArraySize.right);
        focusRect.bottom = Math.min(focusRect.bottom, activeArraySize.bottom);

        // 创建MeteringRectangle，权重设置为最大值
        return new MeteringRectangle(
                focusRect,
                MeteringRectangle.METERING_WEIGHT_MAX
        );
    }
}
