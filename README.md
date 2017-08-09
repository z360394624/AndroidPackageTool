# APK打包工具

## 原理概述
    无解压直接修改apk文件，将渠道号写入apk的assets/channel.txt目录下，然后签名，最后执行zipalign出最终包

## 使用方式：
##### 1.单机使用

- 将打包插件module（builder）导入到本地项目，修改builder/build.gradle文件：    
    注释掉 apply from: project.rootProject.uri('maven.gradle');    
    解开uploadArchives {}注释。
- 执行 ./gradlew uploadArchives命令；
- 在项目主module的gradle文件末尾添加如<font color=red>附录1和附录2</font>配置；
- 根据注释进行配置buildConfig {}内容，然后执行./gradlew buildAPK

#### 2.maven库使用

- 修改根目录的build.gradle文件：
    在dependencies{}下添加classpath 'com.bulder:apkbuilder:apk-builder.1.0.0'
- 修改主module的build.gradle文件,末尾添加<font color=red>附录2</font>配置
- 根据注释进行配置buildConfig {}内容，然后执行./gradlew buildAPK




<font color=red><h3>附录1</h3></font>


    buildscript {
         repositories {
            maven {
                url uri('../repo')
            }
        dependencies {
            classpath 'com.builder.main:builder:1.0.0'
        }
    }    

<font color=red><h3>附录2</h3></font>    

    apply plugin: 'builder'
    buildConfig {
        // 需要打包的版本号
        versionCode 90
        // 打包类型：channel、sample、all对应不同的出包目录
        buildType 'channel'
        // 渠道列表channel的apk输出路径
        outputPathC = "output/channel/"
        // 渠道列表sample的apk输出路径
        outputPathS = "output/sample/"
        // 渠道列表channel + sample的apk输出路径
        outputPathA = "output/all/"
        // 已经打包的apk，未签名，未压缩
        sourceAPKPath "app/build/outputs/apk/app-releaseBatch-unsigned.apk"
        // 签名文件的配置文件
        keyStorePropertiesPath "app/gradle.properties"
        // 渠道列表channel相对路径
        channelListPath = "builderConfig/channelList.txt"
        // 渠道列表sample相对路径
        sampleListPath = "builderConfig/sampleList.txt"
        // 缓存目录
        tempPath = "app/build/temp"
        // 配置线程数
        threads = 4
    }

- <font color=red>buildConfig按照注释进行配置；</font>
- <font color=red>注意修改配置文件中classpath 'com.builder.main:builder:1.0.4'的版本号和插件module版本号一致；</font>
- <font color=red>channelList.txt和sampleList.txt的格式如下：（每行一个渠道号）</font>

    C000    
    C111    
    C222    




