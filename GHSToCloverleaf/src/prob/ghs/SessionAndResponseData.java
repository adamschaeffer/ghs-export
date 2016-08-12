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
	private static final int RESPONSE_LENGTH = 294;

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

	@Override
	public String toString(){
		StringBuilder outputString = new StringBuilder();
		String format = (String) customValues.get("format");
		
		outputString.append(session.toString());
		
		if(format.toLowerCase().equals("expanded")){
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",200))
						.append(Format.format(" ",80));
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",200))
						.append(Format.format(" ",80));
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",200))
						.append(Format.format(" ",80));
			outputString.append(Format.format(" ",11))
						.append(Format.format(new Integer(setIdGenerator.next()).toString(),3))
						.append(Format.format(" ",200))
						.append(Format.format(" ",80));
		}

		int i = 0;
		for(; i < responses.size(); i++){
			ResponseBean thisResponse = responses.get(i);
			thisResponse.setCounter(setIdGenerator);
			
			Set<String> keys = customValues.keySet();
			for(Iterator<String> it = keys.iterator(); it.hasNext(); ){
				String key = it.next();
				thisResponse.setCustomValue(key,customValues.get(key));
			}			
			
			outputString.append(thisResponse.toString());
		}
		for(; i < NUM_QUESTIONS; i++){
			outputString.append(Format.format(" ",11))
						.append(Format.format(" ",3))
						.append(Format.format(" ",200))
						.append(Format.format(" ",80));
			if(format.toLowerCase().equals("expanded"))
				outputString.append(Format.format(" ",11))
							.append(Format.format(" ",3))
							.append(Format.format(" ",200))
							.append(Format.format(" ",80));
		}
		return outputString.toString();
	}
	
	public String getPrintout(){
		String newline = System.getProperty("line.separator");
		StringBuffer printout = new StringBuffer("GHS QUESTIONNAIRE ALERT - ");
		printout.append(customValues.get("export_type")).append(newline).append(newline);
		
		printout.append("Minor: ").append(session.minor_lastname).append(", ").append(session.minor_firstname).append(newline);
		printout.append("Date of Birth: ").append(session.minor_dob).append(newline);
		printout.append("PDJ NO.: ").append(session.mpdj).append(newline);
		printout.append("Start Date/Time: ").append(session.start_date).append(" ").append(session.start_time).append(newline);
		printout.append("End Date/Time: ").append(session.start_date).append(" ").append(session.end_time).append(newline);
		printout.append("Facility: ").append(session.facility_id).append(newline);
		printout.append("Provider: ").append(session.admin_lastname).append(", ").append(session.admin_firstname).append(newline);
		printout.append("GHS Session ID: ").append(session.session_id).append(newline).append(newline);
		
		for(int i = 0; i < responses.size(); i++){
			ResponseBean response = responses.get(i);
			printout.append(response.question).append(newline);
			printout.append(response.question_response).append(newline).append(newline);
		}

		return printout.toString();
	}
}
