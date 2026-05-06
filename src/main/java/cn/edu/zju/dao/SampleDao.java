package cn.edu.zju.dao;

import cn.edu.zju.bean.Sample;
import cn.edu.zju.dbutils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SampleDao extends BaseDao {

    public int save(Sample sample) {
        AtomicInteger key = new AtomicInteger();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement("insert into sample(created_at, uploaded_by, input_type, file_name, parse_status) values (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setTimestamp(1, new Timestamp(sample.getCreatedAt().getTime()));
                preparedStatement.setString(2, sample.getUploadedBy());
                preparedStatement.setString(3, sample.getInputType());
                preparedStatement.setString(4, sample.getFileName());
                preparedStatement.setString(5, sample.getParseStatus());
                preparedStatement.executeUpdate();
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                while (generatedKeys.next()) {
                    key.set(generatedKeys.getInt(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return key.get();
    }

    public List<Sample> findAll() {
        List<Sample> samples = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "select id, created_at, uploaded_by, input_type, file_name, parse_status from sample"
                );
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    int sampleId = resultSet.getInt("id");
                    Date createdAt = new Date(resultSet.getTimestamp("created_at").getTime());
                    String uploadedBy = resultSet.getString("uploaded_by");
                    String inputType = resultSet.getString("input_type");
                    String fileName = resultSet.getString("file_name");
                    String parseStatus = resultSet.getString("parse_status");

                    Sample sample = new Sample(sampleId, createdAt, uploadedBy, inputType, fileName, parseStatus);
                    samples.add(sample);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return samples;
    }

    public Sample findById(int id) {
        AtomicReference<Sample> sample = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement("select id, created_at, uploaded_by, input_type, file_name, parse_status from sample where id = ?");
                preparedStatement.setInt(1, id);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int sampleId = resultSet.getInt("id");
                    Date createdAt = new Date(resultSet.getTimestamp("created_at").getTime());
                    String uploadedBy = resultSet.getString("uploaded_by");
                    String inputType = resultSet.getString("input_type");
                    String fileName = resultSet.getString("file_name");
                    String parseStatus = resultSet.getString("parse_status");

                    sample.set(new Sample(sampleId, createdAt, uploadedBy, inputType, fileName, parseStatus));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return sample.get();
    }

    public void updateParseStatus(int id, String parseStatus) {
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement("update sample set parse_status = ? where id = ?");
                preparedStatement.setString(1, parseStatus);
                preparedStatement.setInt(2, id);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
