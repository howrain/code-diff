package com.dr.code.diff.vercontrol;

import com.dr.code.diff.dto.ClassInfoResult;
import com.dr.code.diff.dto.DiffEntryDto;
import com.dr.code.diff.dto.MethodInfoResult;
import com.dr.code.diff.dto.VersionControlDto;
import com.dr.code.diff.enums.CodeManageTypeEnum;
import com.dr.code.diff.util.MethodParserUtils;
import com.dr.code.diff.util.PathUtils;
import com.dr.common.log.LoggerUtil;
import com.dr.common.utils.file.FileUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @ProjectName: code-diff-parent
 * @Package: com.dr.code.diff.vercontrol
 * @Description: 代码差异获取流程类定义
 * @Author: duanrui
 * @CreateDate: 2021/4/5 9:56
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
@Data
@Slf4j
public abstract class AbstractVersionControl {

//    protected VersionControlDto versionControlDto;


    @Resource(name = "asyncExecutor")
    private Executor executor;


    /**
     * 执行handler
     * @return
     */
    public List<ClassInfoResult> handler(VersionControlDto versionControlDto) {
//        this.versionControlDto = versionControlDto;
        List<VersionControlDto> versionControlDtoList=getDiffCodeClasses(versionControlDto);
        List<ClassInfoResult> diffCodeMethods = getDiffCodeMethods(versionControlDtoList);
        return diffCodeMethods;
    }


    /**
     * 清理临时下载的代码文件
     */
    public void cleanTempCode(VersionControlDto versionControlDto) {
        FileUtil.removeDir(new File(versionControlDto.getNewLocalBasePath()));
        FileUtil.removeDir(new File(versionControlDto.getOldLocalBasePath()));
    }

    /**
     * @date:2021/4/5
     * @className:VersionControl
     * @author:Administrator
     * @description: 获取差异类
     * @param versionControlDto
     * @return
     */
    public abstract List<VersionControlDto> getDiffCodeClasses(VersionControlDto versionControlDto);

    /**
     * 获取操作类型
     */
    public abstract CodeManageTypeEnum getType();


    /**
    * @date:2021/4/24
    * @className:VersionControl
    * @author:Administrator
    * @description: 获取旧版本文件本地路径
    *
    */
    public abstract String getLocalNewPath(VersionControlDto versionControlDto,String filePackage);


    /**
     * @date:2021/4/24
     * @className:VersionControl
     * @author:Administrator
     * @description: 获取新版本文件本地路径
     *
     */
    public abstract String getLocalOldPath(VersionControlDto versionControlDto,String filePackage);


    public String getCodePath(String codePath, String filePackage) {
        return PathUtils.getClassFilePath(codePath, filePackage);
    }
    /**
     * @date:2021/4/5
     * @className:VersionControl
     * @author:Administrator
     * @description: 获取差异方法
     * @param versionControlDtoList
     */
    public List<ClassInfoResult> getDiffCodeMethods(List<VersionControlDto> versionControlDtoList) {
        List<ClassInfoResult> list = new ArrayList<>();
        for (VersionControlDto versionControlDto:versionControlDtoList) {
            if(CollectionUtils.isEmpty(versionControlDto.getDiffClasses())){
                continue;
            }
            LoggerUtil.info(log,"需要对比的差异类数",versionControlDto.getDiffClasses().size());
            String oldLocalBasePath = versionControlDto.getOldLocalBasePath();
            String newLocalBasePath = versionControlDto.getNewLocalBasePath();
            List<CompletableFuture<ClassInfoResult>> priceFuture = versionControlDto.getDiffClasses().stream()
                    .map(item -> getClassMethods(getCodePath(oldLocalBasePath,item.getNewPath()), getCodePath(newLocalBasePath,item.getNewPath()), item))
                    .collect(Collectors.toList());
            CompletableFuture.allOf(priceFuture.toArray(new CompletableFuture[0])).join();
            List<ClassInfoResult> tempList = priceFuture.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(tempList)){
                LoggerUtil.info(log,"计算出最终差异类数",tempList.size());
                list.addAll(tempList);
            }
            cleanTempCode(versionControlDto);

        }

        return list;
    }



    /**
     * 获取类的增量方法
     *
     * @param oldClassFile 旧类的本地地址
     * @param mewClassFile 新类的本地地址
     * @param diffEntry    差异类
     * @return
     */
    private CompletableFuture<ClassInfoResult> getClassMethods(String oldClassFile, String mewClassFile, DiffEntryDto diffEntry) {
        //多线程获取差异方法，此处只要考虑增量代码太多的情况下，每个类都需要遍历所有方法，采用多线程方式加快速度
        return CompletableFuture.supplyAsync(() -> {
            String className = "";
            if(diffEntry.getNewPath().contains("src/main/java/")){
                className = diffEntry.getNewPath().split("src/main/java/")[1].split("\\.")[0];
            }
            String moduleName = diffEntry.getNewPath().split("/")[0];
            //新增类直接标记，不用计算方法
            if (DiffEntry.ChangeType.ADD.equals(diffEntry.getChangeType())) {
                return ClassInfoResult.builder()
                        .classFile(className)
                        .type(DiffEntry.ChangeType.ADD.name())
                        .moduleName(moduleName)
                        .lines(diffEntry.getLines())
                        .build();
            }
            List<MethodInfoResult> diffMethods;
            //获取新类的所有方法
            List<MethodInfoResult> newMethodInfoResults = MethodParserUtils.parseMethods(mewClassFile);
            //如果新类为空，没必要比较
            if (CollectionUtils.isEmpty(newMethodInfoResults)) {
                return null;
            }
            //获取旧类的所有方法
            List<MethodInfoResult> oldMethodInfoResults = MethodParserUtils.parseMethods(oldClassFile);
            //如果旧类为空，新类的方法所有为增量
            if (CollectionUtils.isEmpty(oldMethodInfoResults)) {
                diffMethods = newMethodInfoResults;
            } else {   //否则，计算增量方法
                List<String> md5s = oldMethodInfoResults.stream().map(MethodInfoResult::getMd5).collect(Collectors.toList());
                diffMethods = newMethodInfoResults.stream().filter(m -> !md5s.contains(m.getMd5())).collect(Collectors.toList());
            }
            //没有增量方法，过滤掉
            if (CollectionUtils.isEmpty(diffMethods)) {
                return null;
            }
            return ClassInfoResult.builder()
                    .classFile(className)
                    .methodInfos(diffMethods)
                    .type(DiffEntry.ChangeType.MODIFY.name())
                    .moduleName(moduleName)
                    .lines(diffEntry.getLines())
                    .build();
        }, executor);
    }


}
