package com.dr.code.diff.dto;

import com.dr.code.diff.enums.CodeManageTypeEnum;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ProjectName: base-service
 * @Package: com.dr.jenkins.jenkins.dto
 * @Description: 差异代码参数
 * @Author: duanrui
 * @CreateDate: 2020/6/20 21:41
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2020
 */
@Data
@Builder
public class DiffMethodParams {


    /**
     * git 远程仓库地址
     */
    private String repoUrl;

    /**
     * git原始分支或tag
     */
    private String baseVersion = "";

    /**
     * git现分支或tag
     */
    private String nowVersion = "";
    /**
     * git账号
     */
    private String gitUserName;
    /**
     * git密码
     */
    private String gitPassWord;
    /**
     * 专用于svn新分支
     */
    private String svnRepoUrl;
    /**
     * svn账号
     */
    private String svnUserName;
    /**
     * svn密码
     */
    private String svnPassWord;
    /**
     * 版本控制类型
     */
    private CodeManageTypeEnum codeManageTypeEnum;


    /**
     * jira任务号
     */
    private List<String> jiraTasks = new ArrayList<>();
    /**
     * 检索svn的jira任务日志时，从多少天前开始统计
     */
    private Integer svnJiraDaysAgo;

    /**
     * 检索svn的日志时，检索开始的时间
     */
    private Date starDate;

    /**
     * 检索svn的日志时，检索结束的时间
     */
    private Date endDate;
}
