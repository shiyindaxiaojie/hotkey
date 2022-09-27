package com.jd.platform.hotkey.dashboard.common.domain;

import cn.hutool.core.date.SystemClock;

import java.io.Serializable;

/**
 * 报警消息包装类，用于保存事件最准确的时间
 */
public class PushMsgWrapper implements Serializable {

    private String app;

    private Long date;

    /**
     * -1小于最小阈值; 1超过最大阈值
     */
    private Integer count;

    private String msg;

    public PushMsgWrapper(String app, int count) {
        this.app = app;
        this.count = count;
        this.date = SystemClock.now();
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}


