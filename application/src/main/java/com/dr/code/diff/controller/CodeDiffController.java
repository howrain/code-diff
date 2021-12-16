package com.dr.code.diff.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dr.code.diff.config.CustomizeConfig;
import com.dr.code.diff.dto.ClassInfoResult;
import com.dr.code.diff.dto.DiffMethodParams;
import com.dr.code.diff.enums.CodeManageTypeEnum;
import com.dr.code.diff.service.CodeDiffService;
import com.dr.code.diff.vo.result.CodeDiffResultVO;
import com.dr.common.errorcode.BizCode;
import com.dr.common.exception.BizException;
import com.dr.common.response.ApiResponse;
import com.dr.common.response.UniqueApoResponse;
import com.dr.common.utils.mapper.OrikaMapperUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author rui.duan
 * @version 1.0
 * @className UserController
 * @description 用户管理
 * @date 2019/11/20 6:25 下午
 */
@RestController
@Api(value = "/api/code/diff", tags = "差异代码模块")
@RequestMapping("/api/code/diff")
public class CodeDiffController {

    @Autowired
    private CustomizeConfig customizeConfig;

    @Autowired
    private CodeDiffService codeDiffService;

    @ApiOperation("git获取差异代码")
    @RequestMapping(value = "git/list", method = RequestMethod.GET)
    public UniqueApoResponse<List<CodeDiffResultVO>> getGitList(
            @ApiParam(required = true, name = "gitUrl", value = "git远程仓库地址")
            @RequestParam(value = "gitUrl") String gitUrl,
            @ApiParam(required = true, name = "baseVersion", value = "git原始分支或tag")
            @RequestParam(value = "baseVersion") String baseVersion,
            @ApiParam(required = true, name = "nowVersion", value = "git现分支或tag")
            @RequestParam(value = "nowVersion") String nowVersion,
            @ApiParam(required = false, name = "gitUserName", value = "git账号，不传时默认使用默认的配置")
            @RequestParam(value = "gitUserName") String gitUserName,
            @ApiParam(required = false, name = "gitPassWord", value = "git密码，不传时默认使用默认的配置")
            @RequestParam(value = "gitPassWord") String gitPassWord) {
        if (StringUtils.isBlank(gitUserName)) {
            gitUserName = customizeConfig.getGitUserName();
        }
        if (StringUtils.isBlank(gitPassWord)) {
            gitPassWord = customizeConfig.getGitPassWord();
        }

        DiffMethodParams diffMethodParams = DiffMethodParams.builder()
                .repoUrl(StringUtils.trim(gitUrl))
                .baseVersion(StringUtils.trim(baseVersion))
                .nowVersion(StringUtils.trim(nowVersion))
                .codeManageTypeEnum(CodeManageTypeEnum.GIT)
                .gitUserName(gitUserName)
                .gitPassWord(gitPassWord)
                .build();
        List<ClassInfoResult> diffCodeList = codeDiffService.getDiffCode(diffMethodParams);
        List<CodeDiffResultVO> list = OrikaMapperUtils.mapList(diffCodeList, ClassInfoResult.class, CodeDiffResultVO.class);
        return new UniqueApoResponse<List<CodeDiffResultVO>>().success(list, JSON.toJSONString(list, SerializerFeature.WriteNullListAsEmpty));
    }

