package com.dr.code.diff.dto;

import com.dr.code.diff.enums.CodeManageTypeEnum;
import lombok.Builder;
import lombok.Data;
import org.eclipse.jgit.diff.DiffEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ProjectName: code-diff-parent
 * @Package: com.dr.code.diff.dto
 * @Description: java类作用描述
 * @Author: duanrui
 * @CreateDate: 2021/4/5 10:10
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
@Data
public class VersionControlDto {

    /**
     *  远程仓库地址
     */
    private String repoUrl;

    /**
     * git原始分支或tag/svn 版本
     */
    private String baseVersion;

    /**
     * git现分支或tag、svn 版本
     */
    private String nowVersion;

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
     * 本地旧文件基础地址
     */
    private String oldLocalBasePath;

    /**
     * 本地新文件基础地址
     */
    private String newLocalBasePath;

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

    private List<DiffEntryDto> diffClasses;

    /**
     * 检索svn的日志时，检索开始的时间
     */
    private Date starDate;

    /**
     * 检索svn的日志时，检索结束的时间
     */
    private Date endDate;
}
