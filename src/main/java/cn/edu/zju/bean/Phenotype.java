package cn.edu.zju.bean;

public class Phenotype {

    private int id;
    private String geneSymbol;
    private String diplotype;
    private String phenotype;
    private String activityScore;
    private String functionCategory;

    public Phenotype() {
    }

    public Phenotype(int id, String geneSymbol, String diplotype, String phenotype,
                     String activityScore, String functionCategory) {
        this.id = id;
        this.geneSymbol = geneSymbol;
        this.diplotype = diplotype;
        this.phenotype = phenotype;
        this.activityScore = activityScore;
        this.functionCategory = functionCategory;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGeneSymbol() { return geneSymbol; }
    public void setGeneSymbol(String geneSymbol) { this.geneSymbol = geneSymbol; }

    public String getDiplotype() { return diplotype; }
    public void setDiplotype(String diplotype) { this.diplotype = diplotype; }

    public String getPhenotype() { return phenotype; }
    public void setPhenotype(String phenotype) { this.phenotype = phenotype; }

    public String getActivityScore() { return activityScore; }
    public void setActivityScore(String activityScore) { this.activityScore = activityScore; }

    public String getFunctionCategory() { return functionCategory; }
    public void setFunctionCategory(String functionCategory) { this.functionCategory = functionCategory; }
}
