package cn.edu.zju.dao;

import cn.edu.zju.dbutils.DBUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnovarDaoIntegrationTest {

    private static final int TEST_SAMPLE_ID = -900001;

    private final AnnovarDao annovarDao = new AnnovarDao();

    @Before
    public void cleanBeforeTest() {
        deleteTestRows();
    }

    @After
    public void cleanAfterTest() {
        deleteTestRows();
    }

    @Test
    public void saveAnnovarOutputPersistsRowsAndReturnsNonSynonymousGenes() {
        String annovarOutput = ""
                + "Chr\tStart\tEnd\tRef\tAlt\tFunc.refGene\tGene.refGene\tGeneDetail.refGene\tExonicFunc.refGene\tAAChange.refGene\n"
                + "1\t12345\t12345\tA\tG\texonic\tTPMT\t.\tnonsynonymous SNV\tTPMT:NM_000367:exon1:c.A1G:p.K1R\n"
                + "10\t98765\t98765\tC\tT\texonic\tCYP2C19\t.\tsynonymous SNV\tCYP2C19:NM_000769:exon2:c.C2T:p.G2G\n";

        annovarDao.save(TEST_SAMPLE_ID, annovarOutput);

        List<String> refGenes = annovarDao.getRefGenes(TEST_SAMPLE_ID);

        assertEquals("The mock ANNOVAR output should save two database rows.",
                2, countSavedRows());
        assertTrue("Expected non-synonymous gene TPMT in refGenes, but got: " + refGenes,
                refGenes.contains("TPMT"));
        assertFalse("Expected synonymous gene CYP2C19 to be excluded, but got: " + refGenes,
                refGenes.contains("CYP2C19"));
    }

    private void deleteTestRows() {
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "delete from annovar where sample_id = ?"
                );
                preparedStatement.setInt(1, TEST_SAMPLE_ID);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private int countSavedRows() {
        List<Integer> counts = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "select count(*) from annovar where sample_id = ?"
                );
                preparedStatement.setInt(1, TEST_SAMPLE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    counts.add(resultSet.getInt(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return counts.isEmpty() ? 0 : counts.get(0);
    }
}
