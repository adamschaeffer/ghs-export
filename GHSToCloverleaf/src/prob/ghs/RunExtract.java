package prob.ghs;

/**
 * A simple class for the purpose of running the GHSExtract and sending the data to cloverleaf.
 * 
 * @author ASchaeffer
 */

import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import prob.ghs.GHSExtract;
import prob.util.DBConnection;
import prob.util.Property_Set;
import prob.util.Encrypt;
import prob.util.MailServer;

@Path("/")
public class RunExtract {
	@Path("Export")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public static String ExportGHS(String type){
		System.out.println(type.toUpperCase() + " Export starting...");
		GHSExtract frlFile = null;
		Property_Set prop = new Property_Set("ghst");
		ArrayList<String> errors = null;
		
		String driver = prop.getProperty("driver", false);
		
		try{
			if(driver==null){
				frlFile = new GHSExtract(new Property_Set(type),prop.getProperty("url", true));
			}
			else{
				frlFile = new GHSExtract(new Property_Set(type),
										 prop.getProperty("url", true),
										 prop.getProperty("driver",false),
										 prop.getProperty("username",false),
										 prop.getProperty("password",false));
			}
			errors = frlFile.ProcessData();

			if(!errors.isEmpty()){
				final Property_Set mailProps = new Property_Set("mailserv");
		 		String to   = "adam.schaeffer@probation.lacounty.gov";
				String from = "adam.schaeffer@probation.lacounty.gov";

				String hostname = mailProps.getProperty("host",true),
					   port     = mailProps.getProperty("port",true),
					   username = mailProps.getProperty("username",true),
					   password = mailProps.getProperty("password",true);
				
				try{
					MailServer mail = new MailServer(hostname,port,username,Encrypt.decrypt(password));
					StringBuffer theEmailBody = new StringBuffer("There were errors present when the GHS system data was exported for processing. A summary of those errors is as follows: \n\n");
					for(int i = 0; i < errors.size(); i++){
						theEmailBody.append(errors.get(i)).append("\n");
					}
					mail.SendMsg(from, to, "GHS Export Errors",theEmailBody.toString());
				} catch (RuntimeException e){
					System.out.println(e.getMessage());
				}
			}
		}catch(RuntimeException e){
			System.out.println(e.getMessage());
		}catch(Exception e){
			System.out.println(e.getClass()+": "+e.getMessage());
		}

		if(errors != null && errors.isEmpty()){
			return "Export completed successfully. No errors detected.";
		}
		else if(errors != null){
			return "Export completed succesfully, with errors. Errors are as follows: "+errors.toString();
		}
		else{
			System.out.println("Export terminated abnormally. Please see log for details.");
			return "";
		}
	}
	
	@Path("SendTest")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public static String resendTestRecords(){
		DBConnection db = null;
		Property_Set prop = new Property_Set("ghst");
		
		String driver = prop.getProperty("driver", false);
		
		try{
			if(driver==null){
				db = new DBConnection(prop.getProperty("url", true));
			}
			else{
				db = new DBConnection(prop.getProperty("url", true),
								      prop.getProperty("driver",false),
								      prop.getProperty("username",false),
								      prop.getProperty("password",false));
			}
			
			db.Update("update staging set ack_all = null,ack_prob=null,ack_dhs=null,ack_dmh=null,PROCESSED_DATETIME_ALL=null,PROCESSED_DATETIME_PROB=null,PROCESSED_DATETIME_DHS=null,PROCESSED_DATETIME_DMH=null where export_id >= 12;");
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

		StringBuilder rtn = new StringBuilder("Running ALL export:<br>");
		rtn.append(ExportGHS("all")).append("<br><br>");
		rtn.append("Running Probation export:<br>").append(ExportGHS("prob")).append("<br><br>");
		rtn.append("Running DHS export:<br>").append(ExportGHS("dhs")).append("<br><br>");
		rtn.append("Running DMH export:<br>").append(ExportGHS("dmh")).append("<br><br>");
		return rtn.toString();
	}

	public static void main(String args[]){
		System.out.println(resendTestRecords());
	}
}
