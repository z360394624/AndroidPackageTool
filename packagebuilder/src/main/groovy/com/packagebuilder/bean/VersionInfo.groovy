package com.packagebuilder.bean

public class VersionInfo {

    String versionCode

    ArrayList<ChannelInfo> channelList

    String getVersionCode() {
        return versionCode
    }

    void setVersionCode(String versionCode) {
        this.versionCode = versionCode
    }

    ArrayList<ChannelInfo> getChannelList() {
        return channelList
    }

    void setChannelList(ArrayList<ChannelInfo> channelList) {
        this.channelList = channelList
    }
}