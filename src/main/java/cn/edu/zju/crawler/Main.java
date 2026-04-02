package cn.edu.zju.crawler;

public class Main {
    public static void main(String[] args) {

        DrugLabelCrawler drugLabelCrawler = new DrugLabelCrawler();
        DosingGuidelineCrawler dosingGuidelineCrawler = new DosingGuidelineCrawler();
        VariantGenotypeCrawler variantGenotypeCrawler = new VariantGenotypeCrawler();
        GenotypePhenotypeCrawler genotypePhenotypeCrawler = new GenotypePhenotypeCrawler();

        // comment the step, if you have finished it

        // Step 1
        drugLabelCrawler.doCrawlerDrug();

        // Step 2
        drugLabelCrawler.doCrawlerDrugLabel();

        // Step 3
        dosingGuidelineCrawler.doCrawlerDosingGuidelineList();

        // Step 4: import variant_to_genotype.csv -> variants2genotype table
        variantGenotypeCrawler.doImport();

        // Step 5: import genotype_to_phenotype.csv -> genotype2phenotype table
        genotypePhenotypeCrawler.doImport();
    }
}
