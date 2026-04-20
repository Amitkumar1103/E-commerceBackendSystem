package service;

import model.User;
import util.DBConnection;

import java.sql.*;

public class UserService {

    public User register(String name, String email, String password) {

        String query = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();

            int userId = rs.getInt(1);

            return new User(userId, name, email, password);

        } catch (SQLException e) {
            System.out.println("User registration failed: " + e.getMessage());
            return null;
        }
    }

    public User login(String email, String password) {

        String query = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"));
            }

        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        return null;
    }
}
