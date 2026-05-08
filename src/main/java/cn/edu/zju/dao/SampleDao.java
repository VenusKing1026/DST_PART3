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
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "insert into sample(created_at, uploaded_by, input_type, file_name, parse_status, user_id) values (?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setTimestamp(1, new Timestamp(sample.getCreatedAt().getTime()));
                preparedStatement.setString(2, sample.getUploadedBy());
                preparedStatement.setString(3, sample.getInputType());
                preparedStatement.setString(4, sample.getFileName());
                preparedStatement.setString(5, sample.getParseStatus());
                preparedStatement.setInt(6, sample.getUserId());
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

    public List<Sample> findByUser(int userId) {
        List<Sample> samples = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, uploaded_by, input_type, file_name, parse_status FROM sample WHERE user_id = ? ORDER BY created_at DESC";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    samples.add(mapRowToSample(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return samples;
    }

    public List<Sample> findAll() {
        List<Sample> samples = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, uploaded_by, input_type, file_name, parse_status FROM sample ORDER BY created_at DESC";
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    samples.add(mapRowToSample(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return samples;
    }

    public void updateMatchingStatus(int sampleId, String status) {
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "UPDATE sample SET matching_status = ?, matched_at = ? WHERE id = ?");
                ps.setString(1, status);
                ps.setTimestamp(2, new Timestamp(new Date().getTime()));
                ps.setInt(3, sampleId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Sample findById(int id) {
        AtomicReference<Sample> sample = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, uploaded_by, input_type, file_name, parse_status FROM sample WHERE id = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    sample.set(mapRowToSample(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return sample.get();
    }

    public Sample findById(int id, int userId) {
        AtomicReference<Sample> sample = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, uploaded_by, input_type, file_name, parse_status FROM sample WHERE id = ? AND user_id = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, id);
                ps.setInt(2, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    sample.set(mapRowToSample(rs));
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
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "update sample set parse_status = ? where id = ?");
                preparedStatement.setString(1, parseStatus);
                preparedStatement.setInt(2, id);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean delete(int id, int userId) {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        DBUtils.execSQL(connection -> {
            try {
                String sql = "DELETE FROM sample WHERE id = ? AND user_id = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, id);
                ps.setInt(2, userId);
                int affected = ps.executeUpdate();
                result.set(affected > 0);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return result.get();
    }

    private Sample mapRowToSample(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        Date createdAt = new Date(rs.getTimestamp("created_at").getTime());
        String uploadedBy = rs.getString("uploaded_by");
        String inputType = rs.getString("input_type");
        String fileName = rs.getString("file_name");
        String parseStatus = rs.getString("parse_status");
        return new Sample(id, userId, createdAt, uploadedBy, inputType, fileName, parseStatus);
    }
}
