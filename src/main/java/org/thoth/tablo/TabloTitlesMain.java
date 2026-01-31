
package org.thoth.tablo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *
 * @author Michael
 */
public class TabloTitlesMain {

    
    
    public static void main(String[] args) throws Exception {
     
        String DB_URL 
            = "jdbc:sqlite:D:/Documents/Databases/SQLite/tablo/Tablo.db";
        Connection conn
            = DriverManager.getConnection(DB_URL);
        PreparedStatement stmt
            = conn.prepareStatement("select distinct title from recording where actualDurationInSeconds > 0");
        ResultSet rs
            = stmt.executeQuery();
        
        while (rs.next()) {
            System.out.printf("%s%n", rs.getString("title"));
        }        
    } 
    
}
