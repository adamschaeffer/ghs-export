package prob.ghs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import prob.ghs.beans.Format;
import prob.ghs.beans.ResponseBean;
import prob.ghs.beans.SessionBean;
import prob.ghs.exception.SessionMismatchException;
import prob.util.Counter;

public class SessionAndResponseData {
	private int NUM_QUESTIONS = 148;

	private SessionBean session = null;
	private List<ResponseBean> responses = new ArrayList<ResponseBean>();
	private HashMap<String,String> customValues = new HashMap<String,String>();
	private Counter setIdGenerator = new Counter(1);

	SessionAndResponseData(){}
	SessionAndResponseData(int numQuestions){
		NUM_QUESTIONS = numQuestions;
	}
	
	public void setCustomValue(String key,String value){
		customValues.put(key, value);
	}
	
	public void addSessionData(SessionBean session) throws SessionMismatchException{
		if(this.session == null){
			this.session = session;
		}

		else if(!this.session.equals(session))
			throw new SessionMismatchException();
	}
	
	public void addResponseData(ResponseBean response){
		if(response.exists.equals("yes"))
			responses.add(response);
	}
	
	public boolean isSameSession(SessionBean session){
		return this.session==null || this.session.equals(session);
	}
	
	public String getExportID(){
		return new String(session.export_id);
	}
	public String getSessionID(){
		return new String(session.session_id);
	}
	public String getPDJ() {
		return session.pdj;
	}
	public String getLocation(){
		return session.facility_id;
	}
	public String getTimestamp(boolean prettyFormat){
		if(prettyFormat){
			StringBuilder datetime = new StringBuilder("");
			
			datetime.append(session.end_datetime.substring(4,6))
					.append("/")
					.append(session.end_datetime.substring(6,8))
					.append("/")
					.append(session.end_datetime.substring(0,4))
					.append(" ")
					.append(session.end_datetime.substring(8,10))
					.append(":")
					.append(session.end_datetime.substring(10,12))
					.append(":")
					.append(session.end_datetime.substring(12,14));
					
			
			return datetime.toString();
		}
		else{
			return session.end_datetime;
		}
	}

	@Override
	public String toString(){
		StringBuilder outputString = new StringBuilder();
		String format = (String) customValues.get("format");
		
		outputString.append(session.toString());
		
		if(format.toLowerCase().equals("expanded")){
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",300)); //TODO: RESPONSE LENGTH
//						.append(Format.format(" ",80));
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",300)); //TODO: RESPONSE LENGTH
//						.append(Format.format(" ",80));
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",300)); //TODO: RESPONSE LENGTH
//						.append(Format.format(" ",80));
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",300)); //TODO: RESPONSE LENGTH
//						.append(Format.format(" ",80));
		}

		int i = 0;
		int numResponses = 0;
		for(; i < responses.size(); i++){
			ResponseBean thisResponse = responses.get(i);
			
			if(!customValues.get("export_type").toLowerCase().equals("all") && thisResponse.question_response.equals("No")){
				continue;
			}
			
			numResponses++;
			
			thisResponse.setCounter(setIdGenerator);
			
			Set<String> keys = customValues.keySet();
			for(Iterator<String> it = keys.iterator(); it.hasNext(); ){
				String key = it.next();
				thisResponse.setCustomValue(key,customValues.get(key));
			}			
			
			outputString.append(thisResponse.toString());
		}
		i=numResponses;
		for(; i < NUM_QUESTIONS; i++){
			outputString.append(Format.format(" ",11))
						.append(Format.format(" ",3))
						.append(Format.format(" ",300)); //TODO: RESPONSE LENGTH
//						.append(Format.format(" ",80));
			if(format.toLowerCase().equals("expanded"))
				outputString.append(Format.format(" ",11))
							.append(Format.format(" ",3))
							.append(Format.format(" ",300)); //TODO: RESPONSE LENGTH
//							.append(Format.format(" ",80));
		}
		return outputString.toString();
	}

	public void normalizeResponses() {
		int last_number = 0;
		int this_number = -1;
		
		for(int i = 0; i < responses.size(); i++){
			ResponseBean thisResponse = responses.get(i);
			thisResponse.question_response = thisResponse.question_response.trim();
			
			this_number = new Integer(thisResponse.question_alias);
			
			if(thisResponse.question_response.isEmpty()){
				thisResponse.question_response = "NOT ANSWERED";
			}

			if(this_number != last_number+1){
				for(int j = 1; j <= (this_number - last_number - 1); j++){
					addNonResponse(last_number+j,i++);
				}
			}
			last_number = this_number;
		}
		
		for(int i = last_number+1; i <= 147; i++){
			addNonResponse(i,-1);
		}
		
		for(int i = 0; i < responses.size(); i++){
			System.out.println(responses.get(i).question_alias + ": " + responses.get(i).question_response);
		}
	}
	private void addNonResponse(int alias,int position) {
		ResponseBean newBean = new ResponseBean();
		newBean.question_alias = new Integer(alias).toString();
		newBean.question_response = "N/A";
		
		if(position == -1)
			responses.add(newBean);
		else
			responses.add(position,newBean);
	}
}
