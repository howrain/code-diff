package com.dr.code.diff.vercontrol.svn;

import com.dr.code.diff.config.CustomizeConfig;
import com.dr.code.diff.dto.DiffEntryDto;
import com.dr.code.diff.dto.VersionControlDto;
import com.dr.code.diff.enums.CodeManageTypeEnum;
import com.dr.code.diff.util.PathUtils;
import com.dr.code.diff.util.SvnRepoUtil;
import com.dr.code.diff.vercontrol.AbstractVersionControl;
import com.dr.common.errorcode.BaseCode;
import com.dr.common.errorcode.BizCode;
import com.dr.common.exception.BizException;
import com.dr.common.log.LoggerUtil;
import com.dr.common.utils.file.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName: code-diff-parent
 * @Package: com.dr.code.diff.vercontrol
 * @Description: svn差异代码获取
 * @Author: duanrui
 * @CreateDate: 2021/4/5 9:56
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
@Slf4j
@Component
public class SvnAbstractVersionControl extends AbstractVersionControl {

    @Autowired
    private CustomizeConfig customizeConfig;


    /**
     * 获取操作类型
     */
    @Override
    public CodeManageTypeEnum getType() {
        return CodeManageTypeEnum.SVN;
    }


    @Override
    public List<VersionControlDto> getDiffCodeClasses(VersionControlDto versionControlDto) {
        List<VersionControlDto> versionControlDtoList = new ArrayList<>();
        String svnUserName = versionControlDto.getSvnUserName();
        String svnPassWord = versionControlDto.getSvnPassWord();
        try {
            MySVNDiffStatusHandler.list.clear();
            String nowSvnUrl = versionControlDto.getRepoUrl();
            if (StringUtils.isNotBlank(versionControlDto.getSvnRepoUrl())) {
                nowSvnUrl = versionControlDto.getSvnRepoUrl();
            }
            SVNRevision oldVersion = null;
            SVNRevision newVersion = null;
            //不同reversion的比较和最新reversion的比较
            if (StringUtils.isNotBlank(versionControlDto.getNowVersion()) && StringUtils.isNotBlank(versionControlDto.getBaseVersion())) {
                oldVersion = SVNRevision.create(Long.parseLong(versionControlDto.getBaseVersion()));
                newVersion = SVNRevision.create(Long.parseLong(versionControlDto.getNowVersion()));
            } else {
                oldVersion = SVNRevision.HEAD;
                newVersion = SVNRevision.HEAD;
            }


            SVNDiffClient svnDiffClient = SvnRepoUtil.getSvnDiffClient(svnUserName, svnPassWord);
            // svn工具比较分支与版本的差异
            svnDiffClient.doDiffStatus(SVNURL.parseURIEncoded(versionControlDto.getRepoUrl()), oldVersion, SVNURL.parseURIEncoded(nowSvnUrl), newVersion, SVNDepth.INFINITY, true, new MySVNDiffStatusHandler());
            //将差异代码设置进集合
            //  根据获取到的差异java相对目录 单独下载差异文件
            versionControlDto.setDiffClasses(MySVNDiffStatusHandler.list);


            String localBaseRepoDir = SvnRepoUtil.getSvnLocalDir(versionControlDto.getRepoUrl(), customizeConfig.getSvnLocalBaseRepoDir(), oldVersion.toString());
            String localNowRepoDir = SvnRepoUtil.getSvnLocalDir(nowSvnUrl, customizeConfig.getSvnLocalBaseRepoDir(), newVersion.toString());
            LoggerUtil.info(log, "旧版本本地地址", localBaseRepoDir);
            LoggerUtil.info(log, "新版本本地地址", localNowRepoDir);
            versionControlDto.setNewLocalBasePath(localNowRepoDir);
            versionControlDto.setOldLocalBasePath(localBaseRepoDir);

            versionControlDtoList.add(versionControlDto);

            List<DiffEntryDto> diffClasses = versionControlDto.getDiffClasses();
            for (DiffEntryDto diffClass : diffClasses) {
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
        return PathUtils.getClassFilePath(versionControlDto.getNewLocalBasePath(), filePackage);
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
        String localDir = SvnRepoUtil.getSvnLocalDir(versionControlDto.getRepoUrl(), customizeConfig.getSvnLocalBaseRepoDir(), "");
        return PathUtils.getClassFilePath(versionControlDto.getOldLocalBasePath(), filePackage);
    }

}
