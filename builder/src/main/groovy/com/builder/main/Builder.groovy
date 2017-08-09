package com.builder.main

import com.android.build.gradle.AppExtension;
import com.builder.BuildConfigPluginExtension;
import com.builder.bean.APKInfo
import com.builder.bean.ChannelInfo
import com.builder.bean.PackageInfo
import com.builder.bean.VersionInfo
import com.builder.utils.Callback
import com.builder.utils.ConfigUtil
import com.builder.utils.FileUtils
import com.builder.utils.ListUtils
import com.builder.utils.MD5Util
import groovy.json.JsonOutput
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.process.ExecSpec

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
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
    def final static String TEMP_DIR_NAME = "temp"
    def final static TASK_NAME = "buildAPK"
    def final static ENCODE_STR = "UTF-8"

    Project project;
    def keyStorePropertiesPath
    def configBuildPath
    def channelListPath
    def sampleListPath
    def sourceAPKPath
    def tempPath
    /** apk最终输出路径,根据打包类型不同，有3类 */
    def outputPathChannel
    def outputPathSample
    def outputPathAll
    /** 版本号 */
    def versionCode
    /** 操作系统类型 */
    def osName
    /** 打包类型：channel->channel.txt ;  sample->sample.txt ; all-> sample.txt+channel.txt */
    def buildType
    def channelList = []
    def sampleList = []
    def totalList = []
    /** 线程数 */
    def threads
    /** 多线程执行结果 */
    List<Future<String>> executorResult
    /** 渠道包信息bean,用Collections.synchronizedList让list线程安全 */
    ArrayList<ChannelInfo> channelInfoList = Collections.synchronizedList(new ArrayList<ChannelInfo>())
    /** 渠道包映射文件，json格式 */
    APKInfo apkInfo = new APKInfo()
    // 防止task在运行结束后销毁所有子线程
    boolean locked = true;


    @Override
    void apply(Project pro) {
        project = pro
        /** 初始化插件 */
        initPlugin();
        /** 读取gradle中参数配置文件 */
        project.extensions.create("buildConfig", BuildConfigPluginExtension)
        /** 创建打包的task */
        project.task(TASK_NAME).doFirst {
            /** 初始化task*/
            initTask()
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
            /** 获取目标打包列表 */
            def targetList = getTargetChannelList()
            /** 渠道号列表根据线程数分组 */
            List<List<String>> multiTreadList = ListUtils.splitList(targetList, threads)
            /** 最终线程数，因为可能出现设置的线程数>实际的渠道号组数，此时线程数应该为渠道号组数 */
            int threadSize = multiTreadList.size()
            generateCacheFileForMultiThread(multiTreadList, sourceAPKPath)
            /** 建立线程池 */
            ExecutorService executorService = Executors.newFixedThreadPool(threadSize)
            for (childList in multiTreadList) {
                if (childList.size() == 0) {
                    // 如果线程某组渠道号列表长度为0，不创建线程任务
                    continue
                }
                /** 打包方法 */
                handleChildList(childList, executorService, executorResult)
            }
            /** 开始检测多线程任务是否完成 */
            packageTimeCount(new Callback() {
                @Override
                void onFinish() {
                    println "当前线程数 ：" + executorResult.size()
                    VersionInfo versionInfo = new VersionInfo()
                    versionInfo.setVersionCode("${versionCode}")
                    versionInfo.setChannelList(channelInfoList)

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
            })
            while (locked) {
                // wait for thread
            }

        }
    }

    /** 插件初始化，用于初始化一些固定参数 */
    void initPlugin() {
        def tempDir = new File(project.buildDir, TEMP_DIR_NAME)
        FileUtils.checkDirExistsIfCreate(tempDir.getAbsolutePath())
        tempPath = tempDir.getAbsolutePath()
    }

    /** 初始化task，检测必要参数 */
    void initTask() {
        // 参数文件位置初始化
        configBuildPath = project.buildConfig.configFilePath
        FileUtils.checkDirExistsIfCreate(configBuildPath)
        // 渠道号channelList文件初始化
        channelListPath = project.buildConfig.channelListPath
        FileUtils.checkFileExistsIfCreate(channelListPath)
        // 渠道号sampleList文件初始化
        sampleListPath = project.buildConfig.sampleListPath
        FileUtils.checkFileExistsIfCreate(sampleListPath)
        // 缓存路径初始化
        tempPath = project.buildConfig.tempPath
        FileUtils.checkDirExistsIfCreate(tempPath)
        // keyStore路径初始化
        keyStorePropertiesPath = project.buildConfig.keyStorePropertiesPath
        FileUtils.checkFileExists(keyStorePropertiesPath)
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
        /** 设置打包线程数 */
        threads = project.buildConfig.threads
        /** 初始化多线程执行结果集合 */
        executorResult = new ArrayList<>()
        println "=============================================="
        println "keyStore配置路径>>>" + keyStorePropertiesPath
        println "源apk路径>>>" + sourceAPKPath
        println "版本号>>>" + versionCode
        println "操作系统版本>>>" + osName
        println "打包类型>>>" + buildType
        println "channelList路径>>>" + channelListPath
        println "sampleList路径>>>" + sampleListPath
        println "outputPathChannel>>>>>>" + outputPathChannel
        println "outputPathSample>>>>>>" + outputPathSample
        println "outputPathAll>>>>>>" + outputPathAll
        println "预设线程数>>>>>>" + threads
        println "=============================================="
    }

    /** 根据打包类型，决定读取那个渠道列表 */
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

    /** 签名apk */
    String signAPK(String unsignedAPKPath, String channelId) {
        //读取签名配置文件
        def properties = ConfigUtil.getPropertiesFile(keyStorePropertiesPath)
        def keyStorePath = properties.getProperty("KEY_STORE").toString()
        def keyStorePassword = properties.getProperty("KEY_STORE_PASSWORD").toString()
        def aliasName = properties.getProperty("KEY_ALIAS").toString()
        def aliasPassword = properties.getProperty("KEY_ALIAS_PASSWORD").toString()
        if (!FileUtils.checkFileExists(keyStorePath)) {
            throw new FileNotFoundException("keyStore not found")
        }
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

    /**  对apk执行zipAlign */
    String execZipAlign(String unAlignedAPK, String channelId) {
        def targetPath
        if (buildType == BuildConfigPluginExtension.BUILT_TYPE_SAMPLE) {
            targetPath = FileUtils.getFileAbsolutePath(outputPathSample)
        } else if (buildType == BuildConfigPluginExtension.BUILT_TYPE_CHANNEL) {
            targetPath = FileUtils.getFileAbsolutePath(outputPathChannel)
        } else {
            targetPath = FileUtils.getFileAbsolutePath(outputPathAll)
        }
        def alignedAPKPath = getOutputAPKPath(targetPath, channelId, "-final")
        project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec execSpec) {
                execSpec.executable(getZipAlignExec())
                execSpec.args("-f")
                execSpec.args("4")
                execSpec.args(unAlignedAPK)
                execSpec.args(alignedAPKPath)
            }
        })
        return alignedAPKPath
    }

    /** 写渠道号到apk */
    String writeChannelId(String apkPath, String targetPath, String channelId) {
        println "source apk path ==== ${apkPath} "
        def apk = new ZipFile(apkPath)
        def appended = new ZipOutputStream(new FileOutputStream(targetPath))
        Enumeration entries = apk.entries()
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement()
            ZipEntry newEntry = new ZipEntry(e.getName())
            appended.putNextEntry(newEntry)
            if (!e.isDirectory()) {
                copy(apk.getInputStream(e), appended)
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

    /** 流拷贝方法 */
    void copy(InputStream input, OutputStream output) throws IOException {
        output << input
    }

    /** 生成最终apk的md5 */
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

    /** 获取android sdk中buildTools中的zipLAign工具路径 */
    File getZipAlignExec() {
        def android = project.extensions.getByType(AppExtension)
        def buildToolsFile = new File("${android.getSdkDirectory()}", "build-tools")
        def versionBuildTools = new File(buildToolsFile, "${android.getBuildToolsVersion()}")
        def zipAlignFile = new File(versionBuildTools, "zipalign")
        if (zipAlignFile.exists()) {
            return zipAlignFile
        }
        return null
    }

    void handleChildList(
            final List<String> childList,
            final ExecutorService service, final List<Future<String>> result) {
        Callable<String> task = new Callable<String>() {
            @Override
            String call() throws Exception {
                def threadAPKPath = threadMap.get(childList.get(0))
                for (channelId in childList) {
                    def sourceAPKWithChannelId = APK_NAME_PREFIX + channelId + APK_NAME_SUFFIX
                    // modify apk with channel id
                    println "start write channel id: " + channelId + ", apk file exists:${FileUtils.checkFileExists(sourceAPKPath)}"
                    def unsignedAPKPath = writeChannelId(threadAPKPath, tempPath + File.separator + sourceAPKWithChannelId, channelId)
                    // sign apk
                    println "start sign apk"
                    def signedAPKPath = signAPK(unsignedAPKPath, channelId)
                    // zipalign apk
                    println "start zipalign apk"
                    def alignedAPKPath = execZipAlign(signedAPKPath, channelId)
                    // calculate apk file md5
                    println "start calculate apk md5"
                    def md5Str = generateApkMD5(alignedAPKPath, channelId)
                    channelInfoList.add(generateChannelInfo(alignedAPKPath, "${versionCode}", channelId, md5Str))
                }
                return "current thread handle size = ${channelInfoList.size()}"
            }
        }
        Future<String> r = service.submit(task);
        result.add(r)
    }

    def threadMap = new HashMap<String, String>()

    void generateCacheFileForMultiThread(List<List<String>> multiThreadChannelList, String sourceAPKPath) {
        int size = multiThreadChannelList.size();
        FileUtils.cleanDir(tempPath)
        for (int i = 0; i < size; i++) {
            if (multiThreadChannelList.get(i).size() > 0) {
                def threadAPKName = "${APK_NAME_PREFIX}${i}${APK_NAME_SUFFIX}"
                def targetPath = tempPath + File.separator + threadAPKName
                FileUtils.copyFile(sourceAPKPath, targetPath)
                threadMap.put(multiThreadChannelList.get(i).get(0), targetPath)
            }
        }
    }

    /** 每2s查询一次线程执行结果，如果有一个线程未执行完成就继续等待 */
    void packageTimeCount(Callback callback) {
        println "开始执行打包线程检测"
        Timer timer = new Timer()
        timer.schedule(new TimerTask() {
            @Override
            void run() {
                boolean finish = false
                for (result in executorResult) {
                    if (!result.isDone()) {
                        break
                    } else {
                        finish = true
                    }
                    if (finish) {
                        callback.onFinish()
                        locked = false
                        timer.cancel()
                    }
                }
            }
        }, 1000, 2000)
    }

}

