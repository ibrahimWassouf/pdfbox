/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.multipdf;

import java.awt.geom.AffineTransform;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

/**
 * Adds an overlay to an existing PDF document.
 * 
 * Based on code contributed by Balazs Jerk.
 * 
 */
public class Overlay implements Closeable
{
    /**
     * Possible location of the overlaid pages: foreground or background.
     */
    public enum Position
    {
        FOREGROUND, BACKGROUND
    }

    private LayoutPage defaultOverlayPage;
    private LayoutPage firstPageOverlayPage;
    private LayoutPage lastPageOverlayPage;
    private LayoutPage oddPageOverlayPage;
    private LayoutPage evenPageOverlayPage;

    private final Set<PDDocument> openDocumentsSet = new HashSet<>();
    private Map<Integer, LayoutPage> specificPageOverlayLayoutPageMap = new HashMap<>();

    private Position position = Position.BACKGROUND;

    private OverlayDocument inputFile = new OverlayDocument();
    private OverlayDocument defaultOverlay = new OverlayDocument();
    private OverlayDocument firstPageOverlay = new OverlayDocument();
    private OverlayDocument lastPageOverlay = new OverlayDocument();
    private OverlayDocument allPagesOverlay = new OverlayDocument();
    private OverlayDocument oddPageOverlay = new OverlayDocument();
    private OverlayDocument evenPageOverlay = new OverlayDocument();
    private OverlayDocument[] overlays = {inputFile, defaultOverlay, firstPageOverlay, lastPageOverlay, allPagesOverlay, oddPageOverlay, evenPageOverlay};

    private int numberOfOverlayPages = 0;
    private boolean useAllOverlayPages = false;

    /**
     * This will add overlays to a document.
     *
     * @param specificPageOverlayMap Optional map of overlay files of which the first page will be
     * used for specific pages of the input document. The page numbers are 1-based. The map must be
     * empty (but not null) if no specific mappings are used.
     *
     * @return The modified input PDF document, which has to be saved and closed by the caller. If
     * the input document was passed by {@link #setInputPDF(PDDocument) setInputPDF(PDDocument)}
     * then it is that object that is returned.
     *
     * @throws IOException if something went wrong.
     * @throws IllegalArgumentException if the input document is missing.
     */
    public PDDocument overlay(Map<Integer, String> specificPageOverlayMap) throws IOException
    {
        Map<String, LayoutPage> layouts = new HashMap<>();
        String path;
        loadPDFs();
        for (Map.Entry<Integer, String> e : specificPageOverlayMap.entrySet())
        {
            path = e.getValue();
            LayoutPage layoutPage = layouts.get(path);
            if (layoutPage == null)
            {
                PDDocument doc = loadPDF(path);
                layoutPage = getLayoutPage(doc);
                layouts.put(path, layoutPage);
                openDocumentsSet.add(doc);
            }
            specificPageOverlayLayoutPageMap.put(e.getKey(), layoutPage);
        }
        processPages(inputFile.getDoc());
        return inputFile.getDoc();
    }

    /**
     * This will add overlays documents to a document. If you created the overlay documents with
     * subsetted fonts, you need to save them first so that the subsetting gets done.
     *
     * @param specificPageOverlayDocumentMap Optional map of overlay documents for specific pages. The
     * page numbers are 1-based. The map must be empty (but not null) if no specific mappings are
     * used.
     *
     * @return The modified input PDF document, which has to be saved and closed by the caller. If
     * the input document was passed by {@link #setInputPDF(PDDocument) setInputPDF(PDDocument)}
     * then it is that object that is returned.
     *
     * @throws IOException if something went wrong
     */
    public PDDocument overlayDocuments(Map<Integer, PDDocument> specificPageOverlayDocumentMap) throws IOException
    {
        loadPDFs();
        for (Map.Entry<Integer, PDDocument> e : specificPageOverlayDocumentMap.entrySet())
        {
            PDDocument doc = e.getValue();
            if (doc != null)
            {
                specificPageOverlayLayoutPageMap.put(e.getKey(), getLayoutPage(doc));
            }
        }
        processPages(inputFile.getDoc());
        return inputFile.getDoc();
    }

