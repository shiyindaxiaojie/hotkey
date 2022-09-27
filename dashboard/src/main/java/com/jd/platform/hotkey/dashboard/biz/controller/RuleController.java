package com.jd.platform.hotkey.dashboard.biz.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONTokener;
import com.github.pagehelper.PageInfo;
import com.jd.platform.hotkey.dashboard.common.base.BaseController;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.Page;
import com.jd.platform.hotkey.dashboard.common.domain.Result;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.HitCountVo;
import com.jd.platform.hotkey.dashboard.common.eunm.ResultEnum;
import com.jd.platform.hotkey.dashboard.common.ex.BizException;
import com.jd.platform.hotkey.dashboard.model.Rules;
import com.jd.platform.hotkey.dashboard.biz.service.RuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * @author liyunfeng31
 */
@Controller
@RequestMapping("/rule")
public class RuleController extends BaseController {


	@Resource
	private RuleService ruleService;



	@GetMapping("/viewDetail")
	public String viewDetail(ModelMap modelMap){
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		return "admin/rule/json";
	}

	@PostMapping("/getRule")
	@ResponseBody
	public Rules getRule(String app){
		return ruleService.selectRules(app);
	}



	@PostMapping("/save")
	@ResponseBody
	public Result save(Rules rules){
		checkApp(rules.getApp());
		checkRule(rules.getRules());
		rules.setUpdateUser(userName());
		int b = ruleService.save(rules);
		return b == 0 ? Result.fail():Result.success();
	}



	@PostMapping("/remove")
	@ResponseBody
	public Result remove(String key){
		checkApp(key);
		int b = ruleService.delRule(key, userName());
		return b == 0 ? Result.fail():Result.success();
	}


	@GetMapping("/view")
	public String view(ModelMap modelMap){
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		return "admin/rule/list";
	}

	@PostMapping("/list")
	@ResponseBody
	public Page<Rules> list(PageReq page, String app){
		page.setPageSize(30);
		PageInfo<Rules> info = ruleService.pageKeyRule(page, app);
		return new Page<>(info.getPageNum(),(int)info.getTotal(),info.getList());
	}

	@GetMapping("/edit/{app}")
	public String edit(ModelMap modelMap,@PathVariable("app") String app){
		checkApp(app);
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		modelMap.put("rules", ruleService.selectRules(app));
		return "admin/rule/view";
	}


	@GetMapping("/add")
	public String add(ModelMap modelMap){
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		return "admin/rule/view";
	}


	@PostMapping("/listRules")
	@ResponseBody
	public List<String> rules(){
		return ruleService.listRules(null);
	}


	@GetMapping("/viewHitCount")
	public String viewHitCount(ModelMap modelMap){
		modelMap.put("title", Constant.MONITOR_VIEW);
		return "admin/rule/listhitcount";
	}

	@PostMapping("/listHitCount")
	@ResponseBody
	public Page<HitCountVo> pageRuleHitCount(PageReq page, SearchReq req){
		PageInfo<HitCountVo> info = ruleService.pageRuleHitCount(page, req, ownApp());
		return new Page<>(info.getPageNum(),(int)info.getTotal(),info.getList());
	}


	/**
	 * 校验是否合法
	 * @param rules rules
	 * @return boolean
	 */
	private void checkRule(String rules) {
		if(StrUtil.isBlank(rules)){
			throw new BizException(ResultEnum.PARAM_ERROR.getCode(), "规则不能为空");
		}
		try {
			Object json = new JSONTokener(rules).nextValue();
			if (json instanceof JSONObject) {
				// JSONObject jsonObject = (JSONObject) json;
				throw new BizException(ResultEnum.ILLEGAL_JSON_ARR);
			} else if (json instanceof JSONArray) {
				// JSONArray jsonArray = (JSONArray) json;
			}
		}catch(Exception e){
			throw new BizException(ResultEnum.ILLEGAL_JSON_ARR);
		}
	}


}

