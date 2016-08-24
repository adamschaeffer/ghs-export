package prob.ghs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

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
	
	public String CreateWordDocument(FileOutputStream out) throws IOException{
		XWPFDocument document = new XWPFDocument();
		StringBuffer printout = new StringBuffer();

		XWPFParagraph titleParagraph = document.createParagraph();
		titleParagraph.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun run = titleParagraph.createRun();
		run.setText("GHS QUESTIONNAIRE ALERT - " + customValues.get("export_type"));
		run.setBold(true);
		run.addCarriageReturn();
		
        XWPFParagraph headerParagraph = document.createParagraph();
        XWPFRun runHead = headerParagraph.createRun();
        runHead.setText("Minor: "+session.minor_lastname+", "+session.minor_firstname);
        runHead.addCarriageReturn();
        runHead.setText("Date of Birth: " + session.minor_dob);
        runHead.addCarriageReturn();
        runHead.setText("PDJ NO: "+session.mpdj);
        runHead.addCarriageReturn();
        runHead.setText("Start Date/Time: " + session.start_date + " " + session.start_time);
        runHead.addCarriageReturn();
		runHead.setText("End Date/Time: " + session.start_date + " " + session.end_time);
        runHead.addCarriageReturn();
		runHead.setText("Facility: " + session.facility_id);
        runHead.addCarriageReturn();
		runHead.setText("Provider: " + session.admin_lastname + ", " + session.admin_firstname);
        runHead.addCarriageReturn();
		runHead.setText("GHS Session ID: " + session.session_id);
        runHead.addCarriageReturn();

        XWPFParagraph questionParagraph = document.createParagraph();
		for(int i = 0; i < responses.size(); i++){
	        XWPFRun runQ = questionParagraph.createRun();

	        printout.delete(0,printout.length());
			ResponseBean response = responses.get(i);
			runQ.setText(response.question);
			runQ.addCarriageReturn();
			runQ.setText(response.question_response);
	        runQ.addCarriageReturn();
	        runQ.addCarriageReturn();
		}
		document.write(out);
		out.close();

		return printout.toString();
	}
}
