package com.ir.crawl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.lang.Thread;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import com.googlecode.asyn4j.core.callback.AsynCallBack;
import com.googlecode.asyn4j.service.AsynService;
import com.googlecode.asyn4j.service.AsynServiceImpl;

public class Asyn_Crawl {

	public Asyn_Crawl(AsynService as) {
		this.asynService = as;
		this.starttime = System.currentTimeMillis();
	}

	public void begin() throws IOException {
		
		if (!urlWaiting.isEmpty() && urlProcessed.size()<150) {
			asyn_processURL(urlWaiting.remove(0));
		}
		else {

			Log.c("finish crawling");
			Log.c("the number of urls that were found:" + numFindUrl);
			Log.c("the number of urls that were processed:" + urlProcessed.size());
			Log.c("the number of urls that resulted in an error:" + urlError.size());
			long endtime = System.currentTimeMillis();	
			Log.c((double)(endtime-starttime)/60000 + "mins");
			return;
		}
		
	}

	private void asyn_processURL(final String strUrl) {

		asynService.addWork(new Object[] {"Processing: " + strUrl}, new logService(), "log", new AsynCallBack() {
	        @Override   
	        public void doNotify() {   

				URL url;
				URLConnection connection;
				try {
				url = new URL(strUrl);
				connection = url.openConnection();
				connection = url.openConnection();
				connection.setRequestProperty("User-Agent", "Test Crawler for Course NIR");

				if ((connection.getContentType() != null)
						&& !connection.getContentType().toLowerCase()
								.startsWith("text/")) {
					Log.l("Not processing because content type is: "
							+ connection.getContentType());
					return;
				}
	        	}catch(Exception e) {
	        		e.printStackTrace();
	        		return;
	        	}
				asyn_parseURL(url, connection);
			}
		});

	}

	protected class logService {

		public void log(String entry) {
			try {
				Log.l("$Thread: "+Thread.currentThread().getId()+"$ " + entry);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected class parseURLService {

		public void processUrl(URL url, URLConnection connection) {

			try {
				InputStream is = connection.getInputStream();
				Reader r = new InputStreamReader(is);
				HTMLEditorKit.Parser parse = new HTMLParse().getParser();
				parse.parse(r, new Parser(url), true);
			}catch (IOException e) {
				urlError.add(url.toString());
				asynService.addWork(new Object[] { "Error: " + url}, new logService(), "log");
				return;
			}

			asyn_write2txt(connection.toString(), url);
			try {
				begin();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void asyn_parseURL(URL url, URLConnection connection) {

		asynService.addWork(new Object[] { "Parsing: " + url}, new logService(), 
		"log", new AsynCallBack() {

	        @Override   
	        public void doNotify() {   	
	        	asynService.addWork(new Object[] {url, connection}, new parseURLService(), "processUrl");
	        }
		});
	}

	protected class write2txtService {

		public void write2txt(String s) {
			
			try {
				File f = new File(Context.sharedContext().getValueByName("urlPath"));
				f.mkdirs();
				if (!f.exists()) {
					try {
						f.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}		
				f = new File(Context.sharedContext().getValueByName("urlPath")+"/url.txt");
				FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(s+"\r\n");
				bw.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void asyn_write2txt(String entry, final URL url) {

		asynService.addWork(new Object[] { entry }, new write2txtService(),
		"write2txt", new AsynCallBack() {

	        @Override   
	        public void doNotify() {   				
				urlProcessed.add(url.toString());
				asynService.addWork(new Object[]{ "Complete: " + url }, new logService(), "log");
				try {
					begin();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	
	protected class HTMLParse extends HTMLEditorKit {
		public HTMLEditorKit.Parser getParser() {
			return super.getParser();
		}
	}


	protected class Parser extends HTMLEditorKit.ParserCallback {
		protected URL base;

		public Parser(URL base) {
			this.base = base;
		}

		public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			String href = (String) a.getAttribute(HTML.Attribute.HREF);

			if ((href == null) && (t == HTML.Tag.FRAME))
				href = (String) a.getAttribute(HTML.Attribute.SRC);

			if (href == null)
				return;

			int i = href.indexOf('#');
			if (i != -1)
				href = href.substring(0, i);

			if (href.toLowerCase().startsWith("mailto:")) 
				return;

			try {
				handleLink(base, href);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			handleSimpleTag(t, a, pos); // handle the same way

		}

		protected void handleLink(URL base, String str) throws IOException {
			try {
				URL url = new URL(base, str);
				addURL(url.toString());
			} catch (MalformedURLException e) {
				Log.l("Found malformed URL: " + str);
			}
		}
	}
	
	public void addURL(String url) throws IOException {
		if (urlWaiting.contains(url))
			return;
		if (urlError.contains(url))
			return;
		if (urlProcessed.contains(url))
			return;
		Log.l("$Thread: "+Thread.currentThread().getId()+"$ "+"Adding to workload: " + url);
		urlWaiting.add(url);
		numFindUrl++;
	}
	
	
	private Set<String> urlProcessed = Collections.synchronizedSet(new HashSet<String>());
	private Set<String> urlError = Collections.synchronizedSet(new HashSet<String>());
	private List<String> urlWaiting = Collections.synchronizedList(new ArrayList<String>());
	private int numFindUrl = 0;
	private AsynService asynService;
	private long starttime;
	
	public static void main(String[] args) {


        AsynService asynService =  AsynServiceImpl.getService(300, 3000L, 3, 2);   
        asynService.init();  
		Asyn_Crawl crawl = new Asyn_Crawl(asynService);
		try {
			crawl.addURL(Context.sharedContext().getValueByName("startPoint"));
			crawl.begin();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}