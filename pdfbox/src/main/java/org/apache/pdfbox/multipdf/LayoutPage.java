package org.apache.pdfbox.multipdf;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Stores the overlay page information.
 */
final class LayoutPage
{
    final PDRectangle overlayMediaBox;
    final COSStream overlayContentStream;
    final COSDictionary overlayResources;
    final short overlayRotation;

    LayoutPage(PDRectangle mediaBox, COSStream contentStream, COSDictionary resources, short rotation)
    {
        overlayMediaBox = mediaBox;
        overlayContentStream = contentStream;
        overlayResources = resources;
        overlayRotation = rotation;
    }
}