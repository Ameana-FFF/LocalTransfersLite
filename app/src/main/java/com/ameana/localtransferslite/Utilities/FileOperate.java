package com.ameana.localtransferslite.Utilities;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public abstract class FileOperate {
    /*
     * 这是一个工具类
     * 本类会对文件进行一些基本操作
     * */
    private FileOperate() {
    }

    /*
     * 该方法可以移动文件或文件夹
     * 也可以重命名
     * 重命名时只要保证newFile的父级路径与file一致，名字为想要修改的名字即可
     * 方法会返回修改过后的文件
     * 如果该名字的文件已存在，方法会随机生成一个前缀名
     * */
    public static File renameTo(File file, File newFile) {
        //判断源文件存不存在
        if (!file.exists())
            return null;

        //判断新文件的父级路径是否存在
        String newFileParent = newFile.getParent();
        if (!(new File(newFileParent).exists()))
            return null;

        //判断新文件是否存在
        if (newFile.exists()) {
            StringBuffer newFileName = new StringBuffer();
            String oldFileName = newFile.getName();
            int index = oldFileName.lastIndexOf(".");
            int count = 2;
            while (newFile.exists()) {
                if (index == -1) {
                    newFileName.setLength(0);
                    newFileName.append(oldFileName);
                    newFileName.append("_");
                    if (count < 100000000) {
                        newFileName.append(count);
                        count++;
                    } else {
                        newFileName.append(UUID.randomUUID().toString().split("-")[0]);
                    }
                    newFile = new File(newFileParent, newFileName.toString());
                } else {
                    newFileName.setLength(0);
                    newFileName.append(oldFileName.substring(0, index));
                    newFileName.append("_");
                    if (count < 100000000) {
                        newFileName.append(count);
                        count++;
                    } else {
                        newFileName.append(UUID.randomUUID().toString().split("-")[0]);
                    }
                    newFileName.append(oldFileName.substring(index));
                    newFile = new File(newFileParent, newFileName.toString());
                }
            }
        }

        //移动或重命名文件
        file.renameTo(newFile);

        //返回新文件
        return newFile;
    }

    /*
     * 随机文件名字
     * */
    public static String randomName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /*
     * 文件夹结构返回
     * 该方法会把文件夹里的文件结构以字符串的形式返回
     * 返回的是相对文件夹的路径
     * 返回的路径会放在长度为2的数组里
     * 数组0索引是文件夹的相对路径
     * 数组1索引是文件的相对路径
     * 如果传入一个文件集合，则本方法会按照字符串记录的文件顺序将文件写入集合
     * */
    public static String[] readDirStructure(DocumentFile dir, ArrayList<DocumentFile> files) {
        //确保文件夹存在
        if (!dir.exists())
            return null;

        //确保是文件夹
        if (!dir.isDirectory())
            return null;

        //创建数组来保存数据
        String[] paths = new String[2];

        //调用dirFind方法辅助查找
        StringBuffer dirsPath = new StringBuffer();
        StringBuffer filesPath = new StringBuffer();
        dirFind(dir, dirsPath, filesPath, ".", files);

        //包装结果
        paths[0] = dirsPath.toString();
        paths[1] = filesPath.toString();

        //返回结果
        return paths;
    }

    /*
     * 方法负责辅助readDirStructure获取文件夹结构
     * 本方法为辅助方法不允许其他类调用
     * */
    private static void dirFind(DocumentFile dir, StringBuffer dirsPath, StringBuffer filesPath, String nowPath, ArrayList<DocumentFile> files) {
        //获取文件夹内所有内容
        for (DocumentFile file : dir.listFiles()) {
            if (file.isDirectory()) {
                //如果是文件夹
                //添加文件夹的相对路径到dirsPath
                dirsPath.append(nowPath);
                dirsPath.append("/");
                dirsPath.append(file.getName());
                dirsPath.append("&");
                //递归查找这个文件夹里的文件结构
                dirFind(file, dirsPath, filesPath, nowPath + "/" + file.getName(), files);
            } else {
                //如果是文件
                //添加文件的相对路径到filesPath
                filesPath.append(nowPath);
                filesPath.append("/");
                filesPath.append(file.getName());
                filesPath.append("&");

                //当传入集合存在时将文件放入集合
                if (files != null)
                    files.add(file);
            }
        }
    }

    /*
     * 删除文件夹(如果形参是文件则直接删除)
     * */
    public static boolean deleteDir(File dir) {
        //判断dir是否存在
        if (!dir.exists())
            return false;

        //判断dir是否为文件夹
        if (!dir.isDirectory()) {
            dir.delete();
            return true;
        }

        //递归删除所有文件
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                if (!deleteDir(file))
                    return false;
            } else {
                if (!file.delete())
                    return false;
            }
        }

        //删除文件夹本身并返回结果
        return dir.delete();
    }

    /*
     * 清空文件夹(与删除文件夹不同，它只会清空文件夹里面的内容，而不会删除文件夹)
     * 不可传入文件
     * */
    public static boolean clearedDir(File dir) {
        //判断文件夹是否存在
        if (!dir.exists())
            return false;

        //判断是否为文件夹
        if (!dir.isDirectory())
            return false;

        //清空文件夹
        for (File file : dir.listFiles()) {
            if (!deleteDir(file))
                return false;
        }

        //返回结果
        return true;
    }

    /*
     * 计算文件夹总大小
     * */
    public static long dirLength(DocumentFile dir) {
        //判断文件夹是否存在
        if (!dir.exists())
            return 0L;

        //判断是否为文件夹
        if (!dir.isDirectory())
            return dir.length();

        //递归统计文件夹总大小
        long sum = 0;
        for (DocumentFile file : dir.listFiles()) {
            if (file.isDirectory()) {
                sum += dirLength(file);
            } else {
                sum += file.length();
            }
        }

        //返回总大小
        return sum;
    }

    /*
     * 格式化文件大小单位
     * */
    public static String formatUnit(long size) {
        if (size < 1024L) {
            return size + "B";
        } else if (size < 1048576L) {
            return String.format(Locale.CHINA, "%.2fKB", (double) size / 1024L);
        } else if (size < 1073741824L) {
            return String.format(Locale.CHINA, "%.2fMB", (double) size / 1048576L);
        } else if (size < 1099511627776L) {
            return String.format(Locale.CHINA, "%.2fGB", (double) size / 1073741824L);
        } else {
            return String.format(Locale.CHINA, "%.2fTB", (double) size / 1099511627776L);
        }
    }
}