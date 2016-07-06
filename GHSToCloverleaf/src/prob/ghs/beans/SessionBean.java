package prob.ghs.beans;

import prob.ghs.beans.Format;

public class SessionBean {
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
	public String mpdj;
	public String minor_lastname;
	public String minor_firstname;
	public String minor_middlename;
	public String minor_dob;
	public String admin_lastname;
	public String admin_firstname;
	public String admin_middlename;

	public SessionBean(){}

	@Override
	public String toString(){
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
		  .append(Format.format(mpdj,11))
		  .append(Format.format(minor_lastname,80))
		  .append(Format.format(minor_firstname,80))
		  .append(Format.format(minor_middlename,80))
		  .append(Format.format(minor_dob,8))
		  .append(Format.format(admin_lastname,80))
		  .append(Format.format(admin_firstname,80))
		  .append(Format.format(admin_middlename,80));
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
		
		return session_id.equals(sessionTestObject.session_id) &&
			   message_id.equals(sessionTestObject.message_id) && 
			   start_date.equals(sessionTestObject.start_date) && 
			   start_time.equals(sessionTestObject.start_time) && 
			   end_time.equals(sessionTestObject.end_time) && 
			   start_datetime.equals(sessionTestObject.start_datetime) && 
			   end_datetime.equals(sessionTestObject.end_datetime) && 
			   aid.equals(sessionTestObject.aid) &&
			   oriented.equals(sessionTestObject.oriented) && 
			   mpdj.equals(sessionTestObject.mpdj) && 
			   minor_lastname.equals(sessionTestObject.minor_lastname) && 
		   	   minor_firstname.equals(sessionTestObject.minor_firstname) && 
			   minor_middlename.equals(sessionTestObject.minor_middlename) && 
			   minor_dob.equals(sessionTestObject.minor_dob) && 
			   admin_lastname.equals(sessionTestObject.admin_lastname) && 
			   admin_firstname.equals(sessionTestObject.admin_firstname) && 
			   admin_middlename.equals(sessionTestObject.admin_middlename);
	}
}
