package cn.edu.zju.bean;

/**
 * Represents a genetic variant with genotype information
 * Used for chromosome-aware matching in V4 pipeline
 */
public class VariantWithGT {
    private String geneName;
    private String rsid;
    private String gt;                    // e.g. "0|0", "0|1", "1|1", "1|0"
    private boolean isHeterozygous;       // heterozygous flag
    private boolean isHomozygousAlt;      // homozygous variant flag
    private String chr;
    private int start;
    private int end;
    private String ref;
    private String alt;
    private String chromosome;            // chromosome
    private long position;                // position

    // Default constructor
    public VariantWithGT() {}

    // Constructor with essential fields
    public VariantWithGT(String geneName, String rsid, String gt) {
        this.geneName = geneName;
        this.rsid = rsid;
        this.gt = gt;
        this.isHeterozygous = isHeterozygousGT(gt);
        this.isHomozygousAlt = isHomozygousAltGT(gt);
    }

    // Helper method to determine if GT is heterozygous
    private boolean isHeterozygousGT(String gt) {
        if (gt == null || gt.isEmpty()) {
            return false;
        }
        // Check formats like "0|1", "1|0", "0/1", "1/0" - heterozygous
        if (gt.contains("|") || gt.contains("/")) {
            String[] alleles = gt.replace('|', '/').split("/");
            if (alleles.length >= 2) {
                return !alleles[0].equals(alleles[1]);
            }
        }
        return false;
    }

    // Helper method to determine if GT is homozygous alternate
    private boolean isHomozygousAltGT(String gt) {
        if (gt == null || gt.isEmpty()) {
            return false;
        }
        // Check formats like "1|1", "1/1" - homozygous alternate
        if (gt.contains("|") || gt.contains("/")) {
            String[] alleles = gt.replace('|', '/').split("/");
            if (alleles.length >= 2) {
                return alleles[0].equals(alleles[1]) && !alleles[0].equals("0");
            }
        }
        return false;
    }

    // Getters and setters
    public String getGeneName() {
        return geneName;
    }

    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    public String getRsid() {
        return rsid;
    }

    public void setRsid(String rsid) {
        this.rsid = rsid;
    }

    public String getGt() {
        return gt;
    }

    public void setGt(String gt) {
        this.gt = gt;
        this.isHeterozygous = isHeterozygousGT(gt);
        this.isHomozygousAlt = isHomozygousAltGT(gt);
    }

    public boolean isHeterozygous() {
        return isHeterozygous;
    }

    public void setHeterozygous(boolean heterozygous) {
        isHeterozygous = heterozygous;
    }

    public boolean isHomozygousAlt() {
        return isHomozygousAlt;
    }

    public void setHomozygousAlt(boolean homozygousAlt) {
        isHomozygousAlt = homozygousAlt;
    }

    public String getChr() {
        return chr;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }
}