package com.ameana.localtransferslite.Configure;

import android.os.Environment;

import java.io.File;

public abstract class Configure {
    /*
     * 该类是配置文件读取后得到的一些数据
     * 还包括一些重要常量
     * */
    private Configure() {
    }

    /*
     * 该区域的常量记录设备名称
     * */
    public static final String DEVICES_NAME = android.os.Build.MANUFACTURER + "_" + android.os.Build.MODEL;

    /*
     * 该区域的变量是告诉广播功能的广播端口
     * */
    //广播端口
    private static int broadcastPort;

    //获取广播端口
    public static int getBroadcastPort() {
        return broadcastPort;
    }

    /*
     * 该区域的变量是告诉TCP连接端口号
     * */
    //TCP端口
    private static int TCPPort;

    //获取TCP端口
    public static int getTCPPort() {
        return TCPPort;
    }

    //设置TCP端口
    public static void setTCPPort(int TCPPort) {
        Configure.TCPPort = TCPPort;
    }

    /*
     * 用户储存根目录路径
     * */
    public static final File USER_ROOT_DIR = Environment.getExternalStorageDirectory();

    /*
     * 该区域的变量是文件保存位置
     * */
    //保存路径
    public static File savePath;
    //缓存路径
    public static File cachePath;

    /*
     * 该区域的变量记录安卓软件内部文件保存路径
     * */
    //配置文件保存路径
    public static File fileSaveDir;
    //缓存文件保存路径
    public static File cacheSaveDir;

    /*
     * 该区域的变量记录UPD发送和UI刷新的频率
     * */
    //UDP广播频率
    private static int UDPSendFrequency;

    //获取UDP广播频率
    public static int getUDPSendFrequency() {
        return UDPSendFrequency;
    }

    //UI刷新频率
    private static int UIFlushedFrequency;

    //获取UI刷新频率
    public static int getUIFlushedFrequency() {
        return UIFlushedFrequency;
    }

    /*
     * 记录网络传输缓存区大小
     * */
    //网络传输缓存大小
    private static int bufferSize;

    //获取网络传输缓存大小
    public static int getBufferSize() {
        return bufferSize;
    }

    /*
     * 该方法会读取配置文件初始化所有变量(比较懒，没写配置文件，采用直接赋值的方法)
     * */
    public static boolean initializeVariable() {
        broadcastPort = 27028;
        TCPPort = 14138;
        UDPSendFrequency = 1000;
        UIFlushedFrequency = 3000;
        bufferSize = 16384;
        savePath = new File(USER_ROOT_DIR, "/Download/LocalTransfersLite");
        cachePath = new File(savePath, ".LocalTransfersLiteTemp");

        return true;
    }
}