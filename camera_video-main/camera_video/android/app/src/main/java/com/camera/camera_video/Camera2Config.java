package com.camera.camera_video;


public class Camera2Config {
    public static int RECORD_MAX_TIME = 60;//录制的总时长秒数，单位秒，默认10秒
    public static int RECORD_MIN_TIME = 1;//最小录制时长，单位秒，默认1秒
    public static String PATH_SAVE_VIDEO = Camera2Util.getCamera2Path();//小视频存放地址，不设置的话默认在根目录的Camera2文件夹
    public static String PATH_SAVE_PIC = Camera2Util.getCamera2Path();//图片保存地址，不设置的话默认在根目录的Camera2文件夹
    public static boolean ENABLE_RECORD = true;//是否需要录像功能
    public static boolean ENABLE_CAPTURE = true;//是否需要拍照功能
}
