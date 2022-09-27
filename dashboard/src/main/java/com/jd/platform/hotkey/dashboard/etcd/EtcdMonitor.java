package com.jd.platform.hotkey.dashboard.etcd;

import cn.hutool.core.util.StrUtil;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.IRecord;
import com.jd.platform.hotkey.dashboard.common.monitor.DataHandler;
import com.jd.platform.hotkey.dashboard.biz.mapper.SummaryMapper;
import com.jd.platform.hotkey.dashboard.model.Worker;
import com.jd.platform.hotkey.dashboard.netty.HotKeyReceiver;
import com.jd.platform.hotkey.dashboard.biz.service.WorkerService;
import com.jd.platform.hotkey.dashboard.util.CommonUtil;
import com.jd.platform.hotkey.dashboard.util.RuleUtil;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @ProjectName: hotkey
 * @ClassName: EtcdMonitor
 * @Author: liyunfeng31
 * @Date: 2020/4/18 18:29
 */
@SuppressWarnings("ALL")
@Component
public class EtcdMonitor {

    private static Logger log = LoggerFactory.getLogger(EtcdMonitor.class);

    @Resource
    private IConfigCenter configCenter;

    @Resource
    private WorkerService workerService;

    @Resource
    private SummaryMapper summaryMapper;

    @Resource
    private DataHandler dataHandler;


    private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(16);

    /**
     * 监听新来的热key，该key的产生是来自于手工在控制台添加
     */
    public void watchHandOperationHotKey() {
        threadPoolExecutor.submit(() -> {
            KvClient.WatchIterator watchIterator = configCenter.watchPrefix(ConfigConstant.hotKeyPath);
            while (watchIterator.hasNext()) {
                Event event = event(watchIterator);
                KeyValue kv = event.getKv();
                String v = kv.getValue().toStringUtf8();
                Event.EventType eventType = event.getType();

                String appKey = event.getKv().getKey().toStringUtf8().replace(ConfigConstant.hotKeyPath, "");
                dataHandler.offer(new IRecord() {
                    @Override
                    public String appNameKey() {
                        return appKey;
                    }

                    @Override
                    public String value() {
                        return v;
                    }

                    @Override
                    public int type() {
                        return eventType.getNumber();
                    }

                    @Override
                    public Date createTime() {
                        return new Date();
                    }
                });
            }
        });
    }


    @PostConstruct
    public void startWatch() {
        //拉取rules
        fetchRuleFromEtcd();

        //规则拉取完毕后才能去开始入库
        insertRecords();

        //开始入库
        dealHotKey();

        //监听手工创建的key
        watchHandOperationHotKey();

        //监听rule变化
        watchRule();

//        watchWorkers();

        //观察热key访问次数和总访问次数，并做统计
        watchHitCount();
    }

    /**
     * 每秒去清理一次caffeine
     */
    @Scheduled(fixedRate = 1000)
    public void cleanCaffeine() {
        try {
            HotKeyReceiver.cleanUpCaffeine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertRecords() {
        threadPoolExecutor.submit(() -> {
            dataHandler.insertRecords();
        });
    }


    /**
     * 开始消费各worker发来的热key
     */
    private void dealHotKey() {
        threadPoolExecutor.submit(() -> {
            while (true) {
                try {
                    //获取发来的这个热key，存入本地caffeine，设置过期时间
                    HotKeyModel model = HotKeyReceiver.take();

                    //将该key放入实时热key本地缓存中
                    HotKeyReceiver.writeToLocalCaffeine(model);

                    dataHandler.offer(new IRecord() {
                        @Override
                        public String appNameKey() {
                            return model.getAppName() + "/" + model.getKey();
                        }

                        @Override
                        public String value() {
                            return UUID.randomUUID().toString();
                        }

                        @Override
                        public int type() {
                            return 0;
                        }

                        @Override
                        public Date createTime() {
                            return new Date();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }

    /**
     * 异步监听rule规则变化
     */
    private void watchRule() {
        threadPoolExecutor.submit(() -> {

            KvClient.WatchIterator watchIterator = configCenter.watchPrefix(ConfigConstant.rulePath);
            //如果有新事件，即rule的变更，就重新拉取所有的信息
            while (watchIterator.hasNext()) {
                //这句必须写，next会让他卡住，除非真的有新rule变更
                Event event = event(watchIterator);

                try {
                    log.info("---------watch rule change---------"+event.getType().toString());
                    //全量拉取rule信息
                    fetchRuleFromEtcd();
                } catch (Exception e) {
                    log.error("watch rule err");
                }
            }

        });
    }

    /**
     * 启动后从etcd拉取所有rule
     */
    private void fetchRuleFromEtcd() {
        RuleUtil.init();
        try {
            //从etcd获取rule
            List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.rulePath);

            for (KeyValue keyValue : keyValues) {
                try {
                    log.info(keyValue.getKey() + "---" + keyValue.getValue());
                    String appName = keyValue.getKey().toStringUtf8().replace(ConfigConstant.rulePath, "");
                    if (StrUtil.isEmpty(appName)) {
                        configCenter.delete(keyValue.getKey().toStringUtf8());
                        continue;
                    }
                    String rulesStr = keyValue.getValue().toStringUtf8();
                    RuleUtil.put(appName, FastJsonUtils.toList(rulesStr, KeyRule.class));
                } catch (Exception e) {
                    log.error("rule parse failure");
                }

            }

        } catch (StatusRuntimeException ex) {
            //etcd连不上
            log.error("etcd connected fail. Check the etcd address!!!");
        }

    }

    private void watchWorkers() {
        threadPoolExecutor.submit(() -> {
            KvClient.WatchIterator watchIterator = configCenter.watchPrefix(ConfigConstant.workersPath);
            while (watchIterator.hasNext()) {
                Event event = event(watchIterator);
                KeyValue kv = event.getKv();
                Event.EventType eventType = event.getType();
                String k = kv.getKey().toStringUtf8();
                String v = kv.getValue().toStringUtf8();
                long version = kv.getModRevision();
                String uuid = k + Constant.JOIN + version;
                Worker worker = new Worker(k, v, uuid);
                if (eventType.equals(Event.EventType.PUT)) {
                    workerService.insertWorkerBySys(worker);
                } else if (eventType.equals(Event.EventType.DELETE)) {
                    worker.setState(0);
                    workerService.updateWorker(worker);
                }
            }
        });
    }


    /**
     * 监听热key访问次数和总访问次数
     */
    private void watchHitCount() {
        threadPoolExecutor.submit(() -> {
            KvClient.WatchIterator watchIterator = configCenter.watchPrefix(ConfigConstant.keyHitCountPath);
            while (watchIterator.hasNext()) {
                Event event = event(watchIterator);
                KeyValue kv = event.getKv();
                Event.EventType eventType = event.getType();
                if (Event.EventType.DELETE.equals(eventType)) {
                    continue;
                }
                String k = kv.getKey().toStringUtf8();
                String v = kv.getValue().toStringUtf8();

                try {
                    Map<String, String> map = FastJsonUtils.stringToCollect(v);
                    for (String key : map.keySet()) {
                        int row = summaryMapper.saveOrUpdate(CommonUtil.buildSummary(key, map));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }


    private Event event(KvClient.WatchIterator watchIterator) {
        return watchIterator.next().getEvents().get(0);
    }

    @PreDestroy
    public void close() {
        try {
            threadPoolExecutor.shutdown();
        } catch (Exception e) {
            log.error("EtcdMonitor threadPoolExecutor shutdown exception:{}", e.getMessage(), e);
        }
    }

}
