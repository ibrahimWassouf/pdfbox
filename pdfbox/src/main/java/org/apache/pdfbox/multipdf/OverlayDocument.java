package org.apache.pdfbox.multipdf;

import org.apache.pdfbox.pdmodel.PDDocument;

public class OverlayDocument {
	
	private String name;
	private PDDocument doc;
	private LayoutPage layoutPage;
	
	public PDDocument getDoc() {
		return doc;
	}
	public void setDoc(PDDocument doc) {
		this.doc = doc;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public LayoutPage getLayoutPage() {
		return layoutPage;
	}
	public void setLayoutPage(LayoutPage layoutPage) {
		this.layoutPage = layoutPage;
	}

}
