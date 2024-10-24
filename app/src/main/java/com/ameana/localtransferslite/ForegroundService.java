package com.ameana.localtransferslite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class ForegroundService extends Service {
    //前台服务通知的ID
    private static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 3;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //设置通知点击后的行为
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            //创建通知对象
            Notification notification = null;

            //设置通知渠道(如果安卓版本大于8)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //创建通知渠道
                NotificationChannel channel = new NotificationChannel("FOREGROUND_SERVICE_CHANNEL_ID", "LocalTransfersLite前台服务", NotificationManager.IMPORTANCE_MIN);
                channel.setDescription("应用的前台服务通知，用于保证应用在后台的正常工作");

                //禁用灯光和震动
                channel.enableLights(false);
                channel.enableVibration(false);

                //获取通知管理器并注册通知渠道
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);

                //创建通知
                notification = new Notification.Builder(this, "FOREGROUND_SERVICE_CHANNEL_ID")
                        .setContentTitle("LocalTransfersLite")
                        .setContentText("应用正在运行")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent)
                        .build();
            } else {
                //创建通知
                notification = new Notification.Builder(this)
                        .setContentTitle("LocalTransfersLite")
                        .setContentText("应用正在运行")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent)
                        .build();
            }

            // 启动前台服务
            startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //告诉系统如果应用被关闭不需要重启应用
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            //停止前台服务
            stopForeground(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}