package com.ir.crawl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

public class Asyn_http {

	public Object[] connect(String strUrl, int repeat) {

		URL url;
		URLConnection connection;
		try {
			url = new URL(strUrl);
			connection = url.openConnection();
			connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Test Crawler for Course NIR: 2016IR201330552157");
			connection.setConnectTimeout(3000);
			connection.setReadTimeout(3000);
			connection.connect();

		}
		catch(SocketTimeoutException se) {

    		return new Object[] {repeat};         		
    	}
    	catch(Exception e) {
    		return new Object[] {-1};
    	}
    	return new Object[] { 0, url, connection };
	}

	public Object[] fetch(URL url, URLConnection connection) {

		Reader r;
		try {
			InputStream is = connection.getInputStream();
			r = new InputStreamReader(is);

		}
		catch (IOException e) {
			return new Object[] {-1};
		}
		return new Object[] {0, r};

	}

	public synchronized void write2txt(String s, File file) {
		
		try {
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(s+ "\r\n");
			bw.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}


	public void log(String entry) {
			Log.l("$ Thread:"+Thread.currentThread().getId()+" $ " + entry);
	}

}