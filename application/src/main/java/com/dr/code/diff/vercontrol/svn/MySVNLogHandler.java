package com.dr.code.diff.vercontrol.svn;

import com.dr.code.diff.dto.DiffEntryDto;
import com.dr.code.diff.dto.LogEntryDto;
import org.eclipse.jgit.diff.DiffEntry;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ProjectName: code-diff-parent
 * @Package: com.dr.code.diff.vercontrol
 * @Description: java类作用描述
 * @Author: duanrui
 * @CreateDate: 2021/4/5 18:06
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
public class MySVNLogHandler implements ISVNLogEntryHandler {

    public final static List<LogEntryDto> list = Collections.synchronizedList(new ArrayList<LogEntryDto>());

    private List<String> jiraTasks;

    public MySVNLogHandler(List<String> jiraTasks) {
        this.jiraTasks = jiraTasks;
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
        for (int i = 0; i <this.jiraTasks.size() ; i++) {
            // 判断日志中是否包含要查询的任务号
            if (svnLogEntry.getMessage().contains(jiraTasks.get(i))) {

                LogEntryDto logEntryDto = new LogEntryDto();
                logEntryDto.setAuthor(svnLogEntry.getAuthor());
                logEntryDto.setDate(svnLogEntry.getDate());
                logEntryDto.setMessage(svnLogEntry.getMessage());
                logEntryDto.setRevision(svnLogEntry.getRevision());
                list.add(logEntryDto);
            }
        }


    }
}
