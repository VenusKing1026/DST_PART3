package cn.edu.zju.bean;

import java.util.Date;

public class Sample {
    private int id;
    private Date createdAt;
    private String uploadedBy;
    private String inputType;
    private String fileName;
    private String parseStatus;

    public Sample() {
    }

    public Sample(int id, Date createdAt, String uploadedBy, String inputType, String fileName, String parseStatus) {
        this.id = id;
        this.createdAt = createdAt;
        this.uploadedBy = uploadedBy;
        this.inputType = inputType;
        this.fileName = fileName;
        this.parseStatus = parseStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }
}
