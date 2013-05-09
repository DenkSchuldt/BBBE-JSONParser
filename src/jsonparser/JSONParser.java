/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jsonparser;

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
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *
 * @author Denny
 */
public class JSONParser{
        
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
        String sql = "select * from Projects where id='5' and email='dschuldt@outlook.com'";
        
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
                             
                /*
                 * Parse the JSON to XML
                 */
                JSONObject jo = new JSONObject(json);                
                xml = "<xml>" + org.json.XML.toString(jo) + "</xml>"; //Adding a root to the document. (It could be any tag)
                                
                /*
                 * Pretty print the XML result.
                 */
                fw = new FileWriter("xml.xml");
                pw = new PrintWriter(fw);
                xml = prettyXML(xml);
                pw.println(xml);
                fw.close();
                
                /*
                 * Creating "edit.xml"
                 */
                double start = 0, end = 0;
                String events = readFile("events.xml");
                String mutes = "";
                String skips = "";                
                String fileName = "";
                String text = "";
                try{
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(new File("xml.xml"));
                    Element elemento = doc.getDocumentElement();
                    NodeList trackEvents = elemento.getElementsByTagName("trackEvents");
                    for(int i=0; i<trackEvents.getLength(); i++){
                        Node currentNode = trackEvents.item(i);
                        NodeList nodes = currentNode.getChildNodes();
                        for(int j=0; j<nodes.getLength(); j++){
                            if(nodes.item(j).getNodeName().equals("popcornOptions")){
                                Node popcornOptions = nodes.item(j);
                                NodeList options = popcornOptions.getChildNodes();
                                for(int k=0; k<options.getLength(); k++){
                                    if(options.item(k).getNodeName().equals("start")){
                                        start = Double.parseDouble(options.item(k).getTextContent());
                                    }
                                    if(options.item(k).getNodeName().equals("end")){
                                        end = Double.parseDouble(options.item(k).getTextContent());
                                    }
                                    if(options.item(k).getNodeName().equals("title")){
                                        fileName = options.item(k).getTextContent();
                                    }
                                    if(options.item(k).getNodeName().equals("text")){
                                        text = options.item(k).getTextContent();
                                    }
                                }
                            }
                            if(nodes.item(j).getNodeName().equals("type")){
                                if(nodes.item(j).getTextContent().equals("text")){
                                    if(text.equals("mute")){
                                        mutes = mutes + "\n\t\t<mute start=\"" + start + "\" end=\"" + end + "\"></mute>";
                                    }
                                }
                                if(nodes.item(j).getTextContent().equals("skip")){
                                    skips = skips + "\n\t\t<skip start=\"" + start + "\" end=\"" + end + "\"></skip>";
                                }
                            }
                        }                        
                    }
                    mutes = "\t<mutes>" + mutes + "\n\t\t<name>" + processFileName(fileName) + "</name>" + "\n\t</mutes>";
                    skips = "\n\t<skips>" + skips + "\n\t</skips>";
                    events = events.replaceAll(Pattern.quote("</recording>"),mutes+skips+"\n</recording>");
                    fw = new FileWriter("edit.xml");
                    pw = new PrintWriter(fw);
                    pw.print(events);
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("\n\n\tHouston, we have a problem! :S");
                }finally{                                        
                    fw.close();
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
    
    /*
     * Method: query
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
     * Method: connect
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
     * Method: readFile
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
     * Method: prettyXML
     * Usage: prettyXML("Your ugly xml String here");
     * ------------------------------------------
     * Description: returns a pretty xml String.
     */
    public static String prettyXML(String xml) throws TransformerConfigurationException, TransformerException{
        Source xmlInput = new StreamSource(new StringReader(xml));
        StreamResult xmlOutput = new StreamResult(new StringWriter());        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();        
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
        transformer.transform(xmlInput, xmlOutput);
        return xmlOutput.getWriter().toString();
    }
    
    /*
     * Method: prettyJSON
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
     * Method: removeClipData
     * Usage: removeClipData("Json string");
     * ------------------------------------------
     * Description: remove the "clipData" key and its value from de Json,
     *              which causes conflict when parsing to xml.
     */
    public static String removeClipData(String json){
        int fst = 0, lst = 0;
        String to_replace = "";
        if (json.indexOf("clipData") != -1){
            fst = json.indexOf("clipData") - 1;            
            if((json.charAt(fst + 10) == '{') || (json.charAt(fst + 11) == '{') || (json.charAt(fst + 12) == '{')){
                for(lst=fst; json.charAt(lst) != '}'; lst++){
                    to_replace = to_replace + json.charAt(lst);
                }
                to_replace = to_replace + json.charAt(lst);                
            }
        }
        return json.replaceAll(Pattern.quote(to_replace),"");
    }
    
    /*
     * Method: processFileName
     * Usage: processFileName("The audio file name");
     * ------------------------------------------
     * Description: returns the name of the audio file
     *              without the butter id.
     */
    public static String processFileName(String file){
        String fileName = "";
        for(int i=0; file.charAt(i)!='?'; i++){
            fileName = fileName + file.charAt(i);
        }
        return fileName;
    }
}