    /**
     * Close all input documents which were used for the overlay and opened by this class.
     *
     * @throws IOException if something went wrong
     */
    @Override
    public void close() throws IOException
    {
    	for (OverlayDocument overlay : overlays) {
    		if (overlay.getDoc() != null) {
    			overlay.getDoc().close();
    		}
    		
    	}
        
        for (PDDocument doc : openDocumentsSet)
        {
            doc.close();
        }
        openDocumentsSet.clear();
        specificPageOverlayLayoutPageMap.clear();
    }

    private void loadPDFs() throws IOException
    {
        // input PDF
        if (inputFile.getName() != null)
        {
            inputFile.setDoc(loadPDF(inputFile.getName()));
        }
        if (inputFile.getDoc() == null)
        {
            throw new IllegalArgumentException("No input document");
        }
        
        for (OverlayDocument overlay : overlays) {
        	if (overlay.getName()!= null) {
        		overlay.setDoc(loadPDF(overlay.getName()));
        	}
        }
        // default overlay PDF
        if (defaultOverlay.getName() != null)
        {
            defaultOverlay.setDoc(loadPDF(defaultOverlay.getName()));
        }
        if (defaultOverlay.getDoc() != null)
        {
            defaultOverlayPage = getLayoutPage(defaultOverlay.getDoc());
        }
        // first page overlay PDF
        if (firstPageOverlay.getName() != null)
        {
            firstPageOverlay.setDoc(loadPDF(firstPageOverlay.getName())); 
        }
        if (firstPageOverlay.getDoc() != null)
        {
            firstPageOverlayPage = getLayoutPage(firstPageOverlay.getDoc());
        }
        // last page overlay PDF
        if (lastPageOverlay.getName() != null)
        {
            lastPageOverlay.setDoc(loadPDF(lastPageOverlay.getName()));
        }
        if (lastPageOverlay.getDoc() != null)
        {
            lastPageOverlayPage = getLayoutPage(lastPageOverlay.getDoc());
        }
        // odd pages overlay PDF
        if (oddPageOverlay.getName() != null)
        {
            oddPageOverlay.setDoc(loadPDF(oddPageOverlay.getName()));
        }
        if (oddPageOverlay.getDoc() != null)
        {
            oddPageOverlayPage = getLayoutPage(oddPageOverlay.getDoc());
        }
        // even pages overlay PDF
        if (evenPageOverlay.getName() != null)
        {
            evenPageOverlay.setDoc(loadPDF(evenPageOverlay.getName()));
        }
        if (evenPageOverlay.getDoc() != null)
        {
            evenPageOverlayPage = getLayoutPage(evenPageOverlay.getDoc());
        }
        // all pages overlay PDF
        if (allPagesOverlay.getName() != null)
        {
            allPagesOverlay.setDoc(loadPDF(allPagesOverlay.getName()));
        }
        if (allPagesOverlay.getDoc() != null)
        {
            specificPageOverlayLayoutPageMap = getLayoutPages(allPagesOverlay.getDoc());
            useAllOverlayPages = true;
            numberOfOverlayPages = specificPageOverlayLayoutPageMap.size();
        }
    }
    
    private PDDocument loadPDF(String pdfName) throws IOException
    {
        return Loader.loadPDF(new File(pdfName));
    }

    /**
     * Create a LayoutPage object from the first page of the given document.
     *
     * @param doc
     * @return
     * @throws IOException 
     */
    private LayoutPage getLayoutPage(PDDocument doc) throws IOException
    {
        return createLayoutPage(doc.getPage(0));
    }

