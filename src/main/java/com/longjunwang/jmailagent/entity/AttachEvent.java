package com.longjunwang.jmailagent.entity;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

import java.io.File;
import java.io.InputStream;

public class AttachEvent extends ApplicationEvent {
    private String fileName;
    private InputStream inputStream;
    private File file;

    public AttachEvent(Object source, String fileName, InputStream inputStream) {
        this(source, fileName, inputStream, null);
    }

    public AttachEvent(Object source, File file) {
        this(source, file.getName(), null, file);
    }

    public AttachEvent(Object source, String fileName, InputStream inputStream, File file) {
        super(source);
        this.fileName = fileName;
        this.inputStream = inputStream;
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "AttachEvent{" +
                "fileName='" + fileName + '\'' +
                ", inputStream=" + inputStream +
                ", file=" + file +
                '}';
    }
}
