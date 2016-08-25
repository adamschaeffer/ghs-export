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
import java.util.Iterator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.naming.NamingException;

import prob.ghs.beans.ResponseBean;
import prob.ghs.beans.SessionBean;
import prob.ghs.exception.AcknowledgmentException;
import prob.ghs.FRLField;
import prob.pix.pxSocket;
import prob.util.DBConnection;
import prob.util.Property_Set;

public class GHSExtract {
	private static final Logger GhsLog                = Logger.getLogger(GHSExtract.class.getName());
	private static final Level loglevel               = Level.FINER;
	
	private static final String CUSTOM_MARKER         = "**CUSTOM**"; 
	
	private ArrayList<FRLField> FILE_FORMAT;
	private String QUERY;
	private pxSocket sock                             = null;
	private Property_Set export_props                 = null;
	private DBConnection conn                         = null;
	private ArrayList<SessionAndResponseData> theFile = new ArrayList<SessionAndResponseData>();

	private SimpleDateFormat datetime_format          = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private class DbRow {
		public SessionBean session;
		public ResponseBean response;
		
		public DbRow(){
			session = new SessionBean();
			response = new ResponseBean();
		}
	}

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

	public GHSExtract(Property_Set props,String jndi_name) throws NamingException, SQLException {
		conn = new DBConnection(jndi_name);
		initialize(props);
	}
	public GHSExtract(Property_Set props,String host,String driver,String username,String password){
		conn = new DBConnection(host,driver,username,password);
		initialize(props);
	}
	
	private void initialize(Property_Set props) {
		export_props = props;
		
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
		FILE_FORMAT.add(new FRLField("question",200,"R"," ",true,false));
		
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

	public ArrayList<String> ProcessData(){
		getDataFromDB();
		
		if(theFile.isEmpty()){
			GhsLog.fine("No records to export.");
			return new ArrayList<String>();
		}

		GhsLog.fine("Query completed. Processing data beginning.");

		ArrayList<String> processingErrors = new ArrayList<String>();
		initializeProcessing();
		
		Iterator<SessionAndResponseData> iterator = theFile.iterator();
		while(iterator.hasNext()){
			SessionAndResponseData data = iterator.next();
			data.setCustomValue("export_type",export_props.getProperty("title",true).toUpperCase().substring(0,3));
			try{
				processRow(data);
			} catch(Exception e){
				processingErrors.add("Session " + data.getSessionID() + ", Error: " + e.getMessage());
			}			
			try {
				data.CreateWordDocument(new FileOutputStream("testpoi_"+data.getSessionID()+".docx"));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e.getClass() + ": " + e.getMessage());
			} catch (IOException e) {
				throw new RuntimeException(e.getClass() + ": " + e.getMessage());
			}
		}
		closeProcessing();

		GhsLog.fine("Data processing complete.");

		return processingErrors;
	}
	