    /**
     * Create a LayoutPage object from given PDPage object.
     *
     * @return
     * @throws IOException 
     */
    private LayoutPage createLayoutPage(PDPage page) throws IOException
    {
        COSBase contents = page.getCOSObject().getDictionaryObject(COSName.CONTENTS);
        PDResources resources = page.getResources();
        if (resources == null)
        {
            resources = new PDResources();
        }
        return new LayoutPage(page.getMediaBox(), createCombinedContentStream(contents),
                resources.getCOSObject(), (short) page.getRotation());
    }
    
    private Map<Integer,LayoutPage> getLayoutPages(PDDocument doc) throws IOException
    {
        int i = 0;
        Map<Integer, LayoutPage> layoutPages = new HashMap<>();
        for (PDPage page : doc.getPages())
        {
            layoutPages.put(i, createLayoutPage(page));
            i++;
        }
        return layoutPages;
    }
    
    private COSStream createCombinedContentStream(COSBase contents) throws IOException
    {
        List<COSStream> contentStreams = createContentStreamList(contents);
        // concatenate streams
        COSStream concatStream = inputFile.getDoc().getDocument().createCOSStream();
        try (OutputStream out = concatStream.createOutputStream(COSName.FLATE_DECODE))
        {
            for (COSStream contentStream : contentStreams)
            {
                try (InputStream in = contentStream.createInputStream())
                {
                    in.transferTo(out);
                    out.flush();
                }
            }
        }
        return concatStream;
    }

    // get the content streams as a list
    private List<COSStream> createContentStreamList(COSBase contents) throws IOException
    {
        if (contents == null)
        {
            return Collections.emptyList();
        }
        if (contents instanceof COSStream)
        {
            return Collections.singletonList((COSStream) contents);
        }

        List<COSStream> contentStreams = new ArrayList<>();
        if (contents instanceof COSArray)
        {
            for (COSBase item : (COSArray) contents)
            {
                contentStreams.addAll(createContentStreamList(item));
            }
        }
        else if (contents instanceof COSObject)
        {
            contentStreams.addAll(createContentStreamList(((COSObject) contents).getObject()));
        }
        else
        {
            throw new IOException("Unknown content type: " + contents.getClass().getName());
        }
        return contentStreams;
    }

    private void processPages(PDDocument document) throws IOException
    {
        int pageCounter = 0;
        PDFCloneUtility cloner = new PDFCloneUtility(document);
        PDPageTree pageTree = document.getPages();
        int numberOfPages = pageTree.getCount();
        for (PDPage page : pageTree)
        {
            pageCounter++;
            LayoutPage layoutPage = getLayoutPage(pageCounter, numberOfPages);
            if (layoutPage == null)
            {
                continue;
            }
            COSDictionary pageDictionary = page.getCOSObject();
            COSBase originalContent = pageDictionary.getDictionaryObject(COSName.CONTENTS);
            COSArray newContentArray = new COSArray();
            switch (position)
            {
                case FOREGROUND:
                    // save state
                    newContentArray.add(createStream("q\n"));
                    addOriginalContent(originalContent, newContentArray);
                    // restore state
                    newContentArray.add(createStream("Q\n"));
                    // overlay content last
                    overlayPage(page, layoutPage, newContentArray, cloner);
                    break;
                case BACKGROUND:
                    // overlay content first
                    overlayPage(page, layoutPage, newContentArray, cloner);

                    addOriginalContent(originalContent, newContentArray);
                    break;
                default:
                    throw new IOException("Unknown type of position:" + position);
            }
            pageDictionary.setItem(COSName.CONTENTS, newContentArray);
        }
    }

    private void addOriginalContent(COSBase contents, COSArray contentArray) throws IOException
    {
        if (contents == null)
        {
            return;
        }

        if (contents instanceof COSStream)
        {
            contentArray.add(contents);
        }
        else if (contents instanceof COSArray)
        {
            contentArray.addAll((COSArray) contents);
        }
        else
        {
            throw new IOException("Unknown content type: " + contents.getClass().getName());
        }
    }

