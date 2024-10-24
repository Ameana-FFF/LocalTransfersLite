package com.ameana.localtransferslite.Utilities;

import androidx.documentfile.provider.DocumentFile;

import com.ameana.localtransferslite.Configure.Configure;
import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Net.TCP.TransProgressBar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public abstract class NetIO {
    /*
     * 本类是一个工具类提供网络数据传输方法
     * */
    private NetIO() {
    }

    //发送消息
    public static final byte MESS = 7;
    //发送文件
    public static final byte FILE = 9;
    //发送文件夹
    public static final byte DIR = 11;
    //发送媒体文件
    public static final byte FILES = 12;

    //生成内容大小头文件
    public static byte[] createHead(long len) {
        //生成头文件
        ByteBuffer code = ByteBuffer.allocate(12);
        code.putShort((short) 9786);
        code.putLong(len);
        code.putShort((short) 14886);

        //返回结果
        return code.array();
    }

    //解读内容大小头文件
    public static long unscrambleHead(byte[] head) {
        //判断长度
        if (head.length != 12)
            return -1;

        //包装到缓冲
        ByteBuffer buffer = ByteBuffer.wrap(head);

        //判断数据完整性
        if (buffer.getShort(0) != 9786 || buffer.getShort(10) != 14886)
            return -1;

        //返回数据
        return buffer.getLong(2);
    }

    //服务器返回通信
    //成功
    public static final byte SUCCEED = 111;
    //错误
    public static final byte ERROR = 110;

    //发送消息
    public static void send(String mess, BufferedOutputStream bos) throws IOException {
        //将消息转换为二进制数据
        byte[] data = mess.getBytes("UTF-8");

        //发送头文件
        bos.write(createHead(data.length));

        //防止发送空文本内容
        if (data.length == 0) {
            //刷新数据
            bos.flush();
            return;
        }

        //发送数据
        bos.write(data);

        //刷新数据
        bos.flush();
    }

    //接收消息
    public static String receive(BufferedInputStream bis) throws IOException {
        //读取头文件
        byte[] head = new byte[12];
        dataIntegrality(head, head.length, bis);

        //解密头文件
        long dataLen = unscrambleHead(head);

        //判断是否损坏
        if (dataLen == -1)
            throw new IOException();

        //数据过大时抛出异常
        if (dataLen > 1024 * 1024 * 16)
            throw new IOException();

        //判断是不是空文本内容
        if (dataLen == 0)
            return "";

        //创建等大数组接收数据
        byte[] data = new byte[(int) dataLen];

        //接收数据
        dataIntegrality(data, (int) dataLen, bis);

        //返回数据
        return new String(data, "UTF-8");
    }

    //发送文件
    public static void fileSend(DocumentFile file, BufferedOutputStream bos) throws IOException {
        //读取文件流
        BufferedInputStream bis = new BufferedInputStream(MainActivity.mainActivity.getContentResolver().openInputStream(file.getUri()), Configure.getBufferSize());

        //发送文件大小头文件
        bos.write(createHead(file.length()));

        //创建读取容器
        byte[] temp = new byte[Configure.getBufferSize()];

        //读取并发送
        int len;
        while ((len = bis.read(temp)) != -1) {
            bos.write(temp, 0, len);
            TransProgressBar.addCurrentValue(len);
        }
        bos.flush();

        //发送文件名称
        send(file.getName(), bos);
    }

    //接收文件
    public static File fileReceive(File savePath, File cachePath, BufferedInputStream bis) throws IOException {
        //判断路径是否存在
        if (!savePath.exists())
            return null;

        //判断是否为文件夹
        if (!savePath.isDirectory())
            return null;

        //读取头文件
        byte[] head = new byte[12];
        dataIntegrality(head, head.length, bis);

        //解密头文件
        long fileLen = unscrambleHead(head);

        //判断头文件是否损坏
        if (fileLen == -1)
            throw new IOException();

        //创建临时文件
        File temp = new File(cachePath, FileOperate.randomName());
        if (!temp.createNewFile())
            throw new IOException();

        //创建输出流
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temp), Configure.getBufferSize());

        //写入文件
        byte[] data = new byte[Configure.getBufferSize()];
        int len = 0;
        while (true) {
            if ((fileLen -= len) < data.length)
                break;
            len = bis.read(data);
            bos.write(data, 0, len);
            TransProgressBar.addCurrentValue(len);
        }
        dataIntegrality(data, (int) fileLen, bis);
        bos.write(data, 0, (int) fileLen);
        bos.flush();

        //读取文件名称
        String fileName = receive(bis);

        //重命名并移动到保存目录后返回移动的文件
        return FileOperate.renameTo(temp, new File(savePath, fileName));
    }

    //发送文件夹
    public static void dirSend(DocumentFile dir, BufferedOutputStream bos, BufferedInputStream bis) throws IOException {
        //读取文件夹结构
        ArrayList<DocumentFile> files = new ArrayList<>();
        String[] paths = FileOperate.readDirStructure(dir, files);

        //判断是否为null
        if (paths == null)
            throw new IOException();

        //发送文件夹结构
        send(paths[0], bos);

        //发送文件结构
        send(paths[1], bos);

        //等待对方处理
        if (bis.read() != SUCCEED)
            throw new IOException();

        //按顺序发送文件
        for (DocumentFile documentFile : files) {
            fileSend(documentFile, bos);
        }

        //发送文件夹名称
        send(dir.getName(), bos);
    }

    /*
     * 接收文件夹
     * */
    public static File dirReceive(File savePath, File cachePath, BufferedInputStream bis, BufferedOutputStream bos) throws IOException {
        //接收文件结构
        String dirsPathTemp = receive(bis);
        String filesPathTemp = receive(bis);

        //处理文件夹结构
        String[] dirsPath = dirsPathTemp.split("&");
        String[] filesPath = filesPathTemp.split("&");

        //创建临时文件夹
        File tempSave = new File(cachePath, FileOperate.randomName());

        //创建临时文件夹
        if (!tempSave.mkdirs())
            throw new IOException();

        //创建全部文件夹
        for (String s : dirsPath) {
            if (!s.isEmpty()) {
                if (!(new File(tempSave, s)).mkdirs())
                    throw new IOException();
            }
        }

        //返回成功
        bos.write(SUCCEED);
        bos.flush();

        //按顺序接收文件
        for (String s : filesPath) {
            if (!s.isEmpty()) {
                fileReceive(new File(tempSave, s.substring(0, s.lastIndexOf("/"))), cachePath, bis);
            }
        }

        //接收文件夹名称
        String dirName = receive(bis);

        //重命名并移动到保存目录
        File dir = FileOperate.renameTo(tempSave, new File(savePath, dirName));

        //返回传输完成的文件夹
        return dir;
    }

    /*
     * 保证读取数据完整性
     * */
    public static void dataIntegrality(byte[] data, int len, BufferedInputStream bis) throws IOException {
        if (len > data.length)
            return;

        int index = 0;
        while (len > 0) {
            int readLen = bis.read(data, index, len);
            index += readLen;
            len -= readLen;
        }
    }
}