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

/* $Id: PreviewDialog.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.render.awt.viewer;

// Originally contributed by:
// Juergen Verwohlt: Juergen.Verwohlt@jCatalog.com,
// Rainer Steinkuhle: Rainer.Steinkuhle@jCatalog.com,
// Stanislav Gorkhover: Stanislav.Gorkhover@jCatalog.com

// Java
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.awt.AWTRenderer;

/**
 * AWT Viewer main window. Surrounds a PreviewPanel with a bunch of pretty
 * buttons and controls.
 */
public class PreviewDialog extends JFrame implements StatusListener {

    /**
     *
     */
    private static final long serialVersionUID = -3012213792592743247L;
    /** The Translator for localization */
    protected Translator translator;
    /** The AWT renderer */
    protected AWTRenderer renderer;
    /** The FOUserAgent associated with this window */
    protected FOUserAgent foUserAgent;
    /** The originally configured target resolution */
    protected float configuredTargetResolution;
    /**
     * Renderable instance that can be used to reload and re-render a document
     * after modifications.
     */
    protected Renderable renderable;

    /** The JCombobox to rescale the rendered page view */
    private final JComboBox scale;

    /** The JLabel for the process status bar */
    private final JLabel processStatus;

    /** The JLabel information status bar */
    private final JLabel infoStatus;

    /** The main display area */
    private final PreviewPanel previewPanel;

    /** Formats the text in the scale combobox. */
    private final DecimalFormat percentFormat = new DecimalFormat("###0.0#",
            new DecimalFormatSymbols(Locale.ENGLISH));

