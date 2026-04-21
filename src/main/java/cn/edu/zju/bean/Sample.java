package cn.edu.zju.bean;

import java.util.Date;

public class Sample {
    private int id;  //sample独特的id,是从1开始排序的，主键
    private int userId;  //关联user表的外键

    private Date createdAt;   //sample的创建时间
    private String samplingData;   //sample的数据类型，有且只有两个类型，一个是VCF，一个是annovar


    // 无参构造
    public Sample() {
    }

    // 全参构造
    public Sample(int id, int userId, Date createdAt, String samplingData, String uploadFormat) {
        this.id = id;
        this.userId = userId;

        this.createdAt = createdAt;
        this.samplingData = samplingData;

    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }


    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getSamplingData() { return samplingData; }
    public void setSamplingData(String samplingData) { this.samplingData = samplingData; }

}
