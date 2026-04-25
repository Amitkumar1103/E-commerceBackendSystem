package service;

import model.Product;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    public List<Product> getAllProducts() {

        List<Product> products = new ArrayList<>();

        String query = "SELECT * FROM products";

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getString("description"));

                products.add(p);
            }

        } catch (Exception e) {
            System.out.println("Error fetching products: " + e.getMessage());
        }

        return products;
    }

    public Product getProduct(int id) {
        try (Connection con = DBConnection.getConnection()) {

            String query = "SELECT * FROM products WHERE id=?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getString("description"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void updateStock(int productId, int newStock) {
        try (Connection con = DBConnection.getConnection()) {

            String query = "UPDATE products SET stock=? WHERE id=?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, newStock);
            ps.setInt(2, productId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}