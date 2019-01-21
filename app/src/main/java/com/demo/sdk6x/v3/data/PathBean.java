package com.demo.sdk6x.v3.data;

import com.demo.sdk6x.v3.constants.Constants;

public class PathBean {
    public int resType= Constants.Resource.TYPE_UNKNOWN;
    public String pId="";

    public PathBean() {
    }

    public PathBean(int resType, String pId) {
        this.resType = resType;
        this.pId = pId;
    }
}
