package prob.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import prob.util.Printer;

public class PrintJob {
	ArrayList<Printer> printer = new ArrayList<Printer>();
	String message;
	
	public PrintJob(){	}
	public PrintJob(Printer p){
		printer.add(p);
	}
	public PrintJob(ArrayList<Printer> pList){
		for(int i = 0; i < pList.size(); i++){
			Printer p = pList.get(i);
			printer.add(p.clone());
		}
	}
	
	public void addPrinter(Printer p){
		printer.add(p.clone());
	}
	public void addPrinter(ArrayList<Printer> p){
		if(p==null)
			return;
		
		for(int i = 0; i < p.size(); i++){
			printer.add(p.get(i).clone());
		}
	}
	
	public void setMessage(String s){
		message = new String(s);
	}
	
	public void run() {
		try {
			print();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			throw new RuntimeException("Cannot connect to printer, IO Exception: " + e.getMessage());
		} catch (SQLException e) {
			throw new RuntimeException("Cannot get printer information: " + e.getMessage());
		} catch (RuntimeException e) {
			throw new RuntimeException("Runtime Error: " + e.getMessage());
		}
	}

	private void print() throws UnknownHostException, IOException, SQLException, RuntimeException {
		if(printer == null || printer.size()==0)
			return;
		
		for(int i = 0; i < printer.size(); i++){
			Printer p = printer.get(i);
			p.print(message);
		}
	}
}
