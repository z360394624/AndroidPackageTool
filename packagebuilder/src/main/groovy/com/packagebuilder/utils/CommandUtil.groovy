package com.packagebuilder.utils

public class CommandUtil {

    static void exec(String[] command, String execPath) {
        def wdir = new File(execPath).getAbsoluteFile()
        def env = System.getenv();
        def envlist = []
        env.each() { k, v -> envlist.push("$k=$v") }
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = command.execute(envlist, wdir)
        proc.consumeProcessOutput(sout, serr)
        println proc
        println sout
        println serr
    }

}