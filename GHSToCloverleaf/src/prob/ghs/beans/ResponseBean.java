package prob.ghs.beans;

import java.util.HashMap;

import prob.ghs.beans.Format;

public class ResponseBean {
	public String question_alias;
	public String set_id;
	public String question_response;
	public String question_scale;
	public String question;
	
	private HashMap<String,String> customValues = new HashMap<String,String>();
	
	public void setCustomValue(String key,String value){
		customValues.put(key, value);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(Format.format(question_alias,11))
		  .append(Format.format(customValues.get("set_id"),3))
		  .append(Format.format(question_response,200))
		  .append(Format.format(question_scale,80));
		return sb.toString();
	}
}