    private void overlayPage(PDPage page, LayoutPage layoutPage, COSArray array,
            PDFCloneUtility cloner)
            throws IOException
    {
        PDResources resources = page.getResources();
        if (resources == null)
        {
            resources = new PDResources();
            page.setResources(resources);
        }
        COSName xObjectId = createOverlayXObject(page, layoutPage, cloner);
        array.add(createOverlayStream(page, layoutPage, xObjectId));
    }

    private LayoutPage getLayoutPage(int pageNumber, int numberOfPages)
    {
        LayoutPage layoutPage = null;
        if (!useAllOverlayPages && specificPageOverlayLayoutPageMap.containsKey(pageNumber))
        {
            layoutPage = specificPageOverlayLayoutPageMap.get(pageNumber);
        }
        else if ((pageNumber == 1) && (firstPageOverlayPage != null))
        {
            layoutPage = firstPageOverlayPage;
        }
        else if ((pageNumber == numberOfPages) && (lastPageOverlayPage != null))
        {
            layoutPage = lastPageOverlayPage;
        }
        else if ((pageNumber % 2 == 1) && (oddPageOverlayPage != null))
        {
            layoutPage = oddPageOverlayPage;
        }
        else if ((pageNumber % 2 == 0) && (evenPageOverlayPage != null))
        {
            layoutPage = evenPageOverlayPage;
        }
        else if (defaultOverlayPage != null)
        {
            layoutPage = defaultOverlayPage;
        }
        else if (useAllOverlayPages)
        {
            int usePageNum = (pageNumber -1 ) % numberOfOverlayPages;
            layoutPage = specificPageOverlayLayoutPageMap.get(usePageNum);
        }
        return layoutPage;
    }

    private COSName createOverlayXObject(PDPage page, LayoutPage layoutPage, PDFCloneUtility cloner)
            throws IOException
    {
        PDFormXObject xobjForm = new PDFormXObject(layoutPage.overlayContentStream);
        xobjForm.setResources(new PDResources(
                cloner.cloneForNewDocument(layoutPage.overlayResources)));
        xobjForm.setFormType(1);
        xobjForm.setBBox(layoutPage.overlayMediaBox.createRetranslatedRectangle());
        AffineTransform at = new AffineTransform();
        switch (layoutPage.overlayRotation)
        {
            case 90:
                at.translate(0, layoutPage.overlayMediaBox.getWidth());
                at.rotate(Math.toRadians(-90));
                break;
            case 180:
                at.translate(layoutPage.overlayMediaBox.getWidth(), layoutPage.overlayMediaBox.getHeight());
                at.rotate(Math.toRadians(-180));
                break;
            case 270:
                at.translate(layoutPage.overlayMediaBox.getHeight(), 0);
                at.rotate(Math.toRadians(-270));
                break;
            default:
                break;
        }
        xobjForm.setMatrix(at);
        PDResources resources = page.getResources();
        return resources.add(xobjForm, "OL");
    }

    private COSStream createOverlayStream(PDPage page, LayoutPage layoutPage, COSName xObjectId)
            throws IOException
    {
        // create a new content stream that executes the XObject content
        StringBuilder overlayStream = new StringBuilder();
        overlayStream.append("q\nq\n");
        PDRectangle overlayMediaBox = new PDRectangle(layoutPage.overlayMediaBox.getCOSArray());
        if (layoutPage.overlayRotation == 90 || layoutPage.overlayRotation == 270)
        {
            overlayMediaBox.setLowerLeftX(layoutPage.overlayMediaBox.getLowerLeftY());
            overlayMediaBox.setLowerLeftY(layoutPage.overlayMediaBox.getLowerLeftX());
            overlayMediaBox.setUpperRightX(layoutPage.overlayMediaBox.getUpperRightY());
            overlayMediaBox.setUpperRightY(layoutPage.overlayMediaBox.getUpperRightX());
        }
        AffineTransform at = calculateAffineTransform(page, overlayMediaBox);
        double[] flatmatrix = new double[6];
        at.getMatrix(flatmatrix);
        for (double v : flatmatrix)
        {
            overlayStream.append(float2String((float) v));
            overlayStream.append(' ');
        }
        overlayStream.append(" cm\n");

        // if debugging, insert
        // 0 0 overlayMediaBox.getHeight() overlayMediaBox.getWidth() re\ns\n
        // into the content stream

        overlayStream.append(" /");
        overlayStream.append(xObjectId.getName());
        overlayStream.append(" Do Q\nQ\n");
        return createStream(overlayStream.toString());
    }

