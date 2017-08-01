package com.packagebuilder.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * md5 generator
 */
public class MD5Util {

    static final
    def HEX_DIGITS = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F']

    static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2)
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4])
            sb.append(HEX_DIGITS[b[i] & 0x0f])
        }
        return sb.toString()
    }

    static String md5sum(String filename) throws IOException, NoSuchAlgorithmException {
        InputStream fis
        def buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5;
        println "7-1"
        fis = new FileInputStream(filename);
        println "7-2"
        md5 = MessageDigest.getInstance("MD5");
        println "7-3"
        while ((numRead = fis.read(buffer)) > 0) {
            md5.update(buffer, 0, numRead);
        }
        println "7-4"
        fis.close();
        return toHexString(md5.digest());
    }

    /** calculate md5 of APK file， save md5 as string to file */
    static void generateApkMD5(String apkPath, String md5FilePath) {
        println "5"
        File md5File = new File(md5FilePath)
        println "6"
        if (md5File.exists()) {
            md5File.delete()
        }
        println "7"
        md5File.createNewFile()
        println "7"
        String md5Str = md5sum(apkPath)
        println "9"
        FileUtils.writeFileStr(md5FilePath, md5Str)
    }
}
