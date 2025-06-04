package com.tencent.yoloNcnn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {
    public static List<String> listFiles(String path) {
        if (path == null) {
            return new ArrayList();
        }
        File file = new File(path); //以某路径实例化一个File对象
        if (!file.exists()) { //如果不存在
            boolean dr = file.mkdirs(); //创建目录
        }

        ArrayList<String> list = new ArrayList<>();
        for (File f : file.listFiles()) {
            //如果是文件夹
            if (f.isDirectory()) {
                list.addAll(listFiles(f.getPath()));
            } else {
                list.add(f.getName());
                //下面是带有路径的写法
                //list.add(file.getPath());
            }
        }
        return list;
    }

    public static List<String> 加载模型列表(String path) {
        return 加载模型列表(path, true, false);
    }

    public static List<String> 加载模型列表(String path, boolean 日期倒序, boolean 大小倒序) {
        if (path == null) {
            return new ArrayList<>();
        }

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        ArrayList<File> fileList = new ArrayList<>();
        for (File f : file.listFiles()) {
            if (!f.isDirectory()) {
                fileList.add(f);
            }
        }

        // 按照日期和大小排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                // 先按日期排序
                int dateCompare = Long.compare(f1.lastModified(), f2.lastModified());
                if (日期倒序) {
                    dateCompare = -dateCompare;
                }

                if (dateCompare != 0) {
                    return dateCompare;
                }

                // 再按大小排序
                int sizeCompare = Long.compare(f1.length(), f2.length());
                if (大小倒序) {
                    sizeCompare = -sizeCompare;
                }

                return sizeCompare;
            }
        });

        ArrayList<String> list = new ArrayList<>();
        for (File f : fileList) {
            String fileName = f.getName();
            String fileNameArr[] = fileName.split("\\.");
            fileName = "";
            for (int i = 0; i < fileNameArr.length - 1; i++) {
                fileName += fileNameArr[i] + (i == fileNameArr.length - 2 ? "" : ".");
            }
            if (!list.contains(fileName)) {
                list.add(fileName);
            }
        }

        return list.stream().distinct().collect(Collectors.toList());
    }

    public static int 获取目录文件数量(String path) {
        if (path == null) return 0;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.listFiles().length;
    }

    /**
     * 加载本地图片
     *
     * @param url
     * @return
     */
    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);  ///把流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 读取文件转ByteBuffer
     * @param url
     * @return
     */
    public static ByteBuffer getLocalByteBuffer(String url) {
        File file = new File(url);
        if (!file.exists()) {
            System.err.println("File not found: " + url);
            return null;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileChannel fileChannel = fileInputStream.getChannel()) {
            long fileSize = fileChannel.size();
            // 使用直接缓冲区 (Direct ByteBuffer)
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) fileSize);
            byteBuffer.order(ByteOrder.nativeOrder());  // 设置字节序为本地字节序
            fileChannel.read(byteBuffer);
            byteBuffer.flip();  // 准备缓冲区用于读取
            return byteBuffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //保存文件到本地
    public static void saveBitmap(Bitmap bitmap, String filePath, String fileName) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(filePath + "/" + fileName);
        file.createNewFile();
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void 复制文件到本地目录(Context context, String path, String assetsPath, String filename) {
        InputStream in = null;
        FileOutputStream out = null;
        File mkdir = new File(path);
        File file = new File(path + filename);
        try {
            if (!mkdir.exists()) {
                mkdir.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            in = context.getAssets().open(assetsPath + filename);
            out = new FileOutputStream(file);
            int length = -1;
            byte[] buf = new byte[1024];
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取文本内容
     *
     * @param filePath
     * @return
     */
    public static String readFileContent(String filePath) {
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            File file = new File(filePath);
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            System.out.println("获取内容异常："+e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return sbf.toString();
        }
    }

    /**
     * 写入文件
     *
     * @return
     */
    public static boolean saveFile(String filePath, String fileValue) {
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(fileValue.getBytes());
            fos.flush();
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("目录错误，找不到文件:" + e);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return false;
    }
}