    /**
     * Creates a new PreviewDialog that uses the given renderer.
     *
     * @param foUserAgent
     *            the user agent
     * @param renderable
     *            the Renderable instance that is used to reload/re-render a
     *            document after modifications.
     */
    public PreviewDialog // CSOK: MethodLength
    (final FOUserAgent foUserAgent, final Renderable renderable) {
        this.renderer = (AWTRenderer) foUserAgent.getRendererOverride();
        this.foUserAgent = foUserAgent;
        this.configuredTargetResolution = this.foUserAgent
                .getTargetResolution();
        this.renderable = renderable;
        this.translator = new Translator();

        // Commands aka Actions
        final Command printAction = new Command(
                this.translator.getString("Menu.Print"), "Print") {
            /**
             *
             */
            private static final long serialVersionUID = -6880301894303919528L;

            @Override
            public void doit() {
                startPrinterJob(true);
            }
        };
        final Command firstPageAction = new Command(
                this.translator.getString("Menu.First.page"), "firstpg") {
            /**
             *
             */
            private static final long serialVersionUID = 7569636648850763564L;

            @Override
            public void doit() {
                goToFirstPage();
            }
        };
        final Command previousPageAction = new Command(
                this.translator.getString("Menu.Prev.page"), "prevpg") {
            /**
             *
             */
            private static final long serialVersionUID = -6634525165008663802L;

            @Override
            public void doit() {
                goToPreviousPage();
            }
        };
        final Command nextPageAction = new Command(
                this.translator.getString("Menu.Next.page"), "nextpg") {
            /**
             *
             */
            private static final long serialVersionUID = -5999761598723872159L;

            @Override
            public void doit() {
                goToNextPage();
            }

        };
        final Command lastPageAction = new Command(
                this.translator.getString("Menu.Last.page"), "lastpg") {
            /**
             *
             */
            private static final long serialVersionUID = -3516741511052491703L;

            @Override
            public void doit() {
                goToLastPage();
            }
        };
        final Command reloadAction = new Command(
                this.translator.getString("Menu.Reload"), "reload") {
            /**
             *
             */
            private static final long serialVersionUID = 8947268310371884460L;

            @Override
            public void doit() {
                PreviewDialog.this.previewPanel.reload();
            }
        };
        final Command debugAction = new Command(
                this.translator.getString("Menu.Debug"), "debug") {
            /**
             *
             */
            private static final long serialVersionUID = -3968791195048290331L;

            // TODO use Translator
            @Override
            public void doit() {
                PreviewDialog.this.previewPanel.debug();
            }
        };
        final Command aboutAction = new Command(
                this.translator.getString("Menu.About"), "fopLogo") {
            /**
             *
             */
            private static final long serialVersionUID = 3401208052199171958L;

            @Override
            public void doit() {
                startHelpAbout();
            }
        };

        setTitle("FOP: AWT-" + this.translator.getString("Title.Preview"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Sets size to be 61%x90% of the screen size
        final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        // Needed due to bug in Sun's JVM 1.5 (6429775)
        pack();
        // Rather frivolous size - fits A4 page width in 1024x768 screen on my
        // desktop
        setSize(screen.width * 61 / 100, screen.height * 9 / 10);

        // Page view stuff
        this.previewPanel = new PreviewPanel(foUserAgent, renderable,
                this.renderer);
        getContentPane().add(this.previewPanel, BorderLayout.CENTER);
        this.previewPanel.addPageChangeListener(new PageChangeListener() {
            @Override
            public void pageChanged(final PageChangeEvent pce) {
                new ShowInfo().run();
            }
        });

        // Keyboard shortcuts - pgup/pgdn
        final InputMap im = this.previewPanel
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap am = this.previewPanel.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "nextPage");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "prevPage");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "firstPage");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "lastPage");
        this.previewPanel.getActionMap().put("nextPage", nextPageAction);
        this.previewPanel.getActionMap().put("prevPage", previousPageAction);
        this.previewPanel.getActionMap().put("firstPage", firstPageAction);
        this.previewPanel.getActionMap().put("lastPage", lastPageAction);

        // Scaling combobox
        this.scale = new JComboBox();
        this.scale.addItem(this.translator.getString("Menu.Fit.Window"));
        this.scale.addItem(this.translator.getString("Menu.Fit.Width"));
        this.scale.addItem("25%");
        this.scale.addItem("50%");
        this.scale.addItem("75%");
        this.scale.addItem("100%");
        this.scale.addItem("150%");
        this.scale.addItem("200%");
        this.scale.setMaximumSize(new Dimension(80, 24));
        this.scale.setPreferredSize(new Dimension(80, 24));
        this.scale.setSelectedItem("100%");
        this.scale.setEditable(true);
        this.scale.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                scaleActionPerformed(e);
            }
        });

        // Menu
        setJMenuBar(setupMenu());

        // Toolbar
        final JToolBar toolBar = new JToolBar();
        toolBar.add(printAction);
        toolBar.add(reloadAction);
        toolBar.addSeparator();
        toolBar.add(firstPageAction);
        toolBar.add(previousPageAction);
        toolBar.add(nextPageAction);
        toolBar.add(lastPageAction);
        toolBar.addSeparator(new Dimension(20, 0));
        toolBar.add(new JLabel(this.translator.getString("Menu.Zoom") + " "));
        toolBar.add(this.scale);
        toolBar.addSeparator();
        toolBar.add(debugAction);
        toolBar.addSeparator();
        toolBar.add(aboutAction);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        // Status bar
        final JPanel statusBar = new JPanel();
        this.processStatus = new JLabel();
        this.processStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(0, 3, 0, 0)));
        this.infoStatus = new JLabel();
        this.infoStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(0, 3, 0, 0)));

        statusBar.setLayout(new GridBagLayout());

        this.processStatus.setPreferredSize(new Dimension(200, 21));
        this.processStatus.setMinimumSize(new Dimension(200, 21));

        this.infoStatus.setPreferredSize(new Dimension(100, 21));
        this.infoStatus.setMinimumSize(new Dimension(100, 21));
        statusBar.add(this.processStatus, new GridBagConstraints(0, 0, 1, 0,
                2.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 3), 0, 0));
        statusBar.add(this.infoStatus, new GridBagConstraints(1, 0, 1, 0, 1.0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Creates and initialize the AWT Viewer main window.
     *
     * @param foUserAgent
     *            the FO user agent
     * @param renderable
     *            the target for the rendering
     * @param asMainWindow
     *            true if the window shall act as the main application window.
     * @return the newly initialized preview dialog
     */
    public static PreviewDialog createPreviewDialog(
            final FOUserAgent foUserAgent, final Renderable renderable,
            final boolean asMainWindow) {
        final PreviewDialog frame = new PreviewDialog(foUserAgent, renderable);

        if (asMainWindow) {
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(final WindowEvent we) {
                    System.exit(0);
                }
            });
        }

        // Centers the window
        final Dimension screenSize = Toolkit.getDefaultToolkit()
                .getScreenSize();
        final Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        frame.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
        frame.setStatus(frame.translator.getString("Status.Build.FO.tree"));
        frame.setVisible(true);
        return frame;
    }

    /**
     * Creates a new PreviewDialog that uses the given renderer.
     *
     * @param foUserAgent
     *            the user agent
     */
    public PreviewDialog(final FOUserAgent foUserAgent) {
        this(foUserAgent, null);
    }

    /**
     * Creates a new menubar to be shown in this window.
     *
     * @return the newly created menubar
     */
    private JMenuBar setupMenu() {
        final JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(this.translator.getString("Menu.File"));
        menu.setMnemonic(KeyEvent.VK_F);
        // Adds mostly the same actions, but without icons
        menu.add(new Command(this.translator.getString("Menu.Print"),
                KeyEvent.VK_P) {
            /**
             *
             */
            private static final long serialVersionUID = -2976985898434282500L;

            @Override
            public void doit() {
                startPrinterJob(true);
            }
        });
        // inputHandler must be set to allow reloading
        if (this.renderable != null) {
            menu.add(new Command(this.translator.getString("Menu.Reload"),
                    KeyEvent.VK_R) {
                /**
                 *
                 */
                private static final long serialVersionUID = -4544261978525322724L;

                @Override
                public void doit() {
                    reload();
                }
            });
        }
        menu.addSeparator();
        menu.add(new Command(this.translator.getString("Menu.Exit"),
                KeyEvent.VK_X) {
            /**
             *
             */
            private static final long serialVersionUID = -8905759323360351705L;

            @Override
            public void doit() {
                dispose();
            }
        });
        menuBar.add(menu);

        menu = new JMenu(this.translator.getString("Menu.View"));
        menu.setMnemonic(KeyEvent.VK_V);
        menu.add(new Command(this.translator.getString("Menu.First.page"),
                KeyEvent.VK_F) {
            /**
             *
             */
            private static final long serialVersionUID = -2631046724452265763L;

            @Override
            public void doit() {
                goToFirstPage();
            }
        });
        menu.add(new Command(this.translator.getString("Menu.Prev.page"),
                KeyEvent.VK_P) {
            /**
             *
             */
            private static final long serialVersionUID = 6675397958071542872L;

            @Override
            public void doit() {
                goToPreviousPage();
            }
        });
        menu.add(new Command(this.translator.getString("Menu.Next.page"),
                KeyEvent.VK_N) {
            /**
             *
             */
            private static final long serialVersionUID = -4858194621607248309L;

            @Override
            public void doit() {
                goToNextPage();
            }
        });
        menu.add(new Command(this.translator.getString("Menu.Last.page"),
                KeyEvent.VK_L) {
            /**
             *
             */
            private static final long serialVersionUID = 3897142567939832261L;

            @Override
            public void doit() {
                goToLastPage();
            }
        });
        menu.add(new Command(this.translator.getString("Menu.Go.to.Page"),
                KeyEvent.VK_G) {
            /**
             *
             */
            private static final long serialVersionUID = 6880660057702159492L;

            @Override
            public void doit() {
                showGoToPageDialog();
            }
        });
        menu.addSeparator();
        final JMenu subMenu = new JMenu(this.translator.getString("Menu.Zoom"));
        subMenu.setMnemonic(KeyEvent.VK_Z);
        subMenu.add(new Command("25%", 0) {
            /**
             *
             */
            private static final long serialVersionUID = 4713742105272842208L;

            @Override
            public void doit() {
                setScale(25.0);
            }
        });
        subMenu.add(new Command("50%", 0) {
            /**
             *
             */
            private static final long serialVersionUID = -5332339882429252679L;

            @Override
            public void doit() {
                setScale(50.0);
            }
        });
        subMenu.add(new Command("75%", 0) {
            /**
             *
             */
            private static final long serialVersionUID = -5780774159658218593L;

            @Override
            public void doit() {
                setScale(75.0);
            }
        });
        subMenu.add(new Command("100%", 0) {
            /**
             *
             */
            private static final long serialVersionUID = 4415207530658759319L;

            @Override
            public void doit() {
                setScale(100.0);
            }
        });
        subMenu.add(new Command("150%", 0) {
            /**
             *
             */
            private static final long serialVersionUID = -5342451404417788834L;

            @Override
            public void doit() {
                setScale(150.0);
            }
        });
        subMenu.add(new Command("200%", 0) {
            /**
             *
             */
            private static final long serialVersionUID = -8873321332690231703L;

            @Override
            public void doit() {
                setScale(200.0);
            }
        });
        menu.add(subMenu);
        menu.addSeparator();
        menu.add(new Command(this.translator.getString("Menu.Default.zoom"),
                KeyEvent.VK_D) {
            /**
             *
             */
            private static final long serialVersionUID = -2830315965636891278L;

            @Override
            public void doit() {
                setScale(100.0);
            }
        });
        menu.add(new Command(this.translator.getString("Menu.Fit.Window"),
                KeyEvent.VK_F) {
            /**
             *
             */
            private static final long serialVersionUID = 7322747086626210170L;

            @Override
            public void doit() {
                setScaleToFitWindow();
            }
        });
        menu.add(new Command(this.translator.getString("Menu.Fit.Width"),
                KeyEvent.VK_W) {
            /**
             *
             */
            private static final long serialVersionUID = -6320074587700100629L;

            @Override
            public void doit() {
                setScaleToFitWidth();
            }
        });
        menu.addSeparator();

        final ButtonGroup group = new ButtonGroup();
        final JRadioButtonMenuItem single = new JRadioButtonMenuItem(
                new Command(this.translator.getString("Menu.Single"),
                        KeyEvent.VK_S) {
                    /**
                     *
                     */
                    private static final long serialVersionUID = 6755071136212980710L;

                    @Override
                    public void doit() {
                        PreviewDialog.this.previewPanel
                                .setDisplayMode(PreviewPanel.SINGLE);
                    }
                });
        final JRadioButtonMenuItem cont = new JRadioButtonMenuItem(new Command(
                this.translator.getString("Menu.Continuous"), KeyEvent.VK_C) {
            /**
             *
             */
            private static final long serialVersionUID = 5175720999687635219L;

            @Override
            public void doit() {
                PreviewDialog.this.previewPanel
                        .setDisplayMode(PreviewPanel.CONTINUOUS);
            }
        });
        final JRadioButtonMenuItem facing = new JRadioButtonMenuItem(
                new Command(this.translator.getString("Menu.Facing"), 0) {
                    /**
                     *
                     */
                    private static final long serialVersionUID = -4973490643945148983L;

                    @Override
                    public void doit() {
                        PreviewDialog.this.previewPanel
                                .setDisplayMode(PreviewPanel.CONT_FACING);
                    }
                });
        single.setSelected(true);
        group.add(single);
        group.add(cont);
        group.add(facing);
        menu.add(single);
        menu.add(cont);
        menu.add(facing);

        menuBar.add(menu);

        menu = new JMenu(this.translator.getString("Menu.Help"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(new Command(this.translator.getString("Menu.About"),
                KeyEvent.VK_A) {
            /**
             *
             */
            private static final long serialVersionUID = -6828050572082036080L;

            @Override
            public void doit() {
                startHelpAbout();
            }
        });
        menuBar.add(menu);
        return menuBar;
    }

    /** {@inheritDoc} */
    @Override
    public void notifyRendererStopped() {
        reload();
    }

    private void reload() {
        setStatus(this.translator.getString("Status.Show"));
        this.previewPanel.reload();
    }

    /**
     * Changes the current visible page
     *
     * @param number
     *            the page number to go to
     */
    public void goToPage(final int number) {
        if (number != this.previewPanel.getPage()) {
            this.previewPanel.setPage(number);
            notifyPageRendered();
        }
    }

    /**
     * Shows the previous page.
     */
    public void goToPreviousPage() {
        final int page = this.previewPanel.getPage();
        if (page > 0) {
            goToPage(page - 1);
        }
    }

    /**
     * Shows the next page.
     */
    public void goToNextPage() {
        final int page = this.previewPanel.getPage();
        if (page < this.renderer.getNumberOfPages() - 1) {
            goToPage(page + 1);
        }
    }

    /** Shows the first page. */
    public void goToFirstPage() {
        goToPage(0);
    }

    /**
     * Shows the last page.
     */
    public void goToLastPage() {
        goToPage(this.renderer.getNumberOfPages() - 1);
    }

    /** Shows the About box */
    private void startHelpAbout() {
        final PreviewDialogAboutBox dlg = new PreviewDialogAboutBox(this,
                this.translator);
        // Centers the box
        final Dimension dlgSize = dlg.getPreferredSize();
        final Dimension frmSize = getSize();
        final Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x,
                (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setVisible(true);
    }

    /**
     * Shows "go to page" dialog and then goes to the selected page
     */
    private void showGoToPageDialog() {
        int currentPage = this.previewPanel.getPage();
        final GoToPageDialog d = new GoToPageDialog(this,
                this.translator.getString("Menu.Go.to.Page"), this.translator);
        d.setLocation((int) getLocation().getX() + 50, (int) getLocation()
                .getY() + 50);
        d.setVisible(true);
        currentPage = d.getPageNumber();
        if (currentPage < 1 || currentPage > this.renderer.getNumberOfPages()) {
            return;
        }
        currentPage--;
        goToPage(currentPage);
    }

    /**
     * Scales page image.
     *
     * @param scaleFactor
     *            the scale factor
     */
    public void setScale(final double scaleFactor) {
        this.scale
                .setSelectedItem(this.percentFormat.format(scaleFactor) + "%");
        this.previewPanel.setScaleFactor(scaleFactor / 100d);
    }

    /**
     * Sets the scaling so the contents fit into the window.
     */
    public void setScaleToFitWindow() {
        try {
            setScale(this.previewPanel.getScaleToFitWindow() * 100);
        } catch (final FOPException fopEx) {
            fopEx.printStackTrace();
        }
    }

    /**
     * Sets the scaling so the contents are spread over the whole width
     * available.
     */
    public void setScaleToFitWidth() {
        try {
            setScale(this.previewPanel.getScaleToFitWidth() * 100);
        } catch (final FOPException fopEx) {
            fopEx.printStackTrace();
        }
    }

    private void scaleActionPerformed(final ActionEvent e) {
        final int index = this.scale.getSelectedIndex();
        if (index == 0) {
            setScaleToFitWindow();
        } else if (index == 1) {
            setScaleToFitWidth();
        } else {
            final String item = (String) this.scale.getSelectedItem();
            setScale(Double.parseDouble(item.substring(0, item.indexOf('%'))));
        }
    }

    /**
     * Prints the document.
     *
     * @param showDialog
     *            true if show dialog
     */
    public void startPrinterJob(final boolean showDialog) {
        // Restore originally configured target resolution
        final float saveResolution = this.foUserAgent.getTargetResolution();
        this.foUserAgent.setTargetResolution(this.configuredTargetResolution);

        final PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPageable(this.renderer);
        if (!showDialog || pj.printDialog()) {
            try {
                pj.print();
            } catch (final PrinterException e) {
                e.printStackTrace();
            }
        }

        this.foUserAgent.setTargetResolution(saveResolution);
    }

    /**
     * Sets message to be shown in the status bar in a thread safe way.
     *
     * @param message
     *            the message
     */
    public void setStatus(final String message) {
        SwingUtilities.invokeLater(new ShowStatus(message));
    }

    /** This class is used to show status in a thread safe way. */
    private class ShowStatus implements Runnable {

        /** The message to display */
        private final String message;

        /**
         * Constructs ShowStatus thread
         *
         * @param message
         *            message to display
         */
        public ShowStatus(final String message) {
            this.message = message;
        }

        @Override
        public void run() {
            PreviewDialog.this.processStatus.setText(this.message.toString());
        }
    }

    /**
     * Updates the message to be shown in the info bar in a thread safe way.
     */
    @Override
    public void notifyPageRendered() {
        SwingUtilities.invokeLater(new ShowInfo());
    }

    /** This class is used to show info in a thread safe way. */
    private class ShowInfo implements Runnable {

        @Override
        public void run() {
            final String message = PreviewDialog.this.translator
                    .getString("Status.Page")
                    + " "
                    + (PreviewDialog.this.previewPanel.getPage() + 1)
                    + " "
                    + PreviewDialog.this.translator.getString("Status.of")
                    + " " + PreviewDialog.this.renderer.getNumberOfPages();
            PreviewDialog.this.infoStatus.setText(message);
        }
    }

    /**
     * Opens standard Swing error dialog box and reports given exception
     * details.
     *
     * @param e
     *            the Exception
     */
    public void reportException(final Exception e) {
        final String msg = this.translator.getString("Exception.Occured");
        setStatus(msg);
        JOptionPane.showMessageDialog(getContentPane(),
                "<html><b>" + msg + ":</b><br>" + e.getClass().getName()
                        + "<br>" + e.getMessage() + "</html>",
                this.translator.getString("Exception.Error"),
                JOptionPane.ERROR_MESSAGE);
    }
}
