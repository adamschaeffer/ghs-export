package prob.scheduling;

import weblogic.application.ApplicationLifecycleListener;
public class MyStartup extends ApplicationLifecycleListener {
   public static void main(String[] args) {
     System.out.println("MyStartup(main): in main .. should be for pre-start");
   } // main
}
