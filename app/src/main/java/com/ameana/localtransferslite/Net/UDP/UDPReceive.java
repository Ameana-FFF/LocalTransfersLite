package com.ameana.localtransferslite.Net.UDP;


import android.util.Log;


import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Utilities.ThreadPool;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceive extends Thread {
    /*
     * 该类负责接收广播数据
     * */
    public UDPReceive() {
    }

    //广播接收对象
    private static DatagramSocket dsIn = null;

    //多线程启动
    @Override
    public void run() {
        //创建广播对象
        for (int i = 0; i < 3 && dsIn == null; i++) {
            try {
                dsIn = new DatagramSocket(Configure.getBroadcastPort());
                break;
            } catch (IOException e) {
                Log.e("广播错误", "广播接收服务创建失败，正在尝试重新创建");
                //线程暂停0.5秒后继续
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Log.e("线程错误", "线程暂停失败");
                }
            }
        }

        //判断服务是否启动
        if (dsIn == null) {
            MainActivity.showToast("设备搜索服务创建失败(端口已被占用)");
            return;
        }

        //启动完成后向局域网搜索一次设备
        ThreadPool.submitSearch();

        //创建接收数据的容器
        byte[] dataByte = new byte[128];
        DatagramPacket dp = new DatagramPacket(dataByte, dataByte.length);

        //循环接收数据
        while (true) {
            //接收数据
            try {
                dsIn.receive(dp);
            } catch (IOException e) {
                Log.e("广播错误", "广播接收失败");
                continue;
            }

            //交给其他线程处理数据
            ThreadPool.THREAD_POOL_EXECUTOR.submit(new UDPMessageHandling(new String(dp.getData(), 0, dp.getLength())));
        }
    }
}