	private void getDataFromDB() {
		try {
			ResultSet exportDataSet = conn.Query(QUERY);
			loadDataIntoList(exportDataSet);
		} catch (SQLException e) {
			throw new RuntimeException("SQL Error: " + e.getMessage(),e);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(),e);
		}	
	}
	private void loadDataIntoList(ResultSet rs) throws Exception {
		try{
			SessionAndResponseData currentSessionData = new SessionAndResponseData(new Integer(export_props.getProperty("numquestions",false)));
			currentSessionData.setCustomValue("format",export_props.getProperty("format",false));
			
			while(rs.next()){
				DbRow thisRow = getDbRow(rs);
				
				if(!currentSessionData.isSameSession(thisRow.session)){
					theFile.add(currentSessionData);
					currentSessionData = new SessionAndResponseData(new Integer(export_props.getProperty("numquestions",false)));
					currentSessionData.setCustomValue("format",export_props.getProperty("format",false));
				}

				currentSessionData.addSessionData(thisRow.session);
				currentSessionData.addResponseData(thisRow.response);
			}
			theFile.add(currentSessionData);
		}catch(Exception e){
			GhsLog.severe(e.getClass() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}
		GhsLog.fine("Query results converted to Array format.");
	}
	
	private DbRow getDbRow(ResultSet rs){
		DbRow row = null;
		try{
			row = getRowBeans(rs);
		} catch(SQLException e){
			throw new RuntimeException("SQL Error: " + e.getMessage());
		} catch(NoSuchFieldException e){
			throw new RuntimeException("Attempting to set a field that does not exist: " + e.getMessage());
		} catch(IllegalAccessException e){
			throw new RuntimeException("Illegal access attempt: " + e.getMessage());
		}
		return row;
	}
	
	private DbRow getRowBeans(ResultSet rs) throws SQLException, NoSuchFieldException, IllegalAccessException{
		DbRow beans = new DbRow();
		String FieldToSet;
		String ValueToSet;
		
		Iterator<FRLField> iterator = FILE_FORMAT.iterator();
		while(iterator.hasNext()){
			FRLField thisField = iterator.next();
			
			if(thisField.field_name.contains(CUSTOM_MARKER))
				continue;

			FieldToSet = thisField.field_name;
			ValueToSet = rs.getString(FieldToSet);
			
			if(ValueToSet=="null")
				ValueToSet = "";
			
			if(thisField.lineitem)
				setValueByName(beans.response,FieldToSet,ValueToSet);
			else
				setValueByName(beans.session,FieldToSet,ValueToSet);
			
		}
		return beans;
	}

	private <T> void setValueByName(T objectToSetValueOf, String field_name,String value) 
		throws NoSuchFieldException,IllegalAccessException,SQLException {
		if(objectToSetValueOf == null){
			throw new RuntimeException("Error: Attempting to set a value from a null object: setValueByName(null,"+field_name+","+value+");");
		}
		@SuppressWarnings("rawtypes")
		Class c = objectToSetValueOf.getClass();
		Field f = c.getField(field_name);
		f.set(objectToSetValueOf,value);
		GhsLog.finest("Field set ("+field_name+" = "+value+").");
	}
	
	private void initializeProcessing(){
		String hostName = export_props.getProperty("host",true);
		int port  		= Integer.parseInt(export_props.getProperty("port",true));
		int Timeout_ms  = Integer.parseInt(export_props.getProperty("timeout",true));
		
		GhsLog.fine("Opening socket: " + hostName + ":" + port);
		openSocket(hostName,port,Timeout_ms);
	}
	
	private void openSocket(String hostname, int port, int timeout){
		try{
			sock = new pxSocket(hostname,port,timeout);
		} catch(RuntimeException e){
			System.out.println("Error establishing connection to CloverLeaf: " + e.getMessage());
			throw e;
		}
	}
	
	private void closeProcessing(){
		sock.close();
		conn.close();
	}
	
	private void processRow(SessionAndResponseData process_row) throws SQLException{
		try {
			sendToCloverleaf(process_row.toString());
			updateRecord(process_row.getExportID(),"Y");
		} catch (AcknowledgmentException e) {
			try{
				updateRecord(process_row.getExportID(),"N");
			} catch(SQLException ex){
				throw new RuntimeException("Unable to mark Session ID "+process_row.getSessionID()+" as not acknowledged.",ex);
			}
		} catch(SQLException e){
			throw new RuntimeException("Unable to mark Session ID "+process_row.getSessionID()+" as acknowledged.",e);
		} catch(NullPointerException e){
			throw new RuntimeException("Null pointer exception found when converting to FRL format.",e);
		}
		GhsLog.fine("Staging table updated successfully for Export ID " + process_row.getSessionID());
	}

	/**
	 * This function will transmit the data from this data file to Cloverleaf. Upon receiving an acknowledgement, 
	 * this function will return. If no acknowledgement is received, or if the acknowledgement is invalid , an 
	 * AcknowledgmentException will be thrown.
	 * 
	 * @param theRecord The string record that is already in FRL format and ready to send to cloverleaf.
	 * @throws AcknowledgementException If Cloverleaf does not acknowledge receipt of a record.
	 */
	private void sendToCloverleaf(String theRecord) throws AcknowledgmentException {
		GhsLog.finer("Sending data to Cloverleaf. Full text: " + padZeros(theRecord.length(),6)+theRecord);
		sock.sendMsg(padZeros(theRecord.length(),6)+theRecord + "\r\n");
		
		if(!sock.recvAck()){//no acknowledgment, or acknowledged with incorrect sequence 
			throw new AcknowledgmentException("No acknowledgement received from Cloverleaf.");
		}
	}

	private int updateRecord(String export_id,String Ack_Status) throws SQLException{
		if(export_id==null) throw new SQLException();

		//set the processing date for acknowledged records
		String sql = "update STAGING "+
		             "SET "+export_props.getProperty("timecol",true)+"=?," +
		                    export_props.getProperty("ackcol",true)+"=? " +
		             "WHERE EXPORT_ID=?;";
		GhsLog.fine("Updating staging table for Export ID " + export_id);
		return conn.Update(sql,
						   datetime_format.format(new Date()),
						   Ack_Status,
						   export_id);
	}

	private String padZeros(Integer num,int length_needed){
		StringBuffer output = new StringBuffer(num.toString());
		for(int i = num.toString().length(); i<length_needed; i++){
			output.insert(0,0);
		}
		return output.toString();
	}
}
