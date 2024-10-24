package com.ameana.localtransferslite.Net.TCP;

import android.util.Log;

import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Utilities.FileOperate;

import java.util.Locale;

public class TransProgressBar extends Thread {
    /*
     * 该类负责计算和更新进度
     * */
    public TransProgressBar(long maxValue) {
        //初始化变量
        openTransProgressBar();
        if (maxValue > 0)
            this.maxValue = maxValue;
        else
            this.maxValue = 1;
        resetCurrentValue();
        this.previousValue = 0L;
    }

    //记录总进度
    private long maxValue = 1L;

    //记录当前进度
    private static long currentValue = 0L;

    //记录关闭或开启状态
    private static boolean TransProgressBarSwitch = true;

    //记录上一次进度
    private long previousValue = 0L;

    //创建两把多线程锁
    private static final Object currentValueLock = new Object();
    private static final Object TransProgressBarSwitchLock = new Object();

    //重置当前进度
    private static void resetCurrentValue() {
        synchronized (currentValueLock) {
            TransProgressBar.currentValue = 0L;
        }
    }

    //获取当前进度
    private static long getCurrentValue() {
        synchronized (currentValueLock) {
            return TransProgressBar.currentValue;
        }
    }

    //增加当前进度
    public static void addCurrentValue(long Value) {
        synchronized (currentValueLock) {
            TransProgressBar.currentValue += Value;
        }
    }

    //获取进度条状态
    private static boolean getTransProgressBar() {
        synchronized (TransProgressBarSwitchLock) {
            return TransProgressBarSwitch;
        }
    }

    //打开进度条
    private static void openTransProgressBar() {
        synchronized (TransProgressBarSwitchLock) {
            TransProgressBarSwitch = true;
        }
    }

    //关闭进度条
    public static void closeTransProgressBar() {
        synchronized (TransProgressBarSwitchLock) {
            TransProgressBarSwitch = false;
        }
    }

    //启动多线程
    @Override
    public void run() {
        try {
            //创建StringBuffer
            StringBuffer sb = new StringBuffer();

            //初始化提示信息
            if (getTransProgressBar())
                MainActivity.setDialogText("总大小：*\n进度：*\n速度：*\n剩余时间：*\n剩余大小：*");

            //等待1秒后继续
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e("线程错误", "线程暂停失败");
            }

            //循环检测进度
            while (getCurrentValue() < maxValue && getTransProgressBar()) {
                //计算进度
                double progress = (double) getCurrentValue() / maxValue * 100;

                //计算速度
                long speed = getCurrentValue() - previousValue;

                //计算剩余大小
                long residueSize = maxValue - getCurrentValue();

                //计算剩余时间
                long timeRemaining = -1;
                if (speed > 0)
                    timeRemaining = residueSize / speed;

                //重置上一个进度
                previousValue = getCurrentValue();

                //清空StringBuffer
                sb.setLength(0);

                //拼接文件总大小
                sb.append("总大小：");
                sb.append(FileOperate.formatUnit(maxValue));
                sb.append("\n");

                //拼接进度
                sb.append(String.format(Locale.CHINA, "进度：%.2f", progress));
                sb.append("%\n");

                //拼接速度
                sb.append("速度：");
                sb.append(FileOperate.formatUnit(speed));
                sb.append("每秒\n");

                //拼接剩余时间
                sb.append("剩余时间：");
                if (timeRemaining == -1) {
                    sb.append("∞\n");
                } else if (timeRemaining < 60) {
                    sb.append(timeRemaining);
                    sb.append("秒\n");
                } else if (timeRemaining < 3600) {
                    sb.append(timeRemaining / 60);
                    sb.append("分");
                    sb.append(timeRemaining % 60);
                    sb.append("秒\n");
                } else if (timeRemaining < 86400) {
                    sb.append(timeRemaining / 3600);
                    sb.append("时");
                    sb.append((timeRemaining % 3600) / 60);
                    sb.append("分\n");
                } else {
                    sb.append(timeRemaining / 86400);
                    sb.append("天");
                    sb.append((timeRemaining % 86400) / 3600);
                    sb.append("时\n");
                }

                //拼接剩余大小
                sb.append("剩余大小：");
                sb.append(FileOperate.formatUnit(residueSize));

                //写入弹窗
                if (getTransProgressBar())
                    MainActivity.setDialogText(sb.toString());

                //等待1秒后重新计算
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("线程错误", "线程暂停失败");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeTransProgressBar();
        }
    }
}