//
//  @(#)MapPanelSVGAction.java		4/2002
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


package dip.gui.map;

import dip.gui.ClientFrame;
import dip.gui.dialog.ErrorDialog;
import dip.gui.dialog.prefs.ExportPreferencePanel;
import dip.gui.dialog.prefs.GeneralPreferencePanel;
import dip.gui.swing.XJFileChooser;
import dip.misc.SimpleFileFilter;
import dip.misc.Utils;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.w3c.dom.Document;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

/**
 * Actions for SVG, including:
 * <ol>
 * <li>Print
 * <li>Exporting: JPG
 * <li>Exporting: PNG
 * <li>Exporting: SVG
 * <li>Exporting: PDF
 * </ol>
 * Takes care of error handling--errors go to a popup dialog. <p>
 * All events are executed in a separate thread.
 */
public class MapPanelSVGAction {

    /**
     * Constructor
     */
    private MapPanelSVGAction() {
    }


    /**
     * Error handler for Transcoding
     */
    private static class TranscoderErrorHandler implements org.apache.batik.transcoder.ErrorHandler {

        public TranscoderErrorHandler() {
        }

        @Override
        public void error(final TranscoderException ex) {
            showErrorDialog(ex);
        }

        @Override
        public void fatalError(final TranscoderException ex) {
            showErrorDialog(ex);
        }

        @Override
        public void warning(final TranscoderException ex) {
            showErrorDialog(ex);
        }

        private void showErrorDialog(final TranscoderException ex) {
            ErrorDialog.displayGeneral(null, ex);
        }// showErrorDialog()

    }// inner class TranscoderErrorHandler()


    /**
     * Implements Print
     */
    public static class Print implements ActionListener {
        private final MapPanel mp;

        public Print(final MapPanel mp) {
            this.mp = mp;
        }// Print()

        @Override
        public void actionPerformed(final ActionEvent e) {
            final Document document = mp.getSVGDocument();
            if (document == null) {
                return;
            }

            new Thread(getPMTG(mp), "jdipPrintThread") {
                @Override
                public void run() {
                    final PrintTranscoder pt = new PrintTranscoder();
                    pt.addTranscodingHint(PrintTranscoder.KEY_SHOW_PAGE_DIALOG,
                            Boolean.TRUE);
                    pt.addTranscodingHint(
                            PrintTranscoder.KEY_SHOW_PRINTER_DIALOG,
                            Boolean.TRUE);
                    pt.addTranscodingHint(PrintTranscoder.KEY_SCALE_TO_PAGE,
                            Boolean.TRUE);
                    pt.addTranscodingHint(PrintTranscoder.KEY_PAGE_ORIENTATION,
                            PrintTranscoder.VALUE_PAGE_ORIENTATION_LANDSCAPE);

                    // clone document
                    final Document cloneDoc = (Document) document.cloneNode(true);
                    final TranscoderInput input = new TranscoderInput(cloneDoc);
                    pt.transcode(input, null);

                    try {
                        pt.print();
                    } catch (final java.awt.print.PrinterException ex) {
                        ErrorDialog.displayGeneral(null, ex);
                    }
                }// run()
            }.start(); // Thread()
        }// actionPerformed()

    }// inner class Print


    /**
     * Implements exporting as JPG
     */
    public static class ExportJPG extends Export {
        /**
         * JPG export: default quality of 0.8 (80%)
         **/
        public ExportJPG(final MapPanel mp) {
            super(mp, new JPEGTranscoder(), SimpleFileFilter.JPG_FILTER);
        }// ExportJPG()

        /**
         * Set JPEG-specific options
         */
        @Override
        public void setOptions(final Transcoder t) {
            super.setOptions(t);
            //t.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE);
        }// ExportJPG()

    }// nested class ExportJPG


    /**
     * Implements exporting as PNG
     */
    public static class ExportPNG extends Export {
        /**
         * Create a PNG Export object
         */
        public ExportPNG(final MapPanel mp) {
            super(mp, new PNGTranscoder(), SimpleFileFilter.PNG_FILTER);
        }// ExportPNG()

        /**
         * Set PNG options
         */
        @Override
        public void setOptions(final Transcoder t) {
            super.setOptions(t);
            //t.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE);
        }// ExportJPG()

    }// nested class ExportPNG

    /**
     * Implements exporting as PDF
     */
    public static class ExportPDF extends Export {
        public ExportPDF(final MapPanel mp) {
            super(mp, new PDFTranscoder(), SimpleFileFilter.PDF_FILTER);
        }// ExportPDF()

        /**
         * DO NOT apply Image export settings for PDF.
         */
        @Override
        public void setOptions(final Transcoder t) {
        }// ExportJPG()
    }// nested class ExportPDF

	/* we need a new class, because SVG transcoder expects a Writer not an OutputStream */

    /**
     * Implements exporting as SVG
     */
    public static class ExportSVG implements ActionListener {
        private final MapPanel mp;

