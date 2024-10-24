package com.ameana.localtransferslite.Net.UDP;

import com.ameana.localtransferslite.MainActivity;
import com.ameana.localtransferslite.Net.IPLogs;

public class UDPMessageHandling extends Thread {
    //记录接收到的消息
    private String message;

    /*
     * 本类负责处理UDP接收到的数据
     * */
    public UDPMessageHandling(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        //判断数据完整性
        if (!message.matches("&.*&.*&.*&.*&"))
            return;

        //拆解数据包
        String[] dataTemp = message.substring(1).split("&");

        //判断是否为本机IP
        boolean temp = false;
        for (String localIP : IPLogs.get_LOCAL_IP()) {
            if (localIP.equals(dataTemp[0])) {
                temp = true;
                break;
            }
        }
        if (temp)
            return;

        //判断是否要立刻回复信息
        if (dataTemp[3].equals(UDPSend.SEARCH_MODE))
            UDPSend.UDPOneSend(UDPSend.REPLY_MODE);

        //查询这个IP对应的端口号
        String port = IPLogs.get_CONNECTED_IP().get(dataTemp[0]);

        //判断查询结果
        if (port == null) {
            //未搜索到该IP
            //添加这个设备
            IPLogs.add_CONNECTED_IP(dataTemp[0], dataTemp[1]);
            MainActivity.addDevice(dataTemp[2], dataTemp[0]);
            //回复消息
            UDPSend.UDPOneSend(UDPSend.REPLY_MODE);
        } else if (!port.equals(dataTemp[1])) {
            //更新设备信息
            IPLogs.add_CONNECTED_IP(dataTemp[0], dataTemp[1]);
            //回复消息
            UDPSend.UDPOneSend(UDPSend.REPLY_MODE);
        }
    }
}