package com.packagebuilder


public class BuildConfigPluginExtension {

    def static final BUILT_TYPE_ALL = "all"
    def static final BUILT_TYPE_CHANNEL = "channel"
    def static final BUILT_TYPE_SAMPLE = "sample"

    def sourceAPKPath = "app/build/outputs/apk/app-releaseBatch-unsigned.apk"
    def versionCode = 0
    def buildType = BUILT_TYPE_SAMPLE

    def keyStorePropertiesPath = "app/gradle.properties"
    def configFilePath = "builderConfig"
    def channelListPath = "builderConfig/channelList.txt"
    def sampleListPath = "builderConfig/sampleList.txt"
    def tempPath = "app/build/temp"
    def outputPathC = "output/channel/"
    def outputPathS = "output/sample/"
    def outputPathA = "output/all/"



}