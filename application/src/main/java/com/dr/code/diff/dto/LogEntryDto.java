package com.dr.code.diff.dto;

import lombok.Data;
import org.eclipse.jgit.diff.DiffEntry;

import java.util.Date;
import java.util.List;


@Data
public class LogEntryDto {


    /**
     * 日志提交作者
     */
    protected String author;

    /**
     * 日志提交日期
     */
    private Date date;


    /**
     * 日志提交信息
     */
    private String Message;

    /**
     * 日志提交版本
     */
    private Long Revision;
}
