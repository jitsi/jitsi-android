/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.util;

import java.io.*;
import java.util.logging.*;

/**
 * Slightly modified <tt>ScLogFormatter</tt> to Android specifics.
 *
 * @author Pawel Domas
 */
public class AndroidLogFormatter
    extends java.util.logging.Formatter
{
    private static String lineSeparator = System.getProperty("line.separator");

    private final boolean useAndroidLevels;

    public AndroidLogFormatter()
    {
        this.useAndroidLevels = AndroidConsoleHandler.isUseAndroidLevels();
    }

    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public synchronized String format(LogRecord record)
    {
        StringBuilder sb = new StringBuilder();

        if(!useAndroidLevels)
        {
            //log level
            sb.append(record.getLevel().getLocalizedName());
            sb.append(": ");
        }

        // Thread ID
        sb.append("[").append( record.getThreadID() ).append("] ");

        //caller method
        int lineNumber = inferCaller(record);
        String loggerName = record.getLoggerName();

        if(loggerName == null)
            loggerName = record.getSourceClassName();

        if(loggerName.startsWith("net.java.sip.communicator."))
        {
            sb.append(loggerName.substring("net.java.sip.communicator."
                                               .length()));
        }
        else
            sb.append(record.getLoggerName());

        if (record.getSourceMethodName() != null)
        {
            sb.append(".");
            sb.append(record.getSourceMethodName());

            //include the line number if we have it.
            if(lineNumber != -1)
                sb.append("().").append(Integer.toString(lineNumber));
            else
                sb.append("()");
        }
        sb.append(" ");
        sb.append(record.getMessage());
        sb.append(lineSeparator);
        if (record.getThrown() != null)
        {
            try
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            }
            catch (Exception ex)
            {
            }
        }
        return sb.toString();
    }

    /**
     * Try to extract the name of the class and method that called the current
     * log statement.
     *
     * @param record the logrecord where class and method name should be stored.
     *
     * @return the line number that the call was made from in the caller.
     */
    private int inferCaller(LogRecord record)
    {
        // Get the stack trace.
        StackTraceElement stack[] = (new Throwable()).getStackTrace();

        //the line number that the caller made the call from
        int lineNumber = -1;

        // First, search back to a method in the SIP Communicator Logger class.
        int ix = 0;
        while (ix < stack.length)
        {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (cname.equals("net.java.sip.communicator.util.Logger"))
            {
                break;
            }
            ix++;
        }
        // Now search for the first frame before the SIP Communicator Logger class.
        while (ix < stack.length)
        {
            StackTraceElement frame = stack[ix];
            lineNumber=stack[ix].getLineNumber();
            String cname = frame.getClassName();
            if (!cname.equals("net.java.sip.communicator.util.Logger"))
            {
                // We've found the relevant frame.
                record.setSourceClassName(cname);
                record.setSourceMethodName(frame.getMethodName());
                break;
            }
            ix++;
        }

        return lineNumber;
    }
}
