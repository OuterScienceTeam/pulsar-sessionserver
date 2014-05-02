package outerscience.pulsar.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import javolution.text.TextBuilder;

public class FileLogFormatter extends Formatter
{
	private final SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	
	public String format(LogRecord record)
	{
		TextBuilder output = new TextBuilder();
		output.append(dateformat.format(new Date(record.getMillis())));
		output.append("\t[");
    	output.append(record.getLevel().getName());
    	output.append("]\t");
    	output.append(String.valueOf(record.getThreadID()));
    	output.append("\t");
    	output.append(record.getLoggerName());
    	output.append("\t");
    	output.append(record.getMessage());
    	output.append("\r\n");
    	
    	return output.toString();
  }
}