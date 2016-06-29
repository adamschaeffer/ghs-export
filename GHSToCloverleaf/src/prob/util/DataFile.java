package prob.util;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.NamingException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import prob.util.DBConnection;

public abstract class DataFile<T> {
	protected String theQuery;
	protected ArrayList<T> theFile = null;
	protected ResultSet rs = null;
	protected DBConnection conn;	
	
	/**
	 * Create an OBExtract object that will attempt to get OBXExtract data from the data source named in the parameter. 
	 * If driver is null, we attempt to connect to a data source. Otherwise, we load the driver and connect directly to the DB.
	 * 
	 * @param jndi_name The JNDI name of the data source as configured on the server.
	 * @throws RuntimeException if there is an issue creating a db connection
	 */
	public DataFile(){}
	public DataFile(String jndi_name) throws RuntimeException{
		this(jndi_name,null,null,null);
	}
	public DataFile(String URI,String driver,String username,String password) throws RuntimeException{
		setDBConnection(URI,driver,username,password);
	}

	/**
	 * If using the parameterless constructor, use this function to set the data source.
	 * 
	 * @param jndi The JNDI name of the data source as configured on the server. 
	 */
	protected void setDBConnection(String jndi,String driver,String username,String password) {
		if(conn!=null){
			throw new RuntimeException("Error: Connection already established.");
		}
		else{
			try{
				if(driver==null)
					conn = new DBConnection(jndi);
				else
					conn = new DBConnection(jndi,driver,username,password);
			}
			catch(SQLException e){
				throw new RuntimeException(e.getMessage());
			}
			catch(NamingException e){
				throw new RuntimeException(e.getMessage());
			}
			catch(RuntimeException e){
				throw e;
			}
		}
	}

	/**
	 * Once a connection to a data source has been established, this function will run the query and attempt 
	 * to get the data into a local ResultSet object.
	 * @throws Exception 
	 */
	public void runQuery() {
		theQuery = getQuery();

		if(theQuery == "" || theQuery == null){
			throw new RuntimeException("Invalid query.");
		}
		
		try {
			rs = conn.Query(theQuery);
			theFile = new ArrayList<T>();
			setFile();
		} catch (SQLException e) {
			throw new RuntimeException("Error executing query: " + e.getMessage());
		}catch(Exception e){
			throw new RuntimeException("Another missed exception here: " + e.getClass() + ": " + e.getMessage());
		}
	}

	/**
	 * @return A representation of the extract file as a JSON formatted string
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public String getJsonString() throws JsonGenerationException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		String rtn = mapper.writeValueAsString(theFile);
		return rtn;
	}
	
	/**
	 * This function parses a JSON string and attempts to set the contents of the OBXExtract file.
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public void setFromJsonString(String jsonSource) throws JsonParseException, JsonMappingException, IOException{
	 	ObjectMapper mapper = new ObjectMapper();
	 	theFile = mapper.readValue(jsonSource,new TypeReference<ArrayList<T>>(){});
	}

	/**
	 * This function will close all objects associated with this DataFile object.
	 */
	public void close() {
		try{
			if(conn!=null)
				conn.close();
		}
		catch(Exception e){
			throw new RuntimeException(e.getClass() + ": " + e.getMessage());
		}
	}
	
	public ArrayList<T> getFile(){
		return theFile;
	}

	/**
	 * This function will use the ResultSet variable that is initialized in runQuery,
	 * and set the variable theFile. 
	 * @throws Exception 
	 */
	protected abstract void setFile() throws Exception;

	/**
	 * This function will set the sql query parameter.
	 * @param sql : the sql query that will grab the data that will be used to create the data file contents.
	 */
	public abstract String getQuery();
}
