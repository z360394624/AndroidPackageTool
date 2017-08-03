package com.packagebuilder.main

import com.packagebuilder.BuildConfigPluginExtension
import com.packagebuilder.bean.APKInfo
import com.packagebuilder.bean.ChannelInfo
import com.packagebuilder.bean.PackageInfo
import com.packagebuilder.bean.VersionInfo
import com.packagebuilder.utils.ConfigUtil
import com.packagebuilder.utils.FileUtils
import com.packagebuilder.utils.MD5Util
import groovy.json.JsonOutput
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


public class Builder implements Plugin<Project> {

    def final static APK_FILE_NOT_FOUND = "APK文件未找到，打包失败"
    def final static VERSION_CODE_IS_EMPTY = "版本号不能为空"
    def final static BUILD_TYPE_ERROR = "打包类型只能为：" +
            "${BuildConfigPluginExtension.BUILT_TYPE_ALL}、" +
            "${BuildConfigPluginExtension.BUILT_TYPE_SAMPLE}、" +
            "${BuildConfigPluginExtension.BUILT_TYPE_CHANNEL}"

    def final static String APK_NAME_PREFIX = "GomePlus-"
    def final static String APK_NAME_SUFFIX = ".apk"
    def final static String MD5_NAME_SUFFIX = ".md5"
    def final static String MAPPING_FILE_NAME = "gome.txt"
    def final static TASK_NAME = "buildAPK"
    def final static ENCODE_STR = "UTF-8"

    def final static OS_LINUX = "Linux"
    def final static OS_MAC = "Mac OS X"



    def keyStorePropertiesPath
    def configBuildPath
    def channelListPath
    def sampleListPath
    def sourceAPKPath
    def tempPath
    def outputPathChannel
    def outputPathSample
    def outputPathAll
    def versionCode
    def osName
    def channelList = []
    def sampleList = []
    def totalList = []
    def buildType


    APKInfo apkInfo = new APKInfo()

    @Override
    void apply(Project project) {
        /** 初始化插件，在打包前开始执行 */
        initPlugin(project)
        /** 创建打包的task */
        project.task(TASK_NAME) << {
            /** 初始化task*/
            initTask(project)
            /** 加载sampleListList渠道号 */
            File sampleListFile = new File(sampleListPath)
            /** 加载channelList渠道号 */
            File channelListFile = new File(channelListPath)
            sampleListFile.eachLine(ENCODE_STR) {
                sampleList.add(it)
                totalList.add(it)
            }
            channelListFile.eachLine(ENCODE_STR) {
                channelList.add(it)
                totalList.add(it)
            }
            println ""
            /** 获取目标打包列表 */
            def targetList = getTargetChannelList()
            ArrayList channelList = new ArrayList()
            for (channelId in targetList) {
                println "start handle channelId " + channelId + ">>>>>>>>>>>>>>>>>>>>>"
                def sourceAPKWithChannelId = APK_NAME_PREFIX + channelId + APK_NAME_SUFFIX
                // modify apk with channel id
                println "start write channel id"
                def unsignedAPKPath = writeChannelId(sourceAPKPath, tempPath + File.separator + sourceAPKWithChannelId, channelId)
                // sign apk
                println "start sign apk"
                def signedAPKPath = signAPK(unsignedAPKPath, channelId)
                // zipalign apk
                println "start zipalign apk"
                def alignedAPKPath = zipalignAPK(signedAPKPath, channelId)
                // calculate apk file md5
                println "start calculate apk md5"
                def md5Str = generateApkMD5(alignedAPKPath, channelId)

                channelList.add(generateChannelInfo(alignedAPKPath, "${versionCode}", channelId, md5Str))
            }
            VersionInfo versionInfo = new VersionInfo()
            versionInfo.setVersionCode("${versionCode}")
            versionInfo.setChannelList(channelList)

            ArrayList versionList = new ArrayList()
            versionList.add(versionInfo)
            apkInfo.setVersion("${versionCode}")
            apkInfo.setVersionList(versionList)
            def mappingFilePath
            if (buildType == BuildConfigPluginExtension.BUILT_TYPE_SAMPLE) {
                mappingFilePath = outputPathSample + File.separator + MAPPING_FILE_NAME
            } else if (buildType == BuildConfigPluginExtension.BUILT_TYPE_CHANNEL) {
                mappingFilePath = outputPathChannel + File.separator + MAPPING_FILE_NAME
            } else {
                mappingFilePath = outputPathAll + File.separator + MAPPING_FILE_NAME
            }
            println "mappingFilePath >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + mappingFilePath
            def mappingFileContent = new JsonOutput().toJson(apkInfo)
            FileUtils.writeFileStr(mappingFilePath, mappingFileContent)
        }
    }

