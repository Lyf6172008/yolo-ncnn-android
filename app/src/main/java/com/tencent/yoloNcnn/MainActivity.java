
package com.tencent.yoloNcnn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final String TAG = "YOLO-V5";
    private static final int FILE_SELECT_CODE = 100;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static final int REQUEST_CODE_MEDIA_PROJECTION = 1001;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1002;
    public static MediaProjectionManager mediaProjectionManager;

    //    public static SimpleSocketClient homeClient;
//    public static ImageSocketClient imageSocketClient;
    public static ImageWebSocketClient imageWebSocketClient;
    //    public static WebSocketClient nettyWebSocketClient;
    public static Context context;
    public static Activity activity;
    public static Bitmap bitmap = null;
    public static YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();
    //    public static Yolov5TFLiteDetector yolov5tflite = new Yolov5TFLiteDetector();
    public static MediaProjection mediaProjection;
    public static VirtualDisplay virtualDisplay;
    public static ImageReader imageReader;
    public static Surface surface;
    public static int dpi = 400;
    public static YoloV5Ncnn.Obj[] objects;

    public static Button 开始识别, 退出程序, 权限授权, 添加模型, 刷新模型, 模型切换;
    public static TextView APP权限状态, 使用说明;
    public static RadioButton 关闭远程, 开启远程, yoloV5, yoloV8,关闭知识库,启用知识库;
    public static EditText 服务器地址, 识别类名,大模型密钥,大模型提示词,知识库ID;
    private static RecyclerView 模型列表;
    private static ModelListAdapter adapter;
    public static GridLayoutManager gridLayoutManager;
    public static List<String> modelData;

    public static LinearLayout 识别类名LinearLayout, yolo页面, llm页面;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        is_root();
        context = this;
        activity = this;
        Param.JNI初始化 = yolov5ncnn.Init();
        if (!Param.JNI初始化) {
            Log.i(TAG, "YOLO-V5:JNI初始未完成");
            Toast.makeText(context, "JNI初始失败", Toast.LENGTH_LONG).show();
            System.exit(-1);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        verifyStoragePermissions(this);
        checkPermission(this);
        if (areAllPermissionsGranted()) {
            requestScreenCapture();
            WindowBean.getScreenResolution(context);
            FileUtils.saveFile(WindowBean.screenFilePath, WindowBean.screenWidth + "," + WindowBean.screenHeight);
        }
        AiUtil.刷新配置();
        初始化控件();
        // 初始化MediaProjectionManager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    public static void 刷新模型列表() {
        if ("ncnn".equals(Param.推理框架)) {
            MainActivity.modelData = FileUtils.加载模型列表(Param.ncnn文件地址 + Param.yolo版本);
        } else if ("tflite".equals(Param.推理框架)) {
            MainActivity.modelData = FileUtils.加载模型列表(Param.tflite文件地址);
        }
        adapter = new ModelListAdapter(Param.yolo类名集合);
        模型列表.setAdapter(adapter);
    }

    public void 初始化控件() {
        识别类名LinearLayout = findViewById(R.id.识别类名LinearLayout);
        yolo页面 = findViewById(R.id.yolo页面);
        llm页面 = findViewById(R.id.llm页面);
        开始识别 = findViewById(R.id.开始识别);
        关闭远程 = findViewById(R.id.关闭远程);
        开启远程 = findViewById(R.id.开启远程);
        yoloV5 = findViewById(R.id.yoloV5);
        yoloV8 = findViewById(R.id.yoloV8);
        关闭知识库 = findViewById(R.id.关闭知识库);
        启用知识库 = findViewById(R.id.启用知识库);
        服务器地址 = findViewById(R.id.服务器地址);
        识别类名 = findViewById(R.id.识别类名);
        大模型密钥 = findViewById(R.id.大模型密钥);
        大模型提示词 = findViewById(R.id.大模型提示词);
        知识库ID = findViewById(R.id.知识库ID);
        权限授权 = findViewById(R.id.权限授权);
        退出程序 = findViewById(R.id.退出程序);
        添加模型 = findViewById(R.id.添加模型);
        刷新模型 = findViewById(R.id.刷新模型);
        模型切换 = findViewById(R.id.模型切换);
        APP权限状态 = findViewById(R.id.APP权限状态);
        模型列表 = findViewById(R.id.模型列表);
        gridLayoutManager = new GridLayoutManager(this, 1);
        模型列表.setLayoutManager(gridLayoutManager);
        刷新模型列表();
        set权限状态();
        使用说明 = findViewById(R.id.使用说明);
        if (Param.使用远程服务器) {
            服务器地址.setText(Param.服务器地址);
            关闭远程.setChecked(false);
            开启远程.setChecked(true);
        } else {
            关闭远程.setChecked(true);
            开启远程.setChecked(false);
        }
        if ("v5".equals(Param.yolo版本)) {
            yoloV8.setChecked(false);
            yoloV5.setChecked(true);
        }
        if ("v8".equals(Param.yolo版本)) {
            yoloV8.setChecked(false);
            yoloV5.setChecked(true);
        }
        if(AiUtil.知识库状态){
            启用知识库.setChecked(true);
            关闭知识库.setChecked(false);
        }else{
            启用知识库.setChecked(false);
            关闭知识库.setChecked(true);
        }
        识别类名.setText(Param.yolo类名);
        大模型密钥.setText(AiUtil.APP_KEY);
        大模型提示词.setText(AiUtil.提示词);
        知识库ID.setText(AiUtil.知识库ID);
        使用说明.setText(Param.使用说明);
        if (areAllPermissionsGranted()) {
            权限授权.setVisibility(View.GONE);
        }
        关闭远程.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.使用远程服务器 = false;
            }
        });
        开启远程.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.使用远程服务器 = true;
            }
        });
        yoloV5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.yolo版本 = "v5";
                刷新模型列表();
            }
        });
        yoloV8.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.yolo版本 = "v8";
                刷新模型列表();
            }
        });
        启用知识库.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AiUtil.知识库状态=true;
            }
        });
        关闭知识库.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AiUtil.知识库状态=false;
            }
        });
        权限授权.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                verifyStoragePermissions(activity);
                checkPermission(activity);
                is_root();
            }
        });
        添加模型.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openFileChooser();
            }
        });
        刷新模型.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                刷新模型列表();
            }
        });
        模型切换.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String title = (String) 模型切换.getText();
                if ("切换LLM页面".equals(title)) {
                    模型切换.setText("切换Yolo页面");
                    yolo页面.setVisibility(View.GONE);
                    llm页面.setVisibility(View.VISIBLE);
                    AiUtil.开启大模型 = true;
                } else if ("切换Yolo页面".equals(title)) {
                    模型切换.setText("切换LLM页面");
                    llm页面.setVisibility(View.GONE);
                    yolo页面.setVisibility(View.VISIBLE);
                    AiUtil.开启大模型 = false;
                }
            }
        });
        识别类名.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
