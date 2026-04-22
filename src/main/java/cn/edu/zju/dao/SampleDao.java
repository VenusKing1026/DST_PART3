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

    /**
     * 保存样本，关联到指定用户
     */
    public int save(int userId, String uploadFormat) {
        AtomicInteger key = new AtomicInteger();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "INSERT INTO sample (user_id, created_at, sampling_data) VALUES (?, ?, ?)";
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, userId);
                ps.setTimestamp(2, new Timestamp(new Date().getTime()));
                ps.setString(3, uploadFormat);
                ps.executeUpdate();

                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    key.set(generatedKeys.getInt(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return key.get();
    }

    /**
     * 查询当前用户的所有样本
     */
    public List<Sample> findByUser(int userId) {
        List<Sample> samples = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, sampling_data FROM sample WHERE user_id = ? ORDER BY created_at DESC";
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

    /**
     * 查询所有样本（管理员功能）
     */
    public List<Sample> findAll() {
        List<Sample> samples = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, sampling_data FROM sample ORDER BY created_at DESC";
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

    /**
     * 根据ID查询样本（带权限校验）
     */
    public Sample findById(int id, int userId) {
        AtomicReference<Sample> sample = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, sampling_data FROM sample WHERE id = ? AND user_id = ?";
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

    /**
     * 不校验权限的按ID查询（谨慎使用）
     */
    public Sample findByIdAndUserId(int id) {
        AtomicReference<Sample> sample = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, user_id, created_at, sampling_data FROM sample WHERE id = ?";
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

    /**
     * 删除样本（带权限校验）
     */
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
        String uploadFormat = rs.getString("sampling_data");
        return new Sample(id, userId, createdAt, uploadFormat);
    }
}