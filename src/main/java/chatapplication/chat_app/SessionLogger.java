package chatapplication.chat_app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SessionLogger {

    private static final String URL = "jdbc:mysql://localhost:3306/strangerwave";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    // Called when a user connects — logs the start time
    public static void logSessionStart(String sessionId) {
        String sql = "INSERT INTO session_log (session_id, start_time, video_used) VALUES (?, ?, false)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to log session start: " + e.getMessage());
        }
    }

    // Called when a user disconnects — updates the end time
    public static void logSessionEnd(String sessionId) {
        String sql = "UPDATE session_log SET end_time = ? WHERE session_id = ? AND end_time IS NULL";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, sessionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to log session end: " + e.getMessage());
        }
    }

    // Called when video call starts — marks video_used = true
    public static void logVideoUsed(String sessionId) {
        String sql = "UPDATE session_log SET video_used = true WHERE session_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to log video usage: " + e.getMessage());
        }
    }
}