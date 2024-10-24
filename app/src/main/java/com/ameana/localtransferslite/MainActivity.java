package com.ameana.localtransferslite;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.Net.IPLogs;
import com.ameana.localtransferslite.Net.TCP.TCPReceive;
import com.ameana.localtransferslite.Net.TCP.TCPServer;
import com.ameana.localtransferslite.Net.UDP.UDPReceive;
import com.ameana.localtransferslite.Utilities.FileOperate;
import com.ameana.localtransferslite.Utilities.Message;
import com.ameana.localtransferslite.Utilities.ThreadPool;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    /*
     * 所有UI对象
     * */
    //上下文
    public static MainActivity mainActivity;
    //处理器
    private static Handler handler;
    //屏幕高度
    private static int screenHeight;
    //屏幕宽度
    private static int screenWidth;
    //设备列表
    private static LinearLayout devicesList;
    //选择文件
    private static Button setFile;
    //选择文件夹
    private static Button setDir;
    //选择消息
    private static Button setMess;
    //选择媒体
    private static Button setImg;
    //取消选择
    private static Button cancel;
    //刷新
    private static Button flushed;
    //提示信息
    private static TextView promptText;
    //剪切板
    private static ClipboardManager clipboard;
    //弹窗
    private static AlertDialog dialog = null;
    //弹窗上的文字
    private static TextView dialogText = null;
    //提示信息
    private static Toast toast = null;
    //通知管理
    public static NotificationManagerCompat notificationManagerCompat;

    //应用启动时
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //初始化配置文件变量
        Configure.initializeVariable();

        //初始化UI变量
        //上下文
        mainActivity = this;
        //处理器
        handler = new Handler(Looper.getMainLooper());
        //屏幕高度
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        //屏幕宽度
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        //设备列表
        devicesList = findViewById(R.id.devicesList);
        //选择文件
        setFile = findViewById(R.id.setFile);
        //选择文件夹
        setDir = findViewById(R.id.setDir);
        //选择消息
        setMess = findViewById(R.id.setMess);
        //选择媒体
        setImg = findViewById(R.id.setImg);
        //取消选择
        cancel = findViewById(R.id.cancel);
        //刷新
        flushed = findViewById(R.id.flushed);
        //提示信息
        promptText = findViewById(R.id.promptText);
        promptText.setMovementMethod(new ScrollingMovementMethod());
        //剪切板
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        //通知管理
        notificationManagerCompat = NotificationManagerCompat.from(this);

        //获取配置文件保存路径
        Configure.fileSaveDir = this.getExternalFilesDir("Download");
        if (Configure.fileSaveDir == null)
            Configure.fileSaveDir = this.getFilesDir();
        //获取缓存文件保存路径
        Configure.cacheSaveDir = this.getExternalCacheDir();
        if (Configure.cacheSaveDir == null)
            Configure.cacheSaveDir = this.getCacheDir();
        //清除缓存文件
        FileOperate.clearedDir(Configure.cacheSaveDir);
        FileOperate.deleteDir(Configure.cachePath);

        //初始化提示内容
        updatePromptText("未选择文件或消息，当前局域网IP：正在识别");

        //设置部分按钮功能
        //刷新按钮
        flushed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //设置1秒后可再次点击
                view.setEnabled(false);
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setEnabled(true);
                    }
                }, 1000);

                //提交搜索线程
                ThreadPool.submitSearch();
            }
        });
        //选择文件按钮
        setFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 7);
            }
        });
        //选择文件夹按钮
        setDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, 9);
            }
        });
        //选择文本消息按钮
        setMess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建弹窗
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);

                //设置标题
                builder.setTitle("请输入文本内容");

                //创建一个文本输入框
                EditText input = new EditText(mainActivity);

                //添加到弹窗内
                builder.setView(input);

                //设置正向按钮
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //获取文本
                        String temp = input.getText().toString();

                        //判断是不是空文本
                        if (!temp.isEmpty()) {
                            //写入消息
                            Message.file = null;
                            Message.dir = null;
                            Message.files = null;
                            Message.mess = temp;

                            //编辑提示信息
                            if (temp.length() > 30)
                                temp = temp.substring(0, 15) + "......" + temp.substring(temp.length() - 15);
                            updatePromptText("已输入：" + temp);
                        }
                        //关闭弹窗
                        dialogInterface.cancel();
                    }
                });

                //设置反向按钮
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //关闭弹窗
                        dialogInterface.cancel();
                    }
                });

                //弹出对话框
                builder.show();
            }
        });
        //选择媒体
        setImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, 11);
            }
        });
        //取消按钮
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message.file = null;
                Message.dir = null;
                Message.mess = null;
                Message.files = null;
                IPLogs.flushedPromptTextIP();
            }
        });

        //获取组件通信
        Intent it = getIntent();

        //交给其他方法处理组件通信内容
        disposeIntent(it);

        //开启主要线程
        ThreadPool.THREAD_POOL_EXECUTOR.submit(new UDPReceive());
        ThreadPool.THREAD_POOL_EXECUTOR.submit(new TCPServer());
        ThreadPool.THREAD_POOL_EXECUTOR.submit(new IPLogs());

        //获取权限
        //获取文件权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }
        //获取通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
            }
        }
    }

    //应用再次被启动时
    @Override
    protected void onNewIntent(Intent intent) {
        //当MainActivity已经启动时，再次被调用会调用这个方法
        super.onNewIntent(intent);

        //设置MainActivity的Intent为新传递的
        setIntent(intent);

        //交给其他方法处理Intent
        disposeIntent(intent);
    }

    //应用退到后台时
    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (notificationManagerCompat.areNotificationsEnabled()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(MainActivity.mainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            //启动前台服务
                            startService(new Intent(this, ForegroundService.class));
                        }
                    } else {
                        //启动前台服务
                        startService(new Intent(this, ForegroundService.class));
                    }
                }
            } else {
                //启动前台服务
                startService(new Intent(this, ForegroundService.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //应用回到前台时
    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (notificationManagerCompat.areNotificationsEnabled()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(MainActivity.mainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            //关闭前台服务
                            stopService(new Intent(this, ForegroundService.class));
                        }
                    } else {
                        //关闭前台服务
                        stopService(new Intent(this, ForegroundService.class));
                    }
                }
            } else {
                //关闭前台服务
                stopService(new Intent(this, ForegroundService.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 该方法负责处理组件通信
     * 该方法不能被其他类调用
     * */
    private void disposeIntent(Intent intent) {
        //判断传递过来的文件是一个还是多个
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            //处理单个文件
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            //判断内容是否为空
            if (uri != null) {
                //将文件属性存入消息类
                Message.file = DocumentFile.fromSingleUri(this, uri);
                Message.dir = null;
                Message.mess = null;
                Message.files = null;

                //读取文件名字
                String fileName = "未知文件名";
                if (Message.file != null)
                    fileName = Message.file.getName();

                //将信息写入UI
                if (fileName.length() > 30)
                    fileName = fileName.substring(0, 15) + "......" + fileName.substring(fileName.length() - 15);
                updatePromptText("已选择文件：" + fileName);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) && intent.getType() != null) {
            //处理多个文件
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

            //判断内容是否为空
            if (uris != null) {
                //将文件属性存入消息类
                Message.files = new ArrayList<>();
                Message.dir = null;
                Message.file = null;
                Message.mess = null;

                //遍历集合得到所有文件
                for (Uri uri : uris) {
                    Message.files.add(DocumentFile.fromSingleUri(this, uri));
                }

                //设置提示
                updatePromptText("已选择：" + Message.files.size() + "个文件");
            }
        } else if (intent.getData() != null) {
            //将文件属性存入消息类
            Message.file = DocumentFile.fromSingleUri(this, intent.getData());
            Message.dir = null;
            Message.mess = null;
            Message.files = null;

            //读取文件名字
            String fileName = "未知文件名";
            if (Message.file != null)
                fileName = Message.file.getName();

            //将信息写入UI
            if (fileName.length() > 30)
                fileName = fileName.substring(0, 15) + "......" + fileName.substring(fileName.length() - 15);
            updatePromptText("已选择文件：" + fileName);
        }
    }

    //使用UI线程更新提示信息
    public static void updatePromptText(String text) {
        synchronized (promptText) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    promptText.setText(text);
                }
            });
        }
    }

    //添加一个设备按钮
    public static void addDevice(String deviceName, String IP) {
        synchronized (devicesList) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //创建按钮
                    Button button = new Button(mainActivity);

                    //设置按钮属性
                    button.setText(deviceName);
                    button.setTag(IP);
                    button.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) (screenHeight * 0.15)
                    ));
                    button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

                    //设置按钮功能
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //设置1秒后可再次点击
                            view.setEnabled(false);
                            view.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    view.setEnabled(true);
                                }
                            }, 1000);

                            //判断用户是否没有选择任何文件或消息
                            if (Message.file == null && Message.mess == null && Message.files == null && Message.dir == null) {
                                //提示未选择
                                showToast("未选择内容");
                            } else {
                                //关闭接收功能
                                TCPServer.agree = false;
                                //启动多线程
                                ThreadPool.submitSend((String) view.getTag());
                            }
                        }
                    });

                    //将按钮添加到盒子
                    devicesList.addView(button);
                }
            });
        }
    }

    //清除全部设备
    public static void clearDevice() {
        synchronized (devicesList) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    devicesList.removeAllViews();
                }
            });
        }
    }

    //弹出一个简短提示
    public static void showToast(String str) {
        synchronized (mainActivity) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (toast != null)
                        toast.cancel();
                    toast = Toast.makeText(mainActivity, str, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    }

    //关闭当前弹窗
    public static void closeDialog() {
        synchronized (MainActivity.class) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dialog != null)
                            dialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        dialog = null;
                        dialogText = null;
                    }
                }
            });
        }
    }

    //设置弹窗上的文本内容
    public static void setDialogText(String str) {
        synchronized (MainActivity.class) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dialogText != null)
                            dialogText.setText(str);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    //创建一个弹窗用于弹出传输中
    public static void showInTransmitting(Socket so, String title, String str) {
        synchronized (MainActivity.class) {
            //确保之前的弹窗已关闭
            closeDialog();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //创建弹窗
                    AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);

                    //设置标题
                    builder.setTitle(title);

                    //创建一个文本输入框
                    dialogText = new TextView(mainActivity);

                    //设置初始化文本
                    dialogText.setMovementMethod(new ScrollingMovementMethod());
                    dialogText.setText(str);
                    dialogText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

                    //将文本加入弹窗
                    builder.setView(dialogText);

                    //设置反按钮
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                so.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                dialogInterface.cancel();
                            }
                        }
                    });

                    //创建AlertDialog对象
                    dialog = builder.create();

                    //设置弹窗不可通过返回键取消
                    dialog.setCancelable(false);

                    //显示弹窗
                    dialog.show();
                }
            });
        }
    }

    //创建一个用于确认是否接收的弹窗
    public static void showStatus(String title, String str) {
        synchronized (MainActivity.class) {
            //确保上一个弹窗已经被关闭
            closeDialog();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //创建弹窗
                    AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);

                    //设置标题
                    builder.setTitle(title);

                    //创建一个文本输入框
                    TextView textView = new TextView(mainActivity);

                    //设置初始化文本
                    textView.setMovementMethod(new ScrollingMovementMethod());
                    textView.setText(str);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

                    //将文本加入弹窗
                    builder.setView(textView);

                    //设置正按钮
                    builder.setPositiveButton("同意", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            TCPReceive.setStatus(TCPReceive.CONSENT);
                            dialogInterface.cancel();
                        }
                    });

                    //设置反按钮
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            TCPReceive.setStatus(TCPReceive.NOT_CONSENT);
                            dialogInterface.cancel();
                        }
                    });

                    //创建AlertDialog对象
                    dialog = builder.create();

                    //设置弹窗不可通过返回键取消
                    dialog.setCancelable(false);

                    //显示弹窗
                    dialog.show();
                }
            });
        }
    }

    //创建一个可复制文本内容的弹窗
    public static void showReproducibleTextView(String title, String str) {
        synchronized (MainActivity.class) {
            //确保之前的弹窗已经关闭
            closeDialog();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //创建弹窗
                    AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);

                    //设置标题
                    builder.setTitle(title);

                    //创建文本框
                    TextView textView = new TextView(mainActivity);

                    //设置文本框内容
                    textView.setText(str);

                    //设置文本框可复制且长文本时可以上下滑动
                    textView.setMovementMethod(new ScrollingMovementMethod());
                    textView.setTextIsSelectable(true);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

                    //将文本框加入弹窗
                    builder.setView(textView);

                    //设置正按钮
                    builder.setPositiveButton("复制", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                //写入剪切板
                                ClipData clip = ClipData.newPlainText("simple text", str);
                                clipboard.setPrimaryClip(clip);
                                //弹出提示信息
                                showToast("文本已复制到剪切板");
                            } catch (Exception e) {
                                e.printStackTrace();
                                //弹出出错提示
                                showToast("复制失败");
                            } finally {
                                TCPServer.agree = true;
                                dialogInterface.cancel();
                            }
                        }
                    });

                    //设置反按钮
                    builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            TCPServer.agree = true;
                            dialogInterface.cancel();
                        }
                    });

                    //创建AlertDialog对象
                    dialog = builder.create();

                    //设置弹窗不可通过返回键取消
                    dialog.setCancelable(false);

                    //显示弹窗
                    dialog.show();
                }
            });
        }
    }

    //弹出一个提示信息框
    public static void showMessageBox(String title, String str) {
        synchronized (MainActivity.class) {
            //确保之前的弹窗已经关闭
            closeDialog();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //创建弹窗
                    AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);

                    //设置标题
                    builder.setTitle(title);

                    //创建文本框
                    TextView textView = new TextView(mainActivity);

                    //设置文本框内容
                    textView.setText(str);

                    //设置文本框
                    textView.setMovementMethod(new ScrollingMovementMethod());
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

                    //将文本框加入弹窗
                    builder.setView(textView);

                    //设置反按钮
                    builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            TCPServer.agree = true;
                            dialogInterface.cancel();
                        }
                    });

                    //创建AlertDialog对象
                    dialog = builder.create();

                    //设置弹窗不可通过返回键取消
                    dialog.setCancelable(false);

                    //显示弹窗
                    dialog.show();
                }
            });
        }
    }

    //判断任务返回值
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7 && data != null && data.getData() != null) {
            //将文件属性存入消息类
            Message.file = DocumentFile.fromSingleUri(this, data.getData());
            Message.dir = null;
            Message.mess = null;
            Message.files = null;

            //读取文件名字
            String fileName = "未知文件名";
            if (Message.file != null)
                fileName = Message.file.getName();

            //将信息写入UI
            if (fileName.length() > 30)
                fileName = fileName.substring(0, 15) + "......" + fileName.substring(fileName.length() - 15);
            updatePromptText("已选择文件：" + fileName);
        } else if (requestCode == 9 && data != null && data.getData() != null) {
            //将文件夹属性存入消息类
            Message.dir = DocumentFile.fromTreeUri(this, data.getData());
            Message.file = null;
            Message.mess = null;
            Message.files = null;

            //读取文件夹名字
            String fileName = "未知文件夹名";
            if (Message.dir != null)
                fileName = Message.dir.getName();

            //将信息写入UI
            if (fileName.length() > 30)
                fileName = fileName.substring(0, 15) + "......" + fileName.substring(fileName.length() - 15);
            updatePromptText("已选择文件夹：" + fileName);
        } else if (requestCode == 11 && data != null) {
            //判断媒体选择数量
            if (data.getClipData() != null) {
                //选择多个媒体
                ClipData clipData = data.getClipData();

                //设置消息
                Message.files = new ArrayList<>();
                Message.dir = null;
                Message.file = null;
                Message.mess = null;

                //写入媒体
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Message.files.add(DocumentFile.fromSingleUri(this, clipData.getItemAt(i).getUri()));
                }

                //设置提示
                updatePromptText("已选择：" + Message.files.size() + "个媒体");
            } else if (data.getData() != null) {
                //选择单个媒体
                //设置消息
                Message.file = DocumentFile.fromSingleUri(this, data.getData());
                Message.files = null;
                Message.dir = null;
                Message.mess = null;

                //读取媒体名字
                String fileName = "未知文件名";
                if (Message.file != null)
                    fileName = Message.file.getName();

                //将信息写入UI
                if (fileName.length() > 30)
                    fileName = fileName.substring(0, 15) + "......" + fileName.substring(fileName.length() - 15);
                updatePromptText("已选择文件：" + fileName);
            }
        }
    }
}