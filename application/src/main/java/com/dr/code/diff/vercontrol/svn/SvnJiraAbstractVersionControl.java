package com.dr.code.diff.vercontrol.svn;

import com.dr.code.diff.config.CustomizeConfig;
import com.dr.code.diff.dto.ClassInfoResult;
import com.dr.code.diff.dto.DiffEntryDto;
import com.dr.code.diff.dto.LogEntryDto;
import com.dr.code.diff.dto.VersionControlDto;
import com.dr.code.diff.enums.CodeManageTypeEnum;
import com.dr.code.diff.util.SvnRepoUtil;
import com.dr.code.diff.vercontrol.AbstractVersionControl;
import com.dr.common.errorcode.BaseCode;
import com.dr.common.errorcode.BizCode;
import com.dr.common.exception.BizException;
import com.dr.common.log.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @ProjectName: code-diff-parent
 * @Package: com.dr.code.diff.vercontrol
 * @Description: svn jira差异代码获取
 * @Author: duanrui
 * @CreateDate: 2021/4/5 9:56
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
@Slf4j
@Component
public class SvnJiraAbstractVersionControl extends AbstractVersionControl {

    @Autowired
    private CustomizeConfig customizeConfig;


    /**
     * 获取操作类型
     */
    @Override
    public CodeManageTypeEnum getType() {
        return CodeManageTypeEnum.SVN_JIRA;
    }


