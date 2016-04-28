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
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.lang.Thread;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import com.googlecode.asyn4j.core.callback.AsynCallBack;
import com.googlecode.asyn4j.service.AsynService;
import com.googlecode.asyn4j.service.AsynServiceImpl;
import com.googlecode.asyn4j.core.handler.CacheAsynWorkHandler;
import com.googlecode.asyn4j.core.WorkWeight;

public class Asyn_Crawl {

	public Asyn_Crawl(AsynService as) {

		this.asynService = as;
		this.starttime = System.currentTimeMillis();
		this.run = true;
		file = new File(Context.sharedContext().getValueByName("urlPath"));
		file.mkdirs();
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
		file = new File(Context.sharedContext().getValueByName("urlPath")+"/url.txt");
	}

	public void begin() {
			
		if (!urlWaiting.isEmpty()) {
			asyn_connectURL(urlWaiting.remove(0), 0);
		}
		else {

			asyn_close();
		}
		
	}

	private synchronized void asyn_close() {

		if(run == false) {
			return;
		}
		run = false;
		asynService.addWork(new logService(), "log", new Object[]{"urlWaiting is null"}, new AsynCallBack() {

			@Override
			public void doNotify(){
				asynService.close();
				Log.l("finish crawling");
				Log.l("the number of urls that were found:" + numFindUrl);
				Log.l("the number of urls that were processed:" + urlProcessed.size());
				Log.l("the number of urls that resulted in an error:" + urlError.size());
				long endtime = System.currentTimeMillis();	
				Log.l((double)(endtime-starttime)/60000 + "mins");							
			}
		}, WorkWeight.LOW);
	}

	private void asyn_connectURL(final String strUrl, int repeat) {

		asynService.addWork(new connectURLService(), "connect", new Object[] { strUrl , repeat }, new AsynCallBack() {

	        @Override   
	        public void doNotify() {   

	        	Object[] res = (Object[])methodResult;
	        	int errno = (int)res[0];
	        	if(errno == -1) {
	        		asyn_log((String) res[1]);
	        		begin();
	        		return;
	        	}
	        	if(errno == 0) {
	        		asyn_log((String) res[1]);
	        		asyn_fetchURL((URL)res[2], (URLConnection)res[3]);
	        		return;
	        	}
	        	if(errno < 3) {
	        		asyn_log((String)res[1]);
	        		asyn_connectURL(strUrl, (int)res[0]);
	        		return;
	        	}
	        	else {
	        		asyn_log((String)res[1]);
	        		urlError.add(strUrl);
	        	}
			}			
		});

	}

	private void asyn_fetchURL(URL url, URLConnection connection) {

		asynService.addWork(new fetchURLService(), "fetch", new Object[] {url, connection}, new AsynCallBack() {

	        @Override   
	        public void doNotify() {  

	        	Object[] res = (Object[])methodResult;
	        	int errno = (int)res[0];

	        	if(errno == -1) {
	        		asyn_log((String)res[1]);
	        		begin();
	        		return;
	        	}	       
	        	else {
	        		asyn_log((String)res[1]);
	        		asyn_parseURL((URL)res[2], (Reader)res[3]);
	        	} 	
	        }
		});

	}

	private void asyn_parseURL(URL url, Reader r) {

		asynService.addWork(new parseURLService(), "parse", new Object[] { url, r}, new AsynCallBack() {
	        @Override   
	        public void doNotify() {  
	        	Object[] res = (Object[])methodResult;
	        	int errno = (int)res[0];
	        	
	        	if(errno == -1) {
	        		asyn_log((String)res[1]);
	        		begin();
	        		return;
	        	}
	        	asyn_log((String)res[1]);
	        	asyn_write2txt((String)res[2]);
	        }

		});
	}

	private void asyn_write2txt(final String entry) {

		asynService.addWork(new write2txtService() ,"write2txt", new Object[] { entry}, new AsynCallBack() {

	        @Override   
	        public void doNotify() {   				
				urlProcessed.add(entry);
				asyn_log( "Complete $" + entry +" Successful $");
				begin();
			}
		});
	}

	private void asyn_log(String entry) {

		asynService.addWork(new logService(), "log", new Object[] { entry });

	}

	protected class connectURLService {

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
				repeat ++;
        		return new Object[] {repeat, "Connecting $ " + strUrl +" $ Error: connection timeout for "+repeat +" times"};         		
        	}
        	catch(Exception e) {
        		urlError.add(strUrl);
        		return new Object[] { -1, "Connecting $" + strUrl +" $ Error: open connection failed"};
        	}

