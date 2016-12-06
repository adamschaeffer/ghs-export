package prob.ghs.beans;

import java.util.HashMap;

import prob.ghs.beans.Format;
import prob.util.Counter;
import prob.util.PHPSerialization;

public class ResponseBean {
	Counter set_id_factory;
	
	public String question_alias;
	public String set_id;
	public String question_response;
	public String question;
	public String question_scale;
	public String question_config;
	
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
			  .append(Format.format(question,200)) //TODO: RESPONSE LENGTH
			  .append(Format.format("",80));
			}
		
		String scale_prefix = "";
		if(!question_scale.isEmpty()){
			scale_prefix = "Alert: " + question_scale + " ";
		}
		
		//Question response
		sb.append(Format.format(question_alias,11))
		  .append(Format.format(new Integer(set_id_factory.next()).toString(),3))
		  .append(Format.format(scale_prefix + question_response,200))//TODO: RESPONSE LENGTH
		  .append(Format.format("",80));
		
		return sb.toString();
	}
	public ResponseBean convertResponse() {
		Character rowDelimiter = new Character((char)10);
		
		@SuppressWarnings("unchecked")
		HashMap<String,StringBuffer> map = (HashMap<String,StringBuffer>) PHPSerialization.unserialize(question_config);
		StringBuffer values = map.get("items");
		
		if(values == null){
			return this;
		}
		
		String[] valueList = values.toString().split(rowDelimiter.toString());
		String[] respList = question_response.split("~");
		
		for(int i = 0; i < respList.length; i++){
			for(int j = 0; j < valueList.length; j++){
				String[] codeResponseList = valueList[j].split("[|]");
				if(codeResponseList[0].equals(respList[i])){
					respList[i] = codeResponseList[1];
				}
			}
		}
		question_response = unSplitResponses(respList);
		return this;
	}
	private String unSplitResponses(String[] respList) {
		StringBuilder response_list = new StringBuilder("");
		
		for(int i = 0; i < respList.length; i++){
			response_list.append(respList[i]);
			if(i<respList.length-1){
				response_list.append("~");
			}
		}
		
		return response_list.toString();
	}
}
