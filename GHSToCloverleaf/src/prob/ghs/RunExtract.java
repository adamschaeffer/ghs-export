package prob.ghs;

/**
 * A simple class for the purpose of running the GHSExtract and sending the data to cloverleaf.
 * 
 * @author ASchaeffer
 */


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.text.MessageFormat;

import prob.ghs.GHSExtract;
import prob.util.DBConnection;
import prob.util.Property_Set;
import prob.util.Encrypt;
import prob.util.MailServer;

@Path("/")
public class RunExtract {
	private static Logger l = Logger.getLogger(RunExtract.class.getName());
	
	@Path("Test")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public static String HelloWorld(){
		return "Hello, World!";
	}
	
	@Path("Send174")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public static String resend174_all(){
		String rtn = "Error";
		try{
			rtn = resend_test(new int[] {174},new String[] {"all"});
		}
		catch(Exception e){
			return rtn;
		}
		return rtn;
	}
	@Path("Send152")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public static String resend152_all(){
		String rtn = "Error";
		try{
			rtn = resend_test(new int[] {152},new String[] {"all"});
		}
		catch(Exception e){
			return rtn;
		}
		return rtn;
	}
	
	public static String resendCustom(int[] records, String[] types){
		String rtn = "Error";
		try{
			rtn = resend_test(records,types);
		}
		catch(Exception e){
			return rtn;
		}
		return rtn;
	}

	public static String resend_test(int[] recordsToSend,String[] typesToSend){
		DBConnection db = null;
		Property_Set prop = new Property_Set("ghs_db");
		
		String driver = prop.getProperty("driver", false);
		
		try{
			if(driver==null){
				db = new DBConnection(prop.getProperty("url", true),l);
			}
			else{
				db = new DBConnection(prop.getProperty("url", true),
								      prop.getProperty("driver",false),
								      prop.getProperty("username",false),
								      prop.getProperty("password",false),l);
			}

			StringBuilder whereClause = new StringBuilder(" where ");
			for(int i = 0; i < recordsToSend.length; i++){
				whereClause.append("sid=").append(recordsToSend[i]);
				
				if(i != recordsToSend.length-1){
					whereClause.append(" or ");
				}
			}
			
			StringBuilder setClause = new StringBuilder();
			for(int i = 0; i < typesToSend.length; i++){
				if(typesToSend[i] == "all"){
					setClause.append("ack_all=null");
				}
				else if(typesToSend[i] == "dhs"){
					setClause.append("ack_dhs=null");
				}
				else if(typesToSend[i] == "prob"){
					setClause.append("ack_prob=null");
				}
				else if(typesToSend[i] == "dmh"){
					setClause.append("ack_dmh=null");
				}
				
				if(i!=typesToSend.length-1){
					setClause.append(',');
				}
			}
			
			String query = MessageFormat.format("update staging set "+setClause.toString()+"{0};",whereClause.toString());
			
			db.Update(query);
		} catch (NamingException e) {
			return e.getClass() + ": " + e.getMessage();
		} catch (SQLException e) {
			return e.getClass() + ": " + e.getMessage();
		} catch (RuntimeException e) {
			return e.getClass() + ": " + e.getMessage();
		} finally {
			if(db!=null)
				db.close();
		}
		return "Success";
	}

	public static String ExportGHS(String type,Logger log){
		l = log;
		
		ArrayList<String> errors = null;
		GHSExtract frlFile 		 = null;
		Property_Set prop 		 = new Property_Set("ghs_db");
		
		l.log(Level.FINE,"{0}: Export starting.",type);
		
		String driver = prop.getProperty("driver", false);

		try{
			if(driver==null){
				l.log(Level.FINEST,"{0}: Driver null. Using JNDI.",type);
				frlFile = new GHSExtract(new Property_Set(type),prop.getProperty("url", true),log);
			}
			else{
				l.log(Level.FINEST,"{0}: Loading driver.",type);
				frlFile = new GHSExtract(new Property_Set(type),
										 prop.getProperty("url", true),
										 prop.getProperty("driver",false),
										 prop.getProperty("username",false),
										 prop.getProperty("password",false),log);
			}
			l.log(Level.FINEST,"{0}: Starting processing.",type);
			errors = frlFile.ProcessData();

			if(errors == null){
				l.log(Level.FINE,"{0}: No data to export.",type);
				return "No data to export.";
			}

			l.log(Level.FINER,"{0}: Processing complete. Beginning status email.",type);
			
			StringBuffer theEmailBody;
			String theEmailSubject;
			if(!errors.isEmpty()){
				theEmailSubject = "GHS Export Errors - " + type + " Export";
				theEmailBody = new StringBuffer("There were errors present when the GHS system data was exported for processing. A summary of those errors is as follows: \n\n");
				for(int i = 0; i < errors.size(); i++){
					theEmailBody.append(errors.get(i)).append("\n");
				}
			}
			else{
				theEmailSubject = "GHS Export Results - " + type + " Export";
				theEmailBody = new StringBuffer("GHS Export completed successfully with no errors.");
			}

			final Property_Set mailProps = new Property_Set("mailserv"); 
	 		String to   = "adam.schaeffer@probation.lacounty.gov";
			String from = "adam.schaeffer@probation.lacounty.gov";

			String hostname = mailProps.getProperty("host",true),
				   port     = mailProps.getProperty("port",true),
				   username = mailProps.getProperty("username",true),
				   password = mailProps.getProperty("password",true);

			try{
				MailServer mail = new MailServer(hostname,port,username,Encrypt.decrypt(password));
				mail.SendMsg(from, to, theEmailSubject,theEmailBody.toString());
			} catch (RuntimeException e){
				Object[] params = {type,e.getMessage()};
				l.log(Level.SEVERE,"{0}: Error sending status email: {1}",params);
				System.out.println(e.getMessage());
			} catch (Exception e){
				Object[] params = {type,e.getClass(),e.getMessage()};
				l.log(Level.SEVERE,"{0}: Error sending status email: {1} - {2}",params);
				System.out.println(e.getMessage());
			}
		}catch(RuntimeException e){
			Object[] params = {type,e.getMessage()};
			l.log(Level.SEVERE,"{0}: Error processing GHS data: {1}",params);
			System.out.println(e.getMessage());
		}catch(Exception e){
			Object[] params = {type,e.getClass(),e.getMessage()};
			l.log(Level.SEVERE,"{0}: Error processing GHS data: {1} - {2}",params);
			System.out.println(e.getClass()+": "+e.getMessage());
		}finally{
			if(frlFile!=null)
				frlFile.close();
		}

		if(errors != null && errors.isEmpty()){
			return "Export completed successfully. No errors detected.";
		}
		else if(errors != null){
			return "Export completed succesfully, with errors. Errors are as follows: "+errors.toString();
		}
		else{
			System.out.println("Export terminated abnormally. Please see log for details.");
			return "Export terminated abnormally. Please see log for details.";
		}
	}
	
	public static void main(String args[]){
//		resendCustom(new int[] {197},new String[] {"dhs"});
//		ExportGHS("all",l);
//		ExportGHS("prob",l);
//		ExportGHS("dhs",l);
//		ExportGHS("dmh",l);
	}
}
