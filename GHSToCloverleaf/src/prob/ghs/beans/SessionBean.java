package prob.ghs.beans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import prob.ghs.beans.Format;
import prob.ghs.soap.SoapClient;
import sun.misc.BASE64Encoder;

public class SessionBean {
	public static final String minorInfoServiceUrl = "http://probwebtest001:96/WebAPI/pcms/dhs/GHS/GetDob/";
	
	public String export_id;
	public String session_id;
	public String message_id;
	public String start_date;
	public String start_time;
	public String end_time;
	public String start_datetime;
	public String end_datetime;
	public String facility_id;
	public String aid;
	public String oriented;
	public String pdj;
	public String minor_name;
	public String minor_firstname;
	public String minor_middlename;
	public String minor_lastname;
	public String minor_dob;
	public String admin_name;
	public String admin_firstname;
	public String admin_lastname;

	public SessionBean(){}

	@Override
	public String toString(){
		PopulateNames();

		StringBuilder sb = new StringBuilder();
		sb.append(Format.format(session_id,11))
		  .append(Format.format(message_id,19))
		  .append(Format.format(start_date,8))
		  .append(Format.format(start_time,6))
		  .append(Format.format(end_time,6))
		  .append(Format.format(start_datetime,14))
		  .append(Format.format(end_datetime,14))
		  .append(Format.format(facility_id,10))
		  .append(Format.format(aid,7)) 
		  .append(Format.format(oriented,1))
		  .append(Format.format(pdj.substring(1),11))
		  .append(Format.format(minor_lastname,80))
		  .append(Format.format(minor_firstname,80))
		  .append(Format.format(minor_middlename,80))
		  .append(Format.format(minor_dob,8))
		  .append(Format.format(admin_lastname,80))
		  .append(Format.format(admin_firstname,80))
		  .append(Format.format("",80));//amiddle
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object session){
		SessionBean sessionTestObject;
		
		try{
			sessionTestObject = (SessionBean) session;
		} catch(Exception e){
			return false;
		}
		
		PopulateNames();
		boolean rtn = false;
		
		rtn = session_id.equals(sessionTestObject.session_id) &&
			   message_id.equals(sessionTestObject.message_id) && 
			   start_date.equals(sessionTestObject.start_date) && 
			   start_time.equals(sessionTestObject.start_time) && 
			   end_time.equals(sessionTestObject.end_time) && 
			   start_datetime.equals(sessionTestObject.start_datetime) && 
			   end_datetime.equals(sessionTestObject.end_datetime) && 
			   oriented.equals(sessionTestObject.oriented) && 
			   pdj.equals(sessionTestObject.pdj) && 
			   minor_name.equals(sessionTestObject.minor_name) && 
			   minor_dob.equals(sessionTestObject.minor_dob);
		
		return rtn;
	}
	
	private void PopulateNames() {
		if(minor_lastname==null)
			getMinorName();
		
		if(admin_lastname==null)
			getAdminName();
	}

	private void getMinorName(){
		//get minor first, middle, last name:
		try {
			getMinorNameFromWebservice();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error obtaining minor's name: Malformed URL: " + minorInfoServiceUrl + pdj);
		} catch (IOException e) {
			throw new RuntimeException("Error obtaining minor's name: " + e.getMessage());
		}
	}
	
	private void getMinorNameFromWebservice() throws IOException{
		URL url = new URL(minorInfoServiceUrl+pdj);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept","application/json");
		String authStr = "dhs:ghs123";
		String authStrEnc = new BASE64Encoder().encode(authStr.getBytes("UTF-8")); 
		conn.setRequestProperty("Authorization",String.format("Basic %s",authStrEnc));

		if(conn.getResponseCode() != 200){
			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String output;
		String fname_seq = "\"FIRST_NAME\": \"";
		String mname_seq = "\"MIDDLE_NAME\": \"";
		String lname_seq = "\"LAST_NAME\": \"";
		
		while((output = br.readLine()) != null){
			if(output.indexOf(fname_seq)!=-1){
				int pos_start = output.indexOf(fname_seq)+fname_seq.length();
				int pos_end = output.lastIndexOf('"');
				minor_firstname = output.substring(pos_start,pos_end);
			}
			else if(output.indexOf(mname_seq)!=-1){
				int pos_start = output.indexOf(mname_seq)+mname_seq.length();
				int pos_end = output.lastIndexOf('"');
				minor_middlename = output.substring(pos_start,pos_end);
			} 
			else if(output.indexOf(lname_seq)!=-1){
				int pos_start = output.indexOf(lname_seq)+lname_seq.length();
				int pos_end = output.lastIndexOf('"');
				minor_lastname = output.substring(pos_start,pos_end);
			} 
		}
	}
	
	private void getAdminName(){
		Map<String, String> name = SoapClient.getNameFromID(aid);
		if(name != null){
			admin_firstname = name.get("firstName");
			admin_lastname = name.get("lastName");
			
		}
	}
}
