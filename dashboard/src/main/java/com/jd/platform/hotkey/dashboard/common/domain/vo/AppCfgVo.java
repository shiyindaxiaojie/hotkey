package com.jd.platform.hotkey.dashboard.common.domain.vo;


import com.jd.platform.hotkey.dashboard.common.domain.Constant;

import java.io.Serializable;

/**
 * @ProjectName: hotkey
 * @ClassName: AppCfgVo
 * @Author: liyunfeng31
 * @Date: 2020/9/2 10:29
 */
public class AppCfgVo implements Serializable {

    private String app;

    /**
     * 数据保存时长
     */
    private Integer dataTtl;

    /**
     * 警报周期
     */
    private Integer warnPeriod;

    /**
     * 警报阈值最小值
     */
    private Integer warnMin;

    /**
     * 警报阈值最大值
     */
    private Integer warnMax;

    /**
     * 警报开关 1开启 0关闭
     */
    private Integer warn;

    /**
     * 版本
     */
    private Long version;

    /*
     * 最后修改人
     */
    private String modifier;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Integer getDataTtl() {
        return dataTtl;
    }

    public void setDataTtl(Integer dataTtl) {
        this.dataTtl = dataTtl;
    }

    public Integer getWarnPeriod() {
        return warnPeriod;
    }

    public void setWarnPeriod(Integer warnPeriod) {
        this.warnPeriod = warnPeriod;
    }

    public Integer getWarnMin() {
        return warnMin;
    }

    public void setWarnMin(Integer warnMin) {
        this.warnMin = warnMin;
    }

    public Integer getWarnMax() {
        return warnMax;
    }

    public void setWarnMax(Integer warnMax) {
        this.warnMax = warnMax;
    }

    public Integer getWarn() {
        return warn;
    }

    public void setWarn(Integer warn) {
        this.warn = warn;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public AppCfgVo() {
    }

    public AppCfgVo(String app) {
        this.app = app;
        this.dataTtl = 30;
        this.warnPeriod = 60;
        this.warnMin = Constant.WARN_INIT_MIN;
        this.warnMax = Constant.WARN_INIT_MAX;
        this.version = 0L;
        this.warn = 1;
        this.modifier = "SYSTEM";
    }

}