    /** 初始化插件操作 */
    void initPlugin(Project project) {
        // 读取gradle中参数配置文件
        project.extensions.create("buildConfig", BuildConfigPluginExtension)
        // keyStore路径初始化
        keyStorePropertiesPath = project.buildConfig.keyStorePropertiesPath
        FileUtils.checkDirExists(keyStorePropertiesPath)
        // 参数文件位置初始化
        configBuildPath = project.buildConfig.configFilePath
        FileUtils.checkDirExistsIfCreate(configBuildPath)
        // 缓存路径初始化
        tempPath = project.buildConfig.tempPath
        FileUtils.checkDirExistsIfCreate(tempPath)
        // 渠道号channelList文件初始化
        channelListPath = project.buildConfig.channelListPath
        FileUtils.checkFileExistsIfCreate(channelListPath)
        // 渠道号sampleList文件初始化
        sampleListPath = project.buildConfig.sampleListPath
        FileUtils.checkFileExistsIfCreate(sampleListPath)
        println "=================================================="
        println "configBuildPath>>>>>>" + configBuildPath
        println "configBuildPath>>>>>>" + configBuildPath
        println "tempPath>>>>>>" + tempPath
        println "=================================================="
    }

    /** 初始化task，检测必要参数 */
    void initTask(Project project) {
        // 取版本号
        versionCode = project.buildConfig.versionCode
        /** 检查版本号是否为空 */
        if (versionCode == 0) {
            throw new IllegalArgumentException(VERSION_CODE_IS_EMPTY)
        }
        // channel输出路径初始化
        outputPathChannel = project.buildConfig.outputPathC + versionCode
        FileUtils.checkDirExistsIfCreate(outputPathChannel)
        // sample输出路径初始化
        outputPathSample = project.buildConfig.outputPathS + versionCode
        FileUtils.checkDirExistsIfCreate(outputPathSample)
        // 全量包输出路径
        outputPathAll = project.buildConfig.outputPathA + versionCode
        FileUtils.checkDirExistsIfCreate(outputPathAll)
        /** 检查未签名的apk是否存在 */
        sourceAPKPath = project.buildConfig.sourceAPKPath
        if (!FileUtils.checkFileExists(sourceAPKPath)) {
            throw new FileNotFoundException(APK_FILE_NOT_FOUND)
        }
        /** 读取打包类型，默认为sample */
        buildType = project.buildConfig.buildType
        /** 读取当前操作系统类型 */
        osName = System.getProperty("os.name")
        println "=============================================="
        println "源apk路径>>>" + sourceAPKPath
        println "版本号>>>" + versionCode
        println "操作系统版本>>>" + osName
        println "打包类型>>>" + buildType
        println "channelList路径>>>" + channelListPath
        println "sampleList路径>>>" + sampleListPath
        println "outputPathChannel>>>>>>" + outputPathChannel
        println "outputPathSample>>>>>>" + outputPathSample
        println "outputPathAll>>>>>>" + outputPathAll
        println "=============================================="
    }

    List getTargetChannelList() {
        if (buildType == BuildConfigPluginExtension.BUILT_TYPE_ALL) {
            return totalList
        } else if (buildType == BuildConfigPluginExtension.BUILT_TYPE_CHANNEL) {
            return channelList
        } else if (buildType == BuildConfigPluginExtension.BUILT_TYPE_SAMPLE) {
            return sampleList
        } else {
            throw new IllegalArgumentException(BUILD_TYPE_ERROR)
        }
    }

