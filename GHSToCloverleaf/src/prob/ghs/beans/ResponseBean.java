package prob.ghs.beans;

import java.util.HashMap;

import prob.ghs.beans.Format;
import prob.util.Counter;

public class ResponseBean {
	Counter set_id_factory;
	
	public String question_alias;
	public String set_id;
	public String question_response;
	public String question_scale;
	public String question;
	
	private HashMap<String,String> customValues = new HashMap<String,String>();
	
	public void setCustomValue(String key,String value){
		customValues.put(key, value);
	}
	public void setCounter(Counter set_id_factory){
		this.set_id_factory = set_id_factory;
	}
	
	@Override
	public String toString(){
		String format = (String) customValues.get("format");
		
		StringBuilder sb = new StringBuilder();
		
		if(format.toLowerCase().equals("expanded")){
			//Question text
			sb.append(Format.format(question_alias,11))
			  .append(Format.format(new Integer(set_id_factory.next()).toString(),3))
			  .append(Format.format(question,200))
			  .append(Format.format(question_scale,80));
		}
		
		//Question response
		sb.append(Format.format(question_alias,11))
		  .append(Format.format(new Integer(set_id_factory.next()).toString(),3))
		  .append(Format.format(question_response,200))
		  .append(Format.format(question_scale,80));
		
		return sb.toString();
	}
}
