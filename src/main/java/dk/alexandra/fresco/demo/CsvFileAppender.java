package dk.alexandra.fresco.demo;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import ch.qos.logback.core.FileAppender;

public class CsvFileAppender<E> extends FileAppender<E> {
	
	String csvHeader = "";
	
	public void setHeader(String header) {
		this.csvHeader = header;
	}

	@Override
	public void openFile(String fileName) throws IOException
	{
	    super.openFile(fileName);
	    
	    if (csvHeader != "") {
	    	File file = new File(getFile());
	    	
	    	if (file.exists() && file.isFile() && file.length() == 0)
	    	{
	    		synchronized (lock) {
	    			new PrintWriter(new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8), true).println(csvHeader);
	    		}
	    		
	    	}	    	
	    }
	    
	}
	    
}
