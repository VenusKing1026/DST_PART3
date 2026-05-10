package cn.edu.zju.controller;

import cn.edu.zju.bean.DrugLabel;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MatchingControllerMatchTest {

    @Test
    public void matchAnnotatedGenesToDrugLabels() {
        DrugLabel matchedLabel = drugLabel("label-1",
                "This drug label contains TPMT pharmacogenomic information.");
        DrugLabel unmatchedLabel = drugLabel("label-2",
                "This drug label contains no relevant gene information.");

        List<DrugLabel> matched = new MatchingController().doMatch(
                Arrays.asList("TPMT"),
                Arrays.asList(matchedLabel, unmatchedLabel)
        );

        assertEquals(1, matched.size());
        assertEquals("label-1", matched.get(0).getId());
    }

    private DrugLabel drugLabel(String id, String summaryMarkdown) {
        DrugLabel drugLabel = new DrugLabel();
        drugLabel.setId(id);
        drugLabel.setSummaryMarkdown(summaryMarkdown);
        return drugLabel;
    }
}
