//
//  @(#)Log.java	1.00	4/1/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/
//
package dip.misc;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * A very simple logging class that logs all data to stdout. Note that this
 * was implemented for speed and simplicity, rather than using the J2SDK
 * intrinsic Logging class.
 * <p>
 * By matching methods to log callers, we eliminate String or StringBuffer
 * construction and Object.toString() invocations, which provides a significant
 * speedup.
 * <p>
 */
public final class Log {
    /**
     * Set logging to NONE
     */
    public static final int LOG_NONE = 0;

    /**
     * Set logging to memory only
     */
    public static final int LOG_TO_MEMORY = 1;

    /**
     * Set logging to file only
     */
    public static final int LOG_TO_FILE = 2;

    private static int logLevel = LOG_NONE;
    private static boolean isLogging;
    private static BufferedWriter bw;

    private static String[] buffer;
    private static int bufferNext;

	/*
        HOW the buffer works
		a) string array of given size
		b) two position (start, next)
			start: start of buffer
			
			next: where next position in buffer is; this is start + 1
				  unless start is at 0; then next == start.
				  if start is at (length-1 '2' for a buffer length of 3)
				  then next = 0;
				  
		this prevents us from shifting the string array around in memory
		e.g.: for a buffer length of 3, using data a-z:
		
		0: a	START (0)
		1: b
		2: c	
		
		at this point our buffer is full; we want to add 'd':
		
		0: d	
		1: b	START (1) 	(start is incremented by 1)
		2: c
		
	
	
	*/


    /**
     * Private constructor
     */
    private Log() {
    }// Log()


    /**
     * Print the given Object to the output file / stdout
     * via the Object's toString() method. Follows with a
     * newline.
     */
    private static void println(final Object s) {
        if (isLogging) {
            synchronized (Log.class) {
                final String str = s.toString();
                memLog(str);
                if (logLevel == LOG_TO_FILE) {
                    if (bw == null) {
                        System.out.println(s);
                    } else {
                        try {
                            bw.write(str);
                            bw.newLine();
                            bw.flush();
                        } catch (final IOException e) {
                            System.err.println(e);
                        }
                    }
                }
            }
        }
    }// println()


    /**
     * Print text followed timing delta and current time.
     */
    public static String printTimed(final long lastTime, final Object s0) {
        final long now = System.currentTimeMillis();
        return String
                .format("%s %d ms [delta]; current: %d", s0, now - lastTime,
                        now);
    }// println()

    /**
     * Print the delta from the given time. Return the new time.
     */
    public static String printDelta(final long lastTime, final Object s0) {
        final long now = System.currentTimeMillis();
        return String.format("%s %d ms [delta]%d", s0, now - lastTime, now);
    }// printDelta()


    /**
     * Add to the memory buffer. Unsynchronized!
     */
    private static void memLog(final String s) {
        buffer[bufferNext] = s;
        bufferNext++;
        bufferNext = bufferNext >= buffer.length ? 0 : bufferNext;
    }// memLog()


    /**
     * Returns null if no memory buffer.
     */
    public static synchronized String getMemoryBuffer() {
        if (buffer == null) {
            return null;
        }

        final StringBuffer sb = new StringBuffer(8192);

        // print out buffer contents, starting from 'bufferNext' to end of
        // array, then continuing from beginning to 'bufferNext -1'.
        // if we hit a null entry, stop. (buffer not full)
        for (int i = bufferNext; i < buffer.length; i++) {
            final String s = buffer[i];
            if (s == null) {
                break;
            }

            sb.append(s);
            sb.append('\n');
        }

        for (int i = 0; i < bufferNext; i++) {
            final String s = buffer[i];
            if (s == null) {
                break;
            }

            sb.append(s);
            sb.append('\n');
        }

        return sb.toString();
    }// getMemoryBuffer()

}// class Log


