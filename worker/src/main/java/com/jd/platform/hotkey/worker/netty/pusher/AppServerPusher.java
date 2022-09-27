package com.jd.platform.hotkey.worker.netty.pusher;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Queues;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.model.HotKeyMsg;
import com.jd.platform.hotkey.common.model.typeenum.MessageType;
import com.jd.platform.hotkey.worker.model.AppInfo;
import com.jd.platform.hotkey.worker.netty.holder.ClientInfoHolder;
import com.jd.platform.hotkey.worker.tool.AsyncPool;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 推送到各客户端服务器
 *
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
@Component
public class AppServerPusher implements IPusher {
    /**
     * 热key集中营
     */
    private static LinkedBlockingQueue<HotKeyModel> hotKeyStoreQueue = new LinkedBlockingQueue<>();

    /**
     * 给客户端推key信息
     */
    @Override
    public void push(HotKeyModel model) {
        hotKeyStoreQueue.offer(model);
    }

    @Override
    public void remove(HotKeyModel model) {
        push(model);
    }

    /**
     * 和dashboard那边的推送主要区别在于，给app推送每10ms一次，dashboard那边1s一次
     */
    @PostConstruct
    public void batchPushToClient() {
        AsyncPool.asyncDo(() -> {
            while (true) {
                try {
                    List<HotKeyModel> tempModels = new ArrayList<>();
                    //每10ms推送一次
                    Queues.drain(hotKeyStoreQueue, tempModels, 10, 10, TimeUnit.MILLISECONDS);
                    if (CollectionUtil.isEmpty(tempModels)) {
                        continue;
                    }

                    Map<String, List<HotKeyModel>> allAppHotKeyModels = new HashMap<>();

                    //拆分出每个app的热key集合，按app分堆
                    for (HotKeyModel hotKeyModel : tempModels) {
                        List<HotKeyModel> oneAppModels = allAppHotKeyModels.computeIfAbsent(hotKeyModel.getAppName(), (key) -> new ArrayList<>());
                        oneAppModels.add(hotKeyModel);
                    }

                    //遍历所有app，进行推送
                    for (AppInfo appInfo : ClientInfoHolder.apps) {
                        List<HotKeyModel> list = allAppHotKeyModels.get(appInfo.getAppName());
                        if (CollectionUtil.isEmpty(list)) {
                            continue;
                        }

                        HotKeyMsg hotKeyMsg = new HotKeyMsg(MessageType.RESPONSE_NEW_KEY);
                        hotKeyMsg.setHotKeyModels(list);

                        //整个app全部发送
                        appInfo.groupPush(hotKeyMsg);
                    }

                    allAppHotKeyModels = null;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
