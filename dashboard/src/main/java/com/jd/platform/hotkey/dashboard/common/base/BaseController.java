package com.jd.platform.hotkey.dashboard.common.base;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.common.eunm.ResultEnum;
import com.jd.platform.hotkey.dashboard.common.ex.BizException;
import com.jd.platform.hotkey.dashboard.biz.service.UserService;
import com.jd.platform.hotkey.dashboard.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.propertyeditors.CustomDateEditor;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


public class BaseController {

    @Resource
    protected HttpServletRequest request;
    @Resource
    protected UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    public void checkApp(String app){
        if(StrUtil.isBlank(app)){
            throw new BizException(ResultEnum.PARAM_ERROR.getCode(), "所属APP不能为空");
        }
        String authHeader = JwtTokenUtil.getAuthHeader(request);
        assert authHeader != null;
        Claims claims = JwtTokenUtil.claims(authHeader.substring(2));
        String role = claims.get("role",String.class);
        if(!role.equals(Constant.ADMIN)){
            if(!ownApp().equals(app)){
                throw new BizException(ResultEnum.NO_PERMISSION);
            }
        }
    }

    public void checkKey(String key){
        if(StrUtil.isBlank(key)){
            throw new BizException(ResultEnum.PARAM_ERROR.getCode(), "key不能为空");
        }
    }

    public String userName(){
        final String authHeader = JwtTokenUtil.getAuthHeader(request);
        final String token = authHeader.substring(2);
        return JwtTokenUtil.getUsername(token);
    }



    public SearchReq param(String text){
        String authHeader = JwtTokenUtil.getAuthHeader(request);
        SearchReq dto = JSON.parseObject(text, SearchReq.class);
        if(dto == null){ dto = new SearchReq(); }
       // dto.setAppName(JwtTokenUtil.getAppName(authHeader.substring(2)));
        return dto;
    }


    public String ownApp(){
        String authHeader = JwtTokenUtil.getAuthHeader(request);
        assert authHeader != null;
        Claims claims = JwtTokenUtil.claims(authHeader.substring(2));
        return userService.selectByUserName(claims.getSubject()).getAppName();
    }

}
