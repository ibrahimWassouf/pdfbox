package org.apache.pdfbox.pdmodel;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

public class PDDocumentManuscriptInformation {
	
	private final COSDictionary info;
	

    /**
     * Default Constructor.
     */
    public PDDocumentManuscriptInformation()
    {
        info = new COSDictionary();
    }

    /**
     * Constructor that is used for a preexisting dictionary.
     *
     * @param dic The underlying dictionary.
     */
    public PDDocumentManuscriptInformation( COSDictionary dic )
    {
        info = dic;
    }
	   /**
     * This will get the title of the document.  This will return null if no title exists.
     *
     * @return The title of the document.
     */
    public String getTitle()
    {
        return info.getString( COSName.TITLE );
    }
    
    
    /**
     * This will set the title of the document.
     *
     * @param title The new title for the document.
     */
    public void setTitle( String title )
    {
        info.setString( COSName.TITLE, title );
    }


    /**
     * This will get the author of the document.  This will return null if no author exists.
     *
     * @return The author of the document.
     */
    public String getAuthor()
    {
        return info.getString( COSName.AUTHOR );
    }

    /**
     * This will set the author of the document.
     *
     * @param author The new author for the document.
     */
    public void setAuthor( String author )
    {
        info.setString( COSName.AUTHOR, author );
    }
    

    /**
     * This will get the creator of the document.  This will return null if no creator exists.
     *
     * @return The creator of the document.
     */
    public String getCreator()
    {
        return info.getString( COSName.CREATOR );
    }

    /**
     * This will set the creator of the document.
     *
     * @param creator The new creator for the document.
     */
    public void setCreator( String creator )
    {
        info.setString( COSName.CREATOR, creator );
    }


    /**
     * This will get the producer of the document.  This will return null if no producer exists.
     *
     * @return The producer of the document.
     */
    public String getProducer()
    {
        return info.getString( COSName.PRODUCER );
    }

    /**
     * This will set the producer of the document.
     *
     * @param producer The new producer for the document.
     */
    public void setProducer( String producer )
    {
        info.setString( COSName.PRODUCER, producer );
    }

}
