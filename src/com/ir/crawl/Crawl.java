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
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class Crawl {


	public void begin() throws IOException {
		
		while (!urlWaiting.isEmpty()) {
			processURL(urlWaiting.remove(0));
		}
		
		Log.c("finish crawling");
		Log.c("the number of urls that were found:" + numFindUrl);
		Log.c("the number of urls that were processed:" + urlProcessed.size());
		Log.c("the number of urls that resulted in an error:" + urlError.size());
	}


	public void processURL(String strUrl) throws IOException {
		URL url = new URL(strUrl);
		try {
			Log.l("Processing: " + url);

			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Test Crawler for Course NIR");

			if ((connection.getContentType() != null)
					&& !connection.getContentType().toLowerCase()
							.startsWith("text/")) {
				Log.l("Not processing because content type is: "
						+ connection.getContentType());
				return;
			}

			InputStream is = connection.getInputStream();
			Reader r = new InputStreamReader(is);
			HTMLEditorKit.Parser parse = new HTMLParse().getParser();
			parse.parse(r, new Parser(url), true);
		} catch (IOException e) {
			urlError.add(url.toString());
			Log.l("Error: " + url);
			return;
		}

		write2txt_(url.toString());
		urlProcessed.add(url.toString());
		log("Complete: " + url);
	}

	public void addURL(String url) throws IOException {
		if (urlWaiting.contains(url))
			return;
		if (urlError.contains(url))
			return;
		if (urlProcessed.contains(url))
			return;
		Log.l("Adding to workload: " + url);
		urlWaiting.add(url);
		numFindUrl++;
	}


	public void log(String entry) {
		System.out.println((new Date()) + ":" + entry);
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

	private void write2txt_(String s) throws IOException {
		
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
	}	

	private HashSet<String> urlProcessed = new HashSet<String>();
	private HashSet<String> urlError = new HashSet<String>();
	private List<String> urlWaiting = new ArrayList<String>();
	private int numFindUrl = 0;
	
	public static void main(String[] args) {
		Crawl crawl = new Crawl();
		try {
			crawl.addURL(Context.sharedContext().getValueByName("startPoint"));
			crawl.begin();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}