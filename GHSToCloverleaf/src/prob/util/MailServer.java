package prob.util;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailServer {
	Properties props = null;
	String username = null,password=null;
	Session session = null;
	
	//Constructors
	public MailServer(){
		props = System.getProperties();
		props.setProperty("java.net.preferIPv4Stack","true");
		props.setProperty("mail.smtp.starttls.enable","true");
		props.setProperty("mail.transport.protocol","smtp");

	}
	public MailServer(String host,String port){
		this();
		props.setProperty("mail.smtp.host",host);
		props.setProperty("mail.smtp.port",port);
	}
	public MailServer(String host,String port,String username,String password){
		this(host,port);
		setCredentials(username,password);
	}
	
	//setter methods
	public void setHost(String host){
		props.setProperty("mail.smtp.host",host);
	}
	public void setPort(String port){
		props.setProperty("mail.smtp.port",port);
	}
	public void setCredentials(String username,String password){
		props.setProperty("mail.smtp.auth","true");
		this.username = username;
		this.password = Encrypt.encrypt(password);
	}
	
	//internal functions
	private Session getSession(){
		if(session!=null) 
			return session;
		else if(props.getProperty("mail.smtp.auth")=="true"){
			session = Session.getDefaultInstance(props,new Authenticator(){ protected PasswordAuthentication getPasswordAuthentication(){
				return new PasswordAuthentication("adam.schaeffer@probation.lacounty.gov",Encrypt.decrypt(password));
				}
			});
		}
		else
			session = Session.getDefaultInstance(props);
		
		return session;
	}
	
	//public functions
	public void SendMsg(String from,String to,String subject,String body){
		try{
			MimeMessage message = new MimeMessage(getSession());
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(subject);
			message.setText(body);
			Transport.send(message);
		} catch(MessagingException e){
			throw new RuntimeException("Error sending email: " + e.getMessage());
		}
	}
}