//                    Param.yolo类名 = String.valueOf(识别类名.getText());
                    Param.yolo类名集合 = Arrays.asList(Param.yolo类名.split(","));
                }
            }
        });
        大模型密钥.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    AiUtil.APP_KEY = String.valueOf(大模型密钥.getText());
                    AiUtil.保存配置();
                }
            }
        });
        大模型提示词.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    AiUtil.提示词 = String.valueOf(大模型提示词.getText());
                    AiUtil.保存配置();
                }
            }
        });
        知识库ID.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    AiUtil.知识库ID = String.valueOf(知识库ID.getText());
                    AiUtil.保存配置();
                }
            }
        });
        退出程序.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.exit(0);
            }
        });
        开始识别.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Param.自动打开游戏) {
                    openGame(Param.目标包名);
                }
                if (!areAllPermissionsGranted()) {
                    Toast.makeText(context, "权限不足，授权后重启APP再试", Toast.LENGTH_LONG).show();
                    return;
                }
                Param.开始识别 = true;
                开始识别.setVisibility(View.GONE);
                createVirtualDisplay();
                startService(new Intent(context, FloatingWindowService.class));
//                CanvasWindow.DrawAdd(context);
            }
        });
//        if (Param.游戏类型 == 0)
//            识别类名LinearLayout.setVisibility(View.GONE);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");  // 选择所有文件
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择 YOLO 文件"), FILE_SELECT_CODE);
    }

    private String getFileName(Uri uri) {
        String path = uri.getPath();
        return path != null ? path.substring(path.lastIndexOf("/") + 1) : "unknown";
    }

    private void unzipFile(Uri uri) {
        // 1️⃣ 创建 ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("准备解压...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = true;
            String dirPath = Param.ncnn文件地址 + Param.yolo版本;
            if ("tflite".equals(Param.推理框架)) {
                MainActivity.modelData = FileUtils.加载模型列表(Param.tflite文件地址);
            }
            File outputDir = new File(dirPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

                // 2️⃣ 计算压缩包内的文件总数
                List<ZipEntry> zipEntries = new ArrayList<>();
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    zipEntries.add(zipEntry);
                    zipInputStream.closeEntry();
                }
                int totalFiles = zipEntries.size();

                handler.post(() -> {
                    progressDialog.setMax(totalFiles); // 设置总进度的最大值
                    progressDialog.setProgress(0);
                });

                // 重新打开流
                inputStream.close();
                zipInputStream.close();

                // 重新遍历解压
                InputStream newInputStream = getContentResolver().openInputStream(uri);
                ZipInputStream newZipInputStream = new ZipInputStream(newInputStream);
                int extractedFiles = 0;

                while ((zipEntry = newZipInputStream.getNextEntry()) != null) {
                    File outputFile = new File(outputDir, zipEntry.getName());
                    long fileSize = zipEntry.getSize(); // 获取当前文件大小
                    long extractedSize = 0;

                    ZipEntry finalZipEntry = zipEntry;
                    handler.post(() -> progressDialog.setMessage("解压 " + finalZipEntry.getName()));

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = newZipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                            extractedSize += length;

                            // 3️⃣ 更新单个文件进度
                            int fileProgress = (int) ((extractedSize * 100) / fileSize);
                            handler.post(() -> progressDialog.setSecondaryProgress(fileProgress));
                        }
                    }
                    newZipInputStream.closeEntry();

                    // 4️⃣ 更新总进度
                    extractedFiles++;
                    int finalExtractedFiles = extractedFiles;
                    handler.post(() -> progressDialog.setProgress(finalExtractedFiles));
                }

                newZipInputStream.close();
                newInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }

            boolean finalSuccess = success;
            handler.post(() -> {
                progressDialog.dismiss();
                if (finalSuccess) {
                    Toast.makeText(this, "添加成功", Toast.LENGTH_LONG).show();
                    刷新模型列表();
                } else {
                    Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    public void set权限状态() {
        String 权限状态 = "文件访问权限：" + (hasStoragePermission() ? "正常" : "没有授权");
        权限状态 += "\n悬浮窗口权限：" + (hasOverlayPermission() ? "正常" : "没有授权");
        if (Param.是否有ROOT)
            权限状态 += "\nROOT权限：" + (Param.是否有ROOT ? "正常" : "没有授权");
        if (null != APP权限状态)
            APP权限状态.setText(权限状态);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK) {
            Intent serviceIntent = new Intent(context, CanvasWindow.class);
            serviceIntent.putExtra("data", data);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    String fileName = getFileName(uri);
                    if (fileName.endsWith(".zip")) {
                        unzipFile(uri);
                    } else {
                        Toast.makeText(this, "请选择 zip 文件", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }


    /**
     * 是否存在su命令，并且有执行权限
     *
     * @return 存在su命令，并且有执行权限返回true
     */
    public boolean is_root() {
        File file = null;
        String[] paths = {"/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/", "/su/bin/"};
        try {
            for (String path : paths) {
                file = new File(path + "su");
                if (file.exists() && file.canExecute()) {
                    Param.是否有ROOT = true;
                    return true;
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        } finally {
            set权限状态();
        }
        return false;
    }

    public static void closeSetenforce() {
        try {
            // 获取Runtime实例
            Runtime runtime = Runtime.getRuntime();
            // 执行setenforce 0命令
            Process process = runtime.exec("su -c setenforce 0");
            // 等待命令执行完成
            process.waitFor();
            // 获取命令的输出（如果有）
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // 获取错误流（如果有错误信息输出）
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
            // 检查命令执行的结果
            if (process.exitValue() == 0) {
                System.out.println("SELinux set to permissive successfully.");
            } else {
                System.out.println("Failed to set SELinux to permissive.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean isServiceON(Context context, String className) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo>
                runningServices = activityManager.getRunningServices(100);
        if (runningServices.size() < 0) {
            return false;
        }
        for (int i = 0; i < runningServices.size(); i++) {
            ComponentName service = runningServices.get(i).service;
            if (service.getClassName().contains(className)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("WrongConstant")
    public static void createVirtualDisplay() {
        if (null != mediaProjection) {
            imageReader = ImageReader.newInstance(
                    WindowBean.screenWidth, WindowBean.screenHeight,
                    PixelFormat.RGBA_8888, 2);
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    WindowBean.screenWidth, WindowBean.screenHeight,
                    dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null, null);
        }
    }

    // 打开应用
    public void openGame(String packName) {
        PackageManager packageManager = getPackageManager();
        if (checkPackInfo(packName)) {
            Intent intent = packageManager.getLaunchIntentForPackage(packName);
            startActivity(intent);
        } else {
            Toast.makeText(this, "打开游戏失败!", Toast.LENGTH_LONG).show();
        }
    }

    // 检查包是否存在
    private boolean checkPackInfo(String packname) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(packname, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "找不到应用", Toast.LENGTH_LONG).show();
        }
        return packageInfo != null;
    }

    // 检查是否所有权限都已授予
    private boolean areAllPermissionsGranted() {
        return hasStoragePermission() && hasOverlayPermission();
    }

    //动态获取读写权限
    public void verifyStoragePermissions(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 100);
                }
            }
            int permission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            set权限状态();
        }
    }

    private void requestScreenCapture() {
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    //检测悬浮窗权限
    public boolean checkPermission(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(activity)) {
                Toast.makeText(activity, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
                activity.startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + activity.getPackageName())), 0);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            set权限状态();
        }
        return true;
    }

    private boolean hasStoragePermission() {
        // 检查文件读写权限是否已授予
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasOverlayPermission() {
        // 检查悬浮窗权限是否已授予
        return Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
        }
    }

}
