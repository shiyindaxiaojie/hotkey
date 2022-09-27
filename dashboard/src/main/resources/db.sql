--  !!! 注意设置sql model 否则可能sql报错 ！！！
--  查询你的sql_model参数：select @@global.sql_mode;  发现ONLY_FULL_GROUP_BY 则会导致报错
--  解决方式：set @@global.sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'
--  详情查阅：https://www.cnblogs.com/hjhsblogs/p/11079356.html

DROP TABLE IF EXISTS `hk_change_log`;
CREATE TABLE `hk_change_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '业务key',
  `biz_type` int(11) NOT NULL COMMENT '业务类型：1规则变更；2worker变更',
  `from_str` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '原始值',
  `to_str` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '目标值',
  `app_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '数据所属APP',
  `update_user` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '修改人',
  `create_time` datetime(0) NOT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `uuid` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '防重ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_uuid`(`uuid`) USING BTREE COMMENT '防重索引'
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Compact;



DROP TABLE IF EXISTS `hk_user`;
CREATE TABLE `hk_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `nick_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '昵称',
  `user_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '用户名',
  `pwd` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '密码',
  `phone` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '手机号',
  `role` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '角色：ADMIN-超管，APPADMIN-app管理员，APPUSER-app用户',
  `app_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '所属appName',
  `create_time` datetime(0) NOT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `state` int(11) NOT NULL DEFAULT 1 COMMENT '状态：1可用；0冻结',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_userName`(`user_name`) USING BTREE COMMENT '账号唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Compact;


-- pwd: 123456
INSERT INTO `hk_user` VALUES (2, 'admin', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '1888888', 'ADMIN', '', '2020-07-28 14:01:03', 1);



DROP TABLE IF EXISTS `hk_key_record`;
CREATE TABLE `hk_key_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'key',
  `app_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '所属appName',
  `val` varchar(2048) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'value',
  `duration` int(11) NOT NULL DEFAULT 60 COMMENT '缓存时间',
  `source` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '来源',
  `type` int(11) NOT NULL DEFAULT 1 COMMENT '记录类型：1put；2del; -1unkonw',
  `create_time` datetime(0) NOT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `uuid` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '防重ID',
  `rule` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '''' COMMENT '规则',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_key`(`uuid`) USING BTREE COMMENT '唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Compact;


DROP TABLE IF EXISTS `hk_key_timely`;
CREATE TABLE `hk_key_timely`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'key',
  `val` varchar(2048) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'value',
  `uuid` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '防重ID',
  `app_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '所属appName',
  `duration` int(11) NOT NULL DEFAULT 0 COMMENT '缓存时间',
  `create_time` datetime(0) NOT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_key`(`uuid`) USING BTREE COMMENT '唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Compact;


CREATE TABLE `hk_statistics`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `key_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'keyName',
  `count` int(11) NOT NULL COMMENT '计数',
  `app` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT 'app',
  `days` int(11) NOT NULL COMMENT '天数',
  `hours` bigint(11) NOT NULL COMMENT '小时数',
  `minutes` bigint(11) NOT NULL DEFAULT 0 COMMENT '分钟数',
  `biz_type` int(2) NOT NULL COMMENT '业务类型',
  `rule` varchar(180) NOT NULL COMMENT '所属规则',
  `uuid` varchar(180) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '防重ID',
  `create_time` datetime(0) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_uuid`(`uuid`) USING BTREE COMMENT '防重唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Compact;


CREATE TABLE `hk_rules`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rules` varchar(5000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '规则JSON',
  `app` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '所属APP',
  `update_user` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '修改人',
  `update_time` datetime(0) NOT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_app`(`app`) USING BTREE COMMENT '防重索引'
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Compact;



CREATE TABLE `hk_summary`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `index_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '指标名称',
  `rule` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '规则',
  `app` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT 'app',
  `index_val1` int(11) NOT NULL DEFAULT 0 COMMENT '指标值1',
  `index_val2` int(11) NOT NULL DEFAULT 0 COMMENT '指标值2',
  `index_val3` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '指标值3',
  `days` int(11) NOT NULL DEFAULT 0 COMMENT '天数',
  `hours` int(11) NOT NULL DEFAULT 0 COMMENT '小时数',
  `minutes` bigint(11) NOT NULL DEFAULT 0 COMMENT '分钟数',
  `seconds` bigint(11) NOT NULL DEFAULT 0 COMMENT '秒数',
  `biz_type` tinyint(2) NOT NULL DEFAULT 0 COMMENT '类型',
  `uuid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '防重ID',
  `create_time` datetime(0) NOT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_uuid`(`uuid`) USING BTREE COMMENT '防重索引',
  INDEX `idx_apprule`(`app`, `rule`) USING BTREE COMMENT '查询索引',
  INDEX `ix_ct`(`create_time`) USING BTREE COMMENT '时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '汇总表' ROW_FORMAT = Compact;



/* 请确认以下SQL符合您的变更需求，务必确认无误后再提交执行 */

CREATE TABLE `biz_access_token` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
   `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'token',
   `flag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'flag',
    `CREATED_BY` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '创建人',
    `CREATED_TIME` datetime NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATED_BY` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '修改人',
    `UPDATED_TIME` datetime NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
     PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '咚咚token表' ROW_FORMAT = Compact;



