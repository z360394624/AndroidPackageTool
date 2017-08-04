package com.builder.bean

public class ChannelInfo {

    String channelName

    com.builder.bean.PackageInfo info

    String getChannelName() {
        return channelName
    }

    void setChannelName(String channelName) {
        this.channelName = channelName
    }

    com.builder.bean.PackageInfo getInfo() {
        return info
    }

    void setInfo(com.builder.bean.PackageInfo info) {
        this.info = info
    }
}