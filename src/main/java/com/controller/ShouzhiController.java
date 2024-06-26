package com.controller;


import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.StringUtil;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;

import com.entity.ShouzhiEntity;

import com.service.ShouzhiService;
import com.entity.view.ShouzhiView;
import com.service.YonghuService;
import com.entity.YonghuEntity;
import com.utils.PageUtils;
import com.utils.R;

/**
 * 收支
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/shouzhi")
public class ShouzhiController {
    private static final Logger logger = LoggerFactory.getLogger(ShouzhiController.class);

    @Autowired
    private ShouzhiService shouzhiService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;



    //级联表service
    @Autowired
    private YonghuService yonghuService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isEmpty(role))
            return R.error(511,"权限为空");
        else if("员工".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        params.put("orderBy","id");
        PageUtils page = shouzhiService.queryPage(params);

        //字典表数据转换
        List<ShouzhiView> list =(List<ShouzhiView>)page.getList();
        for(ShouzhiView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ShouzhiEntity shouzhi = shouzhiService.selectById(id);
        if(shouzhi !=null){
            //entity转view
            ShouzhiView view = new ShouzhiView();
            BeanUtils.copyProperties( shouzhi , view );//把实体数据重构到view中

            //级联表
            YonghuEntity yonghu = yonghuService.selectById(shouzhi.getYonghuId());
            if(yonghu != null){
                BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ShouzhiEntity shouzhi, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,shouzhi:{}",this.getClass().getName(),shouzhi.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isEmpty(role))
            return R.error(511,"权限为空");
        else if("员工".equals(role))
            shouzhi.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        Wrapper<ShouzhiEntity> queryWrapper = new EntityWrapper<ShouzhiEntity>()
            .eq("yonghu_id", shouzhi.getYonghuId())
            .eq("shouzhi_name", shouzhi.getShouzhiName())
            .eq("shouzhi_yesno_types", shouzhi.getShouzhiYesnoTypes())
            .eq("shouzhi_leixing_types", shouzhi.getShouzhiLeixingTypes())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShouzhiEntity shouzhiEntity = shouzhiService.selectOne(queryWrapper);
        if(shouzhiEntity==null){
            shouzhi.setInsertTime(new Date());
            shouzhi.setCreateTime(new Date());
            shouzhiService.insert(shouzhi);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ShouzhiEntity shouzhi, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,shouzhi:{}",this.getClass().getName(),shouzhi.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isEmpty(role))
            return R.error(511,"权限为空");
        else if("员工".equals(role))
            shouzhi.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<ShouzhiEntity> queryWrapper = new EntityWrapper<ShouzhiEntity>()
            .notIn("id",shouzhi.getId())
            .andNew()
            .eq("yonghu_id", shouzhi.getYonghuId())
            .eq("shouzhi_name", shouzhi.getShouzhiName())
            .eq("shouzhi_yesno_types", shouzhi.getShouzhiYesnoTypes())
            .eq("shouzhi_leixing_types", shouzhi.getShouzhiLeixingTypes())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShouzhiEntity shouzhiEntity = shouzhiService.selectOne(queryWrapper);
        if(shouzhiEntity==null){
            //  String role = String.valueOf(request.getSession().getAttribute("role"));
            //  if("".equals(role)){
            //      shouzhi.set
            //  }
            shouzhiService.updateById(shouzhi);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        shouzhiService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }






}

