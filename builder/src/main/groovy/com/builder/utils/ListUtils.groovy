package com.builder.utils;

/**
 * Created by luciuszhang on 2017/8/7.
 */

public class ListUtils {

    static List<List<String>> splitList(List<String> sourceList, int num) {
        // list分组后的结果
        List<List<String>> result = new ArrayList<>()
        List<String> childList
        // 熵
        int entropy = (sourceList.size()) / num
        // 余数
        int remainder = (sourceList.size()) % num
        int j = 0
        int tmp = 0
        for (int i = 0; i < num; i++) {
            childList = new ArrayList<>();
            for (j = tmp; j < (entropy + tmp); j++) {
                childList.add(sourceList.get(j))
            }
            tmp = j
            result.add(childList)
        }
        if (remainder != 0) {
            for (int i = 0; i < remainder; i++) {
                result.get(i).add(sourceList.get(sourceList.size() - i - 1))
            }
        }
        return result
    }


    static void main(String[] args) {
        ArrayList<String> testData = ["1","2","3","4","5","6","7","8","9","10","11","12"]
        List<List<String>> result = ListUtils.splitList(testData, 2)
        for (childList in result) {
            println "split childList ============= " + childList
        }

    }
}
