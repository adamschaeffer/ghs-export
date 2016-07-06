package prob.ghs;

import java.util.ArrayList;
import java.util.List;

import prob.ghs.beans.ResponseBean;
import prob.ghs.beans.SessionBean;
import prob.ghs.exception.SessionMismatchException;

public class SessionAndResponseData {
	private static final int NUM_QUESTIONS = 148;
	private static final int RESPONSE_LENGTH = 294;

	SessionBean session = null;
	List<ResponseBean> responses = new ArrayList<ResponseBean>();	
	
	SessionAndResponseData(){}
	
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
		
		outputString.append(session.toString());

		for(int i = 0; i < responses.size(); i++){
			ResponseBean thisResponse = responses.get(i);
			thisResponse.setCustomValue("set_id",new Integer(i+1).toString());
			
			outputString.append(thisResponse.toString());
		}
		
		outputString.append(getBlankResponsePadding());
		
		return outputString.toString();
	}
	
	private String getBlankResponsePadding(){
		if(responses.size() >= NUM_QUESTIONS)
			return "";

		StringBuffer rtn = new StringBuffer("");
		for(int i = 0; i < (NUM_QUESTIONS-responses.size()) * RESPONSE_LENGTH; i++){
			rtn.append(" ");
		}
		
		return rtn.toString();
	}
}
