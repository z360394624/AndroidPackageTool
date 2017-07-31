package com.packagebuilder.main;

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


public class Builder implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('testTask') << {
            println "Hello gradle plugin"
            def sourceApk = "app/build/outputs/apk/app-release-unsigned.apk"
            def signedApkOutputPath = "app/build/outputs/apk"

            def apkSource = "../../app/build/outputs/apk/app-release-unsigned"
            def apkOutput = "../../app/build/outputs/apk"
            def signOutpu = "app/build/outputs/apk"


            // 解压apk
//            unzip(apkSource + ".apk", apkSource);

            File channelList = new File("channelList.txt")
            //指定处理流的编码
            channelList.eachLine("UTF-8") {
                println it
                def unsignedApkFileName = "GomePlus-" + it + ".apk"
                def signedApkFileName = "GomePlus-" + it + "-signed.apk"
                println unsignedApkFileName
                // 修改apk写入渠道号

                writeChannelId(sourceApk, signedApkOutputPath + "/" + unsignedApkFileName, it)
                // 写入渠道号
//                loadChannel(apkSource, it)
                // 压缩apk
//                zip(apkSource, apkOutput + "/" + unsignedApkFileName)
                // 应用签名
                sign(apkOutput + File.separator + signedApkFileName, apkOutput + File.separator + unsignedApkFileName);
            }
        }
    }

    void unzip(String apkPath, String destPath) {
        println "start unzip"
        def destPathFile = new File(destPath)
        if (!destPathFile.exists()) {
            destPathFile.mkdirs()
        }
        def wdir = new File("packagebuilder", "exec").getAbsoluteFile();
        def env = System.getenv();
        def envlist = [];
        env.each() { k, v -> envlist.push("$k=$v") }
        def unzipCommand = "./apktool d -f " + apkPath + " -o " + destPath
        unzipCommand = ['sh', '-c', unzipCommand]
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = unzipCommand.execute(envlist, wdir)
        proc.consumeProcessOutput(sout, serr)
        proc.waitFor()
        println "out> $sout err> $serr"
        println "unzip end"
    }

    void zip(String srcPath, String destFile) {
        println "start zip"
        def wdir = new File("packagebuilder", "exec").getAbsoluteFile();
        def env = System.getenv();
        def envlist = [];
        env.each() { k, v -> envlist.push("$k=$v") }
        def zipCommand = "./apktool b " + srcPath + " -o " + destFile
        zipCommand = ['sh', '-c', zipCommand]
        println zipCommand
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = zipCommand.execute(envlist, wdir)
        proc.consumeProcessOutput(sout, serr)
        proc.waitFor()
        println "out> $sout err> $serr"
        println "zip end"
    }

    // 写入渠道号
    void loadChannel(String filePath, String channel) {
        filePath = "app/build/outputs/apk/app-release-unsigned"
        println filePath + ":" + channel
        File channelFile = new File(filePath + File.separator + "assets" + File.separator + "channel.txt");
        if (channelFile.exists()) channelFile.delete()
        def printWriter = channelFile.newPrintWriter()
        printWriter.write(channel)
        printWriter.flush()
        printWriter.close()
    }

    void sign(String signedApkName, String unsignedApkName) {
        //读取签名配置文件
        def properties = new Properties()
        new File("app/gradle.properties").withInputStream {
            properties.load it
        }
        def keyStorePath = properties.getProperty("KEY_STORE").toString()
        def keyStorePassword = properties.getProperty("KEY_STORE_PASSWORD").toString()
        def aliasName = properties.getProperty("KEY_ALIAS").toString()
        def aliasPassword = properties.getProperty("KEY_ALIAS_PASSWORD").toString()
        println "keyStorePath = " + keyStorePath + ";" + "keyStorePassword = " + keyStorePassword + ";" + "aliasName = " + aliasName + ";" + "aliasPassword = " + aliasPassword + ";"
        try {
            def wdir = new File("packagebuilder", "exec").getAbsoluteFile();
            def env = System.getenv();
            def envlist = [];
            env.each() { k, v -> envlist.push("$k=$v") }
            def signAPK = "jarsigner -verbose -keystore " + keyStorePath + " -storepass " + keyStorePassword + " -signedjar " + signedApkName + " " + unsignedApkName + " " + aliasName  + " -keypass " + aliasPassword
            signAPK = ['sh','-c',signAPK]
            println signAPK
            def sout = new StringBuilder(), serr = new StringBuilder()
            def proc = signAPK.execute(envlist, wdir)
            println signAPK

            def alignAPK = "packagebuilder/exec/zipalign -c -v 4 " + signedApkName + " align-" + signedApkName
            println alignAPK
            println alignAPK.execute().text
        } catch (Exception e) {
            e.printStackTrace()
        }

    }

    void copy(byte[] buffer, InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(buffer))!= -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    void writeChannelId(String apkPath, String targetPath, String channelId) {
        println apkPath

        println targetPath

        def BUFFER = new byte[4096]

        def apk = new ZipFile(apkPath)
        def appended = new ZipOutputStream(new FileOutputStream(targetPath))
        Enumeration entries = apk.entries()
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement()
            ZipEntry newEntry = new ZipEntry(e.getName())
            appended.putNextEntry(newEntry)
            if (!e.isDirectory()) {
                copy(BUFFER, apk.getInputStream(e), appended)
            }
            appended.closeEntry()
        }
        ZipEntry e = new ZipEntry("assets/channel.txt")
        appended.putNextEntry(e)
        appended.write(channelId.getBytes())
        appended.closeEntry()
        apk.close()
        appended.close()
    }
}

