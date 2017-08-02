package com.packagebuilder.bean

public class APKInfo {

    String version

    ArrayList<VersionInfo> versionList

    String getVersion() {
        return version
    }

    void setVersion(String version) {
        this.version = version
    }

    ArrayList<VersionInfo> getVersionList() {
        return versionList
    }

    void setVersionList(ArrayList<VersionInfo> versionList) {
        this.versionList = versionList
    }
}