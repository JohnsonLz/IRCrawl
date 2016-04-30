package com.ir.crawl;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class Html_Parse extends HTMLEditorKit{

	public HTMLEditorKit.Parser getParser() {
		return super.getParser();
	}

	public Object[] parse(URL url, Reader r, int depth, Asyn_Crawl.parseCallBack cb) {

		HTMLEditorKit.Parser parse = getParser();
		Parser p = new Parser(url, depth, cb);
		try {
			parse.parse(r, p, true);
			
		} catch (IOException e) {
			return new Object[] {-1};
		}
		return new Object[] {0};
	}

	protected class Parser extends HTMLEditorKit.ParserCallback {
		protected URL base;
		protected int depth;
		protected Asyn_Crawl.parseCallBack cb;
		
		public Parser(URL base, int depth, Asyn_Crawl.parseCallBack cb) {
			this.base = base;
			this.depth = depth;
			this.cb = cb;
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
				||href.endsWith(".xls") || href.endsWith(".xlsx") || href.endsWith(".zip")
				) {
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
				cb.asyn_add_url(url.toString(), depth+1);
			} catch (MalformedURLException e) {
				cb.asyn_error("Parseing $ "+base.toString()+"Error $Found malformed URL: " + str);
			}
		}

		protected void handleLink(String str){
			try {
				URL url = new URL(str);
				cb.asyn_add_url(url.toString(), depth+1);
			} catch (MalformedURLException e) {
				cb.asyn_error("Parseing $ "+base.toString()+"Error $Found malformed URL: " + str);
			}
		}
	}
}
