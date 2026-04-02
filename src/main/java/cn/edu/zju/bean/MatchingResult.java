package cn.edu.zju.bean;

import java.util.List;

public class MatchingResult {

    private String gene;
    private String diplotype;
    private String phenotype;
    /** dosing_guideline / drug_label 中 summary_markdown 含 "metabolizer" 的结果 */
    private List<DosingGuideline> metabolizerMatches;
    /** dosing_guideline / drug_label 中 summary_markdown 不含 "metabolizer" 的结果 */
    private List<DosingGuideline> generalMatches;

    public MatchingResult() {
    }

    public MatchingResult(String gene, String diplotype, String phenotype,
                          List<DosingGuideline> metabolizerMatches,
                          List<DosingGuideline> generalMatches) {
        this.gene = gene;
        this.diplotype = diplotype;
        this.phenotype = phenotype;
        this.metabolizerMatches = metabolizerMatches;
        this.generalMatches = generalMatches;
    }

    public String getGene() { return gene; }
    public void setGene(String gene) { this.gene = gene; }

    public String getDiplotype() { return diplotype; }
    public void setDiplotype(String diplotype) { this.diplotype = diplotype; }

    public String getPhenotype() { return phenotype; }
    public void setPhenotype(String phenotype) { this.phenotype = phenotype; }

    public List<DosingGuideline> getMetabolizerMatches() { return metabolizerMatches; }
    public void setMetabolizerMatches(List<DosingGuideline> metabolizerMatches) {
        this.metabolizerMatches = metabolizerMatches;
    }

    public List<DosingGuideline> getGeneralMatches() { return generalMatches; }
    public void setGeneralMatches(List<DosingGuideline> generalMatches) {
        this.generalMatches = generalMatches;
    }
}
