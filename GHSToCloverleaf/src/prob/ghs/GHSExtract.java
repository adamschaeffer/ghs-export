package prob.ghs;

/**
 * @author ASchaeffer
 * 
 * This class will extract the GHS data (according to the QUERY variable) from the GHS database (as defined in the file
 * ghs.properties), format the data into an FRL format (according to the FILE_FORMAT variable), and send the data to 
 * cloverleaf (as defined in the file ghs.properties). A log is created in GHS_Export_Log.txt, at the level defined by 
 * the variable logLevel.
 * 
 * If there are any changes to the export file, every effort should be made to accommodate the changes through either
 * the QUERY parameter or the FILE_FORMAT parameter. If new files require new types of formatting not currently supported, 
 * new variables can be added to the FRLField class. Then this information can be used within the FormatField() function
 * in order to obtain the desired output. 
 * 
 * Once a record is sent to cloverleaf, and whether a proper acknowledgement is received or not, a call to the function 
 * UpdateRecord() will update the GHS database. Any changes to this process should be limited to the UpdateRecord() 
 * function, if possible.
 * 
 * The actual database operations are handled by the class prob.util.DBConnection. It will either look up the data source
 * by the JNDI name, or attempt to connect to the database directly, depending on the variables provided in ghs.properties.
 * If you are not using the JNDI name, you must include a jar file for the JDBC driver you are planning to use to
 * connect to the database. 
 */

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.naming.NamingException;

import prob.ghs.exception.AcknowledgmentException;
import prob.ghs.FRLField;
import prob.pix.pxSocket;
import prob.util.DBConnection;
import prob.util.Property_Set;

public class GHSExtract {
	//LOGGER SETTINGS
	private static final Logger GhsLog = Logger.getLogger(GHSExtract.class.getName());
	private static final Level loglevel = Level.FINER;
	
	//CONSTANTS
	private static final int NUM_QUESTIONS = 148;
	private static final int QUESTIONS_LENGTH = 294;
	private static final String CUSTOM_MARKER = "**CUSTOM**"; 
	
	//MEMBER VARIABLES
	private ArrayList<FRLField> FILE_FORMAT;
	private String QUERY;
	private pxSocket sock = null;
	private Property_Set export_props = null;
	private DBConnection conn = null;
	private ArrayList<GHSDao> theFile = new ArrayList<GHSDao>();

