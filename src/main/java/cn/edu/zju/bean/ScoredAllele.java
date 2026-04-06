package cn.edu.zju.bean;

/**
 * 打分策略的中间结果：某个 star allele 与患者 rsID 集合的匹配得分。
 * score = matchedCount / totalRequired（命中数 / 该 allele 需要的 rsID 总数）
 */
public class ScoredAllele {

    private final String starAllele;
    private final int matchedCount;
    private final int totalRequired;

    public ScoredAllele(String starAllele, int matchedCount, int totalRequired) {
        this.starAllele = starAllele;
        this.matchedCount = matchedCount;
        this.totalRequired = totalRequired;
    }

    public String getStarAllele() { return starAllele; }
    public int getMatchedCount() { return matchedCount; }
    public int getTotalRequired() { return totalRequired; }

    /** 命中比例，用于日志和调试 */
    public double getScore() {
        return totalRequired == 0 ? 0 : (double) matchedCount / totalRequired;
    }

    @Override
    public String toString() {
        return starAllele + "(" + matchedCount + "/" + totalRequired + ")";
    }
}
