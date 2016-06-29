package prob.scheduling;

import weblogic.application.ApplicationLifecycleListener;
public class MyShutdown extends ApplicationLifecycleListener {
   public static void main(String[] args) {
     System.out.println("MyShutdown(main): in main .. should be for post-stop");
   } // main
}