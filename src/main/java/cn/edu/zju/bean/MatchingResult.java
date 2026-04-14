package cn.edu.zju.bean;

public class MatchingResult {
    private Integer id;
    private Integer sampleId;
    private String drugLabelId;
    private String drugName;
    private String source;
    private String summaryMarkdown;

    public MatchingResult() {
    }

    public MatchingResult(Integer id, Integer sampleId, String drugLabelId,
                          String drugName, String source, String summaryMarkdown) {
        this.id = id;
        this.sampleId = sampleId;
        this.drugLabelId = drugLabelId;
        this.drugName = drugName;
        this.source = source;
        this.summaryMarkdown = summaryMarkdown;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSampleId() {
        return sampleId;
    }

    public void setSampleId(Integer sampleId) {
        this.sampleId = sampleId;
    }

    public String getDrugLabelId() {
        return drugLabelId;
    }

    public void setDrugLabelId(String drugLabelId) {
        this.drugLabelId = drugLabelId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSummaryMarkdown() {
        return summaryMarkdown;
    }

    public void setSummaryMarkdown(String summaryMarkdown) {
        this.summaryMarkdown = summaryMarkdown;
    }
}