    @ApiOperation("svn同分支获取日期区间差异代码")
    @RequestMapping(value = "svn/date/list", method = RequestMethod.GET)
    public UniqueApoResponse<List<CodeDiffResultVO>> getSvnDateList(
            @ApiParam(required = true, name = "svnUrl", value = "svn远程仓库地址,如svn:192.168.0.1:3690/svn")
            @RequestParam(value = "svnUrl") String svnUrl,
            @ApiParam(required = true, name = "startDate", value = "日期类型，格式yyyy-MM-dd，检索svn的jira任务日志时，从那一天开始统计（从当天凌晨0点开始计算）")
            @RequestParam(value = "startDate") @DateTimeFormat(pattern="yyyy-MM-dd") Date startDate,
            @ApiParam(required = true, name = "endDate", value = "日期类型，格式yyyy-MM-dd，检索svn的jira任务日志时，从那一天结束统计（从当天23:59截止计算）")
            @RequestParam(value = "endDate") @DateTimeFormat(pattern="yyyy-MM-dd") Date endDate,
            @ApiParam(required = false, name = "svnUserName", value = "svn账号，不传时默认使用默认的配置")
            @RequestParam(value = "svnUserName") String svnUserName,
            @ApiParam(required = false, name = "svnPassWord", value = "svn密码，不传时默认使用默认的配置")
            @RequestParam(value = "svnPassWord") String svnPassWord) {
        // 校验起始时间不能大于结束时间
        if(startDate.getTime()>endDate.getTime()){
            throw new BizException(BizCode.START_END_DATE_ERROR);
        }

        if (StringUtils.isBlank(svnUserName)) {
            svnUserName = customizeConfig.getSvnUserName();
        }
        if (StringUtils.isBlank(svnPassWord)) {
            svnPassWord = customizeConfig.getSvnPassWord();
        }

        // 修改结束时间的时分秒为23时59分59秒
        Calendar cal = Calendar.getInstance();
        cal.setTime(endDate);
        cal.set(Calendar.HOUR_OF_DAY, 23); //时
        cal.set(Calendar.MINUTE, 59); //分
        cal.set(Calendar.SECOND, 59); //秒
        endDate = cal.getTime();

        DiffMethodParams diffMethodParams = DiffMethodParams.builder()
                .repoUrl(StringUtils.trim(svnUrl))
                .codeManageTypeEnum(CodeManageTypeEnum.SVN_DATE)
                .svnUserName(svnUserName)
                .svnPassWord(svnPassWord)
                .starDate(startDate)
                .endDate(endDate)
                .build();
        List<ClassInfoResult> diffCodeList = codeDiffService.getDiffCode(diffMethodParams);
        List<CodeDiffResultVO> list = OrikaMapperUtils.mapList(diffCodeList, ClassInfoResult.class, CodeDiffResultVO.class);
        return new UniqueApoResponse<List<CodeDiffResultVO>>().success(list, JSON.toJSONString(list, SerializerFeature.WriteNullListAsEmpty));
    }

    @ApiOperation("svn同分支获取Jira任务号差异代码")
    @RequestMapping(value = "svn/jira/list", method = RequestMethod.GET)
    public UniqueApoResponse<List<CodeDiffResultVO>> getSvnJiraList(
            @ApiParam(required = true, name = "svnUrl", value = "svn远程仓库地址,如svn:192.168.0.1:3690/svn")
            @RequestParam(value = "svnUrl") String svnUrl,
            @ApiParam(required = true, name = "jiraTasks", value = "jira任务号，“,”分隔，可以为多个，如：TASK1,TASK2")
            @RequestParam(value = "jiraTasks") String jiraTasks,
            @ApiParam(required = false, name = "dayAgo", defaultValue = "365", value = "检索svn的jira任务日志时，从多少天前开始统计，默认从365天前开始统计")
            @RequestParam(value = "dayAgo") Integer dayAgo,
            @ApiParam(required = false, name = "svnUserName", value = "svn账号，不传时默认使用默认的配置")
            @RequestParam(value = "svnUserName") String svnUserName,
            @ApiParam(required = false, name = "svnPassWord", value = "svn密码，不传时默认使用默认的配置")
            @RequestParam(value = "svnPassWord") String svnPassWord) {

        if (StringUtils.isBlank(svnUserName)) {
            svnUserName = customizeConfig.getSvnUserName();
        }
        if (StringUtils.isBlank(svnPassWord)) {
            svnPassWord = customizeConfig.getSvnPassWord();
        }
        if (dayAgo == null || dayAgo==0) {
            dayAgo = customizeConfig.getSvnJiraDaysAgo();
        }

        DiffMethodParams diffMethodParams = DiffMethodParams.builder()
                .repoUrl(StringUtils.trim(svnUrl))
                .jiraTasks(Arrays.asList(jiraTasks.split(",")))
                .codeManageTypeEnum(CodeManageTypeEnum.SVN_JIRA)
                .svnUserName(svnUserName)
                .svnPassWord(svnPassWord)
                .svnJiraDaysAgo(dayAgo)
                .build();
        List<ClassInfoResult> diffCodeList = codeDiffService.getDiffCode(diffMethodParams);
        List<CodeDiffResultVO> list = OrikaMapperUtils.mapList(diffCodeList, ClassInfoResult.class, CodeDiffResultVO.class);
        return new UniqueApoResponse<List<CodeDiffResultVO>>().success(list, JSON.toJSONString(list, SerializerFeature.WriteNullListAsEmpty));
    }

