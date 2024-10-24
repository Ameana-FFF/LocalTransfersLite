package com.ameana.localtransferslite.Net.UDP;

import android.util.Log;

import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Net.IPLogs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UDPSend extends Thread {
    /*
     * 该多线程负责发送UDP广播，由线程池管理
     * */
    public UDPSend() {
    }

    //广播发送对象
    private static DatagramSocket dsOut = null;

    //启动多线程
    @Override
    public void run() {
        //初始化广播对象
        while (true) {
            try {
                dsOut = new DatagramSocket();
                break;
            } catch (IOException e) {
                Log.e("广播错误", "广播发送服务创建失败，正在尝试重新创建");
                //线程暂停0.5秒后继续
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Log.e("线程错误", "线程暂停失败");
                }
            }
        }

        //清空设备列表
        MainActivity.clearDevice();
        IPLogs.clear_CONNECTED_IP();

        //第一次发送
        UDPOneSend(UDPSend.SEARCH_MODE);

        //再次循环发送两次
        for (int i = 0; i < 2; i++) {
            //暂停1秒
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Log.e("线程错误", "线程暂停失败");
            }

            //发送
            UDPOneSend(UDPSend.SEARCH_MODE);
        }
    }

    /*
     * 发送一次广播
     * 可以设置发送模式
     * 搜索模式：对方收到信息后一定会回复
     * 回复模式：对方收到信息后可能会回复
     * */
    //搜索模式
    public static final String SEARCH_MODE = "T";
    //回复模式
    public static final String REPLY_MODE = "F";

    public static void UDPOneSend(String MODE) {
        //判断广播服务是否创建
        if (dsOut == null)
            return;

        //发送本机IP和端口号
        for (String s : IPLogs.get_LOCAL_IP()) {
            //获取数据
            byte[] data = ("&" + s + "&" + Configure.getTCPPort() + "&" + Configure.DEVICES_NAME + "&" + MODE + "&").getBytes();

            //包装数据
            DatagramPacket dp = null;
            String[] IPSegment = s.split("\\.");
            try {
                dp = new DatagramPacket(data, data.length, InetAddress.getByName(IPSegment[0] + "." + IPSegment[1] + "." + IPSegment[2] + ".255"), Configure.getBroadcastPort());
            } catch (UnknownHostException e) {
                Log.e("广播错误", "广播数据包装失败");
                continue;
            }

            //发送数据
            try {
                dsOut.send(dp);
            } catch (IOException e) {
                Log.e("广播错误", "广播发送失败");
            }
        }
    }
}