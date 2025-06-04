package com.tencent.yoloNcnn;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.Image;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.URI;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MenuWindowService extends Service {
    private static final String TAG = "YOLO-V5";

    private static WindowManager windowManager;
    private static WindowManager.LayoutParams layoutParams;
    private static View menuView;
    public static View closeMenu2;
    public static LinearLayout menuLinearLayout, menuLinearLayout0, menuLinearLayout1, 识别范围LinearLayout, 推理方式LinearLayout;
    private static RadioButton CPU, GPU, NCNN, TFLITE;
    private static SeekBar 识别速度, 识别范围, 合格率, 去重叠, 图像尺寸;
    private static CheckBox 开启触摸, 保存图片, 识别全屏, 显示基础信息, 显示记牌器, 显示类名, 显示方框, 显示骨骼, 显示轮廓;
    private static Button 退出程序, 重置记牌, 编辑类名;
    private Spinner modelSpinner;
    public static ArrayAdapter<String> modelAdapter;
    private static RecyclerView 类名复选;
    private CheckboxAdapter adapter;
    public static GridLayoutManager gridLayoutManager;
    Image image = null;

    public static EditText 识别类名;

    private Thread screenshotThread = new Thread() {
        @Override
        public void run() {
            long lastScreenUpdateTime = System.currentTimeMillis();
            long identifyUpdateTime = System.currentTimeMillis();
            while (true) {
                try {
                    if (Param.YOLO初始化 && Param.开始识别 && MainActivity.imageReader != null) {
                        long startTime = System.currentTimeMillis();
                        image = MainActivity.imageReader.acquireLatestImage();
                        Param.截图耗时 = (System.currentTimeMillis() - startTime);
                        if (image != null) {
                            startTime = System.currentTimeMillis();
                            if (0 == Param.游戏类型) {
                                MainActivity.bitmap = MediaProjectionHelper.processImage(image, Param.识别全屏 ? -1 : Param.识别范围X, Param.识别全屏 ? -1 : Param.识别范围X);
                            } else if (1 == Param.游戏类型) {
                                MainActivity.bitmap = MediaProjectionHelper.processImage(image, Param.识别全屏 ? -1 : Param.棋牌识别范围X, Param.识别全屏 ? -1 : Param.棋牌识别范围Y);
                            }
                            Param.裁剪耗时 = (System.currentTimeMillis() - startTime);
                            startTime = System.currentTimeMillis();
                            if (null != MainActivity.bitmap) {
                                if (!Param.使用远程服务器) {
                                    if ("ncnn".equals(Param.推理框架)) {
                                        if ("v5".equals(Param.yolo版本)) {
                                            MainActivity.objects = MainActivity.yolov5ncnn.DetectV5(MainActivity.surface, MainActivity.bitmap, Param.使用GPU,
                                                    (float) Param.识别合格率, Param.targetSize, (float) Param.probThreshold,
                                                    (float) Param.nmsThreshold);
                                        }
                                        if ("v8".equals(Param.yolo版本)) {
                                            MainActivity.objects = MainActivity.yolov5ncnn.DetectV8(MainActivity.surface, MainActivity.bitmap, Param.使用GPU,
                                                    (float) Param.识别合格率, Param.targetSize, (float) Param.probThreshold,
                                                    (float) Param.nmsThreshold);
                                        }
                                    } else if ("tflite".equals(Param.推理框架)) {
//                                        MainActivity.objects = MainActivity.yolov5tflite.detect(MainActivity.bitmap);
                                    }
                                } else if (Param.使用远程服务器) {
                                    if (Param.服务器连接状态) {
                                        MainActivity.imageWebSocketClient.sendImage(MainActivity.bitmap);
                                    } else {
                                        MainActivity.imageWebSocketClient.close();
                                        MainActivity.imageWebSocketClient.connectBlocking();
                                    }
                                }
                                Param.推理耗时 = (System.currentTimeMillis() - startTime);
                                if (Param.保存图片 && null != MainActivity.bitmap && MainActivity.objects != null && MainActivity.objects.length > 0) {
                                    for (YoloV5Ncnn.Obj obj : MainActivity.objects) {
                                        if (obj != null && Param.checkedStates.get(Param.yolo类名集合.indexOf(obj.label))) {
                                            FileUtils.saveBitmap(MainActivity.bitmap, Param.保存图片路径, "YOLO_" + System.currentTimeMillis() + ".png");
                                        }
                                    }
                                }
//                                System.out.println("获取图像：" + Param.截图耗时 + " ms 裁剪图像：" + Param.裁剪耗时 + " ms 识别图像：" + Param.推理耗时 + " ms");
                            }
                            if (System.currentTimeMillis() - identifyUpdateTime >= Param.信息刷新速度) {
                                Param.耗时 = System.currentTimeMillis() - startTime;
                                Param.更新平均帧率(1000 / Param.推理耗时);
                                identifyUpdateTime = startTime;
                            }
                        }
                        Thread.sleep(Param.识别速度);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastScreenUpdateTime >= Param.信息刷新速度) {
                        WindowBean.isLandscape(getApplicationContext());
                        if (Param.保存图片)
                            Param.保存图片数量 = FileUtils.获取目录文件数量(Param.保存图片路径);
                        layoutParams.width = WindowBean.screenWidth;
                        layoutParams.height = WindowBean.screenHeight;
                        lastScreenUpdateTime = currentTime;
                        MainActivity.yolov5ncnn.setScreen(WindowBean.screenWidth, WindowBean.screenHeight);
                        updateMenuLinearLayout();
                    }
                    if (image != null) image.close();
                    MainActivity.bitmap = null;
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        menuView = LayoutInflater.from(this).inflate(R.layout.menu, null);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        // 计算屏幕宽度
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        // 设置初始位置为左侧屏幕外
        layoutParams.x = -menuView.getWidth();
        layoutParams.y = 0;
        layoutParams.width = WindowBean.screenWidth;
        layoutParams.height = WindowBean.screenHeight;
        windowManager.addView(menuView, layoutParams);
        // 滑入动画
        ObjectAnimator animator = ObjectAnimator.ofFloat(menuView, "translationX", -menuView.getWidth(), 0);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        initView();
        viewListener();
        WindowBean.menuWindwosStatus = true;
        ViewGroup.LayoutParams layoutParams = menuLinearLayout.getLayoutParams();
        menuLinearLayout.setLayoutParams(layoutParams);
        if (Param.使用远程服务器) {
            try {
                URI serverUri = new URI(String.valueOf(MainActivity.服务器地址.getText()));
                MainActivity.imageWebSocketClient = new ImageWebSocketClient(serverUri);
                MainActivity.imageWebSocketClient.connectBlocking();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        CardCounter.initialize();
        screenshotThread.start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //初始化控件
    public void initView() {
        modelAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item, MainActivity.modelData);
        modelAdapter.setDropDownViewResource(R.layout.dropdown_stytle);
        menuLinearLayout = menuView.findViewById(R.id.menuLinearLayout);
        menuLinearLayout0 = menuView.findViewById(R.id.menuLinearLayout0);
        menuLinearLayout1 = menuView.findViewById(R.id.menuLinearLayout1);
        识别范围LinearLayout = menuView.findViewById(R.id.识别范围LinearLayout);
        推理方式LinearLayout = menuView.findViewById(R.id.推理方式LinearLayout);
        closeMenu2 = menuView.findViewById(R.id.closeMenu2);
        CPU = menuView.findViewById(R.id.CPU);
        GPU = menuView.findViewById(R.id.GPU);
        NCNN = menuView.findViewById(R.id.NCNN);
        TFLITE = menuView.findViewById(R.id.TFLITE);
        识别速度 = menuView.findViewById(R.id.识别速度);
        识别范围 = menuView.findViewById(R.id.识别范围);
        合格率 = menuView.findViewById(R.id.合格率);
        去重叠 = menuView.findViewById(R.id.nmsThreshold);
        图像尺寸 = menuView.findViewById(R.id.图像尺寸);
        退出程序 = menuView.findViewById(R.id.退出程序);
        重置记牌 = menuView.findViewById(R.id.重置记牌);
        开启触摸 = menuView.findViewById(R.id.开启触摸);
        保存图片 = menuView.findViewById(R.id.保存图片);
        编辑类名 = menuView.findViewById(R.id.编辑类名);
        识别全屏 = menuView.findViewById(R.id.识别全屏);
        显示基础信息 = menuView.findViewById(R.id.显示基础信息);
        显示记牌器 = menuView.findViewById(R.id.显示记牌器);
        显示类名 = menuView.findViewById(R.id.显示类名);
        显示方框 = menuView.findViewById(R.id.显示方框);
        显示骨骼 = menuView.findViewById(R.id.显示骨骼);
        显示轮廓 = menuView.findViewById(R.id.显示轮廓);
        识别类名 = menuView.findViewById(R.id.识别类名);
        识别类名.setText(Param.yolo类名);
        modelSpinner = menuView.findViewById(R.id.model);
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setOnItemSelectedListener(new SpinnerSelectedListener());
        Param.yolo类名集合 = Arrays.asList(Param.yolo类名.split(","));
        类名复选 = menuView.findViewById(R.id.类名复选);
        gridLayoutManager = new GridLayoutManager(this, 3);
        类名复选.setLayoutManager(gridLayoutManager);
        adapter = new CheckboxAdapter(Param.yolo类名集合);
        类名复选.setAdapter(adapter);

        if (Param.使用GPU) {
            CPU.setChecked(false);
            GPU.setChecked(true);
        } else {
            CPU.setChecked(true);
            GPU.setChecked(false);
        }
        if ("ncnn".equals(Param.推理框架)) {
            TFLITE.setChecked(false);
            NCNN.setChecked(true);
        } else {
            TFLITE.setChecked(true);
            NCNN.setChecked(false);
        }
        开启触摸.setChecked(Param.开启触摸);
        保存图片.setChecked(Param.保存图片);
        识别全屏.setChecked(Param.识别全屏);
        显示记牌器.setChecked(Param.显示记牌器);
        显示类名.setChecked(Param.显示类名);
        显示方框.setChecked(Param.显示方框);
        显示骨骼.setChecked(Param.显示骨骼);
        显示轮廓.setChecked(Param.显示轮廓);
        显示基础信息.setChecked(Param.显示基础信息);
        识别速度.setProgress(Param.识别速度);
        识别范围.setProgress(Param.识别范围X);

        合格率.setMin((int) (Param.probThreshold * 100));
        合格率.setProgress((int) (Param.识别合格率 * 100));
        去重叠.setProgress((int) (Param.nmsThreshold * 100));
        图像尺寸.setProgress(Param.targetSize);

        if (!Param.是否有ROOT)
            开启触摸.setVisibility(View.GONE);
        if (Param.识别全屏 || Param.游戏类型 == 1)
            识别范围LinearLayout.setVisibility(View.GONE);
        if (Param.使用远程服务器)
            推理方式LinearLayout.setVisibility(View.GONE);
        if (Param.游戏类型 == 0) {
            显示记牌器.setVisibility(View.GONE);
            重置记牌.setText("测试按钮");
        } else if (Param.游戏类型 == 1) {
            显示记牌器.setVisibility(View.VISIBLE);
            重置记牌.setText("重置记牌");
        }
    }

    public void viewListener() {
        closeMenu2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide();
            }
        });

        保存图片.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.保存图片 = isChecked;
            }
        });
        识别全屏.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.识别全屏 = isChecked;
                识别范围LinearLayout.setVisibility(Param.识别全屏 ? View.GONE : View.VISIBLE);
            }
        });
        显示基础信息.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.显示基础信息 = isChecked;
            }
        });
        显示记牌器.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.显示记牌器 = isChecked;
            }
        });
        显示类名.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.显示类名 = isChecked;
            }
        });
        显示方框.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.显示方框 = isChecked;
            }
        });
        显示骨骼.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.显示骨骼 = isChecked;
            }
        });
        显示轮廓.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.显示轮廓 = isChecked;
            }
        });
        CPU.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.使用GPU = false;
            }
        });
        GPU.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.使用GPU = true;
            }
        });
        NCNN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.推理框架 = "ncnn";
                刷新模型菜单();
            }
        });
        TFLITE.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Param.推理框架 = "tflite";
                刷新模型菜单();
            }
        });
        退出程序.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.exit(0);
            }
        });
        重置记牌.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CardCounter.initialize();
            }
        });
        编辑类名.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if ("编辑类名".equals(String.valueOf(编辑类名.getText()))) {
                    Param.YOLO初始化 = false;
                    编辑类名.setText("保存");
                    编辑类名.setBackgroundResource(R.drawable.btn3);
                    识别类名.setVisibility(View.VISIBLE);
                    类名复选.setVisibility(View.GONE);
                } else {
                    编辑类名.setText("编辑类名");
                    编辑类名.setBackgroundResource(R.drawable.btn4);
                    识别类名.setVisibility(View.GONE);
                    类名复选.setVisibility(View.VISIBLE);
//                    Param.yolo类名 = String.valueOf(识别类名.getText());
                    Param.yolo类名集合 = Arrays.asList(String.valueOf(识别类名.getText()).split(","));
                    adapter.setData(Param.yolo类名集合);
                    类名复选.setAdapter(adapter);
                    Param.YOLO初始化 = MainActivity.yolov5ncnn.setModel(Param.模型参数地址, Param.模型地址, String.valueOf(识别类名.getText()), Param.yolo版本);
                    FileUtils.saveFile(Param.模型类名地址, String.valueOf(识别类名.getText()));
                }
            }
        });
        开启触摸.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Param.开启触摸 = isChecked;
                if (Param.开启触摸 && Param.是否有ROOT) {
                    String shFileName = "touch_socket";
                    FileUtils.复制文件到本地目录(MainActivity.context, "/sdcard/XXGUI/", "sh/", shFileName);
                    String[] cmds = new String[]{"mv /sdcard/XXGUI/" + shFileName + " /data/local/tmp/" + shFileName, "chmod 777 /data/local/tmp/" + shFileName};
                    CommandExecution.execCommand(cmds, true);
                    CommandExecution.execCmd("./data/local/tmp/" + shFileName);
                    new Thread(new Thread() {
                        @Override
                        public void run() {
                            String cmd = "'chmod -R 777 /data/local/tmp/touch_socket && "
                                    + "ps -ef | grep touch_socket | grep -v grep | awk \"{print $2}\" | xargs kill -9 && "
                                    + "/data/local/tmp/touch_socket'";
                            CommandExecution.execCommand(cmd, true);
                        }
                    }).start();
                }
                MainActivity.yolov5ncnn.setAim(Param.开启触摸);
            }
        });
        识别速度.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Param.识别速度 = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        识别范围.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Param.识别范围X = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        合格率.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Param.识别合格率 = (float) seekBar.getProgress() / 100;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        去重叠.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Param.nmsThreshold = (float) seekBar.getProgress() / 100;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        图像尺寸.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Param.targetSize = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void 刷新模型菜单() {
        if ("ncnn".equals(Param.推理框架)) {
            MainActivity.modelData = FileUtils.加载模型列表(Param.ncnn文件地址 + Param.yolo版本);
        } else if ("tflite".equals(Param.推理框架)) {
            MainActivity.modelData = FileUtils.加载模型列表(Param.tflite文件地址);
        }
        modelAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item, MainActivity.modelData);
        modelSpinner.setAdapter(modelAdapter);
    }

    public static void show() {
        if (menuView != null) {
            menuView.setVisibility(View.VISIBLE);
            showFloatingView();
            FloatingWindowService.hide();
        }
    }

    public static void hide() {
        if (menuView != null) {
            menuView.setVisibility(View.GONE);
            FloatingWindowService.show();
        }
    }

    private static void showFloatingView() {
        if (windowManager != null && menuView != null && layoutParams != null) {
            ObjectAnimator animator = null;
            if (Param.横屏) {
                gridLayoutManager.setSpanCount(2);
                layoutParams.x = 0;
                windowManager.updateViewLayout(menuView, layoutParams);
                animator = ObjectAnimator.ofFloat(menuView, "translationX", 0 == WindowBean.windowDirection ? -menuView.getWidth() : menuView.getWidth(), 0);
            } else {
                gridLayoutManager.setSpanCount(3);
                layoutParams.y = 0;
                windowManager.updateViewLayout(menuView, layoutParams);
                animator = ObjectAnimator.ofFloat(menuView, "translationY", 0 == WindowBean.windowDirection ? -menuView.getHeight() : menuView.getHeight(), 0);
            }
            类名复选.setLayoutManager(gridLayoutManager);
            animator.setDuration(200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }
    }

    class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            Param.YOLO初始化 = false;
            if (Param.FPS类型.contains(MainActivity.modelData.get(arg2))) {
                Param.游戏类型 = 0;
                显示记牌器.setVisibility(View.GONE);
            } else if (Param.棋牌类型.contains(MainActivity.modelData.get(arg2))) {
                Param.游戏类型 = 1;
                显示记牌器.setVisibility(View.VISIBLE);
            }else if (Param.台球类型.contains(MainActivity.modelData.get(arg2))) {
                Param.游戏类型 = 2;
                显示记牌器.setVisibility(View.GONE);
            }
            if ("ncnn".equals(Param.推理框架)) {
                Param.模型地址 = Param.ncnn文件地址 + Param.yolo版本 + "/" + MainActivity.modelData.get(arg2) + ".bin";
                Param.模型参数地址 = Param.ncnn文件地址 + Param.yolo版本 + "/" + MainActivity.modelData.get(arg2) + ".param";
                Param.模型类名地址 = Param.ncnn文件地址 + Param.yolo版本 + "/" + MainActivity.modelData.get(arg2) + ".class";
                String 类名 = FileUtils.readFileContent(Param.模型类名地址);
                if (null == 类名 || "".equals(类名) || 0 >= 类名.length())
                    类名 = Param.yolo类名;
                Param.yolo类名集合 = Arrays.asList(类名.split(","));
                adapter.setData(Param.yolo类名集合);
                类名复选.setAdapter(adapter);
                if (!Param.使用远程服务器) {
                    Param.YOLO初始化 = MainActivity.yolov5ncnn.setModel(Param.模型参数地址, Param.模型地址, 类名, Param.yolo版本);
                } else {
                    Param.YOLO初始化 = true;
                }
                Param.显示骨骼 = false;
                Param.显示轮廓 = false;
                显示骨骼.setVisibility(View.GONE);
                显示轮廓.setVisibility(View.GONE);
                if (-1 < MainActivity.modelData.get(arg2).indexOf("pose")) {
                    显示骨骼.setVisibility(View.VISIBLE);
                    Param.显示骨骼 = true;
                }
                if (-1 < MainActivity.modelData.get(arg2).indexOf("seg")) {
                    显示轮廓.setVisibility(View.VISIBLE);
                    Param.显示轮廓 = true;
                }
            } else if ("tflite".equals(Param.推理框架)) {
//                Param.模型地址 = Param.tflite文件地址 + MainActivity.modelData.get(arg2) + ".tflite";
//                MainActivity.yolov5tflite.addGPUDelegate();
//                Param.YOLO初始化 = MainActivity.yolov5tflite.initialModel(Param.模型地址);
            }
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    public static void updateMenuLinearLayout() {
        final Activity activity = MainActivity.activity;
        if (activity != null && null != menuLinearLayout0 && null != menuLinearLayout1 && null != closeMenu2) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LinearLayout.LayoutParams menuLinearLayout1LayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1200
                    );
                    if (Param.横屏)
                        menuLinearLayout1LayoutParams = new LinearLayout.LayoutParams(
                                800, LinearLayout.LayoutParams.MATCH_PARENT
                        );
                    menuLinearLayout1.setLayoutParams(menuLinearLayout1LayoutParams);
                    menuLinearLayout1.setPadding(0, 0, 0, Param.横屏 ? 0 : 50);
                    menuLinearLayout0.setOrientation(Param.横屏 ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
                    closeMenu2.setBackgroundResource(Param.横屏 ? R.drawable.menu_bg_shadow_rigth : R.drawable.menu_bg_shadow_bottom);
                }
            });
        }
    }
}

