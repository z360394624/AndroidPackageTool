package com.builder.utils

public class ConfigUtil {

    /** 读取配置文件 */
    static Properties getPropertiesFile(String propertiesPath) {
        def properties = new Properties()
        def propertiesFile = new File(propertiesPath)
        if (!propertiesFile.exists()) {
            throw new FileNotFoundException(propertiesPath + " not found!")
        }
        propertiesFile.withInputStream {
            properties.load it
        }
        return properties
    }


}