	//INTEGRATED CLASSES
	private SimpleDateFormat datetime_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	static{
		GhsLog.setLevel(loglevel);
		FileHandler h = null;
			try {
				h = new FileHandler("GHS_Export_Log_%u.txt");
				h.setLevel(loglevel);
				h.setFormatter(new SimpleFormatter());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			GhsLog.addHandler(h);
	}

	//////////////////////////////////////////////////////////////////
	//Constructors
	//////////////////////////////////////////////////////////////////
	public GHSExtract(Property_Set props,String jndi_name) throws NamingException, SQLException {
		conn = new DBConnection(jndi_name);
		export_props = props;
		initialize();
	}
	public GHSExtract(Property_Set props,String host,String driver,String username,String password){
		conn = new DBConnection(host,driver,username,password);
		export_props = props;
		initialize();
	}
	
	//////////////////////////////////////////////////////////////////
	//Initialize member variables
	//////////////////////////////////////////////////////////////////
	private void initialize() {
		StringBuffer t = new StringBuffer("select distinct ");
		FILE_FORMAT = new ArrayList<FRLField>();
		FILE_FORMAT.add(new FRLField("export_id",true));
		FILE_FORMAT.add(new FRLField("session_id",11,"R"," "));
		FILE_FORMAT.add(new FRLField("message_id",19,"R"," "));
		FILE_FORMAT.add(new FRLField("start_date",8,"R"," "));
		FILE_FORMAT.add(new FRLField("start_time",6,"R"," "));
		FILE_FORMAT.add(new FRLField("end_time",6,"R"," "));
		FILE_FORMAT.add(new FRLField("start_datetime",14,"R"," "));
		FILE_FORMAT.add(new FRLField("end_datetime",14,"R"," "));
		FILE_FORMAT.add(new FRLField("facility_id",10,"R"," "));
		FILE_FORMAT.add(new FRLField("aid",7,"R"," "));
		FILE_FORMAT.add(new FRLField("oriented",1,"R"," "));
		FILE_FORMAT.add(new FRLField("mpdj",11,"R"," "));
		FILE_FORMAT.add(new FRLField("minor_lastname",80,"R"," "));
		FILE_FORMAT.add(new FRLField("minor_firstname",80,"R"," "));
		FILE_FORMAT.add(new FRLField("minor_middlename",80,"R"," "));
		FILE_FORMAT.add(new FRLField("minor_dob",8,"R"," "));
		FILE_FORMAT.add(new FRLField("admin_lastname",80,"R"," "));
		FILE_FORMAT.add(new FRLField("admin_firstname",80,"R"," "));
		FILE_FORMAT.add(new FRLField("admin_middlename",80,"R"," "));
		FILE_FORMAT.add(new FRLField("question_alias",11,"R"," ",true,false));
		FILE_FORMAT.add(new FRLField(CUSTOM_MARKER+"set_id",3,"R"," ",true,true));
		FILE_FORMAT.add(new FRLField("question_response",200,"R"," ",true,false));
		FILE_FORMAT.add(new FRLField("question_scale",80,"R"," ",true,false));
		
		for(int i = 0; i < FILE_FORMAT.size(); i++){
			if(FILE_FORMAT.get(i).field_name.contains(CUSTOM_MARKER))
				continue;
			
			t.append(FILE_FORMAT.get(i).field_name);
			
			if(i < FILE_FORMAT.size()-1)
				t.append(",");
		}
		
		QUERY = t.toString() + " from "+export_props.getProperty("viewname",true)+
							   " where "+export_props.getProperty("ackcol",true)+" is null" +
							   " order by export_id,question_alias asc;";
		GhsLog.finer("Query initialized: " + QUERY);
	}
	
	//////////////////////////////////////////////////////////////////
	//Helper functions to obtain data from database
	//////////////////////////////////////////////////////////////////
	/**
	 * Uses the ResultSet from the SQL query to create a list of GHSDao 
	 * objects to represent the data extract.
	 * 
	 * @param rs - ResultSet from an SQL query.
	 * @throws SQLException if there is a problem with the query or database.
	 * 
	 */
	private void setUpQueryResults(ResultSet rs) throws Exception {
		try{
			GHSDao tmp;
			while(rs.next()){
				tmp = new GHSDao();
				for(int i = 0; i < FILE_FORMAT.size(); i++){
					String FieldToSet = FILE_FORMAT.get(i).field_name;
					
					if(FieldToSet.contains(CUSTOM_MARKER))
						continue;
					
					String ValueToSet = rs.getString(FILE_FORMAT.get(i).field_name);
					
					if(ValueToSet=="null") 
						ValueToSet = "";
					
					setValueByName(tmp,FieldToSet,ValueToSet);
				}
				theFile.add(tmp);
			}
		}catch(Exception e){
			GhsLog.severe("Unknown exception caught in setFile(): " + e.getClass() + ": " + e.getMessage());
			throw new RuntimeException("Unknown error in setFile() function.",e);
		}
		GhsLog.fine("Query results converted to Array format.");
	}

	//////////////////////////////////////////////////////////////////
	//Helper functions to send data to cloverleaf.
	//////////////////////////////////////////////////////////////////
	private void InitializeProcessing(){
		String hostName = export_props.getProperty("host",true);
		int port  		= Integer.parseInt(export_props.getProperty("port",true));
		int Timeout_ms  = Integer.parseInt(export_props.getProperty("timeout",true));
		
		GhsLog.fine("Opening socket: " + hostName + ":" + port);
		try{
			sock = new pxSocket(hostName,port,Timeout_ms);
		} catch(RuntimeException e){
			System.out.println("Error establishing connection to CloverLeaf: " + e.getMessage());
			throw e;
		}
	}
	private void CloseProcessing(){
		sock.close();
		conn.close();
	}
		
	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	public ArrayList<String> ProcessData() throws RuntimeException{
		try {
			setUpQueryResults(conn.Query(QUERY));
		} catch (SQLException e) {
			throw new RuntimeException("SQL Error: " + e.getMessage(),e);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		
		if(theFile.isEmpty()){
			GhsLog.fine("No records to export.");
			return new ArrayList<String>();
		}

		GhsLog.fine("Query completed. Processing data beginning.");

		ArrayList<String> processingErrors = new ArrayList<String>();
		try{
			InitializeProcessing();
		} catch(RuntimeException e){
			throw e;
		}
		
		String currentSession           = null;
		String exportID                 = null;
		StringBuffer processedRowBuffer = new StringBuffer("");
		Iterator<GHSDao> i              = theFile.iterator();
		int questionsAdded              = 0;
		
		while(i.hasNext()){
			GHSDao currentRow = (GHSDao) i.next();

			if(currentSession==null || !currentSession.equals(currentRow.session_id)){
				if(exportID!=null){
					GhsLog.fine("Sending data to Cloverleaf for Export ID = " + exportID);
					appendPaddingForBlanks(processedRowBuffer,questionsAdded);
					SendToCL(processedRowBuffer,exportID,processingErrors,currentSession);
				}
				currentSession = currentRow.session_id;
				exportID       = currentRow.export_id;
				questionsAdded = 1;

				processedRowBuffer.append(getProcessedData(processedRowBuffer, new Integer(questionsAdded).toString(), currentRow,"all"));				
			}
			else{
				questionsAdded++;
				if(questionsAdded > NUM_QUESTIONS) 
					continue;
				else
					processedRowBuffer.append(getProcessedData(processedRowBuffer, new Integer(questionsAdded).toString(), currentRow,"line"));
			}
		}
		appendPaddingForBlanks(processedRowBuffer, questionsAdded);
		GhsLog.fine("Sending data to Cloverleaf for Export ID = " + exportID);
		SendToCL(processedRowBuffer,exportID,processingErrors,currentSession);

		CloseProcessing();
		
		GhsLog.fine("Data processing complete.");

		return processingErrors;
	}
	private String getProcessedData(StringBuffer processedRowBuffer,String questionsAdded, GHSDao currentRow,String dataToProcess) {
		String rtn;
		HashMap<String,String> customVals = new HashMap<String,String>();
		customVals.put("set_id",questionsAdded);
		if(dataToProcess.toLowerCase().equals("all"))
			rtn = currentRow.toString_all(customVals);
		else if(dataToProcess.toLowerCase().equals("line"))
			rtn = currentRow.toString_line(customVals);
		else
			rtn = null;
		
		return rtn;
	}
	private void appendPaddingForBlanks(StringBuffer theRow, int questionsAdded) {
		if(questionsAdded >= NUM_QUESTIONS)
			return;
		theRow.append(repeatChar(" ",QUESTIONS_LENGTH*(NUM_QUESTIONS-questionsAdded)));
	}
	
	private void SendToCL(StringBuffer process_row,String exportID,ArrayList<String> errors,String current_session){
		if(process_row.length()!=0){
			try {
				ProcessRow(process_row.toString());
				if(UpdateRecord(exportID,"Y")==0)
				{
					errors.add("Unable to update acknowledgement status for Session ID " + current_session + ".");
					errors.add(process_row.toString());
				}
			} catch (AcknowledgmentException e) {
				errors.add("Error: No acknowledgement received for Session ID " + current_session + ".");
				errors.add(process_row.toString());
				try{
					UpdateRecord(exportID,"N");
				} catch(SQLException e2){
					errors.add("Error marking record as unacknowledged (Session ID " +current_session+"): " + e2.getMessage());
					errors.add(process_row.toString());
				}
			} catch(SQLException e){
				errors.add("Error marking record as processed (Session ID " +current_session+"): " + e.getMessage());
				errors.add(process_row.toString());
			}
			GhsLog.fine("Staging table updated successfully for Export ID " + exportID);
			process_row.delete(0, process_row.length());
		}
	}

	private void setValueByName(GHSDao tmp, String field_name,String value) throws NoSuchFieldException,IllegalAccessException,SQLException {
		if(tmp == null){
			throw new RuntimeException("Error: Attempting to set a value from a null object: setValueByName(null,"+field_name+","+value+");");
		}
		@SuppressWarnings("rawtypes")
		Class c = tmp.getClass();
		Field f = c.getField(field_name);
		f.set(tmp,value);
		GhsLog.finest("Field set ("+field_name+" = "+value+").");
	}

	/**
	 * This function will transmit the data from this data file to Cloverleaf. Upon receiving an acknowledgement, 
	 * this function will return. If no acknowledgement is received, or if the acknowledgement is invalid (any two-byte 
	 * sequence other than 0x%02x), an AcknowledgmentException will be thrown.
	 * 
	 * @param theRecord The string record that is already in FRL format and ready to send to cloverleaf.
	 * @throws AcknowledgementException If Cloverleaf does not acknowledge receipt of a record.
	 * @throws SQLException 
	 */
	private void ProcessRow(String theRecord) throws AcknowledgmentException {
		GhsLog.finer("Sending data to Cloverleaf. Full text: : " + padZeros(theRecord.length(),6)+theRecord);
		sock.sendMsg(padZeros(theRecord.length(),6)+theRecord);
		
		if(!sock.recvAck()){//no acknowledgment, or acknowledged with incorrect sequence 
			throw new AcknowledgmentException("No acknowledgement received from Cloverleaf.");
		}
	}

	private int UpdateRecord(String export_id,String Ack_Status) throws SQLException{
		if(export_id==null) throw new SQLException();

		//set the processing date for acknowledged records
		String sql = "update STAGING "+
		             "SET "+export_props.getProperty("timecol",true)+"=?," +
		                 export_props.getProperty("ackcol",true)+"=? " +
		             "WHERE EXPORT_ID=?;";
		GhsLog.fine("Attempting to update staging table for Export ID " + export_id);
		return conn.Update(sql,datetime_format.format(new Date()),Ack_Status,export_id);
	}

	private String padZeros(Integer num,int length_needed){
		StringBuffer output = new StringBuffer(num.toString());
		for(int i = num.toString().length(); i<length_needed; i++){
			output.insert(0,0);
		}
		return output.toString();
	}
	
	private String repeatChar(String charToRepeat,int numRepetitions){
		StringBuffer rtn = new StringBuffer("");
		
		for(int i = 0; i < numRepetitions; i++){
			rtn.append(charToRepeat);
		}
		
		return rtn.toString();
	}
}
