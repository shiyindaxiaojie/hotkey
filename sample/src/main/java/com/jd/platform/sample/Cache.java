package com.jd.platform.sample;

import org.springframework.stereotype.Component;

/**
 * @author wuweifeng wrote on 2020-02-21
 * @version 1.0
 */
@Component
public class Cache {
//    @Resource
//    private RedisTemplate<String, String> redisTemplate;
//
//
//    public String getFromRedis(String key) {
//        return redisTemplate.opsForValue().get(key);
//    }


//    public String get(String key) {
//        Object object = JdHotKeyStore.getValue(key);
//        //如果已经缓存过了
//        if (object != null) {
//            System.out.println("is hot key");
//            return object.toString();
//        } else {
//            String value = getFromRedis(key);
//            JdHotKeyStore.smartSet(key, value);
//            return value;
//        }
//    }
//
//    public void set(String key, String value) {
//        redisTemplate.opsForValue().set(key, value);
//    }
//
//    public void remove(String key) {
//        JdHotKeyStore.remove(key);
//    }

}
