package com.builder.utils;

public class FileUtils {


    static void writeFileStr(String filePath, String content) {
        File targetFile = new File(filePath)
        if (targetFile.exists()) targetFile.delete()
        def printWriter = targetFile.newPrintWriter()
        printWriter.write(content)
        printWriter.flush()
        printWriter.close()
    }

    /** 校验目录是否存在，不存在则创建 */
    static void checkDirExistsIfCreate(String path) {
        File file = new File(path)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    /** 校验文件是否存在，不存在则创建 */
    static void checkFileExistsIfCreate(String path) {
        File file = new File(path)
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
    }

    static boolean checkFileExists(String path) {
        return new File(path).exists()
    }

    static void checkParentExists(String filePath) {
        File file = new File(filePath)
        if (file.getParentFile().exists()) {
            file.getParentFile().mkdirs()
        }
    }

    static String getFileAbsolutePath(String path) {
        return new File(path).getAbsolutePath()
    }

    static void checkOutputDir(String outputFile) {
        File targetFile = new File(outputFile)
        if (targetFile.getParentFile().exists()) {
            targetFile.getParentFile().delete()
        }
        targetFile.getParentFile().mkdirs()
    }

}