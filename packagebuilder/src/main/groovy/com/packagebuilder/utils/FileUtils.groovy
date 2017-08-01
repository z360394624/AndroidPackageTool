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

}