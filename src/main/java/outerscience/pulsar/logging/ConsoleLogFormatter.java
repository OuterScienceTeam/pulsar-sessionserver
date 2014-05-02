package outerscience.pulsar.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import javolution.text.TextBuilder;

public class ConsoleLogFormatter extends Formatter
{
	private final SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	
	public String format(LogRecord record)
	{
		TextBuilder output = new TextBuilder(500);
		output.append(dateformat.format(new Date(record.getMillis())));
		output.append(" [");
		output.append(record.getLevel().getName());
		output.append("] ");
		output.append(String.valueOf(record.getThreadID()));
		output.append(" ");
		output.append(record.getLoggerName());
		output.append(" ");
		output.append(record.getMessage());
		output.append("\r\n");
	
		if(record.getThrown() != null)
		{
			try(StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw))
			{
				record.getThrown().printStackTrace(pw);
				output.append(sw.toString());
				output.append("\r\n");
			}
			catch (Exception e){}
		}
		
		return output.toString();
	}
}