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

/* $Id: GoToPageDialog.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.awt.viewer;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Go to Page Dialog. Originally contributed by: Juergen Verwohlt:
 * Juergen.Verwohlt@jCatalog.com, Rainer Steinkuhle:
 * Rainer.Steinkuhle@jCatalog.com, Stanislav Gorkhover:
 * Stanislav.Gorkhover@jCatalog.com
 */
public class GoToPageDialog extends JDialog {

    /**
     *
     */
    private static final long serialVersionUID = 3256524020494932166L;
    private JTextField pageNumberField;
    private int pageNumber = -1;

    /**
     * Creates modal dialog with a given title, attached to a given frame.
     *
     * @param frame
     *            Frame to attach to
     * @param title
     *            dialog title
     * @param translator
     *            translator for localization
     */
    public GoToPageDialog(final Frame frame, final String title,
            final Translator translator) {
        super(frame, title, true);
        jbInit(translator);
        pack();
    }

    private void jbInit(final Translator translator) {
        final JPanel panel1 = new JPanel();
        final GridBagLayout gridBagLayout1 = new GridBagLayout();
        final JLabel pgNbLabel = new JLabel();
        this.pageNumberField = new JTextField();
        final JButton okButton = new JButton();
        final JButton cancelButton = new JButton();
        panel1.setLayout(gridBagLayout1);
        pgNbLabel.setText(translator.getString("Label.Page.number"));
        okButton.setText(translator.getString("Button.Ok"));
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                okButtonActionPerformed(e);
            }
        });
        cancelButton.setText(translator.getString("Button.Cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                cancelButtonActionPerformed(e);
            }
        });
        panel1.setMinimumSize(new Dimension(250, 78));
        getContentPane().add(panel1);
        panel1.add(pgNbLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                        10, 10, 10, 5), 0, 0));
        panel1.add(this.pageNumberField, new GridBagConstraints(1, 0, 1, 1,
                1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(10, 5, 10, 10), 0, 0));
        panel1.add(okButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,
                        0, 10, 5), 0, 0));
        panel1.add(cancelButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,
                        10, 10, 10), 0, 0));
    }

    private void okButtonActionPerformed(final ActionEvent e) {
        try {
            this.pageNumber = Integer.parseInt(this.pageNumberField.getText());
            dispose();
        } catch (final NumberFormatException nfe) {
            this.pageNumberField.setText("???");
        }

    }

    private void cancelButtonActionPerformed(final ActionEvent e) {
        this.pageNumber = -1;
        dispose();
    }

    /**
     * Returns page number, entered by user.
     *
     * @return the page number
     */
    public int getPageNumber() {
        return this.pageNumber;
    }
}
