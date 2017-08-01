package com.packagebuilder.utils

public class CommandUtil {

    static void exec(def command, String execPath) {
        def env = System.getenv();
        def envlist = [];
        env.each() { k, v -> envlist.push("$k=$v") }
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = command.execute(envlist, execPath)
        println proc
        println sout
        println serr
    }

}