package prob.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class Printer {
	private Socket printer;
	private DataOutputStream printerStream;
	String ipAddress;
	int port;
	private static final String ESCAPE = (char)27 + "%-12345X";

	public Printer(String ipAddress,Integer port){
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
	private void initPrintJob() throws IOException{
		printer = new Socket(ipAddress,port);
		printerStream = new DataOutputStream(printer.getOutputStream());

		String cmd = ESCAPE + "@PJL\r\n";
		printerStream.write(cmd.getBytes());
		printerStream.flush();
		
		cmd = "@PJL JOB [NAME = \"GHS TEST PRINT\"]\r\n";
		printerStream.write(cmd.getBytes());
		printerStream.flush();
		
		cmd = "@PJL ENTER LANGUAGE = PCL\r\n";
		printerStream.write(cmd.getBytes());
		printerStream.flush();
	}

	public void print(String msg){
		if(ipAddress == null && ipAddress.isEmpty())
			return;
		
		try {
			initPrintJob();
			String cmd = (char)27 + "E" + msg + "\r\n";
			printerStream.write(cmd.getBytes());
			printerStream.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error sending string to printer.",e);
		} finally {
			closePrintJob();
		}
	}
	
	public void print(File file) throws IOException {
		if(ipAddress == null && ipAddress.isEmpty())
			return;
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		DataInputStream dataIn = new DataInputStream(bis);
		
		try {
			initPrintJob();
			String resetCmd = new String((char)27 + "E");
			printerStream.write(resetCmd.getBytes());
			while(dataIn.available() > 0){
				int dataFromFile;
				
				try{
					dataFromFile = dataIn.read();
				}catch(IOException e){
					throw new RuntimeException("Error reading from file to be printed.",e);
				}
				
				try{
					printerStream.write(dataFromFile);
				} catch(IOException e){
					throw new RuntimeException("Error sending data to printer",e);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error checking file stream.",e);
		} finally {
			closePrintJob();
			dataIn.close();
			bis.close();
		}
	}

	private void closePrintJob() {
		try {
		/*
		* These two writes were required according to the documentation I found,
		* but they end up printing to a new page, so it looks like my source was incorrect.
		* However, if there's an issue with this class messing with printers, start investigating here.
		 */			
//			String cmd = ESCAPE + " @PJL EOJ NAME=\"GHS TEST PRINT\" \r\n";
//			printerStream.write(cmd.getBytes());
//			
//			cmd = ESCAPE;
//			printerStream.write(cmd.getBytes());
			try{
				Thread.sleep(20000);
			} catch(InterruptedException e){
				throw new RuntimeException("Error: Interrupted before print job could complete.",e);
			}
			printerStream.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing print job.",e);
		}finally{
			try{
				printerStream.close();
				printer.close();
			}
			catch(IOException e){
				throw new RuntimeException("Cannot close printer connection: "+e.getMessage());
			}
		}
		
	}
	
	public Printer clone(){
		return new Printer(ipAddress,port);
	}
}