    String signAPK(String unsignedAPKPath, String channelId) {
        //读取签名配置文件
        def properties = ConfigUtil.getPropertiesFile(keyStorePropertiesPath)
        def keyStorePath = properties.getProperty("KEY_STORE").toString()
        def keyStorePassword = properties.getProperty("KEY_STORE_PASSWORD").toString()
        def aliasName = properties.getProperty("KEY_ALIAS").toString()
        def aliasPassword = properties.getProperty("KEY_ALIAS_PASSWORD").toString()
        println "keyStorePath = " + keyStorePath + ";" + "keyStorePassword = " + keyStorePassword + ";" + "aliasName = " + aliasName + ";" + "aliasPassword = " + aliasPassword + ";"
        def signedAPKPath = FileUtils.getFileAbsolutePath(tempPath) + File.separator + APK_NAME_PREFIX + channelId + "-" + "signed" + APK_NAME_SUFFIX
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
        def targetPath
        if (buildType == BuildConfigPluginExtension.BUILT_TYPE_SAMPLE) {
            targetPath = FileUtils.getFileAbsolutePath(outputPathSample)
        } else if (buildType == BuildConfigPluginExtension.BUILT_TYPE_CHANNEL) {
            targetPath = FileUtils.getFileAbsolutePath(outputPathChannel)
        } else {
            targetPath = FileUtils.getFileAbsolutePath(outputPathAll)
        }
        def workDir = new File("packagebuilder", "exec").getAbsolutePath();
        def zipalignString = getCommand()
        def alignedAPKPath = getOutputAPKPath(targetPath, channelId, "-final")
//        zipalignString.append(" -f")
//        zipalignString.append(" -v")
        zipalignString.append(" 4 ")
        zipalignString.append(signedAPKPath)
        zipalignString.append(" ")
        zipalignString.append(FileUtils.getFileAbsolutePath(alignedAPKPath))
        String command = zipalignString.toString()
        println "zipalign ==========" + command
        exec(command, workDir)
        return alignedAPKPath
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
        }

        ZipEntry e = new ZipEntry("assets/channel.txt")
        appended.putNextEntry(e)
        appended.write(channelId.getBytes())
        appended.closeEntry()
        appended.close()
        apk.close()
        return targetPath
    }

    String generateApkMD5(String signedAPKPath, String channelId) {
        File signedAPK = new File(signedAPKPath)
        File md5File = new File(signedAPK.getParentFile(), APK_NAME_PREFIX + channelId + MD5_NAME_SUFFIX)
        return MD5Util.generateApkMD5(signedAPKPath, md5File.getAbsolutePath())
    }

    /** 执行命令行 */
    void exec(String command, String execPath) {
        def wdir = new File(execPath).getAbsoluteFile()
        def env = System.getenv();
        def envlist = []
        env.each() { k, v -> envlist.push("$k=$v") }
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = command.execute(envlist, wdir)
        proc.waitFor()
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


    String getOutputAPKPath(String pathPrefix, String channelId, String subfix) {
        def targetFile = new File(pathPrefix + File.separator + channelId + File.separator)
        if (!targetFile.exists()) {
            targetFile.mkdirs()
        }
        return targetFile.getAbsolutePath() + File.separator + APK_NAME_PREFIX + channelId + subfix + APK_NAME_SUFFIX
    }

    ChannelInfo generateChannelInfo(String apkPath, String versionCode, String channelId, String md5Str) {
        PackageInfo packageInfo = new PackageInfo()
        packageInfo.setMd5(md5Str)
        packageInfo.setApk("/${versionCode}/${channelId}/${new File(apkPath).getName()}")
        ChannelInfo channelInfo = new ChannelInfo()
        channelInfo.setChannelName(channelId)
        channelInfo.setInfo(packageInfo)
        return channelInfo
    }

}

