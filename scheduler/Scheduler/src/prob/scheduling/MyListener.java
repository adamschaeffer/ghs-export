package prob.scheduling;

import weblogic.application.ApplicationLifecycleListener;
import weblogic.application.ApplicationLifecycleEvent;

public class MyListener extends ApplicationLifecycleListener {
	JobScheduler jobScheduler;

	@Override
	public void postStart(ApplicationLifecycleEvent evt){
		System.out.println("\n\n\nTimer Initialization Begin\n\n\n");
		//Start the Scheduler
		jobScheduler = new JobScheduler();
		System.out.println("\n\n\nTimer Initialization Successful\n\n\n");
	}
	@Override
	public void preStop(ApplicationLifecycleEvent evt){
		System.out.println("\n\n\nStop Timer\n\n\n");
		//Stop the Scheduler
		jobScheduler.cleanUp();
		System.out.println("\n\n\nTimer Stopped\n\n\n");
	}
}