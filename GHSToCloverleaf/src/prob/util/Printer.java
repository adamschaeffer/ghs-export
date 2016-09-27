package prob.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class Printer {
	private Socket printer;
	private DataOutputStream printerStream;
	private static final String ESCAPE = (char)27 + "%-12345X";
	private BufferedReader reader;

	public Printer(String ipAddress,Integer port) throws UnknownHostException, IOException{
		printer = new Socket(ipAddress,port);
		
		printerStream = new DataOutputStream(printer.getOutputStream());
//		reader = new BufferedReader(new InputStreamReader(printer.getInputStream()));
	}
	
	private void initPrintJob() throws IOException{
		String cmd = ESCAPE + "@PJL\r\n";
		printerStream.write(cmd.getBytes());
		printerStream.flush();
		
		cmd = "@PJL JOB NAME = \"GHS TEST PRINT\"\r\n";
		printerStream.write(cmd.getBytes());
		printerStream.flush();
//		System.out.println("Printer Response: " + reader.readLine());
		
		cmd = "@PJL ENTER LANGUAGE = PCL\r\n";
		printerStream.write(cmd.getBytes());
		printerStream.flush();		
	}

	public void print(String msg){
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
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		DataInputStream dataIn = new DataInputStream(bis);
		
		try {
			initPrintJob();
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
			//String cmd = ESCAPE + "@JPL EOF \r\n";
			//printerStream.write(cmd.getBytes());
			
			//cmd = ESCAPE;
			//printerStream.write(cmd.getBytes());
			try{
				Thread.sleep(20000);
			} catch(InterruptedException e){
				throw new RuntimeException("Error: Interrupted before print job could complete.",e);
			}
			printerStream.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing print job.",e);
		}
		
	}
}
