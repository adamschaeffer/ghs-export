package prob.scheduling;

import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;

import prob.util.DBConnection;
import weblogic.management.timer.Timer;

public final class JobScheduler implements NotificationListener {
	private static final Level logging_level = Level.WARNING;
	
	private Timer timer;
	private static final long JOB_SCHEDULE_PERIOD = Timer.ONE_MINUTE * 5;
	
	private Integer GHSExport_ID;
		
	public JobScheduler(){
		timer = new Timer();
		timer.addNotificationListener(this,null,"Handback object?");
		
		GHSExport_ID = timer.addNotification("GHSExport","GHSExport",this,new Date(),JOB_SCHEDULE_PERIOD);
		
		timer.start();
		System.out.println("JobScheduler started.");
	}

	@Override
	public void handleNotification(Notification notif, Object handback){
		Logger l = Logger.getLogger(DBConnection.class.getName());
		FileHandler fh = null;
		try {
			fh = new FileHandler("GHS_Export.log",true);
			fh.setLevel(logging_level);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		SimpleFormatter formatter = new SimpleFormatter();
		
		if(fh!=null){
			fh.setFormatter(formatter);
			l.addHandler(fh);
		}
		l.setLevel(logging_level);
		
		l.log(Level.FINE,"GHS Export Beginning.");
		
		String type = notif.getType();
	    if(type.equals("GHSExport")){
	    	System.out.println("-----------------All Export Starting-----------------");
	    	l.log(Level.FINE,"Initiating All export.");
	    	System.out.println(prob.ghs.RunExtract.ExportGHS("all",l));
	    	System.out.println("-----------------Probation Export Starting-----------------");
	    	l.log(Level.FINE,"Initiating Probation export.");
	    	System.out.println(prob.ghs.RunExtract.ExportGHS("prob",l));
	    	System.out.println("-----------------DHS Export Starting-----------------");
	    	l.log(Level.FINE,"Initiating DHS export.");
	    	System.out.println(prob.ghs.RunExtract.ExportGHS("dhs",l));
	    	System.out.println("-----------------DMH Export Starting-----------------");
	    	l.log(Level.FINE,"Initiating DMH export.");
	    	System.out.println(prob.ghs.RunExtract.ExportGHS("dmh",l));
	    	System.out.println("-----------------GHS Export job has completed.-----------------\n\n");
	    	l.log(Level.FINE,"GHS Export job has completed");
		}
	    
	    if(fh!=null)
	    	fh.close();
	}

	public synchronized void cleanUp(){
	    System.out.println(">>> MyAppJobScheduler cleanUp method called.");
	    try{
 	       timer.stop();
	       timer.removeNotification(GHSExport_ID);
	       System.out.println(">>> MyAppJobScheduler Scheduler stopped.");
	    } catch (InstanceNotFoundException e){
	       e.printStackTrace();
	    }
	}
	
	protected void finalize() throws Throwable{
		System.out.println(">>> MyAppJobScheduler finalize called.");
		cleanUp();
		
		super.finalize();
	}
}