    /**
     * Calculate the transform to be used when positioning the overlay. The default implementation
     * centers on the destination. Override this method to do your own, e.g. move to a corner, or
     * rotate.
     *
     * @param page The page that will get the overlay.
     * @param overlayMediaBox The overlay media box.
     * @return The affine transform to be used.
     */
    protected AffineTransform calculateAffineTransform(PDPage page, PDRectangle overlayMediaBox)
    {
        AffineTransform at = new AffineTransform();
        PDRectangle pageMediaBox = page.getMediaBox();
        float hShift = (pageMediaBox.getWidth() - overlayMediaBox.getWidth()) / 2.0f;
        float vShift = (pageMediaBox.getHeight() - overlayMediaBox.getHeight()) / 2.0f;
        at.translate(hShift, vShift);
        return at;
    }

    private String float2String(float floatValue)
    {
        // use a BigDecimal as intermediate state to avoid 
        // a floating point string representation of the float value
        BigDecimal value = new BigDecimal(String.valueOf(floatValue));
        String stringValue = value.toPlainString();
        // remove fraction digit "0" only
        if (stringValue.indexOf('.') > -1 && !stringValue.endsWith(".0"))
        {
            while (stringValue.endsWith("0") && !stringValue.endsWith(".0"))
            {
                stringValue = stringValue.substring(0,stringValue.length()-1);
            }
        }
        return stringValue;
    }
    
    private COSStream createStream(String content) throws IOException
    {
        COSStream stream = inputFile.getDoc().getDocument().createCOSStream();
        try (OutputStream out = stream.createOutputStream(
                content.length() > 20 ? COSName.FLATE_DECODE : null))
        {
            out.write(content.getBytes(StandardCharsets.ISO_8859_1));
        }
        return stream;
    }

    /**
     * Sets the overlay position.
     * 
     * @param overlayPosition the overlay position
     */
    public void setOverlayPosition(Position overlayPosition)
    {
        position = overlayPosition;
    }

    /**
     * Sets the file to be overlaid.
     *
     * @param inputFile the file to be overlaid. The {@link PDDocument} object gathered from
     * opening this file will be returned by
     * {@link #overlay(java.util.Map) overlay(Map&lt;Integer, String&gt;)}.
     */
    public void setInputFile(String inputFile)
    {
        this.inputFile.setName(inputFile);
    }

    /**
     * Sets the PDF to be overlaid.
     *
     * @param inputPDF the PDF to be overlaid. This will be the object that is returned by
     * {@link #overlay(java.util.Map) overlay(Map&lt;Integer, String&gt;)}.
     */
    public void setInputPDF(PDDocument inputPDF)
    {
        inputFile.setDoc(inputPDF);
    }

    /**
     * Returns the input file.
     * 
     * @return the input file
     */
    public String getInputFile()
    {
        return inputFile.getName();
    }

    /**
     * Sets the default overlay file.
     * 
     * @param defaultOverlayFile the default overlay file
     */
    public void setDefaultOverlayFile(String defaultOverlayFile)
    {
        defaultOverlay.setName(defaultOverlayFile);
    }