    @ApiOperation("svn同分支获取差异代码")
    @RequestMapping(value = "svn/list", method = RequestMethod.GET)
    public UniqueApoResponse<List<CodeDiffResultVO>> getSvnList(
            @ApiParam(required = true, name = "svnUrl", value = "svn远程仓库地址,如svn:192.168.0.1:3690/svn")
            @RequestParam(value = "svnUrl") String svnUrl,
            @ApiParam(required = true, name = "baseVersion", value = "svn原始分支,如：1")
            @RequestParam(value = "baseVersion") String baseVersion,
            @ApiParam(required = true, name = "nowVersion", value = "svn现分支，如：2")
            @RequestParam(value = "nowVersion") String nowVersion,
            @ApiParam(required = false, name = "svnUserName", value = "svn账号，不传时默认使用默认的配置")
            @RequestParam(value = "svnUserName") String svnUserName,
            @ApiParam(required = false, name = "svnPassWord", value = "svn密码，不传时默认使用默认的配置")
            @RequestParam(value = "svnPassWord") String svnPassWord) {
        if (StringUtils.isBlank(svnUserName)) {
            svnUserName = customizeConfig.getSvnUserName();
        }
        if (StringUtils.isBlank(svnPassWord)) {
            svnPassWord = customizeConfig.getSvnPassWord();
        }

        DiffMethodParams diffMethodParams = DiffMethodParams.builder()
                .repoUrl(StringUtils.trim(svnUrl))
                .baseVersion(StringUtils.trim(baseVersion))
                .nowVersion(StringUtils.trim(nowVersion))
                .codeManageTypeEnum(CodeManageTypeEnum.SVN)
                .svnUserName(svnUserName)
                .svnPassWord(svnPassWord)
                .build();
        List<ClassInfoResult> diffCodeList = codeDiffService.getDiffCode(diffMethodParams);
        List<CodeDiffResultVO> list = OrikaMapperUtils.mapList(diffCodeList, ClassInfoResult.class, CodeDiffResultVO.class);
        return new UniqueApoResponse<List<CodeDiffResultVO>>().success(list, JSON.toJSONString(list, SerializerFeature.WriteNullListAsEmpty));
    }

