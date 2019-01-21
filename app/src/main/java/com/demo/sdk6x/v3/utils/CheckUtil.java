package com.demo.sdk6x.v3.utils;

import java.util.List;

public class CheckUtil {


    public static boolean isEmpty(List list){
        if (list==null || list.size()==0){
            return true;
        }
        return false;
    }

    public static boolean isEmpty(String string){
        if (string==null || string.length()==0){
            return true;
        }
        return false;
    }
}
