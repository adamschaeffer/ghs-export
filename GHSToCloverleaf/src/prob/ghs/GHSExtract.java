package prob.ghs;

/**
 * @author ASchaeffer
 * 
 * This class will extract the GHS data (according to the QUERY variable) from the GHS database (as defined in the file
 * ghs.properties), format the data into an FRL format (according to the FILE_FORMAT variable), and send the data to 
 * cloverleaf (as defined in the file ghs.properties). A log is created in GHS_Export_Log.txt, at the level defined by 
 * the variable logLevel.
 * 
 * If there are any changes to the export file, note that changes to the format of the fields should be made within the
 * toString() methods of the classes ResponseBean, SessionBean, and SessionAndResponseData. Changes to which fields should
 * be included can be accomodated with changes to the FILE_FORMAT member variable of this class, as well as changes
 * to the toString() method of the classes listed above.
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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;

import prob.ghs.beans.PrintMessage;
import prob.ghs.beans.ResponseBean;
import prob.ghs.beans.SessionBean;
import prob.ghs.exception.AcknowledgmentException;
import prob.ghs.exception.SessionMismatchException;
import prob.ghs.FRLField;
import prob.pix.pxSocket;
import prob.util.DBConnection;
import prob.util.PrintJob;
import prob.util.Property_Set;
import prob.util.Printer;

public class GHSExtract {
	private Logger GhsLog;
	
	private static final String CUSTOM_MARKER         = "**CUSTOM**"; 
	
	private ArrayList<FRLField> FILE_FORMAT;
	private String QUERY;
	private pxSocket sock                             = null;
	private Property_Set export_props                 = null;
	private DBConnection conn                         = null;
	private ArrayList<PrintMessage> printMessage      = new ArrayList<PrintMessage>();
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

	public GHSExtract(Property_Set props,String jndi_name,Logger l) throws NamingException, SQLException {
		GhsLog = l;
		conn = new DBConnection(jndi_name,GhsLog);
		initialize(props);
	}
	public GHSExtract(Property_Set props,String host,String driver,String username,String password,Logger l){
		GhsLog = l;
		conn = new DBConnection(host,driver,username,password,GhsLog);
		initialize(props);
	}
	
	private void initialize(Property_Set props) {
		export_props = props;
		
		StringBuffer t = new StringBuffer("select distinct ");
		FILE_FORMAT = new ArrayList<FRLField>();
		FILE_FORMAT.add(new FRLField("export_id").skipField(true));
		FILE_FORMAT.add(new FRLField("session_id"));
		FILE_FORMAT.add(new FRLField("message_id"));
		FILE_FORMAT.add(new FRLField("start_date"));
		FILE_FORMAT.add(new FRLField("start_time"));
		FILE_FORMAT.add(new FRLField("end_time"));
		FILE_FORMAT.add(new FRLField("start_datetime"));
		FILE_FORMAT.add(new FRLField("end_datetime"));
		FILE_FORMAT.add(new FRLField("facility_id"));
		FILE_FORMAT.add(new FRLField("oriented"));
		FILE_FORMAT.add(new FRLField("pdj"));
		FILE_FORMAT.add(new FRLField("minor_name"));
		FILE_FORMAT.add(new FRLField(CUSTOM_MARKER+"minor_lastname").skipField(true).isCustom(true));
		FILE_FORMAT.add(new FRLField(CUSTOM_MARKER+"minor_middlename").skipField(true).isCustom(true));
		FILE_FORMAT.add(new FRLField("minor_dob"));
		FILE_FORMAT.add(new FRLField("aid"));
		FILE_FORMAT.add(new FRLField("admin_name"));
		FILE_FORMAT.add(new FRLField("question_alias").isLineitem(true));
		FILE_FORMAT.add(new FRLField("question_id").isLineitem(true));
		FILE_FORMAT.add(new FRLField(CUSTOM_MARKER+"set_id").isLineitem(true).isCustom(true));
		FILE_FORMAT.add(new FRLField("question_response").isLineitem(true));
		FILE_FORMAT.add(new FRLField("question").isLineitem(true));
		FILE_FORMAT.add(new FRLField("question_scale").isLineitem(true));
		FILE_FORMAT.add(new FRLField("question_config").isLineitem(true).skipField(true));

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

		GhsLog.log(Level.FINER,"{0}: Query initialized: " + QUERY,export_props.getProperty("title",true).toUpperCase());
	}

	public ArrayList<String> ProcessData(){
		ArrayList<String> processingErrors = new ArrayList<String>();

		GhsLog.log(Level.FINE,"{0}: Export started.",export_props.getProperty("title",true).toUpperCase());
		try{
			getDataFromDB();
		}
		catch(Exception e){
			processingErrors.add("Unable to get data from database: " + e.getMessage());
			GhsLog.log(Level.SEVERE,"{0}: Unable to get data from database: {1}",new Object[]{export_props.getProperty("title",true).toUpperCase(),e.getMessage()});
			return processingErrors;
		}
		
		if(theFile.isEmpty()){
			GhsLog.log(Level.FINE,"{0}: No records to export.",export_props.getProperty("title",true).toUpperCase());
			close();
			return null;
		}

		GhsLog.log(Level.FINE,"{0}: Query completed. Processing data beginning.",export_props.getProperty("title",true).toUpperCase());

		initializeProcessing();
		try{
			Iterator<SessionAndResponseData> iterator = theFile.iterator();
			while(iterator.hasNext()){
				SessionAndResponseData data = iterator.next();
				data.setCustomValue("export_type",export_props.getProperty("title",true).toUpperCase().substring(0,3));//substring will convert probation to prob. dhs and 3-character titles will remain 3 characters.
				try{
					processRow(data);

					String PrintableMessage = new StringBuilder("PDJ no.: ")
							            		.append(data.getPDJ())
							            		.append(": A survey has been completed on ")
							            		.append(data.getTimestamp(true))
							            		.append("\n\n").toString();
					String PrintableLocation = data.getLocation();				
				
					PrintMessage newPrintMessage = new PrintMessage(PrintableMessage,PrintableLocation);
					printMessage.add(newPrintMessage);
				} catch(Exception e){
					processingErrors.add("Session " + data.getSessionID() + ", Error: " + e.getMessage());
				}
			}
		} catch(Exception e){
			processingErrors.add("Unknown error occured. Details: " + e.getClass() + ": " + e.getMessage());
		}
		finally{
			if(!printMessage.isEmpty()){
				for(int i = 0; i < printMessage.size(); i++){
					PrintMessage thisMessage = printMessage.get(i);
					ArrayList<Printer> printerList = getPrinterList(thisMessage.location);
					PrintJob pj = new PrintJob();
					pj.setMessage(thisMessage.msg);
					pj.addPrinter(printerList);
					pj.run();
				}
			}
			close();
		}

		GhsLog.log(Level.FINE,"{0}: Data processing complete.",export_props.getProperty("title",true).toUpperCase());

		return processingErrors;
	}
	
	private void getDataFromDB() {
		try {
			loadDataIntoList();
		} catch (SQLException e) {
			throw new RuntimeException("SQL Error: " + e.getMessage(),e);
		} catch(SessionMismatchException e){
			throw new RuntimeException("Session data doesn't match. GHSExtract.loadDataIntoList(), Line 14.");
		} catch(NullPointerException e){
			throw new RuntimeException("Null pointer exception from loadDataIntoList().");
		}
	}

	private void loadDataIntoList() throws SQLException, SessionMismatchException {
		ResultSet rs = conn.Query(QUERY);
		
		if(rs==null){
			GhsLog.log(Level.SEVERE,"{0}: Unable to get data from database.",export_props.getProperty("title",true).toUpperCase());
			return;
		}
		
		if(!rs.next()){
			return;
		}
		else
			rs.beforeFirst();

		SessionAndResponseData currentSessionData = new SessionAndResponseData(new Integer(export_props.getProperty("numquestions",false)));
		currentSessionData.setCustomValue("format",export_props.getProperty("format",false));
		
		while(rs.next()){
			DbRow thisRow = null;
			thisRow = getDbRow(rs);

			if(!currentSessionData.isSameSession(thisRow.session)){
				theFile.add(currentSessionData);
				if(export_props.getProperty("doctype",true).toLowerCase().equals("all")){
					currentSessionData.normalizeResponses();
				}
				currentSessionData = new SessionAndResponseData(new Integer(export_props.getProperty("numquestions",false)));
				currentSessionData.setCustomValue("format",export_props.getProperty("format",false));
			}
		
			currentSessionData.addSessionData(thisRow.session);
			currentSessionData.addResponseData(thisRow.response.convertResponse());
		}
		if(export_props.getProperty("doctype",true).toLowerCase().equals("all")){
			currentSessionData.normalizeResponses();
		}
		theFile.add(currentSessionData);
		
		
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
		} catch(NullPointerException e){
			throw new RuntimeException("Null pointer exception from getRowBeans().");
		}
		return row;
	}

	private DbRow getRowBeans(ResultSet rs) throws SQLException, NoSuchFieldException, IllegalAccessException{
		String lineitemExists = "yes";
		DbRow beans = new DbRow();
		String FieldToSet;
		String ValueToSet;

		if(rs.getString("question_id") == null){
			lineitemExists = "no";
		}
		
		setValueByName(beans.response,"exists",lineitemExists);

		Iterator<FRLField> fieldIterator = FILE_FORMAT.iterator();
		while(fieldIterator.hasNext()){
			FRLField thisField = fieldIterator.next();
			
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

	private ArrayList<Printer> getPrinterList(String location){
		ArrayList<Printer> printerList = new ArrayList<Printer>();
		String theQuery = "SELECT DISTINCT IPADDRESS,PORT,LOCATION FROM PRINTERS WHERE UPPER(DOCTYPE)=UPPER(?) AND UPPER(LOCATION)=UPPER(?) AND IPADDRESS IS NOT NULL;";
		String docType = export_props.getProperty("doctype",false);
		
		if(docType == null)
			return null;
		try{
			ResultSet rs = conn.Query(theQuery,docType,location);
	
			while(rs.next()){
				printerList.add(new Printer(rs.getString("IPADDRESS"),rs.getInt("PORT")));
			}
		}catch(SQLException e){
			throw new RuntimeException("Error getting printer list: " + e.getMessage(),e);
		}
		
		return printerList;
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
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unable to convert UTF-8 string to byte array.",e);
		}
		GhsLog.fine("Staging table updated successfully for Export ID " + process_row.getExportID());
	}

	/**
	 * This function will transmit the data from this data file to Cloverleaf. Upon receiving an acknowledgement, 
	 * this function will return. If no acknowledgement is received, or if the acknowledgement is invalid , an 
	 * AcknowledgmentException will be thrown.
	 * 
	 * @param theRecord The string record that is already in FRL format and ready to send to cloverleaf.
	 * @throws UnsupportedEncodingException 
	 * @throws AcknowledgementException If Cloverleaf does not acknowledge receipt of a record.
	 */
	private void sendToCloverleaf(String theRecord) throws AcknowledgmentException, UnsupportedEncodingException {
		GhsLog.finer("Sending data to Cloverleaf. Full text: " + padZeros(theRecord.length(),6)+theRecord);
		String exportString = new String(padZeros(theRecord.length(),6)+theRecord);
		
		sock.sendMsg(exportString);//.getBytes("utf-8");
		
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
	
	public void close(){
		if(conn!=null)
			conn.close();
		
		if(sock!=null)
			sock.close();
	}
}
