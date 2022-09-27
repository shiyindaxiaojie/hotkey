package com.jd.platform.hotkey.dashboard.common.monitor;


import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Queues;
import com.ibm.etcd.api.KeyValue;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.dashboard.biz.mapper.KeyRecordMapper;
import com.jd.platform.hotkey.dashboard.biz.mapper.StatisticsMapper;
import com.jd.platform.hotkey.dashboard.biz.mapper.SummaryMapper;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.IRecord;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.AppCfgVo;
import com.jd.platform.hotkey.dashboard.model.KeyRecord;
import com.jd.platform.hotkey.dashboard.model.Statistics;
import com.jd.platform.hotkey.dashboard.netty.HotKeyReceiver;
import com.jd.platform.hotkey.dashboard.util.DateUtil;
import com.jd.platform.hotkey.dashboard.util.RuleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Component
public class DataHandler {

    private static Logger log = LoggerFactory.getLogger(DataHandler.class);

    @Resource
    private KeyRecordMapper keyRecordMapper;

    @Resource
    private StatisticsMapper statisticsMapper;

    @Resource
    private SummaryMapper summaryMapper;

    @Resource
    private IConfigCenter configCenter;

    /**
     * 队列
     */
    private BlockingQueue<IRecord> queue = new LinkedBlockingQueue<>();

