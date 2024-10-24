package com.ameana.localtransferslite.Net;

import android.util.Log;

import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Utilities.Message;
import com.ameana.localtransferslite.Utilities.ThreadPool;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPLogs extends Thread {
    /*
     * 该类记录本地IP和已连接的IP
     * 该类的方法可以保证多线程读取数据的安全性
     * */
    public IPLogs() {
    }

    //记录本地IP
    private static final HashSet<String> LOCAL_IP = new HashSet<>();
    //记录已连接的IP
    private static final HashMap<String, String> CONNECTED_IP = new HashMap<>();

    //启动多线程
    @Override
    public void run() {
        while (true) {
            //刷新IP地址
            flushedLocalIP();

            //刷新提示UI
            flushedPromptTextIP();

            //线程暂停几秒后继续
            try {
                Thread.sleep(Configure.getUIFlushedFrequency());
            } catch (InterruptedException e) {
                Log.e("线程错误", "线程暂停失败");
            }
        }
    }

    //向LOCAL_IP里添加数据
    public static boolean add_LOCAL_IP(String IP) {
        synchronized (LOCAL_IP) {
            return LOCAL_IP.add(IP);
        }
    }

    //向LOCAL_IP里添加多个数据
    public static boolean add_LOCAL_IP(HashSet<String> IPS) {
        synchronized (LOCAL_IP) {
            return LOCAL_IP.addAll(IPS);
        }
    }

    //向CONNECTED_IP里添加数据
    public static void add_CONNECTED_IP(String IP, String port) {
        synchronized (CONNECTED_IP) {
            CONNECTED_IP.put(IP, port);
        }
    }

    //获取所有LOCAL_IP里的数据
    public static HashSet<String> get_LOCAL_IP() {
        synchronized (LOCAL_IP) {
            return new HashSet<String>(LOCAL_IP);
        }
    }

    //获取所有CONNECTED_IP里的数据
    public static HashMap<String, String> get_CONNECTED_IP() {
        synchronized (CONNECTED_IP) {
            return new HashMap<String, String>(CONNECTED_IP);
        }
    }

    //删除一个指定LOCAL_IP里的数据
    public static boolean rm_LOCAL_IP(String IP) {
        synchronized (LOCAL_IP) {
            return LOCAL_IP.remove(IP);
        }
    }

    //删除一个指定CONNECTED_IP里的数据
    public static void rm_CONNECTED_IP(String IP) {
        synchronized (CONNECTED_IP) {
            CONNECTED_IP.remove(IP);
        }
    }

    //清空LOCAL_IP里的数据
    public static void clear_LOCAL_IP() {
        synchronized (LOCAL_IP) {
            LOCAL_IP.clear();
        }
    }

    //清空CONNECTED_IP里的数据
    public static void clear_CONNECTED_IP() {
        synchronized (CONNECTED_IP) {
            CONNECTED_IP.clear();
        }
    }

    /*
     * 刷新本地IP地址
     * */
    public static void flushedLocalIP() {
        //获取ip地址
        ArrayList<InetAddress> inetArr = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetArr.add(inetAddresses.nextElement());
                }
            }
        } catch (SocketException e) {
            Log.e("错误", "本地IP获取失败！");
            return;
        }

        //第一次过滤ip
        ArrayList<String> temp = new ArrayList<>();
        for (InetAddress address : inetArr) {
            Pattern pt = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
            Matcher mc = pt.matcher(address.toString());
            while (mc.find()) {
                temp.add(mc.group());
            }
        }

        //第二次过滤ip
        HashSet<String> IPlist = new HashSet<>();
        for (String s : temp) {
            int tempIP = Integer.parseInt(s.split("\\.")[0]);
            if (tempIP == 192 || tempIP == 172)
                IPlist.add(s);
        }

        //判断是否断网，如果没网，则清空IP和设备
        if (IPlist.isEmpty()) {
            IPLogs.clear_LOCAL_IP();
            IPLogs.clear_CONNECTED_IP();
            MainActivity.clearDevice();
        }

        //判断IP是否改变
        boolean judgeTemp;
        for (String newLocalIP : IPlist) {
            judgeTemp = true;
            for (String localIP : IPLogs.get_LOCAL_IP()) {
                if (localIP.equals(newLocalIP)) {
                    judgeTemp = false;
                    break;
                }
            }
            if (judgeTemp) {
                //读取的IP在记录里找不到证明IP改变了
                //清空之前的IP
                IPLogs.clear_LOCAL_IP();
                //添加新IP
                IPLogs.add_LOCAL_IP(IPlist);
                //启动查找线程重新查找设备
                ThreadPool.submitSearch();
                //结束方法
                return;
            }
        }
    }

    /*
     * 调用主线程UI方法更新promptText上显示的IP地址(当用户未选择任何文件或消息时)
     * */
    public static void flushedPromptTextIP() {
        //判断用户是否没有选择任何文件或消息
        if (!(Message.file == null && Message.mess == null && Message.files == null && Message.dir == null))
            return;

        //获取IP
        StringBuffer sb = new StringBuffer();
        sb.append("未选择文件或消息，当前局域网IP：");
        HashSet<String> ips = IPLogs.get_LOCAL_IP();
        if (ips.isEmpty()) {
            sb.append("无局域网IP");
        } else {
            for (String s : ips) {
                sb.append("<");
                sb.append(s);
                sb.append(">");
            }
        }

        //刷新UI
        MainActivity.updatePromptText(sb.toString());
    }
}