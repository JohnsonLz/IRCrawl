package com.ir.crawl;

import java.io.File;
import java.util.Iterator;
import java.util.HashMap;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class Context {

	private Context() {
		parseXMLConfigure_();
	}

	private static class SingletonHolder {

		private static final Context INSTANCE = new Context();
	}

	public static Context sharedContext() {

		return SingletonHolder.INSTANCE;
	}

	private void parseXMLConfigure_() {

		SAXReader reader = new SAXReader();
		Document document;
		try {
			document = reader.read(new File("configure.xml"));
		}catch (DocumentException e) {
			e.printStackTrace();
			return;
		}
		Element root = document.getRootElement();

		Iterator it = root.elementIterator();
		while(it.hasNext()) {
			Element element = (Element)it.next();
			Iterator eleItor = element.elementIterator();
			while(eleItor.hasNext()) {
				Element ele = (Element)eleItor.next();
				map_.put(ele.getName(), ele.getText());
			}
		}

	}

	public String getValueByName(String name) {

		return map_.get(name);
	}

	private HashMap<String, String> map_ = new HashMap<String, String>();

}
