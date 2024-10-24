package com.ameana.localtransferslite.Utilities;

import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;

public abstract class Message {
    /*
     * 该目录存储了用户选择的消息内容
     * */
    private Message() {
    }

    //用户选择的文件
    public static DocumentFile file = null;

    //用户选择的文件夹
    public static DocumentFile dir = null;

    //用户输入的消息
    public static String mess = null;

    //用户选择的多个文件
    public static ArrayList<DocumentFile> files = null;
}