    @Override
    public List<VersionControlDto> getDiffCodeClasses(VersionControlDto versionControlDto) {
        List<VersionControlDto> versionControlDtoList = new ArrayList<>();
        String svnUserName = versionControlDto.getSvnUserName();
        String svnPassWord = versionControlDto.getSvnPassWord();
        Integer svnJiraDaysAgo = versionControlDto.getSvnJiraDaysAgo();
        try {
            MySVNLogHandler.list.clear();
            MySVNDiffStatusHandler.list.clear();
            String nowSvnUrl = versionControlDto.getRepoUrl();
            if (StringUtils.isNotBlank(versionControlDto.getSvnRepoUrl())) {
                nowSvnUrl = versionControlDto.getSvnRepoUrl();
            }


            SVNURL svnurl = SVNURL.parseURIEncoded(versionControlDto.getRepoUrl());
            SVNLogClient svnLogClient = SvnRepoUtil.getSvnLogClient(svnUserName, svnPassWord);
            SVNRepository svnRepository = SvnRepoUtil.getSvnRepository(svnUserName, svnPassWord, svnurl);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
//            Integer svnJiraDaysAgo = customizeConfig.getSvnJiraDaysAgo();
            calendar.add(Calendar.DATE, -svnJiraDaysAgo);
            Date begin = calendar.getTime();
            // 计算出多少天之前的版本号
            long datedRevision = svnRepository.getDatedRevision(begin);

            SVNRevision startRevision = SVNRevision.create(datedRevision);
            SVNRevision endRevision = SVNRevision.HEAD;
            List<String> jiraTasks = versionControlDto.getJiraTasks();
            // 从svn日志中筛选出来涉及到Jira版本号的所有版本
            svnLogClient.doLog(svnurl, new String[]{""}, null, startRevision, endRevision, true, true, 0L, new MySVNLogHandler(jiraTasks));

            log.info(String.format("从 %d 天前开始，共查询到了 %d 个版本号", svnJiraDaysAgo, MySVNLogHandler.list.size()));


            SVNDiffClient svnDiffClient = SvnRepoUtil.getSvnDiffClient(svnUserName, svnPassWord);
            for (LogEntryDto logEntryDto : MySVNLogHandler.list) {
                MySVNDiffStatusHandler.list.clear();

                Long version = logEntryDto.getRevision();
                SVNRevision oldVersion = SVNRevision.create(version - 1L);
                SVNRevision newVersion = SVNRevision.create(version);
                // svn工具比较分支与版本的差异
                svnDiffClient.doDiffStatus(SVNURL.parseURIEncoded(versionControlDto.getRepoUrl()), oldVersion, SVNURL.parseURIEncoded(nowSvnUrl), newVersion, SVNDepth.INFINITY, true, new MySVNDiffStatusHandler());
                //将差异代码设置进集合
                //  根据获取到的差异java相对目录 单独下载差异文件
                List<DiffEntryDto> curDiffEntryDtoList = new ArrayList(MySVNDiffStatusHandler.list);


                // 因为jira任务号对应的svn版本可能会是多个，所以将所有涉及到的版本代码全部放在一起
                String localBaseRepoDir = SvnRepoUtil.getSvnLocalDir(versionControlDto.getRepoUrl(), customizeConfig.getSvnLocalBaseRepoDir(), oldVersion.toString());
                String localNowRepoDir = SvnRepoUtil.getSvnLocalDir(nowSvnUrl, customizeConfig.getSvnLocalBaseRepoDir(), newVersion.toString());
                LoggerUtil.info(log, "jira任务号旧版本本地地址", localBaseRepoDir);
                LoggerUtil.info(log, "jira任务号新版本本地地址", localNowRepoDir);
                VersionControlDto curVersionControlDto = new VersionControlDto();
                curVersionControlDto.setNewLocalBasePath(localNowRepoDir);
                curVersionControlDto.setOldLocalBasePath(localBaseRepoDir);
                versionControlDtoList.add(curVersionControlDto);

                //将每一个涉及到的svn版本中的差异文件下载到目录中
                for (DiffEntryDto diffClass : curDiffEntryDtoList) {
                    // 差异文件的相对路径
                    String relativePath = diffClass.getNewPath();
                    DiffEntry.ChangeType changeType = diffClass.getChangeType();
                    // 如果差异类型为 修改
                    if (DiffEntry.ChangeType.MODIFY.equals(changeType) && relativePath.endsWith(".java")) {
                        StringBuilder localBaseModifyRepoDir = new StringBuilder(localBaseRepoDir);
                        StringBuilder localNowModifyRepoDir = new StringBuilder(localNowRepoDir);
                        localBaseModifyRepoDir.append(File.separator).append(relativePath);
                        localNowModifyRepoDir.append(File.separator).append(relativePath);
                        // svn clone到本地时 只clone差异文件
                        SvnRepoUtil.exportFile(versionControlDto.getRepoUrl() + "/" + relativePath, localBaseModifyRepoDir.toString(), oldVersion, svnUserName, svnPassWord);
                        SvnRepoUtil.exportFile(nowSvnUrl + "/" + relativePath, localNowModifyRepoDir.toString(), newVersion, svnUserName, svnPassWord);
                    }
                }
                curVersionControlDto.setDiffClasses(curDiffEntryDtoList);
            }


        } catch (SVNException e) {
            e.printStackTrace();
            if (e instanceof SVNAuthenticationException) {
                throw new BizException(BizCode.SVN_AUTH_FAILED);
            } else {
                throw new BizException(BaseCode.SYSTEM_FAILD);
            }
        }
        return versionControlDtoList;
    }


    /**
     * @param filePackage
     * @date:2021/4/24
     * @className:VersionControl
     * @author:Administrator
     * @description: 获取旧版本文件本地路径
     */
    @Override
    public String getLocalNewPath(VersionControlDto versionControlDto, String filePackage) {
        return getCodePath(versionControlDto.getNewLocalBasePath(), filePackage);
    }


    /**
     * @param filePackage
     * @date:2021/4/24
     * @className:VersionControl
     * @author:Administrator
     * @description: 获取新版本文件本地路径
     */
    @Override
    public String getLocalOldPath(VersionControlDto versionControlDto, String filePackage) {
        return getCodePath(versionControlDto.getOldLocalBasePath(), filePackage);
    }

}