    @ApiOperation("svn不同分支获取差异代码")
    @RequestMapping(value = "svn/branch/list", method = RequestMethod.GET)
    public UniqueApoResponse<List<CodeDiffResultVO>> getSvnBranchList(
            @ApiParam(required = true, name = "baseSvnUrl", value = "svn原始分支远程仓库地址,如svn:192.168.0.1:3690/svn/truck")
            @RequestParam(value = "baseSvnUrl") String baseSvnUrl,
            @ApiParam(required = true, name = "nowSvnUrl", value = "svn现分支远程仓库地址,如svn:192.168.0.1:3690/svn/feature")
            @RequestParam(value = "nowSvnUrl") String nowSvnUrl,
            @ApiParam(required = false, name = "svnUserName", value = "svn账号，不传时默认使用默认的配置")
            @RequestParam(value = "svnUserName") String svnUserName,
            @ApiParam(required = false, name = "svnPassWord", value = "svn密码，不传时默认使用默认的配置")
            @RequestParam(value = "svnPassWord") String svnPassWord
    ) {
        if (StringUtils.isBlank(svnUserName)) {
            svnUserName = customizeConfig.getSvnUserName();
        }
        if (StringUtils.isBlank(svnPassWord)) {
            svnPassWord = customizeConfig.getSvnPassWord();
        }

        DiffMethodParams diffMethodParams = DiffMethodParams.builder()
                .repoUrl(baseSvnUrl)
                .svnRepoUrl(nowSvnUrl)
                .codeManageTypeEnum(CodeManageTypeEnum.SVN)
                .svnUserName(svnUserName)
                .svnPassWord(svnPassWord)
                .build();
        List<ClassInfoResult> diffCodeList = codeDiffService.getDiffCode(diffMethodParams);
        List<CodeDiffResultVO> list = OrikaMapperUtils.mapList(diffCodeList, ClassInfoResult.class, CodeDiffResultVO.class);
        return new UniqueApoResponse<List<CodeDiffResultVO>>().success(list, JSON.toJSONString(list, SerializerFeature.WriteNullListAsEmpty));
    }


    @ApiOperation("svn不同分支不同reversion获取差异代码")
    @RequestMapping(value = "svn/branch/reversion/list", method = RequestMethod.GET)
    public UniqueApoResponse<List<CodeDiffResultVO>> getSvnBranchReversionList(
            @ApiParam(required = true, name = "baseSvnUrl", value = "svn原始分支远程仓库地址,如svn:192.168.0.1:3690/svn/truck")
            @RequestParam(value = "baseSvnUrl") String baseSvnUrl,
            @ApiParam(required = true, name = "baseVersion", value = "svn原始分支,如：1")
            @RequestParam(value = "baseVersion") String baseVersion,
            @ApiParam(required = true, name = "nowSvnUrl", value = "svn现分支远程仓库地址,如svn:192.168.0.1:3690/svn/feature")
            @RequestParam(value = "nowSvnUrl") String nowSvnUrl,
            @ApiParam(required = true, name = "nowVersion", value = "svn现分支，如：2")
            @RequestParam(value = "nowVersion") String nowVersion,
            @ApiParam(required = false, name = "svnUserName", value = "svn账号，不传时默认使用默认的配置")
            @RequestParam(value = "svnUserName") String svnUserName,
            @ApiParam(required = false, name = "svnPassWord", value = "svn密码，不传时默认使用默认的配置")
            @RequestParam(value = "svnPassWord") String svnPassWord
    ) {
        if (StringUtils.isBlank(svnUserName)) {
            svnUserName = customizeConfig.getSvnUserName();
        }
        if (StringUtils.isBlank(svnPassWord)) {
            svnPassWord = customizeConfig.getSvnPassWord();
        }

        DiffMethodParams diffMethodParams = DiffMethodParams.builder()
                .repoUrl(baseSvnUrl)
                .svnRepoUrl(nowSvnUrl)
                .baseVersion(baseVersion)
                .nowVersion(nowVersion)
                .codeManageTypeEnum(CodeManageTypeEnum.SVN)
                .svnUserName(svnUserName)
                .svnPassWord(svnPassWord)
                .build();
        List<ClassInfoResult> diffCodeList = codeDiffService.getDiffCode(diffMethodParams);
        List<CodeDiffResultVO> list = OrikaMapperUtils.mapList(diffCodeList, ClassInfoResult.class, CodeDiffResultVO.class);
        return new UniqueApoResponse<List<CodeDiffResultVO>>().success(list, JSON.toJSONString(list, SerializerFeature.WriteNullListAsEmpty));
    }


}
