package cn.edu.zju.bean;

public class Genotype {

    private int id;
    private String geneSymbol;
    private String rsid;
    private String chromosome;
    private Long position;
    private String refAllele;
    private String altAllele;
    private String starAllele;
    private String alleleFunction;
    private Boolean isRequired;

    public Genotype() {
    }

    public Genotype(int id, String geneSymbol, String rsid, String chromosome, Long position,
                    String refAllele, String altAllele, String starAllele,
                    String alleleFunction, Boolean isRequired) {
        this.id = id;
        this.geneSymbol = geneSymbol;
        this.rsid = rsid;
        this.chromosome = chromosome;
        this.position = position;
        this.refAllele = refAllele;
        this.altAllele = altAllele;
        this.starAllele = starAllele;
        this.alleleFunction = alleleFunction;
        this.isRequired = isRequired;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGeneSymbol() { return geneSymbol; }
    public void setGeneSymbol(String geneSymbol) { this.geneSymbol = geneSymbol; }

    public String getRsid() { return rsid; }
    public void setRsid(String rsid) { this.rsid = rsid; }

    public String getChromosome() { return chromosome; }
    public void setChromosome(String chromosome) { this.chromosome = chromosome; }

    public Long getPosition() { return position; }
    public void setPosition(Long position) { this.position = position; }

    public String getRefAllele() { return refAllele; }
    public void setRefAllele(String refAllele) { this.refAllele = refAllele; }

    public String getAltAllele() { return altAllele; }
    public void setAltAllele(String altAllele) { this.altAllele = altAllele; }

    public String getStarAllele() { return starAllele; }
    public void setStarAllele(String starAllele) { this.starAllele = starAllele; }

    public String getAlleleFunction() { return alleleFunction; }
    public void setAlleleFunction(String alleleFunction) { this.alleleFunction = alleleFunction; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
}
