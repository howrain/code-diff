package com.dr.code.diff.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @ProjectName: cmdb
 * @Package: com.dr.cmdb.application.config
 * @Description: 自定义参数
 * @Author: duanrui
 * @CreateDate: 2021/3/18 9:49
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
@Data
@Configuration
public class CustomizeConfig {


    /**
     * git账号
     */
    @Value(value = "${git.userName}")
    private String gitUserName;
    /**
     * git密码
     */
    @Value(value = "${git.password}")
    private String gitPassWord;

    /**
     * git下载代码到本地的根目录
     */
    @Value(value = "${git.local.base.dir}")
    private String gitLocalBaseRepoDir;


    /**
     * svn账号
     */
    @Value(value = "${svn.userName}")
    private String svnUserName;
    /**
     * svn密码
     */
    @Value(value = "${svn.password}")
    private String svnPassWord;

    /**
     * git下载代码到本地的根目录
     */
    @Value(value = "${svn.local.base.dir}")
    private String svnLocalBaseRepoDir;

    /**
     * 检索svn的jira任务日志时，从多少天前开始统计
     */
    @Value(value = "${svnjira.daysago}")
    private Integer svnJiraDaysAgo;



}