        public ExportSVG(final MapPanel mp) {
            this.mp = mp;
        }// ExportJPG()

        @Override
        public void actionPerformed(final ActionEvent e) {
            final Document document = mp.getSVGDocument();
            if (document == null) {
                return;
            }

            new Thread(getPMTG(mp), "jdipExportSVGThread") {
                @Override
                public void run() {
                    // get the file
                    final File file = getSaveFile(mp.getClientFrame(),
                            SimpleFileFilter.SVG_FILTER);
                    if (file == null) {
                        return;
                    }

                    // create a transcoder, set error handler
                    final SVGTranscoder transcoder = new SVGTranscoder();
                    transcoder.setErrorHandler(new TranscoderErrorHandler());
                    ExportPreferencePanel.applyTranscodingHints(transcoder);


                    // clone document
                    final Document cloneDoc = (Document) document
                            .cloneNode(true);

                    // create the transcoder input
                    final TranscoderInput input = new TranscoderInput(cloneDoc);
                    Writer writer = null;

                    try {
                        // create the transcoder output -- it must be a Writer!
                        writer = new BufferedWriter(new FileWriter(file));
                        final TranscoderOutput output = new TranscoderOutput(writer);

                        // save image
                        transcoder.transcode(input, output);

                        writer.flush();
                    } catch (final Exception ex) {
                        ErrorDialog.displayFileIO(null, ex, file.getName());
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (final IOException e) {
                                ErrorDialog
                                        .displayFileIO(null, e, file.getName());
                            }
                        }
                    }
                }// run()
            }.start(); // Thread()
        }// actionPerformed()
    }// nested class ExportSVG


    /**
     * Implements basic exporting
     */
    public static class Export implements ActionListener {
        private final MapPanel mp;
        private final SimpleFileFilter simpleFileFilter;
        private final Transcoder transcoder;

        /**
         * Export
         */
        public Export(final MapPanel mp, final Transcoder t,
                      final SimpleFileFilter simpleFileFilter) {
            if (mp == null || t == null || simpleFileFilter == null) {
                throw new IllegalArgumentException();
            }

            this.mp = mp;
            this.simpleFileFilter = simpleFileFilter;
            transcoder = t;
        }// ExportJPG()

        /**
         * Set any transcoding options.
         * By default, uses ExportPreferencePanel to set options;
         * thus, subclasses should call their super() constructor unless
         * they wish to override the preference settings.
         */
        public void setOptions(final Transcoder t) {
            ExportPreferencePanel.applyTranscodingHints(t);
        }// setOptions()


        /**
         * Perform the Export
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            final Document document = mp.getSVGDocument();
            if (document == null) {
                return;
            }

            new Thread(getPMTG(mp), "jdipExportThread") {
                @Override
                public void run() {
                    // get the file
                    final File file = getSaveFile(mp.getClientFrame(),
                            simpleFileFilter);
                    if (file == null) {
                        return;
                    }

                    // set error handler
                    transcoder.setErrorHandler(new TranscoderErrorHandler());

                    // set options
                    setOptions(transcoder);

                    // create the transcoder input, after cloning the document.
                    final Document cloneDoc = (Document) document
                            .cloneNode(true);
                    final TranscoderInput input = new TranscoderInput(cloneDoc);

                    OutputStream ostream = null;

                    try {
                        // create the transcoder output
                        ostream = new BufferedOutputStream(
                                new FileOutputStream(file));
                        final TranscoderOutput output = new TranscoderOutput(ostream);

                        // save image
                        transcoder.transcode(input, output);
                        ostream.flush();
                    } catch (final Exception ex) {
                        ErrorDialog.displayFileIO(null, ex, file.getName());
                    } finally {
                        if (ostream != null) {
                            try {
                                ostream.close();
                            } catch (final IOException e) {
                                ErrorDialog
                                        .displayFileIO(null, e, file.getName());
                            }
                        }
                    }
                }// run()
            }.start(); // Thread()
        }// actionPerformed()
    }// nested class Export


    /**
     * Popup a "save as" file requester
     */
    private static synchronized File getSaveFile(final ClientFrame cf,
                                                 final SimpleFileFilter simpleFileFilter) {
        final XJFileChooser chooser = XJFileChooser.getXJFileChooser();
        chooser.addFileFilter(simpleFileFilter);
        chooser.setFileFilter(simpleFileFilter);
        chooser.setCurrentDirectory(GeneralPreferencePanel.getDefaultGameDir());
        chooser.setSelectedFile(new File(cf.getPM().getSuggestedExportName()));
        final File file = chooser
                .displaySave(cf, Utils.getLocalString("MapPanel.export.title"));
        chooser.dispose();
        return file;
    }// getSaveFile()

    /**
     * Get the PM thread group from a MapPanel
     */
    private static ThreadGroup getPMTG(final MapPanel mp) {
        return mp.getClientFrame().getPM().getPMThreadGroup();
    }// getPMTG()


}// class MapSVGAction
