package com.ameana.localtransferslite.Net.TCP;

import android.util.Log;

import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Utilities.NetIO;
import com.ameana.localtransferslite.Utilities.ThreadPool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class TCPServer extends Thread {
    /*
     * 本类将使用多线程创建一个服务器，循环监听接收连接请求
     * */
    public TCPServer() {
    }

    //判断当前是否允许处理请求
    public static boolean agree = true;

    //判断请求是否已经处理
    private boolean isRequestProcessed = false;

    //创建服务器对象
    public static ServerSocket ss = null;

    //启动多线程
    @Override
    public void run() {
        //创建服务器
        Random rd = new Random();
        while (ss == null) {
            try {
                ss = new ServerSocket(Configure.getTCPPort());
                break;
            } catch (IOException e) {
                //重设端口号
                Configure.setTCPPort(rd.nextInt(30000) + 30000);
                Log.e("服务器错误", "服务器创建失败，正在重试");
                //线程暂停0.5秒后继续
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Log.e("线程错误", "线程暂停失败");
                }
            }
        }

        //循环监听
        while (true) {
            //创建连接对象
            Socket so = null;

            try {
                //监听请求
                so = ss.accept();

                //判断当前是否能处理任务
                if (!agree) {
                    so.close();
                    continue;
                }

                //设置不处理其他请求
                agree = false;

                //告诉客户端现在可以处理请求
                so.getOutputStream().write(NetIO.SUCCEED);
                so.getOutputStream().flush();

                //提交任务处理
                ThreadPool.submitReceive(so);

                //设置请求已被处理
                isRequestProcessed = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!isRequestProcessed) {
                    //弹出提示信息
                    MainActivity.showToast("连接断开");
                    //设置服务器可以再次接收请求
                    agree = true;
                    //尝试断开连接
                    if (so != null) {
                        try {
                            so.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}