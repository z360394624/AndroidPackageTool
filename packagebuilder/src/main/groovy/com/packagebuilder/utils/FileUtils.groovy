package com.packagebuilder.utils;

public class FileUtils {


    static void writeFileStr(String filePath, String content) {
        File targetFile = new File(filePath)
        if (targetFile.exists()) targetFile.delete()
        def printWriter = targetFile.newPrintWriter()
        printWriter.write(content)
        printWriter.flush()
        printWriter.close()
    }

    static void checkDirExists(String path) {
        File file = new File(path)
        if (file.exists()) {
            file.delete()
        }
        file.mkdirs()
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