        	return new Object[] { 0, "Connecting $" + strUrl +" $ Successful", url, connection };
		}
	}

	protected class fetchURLService {

		public Object[] fetch(URL url, URLConnection connection) {

			Reader r;
			try {
				if ((connection.getContentType() != null)
					&& !connection.getContentType().toLowerCase().startsWith("text/")) {
					
					urlError.add(url.toString());
					return new Object[] { -1, "Fetching $ Error $ Not processing because content type is: " + connection.getContentType() };
				}
				InputStream is = connection.getInputStream();
				r = new InputStreamReader(is);

			}
			catch (IOException e) {
			
				urlError.add(url.toString());
				return new Object[] {-1, "Fetching $ " + url.toString() +" $ Error: get InputStream failed"};
			}
			return new Object[] {0, "Fetching $ " + url.toString() +" $ Successful", url, r};

		}

	}

	protected class parseURLService {

		public Object[] parse(URL url, Reader r) {

			HTMLEditorKit.Parser parse = new HTMLParse().getParser();
			try {
				parse.parse(r, new Parser(url), true);
			} catch (IOException e) {
				return new Object[] {-1, "Parsing $ " + url.toString() +" $ Error: IOException"};
			}
			return new Object[] {0, "Parsing $ " +url.toString() +" $ Successful", url.toString()};
		}
	}

	protected class write2txtService {

		public synchronized void write2txt(String s) {
			
			try {
				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(s+ "\r\n");
				bw.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected class logService {

		public void log(String entry) {
				Log.l("$ Thread:"+Thread.currentThread().getId()+" $ " + entry);
		}
	}

	protected class closeService {

		public void close() {

			asynService.close();
			Log.l("finish crawling");
			Log.l("the number of urls that were found:" + numFindUrl);
			Log.l("the number of urls that were processed:" + urlProcessed.size());
			Log.l("the number of urls that resulted in an error:" + urlError.size());
			long endtime = System.currentTimeMillis();	
			Log.l((double)(endtime-starttime)/60000 + "mins");			
		}
	}


	protected class HTMLParse extends HTMLEditorKit {
		public HTMLEditorKit.Parser getParser() {
			return super.getParser();
		}
	}


	protected class Parser extends HTMLEditorKit.ParserCallback {
		protected URL base;
		protected int depth;

		public Parser(URL base) {
			this.base = base;
			depth = urlDepth.get(base.toString());
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

			href = href.toLowerCase();
			if (href.startsWith("mailto:")) 
				return;
			if (href.startsWith("http")) {
				i = href.indexOf("scut.edu.cn");
				if(i == -1) {
					return;
				}
				handleLink(href);
			}
			if(href.endsWith(".css") || href.endsWith(".js") || href.endsWith("rar")) {
				return;
			}

			if(href.startsWith("../")) {
				URL u;
				String s = base.toString();
				if(s.endsWith("/")) {
					//www.scut.edu.cn/a/b/
					s = s.substring(0, s.length()-1);
				}
				else {
					//www.scut.edu.cn/a/b/home.html
					s = s.substring(0, s.lastIndexOf("/"));
				}
				try {
					u = new URL(s.substring(0, s.lastIndexOf("/")));
				}catch (MalformedURLException e) {
					begin();
					return;
				}
				handleLink(u, href.substring(2, href.length()));
			}
			if(href.startsWith("./")) {
				URL u;
				String s = base.toString();
				try {
					u = new URL(s.substring(0, s.lastIndexOf("/")));
				}catch (MalformedURLException e) {
					begin();
					return;
				}
				handleLink(u, href.substring(1, href.length()));
			}
			if(href.startsWith("/")) {
				URL u;
				String s = base.toString();
				try {
					u = new URL(s.substring(0, s.lastIndexOf("/")));
				}catch (MalformedURLException e) {
					begin();
					return;
				}
				handleLink(u, href);
			}
			handleLink(base, href);
		}

		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			if(t == HTML.Tag.A || t == HTML.Tag.FRAME || t == HTML.Tag.AREA) {
				handleSimpleTag(t, a, pos); // handle the same way
			}

		}

		protected void handleLink(URL base, String str){
			try {
				URL url = new URL(base, str);
				addURL(url.toString(), depth+1);
			} catch (MalformedURLException e) {
				asyn_log("Parseing $ "+base.toString()+"Error $Found malformed URL: " + str);
				begin();
			}
		}

		protected void handleLink(String str){
			try {
				URL url = new URL(str);
				addURL(url.toString(), depth +1);
			} catch (MalformedURLException e) {
				asyn_log("Parseing $ "+base.toString()+"Error $Found malformed URL: " + str);
				begin();
			}
		}
	}
	
	public void addURL(String url, int depth) {

		if(depth > depthCrawl) {
			return;
		}
		if (urlWaiting.contains(url))
			return;
		if (urlError.contains(url))
			return;
		if (urlProcessed.contains(url))
			return;
		asyn_log("Adding to workload $ " + url);
		urlWaiting.add(url);
		urlDepth.put(url, depth);
		numFindUrl++;
	}


	private Set<String> urlProcessed = Collections.synchronizedSet(new HashSet<String>());
	private Set<String> urlError = Collections.synchronizedSet(new HashSet<String>());
	private List<String> urlWaiting = Collections.synchronizedList(new ArrayList<String>());
	private Map<String ,Integer> urlDepth = new ConcurrentHashMap<String, Integer>();
	private int numFindUrl = 0;
	private AsynService asynService;
	private long starttime;
	private File file;
	private final int depthCrawl = Integer.parseInt(Context.sharedContext().getValueByName("depth"));
	private boolean run;
	
	public static void main(String[] args) {


        AsynService asynService =  AsynServiceImpl.getService(300, 3000L, 5, 1, 3000L);   
        //asynService.setWorkQueueFullHandler(new CacheAsynWorkHandler()); 
        asynService.init();  
		Asyn_Crawl crawl = new Asyn_Crawl(asynService);
		crawl.addURL(Context.sharedContext().getValueByName("startPoint"), 1);
		crawl.begin();
	}
}