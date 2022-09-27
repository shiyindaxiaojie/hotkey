package com.jd.platform.hotkey.dashboard.biz.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.util.StringUtil;
import com.google.protobuf.ByteString;
import com.ibm.etcd.api.KeyValue;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.dashboard.biz.mapper.UserMapper;
import com.jd.platform.hotkey.dashboard.common.domain.Page;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.AppCfgVo;
import com.jd.platform.hotkey.dashboard.biz.service.AppCfgService;
import com.jd.platform.hotkey.dashboard.util.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ProjectName: hotkey
 * @ClassName: AppCfgServiceImpl
 * @Author: liyunfeng31
 * @Date: 2020/9/2 9:57
 */
@Service
public class AppCfgServiceImpl implements AppCfgService {


    @Resource
    private IConfigCenter configCenter;

    @Resource
    private UserMapper userMapper;


    @Override
    public Page<AppCfgVo> pageAppCfgVo(PageReq page, String app) {

        List<String> apps = userMapper.listApp();

        List<KeyValue> keyValues = getKVList(apps);

        List<AppCfgVo> cfgVos = new ArrayList<>();
        for (KeyValue kv : keyValues) {
            String key = kv.getKey().toStringUtf8();
            String k = key.replace(ConfigConstant.appCfgPath, "");
            //如果某个配置所属的app已经被删了，则删除该app的配置
            if (!apps.contains(k)) {
                configCenter.delete(key);
                continue;
            }
            //取到配置信息
            String v = kv.getValue().toStringUtf8();
            //如果为空，则赋值初始化
            if (StringUtil.isEmpty(v)) {
                v = JSON.toJSONString(new AppCfgVo(k));
                configCenter.put(key, v);
            }

            AppCfgVo vo = JSON.parseObject(v, AppCfgVo.class);
            vo.setVersion(kv.getModRevision());
            //如果是管理员，则添加所有
            if (StringUtils.isEmpty(app)) {
                cfgVos.add(vo);
            } else {
                //添加自己的app信息
                if (k.equals(app)) {
                    cfgVos.add(vo);
                }
            }
        }
        return PageUtil.pagination(cfgVos, page.getPageSize(), page.getPageNum() - 1);
    }

    @Override
    public AppCfgVo selectAppCfgVo(String app) {
        KeyValue kv = configCenter.getKv(ConfigConstant.appCfgPath + app);
        if (kv == null || kv.getValue() == null) {
            AppCfgVo ap = new AppCfgVo(app);
            configCenter.put(ConfigConstant.appCfgPath + app, JSON.toJSONString(ap));
            return ap;
        }
        String v = kv.getValue().toStringUtf8();
        AppCfgVo cfg = JSON.parseObject(v, AppCfgVo.class);
        cfg.setVersion(kv.getModRevision());
        return cfg;
    }

    /**
     * todo 多节点问题 待完善
     */
    @Override
    public void saveAppCfgVo(AppCfgVo cfg) {
        configCenter.put(ConfigConstant.appCfgPath + cfg.getApp(), JSON.toJSONString(cfg));
    }


    /**
     * 比较和补充
     * @param apps  all db apps
     */
    private List<KeyValue> getKVList(List<String> apps){
        
        List<KeyValue> keyValues = new ArrayList<>(configCenter.getPrefix(ConfigConstant.appCfgPath));
        List<String> etcdApps = keyValues.stream()
                .map(x -> x.getKey().toStringUtf8()
                .replace(ConfigConstant.appCfgPath, ""))
                .collect(Collectors.toList());
        List<String> tempList = new ArrayList<>(apps);
        tempList.removeAll(etcdApps);
        for (String diffApp : tempList) {
            KeyValue kv = KeyValue.newBuilder().setKey(ByteString.copyFromUtf8(ConfigConstant.appCfgPath + diffApp)).build();
            keyValues.add(kv);
        }
        return keyValues;
    }
}
