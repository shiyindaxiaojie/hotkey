package com.jd.platform.hotkey.dashboard.biz.controller;

import com.jd.platform.hotkey.dashboard.common.base.BaseController;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.Page;
import com.jd.platform.hotkey.dashboard.common.domain.Result;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.AppCfgVo;
import com.jd.platform.hotkey.dashboard.biz.service.AppCfgService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@Controller
@RequestMapping("/appCfg")
public class AppCfgController extends BaseController {


	private String prefix = "admin/appcfg";


	@Resource
	private AppCfgService appCfgService;

	@GetMapping("/view")
	public String view(ModelMap modelMap){
		modelMap.put("title", Constant.APP_CFG_VIEW);
		return prefix + "/list";
	}


	@PostMapping("/list")
	@ResponseBody
	public Page<AppCfgVo> list(PageReq page, String app){
		return appCfgService.pageAppCfgVo(page, app);
	}


	@GetMapping("/edit/{app}")
	public String edit(@PathVariable("app") String app, ModelMap modelMap){
		modelMap.put("appCfg", appCfgService.selectAppCfgVo(app));
		return prefix + "/edit";
	}

	@PostMapping("/save")
	@ResponseBody
	public Result save(AppCfgVo cfg){
		cfg.setModifier(userName());
		appCfgService.saveAppCfgVo(cfg);
		return Result.success();
	}


}

