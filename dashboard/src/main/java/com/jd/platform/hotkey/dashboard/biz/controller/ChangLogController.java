package com.jd.platform.hotkey.dashboard.biz.controller;

import com.github.pagehelper.PageInfo;
import com.jd.platform.hotkey.dashboard.common.base.BaseController;
import com.jd.platform.hotkey.dashboard.common.domain.*;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.model.ChangeLog;
import com.jd.platform.hotkey.dashboard.biz.service.ChangeLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@Controller
@RequestMapping("/changeLog")
public class ChangLogController extends BaseController {
	
	private String prefix = "admin/changeLog";

	@Resource
	private ChangeLogService logService;


	@GetMapping("/view")
	public String view(ModelMap modelMap){
		modelMap.put("title", Constant.CHANG_LOG_VIEW);
		return prefix + "/list";
	}


	@PostMapping("/list")
	@ResponseBody
	public Page<ChangeLog> list(PageReq page, String searchText){
		PageInfo<ChangeLog> info = logService.pageChangeLog(page, param(searchText));
		return new Page<>(info.getPageNum(),(int)info.getTotal(),info.getList());
	}


	@GetMapping("/edit/{id}")
	public String edit(@PathVariable("id") Integer id, ModelMap modelMap){
		modelMap.put("changeLog", logService.selectByPrimaryKey(id));
		return prefix + "/edit";
	}

}

