package com.camera.camera_video;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import java.util.ArrayList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Camera2Util {
    //选择合适的视频size，并且不能大于1080p
    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }


    // 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择，activity我们已经固定了方向，所以这里无需在做判断
    protected static Size getCloselyPreSize(Size[] sizeMap, int surfaceWidth, int surfaceHeight) {
        int ReqTmpWidth;
        int ReqTmpHeight;
        ReqTmpWidth = surfaceHeight;
        ReqTmpHeight = surfaceWidth;
        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Size size : sizeMap) {
            if ((size.getWidth() == ReqTmpWidth) && (size.getHeight() == ReqTmpHeight)) {
                return size;
            }
        }

        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Size retSize = null;
        for (Size size : sizeMap) {
            curRatio = ((float) size.getWidth()) / size.getHeight();
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    public static boolean isLowDevice = false;

    /**
     * 核心方法，这里是通过从sizeMap中获取和Textureview宽高比例相同的map，然后在获取接近自己想获取到的尺寸
     * 之所以这么做是因为我们要确保预览尺寸不要太大，这样才不会太卡
     *
     * @return
     */
    public static Size getOptimalPreviewSize(Size[] sizes, int w, int h) {
        if (sizes == null) return null;
        Size optimalSize = null;
        List<Size> list = new ArrayList();
        WindowManager windowManager = (WindowManager) XChatApp.mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;  // 屏幕高度（像素）
        for (Size size : sizes) {
            double hwRatio = (double) size.getHeight() / size.getWidth();
            if (hwRatio == 0.5625 || hwRatio == 0.75) {
                list.add(size);
                if (size.getWidth() == screenHeight) {
                    optimalSize = size;
                    break;
                } else if (size.getWidth() > screenHeight) {
                    optimalSize = size;
                } else if (size.getWidth() < screenHeight) {
                    break;
                }
            }
        }
        if (optimalSize == null && list.size() > 1) {
            optimalSize = list.get(1);
        }
        if (optimalSize != null && optimalSize.getHeight() < 1080) {
            isLowDevice = true;
            optimalSize = new Size(1080, 1920);
        }
        return optimalSize;
    }

    public static Size getLowOptimalPreviewSize(Size[] sizes, int w, int h) {
        if (sizes == null) return null;
        Size optimalSize = null;
        List<Size> list = new ArrayList();
        for (Size size : sizes) {
            double hwRatio = (double) size.getHeight() / size.getWidth();
            if (hwRatio == 0.5625) {
                list.add(size);
            }
        }
        if (list.size() > 1) {
            optimalSize = list.get(1);
        }
        return optimalSize;
    }

    public static Size getVideoPreviewSize(Size[] sizeMap, int surfaceWidth, int surfaceHeight) {
        List<Size> sizeList = new ArrayList<>();
        Size retSize = null;
        for (Size size : sizeMap) {
            if (size.getWidth() == 1920 || (size.getWidth() == 1280 && size.getHeight() == 720)) {
                sizeList.add(size);
            }
        }

        if (sizeList.size() > 0) {
            retSize = sizeList.get(0);
        } else {
            retSize = getCloselyPreSize(sizeMap, surfaceWidth, surfaceHeight);
        }
        if (retSize != null && retSize.getHeight() < 1080) {
            isLowDevice = true;
            retSize = new Size(1080, 1920);
        }
        return retSize;
    }

    /**
     * 使用Camera2录制和所拍的照片都会在这里
     */
    public static String getCamera2Path() {
        String picturePath = MainActivity.mContext.getCacheDir() + "/CameraV2/";
        File file = new File(picturePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return picturePath;
    }

    /**
     * 判断传入的地址是否已经有这个文件夹，没有的话需要创建
     */
    public static void createSavePath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        int height = options.outHeight;
        int width = options.outWidth;

        // 计算最大的inSampleSize值，使得图片尺寸不小于目标尺寸
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