    /**
     * 入队
     */
    public void offer(IRecord record) {
        try {
            queue.put(record);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void insertRecords() {
        while (true) {
            try {
                List<IRecord> records = new ArrayList<>();
                Queues.drain(queue, records, 1000, 1, TimeUnit.SECONDS);
                if (CollectionUtil.isEmpty(records)) {
                    continue;
                }
                List<KeyRecord> keyRecordList = new ArrayList<>(records.size());
                for (IRecord iRecord : records) {
                    KeyRecord keyRecord = handHotKey(iRecord);
                    if (keyRecord != null) {
                        keyRecordList.add(keyRecord);
                    }
                }
                if(CollectionUtil.isEmpty(keyRecordList)){
                    continue;
                }
                keyRecordMapper.batchInsert(keyRecordList);

            } catch (Exception e) {
                log.error("batch insert error:{}", e.getMessage(), e);
//                e.printStackTrace();
            }
        }

    }

    /**
     * 读取netty发来的热key，进行初步处理，并写入本地caffeine
      */
    public void dealHotKey() {
        while (true) {
            try {
                HotKeyModel model = HotKeyReceiver.take();
                //将该key放入实时热key本地缓存中
                if (model != null) {
                    //将key放到队列里，供入库时分批调用
                    putRecord(model.getAppName(), model.getKey(), model.getCreateTime());
                    //获取发来的这个热key，存入本地caffeine，设置过期时间
                    HotKeyReceiver.writeToLocalCaffeine(model);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将待入库的热key放到队列
     */
    private void putRecord(String app, String key, long createTime) {
        try {
            queue.put(new IRecord() {
                @Override
                public String appNameKey() {
                    return app + "/" + key;
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
                    return new Date(createTime);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 处理热点key和记录
     */
    private KeyRecord handHotKey(IRecord record) {
        Date date = record.createTime();
        String appKey = record.appNameKey();
        String value = record.value();
        //appName+"/"+"key"
        String[] arr = appKey.split("/");
        String appName = arr[0];
        String key = arr[1];
        String uuid = UUID.randomUUID().toString();
        int type = record.type();

        //组建成对象，供累计后批量插入、删除
        if (type == 0) {
            //如果是客户端删除时发出的put指令
            if (com.jd.platform.hotkey.common.tool.Constant.DEFAULT_DELETE_VALUE.equals(value)) {
                log.info("client remove key event : " + appKey);
                return null;
            }
            //手工添加的是时间戳13位，worker传过来的是uuid
            String source = value.length() == 13 ? Constant.HAND : Constant.SYSTEM;
            String rule = "";
            KeyRule keyRule = RuleUtil.findByKey(appKey);
            int duration = 0;
            if(keyRule != null){
                rule = RuleUtil.ruleNameKey(appName, keyRule.getKey());
                duration = keyRule.getDuration();
            }
            KeyRecord keyRecord = new KeyRecord(key, rule, appName, duration, source, type, uuid, date);
            keyRecord.setRule(rule);
            return keyRecord;
        } else {
            //是删除
            return null;
        }
    }


    /**
     * 每小时 统计一次record 表 结果记录到统计表
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void offlineStatistics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            Date nowTime = DateUtil.ldtToDate(now);
            int day = DateUtil.nowDay(now);
            int hour = DateUtil.nowHour(now);
            SearchReq preHour = new SearchReq(now.minusHours(1));
            List<Statistics> records = keyRecordMapper.maxHotKey(preHour);
            if (records.size() != 0) {
                records.forEach(x -> {
                    x.setBizType(1);
                    x.setCreateTime(nowTime);
                    x.setDays(day);
                    x.setHours(hour);
                    x.setUuid(1 + "_" + x.getKeyName() + "_" + hour);
                });
            }
            log.info("每小时统计最热点,时间：{}, 行数：{}", now.toString(), records.size());
            List<Statistics> statistics = keyRecordMapper.statisticsByRule(preHour);
            if (statistics.size() != 0) {
                statistics.forEach(x -> {
                    x.setBizType(6);
                    x.setRule(x.getRule());
                    x.setCreateTime(nowTime);
                    x.setDays(day);
                    x.setHours(hour);
                    x.setUuid(6 + "_" + x.getKeyName() + "_" + hour);
                });
                log.info("每小时统计规则,时间：{}, data list：{}", now.toString(), JSON.toJSONString(statistics));
                records.addAll(statistics);
            }
            if (records.size() > 0) {
                int row = statisticsMapper.batchInsert(records);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 每分钟统计一次record 表 结果记录到统计表
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void offlineStatisticsRule() {
        try {
            LocalDateTime now = LocalDateTime.now();
            Date nowTime = DateUtil.ldtToDate(now);
            int day = DateUtil.nowDay(now);
            int hour = DateUtil.nowHour(now);
            long minus = DateUtil.nowMinus(now);

            List<Statistics> records = keyRecordMapper.statisticsByRule(new SearchReq(now.minusMinutes(1)));
            if (records.size() == 0) {
                return;
            }
            records.forEach(x -> {
                x.setBizType(5);
                x.setRule(x.getRule());
                x.setCreateTime(nowTime);
                x.setDays(day);
                x.setHours(hour);
                x.setMinutes(minus);
                x.setUuid(5 + "_" + x.getKeyName() + "_" + minus);
            });
            int row = statisticsMapper.batchInsert(records);
//            log.info("每分钟统计规则，时间：{}, 影响行数：{}, data list:{}", now.toString(), row, JSON.toJSONString(records));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 每天根据app的配置清理过期数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void clearExpireData() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.clearCfgPath);
            for (KeyValue kv : keyValues) {
                String key = kv.getKey().toStringUtf8();
                String ttl = kv.getValue().toStringUtf8();
                String app = key.replace(ConfigConstant.clearCfgPath, "");
                Date expireDate = DateUtil.ldtToDate(now.minusDays(Integer.parseInt(ttl)));
                summaryMapper.clearExpireData(app, expireDate);
                keyRecordMapper.clearExpireData(app, expireDate);
                statisticsMapper.clearExpireData(app, expireDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 每10秒检测一次热点记录 用于监控报警
     */
    //TODO WANG
//    @PostConstruct
    public void scanRecordForMonitor() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        //开启拉取etcd的worker信息，如果拉取失败，则定时继续拉取
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                SearchReq req = new SearchReq();
                Date date = new Date();
                req.setEndTime(date);
                List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.appCfgPath);
                for (KeyValue kv : keyValues) {
                    queryRecordAndCheck(kv, req, date);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 2, 10, TimeUnit.SECONDS);

    }

    /**
     * 查询条数 比对配置 发送报警
     */
    private void queryRecordAndCheck(KeyValue kv, SearchReq req, Date date) {
        String val = kv.getValue().toStringUtf8();
        AppCfgVo cfg = JSON.parseObject(val, AppCfgVo.class);
        if (cfg.getWarn() != 1) {
            return;
        }

        req.setApp(cfg.getApp());
        req.setStartTime(new Date(date.getTime() - cfg.getWarnPeriod() * 1000));

        int count = keyRecordMapper.countKeyRecord(req);
        //抽样2%打印
        if (count > 0 && Math.abs(new Random().nextInt()) % 50 == 0) {
            log.info("应用app:{}, 记录count:{}, 统计时间Period:{}", cfg.getApp(), count, cfg.getWarnPeriod());
        }
        int type = 0;
        if (count >= cfg.getWarnMax()) {
            type = 1;
        } else if (count <= cfg.getWarnMin()) {
            type = 2;
        }

        if (type > 0) {
            String str = type == 1 ? "高于最大" : "低于最小";
            int threshold = type == 1 ? cfg.getWarnMax() : cfg.getWarnMin();
            String time = LocalDateTime.now().toString().replace("T", " ");
            String content = String.format("【警报】 应用：【%s】  热点记录在%d秒内累计: %d, %s阈值: %d \n【时间】：%s", cfg.getApp(), cfg.getWarnPeriod(), count, str, threshold, time);
            //TODO WANG
            System.out.println(content);
//            pushHandler.pushMsg(cfg.getApp(), date, content);
        }
    }


}
