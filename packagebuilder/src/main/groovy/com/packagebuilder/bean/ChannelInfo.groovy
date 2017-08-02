package com.packagebuilder.bean

public class ChannelInfo {

    String channelName

    PackageInfo info

    String getChannelName() {
        return channelName
    }

    void setChannelName(String channelName) {
        this.channelName = channelName
    }

    PackageInfo getInfo() {
        return info
    }

    void setInfo(PackageInfo info) {
        this.info = info
    }
}