package com.ir.crawl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Log {

	public static void d(String entry) {
		if(Context.sharedContext().getValueByName("debug").equals("true")) {
			System.out.println(entry);
		}
	}

	public static void w(String entry) throws IOException{
		if(Context.sharedContext().getValueByName("console").equals("true")) {
			System.out.println("warning" + (new Date()) + ":" + entry);
		}
		if(Context.sharedContext().getValueByName("file").equals("true")) {
			writeString_("warning" + (new Date()) + ":" + entry);
		}
	}

	public static void l(String entry) throws IOException{
		if(Context.sharedContext().getValueByName("console").equals("true")) {
			System.out.println((new Date()) + ":" + entry);
		}
		if(Context.sharedContext().getValueByName("file").equals("true")) {
			writeString_((new Date()) + ":" + entry);
		}
	}

	public static void c(String entry) {
		if(Context.sharedContext().getValueByName("console").equals("true")) {
			System.out.println((new Date()) + ":" + entry);
		}
	}

	private static void writeString_(String s) throws IOException {
		File f = new File(Context.sharedContext().getValueByName("logPath"));
		f.mkdirs();
		f = new File(Context.sharedContext().getValueByName("logPath")+"/log.txt");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter fw = new FileWriter(f, true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(s+"\r\n");
		bw.close();
	}		


}