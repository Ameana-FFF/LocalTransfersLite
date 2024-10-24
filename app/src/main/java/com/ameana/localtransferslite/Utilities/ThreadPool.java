package com.ameana.localtransferslite.Utilities;

import com.ameana.localtransferslite.Net.TCP.TCPReceive;
import com.ameana.localtransferslite.Net.TCP.TCPSend;
import com.ameana.localtransferslite.Net.TCP.TransProgressBar;
import com.ameana.localtransferslite.Net.UDP.UDPSend;

import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ThreadPool {
    /*
     * 该类记录了线程池对象，方便调用
     * 每个线程池容量为1是为了防止用户多次提交任务
     * 线程池会丢弃多次提交的任务
     * */
    private ThreadPool() {
    }

    /*
     * 主要功能线程池
     * */
    public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            6,
            8,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    //进度条线程
    private static final ThreadPoolExecutor PROGRESS_THREAD_POOL = new ThreadPoolExecutor(
            1,
            1,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    //搜索设备线程池
    private static final ThreadPoolExecutor SEARCH_THREAD_POOL = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    //发送消息线程池
    private static final ThreadPoolExecutor SEND_THREAD_POOL = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    //接收消息线程池
    private static final ThreadPoolExecutor RECEIVE_THREAD_POOL = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    //提交搜索设备任务
    public static void submitSearch() {
        synchronized (SEARCH_THREAD_POOL) {
            if (SEARCH_THREAD_POOL.getActiveCount() != 1)
                SEARCH_THREAD_POOL.submit(new UDPSend());
        }
    }

    //提交发送消息任务
    public static void submitSend(String IPAndPort) {
        synchronized (SEND_THREAD_POOL) {
            if (SEND_THREAD_POOL.getActiveCount() != 1)
                SEND_THREAD_POOL.submit(new TCPSend(IPAndPort));
        }
    }

    //提交接收消息任务
    public static void submitReceive(Socket so) {
        synchronized (RECEIVE_THREAD_POOL) {
            if (RECEIVE_THREAD_POOL.getActiveCount() != 1)
                RECEIVE_THREAD_POOL.submit(new TCPReceive(so));
        }
    }

    //提交进度条任务
    public static void submitProgress(long len) {
        synchronized (PROGRESS_THREAD_POOL) {
            PROGRESS_THREAD_POOL.submit(new TransProgressBar(len));
        }
    }
}