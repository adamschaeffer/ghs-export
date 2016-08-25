package prob.scheduling;

import java.util.Date;

import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import weblogic.management.timer.Timer;

public final class JobScheduler implements NotificationListener {
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
		String type = notif.getType();
	    if(type.equals("GHSExport")){
	    	System.out.println("======================================\n");
	    	System.out.println("-----------------All Export Starting-----------------");
	    	prob.ghs.RunExtract.ExportGHS("all");
	    	System.out.println("-----------------Probation Export Starting-----------------");
	    	prob.ghs.RunExtract.ExportGHS("prob");
	    	System.out.println("-----------------DHS Export Starting-----------------");
	    	prob.ghs.RunExtract.ExportGHS("dhs");
	    	System.out.println("-----------------DMH Export Starting-----------------");
	    	prob.ghs.RunExtract.ExportGHS("dmh");
	    	System.out.println("GHS Export job has completed.\n======================================\n");
		}
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
