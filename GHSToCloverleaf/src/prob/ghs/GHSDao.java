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
		Format f = new Format();
		sb.append(f.format(session_id,11))
		  .append(f.format(message_id,19))
		  .append(f.format(start_date,8))
		  .append(f.format(start_time,6))
		  .append(f.format(end_time,6))
		  .append(f.format(start_datetime,14))
		  .append(f.format(end_datetime,14))
		  .append(f.format(facility_id,10))
		  .append(f.format(aid,7))
		  .append(f.format(oriented,1))
		  .append(f.format(mpdj,11))
		  .append(f.format(minor_lastname,80))
		  .append(f.format(minor_firstname,80))
		  .append(f.format(minor_middlename,80))
		  .append(f.format(minor_dob,8))
		  .append(f.format(admin_lastname,80))
		  .append(f.format(admin_firstname,80))
		  .append(f.format(admin_middlename,80))
		  .append(toString_line(custom));
		return sb.toString();
	}
	public String toString_line(HashMap<String,String> custom){
		StringBuilder sb = new StringBuilder();
		Format f = new Format();
		sb.append(f.format(question_alias,11))
		  .append(f.format(custom.get("set_id"),3))
		  .append(f.format(question_response,200))
		  .append(f.format(question_scale,80));
		return sb.toString();
	}
	
	public class Format {
		public String format(String StringToFormat, int NewLength, char PaddingCharacter, char Pad_Direction){
			StringBuffer rslt;
			if(StringToFormat == null)
				rslt = new StringBuffer("");
			else
				rslt = new StringBuffer(StringToFormat.substring(0,Math.min(NewLength,StringToFormat.length())));
			
			if(rslt.length() < NewLength){
				int numToAdd = NewLength - rslt.length();
				for(int i = 0; i < numToAdd; i++){
					if(Pad_Direction=='L'){
						rslt.insert(0,PaddingCharacter);
					}
					else if(Pad_Direction=='R'){
						rslt.append(PaddingCharacter);
					}
				}
			}
			
			return rslt.toString();
		}
		public String format(String StringToFormat, int NewLength, String PaddingCharacter, String Pad_Direction){
			return format(StringToFormat,NewLength,PaddingCharacter.charAt(0),Pad_Direction.charAt(0));
		}
		public String format(String StringToFormat,int NewLength){
			return format(StringToFormat,NewLength,' ','R');
		}
	}
}
