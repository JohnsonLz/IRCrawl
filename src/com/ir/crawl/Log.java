package com.ir.crawl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

	static File f;
	static DateFormat format;
	static {

		f = new File(Context.sharedContext().getValueByName("logPath"));
		f.mkdirs();
		f = new File(Context.sharedContext().getValueByName("logPath")+"/log.txt");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  

	}

	public static void d(String entry) {
		if(Context.sharedContext().getValueByName("debug").equals("true")) {
			System.out.println(entry);
		}
	}

	public static void w(String entry) {
		if(Context.sharedContext().getValueByName("console").equals("true")) {
			System.out.println("warning" + (format.format(new Date())) + ":" + entry);
		}
		if(Context.sharedContext().getValueByName("file").equals("true")) {
			writeString_("warning" + (format.format(new Date())) + ":" + entry);
		}
	}

	public static void l(String entry) {
		if(Context.sharedContext().getValueByName("console").equals("true")) {
			System.out.println((format.format(new Date())) + ":" + entry);
		}
		if(Context.sharedContext().getValueByName("file").equals("true")) {
			writeString_((format.format(new Date())) + ":" + entry);
		}
	}

	public static void c(String entry) {
		if(Context.sharedContext().getValueByName("console").equals("true")) {
			System.out.println((format.format(new Date())) + ":" + entry);
		}
	}

	private synchronized static void writeString_(String s){
	
		try {

			FileWriter fw = new FileWriter(f, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(s+"\r\n");
			bw.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}		


}