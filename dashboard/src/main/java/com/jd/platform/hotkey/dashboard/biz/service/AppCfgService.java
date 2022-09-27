package com.jd.platform.hotkey.dashboard.biz.service;

import com.jd.platform.hotkey.dashboard.common.domain.Page;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.AppCfgVo;

/**
 * @ProjectName: hotkey
 * @ClassName: ClearService
 * @Author: liyunfeng31
 * @Date: 2020/8/3 9:51
 */
public interface AppCfgService {

    Page<AppCfgVo> pageAppCfgVo(PageReq page, String app);

    AppCfgVo selectAppCfgVo(String app);

    void saveAppCfgVo(AppCfgVo cfg);

}
