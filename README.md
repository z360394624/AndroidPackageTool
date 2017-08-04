# AndroidPackageTool

A android package plugin by groovy, Read channelId from channelList.txt, write into assets/channel.txt



当前模式需要讲整个插件module在本地编译，因为插件件模块下的exec目录下的zipalign无法被打包进aar中，后继会采用gradle的task来执行zipalign。
## 使用方式：
    1.将打包插件module（builder）导入到本地项目，在保证编译环境的情况下，执行./gradlew uploadArchives构建插件；

    2.在项目主module的gradle文件末尾添加如附录配置；

    3.在确保配置的文件都不为空的情况下，执行./gradlew buildAPK



<font color=red><h1>附录</h1></font>


    buildscript {
         repositories {
            maven {
    //            url uri('http://10.69.58.111:8081/repository/3rd_part/')
                url uri('../repo')
            }

        dependencies {
            classpath 'com.builder.main:builder:1.0.4'
        }
    }
    apply plugin: 'builder'
    buildConfig {
        versionCode 900
        buildType 'channel'
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
        // 渠道列表channel的apk输出路径
        outputPathC = "output/channel/"
        // 渠道列表sample的apk输出路径
        outputPathS = "output/sample/"
        // 渠道列表channel + sample的apk输出路径
        outputPathA = "output/all/"
    }

- <font color=red>buildConfig按照注释进行配置；</font>
- <font color=red>注意修改配置文件中classpath 'com.builder.main:builder:1.0.4'的版本号和插件module版本号一致；</font>
- <font color=red>暂不支持上传到maven私服，因为zipalign没有打包进插件module的aar中；</font>
- <font color=red>channelList.txt和sampleList.txt的格式如下：（每行一个渠道号）</font>

    C000    
    C111    
    C222    




