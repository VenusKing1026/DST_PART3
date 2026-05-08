package cn.edu.zju.dao;

import cn.edu.zju.bean.User;
import cn.edu.zju.dbutils.DBUtils;

import java.sql.*;
import java.util.concurrent.atomic.AtomicReference;

public class UserDao extends BaseDao {

    /**
     * 根据用户名查询用户（登录用）
     */
    public User findByUsername(String username) {
        AtomicReference<User> user = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, username, password_hash AS password FROM user WHERE username = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    user.set(mapRowToUser(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return user.get();
    }

    /**
     * 根据ID查询用户
     */
    public User findById(int id) {
        AtomicReference<User> user = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT id, username, password_hash AS password FROM user WHERE id = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    user.set(mapRowToUser(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return user.get();
    }

    /**
     * 保存新用户（注册用）
     * @return 新插入的用户ID，失败返回 -1
     */
    public int save(User user) {
        AtomicReference<Integer> generatedId = new AtomicReference<>(-1);
        DBUtils.execSQL(connection -> {
            try {
                String sql = "INSERT INTO user (username, password_hash) VALUES (?, ?)";
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        generatedId.set(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return generatedId.get();
    }

    /**
     * 检查用户名是否已存在
     */
    public boolean existsByUsername(String username) {
        AtomicReference<Boolean> exists = new AtomicReference<>(false);
        DBUtils.execSQL(connection -> {
            try {
                String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    exists.set(rs.getInt(1) > 0);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return exists.get();
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        return new User(id, username, password);
    }
}
