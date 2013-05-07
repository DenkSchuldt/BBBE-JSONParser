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
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.swing.JOptionPane;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
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
    public static String json = "";
    public static String xml = "";
    public static FileWriter fw = null;
    public static PrintWriter pw = null;
    
    public static void main(String args[]) throws IOException, TransformerConfigurationException, TransformerException{
        
        path = "Z:/butter/popcorn.sqlite";
        String sql = "select * from Projects where id='4' and email='dschuldt@outlook.com'";
        
        try{
            
            /*
             * Query to the DB 
             */
            result = query(sql);
            
            if(result != null){                
                int number_columns = result.getMetaData().getColumnCount();
                for(int j = 1;j <= number_columns;j++){
                    System.out.println(""+result.getMetaData().getColumnName(j));
                }
                                
                /*
                 * Pretty print the JSON result.
                 */
                fw = new FileWriter("json.json");
                pw = new PrintWriter(fw);                
                json = prettyJSON(result.getString(2));
                json = removeClipData(json);
                pw.println(json);
                fw.close();
                fw = null; pw = null;
                             
                /*
                 * Parse the JSON to XML
                 */
                JSONObject jo = new JSONObject(json);
                String new_xml = org.json.XML.toString(jo);
                fw = new FileWriter("generated.xml");
                pw = new PrintWriter(fw);
                pw.println(new_xml);
                fw.close();
                fw = null; pw = null;
                                
                /*
                 * Pretty print the XML result.
                 */                
                fw = new FileWriter("pretty_generated.xml");
                pw = new PrintWriter(fw);
                xml = prettyXML(readFile("generated.xml"));
                pw.println(xml);
                fw.close();
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
    
    /*
     * Method: query(String sql)
     * Usage: query("Your query to the sqlite DB");
     * --------------------------------------------
     * Description: Realizes a query to the sqlite DB, and 
     *              return the result as a ResultSet.
     */
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
    
    /*
     * Method: connect()
     * Usage: connect();
     * -----------------
     * Description: Connects to the Sqlite DB.
     */
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
        
    /*
     * Method: readFile(String path)
     * Usage: readFile("Path to your file.extension");
     * -----------------------------------------------
     * Description: Returns the content of a file as a String.
     */
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
    
    /*
     * Method: prettyXML(String xml)
     * Usage: prettyXML("Your ugly xml String here");
     * ------------------------------------------
     * Description: returns a pretty xml String.
     */
    public static String prettyXML(String xml) throws TransformerConfigurationException, TransformerException{
        Source xmlInput = new StreamSource(new StringReader(xml));
        StreamResult xmlOutput = new StreamResult(new StringWriter());        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "testing.dtd");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");        
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
        transformer.transform(xmlInput, xmlOutput);
        return xmlOutput.getWriter().toString();
    }
    
    /*
     * Method: prettyJSON(String json)
     * Usage: prettyJSON("Your ugly json String here");
     * ------------------------------------------
     * Description: returns a pretty json String.
     */
    public static String prettyJSON(String json){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        return gson.toJson(je);
    }
    
    /*
     * Method: removeClipData(String json)
     * Usage: removeClipData("Json string");
     * ------------------------------------------
     * Description: remove the "clipData" key and its value from de Json,
     *              which causes conflict when parsing to xml.
     */
    public static String removeClipData(String json){        
        int fst = 0, lst = 0;
        String to_replace = "";
        if (json.indexOf("clipData") != -1) {                        
            fst = json.indexOf("clipData");                                                            
            for(lst=fst; json.charAt(lst) != '}'; lst++){
                to_replace = to_replace + json.charAt(lst);
            }
            to_replace = to_replace + json.charAt(lst);            
        }
        String resp = json.replaceAll(to_replace,"");
        return resp;
    }
}
