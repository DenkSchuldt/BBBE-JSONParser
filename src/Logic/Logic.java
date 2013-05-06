/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;
import org.json.JSONObject;

/**
 *
 * @author Denny
 */
public class Logic{
        
    public static ResultSet result = null;    
    public static Connection connection;
    public static Statement query_statement;
    public static String path;
    
    public static void main(String args[]) throws IOException{
        
        path = "popcorn.sqlite";
        String sql = "select * from Projects"; // + Email.
        
        try{
            result = query(sql);
            if(result != null){
                int number_columns = result.getMetaData().getColumnCount();
                for(int j = 1;j <= number_columns;j++){
                    System.out.println(""+result.getMetaData().getColumnName(j));
                }
                                
                FileWriter fw = new FileWriter("json.json");
                PrintWriter pw = new PrintWriter(fw);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(result.getString(2));
                String prettyJsonString = gson.toJson(je);
                pw.println(prettyJsonString);
                fw.close();
                                
                JSONObject jo = new JSONObject(readFile("json.json"));                
                String xml = org.json.XML.toString(jo);
                FileWriter fw2 = new FileWriter("generated.xml");
                PrintWriter pw2 = new PrintWriter(fw2);
                pw2.println(xml);
                fw2.close();
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
            JOptionPane.showMessageDialog(null,"0 -> "+e.getMessage());
        }
        return result;
    }    
    
    public static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "1 -> " + e.getMessage());
        }	 
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            query_statement = connection.createStatement();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,"2 -> "  + e.getMessage());
        }
    }
    
    private static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        }
        finally {
            stream.close();
        }
    }
}
