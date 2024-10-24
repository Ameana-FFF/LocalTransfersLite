package com.ameana.localtransferslite.Net.TCP;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Utilities.FileOperate;
import com.ameana.localtransferslite.Utilities.NetIO;
import com.ameana.localtransferslite.Utilities.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.net.Socket;

public class TCPReceive extends Thread {
    /*
     * 本类负责处理接收请求
     * */
    public TCPReceive(Socket so) {
        this.so = so;
        TCPReceive.setStatus(TCPReceive.UNDETERMINED);
    }

    //socket连接对象
    private final Socket so;

    //用户的选择模式
    public static final int CONSENT = 77;
    public static final int NOT_CONSENT = 78;
    public static final int UNDETERMINED = 79;

    //用户当前的选择
    private static int status = UNDETERMINED;

    //获取用户的选择
    public static int getStatus() {
        synchronized (TCPReceive.class) {
            return status;
        }
    }

    //设置选择
    public static void setStatus(int status) {
        synchronized (TCPReceive.class) {
            TCPReceive.status = status;
        }
    }

    //判断消息框是否弹出
    private boolean isMessageShown = false;

    //文件保存路径
    private File savePath = Configure.savePath;

    //文件缓存路径
    private File cachePath = Configure.cachePath;

    //启动多线程
    @Override
    public void run() {
        //创建缓存流
        try (BufferedInputStream bis = new BufferedInputStream(so.getInputStream());
             BufferedOutputStream bos = new BufferedOutputStream(so.getOutputStream());) {
            //接收类型
            int type = bis.read();

            //判断类型
            if (type == NetIO.MESS) {
                //接收消息
                String str = NetIO.receive(bis);
                //弹出消息
                MainActivity.showReproducibleTextView(so.getInetAddress().getHostName(), str);
                //设置消息框已经弹出
                isMessageShown = true;
                //弹出一条提示
                MainActivity.showToast("收到一条信息");

            } else {
                //判断有没有权限保存
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(MainActivity.mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        savePath = Configure.fileSaveDir;
                        cachePath = Configure.cacheSaveDir;
                    }
                }

                //检查保存目录是否存在
                if (!savePath.exists())
                    savePath.mkdirs();
                if (!cachePath.exists())
                    cachePath.mkdirs();

                //进一步判断消息类型
                if (type == NetIO.FILE) {
                    //弹出同意框
                    MainActivity.showStatus(so.getInetAddress().getHostName(), so.getInetAddress().getHostName() + "发来一个文件，是否同意接收");
                    //等待用户选择
                    while (TCPReceive.getStatus() == UNDETERMINED) {
                    }
                    //判断用户选择
                    if (TCPReceive.getStatus() == NOT_CONSENT) {
                        //用户不同意
                        bos.write(NetIO.ERROR);
                        bos.flush();
                        so.close();
                        return;
                    } else if (TCPReceive.getStatus() == CONSENT) {
                        //用户同意
                        bos.write(NetIO.SUCCEED);
                        bos.flush();
                    } else {
                        MainActivity.showToast("连接断开");
                        return;
                    }
                    //弹出弹窗
                    MainActivity.showInTransmitting(so, so.getInetAddress().getHostName(), "传输中...");
                    //接收文件大小
                    byte[] fileLenTemp = new byte[12];
                    NetIO.dataIntegrality(fileLenTemp, fileLenTemp.length, bis);
                    long fileLen = NetIO.unscrambleHead(fileLenTemp);
                    //启动进度条
                    ThreadPool.submitProgress(fileLen);
                    //接收文件
                    NetIO.fileReceive(savePath, cachePath, bis);

                } else if (type == NetIO.DIR) {
                    //弹出同意框
                    MainActivity.showStatus(so.getInetAddress().getHostName(), so.getInetAddress().getHostName() + "发来一个文件夹，是否同意接收");
                    //等待用户选择
                    while (TCPReceive.getStatus() == UNDETERMINED) {
                    }
                    //判断用户选择
                    if (TCPReceive.getStatus() == NOT_CONSENT) {
                        //用户不同意
                        bos.write(NetIO.ERROR);
                        bos.flush();
                        so.close();
                        return;
                    } else if (TCPReceive.getStatus() == CONSENT) {
                        //用户同意
                        bos.write(NetIO.SUCCEED);
                        bos.flush();
                    } else {
                        MainActivity.showToast("连接断开");
                        return;
                    }
                    //弹出弹窗
                    MainActivity.showInTransmitting(so, so.getInetAddress().getHostName(), "传输中...");
                    //接收文件大小
                    byte[] dirLenTemp = new byte[12];
                    NetIO.dataIntegrality(dirLenTemp, dirLenTemp.length, bis);
                    long dirLen = NetIO.unscrambleHead(dirLenTemp);
                    //启动进度条
                    ThreadPool.submitProgress(dirLen);
                    //接收文件夹
                    NetIO.dirReceive(savePath, cachePath, bis, bos);

                } else if (type == NetIO.FILES) {
                    //弹出同意框
                    MainActivity.showStatus(so.getInetAddress().getHostName(), so.getInetAddress().getHostName() + "发来多个文件(媒体)，是否同意接收");
                    //等待用户选择
                    while (TCPReceive.getStatus() == UNDETERMINED) {
                    }
                    //判断用户选择
                    if (TCPReceive.getStatus() == NOT_CONSENT) {
                        //用户不同意
                        bos.write(NetIO.ERROR);
                        bos.flush();
                        so.close();
                        return;
                    } else if (TCPReceive.getStatus() == CONSENT) {
                        //用户同意
                        bos.write(NetIO.SUCCEED);
                        bos.flush();
                    } else {
                        MainActivity.showToast("连接断开");
                        return;
                    }
                    //弹出弹窗
                    MainActivity.showInTransmitting(so, so.getInetAddress().getHostName(), "传输中...");
                    //接收文件大小
                    byte[] filesLenTemp = new byte[12];
                    NetIO.dataIntegrality(filesLenTemp, filesLenTemp.length, bis);
                    long filesLen = NetIO.unscrambleHead(filesLenTemp);
                    //启动进度条
                    ThreadPool.submitProgress(filesLen);
                    //接收文件数量
                    byte[] head = new byte[12];
                    NetIO.dataIntegrality(head, head.length, bis);
                    long number = NetIO.unscrambleHead(head);
                    //循环接收文件
                    for (long i = 0; i < number; i++) {
                        NetIO.fileReceive(savePath, cachePath, bis);
                    }
                }

                //关闭进度条
                TransProgressBar.closeTransProgressBar();

                //设置提示消息
                StringBuffer sb = new StringBuffer();
                sb.append("接收成功，文件保存在：");
                sb.append(savePath.toString().substring(Configure.USER_ROOT_DIR.toString().length() + 1));
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(MainActivity.mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        sb.append("\n\n由于未授予储存权限，所以文件将保存在应用根目录下，授予储存权限后即可保存在下载目录下");
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (MainActivity.notificationManagerCompat.areNotificationsEnabled()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(MainActivity.mainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                sb.append("\n\n如果想要提高后台和熄屏传输的稳定性，请开启通知权限，并同意应用的前台服务");
                            }
                        }
                    } else {
                        sb.append("\n\n如果想要提高后台和熄屏传输的稳定性，请开启通知权限，并同意应用的前台服务");
                    }
                }

                //弹出提示框
                MainActivity.showMessageBox("提示", sb.toString());

                //设置消息框已经弹出
                isMessageShown = true;

                //弹出一条提示
                MainActivity.showToast("接收完成");
            }
            //返回接收成功
            bos.write(NetIO.SUCCEED);
            bos.flush();

            //关闭连接
            so.close();

        } catch (Exception e) {
            e.printStackTrace();
            MainActivity.showToast("连接断开");
        } finally {
            TransProgressBar.closeTransProgressBar();
            if (!isMessageShown) {
                TCPServer.agree = true;
                MainActivity.closeDialog();
            }
            //清除缓存文件
            FileOperate.clearedDir(Configure.cacheSaveDir);
            FileOperate.deleteDir(Configure.cachePath);
        }
    }
}