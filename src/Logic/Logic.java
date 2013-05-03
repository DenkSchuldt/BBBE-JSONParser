/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;

/**
 *
 * @author Denny
 */
public class Logic{
        
    public static ResultSet result = null;        
    public static Connection connection;
    public static Statement query_statement;
    public static String path;
    
    public static void main(String args[]){
        path = "C:/Users/Denny/Desktop/popcorn.sqlite";
        
        String sql = "select * from Projects";
        try {
            result = query(sql);
            if(result != null){
                int number_columns = result.getMetaData().getColumnCount();
                for(int j = 1;j <= number_columns;j++){
                    System.out.println(""+result.getMetaData().getColumnName(j));                    
                }                
            }
        }catch(SQLException e){}
        finally{
            try{
                query_statement.close();
                connection.close();
                if(result != null){
                    result.close();
                 }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    
    public static ResultSet query(String sql){
        connect();
        ResultSet result = null;
        try{
            result = query_statement.executeQuery(sql);
        }catch (SQLException e) {
            System.out.println("Message: " + e.getMessage());
            System.out.println("Status: " + e.getSQLState());
            System.out.println("Error code: " + e.getErrorCode());
            JOptionPane.showMessageDialog(null,""+e.getMessage());
        }
        return result;
    }    
    
    public static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }	 
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            query_statement = connection.createStatement();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }    
    
}
