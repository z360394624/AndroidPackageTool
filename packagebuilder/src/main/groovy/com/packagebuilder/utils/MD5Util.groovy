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
        fis = new FileInputStream(filename);
        md5 = MessageDigest.getInstance("MD5");
        while ((numRead = fis.read(buffer)) > 0) {
            md5.update(buffer, 0, numRead);
        }
        fis.close();
        return toHexString(md5.digest());
    }

    /** calculate md5 of APK file， save md5 as string to file */
    static String generateApkMD5(String apkPath, String md5FilePath) {
        File md5File = new File(md5FilePath)
        if (md5File.exists()) {
            md5File.delete()
        }
        md5File.createNewFile()
        String md5Str = md5sum(apkPath)
        def printWriter = md5File.newPrintWriter()
        printWriter.write(md5Str)
        printWriter.flush()
        printWriter.close()
        return md5Str
    }
}