    /**
     * Sets the default overlay PDF. If you created the overlay document with
     * subsetted fonts, you need to save it first so that the subsetting gets done.
     * 
     * @param defaultOverlayPDF the default overlay PDF
     */
    public void setDefaultOverlayPDF(PDDocument defaultOverlayPDF)
    {
        defaultOverlay.setDoc(defaultOverlayPDF);
    }

    /**
     * Returns the default overlay file.
     * 
     * @return the default overlay file
     */
    public String getDefaultOverlayFile()
    {
        return defaultOverlay.getName();
    }

    /**
     * Sets the first page overlay file.
     * 
     * @param firstPageOverlayFile the first page overlay file
     */
    public void setFirstPageOverlayFile(String firstPageOverlayFile)
    {
        firstPageOverlay.setName(firstPageOverlayFile);
    }

    /**
     * Sets the first page overlay PDF. If you created the overlay document with
     * subsetted fonts, you need to save it first so that the subsetting gets done.
     * 
     * @param firstPageOverlayPDF the first page overlay PDF
     */
    public void setFirstPageOverlayPDF(PDDocument firstPageOverlayPDF)
    {
        firstPageOverlay.setDoc(firstPageOverlayPDF);
    }

    /**
     * Sets the last page overlay file.
     * 
     * @param lastPageOverlayFile the last page overlay file
     */
    public void setLastPageOverlayFile(String lastPageOverlayFile)
    {
        lastPageOverlay.setName(lastPageOverlayFile);
    }

    /**
     * Sets the last page overlay PDF. If you created the overlay document with
     * subsetted fonts, you need to save it first so that the subsetting gets done.
     * 
     * @param lastPageOverlayPDF the last page overlay PDF
     */
    public void setLastPageOverlayPDF(PDDocument lastPageOverlayPDF)
    {
        lastPageOverlay.setDoc(lastPageOverlayPDF);
    }

    /**
     * Sets the all pages overlay file.
     * 
     * @param allPagesOverlayFile the all pages overlay file
     */
    public void setAllPagesOverlayFile(String allPagesOverlayFile)
    {
        allPagesOverlay.setName(allPagesOverlayFile);
    }

    /**
     * Sets the all pages overlay PDF. If you created the overlay document with
     * subsetted fonts, you need to save it first so that the subsetting gets done.
     * 
     * @param allPagesOverlayPDF the all pages overlay PDF. This should not be a PDDocument that you
     * created on the fly, it should be saved first, if it contains any fonts that are subset.
     */
    public void setAllPagesOverlayPDF(PDDocument allPagesOverlayPDF)
    {
        allPagesOverlay.setDoc(allPagesOverlayPDF);
    }

    /**
     * Sets the odd page overlay file.
     * 
     * @param oddPageOverlayFile the odd page overlay file
     */
    public void setOddPageOverlayFile(String oddPageOverlayFile)
    {
        oddPageOverlay.setName(oddPageOverlayFile);
    }

    /**
     * Sets the odd page overlay PDF. If you created the overlay document with
     * subsetted fonts, you need to save it first so that the subsetting gets done.
     * 
     * @param oddPageOverlayPDF the odd page overlay PDF
     */
    public void setOddPageOverlayPDF(PDDocument oddPageOverlayPDF)
    {
        oddPageOverlay.setDoc(oddPageOverlayPDF);
    }

    /**
     * Sets the even page overlay file.
     * 
     * @param evenPageOverlayFile the even page overlay file
     */
    public void setEvenPageOverlayFile(String evenPageOverlayFile)
    {
        evenPageOverlay.setName(evenPageOverlayFile);
    }

    /**
     * Sets the even page overlay PDF. If you created the overlay document with
     * subsetted fonts, you need to save it first so that the subsetting gets done.
     * 
     * @param evenPageOverlayPDF the even page overlay PDF
     */
    public void setEvenPageOverlayPDF(PDDocument evenPageOverlayPDF)
    {
        evenPageOverlay.setDoc(evenPageOverlayPDF);
    }
}
