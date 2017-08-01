package com.packagebuilder.main

import com.packagebuilder.utils.ConfigUtil
import com.packagebuilder.utils.FileUtils
import com.packagebuilder.utils.MD5Util
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


public class Builder implements Plugin<Project> {

    def final static OS_LINUX = "Linux"
    def final static OS_MAC = "Mac OS X"

    def final static String BUILD_CONFIG_PATH = "buildConfig.properties"
    def final static String KEYSTORE_FILE_PATH = "app/gradle.properties"
    def final static String APK_NAME_PREFIX = "GomePlus-"
    def final static String APK_NAME_SUFFIX = ".apk"
    def final static String MD5_NAME_SUFFIX = ".md5"

    def channelListPath
    def sourceAPKPath
    def tempPath
    def outputPath
    def versionCode
    def osName

    void init() {
        def properties = ConfigUtil.getPropertiesFile(BUILD_CONFIG_PATH)
        channelListPath = properties.getProperty("CHANNEL_LIST_PATH")
        sourceAPKPath = properties.getProperty("SOURCE_APK_PATH")
        tempPath = properties.getProperty("TEMP_PATH")
        FileUtils.checkDirExists(FileUtils.getFileAbsolutePath(tempPath))
        outputPath = properties.getProperty("OUTPUT_PATH")
        FileUtils.checkDirExists(FileUtils.getFileAbsolutePath(outputPath))
        versionCode = properties.getProperty("VERSION_CODE")
        osName = System.getProperty("os.name")
    }


    @Override
    void apply(Project project) {
        project.task('testTask') << {

            init()

            println channelListPath
            File channelListFile = new File(channelListPath)
            //指定处理流的编码
            channelListFile.eachLine("UTF-8") {
                println System.getProperty("os.name")
                println "versionCode ======" + versionCode
                println "Handle Channel: " + it
                def sourceAPKWithChannelId = APK_NAME_PREFIX + it + APK_NAME_SUFFIX
                // modify apk with channel id
                println "write ChannelId(" + it + ") to apk"
                def unsignedAPKPath = writeChannelId(sourceAPKPath, tempPath + File.separator + sourceAPKWithChannelId, it)
                // sign apk
                println "sign apk: " + unsignedAPKPath
                def signedAPKPath = signAPK(unsignedAPKPath, it)
                // zipalign apk
                println "zipalign apk: " + signedAPKPath
                def alignedAPKPath = zipalignAPK(signedAPKPath, it)
                // calculate apk file md5
                println "calcutating apk: " + alignedAPKPath + " md5: "
                generateApkMD5(alignedAPKPath, it)
            }

        }
    }

    String signAPK(String unsignedAPKPath, String channelId) {
        //读取签名配置文件
        def properties = ConfigUtil.getPropertiesFile(KEYSTORE_FILE_PATH)
        def keyStorePath = properties.getProperty("KEY_STORE").toString()
        def keyStorePassword = properties.getProperty("KEY_STORE_PASSWORD").toString()
        def aliasName = properties.getProperty("KEY_ALIAS").toString()
        def aliasPassword = properties.getProperty("KEY_ALIAS_PASSWORD").toString()
        println "keyStorePath = " + keyStorePath + ";" + "keyStorePassword = " + keyStorePassword + ";" + "aliasName = " + aliasName + ";" + "aliasPassword = " + aliasPassword + ";"
        def signedAPKPath = FileUtils.getFileAbsolutePath(tempPath) + File.separator + APK_NAME_PREFIX + channelId + "-" + "signed" + APK_NAME_SUFFIX
//        FileUtils.checkParentExists(checkParentExists)
        def signCommand = new StringBuffer("jarsigner -keystore ")
        signCommand.append(keyStorePath)
        signCommand.append(" -storepass ")
        signCommand.append(keyStorePassword)
        signCommand.append(" -signedjar ")
        signCommand.append(signedAPKPath)
        signCommand.append(" ")
        signCommand.append(FileUtils.getFileAbsolutePath(unsignedAPKPath))
        signCommand.append(" ")
        signCommand.append(aliasName)
        signCommand.append(" -keypass ")
        signCommand.append(aliasPassword)
        String command = signCommand.toString()
        println "signAPK ==========" + command
        exec(command, "/")
        return signedAPKPath
    }

    String zipalignAPK(String signedAPKPath, String channelId) {
        def workDir = new File("packagebuilder", "exec").getAbsolutePath();
        def zipalignString = getCommand()
        def alignedAPKPath = FileUtils.getFileAbsolutePath(outputPath) + File.separator + versionCode + File.separator + channelId + File.separator + APK_NAME_PREFIX + channelId + "-final" + APK_NAME_SUFFIX
        FileUtils.checkOutputDir(alignedAPKPath)
        zipalignString.append(" -f")
//        zipalignString.append(" -v")
        zipalignString.append(" 4 ")
        zipalignString.append(signedAPKPath)
        zipalignString.append(" ")
        zipalignString.append(FileUtils.getFileAbsolutePath(alignedAPKPath))
        String command = zipalignString.toString()
        println "zipalign ==========" + command
        try {
            exec(command, workDir)
        } catch (Exception e) {
        } finally {
            return alignedAPKPath
        }

    }

    void copy(byte[] buffer, InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    String writeChannelId(String apkPath, String targetPath, String channelId) {
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
        appended.close()
        apk.close()
        return targetPath
    }

    void generateApkMD5(String signedAPKPath, String channelId) {
//        def md5FilePath = outputPath + File.separator + versionCode + File.separator + channelId
        File signedAPK = new File(signedAPKPath)
        println signedAPK.getParentFile().getAbsolutePath()
        File md5File = new File(signedAPK.getParentFile(), APK_NAME_PREFIX + channelId + MD5_NAME_SUFFIX)
        MD5Util.generateApkMD5(signedAPKPath, md5File.getAbsolutePath())
    }


    void exec(String command, String execPath) {
        def wdir = new File(execPath).getAbsoluteFile()
        def env = System.getenv();
        def envlist = []
        env.each() { k, v -> envlist.push("$k=$v") }
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = command.execute(envlist, wdir)
        proc.consumeProcessOutput(sout, serr)
        println proc.text
        println sout
        println serr
    }

    StringBuffer getCommand() {
        def command;
        if (osName.startsWith(OS_LINUX)) {
            command = new StringBuffer("./zipalign-linux")
        } else if (osName.startsWith(OS_MAC)) {
            command = new StringBuffer("./zipalign-mac")
        } else {
            command = new StringBuffer("")
        }
        return command
    }

}

