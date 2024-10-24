package com.ameana.localtransferslite.Net.TCP;

import androidx.documentfile.provider.DocumentFile;

import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Net.IPLogs;
import com.ameana.localtransferslite.Utilities.FileOperate;
import com.ameana.localtransferslite.Utilities.Message;
import com.ameana.localtransferslite.Utilities.NetIO;
import com.ameana.localtransferslite.Utilities.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPSend extends Thread {
    /*
     * 本类负责发送数据
     * */
    public TCPSend(String IP) {
        this.IP = IP;
    }

    //IP和端口号
    private String IP;

    //启动多线程
    @Override
    public void run() {
        //获取端口号
        String port = IPLogs.get_CONNECTED_IP().get(IP);

        //判断是否存在
        if (port == null)
            return;

        //请求连接
        try (Socket so = new Socket()) {
            //发起连接，设置超时时间为3秒
            so.connect(new InetSocketAddress(IP, Integer.parseInt(port)), 3000);

            //获取网络流
            BufferedOutputStream bos = new BufferedOutputStream(so.getOutputStream(), Configure.getBufferSize());
            BufferedInputStream bis = new BufferedInputStream(so.getInputStream(), Configure.getBufferSize());

            //尝试读取数据
            if (bis.read() == -1) {
                MainActivity.showToast("对方主机繁忙");
                return;
            }

            //判断要发送什么
            if (Message.mess != null) {
                //发送类型
                bos.write(NetIO.MESS);
                bos.flush();
                //发送消息
                NetIO.send(Message.mess, bos);

            } else {
                //弹出弹窗
                MainActivity.showInTransmitting(so, so.getInetAddress().getHostName(), "等待对方同意请求");

                //判断消息类型
                if (Message.file != null) {
                    //发送类型
                    bos.write(NetIO.FILE);
                    bos.flush();
                    //等待确认请求
                    if (bis.read() != NetIO.SUCCEED) {
                        MainActivity.showToast("对方不同意请求");
                        return;
                    }
                    //获取文件大小
                    long fileLen = Message.file.length();
                    //发送文件大小
                    bos.write(NetIO.createHead(fileLen));
                    bos.flush();
                    //启动进度条
                    ThreadPool.submitProgress(fileLen);
                    //发送文件
                    NetIO.fileSend(Message.file, bos);

                } else if (Message.dir != null) {
                    //发送类型
                    bos.write(NetIO.DIR);
                    bos.flush();
                    //等待确认请求
                    if (bis.read() != NetIO.SUCCEED) {
                        MainActivity.showToast("对方不同意请求");
                        return;
                    }
                    //获取文件夹大小
                    long dirLen = FileOperate.dirLength(Message.dir);
                    //发送文件夹大小
                    bos.write(NetIO.createHead(dirLen));
                    bos.flush();
                    //启动进度条
                    ThreadPool.submitProgress(dirLen);
                    //发送文件夹
                    NetIO.dirSend(Message.dir, bos, bis);

                } else if (Message.files != null) {
                    //发送类型
                    bos.write(NetIO.FILES);
                    bos.flush();
                    //等待确认请求
                    if (bis.read() != NetIO.SUCCEED) {
                        MainActivity.showToast("对方不同意请求");
                        return;
                    }
                    //获取所有文件大小
                    long filesLen = 0L;
                    for (DocumentFile file : Message.files) {
                        filesLen += file.length();
                    }
                    //发送所有文件大小
                    bos.write(NetIO.createHead(filesLen));
                    bos.flush();
                    //启动进度条
                    ThreadPool.submitProgress(filesLen);
                    //发送数量
                    bos.write(NetIO.createHead(Message.files.size()));
                    bos.flush();
                    //循环发送文件
                    for (DocumentFile file : Message.files) {
                        NetIO.fileSend(file, bos);
                    }
                }

                //关闭进度条
                TransProgressBar.closeTransProgressBar();
                MainActivity.setDialogText("发送完成，等待对方处理");
            }
            //等待服务器接收
            if (bis.read() != NetIO.SUCCEED)
                throw new IOException();

            //正常断开连接
            so.close();

            //提示成功
            MainActivity.showToast("发送成功");

        } catch (SocketTimeoutException i) {
            MainActivity.showToast("连接超时");
        } catch (Exception e) {
            e.printStackTrace();
            MainActivity.showToast("连接断开");
        } finally {
            TransProgressBar.closeTransProgressBar();
            MainActivity.closeDialog();
            TCPServer.agree = true;
        }
    }
}