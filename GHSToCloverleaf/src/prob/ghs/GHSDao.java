package prob.ghs;

import java.util.HashMap;

/**
 * Getters/Setters are not used, as that would require more maintenance of the code. Reflection is used
 * to set the values of these fields, so that I can change the FILE_FORMAT variable of GHSExtract, and do
 * not need to update this class at all.
 * 
 * @author ASchaeffer
 */

public class GHSDao{
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
	public String question_alias;
	public String set_id;
	public String question_response;
	public String question_scale;
	public String question;

	public String toString_all(HashMap<String,String> custom){
		StringBuilder sb = new StringBuilder();
		sb.append(toString_head(custom))
		  .append(toString_line(custom));
		return sb.toString();
	}
	public String toString_head(HashMap<String,String> custom){
		StringBuilder sb = new StringBuilder();
		sb.append(prob.ghs.beans.Format.format(session_id,11))
		  .append(prob.ghs.beans.Format.format(message_id,19))
		  .append(prob.ghs.beans.Format.format(start_date,8))
		  .append(prob.ghs.beans.Format.format(start_time,6))
		  .append(prob.ghs.beans.Format.format(end_time,6))
		  .append(prob.ghs.beans.Format.format(start_datetime,14))
		  .append(prob.ghs.beans.Format.format(end_datetime,14))
		  .append(prob.ghs.beans.Format.format(facility_id,10))
		  .append(prob.ghs.beans.Format.format(aid,7))
		  .append(prob.ghs.beans.Format.format(oriented,1))
		  .append(prob.ghs.beans.Format.format(mpdj,11))
		  .append(prob.ghs.beans.Format.format(minor_lastname,80))
		  .append(prob.ghs.beans.Format.format(minor_firstname,80))
		  .append(prob.ghs.beans.Format.format(minor_middlename,80))
		  .append(prob.ghs.beans.Format.format(minor_dob,8))
		  .append(prob.ghs.beans.Format.format(admin_lastname,80))
		  .append(prob.ghs.beans.Format.format(admin_firstname,80))
		  .append(prob.ghs.beans.Format.format(admin_middlename,80));
		return sb.toString();
	}
	public String toString_line(HashMap<String,String> custom){
		StringBuilder sb = new StringBuilder();
		sb.append(prob.ghs.beans.Format.format(question_alias,11))
		  .append(prob.ghs.beans.Format.format(custom.get("set_id"),3))
		  .append(prob.ghs.beans.Format.format(question_response,200))
		  .append(prob.ghs.beans.Format.format(question_scale,80));
		return sb.toString();
	}
}
