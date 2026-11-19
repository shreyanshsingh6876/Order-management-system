package com.ecommerce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Scanner;

public class OrderManagementSystem {

    // Change these values to match your local MySQL configuration.
    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "Shyam@29";

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connected to ecommerce_db successfully.");

            while (true) {
                showMenu();
                int choice = readInt("Enter your choice: ");

                switch (choice) {
                    case 1 -> insertSampleOrders(con);
                    case 2 -> displayOrdersByStatus(con);
                    case 3 -> increaseQuantity(con);
                    case 4 -> updateOrderStatus(con);
                    case 5 -> displayTotalOrderValue(con);
                    case 6 -> deleteCancelledOrder(con);
                    case 0 -> {
                        System.out.println("Exiting application.");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database connection/error: " + e.getMessage());
            System.out.println("Check that MySQL is running, ecommerce_db exists, and credentials are correct.");
        }
    }

    private static void showMenu() {
        System.out.println("\n========== ORDER MANAGEMENT SYSTEM ==========");
        System.out.println("1. Insert three sample orders");
        System.out.println("2. Search orders by status (quantity > 2)");
        System.out.println("3. Increase order quantity");
        System.out.println("4. Update order status");
        System.out.println("5. Display total order value");
        System.out.println("6. Delete an order (only if Cancelled)");
        System.out.println("0. Exit");
        System.out.println("==============================================");
    }

    // (a) Insert at least three records using PreparedStatement.
    private static void insertSampleOrders(Connection con) {
        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        Object[][] orders = {
            {1001, "Aarav Sharma", "Keyboard", 3, 799.00, "Pending"},
            {1002, "Priya Verma", "Mouse", 5, 399.00, "Shipped"},
            {1003, "Rahul Singh", "Monitor", 2, 8999.00, "Cancelled"}
        };

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Object[] order : orders) {
                ps.setInt(1, (Integer) order[0]);
                ps.setString(2, (String) order[1]);
                ps.setString(3, (String) order[2]);
                ps.setInt(4, (Integer) order[3]);
                ps.setDouble(5, (Double) order[4]);
                ps.setString(6, (String) order[5]);

                try {
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Inserted order ID " + order[0] + " successfully.");
                    }
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Duplicate order ID " + order[0] + ". Record was not inserted.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Insert error: " + e.getMessage());
        }
    }

    // (b) Status match, quantity > 2, unit_price descending.
    private static void displayOrdersByStatus(Connection con) {
        System.out.print("Enter order status: ");
        String status = sc.nextLine().trim();

        String sql = "SELECT * FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    printOrder(rs);
                }
                if (!found) {
                    System.out.println("No matching orders found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    // (c) Add the entered quantity to the existing quantity.
    private static void increaseQuantity(Connection con) {
        int orderId = readInt("Enter order ID: ");
        int additionalQuantity = readInt("Enter additional quantity: ");

        if (additionalQuantity <= 0) {
            System.out.println("Additional quantity must be greater than zero.");
            return;
        }

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, additionalQuantity);
            ps.setInt(2, orderId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Quantity updated successfully.");
            } else {
                System.out.println("Order ID " + orderId + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Quantity update error: " + e.getMessage());
        }
    }

    // (d) Update only when the order exists and status is valid.
    private static void updateOrderStatus(Connection con) {
        int orderId = readInt("Enter order ID: ");
        System.out.print("Enter new status (Pending, Shipped, Delivered): ");
        String newStatus = sc.nextLine().trim();

        if (!isValidStatus(newStatus)) {
            System.out.println("Invalid status. Allowed values: Pending, Shipped, Delivered.");
            return;
        }

        String selectSql = "SELECT * FROM Orders WHERE order_id = ?";
        String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";

        try (PreparedStatement select = con.prepareStatement(selectSql)) {
            select.setInt(1, orderId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order ID " + orderId + " does not exist.");
                    return;
                }
            }

            try (PreparedStatement update = con.prepareStatement(updateSql)) {
                update.setString(1, normalizeStatus(newStatus));
                update.setInt(2, orderId);
                update.executeUpdate();
            }

            System.out.println("Order status updated. Updated order details:");
            displaySingleOrder(con, orderId);
        } catch (SQLException e) {
            System.out.println("Status update error: " + e.getMessage());
        }
    }

    private static boolean isValidStatus(String status) {
        return status.equalsIgnoreCase("Pending")
            || status.equalsIgnoreCase("Shipped")
            || status.equalsIgnoreCase("Delivered");
    }

    private static String normalizeStatus(String status) {
        if (status.equalsIgnoreCase("pending")) return "Pending";
        if (status.equalsIgnoreCase("shipped")) return "Shipped";
        return "Delivered";
    }

    private static void displaySingleOrder(Connection con, int orderId) throws SQLException {
        String sql = "SELECT * FROM Orders WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) printOrder(rs);
            }
        }
    }

    // (e) SUM(quantity * unit_price).
    private static void displayTotalOrderValue(Connection con) {
        String sql = "SELECT SUM(quantity * unit_price) AS total_value FROM Orders";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double total = rs.getDouble("total_value");
                System.out.printf("Total order value: %.2f%n", total);
            }
        } catch (SQLException e) {
            System.out.println("Total calculation error: " + e.getMessage());
        }
    }

    // Delete only if the selected order currently has Cancelled status.
    private static void deleteCancelledOrder(Connection con) {
        int orderId = readInt("Enter order ID to delete: ");

        String sql = "DELETE FROM Orders WHERE order_id = ? AND status = 'Cancelled'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Cancelled order deleted successfully.");
            } else {
                System.out.println("Order cannot be deleted: it does not exist or its status is not Cancelled.");
            }
        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }

    private static void printOrder(ResultSet rs) throws SQLException {
        System.out.println("----------------------------------------------");
        System.out.println("Order ID      : " + rs.getInt("order_id"));
        System.out.println("Customer Name : " + rs.getString("customer_name"));
        System.out.println("Product Name  : " + rs.getString("product_name"));
        System.out.println("Quantity      : " + rs.getInt("quantity"));
        System.out.printf("Unit Price    : %.2f%n", rs.getDouble("unit_price"));
        System.out.println("Status        : " + rs.getString("status"));
        System.out.println("----------------------------------------------");
    }

    private static int readInt(String message) {
        while (true) {
            System.out.print(message);
            String value = sc.nextLine().trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }
}
