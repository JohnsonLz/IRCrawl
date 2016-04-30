package com.ir.crawl;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
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

	public void begin(String url, int depth) {
			
		addURL(url, 0);
	}

	private void asyn_close() {

		asynService.addWork(new closeService(), "close", new Object[]{});
	}

	private void asyn_connectURL(final String strUrl, int repeat) {

		asynService.addWork(new Asyn_Http(), "connect", new Object[] { strUrl , repeat }, new AsynCallBack() {

	        @Override   
	        public void doNotify() {   

	        	Object[] res = (Object[])methodResult;
	        	int errno = (int)res[0];
	        	if(errno == -1) {
	        		asyn_log("Connecting $ " + strUrl +" $ Error: open connection failed");
	        		return;
	        	}
	        	if(errno == 0) {
	        		asyn_log("Connecting $ " + strUrl +" $ Successful");
	        		asyn_fetchURL((URL)res[1], (URLConnection)res[2]);
	        		return;
	        	}
	        	if(errno <= 3) {
	        		asyn_log("Connecting $ " + strUrl +" $ Error: connection timeout for "+ (repeat+1) +" times");
	        		asyn_connectURL(strUrl, repeat+1);
	        		return;
	        	}
			}			
		});

	}

	private void asyn_fetchURL(final URL url, final URLConnection connection) {

		asynService.addWork(new Asyn_Http(), "fetch", new Object[] {url, connection}, new AsynCallBack() {

	        @Override   
	        public void doNotify() {  

	        	Object[] res = (Object[])methodResult;
	        	int errno = (int)res[0];

	        	if(errno == -1) {
	        		asyn_log("Fetching $ " + url.toString() +" $ Error: get InputStream failed");
	        		return;
	        	}	       
	        	else {
	        		asyn_log("Fetching $ " + url.toString() +" $ Successful");
	        		asyn_parseURL(url, (Reader)res[1]);
	        	} 	
	        }
		});

	}

	private void asyn_parseURL(final URL url, Reader r) {

		asynService.addWork(new parseURLService(), "parse", new Object[] { url, r, urlDepth.get(url.toString())}, new AsynCallBack() {
	        @Override   
	        public void doNotify() {  
	        	Object[] res = (Object[])methodResult;
	        	int errno = (int)res[0];
	        	
	        	if(errno == -1) {
	        		asyn_log("Parsing $ " + url.toString() +" $ Error: IOException");
	        		return;
	        	}
	        	asyn_log("Parsing $ " +url.toString() +" $ Successful");
	        	asyn_write2txt(url.toString(), file);
	        }

		});
	}

	private void asyn_write2txt(final String entry, File file) {

		asynService.addWork(new Asyn_Http() ,"write2txt", new Object[] { entry, file}, new AsynCallBack() {

	        @Override   
	        public void doNotify() { 

				asyn_log( "Complete $ " + entry +" Successful $");
				synchronized(lock) {
					if(run == true) {
						if(run == false) {
							return;
						}
						run = false;
						asyn_close();
					}
				}
			}
		});
	}

	private void asyn_log(String entry) {

		asynService.addWork(new Asyn_Http(), "log", new Object[] { entry });

	}

	protected class closeService {
	
		public void close() {

			new Thread() {
				@Override
				public void run() {
					long total = 0;
					long execute = 0;
					long  callback = 0;
					while(true) {						
						try {
							Thread.sleep(3000);			
							long t = asynService.getRunStatMap().get("total");
							long e = asynService.getRunStatMap().get("execute");
							long c = asynService.getRunStatMap().get("callback");
							Log.d("total" + total +"t" +t);
							Log.d("execute" + execute +"e" +t);
							Log.d("callback" + callback + "c" + c);
							if(total != t || execute != e || callback != c || !asynService.getRunStatMap().get("total").equals(asynService.getRunStatMap().get("execute"))) {
								total = t;
								execute = e;
								callback = c;
							}
							else {
								Log.l("finish crawling");
								long endtime = System.currentTimeMillis();	
								Log.l((double)(endtime-starttime)/60000 + "mins");	
								asynService.close();
								break;
							}
						}catch(InterruptedException e) {

						}
					}

				}
			}.start();		
		}

	}

	protected class parseURLService {

		public Object[] parse(URL url, Reader r, int depth) {

			HTMLEditorKit.Parser parse = new Html_Parse().getParser();
			Parser p = new Parser(url, depth);
			try {
				parse.parse(r, p, true);
				
			} catch (IOException e) {
				return new Object[] {-1};
			}
			return new Object[] {0};
		}
	}

	protected class Html_Parse extends HTMLEditorKit{

		public HTMLEditorKit.Parser getParser() {
			return super.getParser();
		}
	}

	protected class Parser extends HTMLEditorKit.ParserCallback {
		protected URL base;
		protected int depth;
		
		public Parser(URL base, int depth) {
			this.base = base;
			this.depth = depth;
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
			if(href.endsWith(".css") || href.endsWith(".js") || href.endsWith(".rar") ||href.endsWith(".xml")
				||href.endsWith(".doc") || href.endsWith(".docx") || href.endsWith(".pdf") ||href.endsWith(".jpg")
			    ||href.endsWith(".xls") || href.endsWith(".xlsx") || href.endsWith(".zip")) {
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
			}
		}

		protected void handleLink(String str){
			try {
				URL url = new URL(str);
				addURL(url.toString(), depth+1);
			} catch (MalformedURLException e) {
				asyn_log("Parseing $ "+base.toString()+"Error $Found malformed URL: " + str);
			}
		}
	}
	
	private void addURL(String url, int depth) {

		if(depth > depthCrawl) {
			return;
		}
		if(url.endsWith(".css") || url.endsWith(".js") || url.endsWith(".rar") ||url.endsWith(".xml")
			||url.endsWith(".doc") || url.endsWith(".docx") || url.endsWith(".pdf") ||url.endsWith(".jpg")
		    ||url.endsWith(".xls") || url.endsWith(".xlsx") || url.endsWith(".zip")) {
			Log.d(url);
			return;
		}
		synchronized(lock) {
			if(urlFound.contains(url)) {
				return;
			}
			asyn_log("Adding $ " + url);
			urlFound.add(url);
			urlDepth.put(url, depth);
			asyn_connectURL(url, 0);
		}
	}


	private Set<String> urlFound = new HashSet<String>();
	private Map<String ,Integer> urlDepth = new ConcurrentHashMap<String, Integer>();
	private AsynService asynService;
	private long starttime;
	private File file;
	private final int depthCrawl = Integer.parseInt(Context.sharedContext().getValueByName("depth"));
	private Object lock = new Object();
	private boolean run;
	
	public static void main(String[] args) {


        AsynService asynService =  AsynServiceImpl.getService(300, 10000L, 4, 4, 3000L);   
        //asynService.setWorkQueueFullHandler(new CacheAsynWorkHandler()); 
        asynService.init();  
		Asyn_Crawl crawl = new Asyn_Crawl(asynService);
		crawl.begin(Context.sharedContext().getValueByName("startPoint"), 1);
	}
}