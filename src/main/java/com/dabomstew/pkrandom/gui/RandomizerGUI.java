package com.dabomstew.pkrandom.gui;

/*----------------------------------------------------------------------------*/
/*--  RandomizerGUI.java - the main GUI for the randomizer, containing the  --*/
/*--                       various options available and such.              --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import com.dabomstew.pkrandom.*;
import com.dabomstew.pkrandom.exceptions.InvalidSupplementFilesException;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import com.dabomstew.pkrandom.romhandlers.emeraldex.EmeraldExRomHandlerFactory;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

/**
 * 
 * @author Stewart
 */
public class RandomizerGUI extends javax.swing.JFrame {

    private static final String CONFIG_FILE_EXTENSION = "rnqs";

    private static final long serialVersionUID = 637989089525556154L;
    private RomHandler romHandler;
    protected RomHandler.Factory[] checkHandlers;

    private OperationDialog opDialog;
    private List<JCheckBox> tweakCheckboxes;
    private boolean presetMode;
    private GenRestrictions currentRestrictions;
    private final LayoutManager noTweaksLayout;

    private int bulkSaveAmount = -1;
    private int bulkSaveCounter = -1;
    private Path bulkParent = null;
    private static final String BULK_OPTION_1 = "Make 1 ROM";
    private static final String BULK_OPTION_10 = "Make 10 ROMs";
    private static final String BULK_OPTION_25 = "Make 25 ROMs";
    private static final String BULK_OPTION_50 = "Make 50 ROMs";
    private static final String BULK_OPTION_100 = "Make 100 ROMs";

    private String settingsFilePath = null;
    private boolean hasUnsavedSettings = false;

    // Settings
    private boolean autoUpdateEnabled;
    private boolean haveCheckedCustomNames;
    private final ImageIcon emptyIcon;
    {
        URL resource = getClass().getResource("/emptyIcon.png");
        emptyIcon = Optional.ofNullable(resource).map(ImageIcon::new).orElse(new ImageIcon());
    }

    private final ImageIcon appIcon;
    {
        URL resource = getClass().getResource("/icon.png");
        appIcon = Optional.ofNullable(resource).map(ImageIcon::new).orElse(new ImageIcon());
    }

    private static final Border DEFAULT_BORDER = BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(90, 90, 90, 255));

    java.util.ResourceBundle bundle;

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {

        FlatRobotoFont.install();
        FlatLaf.setPreferredFontFamily( FlatRobotoFont.FAMILY );
        FlatLaf.setPreferredLightFontFamily( FlatRobotoFont.FAMILY_LIGHT );
        FlatLaf.setPreferredSemiboldFontFamily( FlatRobotoFont.FAMILY_SEMIBOLD );
        FlatDarkLaf.setup();

        boolean autoupdate = true;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--noupdate")) {
                autoupdate = false;
                break;
            }
        }
        final boolean au = autoupdate;
        boolean onWindowsNativeLAF = false;

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new RandomizerGUI(au, false));
    }

    // constructor
    /**
     * Creates new form RandomizerGUI
     */
    public RandomizerGUI(boolean autoupdate, boolean onWindowsLAF) {

        setIconImage(appIcon.getImage());

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        bundle = ResourceBundle.getBundle("Bundle", Locale.ROOT); // NOI18N
        testForRequiredConfigs();
        checkHandlers = new RomHandler.Factory[] { new EmeraldExRomHandlerFactory() };
        autoUpdateEnabled = false;
        haveCheckedCustomNames = false;
        attemptReadConfig();
        if (!autoupdate) {
            // override autoupdate
            autoUpdateEnabled = false;
        }
        initComponents();
        initTweaksPanel();
        guiCleanup();
        scrollPaneSetup();
        noTweaksLayout = miscTweaksPanel.getLayout();
        initialiseState();

        boolean canWrite = attemptWriteConfig();
        if (!canWrite) {
            JOptionPane.showMessageDialog(null, bundle.getString("RandomizerGUI.cantWriteConfigFile"));
            autoUpdateEnabled = false;
        }
        setLocationRelativeTo(null);
        setVisible(true);
        if (!haveCheckedCustomNames) {
            checkCustomNames();
        }
    }

    private void guiCleanup() {
        // All systems: test for font size and adjust if required
        Font f = pokeLimitCB.getFont();
        if (f == null || !f.getFontName().equalsIgnoreCase(FlatRobotoFont.FAMILY) || f.getSize() != 12) {
            System.out.println("activating font face fix");
            Font regularFont = new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 12);
            Font boldFont = new Font(FlatRobotoFont.FAMILY, Font.BOLD, 12);
            fontFaceFix(this, regularFont, boldFont);
            for (JCheckBox cb : tweakCheckboxes) {
                cb.setFont(regularFont);
            }
            randomizerOptionsPane.setFont(new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 10));
        }
    }

    private void scrollPaneSetup() {
        /* @formatter:off */
        optionsScrollPane = new JScrollPane();
        optionsScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        optionsScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        optionsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel optionsContainerPanel = new JPanel();
        javax.swing.GroupLayout optionsContainerPanelLayout = new javax.swing.GroupLayout(optionsContainerPanel);
        optionsContainerPanel.setLayout(optionsContainerPanelLayout);
        optionsContainerPanelLayout
                .setHorizontalGroup(optionsContainerPanelLayout
                        .createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                optionsContainerPanelLayout
                                        .createSequentialGroup()
                                        .addComponent(
                                                pokemonTypesPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                typeChartPanel,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE))
                        .addComponent(pokemonMovesetsPanel,
                                javax.swing.GroupLayout.Alignment.TRAILING,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(trainersPokemonPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(wildPokemonPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(starterPokemonPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(staticPokemonPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(tmhmsPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addGroup(
                                optionsContainerPanelLayout
                                        .createSequentialGroup()
                                        .addComponent(
                                                baseStatsPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                abilitiesPanel,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE))
                        .addComponent(moveTutorsPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(inGameTradesPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(fieldItemsPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(pokemonEvolutionsPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(moveDataPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(miscTweaksPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(warpsPanel,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
        );
        optionsContainerPanelLayout
                .setVerticalGroup(optionsContainerPanelLayout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                optionsContainerPanelLayout
                                        .createSequentialGroup()
                                        .addGroup(
                                                optionsContainerPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(
                                                                baseStatsPanel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE)
                                                        .addComponent(
                                                                abilitiesPanel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE))
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(
                                                optionsContainerPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(
                                                                pokemonTypesPanel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE)
                                                        .addComponent(
                                                                typeChartPanel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE))
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                pokemonEvolutionsPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                starterPokemonPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                moveDataPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                pokemonMovesetsPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                trainersPokemonPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                wildPokemonPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                staticPokemonPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                tmhmsPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                moveTutorsPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                inGameTradesPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                fieldItemsPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                miscTweaksPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                warpsPanel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap(
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)));

        optionsScrollPane.setViewportView(optionsContainerPanel);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(optionsScrollPane,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                747, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(generalOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(loadQSButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(saveQSButton))
                            .addComponent(romInfoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(28, 28, 28)
                        .addComponent(gameMascotLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(openROMButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(saveROMButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bulkSaveSelection, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(seedInput, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(usePresetsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(settingsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(versionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(websiteLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup().addComponent(randomizerOptionsPane)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(generalOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gameMascotLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(openROMButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(saveROMButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(bulkSaveSelection)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(seedInput)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(usePresetsButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(settingsButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(romInfoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(loadQSButton)
                            .addComponent(saveQSButton))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(versionLabel)
                    .addComponent(websiteLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(randomizerOptionsPane))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(optionsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 457, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE))
        );

        randomizerOptionsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        randomizerOptionsPane.setMaximumSize(new Dimension(800, 15));
        //getContentPane().remove(randomizerOptionsPane);
        getContentPane().setLayout(layout);
        /* @formatter:on */
    }

    private void fontFaceFix(Container root, Font font, Font boldFont) {
        for (Component c : root.getComponents()) {
            if (c != versionLabel) {
                c.setFont(font);
            }
            if (c instanceof JComponent) {
                JComponent jc = (JComponent) c;
                Border b = jc.getBorder();
                if (b instanceof TitledBorder) {
                    ((TitledBorder) b).setTitleFont(boldFont);
                }
            }
            if (c instanceof Container) {
                fontFaceFix((Container) c, font, boldFont);
            }
        }

    }

    private void initTweaksPanel() {
        tweakCheckboxes = new ArrayList<>();
        int numTweaks = MiscTweak.allTweaks.size();
        for (int i = 0; i < numTweaks; i++) {
            MiscTweak ct = MiscTweak.allTweaks.get(i);
            JCheckBox tweakBox = new JCheckBox();
            tweakBox.setText(ct.getTweakName());
            tweakBox.setToolTipText(ct.getTooltipText());
            tweakCheckboxes.add(tweakBox);
        }
    }

    // config-related stuff

    private static final int TWEAK_COLS = 4;

    private GroupLayout makeTweaksLayout(List<JCheckBox> tweaks) {
        GroupLayout gl = new GroupLayout(miscTweaksPanel);
        int numTweaks = tweaks.size();

        // Handle columns
        SequentialGroup columnsGroup = gl.createSequentialGroup().addContainerGap();
        int numCols = Math.min(TWEAK_COLS, numTweaks);
        ParallelGroup[] colGroups = new ParallelGroup[numCols];
        for (int col = 0; col < numCols; col++) {
            if (col > 0) {
                columnsGroup.addGap(18, 18, 18);
            }
            colGroups[col] = gl.createParallelGroup(GroupLayout.Alignment.LEADING);
            columnsGroup.addGroup(colGroups[col]);
        }
        for (int tweak = 0; tweak < numTweaks; tweak++) {
            colGroups[tweak % numCols].addComponent(tweaks.get(tweak));
        }
        columnsGroup.addContainerGap();
        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(columnsGroup));

        // And rows
        SequentialGroup rowsGroup = gl.createSequentialGroup().addContainerGap();
        int numRows = (numTweaks - 1) / numCols + 1;
        ParallelGroup[] rowGroups = new ParallelGroup[numRows];
        for (int row = 0; row < numRows; row++) {
            if (row > 0) {
                rowsGroup.addPreferredGap(ComponentPlacement.UNRELATED);
            }
            rowGroups[row] = gl.createParallelGroup(GroupLayout.Alignment.BASELINE);
            rowsGroup.addGroup(rowGroups[row]);
        }
        for (int tweak = 0; tweak < numTweaks; tweak++) {
            rowGroups[tweak / numCols].addComponent(tweaks.get(tweak));
        }
        rowsGroup.addContainerGap();
        gl.setVerticalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(rowsGroup));
        return gl;
    }

    /**
     * Repurposed: now checks for converting old custom names format to new
     */
    private void checkCustomNames() {
        String[] cnamefiles = new String[] { SysConstants.tnamesFile, SysConstants.tclassesFile,
                SysConstants.nnamesFile };

        boolean foundFile = false;
        for (int file = 0; file < 3; file++) {
            File currentFile = new File(SysConstants.ROOT_PATH + cnamefiles[file]);
            if (currentFile.exists()) {
                foundFile = true;
                break;
            }
        }

        if (foundFile) {
            int response = JOptionPane.showConfirmDialog(RandomizerGUI.this,
                    bundle.getString("RandomizerGUI.convertNameFilesDialog.text"),
                    bundle.getString("RandomizerGUI.convertNameFilesDialog.title"), JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                try {
                    CustomNamesSet newNamesData = CustomNamesSet.importOldNames();
                    Path filePath = Paths.get(SysConstants.customNamesFile);
                    Files.deleteIfExists(filePath);
                    Files.createFile(filePath);
                    for (String str : newNamesData.getData()) {
                        Files.writeString(filePath, str + System.lineSeparator(), java.nio.file.StandardOpenOption.APPEND);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.convertNameFilesFailed"));
                }
            }

            haveCheckedCustomNames = true;
            attemptWriteConfig();
        }

    }

    private void attemptReadConfig() {
        File fh = new File(SysConstants.ROOT_PATH + "config.ini");
        if (!fh.exists() || !fh.canRead()) {
            return;
        }

        try {
            Scanner sc = new Scanner(fh, StandardCharsets.UTF_8);
            while (sc.hasNextLine()) {
                String q = sc.nextLine().trim();
                if (q.contains("//")) {
                    q = q.substring(0, q.indexOf("//")).trim();
                }
                if (!q.isEmpty()) {
                    String[] tokens = q.split("=", 2);
                    if (tokens.length == 2) {
                        String key = tokens[0].trim();
                        if (key.equalsIgnoreCase("autoupdate")) {
                            autoUpdateEnabled = Boolean.parseBoolean(tokens[1].trim());
                        } else if (key.equalsIgnoreCase("checkedcustomnames172")) {
                            haveCheckedCustomNames = Boolean.parseBoolean(tokens[1].trim());
                        } else if (key.equalsIgnoreCase("usescrollpane")) {
                        }
                    }
                }
            }
            sc.close();
        } catch (IOException ignored) {
            /* Do Nothing */
        }
    }

    private boolean attemptWriteConfig() {
        File fh = new File(SysConstants.ROOT_PATH + "config.ini");
        if (fh.exists() && !fh.canWrite()) {
            return false;
        }

        try {
            PrintStream ps = new PrintStream(Files.newOutputStream(fh.toPath()), true, "UTF-8");
            ps.println("autoupdate=" + autoUpdateEnabled);
            ps.println("checkedcustomnames=true");
            ps.println("checkedcustomnames172=" + haveCheckedCustomNames);
            ps.close();
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    private void testForRequiredConfigs() {
        try {
            Utils.testForRequiredConfigs();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                    String.format(bundle.getString("RandomizerGUI.configFileMissing"), e.getMessage()));
            System.exit(1);
            return;
        }
    }

    // form initial state

    private void initialiseState() {
        this.romHandler = null;
        this.currentRestrictions = null;
        this.websiteLinkLabel.setText("<html><a href=\"" + SysConstants.WEBSITE_URL + "\">" + SysConstants.WEBSITE_URL
                + "</a>");
        initialFormState();
        this.romOpenChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
        this.romSaveChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
        if (new File(SysConstants.ROOT_PATH + "settings/").exists()) {
            this.qsOpenChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH + "settings/"));
            this.qsSaveChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH + "settings/"));
        } else {
            this.qsOpenChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
            this.qsSaveChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initialFormState() {

        this.bulkSaveSelection.setEnabled(false);

        // Disable all rom components
        this.goRemoveTradeEvosCheckBox.setEnabled(false);
        this.goRemoveTradeEvosCheckBox.setVisible(false);
        this.goUpdateMovesCheckBox.setEnabled(false);
        this.goUpdateMovesCheckBox.setVisible(false);
        this.goUpdateMovesLegacyCheckBox.setEnabled(false);
        this.goUpdateMovesLegacyCheckBox.setVisible(false);
        this.goCondenseEvosCheckBox.setEnabled(false);
        this.goCondenseEvosCheckBox.setVisible(false);

        this.goRemoveTradeEvosCheckBox.setSelected(false);
        this.goUpdateMovesCheckBox.setSelected(false);
        this.goUpdateMovesLegacyCheckBox.setSelected(false);
        this.goCondenseEvosCheckBox.setSelected(false);

        // this.goUpdateMovesLegacyCheckBox.setVisible(true);
        this.pokeLimitCB.setEnabled(false);
        this.pokeLimitCB.setSelected(false);
        this.pokeLimitBtn.setEnabled(false);
        this.pokeLimitBtn.setVisible(true);
        this.pokeLimitCB.setVisible(true);
        this.raceModeCB.setEnabled(false);
        this.raceModeCB.setSelected(false);
        this.brokenMovesCB.setEnabled(false);
        this.brokenMovesCB.setSelected(false);

        this.riRomNameLabel.setText(bundle.getString("RandomizerGUI.noRomLoaded"));
        this.riRomCodeLabel.setText("");
        this.riRomSupportLabel.setText("");

        this.loadQSButton.setEnabled(false);
        this.saveQSButton.setEnabled(false);

        this.pbsChangesUnchangedRB.setEnabled(false);
        this.pbsChangesRandomRB.setEnabled(false);
        this.pbsChangesShuffleRB.setEnabled(false);
        this.pbsChangesRandomBSTRB.setEnabled(false);
        this.pbsChangesRandomBSTPERCRB.setEnabled(false);
        this.pbsChangesEqualizeRB.setEnabled(false);
        this.pbsChangesUnchangedRB.setSelected(true);
        this.pbsStandardEXPCurvesCB.setEnabled(false);
        this.pbsStandardEXPCurvesCB.setSelected(false);
        this.pbsFollowEvolutionsCB.setEnabled(false);
        this.pbsFollowEvolutionsCB.setSelected(false);
        this.pbsUpdateStatsCB.setEnabled(false);
        this.pbsUpdateStatsCB.setSelected(false);
        this.pbsUpdateStatsCB.setVisible(false);
        this.pbsBaseStatRangeSlider.setEnabled(false);
        this.pbsBaseStatRangeSlider.setValue(this.pbsBaseStatRangeSlider.getMinimum());
        this.pbsDontRandomizeRatioCB.setEnabled(false);
        this.pbsDontRandomizeRatioCB.setSelected(false);
        this.pbsEvosBuffStatsCB.setEnabled(false);
        this.pbsEvosBuffStatsCB.setSelected(false);

        this.abilitiesPanel.setVisible(true);
        this.paUnchangedRB.setEnabled(false);
        this.paRandomizeRB.setEnabled(false);
        this.paWonderGuardCB.setEnabled(false);
        this.paFollowEvolutionsCB.setEnabled(false);
        this.paBanTrappingCB.setEnabled(false);
        this.paBanNegativeCB.setEnabled(false);
        this.paUnchangedRB.setSelected(true);
        this.paWonderGuardCB.setSelected(false);
        this.paFollowEvolutionsCB.setSelected(false);
        this.paBanTrappingCB.setSelected(false);
        this.paBanNegativeCB.setSelected(false);

        this.spCustomPoke1Chooser.setEnabled(false);
        this.spCustomPoke2Chooser.setEnabled(false);
        this.spCustomPoke3Chooser.setEnabled(false);
        this.spCustomPoke1Chooser.setSelectedIndex(0);
        this.spCustomPoke1Chooser.setModel(new DefaultComboBoxModel(new String[] { "--" }));
        this.spCustomPoke2Chooser.setSelectedIndex(0);
        this.spCustomPoke2Chooser.setModel(new DefaultComboBoxModel(new String[] { "--" }));
        this.spCustomPoke3Chooser.setSelectedIndex(0);
        this.spCustomPoke3Chooser.setModel(new DefaultComboBoxModel(new String[] { "--" }));
        this.spCustomRB.setEnabled(false);
        this.spRandomRB.setEnabled(false);
        this.spRandom2EvosRB.setEnabled(false);
        this.spRandom1EvosRB.setEnabled(false);
        this.spRandom0EvosRB.setEnabled(false);
        this.spUnchangedRB.setEnabled(false);
        this.spUnchangedRB.setSelected(true);
        this.spHeldItemsCB.setEnabled(false);
        this.spHeldItemsCB.setSelected(false);
        this.spHeldItemsCB.setVisible(true);
        this.spHeldItemsBanBadCB.setEnabled(false);
        this.spHeldItemsBanBadCB.setSelected(false);
        this.spHeldItemsBanBadCB.setVisible(true);
        this.spBanLegendaryStartersCB.setEnabled(false);
        this.spBanLegendaryStartersCB.setSelected(false);
        this.spBanLegendaryStartersCB.setVisible(true);
        this.spOnlyLegendaryStartersCB.setEnabled(false);
        this.spOnlyLegendaryStartersCB.setSelected(false);
        this.spOnlyLegendaryStartersCB.setVisible(true);

        this.mdRandomAccuracyCB.setEnabled(false);
        this.mdRandomAccuracyCB.setSelected(false);
        this.mdRandomPowerCB.setEnabled(false);
        this.mdRandomPowerCB.setSelected(false);
        this.mdRandomPPCB.setEnabled(false);
        this.mdRandomPPCB.setSelected(false);
        this.mdRandomTypeCB.setEnabled(false);
        this.mdRandomTypeCB.setSelected(false);
        this.mdRandomCategoryCB.setEnabled(false);
        this.mdRandomCategoryCB.setSelected(false);
        this.mdRandomCategoryCB.setVisible(true);

        this.pmsRandomTotalRB.setEnabled(false);
        this.pmsRandomTypeRB.setEnabled(false);
        this.pmsUnchangedRB.setEnabled(false);
        this.pmsUnchangedRB.setSelected(true);
        this.pmsMetronomeOnlyRB.setEnabled(false);
        this.pms4MovesCB.setEnabled(false);
        this.pms4MovesCB.setSelected(false);
        this.pms4MovesCB.setVisible(true);
        this.pmsReorderDamagingMovesCB.setEnabled(false);
        this.pmsReorderDamagingMovesCB.setSelected(false);
        this.pmsForceGoodDamagingCB.setEnabled(false);
        this.pmsForceGoodDamagingCB.setSelected(false);
        this.pmsForceGoodDamagingSlider.setEnabled(false);
        this.pmsForceGoodDamagingSlider.setValue(this.pmsForceGoodDamagingSlider.getMinimum());

        this.ptRandomFollowEvosRB.setEnabled(false);
        this.ptRandomTotalRB.setEnabled(false);
        this.ptUnchangedRB.setEnabled(false);
        this.ptUnchangedRB.setSelected(true);

        this.tpPowerLevelsCB.setEnabled(false);
        this.tpRandomRB.setEnabled(false);
        this.tpRivalCarriesStarterCB.setEnabled(false);
        this.tpTypeThemedRB.setEnabled(false);
        this.tpTypeMatchRB.setEnabled(false);
        this.tpTypeWeightingCB.setEnabled(false);
        this.tpNoLegendariesCB.setEnabled(false);
        this.tpNoEarlyShedinjaCB.setEnabled(false);
        this.tpNoEarlyShedinjaCB.setVisible(true);
        this.tpUnchangedRB.setEnabled(false);
        this.tpForceFullyEvolvedCB.setEnabled(false);
        this.tpForceFullyEvolvedSlider.setEnabled(false);
        this.tpLevelModifierCB.setEnabled(false);
        this.tpLevelModifierSlider.setEnabled(false);

        this.tpUnchangedRB.setSelected(true);
        this.tpPowerLevelsCB.setSelected(false);
        this.tpRivalCarriesStarterCB.setSelected(false);
        this.tpTypeWeightingCB.setSelected(false);
        this.tpNoLegendariesCB.setSelected(false);
        this.tpNoEarlyShedinjaCB.setSelected(false);
        this.tpForceFullyEvolvedCB.setSelected(false);
        this.tpForceFullyEvolvedSlider.setValue(this.tpForceFullyEvolvedSlider.getMinimum());
        this.tpLevelModifierCB.setSelected(false);
        this.tpLevelModifierSlider.setValue(0);

        this.tnRandomizeCB.setEnabled(false);
        this.tcnRandomizeCB.setEnabled(false);

        this.tnRandomizeCB.setSelected(false);
        this.tcnRandomizeCB.setSelected(false);

        this.tnRandomizeCB.setVisible(true);
        this.tcnRandomizeCB.setVisible(true);

        this.wpUnchangedRB.setEnabled(false);
        this.wpRandomRB.setEnabled(false);
        this.wpArea11RB.setEnabled(false);
        this.wpGlobalRB.setEnabled(false);
        this.wpUnchangedRB.setSelected(true);

        this.wpARNoneRB.setEnabled(false);
        this.wpARCatchEmAllRB.setEnabled(false);
        this.wpARTypeThemedRB.setEnabled(false);
        this.wpARSimilarStrengthRB.setEnabled(false);
        this.wpARNoneRB.setSelected(true);

        this.wpUseTimeCB.setEnabled(false);
        this.wpUseTimeCB.setVisible(true);
        this.wpUseTimeCB.setSelected(false);

        this.wpNoLegendariesCB.setEnabled(false);
        this.wpNoLegendariesCB.setSelected(false);

        this.wpCatchRateCB.setEnabled(false);
        this.wpCatchRateCB.setSelected(false);
        this.wpCatchRateSlider.setEnabled(false);
        this.wpCatchRateSlider.setValue(this.wpCatchRateSlider.getMinimum());

        this.wpHeldItemsCB.setEnabled(false);
        this.wpHeldItemsCB.setSelected(false);
        this.wpHeldItemsCB.setVisible(true);
        this.wpHeldItemsBanBadCB.setEnabled(false);
        this.wpHeldItemsBanBadCB.setSelected(false);
        this.wpHeldItemsBanBadCB.setVisible(true);
        
        this.wpCondenseEncounterSlotsCB.setEnabled(false);
        this.wpCondenseEncounterSlotsCB.setSelected(false);
        this.wpCondenseEncounterSlotsCB.setVisible(false);

        this.stpRandomL4LRB.setEnabled(false);
        this.stpRandomTotalRB.setEnabled(false);
        this.stpUnchangedRB.setEnabled(false);
        this.stpUnchangedRB.setSelected(true);

        this.tmmRandomRB.setEnabled(false);
        this.tmmUnchangedRB.setEnabled(false);
        this.tmmUnchangedRB.setSelected(true);

        this.thcRandomTotalRB.setEnabled(false);
        this.thcRandomTypeRB.setEnabled(false);
        this.thcUnchangedRB.setEnabled(false);
        this.thcFullRB.setEnabled(false);
        this.thcUnchangedRB.setSelected(true);

        this.tmKeepFieldMovesCB.setEnabled(false);
        this.tmKeepFieldMovesCB.setSelected(false);
        this.tmFullHMCompatCB.setEnabled(false);
        this.tmFullHMCompatCB.setSelected(false);
        this.tmFullHMCompatCB.setVisible(false);
        this.tmForceGoodDamagingCB.setEnabled(false);
        this.tmForceGoodDamagingCB.setSelected(false);
        this.tmForceGoodDamagingSlider.setEnabled(false);
        this.tmForceGoodDamagingSlider.setValue(this.tmForceGoodDamagingSlider.getMinimum());

        this.mtmRandomRB.setEnabled(false);
        this.mtmUnchangedRB.setEnabled(false);
        this.mtmUnchangedRB.setSelected(true);

        this.mtcRandomTotalRB.setEnabled(false);
        this.mtcRandomTypeRB.setEnabled(false);
        this.mtcUnchangedRB.setEnabled(false);
        this.mtcFullRB.setEnabled(false);
        this.mtcUnchangedRB.setSelected(true);

        this.wrUnchangedRB.setEnabled(false);
        this.wrRandomRB.setEnabled(false);
        this.wrKeepUselessDeadends.setEnabled(false);
        this.wrRemoveGymOrderLogic.setEnabled(false);

        this.tcUnchangedRB.setEnabled(false);
        this.tcRandomShuffleRowsRB.setEnabled(false);
        this.tcRandomShuffleRB.setEnabled(false);
        this.tcRandomTotalRB.setEnabled(false);

        this.mtKeepFieldMovesCB.setEnabled(false);
        this.mtKeepFieldMovesCB.setSelected(false);
        this.mtKeepFieldMovesCB.setVisible(false);

        this.mtForceGoodDamagingCB.setEnabled(false);
        this.mtForceGoodDamagingCB.setSelected(false);
        this.mtForceGoodDamagingSlider.setEnabled(false);
        this.mtForceGoodDamagingSlider.setValue(this.mtForceGoodDamagingSlider.getMinimum());

        this.mtMovesPanel.setVisible(true);
        this.mtCompatPanel.setVisible(true);
        this.mtNoExistLabel.setVisible(false);

        this.igtUnchangedRB.setEnabled(false);
        this.igtGivenOnlyRB.setEnabled(false);
        this.igtBothRB.setEnabled(false);
        this.igtUnchangedRB.setSelected(true);

        this.igtRandomItemCB.setEnabled(false);
        this.igtRandomItemCB.setSelected(false);
        this.igtRandomItemCB.setVisible(true);

        this.igtRandomIVsCB.setEnabled(false);
        this.igtRandomIVsCB.setSelected(false);
        this.igtRandomIVsCB.setVisible(true);

        this.igtRandomOTCB.setEnabled(false);
        this.igtRandomOTCB.setSelected(false);
        this.igtRandomOTCB.setVisible(true);

        this.igtRandomNicknameCB.setEnabled(false);
        this.igtRandomNicknameCB.setSelected(false);

        this.fiUnchangedRB.setEnabled(false);
        this.fiShuffleRB.setEnabled(false);
        this.fiRandomRB.setEnabled(false);
        this.fiUnchangedRB.setSelected(true);

        this.fiBanBadCB.setEnabled(false);
        this.fiBanBadCB.setSelected(false);
        this.fiBanBadCB.setVisible(true);

        this.fiRandomizeGivenItemsCB.setEnabled(false);
        this.fiRandomizeGivenItemsCB.setSelected(false);
        this.fiRandomizeGivenItemsCB.setVisible(true);

        this.fiRandomizePickupTablesCB.setEnabled(false);
        this.fiRandomizePickupTablesCB.setSelected(false);
        this.fiRandomizePickupTablesCB.setVisible(true);

        this.fiRandomizeBerryTreesCB.setEnabled(false);
        this.fiRandomizeBerryTreesCB.setSelected(false);
        this.fiRandomizeBerryTreesCB.setVisible(true);

        this.fiRandomizeMartsCB.setEnabled(false);
        this.fiRandomizeMartsCB.setSelected(false);
        this.fiRandomizeMartsCB.setVisible(true);

        this.fiAllMartsHaveBallAndRepel.setEnabled(false);
        this.fiAllMartsHaveBallAndRepel.setSelected(false);
        this.fiAllMartsHaveBallAndRepel.setVisible(true);

        this.fiRandomItemPrices.setEnabled(false);
        this.fiRandomItemPrices.setSelected(false);
        this.fiRandomItemPrices.setVisible(true);

        this.tpRandomFrontier.setEnabled(false);
        this.tpRandomFrontier.setSelected(false);
        this.tpRandomFrontier.setVisible(true);

        this.tpFillBossTeams.setEnabled(false);
        this.tpFillBossTeams.setSelected(false);
        this.tpFillBossTeams.setVisible(true);

        this.peUnchangedRB.setSelected(true);
        this.peUnchangedRB.setEnabled(false);
        this.peRandomRB.setEnabled(false);
        this.peForceChangeCB.setSelected(false);
        this.peForceChangeCB.setEnabled(false);
        this.peThreeStagesCB.setSelected(false);
        this.peThreeStagesCB.setEnabled(false);
        this.peSameTypeCB.setSelected(false);
        this.peSameTypeCB.setEnabled(false);
        this.peSimilarStrengthCB.setSelected(false);
        this.peSimilarStrengthCB.setEnabled(false);

        for (JCheckBox cb : tweakCheckboxes) {
            cb.setVisible(true);
            cb.setEnabled(false);
            cb.setSelected(false);
        }

        this.mtNoneAvailableLabel.setVisible(false);
        miscTweaksPanel.setLayout(makeTweaksLayout(tweakCheckboxes));

        this.gameMascotLabel.setIcon(emptyIcon);
    }

    // rom loading

    private void loadROM() {
        romOpenChooser.setSelectedFile(null);
        int returnVal = romOpenChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File fh = romOpenChooser.getSelectedFile();
            loadRomFile(fh);
        }

    }

    private void loadRomFile(File fh) {
        try {
            Utils.validateRomFile(fh);
        } catch (Utils.InvalidROMException e) {
            switch (e.getType()) {
            case LENGTH:
                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString("RandomizerGUI.tooShortToBeARom"), fh.getName()));
                return;
            case ZIP_FILE:
                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString("RandomizerGUI.openedZIPfile"), fh.getName()));
                return;
            case RAR_FILE:
                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString("RandomizerGUI.openedRARfile"), fh.getName()));
                return;
            case IPS_FILE:
                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString("RandomizerGUI.openedIPSfile"), fh.getName()));
                return;
            case UNREADABLE:
                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString("RandomizerGUI.unreadableRom"), fh.getName()));
                return;
            }
        }

        for (RomHandler.Factory rhf : checkHandlers) {
            String absolutePath = fh.getAbsolutePath();
            if (rhf.isLoadable(absolutePath)) {
                this.romHandler = rhf.create(RandomSource.instance());
                opDialog = new OperationDialog(bundle.getString("RandomizerGUI.loadingText"), this, true);
                Thread t = new Thread(() -> {
                    boolean romLoaded = false;
                    SwingUtilities.invokeLater(() -> opDialog.setVisible(true));
                    try {
                        tryLoadCustomConfig();
                        romLoaded = RandomizerGUI.this.romHandler.loadRom(absolutePath);
                        if(!romLoaded) {
                            JOptionPane.showMessageDialog(RandomizerGUI.this,
                                    String.format(bundle.getString("RandomizerGUI.unsupportedRom"), fh.getName()));
                        }
                    } catch (Exception ex) {
                        attemptToLogException(ex, "RandomizerGUI.loadFailed", "RandomizerGUI.loadFailedNoLog");
                    }
                    final boolean loadSuccess = romLoaded;
                    SwingUtilities.invokeLater(() -> {
                        RandomizerGUI.this.opDialog.setVisible(false);
                        RandomizerGUI.this.initialFormState();
                        if (loadSuccess) {
                            romLoaded();
                            tryLoadConfig();
                        }
                    });
                });
                t.start();

                return;
            }
        }
        JOptionPane.showMessageDialog(this,
                String.format(bundle.getString("RandomizerGUI.unsupportedRom"), fh.getName()));
    }

    /**
     * When the rom is loaded we check the directory the rom is in for any config files, if there are multiple priority
     * goes to any with the rom code name, then alphabetic comparison
     * If none are found in the rom directory we check the jar directory
     */
    private void tryLoadConfig() {

        try {

            String romCode = romHandler.getROMCode().toLowerCase();
            File romDirectory = Paths.get(romHandler.loadedFilename()).getParent().toFile();

            if (settingsFilePath != null) {
                File fh = new File(settingsFilePath);
                if (fh.exists()) {
                    loadSettingsFile(fh);
                    return;
                } else {
                    settingsFilePath = null;
                }
            }

            FileFilter configExtFilter = file -> file.getName().toLowerCase().trim().endsWith("." + CONFIG_FILE_EXTENSION);
            Comparator<File> configPriorityComparator =
                    Comparator.<File, Boolean>comparing(f -> f.getName().toLowerCase().trim().contains(romCode), Comparator.reverseOrder())
                              .thenComparing(File::getName);


            System.out.println("Checking for settings file in " + romDirectory.getAbsolutePath());
            File[] romDirectoryFiles = romDirectory.listFiles(configExtFilter);

            if (romDirectoryFiles != null && romDirectoryFiles.length > 0)
            {
                ArrayList<File> matchingFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(romDirectoryFiles)));
                if (!matchingFiles.isEmpty()) {
                    matchingFiles.sort(configPriorityComparator);
                    System.out.println("Trying to load settings file from rom directory " + matchingFiles.get(0).getName());
                    loadSettingsFile(matchingFiles.get(0));
                    return;
                }
            } else {
                System.out.println("No settings file found in " + romDirectory.getAbsolutePath());
            }


            File jarDirectory = FileFunctions.getJarDirectory();
            System.out.println("Checking for settings file in " + jarDirectory.getAbsolutePath());
            File[] jarDirectoryFiles = jarDirectory.listFiles(configExtFilter);

            if (jarDirectoryFiles != null && jarDirectoryFiles.length > 0)
            {
                ArrayList<File> matchingFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(jarDirectoryFiles)));
                if (!matchingFiles.isEmpty()) {
                    matchingFiles.sort(configPriorityComparator);
                    System.out.println("Trying to load settings file from rom directory " + matchingFiles.get(0).getName());
                    loadSettingsFile(matchingFiles.get(0));
                }
            } else {
                System.out.println("No settings file found in " + jarDirectory.getAbsolutePath());
            }


        } catch (Exception e) {
            /* Do nothing if we error searching for a config file, they can enter one themselves */
        }

        System.out.println("No Settings (.rnqs) file found for rom code " + romHandler.getROMCode());

    }

    private void tryLoadCustomConfig() {
        try {
            FileFilter configExtFilter = file -> file.getName().toLowerCase().trim().endsWith("_custom_config.json");
            File jarDirectory = FileFunctions.getJarDirectory();
            System.out.println("Checking for custom config file in " + jarDirectory.getAbsolutePath());
            File[] jarDirectoryFiles = jarDirectory.listFiles(configExtFilter);

            if (jarDirectoryFiles != null && jarDirectoryFiles.length > 0)
            {
                ArrayList<File> matchingFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(jarDirectoryFiles)));
                if (!matchingFiles.isEmpty()) {
                    System.out.println("Trying to load custom config file from rom directory " + matchingFiles.get(0).getName());

                    try (FileReader reader = new FileReader(matchingFiles.get(0))) {
                        Gson gson = new Gson();
                        CustomConfig config = gson.fromJson(reader, CustomConfig.class);
                        config.init();
                        romHandler.setCustomConfig(config);
                    } catch (IOException e) {
                        romHandler.setCustomConfig(new CustomConfig());
                        System.out.println("Failed to load custom json file");
                        JOptionPane.showMessageDialog(this,
                                String.format(bundle.getString("RandomizerGUI.customConfigLoadFailed")));
                    }

                }
            } else {
                romHandler.setCustomConfig(new CustomConfig());
                System.out.println("No custom config file found in " + jarDirectory.getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString("RandomizerGUI.customConfigLoadFailed")));
            }

        } catch (Exception e) {
            romHandler.setCustomConfig(new CustomConfig());
            JOptionPane.showMessageDialog(this,
                    String.format(bundle.getString("RandomizerGUI.customConfigLoadFailed")));
            /* Do nothing if we error searching for a custom config file, they can enter one themselves */
        }
    }

    private void romLoaded() {
        try {
            this.currentRestrictions = null;
            this.riRomNameLabel.setText(this.romHandler.getROMName());
            this.riRomCodeLabel.setText(this.romHandler.getROMCode());
            this.riRomSupportLabel.setText(bundle.getString("RandomizerGUI.romSupportPrefix") + " "
                    + this.romHandler.getSupportLevel());
            this.goUpdateMovesCheckBox.setSelected(false);
            this.goUpdateMovesCheckBox.setSelected(false);
            this.goUpdateMovesCheckBox.setEnabled(true);
            this.goUpdateMovesLegacyCheckBox.setSelected(false);
            this.goUpdateMovesLegacyCheckBox.setEnabled(false);
            // this.goUpdateMovesLegacyCheckBox.setVisible(true);
            this.goRemoveTradeEvosCheckBox.setSelected(false);
            this.goRemoveTradeEvosCheckBox.setEnabled(true);
            this.goCondenseEvosCheckBox.setSelected(false);
            this.goCondenseEvosCheckBox.setEnabled(true);
            this.raceModeCB.setSelected(false);
            this.raceModeCB.setEnabled(true);

            this.pokeLimitCB.setSelected(false);
            this.pokeLimitCB.setEnabled(true);
            this.pokeLimitBtn.setEnabled(true);

            this.brokenMovesCB.setSelected(false);
            this.brokenMovesCB.setEnabled(true);

            this.loadQSButton.setEnabled(true);
            this.saveQSButton.setEnabled(true);

            this.pbsChangesUnchangedRB.setEnabled(true);
            this.pbsChangesUnchangedRB.setSelected(true);
            this.pbsChangesRandomRB.setEnabled(true);
            this.pbsChangesShuffleRB.setEnabled(true);
            this.pbsChangesRandomBSTRB.setEnabled(true);
            this.pbsChangesRandomBSTPERCRB.setEnabled(true);
            this.pbsChangesEqualizeRB.setEnabled(true);

            this.pbsStandardEXPCurvesCB.setEnabled(true);
            this.pbsStandardEXPCurvesCB.setSelected(false);

            this.pbsUpdateStatsCB.setEnabled(romHandler.generationOfPokemon() < 6);
            this.pbsUpdateStatsCB.setSelected(false);

            if (romHandler.abilitiesPerPokemon() > 0) {
                this.paUnchangedRB.setEnabled(true);
                this.paUnchangedRB.setSelected(true);
                this.paRandomizeRB.setEnabled(true);
                this.paWonderGuardCB.setEnabled(false);
                this.paFollowEvolutionsCB.setEnabled(false);
            } else {
                this.abilitiesPanel.setVisible(false);
            }

            this.spUnchangedRB.setEnabled(true);
            this.spUnchangedRB.setSelected(true);

            this.spCustomPoke3Chooser.setVisible(true);
            if (romHandler.canChangeStarters()) {
                this.spCustomRB.setEnabled(true);
                this.spRandomRB.setEnabled(true);
                this.spRandom2EvosRB.setEnabled(true);
                this.spRandom1EvosRB.setEnabled(true);
                this.spRandom0EvosRB.setEnabled(true);
                populateDropdowns();
            }

            this.spHeldItemsCB.setSelected(false);
            boolean hasStarterHeldItems = true;
            this.spHeldItemsCB.setEnabled(hasStarterHeldItems);
            this.spHeldItemsCB.setVisible(hasStarterHeldItems);
            this.spHeldItemsBanBadCB.setEnabled(false);
            this.spHeldItemsBanBadCB.setVisible(hasStarterHeldItems);

            this.mdRandomAccuracyCB.setEnabled(true);
            this.mdRandomPowerCB.setEnabled(true);
            this.mdRandomPPCB.setEnabled(true);
            this.mdRandomTypeCB.setEnabled(true);
            this.mdRandomCategoryCB.setEnabled(romHandler.hasPhysicalSpecialSplit());
            this.mdRandomCategoryCB.setVisible(romHandler.hasPhysicalSpecialSplit());

            this.pmsRandomTotalRB.setEnabled(true);
            this.pmsRandomTypeRB.setEnabled(true);
            this.pmsUnchangedRB.setEnabled(true);
            this.pmsUnchangedRB.setSelected(true);
            this.pmsMetronomeOnlyRB.setEnabled(true);

            this.pms4MovesCB.setVisible(romHandler.supportsFourStartingMoves());

            this.ptRandomFollowEvosRB.setEnabled(true);
            this.ptRandomTotalRB.setEnabled(true);
            this.ptUnchangedRB.setEnabled(true);
            this.ptUnchangedRB.setSelected(true);

            this.tpRandomRB.setEnabled(true);
            this.tpTypeThemedRB.setEnabled(true);
            this.tpTypeMatchRB.setEnabled(true);
            this.tpUnchangedRB.setEnabled(true);
            this.tpUnchangedRB.setSelected(true);
            this.tnRandomizeCB.setEnabled(romHandler.canChangeTrainerText());
            this.tcnRandomizeCB.setEnabled(romHandler.canChangeTrainerText());
            this.tnRandomizeCB.setVisible(romHandler.canChangeTrainerText());
            this.tcnRandomizeCB.setVisible(romHandler.canChangeTrainerText());

            this.tpNoEarlyShedinjaCB.setVisible(romHandler.generationOfPokemon() >= 3);
            this.tpNoEarlyShedinjaCB.setSelected(false);

            this.wpArea11RB.setEnabled(true);
            this.wpGlobalRB.setEnabled(true);
            this.wpRandomRB.setEnabled(true);
            this.wpUnchangedRB.setEnabled(true);
            this.wpUnchangedRB.setSelected(true);
            this.wpUseTimeCB.setEnabled(false);
            this.wpNoLegendariesCB.setEnabled(false);
            if (!romHandler.hasTimeBasedEncounters()) {
                this.wpUseTimeCB.setVisible(false);
            }
            this.wpCatchRateCB.setEnabled(true);

            this.wpHeldItemsCB.setSelected(false);
            this.wpHeldItemsCB.setEnabled(true);
            this.wpHeldItemsCB.setVisible(true);
            this.wpHeldItemsBanBadCB.setSelected(false);
            this.wpHeldItemsBanBadCB.setEnabled(false);
            this.wpHeldItemsBanBadCB.setVisible(true);

            this.stpUnchangedRB.setEnabled(true);
            if (this.romHandler.canChangeStaticPokemon()) {
                this.stpRandomL4LRB.setEnabled(true);
                this.stpRandomTotalRB.setEnabled(true);

            }
            
            this.wpCondenseEncounterSlotsCB.setEnabled(false);
            this.wpCondenseEncounterSlotsCB.setSelected(false);
            this.wpCondenseEncounterSlotsCB.setVisible(romHandler.canCondenseEncounterSlots());

            this.tmmRandomRB.setEnabled(true);
            this.tmmUnchangedRB.setEnabled(true);
            this.tmFullHMCompatCB.setEnabled(true);

            this.thcRandomTotalRB.setEnabled(true);
            this.thcRandomTypeRB.setEnabled(true);
            this.thcUnchangedRB.setEnabled(true);
            this.thcFullRB.setEnabled(true);

            if (this.romHandler.hasMoveTutors()) {
                this.mtmRandomRB.setEnabled(true);
                this.mtmUnchangedRB.setEnabled(true);

                this.mtcRandomTotalRB.setEnabled(true);
                this.mtcRandomTypeRB.setEnabled(true);
                this.mtcUnchangedRB.setEnabled(true);
                this.mtcFullRB.setEnabled(true);
            } else {
                this.mtCompatPanel.setVisible(false);
                this.mtMovesPanel.setVisible(false);
                this.mtNoExistLabel.setVisible(true);
            }

            this.wrUnchangedRB.setEnabled(true);
            this.wrRandomRB.setEnabled(true);
            this.wrKeepUselessDeadends.setEnabled(wrRandomRB.isSelected());
            this.wrRemoveGymOrderLogic.setEnabled(wrRandomRB.isSelected());

            this.tcUnchangedRB.setEnabled(true);
            this.tcRandomShuffleRowsRB.setEnabled(true);
            this.tcRandomShuffleRB.setEnabled(true);
            this.tcRandomTotalRB.setEnabled(true);

            this.igtUnchangedRB.setEnabled(true);
            this.igtBothRB.setEnabled(true);
            this.igtGivenOnlyRB.setEnabled(true);

            this.fiUnchangedRB.setEnabled(true);
            this.fiRandomRB.setEnabled(true);
            this.fiShuffleRB.setEnabled(true);

            this.fiBanBadCB.setEnabled(false);
            this.fiBanBadCB.setSelected(false);

            this.fiRandomizeGivenItemsCB.setEnabled(false);
            this.fiRandomizeGivenItemsCB.setSelected(false);

            this.fiRandomizePickupTablesCB.setEnabled(false);
            this.fiRandomizePickupTablesCB.setSelected(false);

            this.fiRandomizeBerryTreesCB.setEnabled(false);
            this.fiRandomizeBerryTreesCB.setSelected(false);

            this.fiRandomizeMartsCB.setEnabled(false);
            this.fiRandomizeMartsCB.setSelected(false);

            this.fiAllMartsHaveBallAndRepel.setEnabled(false);
            this.fiAllMartsHaveBallAndRepel.setSelected(false);

            this.fiRandomItemPrices.setEnabled(false);
            this.fiRandomItemPrices.setSelected(false);

            this.tpRandomFrontier.setEnabled(false);
            this.tpRandomFrontier.setSelected(false);

            this.tpFillBossTeams.setEnabled(false);
            this.tpFillBossTeams.setSelected(false);

            this.peUnchangedRB.setEnabled(true);
            this.peUnchangedRB.setSelected(true);
            this.peRandomRB.setEnabled(true);

            int mtsAvailable = this.romHandler.miscTweaksAvailable();
            List<JCheckBox> usableCheckboxes = getjCheckBoxes(mtsAvailable);

            if (!usableCheckboxes.isEmpty()) {
                this.mtNoneAvailableLabel.setVisible(false);
                miscTweaksPanel.setLayout(makeTweaksLayout(usableCheckboxes));
            } else {
                this.mtNoneAvailableLabel.setVisible(true);
                miscTweaksPanel.setLayout(noTweaksLayout);
            }

            this.gameMascotLabel.setIcon(makeMascotIcon());

        } catch (Exception ex) {
            attemptToLogException(ex, "RandomizerGUI.processFailed", "RandomizerGUI.processFailedNoLog");
            this.romHandler = null;
            this.initialFormState();
        }
    }

    private List<JCheckBox> getjCheckBoxes(int mtsAvailable) {
        int mtCount = MiscTweak.allTweaks.size();
        List<JCheckBox> usableCheckboxes = new ArrayList<JCheckBox>();

        for (int mti = 0; mti < mtCount; mti++) {
            MiscTweak mt = MiscTweak.allTweaks.get(mti);
            JCheckBox mtCB = tweakCheckboxes.get(mti);
            mtCB.setSelected(false);
            if ((mtsAvailable & mt.getValue()) != 0) {
                mtCB.setVisible(true);
                mtCB.setEnabled(true);
                usableCheckboxes.add(mtCB);
            } else {
                mtCB.setVisible(false);
                mtCB.setEnabled(false);
            }
        }
        return usableCheckboxes;
    }

    private ImageIcon makeMascotIcon() {

        int mascotAttempts = 0;
        ImageIcon result = null;

        // This has been failing on some weird ones like Pawmo (index 1014 / species 1306),
        // Assume there's something special about the palettes or sprites? maybe a gender sprite thing?
        // In any case I'm just going to try looping a few times to see it that fixes the issue

        do {

            try {
                BufferedImage handlerImg = romHandler.getMascotImage();

                if (handlerImg == null) {
                    return emptyIcon;
                }

                BufferedImage nImg = IconBackgroundUtils.createGradientCircle(128, 128, RandomSource.instance());

                int hW = handlerImg.getWidth();
                int hH = handlerImg.getHeight();
                nImg.getGraphics().drawImage(handlerImg, 64 - hW / 2, 64 - hH / 2, this);
                result = new ImageIcon(nImg);

            } catch (Exception ex) {
                /* Do nothing */
            }

            mascotAttempts++;

        } while (result == null && mascotAttempts < 5);


        return result == null ? emptyIcon : result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void populateDropdowns() {
        List<Pokemon> currentStarters = romHandler.getStarters();
        List<Pokemon> allPokes = romHandler.getPokemon();
        String[] pokeNames = new String[allPokes.size() - 1];
        for (int i = 1; i < allPokes.size(); i++) {
            Pokemon pokemon = allPokes.get(i);
            if (pokemon != null)
                pokeNames[i - 1] = pokemon.getName();
        }
        this.spCustomPoke1Chooser.setModel(new DefaultComboBoxModel(pokeNames));
        this.spCustomPoke1Chooser.setSelectedIndex(allPokes.indexOf(currentStarters.get(0)) - 1);
        this.spCustomPoke2Chooser.setModel(new DefaultComboBoxModel(pokeNames));
        this.spCustomPoke2Chooser.setSelectedIndex(allPokes.indexOf(currentStarters.get(1)) - 1);
        this.spCustomPoke3Chooser.setModel(new DefaultComboBoxModel(pokeNames));
        this.spCustomPoke3Chooser.setSelectedIndex(allPokes.indexOf(currentStarters.get(2)) - 1);
    }

    private void onTabChanged() {
        int selectedIndex = randomizerOptionsPane.getSelectedIndex();
        Runnable setScrollHeight;
        switch(selectedIndex) {
            case 14: // Warps
            case 13: // Misc
            case 12: // Items
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(2400);
                break;
            case 11: // Trades
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(2210);
                break;
            case 10: // Tutors
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(2000);
                break;
            case 9: // TM/HM
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(1775);
                break;
             case 8: // Statics
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(1650);
                break;
            case 7: // Wilds
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(1450);
                break;
            case 6: // Trainers
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(1170);
                break;
            case 5: // Learnsets
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(980);
                break;
            case 4: // Move Data
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(810);
                break;
            case 3: // Starters
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(580);
                break;
            case 2: // Evos
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(410);
                break;
            case 1: // Types
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(250);
                break;
            case 0: // Stats
            default:
                setScrollHeight = () -> optionsScrollPane.getVerticalScrollBar().setValue(0);


        }

        javax.swing.SwingUtilities.invokeLater(setScrollHeight);
    }

    private void enableOrDisableSubControls() {
        // This isn't for a new ROM being loaded (that's romLoaded)
        // This is just for when a radio button gets selected or state is loaded
        // and we need to enable/disable secondary controls
        // e.g. wild pokemon / trainer pokemon "modifier"
        // and the 3 starter pokemon dropdowns

        this.goUpdateMovesLegacyCheckBox.setEnabled(false);
        this.goUpdateMovesLegacyCheckBox.setSelected(false);

        this.pokeLimitBtn.setEnabled(this.pokeLimitCB.isSelected());

        if (this.spCustomRB.isSelected()) {
            this.spCustomPoke1Chooser.setEnabled(true);
            this.spCustomPoke2Chooser.setEnabled(true);
            this.spCustomPoke3Chooser.setEnabled(true);
        } else {
            this.spCustomPoke1Chooser.setEnabled(false);
            this.spCustomPoke2Chooser.setEnabled(false);
            this.spCustomPoke3Chooser.setEnabled(false);
        }

        if (this.pbsChangesUnchangedRB.isSelected()) {
            this.pbsFollowEvolutionsCB.setEnabled(false);
            this.pbsFollowEvolutionsCB.setSelected(false);
        	this.pbsUpdateStatsCB.setEnabled(true);
        	this.pbsDontRandomizeRatioCB.setEnabled(false);
        } else {
            this.pbsFollowEvolutionsCB.setEnabled(true);
        }
        
        if(this.pbsChangesRandomRB.isSelected()) {
        	this.pbsUpdateStatsCB.setEnabled(true);
        	this.pbsDontRandomizeRatioCB.setEnabled(false);
        }
        
        if(this.pbsChangesShuffleRB.isSelected()) {
        	this.pbsUpdateStatsCB.setEnabled(true);
        	this.pbsDontRandomizeRatioCB.setEnabled(false);
        }

        if(this.pbsChangesRandomBSTRB.isSelected()) {
        	this.pbsDontRandomizeRatioCB.setEnabled(true);
        	this.pbsEvosBuffStatsCB.setEnabled(true);
        	this.pbsUpdateStatsCB.setEnabled(false);
        	this.pbsUpdateStatsCB.setSelected(false);
        } else {
        	this.pbsDontRandomizeRatioCB.setEnabled(false);
        	this.pbsEvosBuffStatsCB.setEnabled(false);
        	this.pbsEvosBuffStatsCB.setSelected(false);
        }
        
        if(this.pbsChangesRandomBSTPERCRB.isSelected()) {
        	this.pbsDontRandomizeRatioCB.setEnabled(true);
        	this.pbsBaseStatRangeSlider.setEnabled(true);
        	this.pbsUpdateStatsCB.setEnabled(true);
        } else {
        	this.pbsBaseStatRangeSlider.setEnabled(false);
        }
        
        if(this.pbsChangesEqualizeRB.isSelected()) {
        	this.pbsDontRandomizeRatioCB.setEnabled(true);
        	this.pbsUpdateStatsCB.setEnabled(false);
        }
        
        if (this.spHeldItemsCB.isSelected() && this.spHeldItemsCB.isVisible() && this.spHeldItemsCB.isEnabled()) {
            this.spHeldItemsBanBadCB.setEnabled(true);
        } else {
            this.spHeldItemsBanBadCB.setEnabled(false);
            this.spHeldItemsBanBadCB.setSelected(false);
        }
        
        if(this.spRandom0EvosRB.isSelected() && this.spBanLegendaryStartersCB.isSelected()) {
        	this.spOnlyLegendaryStartersCB.setEnabled(false);
        	this.spOnlyLegendaryStartersCB.setSelected(false);
        } else if(this.spRandom0EvosRB.isSelected() && this.spOnlyLegendaryStartersCB.isSelected()){
        	this.spBanLegendaryStartersCB.setEnabled(false);
        	this.spBanLegendaryStartersCB.setSelected(false);
        } else if(this.spRandom0EvosRB.isSelected()) {
        	this.spBanLegendaryStartersCB.setEnabled(true);
        	this.spOnlyLegendaryStartersCB.setEnabled(true);
        } else {
        	this.spBanLegendaryStartersCB.setEnabled(false);
        	this.spBanLegendaryStartersCB.setSelected(false);
        	this.spOnlyLegendaryStartersCB.setEnabled(false);
        	this.spOnlyLegendaryStartersCB.setSelected(false);
        }
        
        if (this.paRandomizeRB.isSelected()) {
            this.paWonderGuardCB.setEnabled(true);
            this.paFollowEvolutionsCB.setEnabled(true);
            this.paBanTrappingCB.setEnabled(true);
            this.paBanNegativeCB.setEnabled(true);
        } else {
            this.paWonderGuardCB.setEnabled(false);
            this.paWonderGuardCB.setSelected(false);
            this.paFollowEvolutionsCB.setEnabled(false);
            this.paFollowEvolutionsCB.setSelected(false);
            this.paBanTrappingCB.setEnabled(false);
            this.paBanTrappingCB.setSelected(false);
            this.paBanNegativeCB.setEnabled(false);
            this.paBanNegativeCB.setSelected(false);
        }

        if (this.tpUnchangedRB.isSelected()) {
            this.tpPowerLevelsCB.setEnabled(false);
            this.tpNoLegendariesCB.setEnabled(false);
            this.tpNoEarlyShedinjaCB.setEnabled(false);
            this.tpNoEarlyShedinjaCB.setSelected(false);
            this.tpForceFullyEvolvedCB.setEnabled(false);
            this.tpForceFullyEvolvedCB.setSelected(false);
            this.tpLevelModifierCB.setEnabled(false);
            this.tpLevelModifierCB.setSelected(false);
            this.tpFillBossTeams.setEnabled(false);
            this.tpFillBossTeams.setSelected(false);
        } else {
            this.tpPowerLevelsCB.setEnabled(true);
            this.tpNoLegendariesCB.setEnabled(true);
            this.tpNoEarlyShedinjaCB.setEnabled(true);
            this.tpForceFullyEvolvedCB.setEnabled(true);
            this.tpLevelModifierCB.setEnabled(true);
            this.tpFillBossTeams.setEnabled(true);
        }

        if (this.tpForceFullyEvolvedCB.isSelected()) {
            this.tpForceFullyEvolvedSlider.setEnabled(true);
        } else {
            this.tpForceFullyEvolvedSlider.setEnabled(false);
            this.tpForceFullyEvolvedSlider.setValue(this.tpForceFullyEvolvedSlider.getMinimum());
        }

        if (this.tpLevelModifierCB.isSelected()) {
            this.tpLevelModifierSlider.setEnabled(true);
        } else {
            this.tpLevelModifierSlider.setEnabled(false);
            this.tpLevelModifierSlider.setValue(0);
        }

        if (!this.spUnchangedRB.isSelected() || !this.tpUnchangedRB.isSelected()) {
            this.tpRivalCarriesStarterCB.setEnabled(true);
        } else {
            this.tpRivalCarriesStarterCB.setEnabled(false);
            this.tpRivalCarriesStarterCB.setSelected(false);
        }

        this.tpTypeWeightingCB.setEnabled(this.tpTypeThemedRB.isSelected());

        if (this.wpArea11RB.isSelected() || this.wpRandomRB.isSelected()) {
            this.wpARNoneRB.setEnabled(true);
            this.wpARSimilarStrengthRB.setEnabled(true);
            this.wpARCatchEmAllRB.setEnabled(true);
            this.wpARTypeThemedRB.setEnabled(true);
        } else if (this.wpGlobalRB.isSelected()) {
            if (this.wpARCatchEmAllRB.isSelected() || this.wpARTypeThemedRB.isSelected()) {
                this.wpARNoneRB.setSelected(true);
            }
            this.wpARNoneRB.setEnabled(true);
            this.wpARSimilarStrengthRB.setEnabled(true);
            this.wpARCatchEmAllRB.setEnabled(false);
            this.wpARTypeThemedRB.setEnabled(false);
        } else {
            this.wpARNoneRB.setEnabled(false);
            this.wpARSimilarStrengthRB.setEnabled(false);
            this.wpARCatchEmAllRB.setEnabled(false);
            this.wpARTypeThemedRB.setEnabled(false);
            this.wpARNoneRB.setSelected(true);
        }

        if (this.wpUnchangedRB.isSelected()) {
            this.wpUseTimeCB.setEnabled(false);
            this.wpNoLegendariesCB.setEnabled(false);
        } else {
            this.wpUseTimeCB.setEnabled(true);
            this.wpNoLegendariesCB.setEnabled(true);
        }

        if (this.wpHeldItemsCB.isSelected() && this.wpHeldItemsCB.isVisible() && this.wpHeldItemsCB.isEnabled()) {
            this.wpHeldItemsBanBadCB.setEnabled(true);
        } else {
            this.wpHeldItemsBanBadCB.setEnabled(false);
            this.wpHeldItemsBanBadCB.setSelected(false);
        }

        if (this.wpCatchRateCB.isSelected()) {
            this.wpCatchRateSlider.setEnabled(true);
        } else {
            this.wpCatchRateSlider.setEnabled(false);
            this.wpCatchRateSlider.setValue(this.wpCatchRateSlider.getMinimum());
        }
        
        if(romHandler.canCondenseEncounterSlots()) {
            this.wpCondenseEncounterSlotsCB.setEnabled(this.wpRandomRB.isSelected());
            if(!this.wpRandomRB.isSelected()) {
                this.wpCondenseEncounterSlotsCB.setSelected(false);
            }
        }

        if (this.igtUnchangedRB.isSelected()) {
            this.igtRandomItemCB.setEnabled(false);
            this.igtRandomIVsCB.setEnabled(false);
            this.igtRandomNicknameCB.setEnabled(false);
            this.igtRandomOTCB.setEnabled(false);
        } else {
            this.igtRandomItemCB.setEnabled(true);
            this.igtRandomIVsCB.setEnabled(true);
            this.igtRandomNicknameCB.setEnabled(true);
            this.igtRandomOTCB.setEnabled(true);
        }

        if (this.pmsMetronomeOnlyRB.isSelected()) {
            this.tmmUnchangedRB.setEnabled(false);
            this.tmmRandomRB.setEnabled(false);
            this.tmmUnchangedRB.setSelected(true);

            this.mtmUnchangedRB.setEnabled(false);
            this.mtmRandomRB.setEnabled(false);
            this.mtmUnchangedRB.setSelected(true);

            this.tmKeepFieldMovesCB.setEnabled(false);
            this.tmKeepFieldMovesCB.setSelected(false);
            this.tmForceGoodDamagingCB.setEnabled(false);
            this.tmForceGoodDamagingCB.setSelected(false);

            this.mtKeepFieldMovesCB.setEnabled(false);
            this.mtKeepFieldMovesCB.setSelected(false);
            this.mtForceGoodDamagingCB.setEnabled(false);
            this.mtForceGoodDamagingCB.setSelected(false);
        } else {
            this.tmmUnchangedRB.setEnabled(true);
            this.tmmRandomRB.setEnabled(true);

            this.mtmUnchangedRB.setEnabled(true);
            this.mtmRandomRB.setEnabled(true);

            if (!(this.tmmUnchangedRB.isSelected())) {
                this.tmKeepFieldMovesCB.setEnabled(true);
                this.tmForceGoodDamagingCB.setEnabled(true);
            } else {
                this.tmKeepFieldMovesCB.setEnabled(false);
                this.tmKeepFieldMovesCB.setSelected(false);
                this.tmForceGoodDamagingCB.setEnabled(false);
                this.tmForceGoodDamagingCB.setSelected(false);
            }

            if (this.romHandler.hasMoveTutors() && !(this.mtmUnchangedRB.isSelected())) {
                this.mtKeepFieldMovesCB.setEnabled(true);
                this.mtForceGoodDamagingCB.setEnabled(true);
            } else {
                this.mtKeepFieldMovesCB.setEnabled(false);
                this.mtKeepFieldMovesCB.setSelected(false);
                this.mtForceGoodDamagingCB.setEnabled(false);
                this.mtForceGoodDamagingCB.setSelected(false);
            }
        }

        if (this.tmForceGoodDamagingCB.isSelected()) {
            this.tmForceGoodDamagingSlider.setEnabled(true);
        } else {
            this.tmForceGoodDamagingSlider.setEnabled(false);
            this.tmForceGoodDamagingSlider.setValue(this.tmForceGoodDamagingSlider.getMinimum());
        }

        if (this.mtForceGoodDamagingCB.isSelected()) {
            this.mtForceGoodDamagingSlider.setEnabled(true);
        } else {
            this.mtForceGoodDamagingSlider.setEnabled(false);
            this.mtForceGoodDamagingSlider.setValue(this.mtForceGoodDamagingSlider.getMinimum());
        }

        this.tmFullHMCompatCB.setEnabled(!this.thcFullRB.isSelected());

        if (this.pmsMetronomeOnlyRB.isSelected() || this.pmsUnchangedRB.isSelected()) {
            this.pms4MovesCB.setEnabled(false);
            this.pms4MovesCB.setSelected(false);
            this.pmsForceGoodDamagingCB.setEnabled(false);
            this.pmsForceGoodDamagingCB.setSelected(false);
            this.pmsReorderDamagingMovesCB.setEnabled(false);
            this.pmsReorderDamagingMovesCB.setSelected(false);
        } else {
            this.pms4MovesCB.setEnabled(true);
            this.pmsForceGoodDamagingCB.setEnabled(true);
            this.pmsReorderDamagingMovesCB.setEnabled(true);
        }

        if (this.pmsForceGoodDamagingCB.isSelected()) {
            this.pmsForceGoodDamagingSlider.setEnabled(true);
        } else {
            this.pmsForceGoodDamagingSlider.setEnabled(false);
            this.pmsForceGoodDamagingSlider.setValue(this.pmsForceGoodDamagingSlider.getMinimum());
        }

        if (this.fiRandomRB.isSelected() && this.fiRandomRB.isVisible() && this.fiRandomRB.isEnabled()) {
            this.fiBanBadCB.setEnabled(true);
            this.fiRandomizeGivenItemsCB.setEnabled(true);
            this.fiRandomizePickupTablesCB.setEnabled(true);
            this.fiRandomizeBerryTreesCB.setEnabled(true);
            this.fiRandomizeMartsCB.setEnabled(true);
        } else {
            this.fiBanBadCB.setEnabled(false);
            this.fiBanBadCB.setSelected(false);

            this.fiRandomizeGivenItemsCB.setEnabled(false);
            this.fiRandomizeGivenItemsCB.setSelected(false);

            this.fiRandomizePickupTablesCB.setEnabled(false);
            this.fiRandomizePickupTablesCB.setSelected(false);

            this.fiRandomizeBerryTreesCB.setEnabled(false);
            this.fiRandomizeBerryTreesCB.setSelected(false);

            this.fiRandomizeMartsCB.setEnabled(false);
            this.fiRandomizeMartsCB.setSelected(false);
        }

        this.fiRandomItemPrices.setEnabled(true);

        this.tpRandomFrontier.setEnabled(true);

        if (this.fiRandomizeMartsCB.isSelected() && this.fiRandomizeMartsCB.isEnabled()) {
            this.fiAllMartsHaveBallAndRepel.setEnabled(true);
        } else {
            this.fiAllMartsHaveBallAndRepel.setEnabled(false);
            this.fiAllMartsHaveBallAndRepel.setSelected(false);
        }

        this.peForceChangeCB.setEnabled(this.peRandomRB.isSelected());
        this.peThreeStagesCB.setEnabled(this.peRandomRB.isSelected());
        this.peSameTypeCB.setEnabled(this.peRandomRB.isSelected());
        this.peSimilarStrengthCB.setEnabled(this.peRandomRB.isSelected());

        this.wrKeepUselessDeadends.setEnabled(wrRandomRB.isSelected());
        this.wrRemoveGymOrderLogic.setEnabled(wrRandomRB.isSelected());
        if (!wrRandomRB.isSelected()) {
            wrKeepUselessDeadends.setSelected(false);
            wrRemoveGymOrderLogic.setSelected(false);
        }


        hasUnsavedSettings = true;
    }

    private void saveROM() {
        if (romHandler == null) {
            return; // none loaded
        }
        if (raceModeCB.isSelected() && tpUnchangedRB.isSelected() && wpUnchangedRB.isSelected()) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.raceModeRequirements"));
            return;
        }
        if (pokeLimitCB.isSelected()
                && (this.currentRestrictions == null || this.currentRestrictions.nothingSelected())) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.pokeLimitNotChosen"));
            return;
        }

        if (bulkSaveAmount > 0 && hasUnsavedSettings) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.bulkSaveSettings"));
            return;
        }

        // Get a seed
        if (romHandler.getSeedUsed() == null || romHandler.getSeedUsed() == 0) {
            long seed;

            if (!seedInput.getText().isEmpty()) {
                seed = RandomSource.seedFromString(seedInput.getText());
            } else {
                seed = RandomSource.pickSeed();
            }
            romHandler.setSeedUsed(seed);
        }

        romSaveChooser.setSelectedFile(new File(romHandler.getROMName()
                                                                    .replaceAll("\\(", "")
                                                                    .replaceAll("\\)", "")
                                                                    .replaceAll(" ", "_") +
                                                                    "_" + romHandler.getSeedUsed() + ".gba"));
        int returnVal = romSaveChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fh = romSaveChooser.getSelectedFile();

            bulkParent = fh.toPath().getParent();

            saveRom(fh);
        }
    }

    private void saveRom(File fh) {

        if (bulkSaveCounter < 0) {
            bulkSaveCounter = bulkSaveAmount;
        }
        bulkSaveCounter--;
        if (bulkSaveCounter == 0) {
            bulkSaveCounter = -1;
        }

        // Fix or add extension
        List<String> extensions = new ArrayList<>(Collections.singletonList("gba"));
        extensions.remove(this.romHandler.getDefaultExtension());
        fh = FileFunctions.fixFilename(fh, this.romHandler.getDefaultExtension(), extensions);

        // Apply the seed
        RandomSource.seed(romHandler.getSeedUsed());
        presetMode = false;

        try {
            CustomNamesSet cns = FileFunctions.getCustomNames();
            performRandomization(fh.getAbsolutePath(), romHandler.getSeedUsed(), cns);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.cantLoadCustomNames"));
        }
    }

    private void updateBulkSaveNumber() {
        Object selectedItem = bulkSaveSelection.getSelectedItem();

        if (!(selectedItem instanceof String)) {
            return;
        }

        switch ((String) selectedItem)
        {
            case BULK_OPTION_100:
                bulkSaveAmount = 100;
                break;
            case BULK_OPTION_50:
                bulkSaveAmount = 50;
                break;
            case BULK_OPTION_25:
                bulkSaveAmount = 25;
                break;
            case BULK_OPTION_10:
                bulkSaveAmount = 10;
                break;
            case BULK_OPTION_1:
            default:
                bulkSaveAmount = -1;
        }
    }

    private Settings getCurrentSettings() throws IOException {
        return createSettingsFromState(FileFunctions.getCustomNames());
    }

    public String getValidRequiredROMName(String config, CustomNamesSet customNames)
            throws UnsupportedEncodingException, InvalidSupplementFilesException {
        try {
            Utils.validatePresetSupplementFiles(config, customNames);
        } catch (InvalidSupplementFilesException e) {
            if (Objects.requireNonNull(e.getType()) == InvalidSupplementFilesException.Type.CUSTOM_NAMES) {
                JOptionPane.showMessageDialog(null, bundle.getString("RandomizerGUI.presetFailTrainerNames"));
                throw e;
            }
            throw e;
        }
        byte[] data = Utils.base64ToBytes(config);

        int nameLength = data[Settings.LENGTH_OF_SETTINGS_DATA] & 0xFF;
        if (data.length != Settings.LENGTH_OF_SETTINGS_DATA + 9 + nameLength) {
            return null; // not valid length
        }
        return new String(data, Settings.LENGTH_OF_SETTINGS_DATA + 1, nameLength, StandardCharsets.US_ASCII);
    }

    private void restoreStateFromSettings(Settings settings) {

        this.bulkSaveSelection.setEnabled(true);

        this.goRemoveTradeEvosCheckBox.setSelected(settings.isChangeImpossibleEvolutions());
        this.goUpdateMovesCheckBox.setSelected(settings.isUpdateMoves());
        this.goUpdateMovesLegacyCheckBox.setSelected(settings.isUpdateMovesLegacy());
        this.tnRandomizeCB.setSelected(settings.isRandomizeTrainerNames());
        this.tcnRandomizeCB.setSelected(settings.isRandomizeTrainerClassNames());

        this.pbsChangesEqualizeRB.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.EQUALIZE);
        this.pbsChangesRandomBSTPERCRB.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.RANDOMBSTPERC);
        this.pbsChangesRandomBSTRB.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.RANDOMBST);
        this.pbsChangesRandomRB.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.RANDOM);
        this.pbsChangesShuffleRB.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.SHUFFLE);
        this.pbsChangesUnchangedRB.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.UNCHANGED);
        this.pbsStandardEXPCurvesCB.setSelected(settings.isStandardizeEXPCurves());
        this.pbsFollowEvolutionsCB.setSelected(settings.isBaseStatsFollowEvolutions());
        this.pbsUpdateStatsCB.setSelected(settings.isUpdateBaseStats());
        this.pbsBaseStatRangeSlider.setValue(settings.getBaseStatRange());
        this.pbsDontRandomizeRatioCB.setSelected(settings.isDontRandomizeRatio());
        this.pbsEvosBuffStatsCB.setSelected(settings.isEvosBuffStats());

        this.paUnchangedRB.setSelected(settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED);
        this.paRandomizeRB.setSelected(settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE);
        this.paWonderGuardCB.setSelected(settings.isAllowWonderGuard());
        this.paFollowEvolutionsCB.setSelected(settings.isAbilitiesFollowEvolutions());
        this.paBanTrappingCB.setSelected(settings.isBanTrappingAbilities());
        this.paBanNegativeCB.setSelected(settings.isBanNegativeAbilities());

        this.ptRandomFollowEvosRB.setSelected(settings.getTypesMod() == Settings.TypesMod.RANDOM_FOLLOW_EVOLUTIONS);
        this.ptRandomTotalRB.setSelected(settings.getTypesMod() == Settings.TypesMod.COMPLETELY_RANDOM);
        this.ptUnchangedRB.setSelected(settings.getTypesMod() == Settings.TypesMod.UNCHANGED);
        this.raceModeCB.setSelected(settings.isRaceMode());
        this.brokenMovesCB.setSelected(settings.doBlockBrokenMoves());
        this.pokeLimitCB.setSelected(settings.isLimitPokemon());

        this.goCondenseEvosCheckBox.setSelected(settings.isMakeEvolutionsEasier());

        this.spCustomRB.setSelected(settings.getStartersMod() == Settings.StartersMod.CUSTOM);
        this.spRandomRB.setSelected(settings.getStartersMod() == Settings.StartersMod.COMPLETELY_RANDOM);
        this.spUnchangedRB.setSelected(settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
        this.spRandom2EvosRB.setSelected(settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_TWO_EVOLUTIONS);
        this.spRandom1EvosRB.setSelected(settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_ONE_EVOLUTION);
        this.spRandom0EvosRB.setSelected(settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_NO_EVOLUTIONS);
        this.spHeldItemsCB.setSelected(settings.isRandomizeStartersHeldItems());
        this.spHeldItemsBanBadCB.setSelected(settings.isBanBadRandomStarterHeldItems());
        this.spBanLegendaryStartersCB.setSelected(settings.isBanLegendaryStarters());
        this.spOnlyLegendaryStartersCB.setSelected(settings.isOnlyLegendaryStarters());
        
        int[] customStarters = settings.getCustomStarters();
        this.spCustomPoke1Chooser.setSelectedIndex(customStarters[0] - 1);
        this.spCustomPoke2Chooser.setSelectedIndex(customStarters[1] - 1);
        this.spCustomPoke3Chooser.setSelectedIndex(customStarters[2] - 1);

        this.peUnchangedRB.setSelected(settings.getEvolutionsMod() == Settings.EvolutionsMod.UNCHANGED);
        this.peRandomRB.setSelected(settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM);
        this.peSimilarStrengthCB.setSelected(settings.isEvosSimilarStrength());
        this.peSameTypeCB.setSelected(settings.isEvosSameTyping());
        this.peThreeStagesCB.setSelected(settings.isEvosMaxThreeStages());
        this.peForceChangeCB.setSelected(settings.isEvosForceChange());

        this.mdRandomAccuracyCB.setSelected(settings.isRandomizeMoveAccuracies());
        this.mdRandomCategoryCB.setSelected(settings.isRandomizeMoveCategory());
        this.mdRandomPowerCB.setSelected(settings.isRandomizeMovePowers());
        this.mdRandomPPCB.setSelected(settings.isRandomizeMovePPs());
        this.mdRandomTypeCB.setSelected(settings.isRandomizeMoveTypes());

        this.pmsRandomTotalRB.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.COMPLETELY_RANDOM);
        this.pmsRandomTypeRB.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.RANDOM_PREFER_SAME_TYPE);
        this.pmsUnchangedRB.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.UNCHANGED);
        this.pmsMetronomeOnlyRB.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY);
        this.pms4MovesCB.setSelected(settings.isStartWithFourMoves());
        this.pmsReorderDamagingMovesCB.setSelected(settings.isReorderDamagingMoves());
        this.pmsForceGoodDamagingCB.setSelected(settings.isMovesetsForceGoodDamaging());
        this.pmsForceGoodDamagingSlider.setValue(settings.getMovesetsGoodDamagingPercent());

        this.tpPowerLevelsCB.setSelected(settings.isTrainersUsePokemonOfSimilarStrength());
        this.tpRandomRB.setSelected(settings.getTrainersMod() == Settings.TrainersMod.RANDOM);
        this.tpRivalCarriesStarterCB.setSelected(settings.isRivalCarriesStarterThroughout());
        this.tpTypeThemedRB.setSelected(settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED);
        this.tpTypeMatchRB.setSelected(settings.getTrainersMod() == Settings.TrainersMod.TYPE_MATCHED);
        this.tpTypeWeightingCB.setSelected(settings.isTrainersMatchTypingDistribution());
        this.tpUnchangedRB.setSelected(settings.getTrainersMod() == Settings.TrainersMod.UNCHANGED);
        this.tpNoLegendariesCB.setSelected(settings.isTrainersBlockLegendaries());
        this.tpNoEarlyShedinjaCB.setSelected(settings.isTrainersBlockEarlyWonderGuard());
        this.tpForceFullyEvolvedCB.setSelected(settings.isTrainersForceFullyEvolved());
        this.tpForceFullyEvolvedSlider.setValue(settings.getTrainersForceFullyEvolvedLevel());
        this.tpLevelModifierCB.setSelected(settings.isTrainersLevelModified());
        this.tpLevelModifierSlider.setValue(settings.getTrainersLevelModifier());

        this.wpARCatchEmAllRB
                .setSelected(settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL);
        this.wpArea11RB.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.AREA_MAPPING);
        this.wpARNoneRB.setSelected(settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.NONE);
        this.wpARTypeThemedRB
                .setSelected(settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.TYPE_THEME_AREAS);
        this.wpGlobalRB.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.GLOBAL_MAPPING);
        this.wpRandomRB.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.RANDOM);
        this.wpUnchangedRB.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
        this.wpUseTimeCB.setSelected(settings.isUseTimeBasedEncounters());

        this.wpCatchRateCB.setSelected(settings.isUseMinimumCatchRate());
        this.wpCatchRateSlider.setValue(settings.getMinimumCatchRateLevel());
        this.wpNoLegendariesCB.setSelected(settings.isBlockWildLegendaries());
        this.wpARSimilarStrengthRB
                .setSelected(settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH);
        this.wpHeldItemsCB.setSelected(settings.isRandomizeWildPokemonHeldItems());
        this.wpHeldItemsBanBadCB.setSelected(settings.isBanBadRandomWildPokemonHeldItems());
        this.wpCondenseEncounterSlotsCB.setSelected(settings.isCondenseEncounterSlots());

        this.stpUnchangedRB.setSelected(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.UNCHANGED);
        this.stpRandomL4LRB.setSelected(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.RANDOM_MATCHING);
        this.stpRandomTotalRB
                .setSelected(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.COMPLETELY_RANDOM);

        this.thcRandomTotalRB
                .setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.FIFTY_PERCENT);
        this.thcRandomTypeRB
                .setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.SAME_TYPE);
        this.thcUnchangedRB
                .setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.UNCHANGED);
        this.tmmRandomRB.setSelected(settings.getTmsMod() == Settings.TMsMod.RANDOM);
        this.tmmUnchangedRB.setSelected(settings.getTmsMod() == Settings.TMsMod.UNCHANGED);
        this.tmKeepFieldMovesCB.setSelected(settings.isKeepFieldMoveTMs());
        this.thcFullRB.setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.FULL);
        this.tmFullHMCompatCB.setSelected(settings.isFullHMCompat());
        this.tmForceGoodDamagingCB.setSelected(settings.isTmsForceGoodDamaging());
        this.tmForceGoodDamagingSlider.setValue(settings.getTmsGoodDamagingPercent());

        this.mtcRandomTotalRB
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.FIFTY_PERCENT);
        this.mtcRandomTypeRB
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.SAME_TYPE);
        this.mtcUnchangedRB
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.UNCHANGED);
        this.mtmRandomRB.setSelected(settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.RANDOM);
        this.mtmUnchangedRB.setSelected(settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.UNCHANGED);
        this.mtKeepFieldMovesCB.setSelected(settings.isKeepFieldMoveTutors());
        this.mtcFullRB
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.FULL);
        this.mtForceGoodDamagingCB.setSelected(settings.isTutorsForceGoodDamaging());
        this.mtForceGoodDamagingSlider.setValue(settings.getTutorsGoodDamagingPercent());

        this.igtBothRB
                .setSelected(settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN_AND_REQUESTED);
        this.igtGivenOnlyRB.setSelected(settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN);
        this.igtRandomItemCB.setSelected(settings.isRandomizeInGameTradesItems());
        this.igtRandomIVsCB.setSelected(settings.isRandomizeInGameTradesIVs());
        this.igtRandomNicknameCB.setSelected(settings.isRandomizeInGameTradesNicknames());
        this.igtRandomOTCB.setSelected(settings.isRandomizeInGameTradesOTs());
        this.igtUnchangedRB.setSelected(settings.getInGameTradesMod() == Settings.InGameTradesMod.UNCHANGED);

        this.fiRandomRB.setSelected(settings.getFieldItemsMod() == Settings.FieldItemsMod.RANDOM);
        this.fiShuffleRB.setSelected(settings.getFieldItemsMod() == Settings.FieldItemsMod.SHUFFLE);
        this.fiUnchangedRB.setSelected(settings.getFieldItemsMod() == Settings.FieldItemsMod.UNCHANGED);
        this.fiBanBadCB.setSelected(settings.isBanBadRandomFieldItems());
        this.fiRandomizeGivenItemsCB.setSelected(settings.isRandomizeGivenItems());
        this.fiRandomizePickupTablesCB.setSelected(settings.isRandomizePickupTables());
        this.fiRandomizeBerryTreesCB.setSelected(settings.isRandomizeBerryTrees());
        this.fiRandomizeMartsCB.setSelected(settings.isRandomizeMarts());
        this.fiAllMartsHaveBallAndRepel.setSelected(settings.isAllMartsHaveBallAndRepel());
        this.fiRandomItemPrices.setSelected(settings.isRandomItemPrices());
        this.tpRandomFrontier.setSelected(settings.isRandomizeFrontier());
        this.tpFillBossTeams.setSelected(settings.isFillBossTeams());

        if (settings.getTypeChartMod() == Settings.TypeChartMod.UNCHANGED) {
            tcUnchangedRB.setSelected(true);
            tcRandomShuffleRowsRB.setSelected(false);
            tcRandomShuffleRB.setSelected(false);
            tcRandomTotalRB.setSelected(false);
        } else if (settings.getTypeChartMod() == Settings.TypeChartMod.SHUFFLE_ROW) {
            tcUnchangedRB.setSelected(false);
            tcRandomShuffleRowsRB.setSelected(true);
            tcRandomShuffleRB.setSelected(false);
            tcRandomTotalRB.setSelected(false);
        } else if (settings.getTypeChartMod() == Settings.TypeChartMod.SHUFFLE) {
            tcUnchangedRB.setSelected(false);
            tcRandomShuffleRowsRB.setSelected(false);
            tcRandomShuffleRB.setSelected(true);
            tcRandomTotalRB.setSelected(false);
        } else if (settings.getTypeChartMod() == Settings.TypeChartMod.RANDOM) {
            tcUnchangedRB.setSelected(false);
            tcRandomShuffleRowsRB.setSelected(false);
            tcRandomShuffleRB.setSelected(false);
            tcRandomTotalRB.setSelected(true);
        }

        if (settings.isRandomWarps()) {
            wrRandomRB.setSelected(true);
            wrUnchangedRB.setSelected(false);
        } else {
            wrRandomRB.setSelected(false);
            wrUnchangedRB.setSelected(true);
        }

        this.wrKeepUselessDeadends.setSelected(settings.isKeepUselessDeadends());
        this.wrRemoveGymOrderLogic.setSelected(settings.isRemoveGymOrderLogic());

        this.currentRestrictions = settings.getCurrentRestrictions();
        if (this.currentRestrictions != null) {
            this.currentRestrictions.limitToGen(this.romHandler.generationOfPokemon());
        }

        int mtsSelected = settings.getCurrentMiscTweaks();
        int mtCount = MiscTweak.allTweaks.size();

        for (int mti = 0; mti < mtCount; mti++) {
            MiscTweak mt = MiscTweak.allTweaks.get(mti);
            JCheckBox mtCB = tweakCheckboxes.get(mti);
            mtCB.setSelected((mtsSelected & mt.getValue()) != 0);
        }

        this.enableOrDisableSubControls();
        hasUnsavedSettings = false;
    }

    private Settings createSettingsFromState(CustomNamesSet customNames) {
        Settings settings = new Settings();
        settings.setRomName(this.romHandler.getROMName());
        settings.setChangeImpossibleEvolutions(goRemoveTradeEvosCheckBox.isSelected());
        settings.setUpdateMoves(goUpdateMovesCheckBox.isSelected());
        settings.setUpdateMovesLegacy(goUpdateMovesLegacyCheckBox.isSelected());
        settings.setRandomizeTrainerNames(tnRandomizeCB.isSelected());
        settings.setRandomizeTrainerClassNames(tcnRandomizeCB.isSelected());

        settings.setBaseStatisticsMod(pbsChangesUnchangedRB.isSelected(), pbsChangesShuffleRB.isSelected(),
                pbsChangesRandomRB.isSelected(), pbsChangesRandomBSTRB.isSelected(), 
                pbsChangesRandomBSTPERCRB.isSelected(), pbsChangesEqualizeRB.isSelected());
        settings.setStandardizeEXPCurves(pbsStandardEXPCurvesCB.isSelected());
        settings.setBaseStatsFollowEvolutions(pbsFollowEvolutionsCB.isSelected());
        settings.setUpdateBaseStats(pbsUpdateStatsCB.isSelected());
        settings.setBaseStatRange(pbsBaseStatRangeSlider.getValue());
        settings.setDontRandomizeRatio(pbsDontRandomizeRatioCB.isSelected());
        settings.setEvosBuffStats(pbsEvosBuffStatsCB.isSelected());

        settings.setAbilitiesMod(paUnchangedRB.isSelected(), paRandomizeRB.isSelected());
        settings.setAllowWonderGuard(paWonderGuardCB.isSelected());
        settings.setAbilitiesFollowEvolutions(paFollowEvolutionsCB.isSelected());
        settings.setBanTrappingAbilities(paBanTrappingCB.isSelected());
        settings.setBanNegativeAbilities(paBanNegativeCB.isSelected());

        settings.setTypesMod(ptUnchangedRB.isSelected(), ptRandomFollowEvosRB.isSelected(),
                ptRandomTotalRB.isSelected());
        settings.setRaceMode(raceModeCB.isSelected());
        settings.setBlockBrokenMoves(brokenMovesCB.isSelected());
        settings.setLimitPokemon(pokeLimitCB.isSelected());

        settings.setMakeEvolutionsEasier(goCondenseEvosCheckBox.isSelected());

        settings.setStartersMod(spUnchangedRB.isSelected(), spCustomRB.isSelected(), spRandomRB.isSelected(),
                spRandom2EvosRB.isSelected(), spRandom1EvosRB.isSelected(), spRandom0EvosRB.isSelected());
        settings.setRandomizeStartersHeldItems(spHeldItemsCB.isSelected());
        settings.setBanBadRandomStarterHeldItems(spHeldItemsBanBadCB.isSelected());
        settings.setBanLegendaryStarters(spBanLegendaryStartersCB.isSelected());
        settings.setOnlyLegendaryStarters(spOnlyLegendaryStartersCB.isSelected());

        int[] customStarters = new int[] { spCustomPoke1Chooser.getSelectedIndex() + 1,
                spCustomPoke2Chooser.getSelectedIndex() + 1, spCustomPoke3Chooser.getSelectedIndex() + 1 };
        settings.setCustomStarters(customStarters);

        settings.setEvolutionsMod(peUnchangedRB.isSelected(), peRandomRB.isSelected());
        settings.setEvosSimilarStrength(peSimilarStrengthCB.isSelected());
        settings.setEvosSameTyping(peSameTypeCB.isSelected());
        settings.setEvosMaxThreeStages(peThreeStagesCB.isSelected());
        settings.setEvosForceChange(peForceChangeCB.isSelected());

        settings.setRandomizeMoveAccuracies(mdRandomAccuracyCB.isSelected());
        settings.setRandomizeMoveCategory(mdRandomCategoryCB.isSelected());
        settings.setRandomizeMovePowers(mdRandomPowerCB.isSelected());
        settings.setRandomizeMovePPs(mdRandomPPCB.isSelected());
        settings.setRandomizeMoveTypes(mdRandomTypeCB.isSelected());

        settings.setMovesetsMod(pmsUnchangedRB.isSelected(), pmsRandomTypeRB.isSelected(),
                pmsRandomTotalRB.isSelected(), pmsMetronomeOnlyRB.isSelected());
        settings.setStartWithFourMoves(pms4MovesCB.isSelected());
        settings.setReorderDamagingMoves(pmsReorderDamagingMovesCB.isSelected());

        settings.setMovesetsForceGoodDamaging(pmsForceGoodDamagingCB.isSelected());
        settings.setMovesetsGoodDamagingPercent(pmsForceGoodDamagingSlider.getValue());

        settings.setTrainersMod(tpUnchangedRB.isSelected(), tpRandomRB.isSelected(), tpTypeThemedRB.isSelected(), tpTypeMatchRB.isSelected());
        settings.setTrainersUsePokemonOfSimilarStrength(tpPowerLevelsCB.isSelected());
        settings.setRivalCarriesStarterThroughout(tpRivalCarriesStarterCB.isSelected());
        settings.setTrainersMatchTypingDistribution(tpTypeWeightingCB.isSelected());
        settings.setTrainersBlockLegendaries(tpNoLegendariesCB.isSelected());
        settings.setTrainersBlockEarlyWonderGuard(tpNoEarlyShedinjaCB.isSelected());
        settings.setTrainersForceFullyEvolved(tpForceFullyEvolvedCB.isSelected());
        settings.setTrainersForceFullyEvolvedLevel(tpForceFullyEvolvedSlider.getValue());
        settings.setTrainersLevelModified(tpLevelModifierCB.isSelected());
        settings.setTrainersLevelModifier(tpLevelModifierSlider.getValue());

        settings.setWildPokemonMod(wpUnchangedRB.isSelected(), wpRandomRB.isSelected(), wpArea11RB.isSelected(),
                wpGlobalRB.isSelected());
        settings.setWildPokemonRestrictionMod(wpARNoneRB.isSelected(), wpARSimilarStrengthRB.isSelected(),
                wpARCatchEmAllRB.isSelected(), wpARTypeThemedRB.isSelected());
        settings.setUseTimeBasedEncounters(wpUseTimeCB.isSelected());
        settings.setUseMinimumCatchRate(wpCatchRateCB.isSelected());
        settings.setMinimumCatchRateLevel(wpCatchRateSlider.getValue());
        settings.setBlockWildLegendaries(wpNoLegendariesCB.isSelected());
        settings.setRandomizeWildPokemonHeldItems(wpHeldItemsCB.isSelected());
        settings.setBanBadRandomWildPokemonHeldItems(wpHeldItemsBanBadCB.isSelected());
        settings.setCondenseEncounterSlots(wpRandomRB.isSelected() && wpCondenseEncounterSlotsCB.isSelected());
        // temp: always use reasonable mode for CEA. may be made into an option later
        settings.setCatchEmAllReasonableSlotsOnly(wpARCatchEmAllRB.isSelected());

        settings.setStaticPokemonMod(stpUnchangedRB.isSelected(), stpRandomL4LRB.isSelected(),
                stpRandomTotalRB.isSelected());

        settings.setTmsMod(tmmUnchangedRB.isSelected(), tmmRandomRB.isSelected());

        settings.setTmsHmsCompatibilityMod(thcUnchangedRB.isSelected(), thcRandomTypeRB.isSelected(),
                thcRandomTotalRB.isSelected(), thcFullRB.isSelected());
        settings.setKeepFieldMoveTMs(tmKeepFieldMovesCB.isSelected());
        settings.setFullHMCompat(tmFullHMCompatCB.isSelected());
        settings.setTmsForceGoodDamaging(tmForceGoodDamagingCB.isSelected());
        settings.setTmsGoodDamagingPercent(tmForceGoodDamagingSlider.getValue());

        settings.setMoveTutorMovesMod(mtmUnchangedRB.isSelected(), mtmRandomRB.isSelected());
        settings.setMoveTutorsCompatibilityMod(mtcUnchangedRB.isSelected(), mtcRandomTypeRB.isSelected(),
                mtcRandomTotalRB.isSelected(), mtcFullRB.isSelected());
        settings.setKeepFieldMoveTutors(mtKeepFieldMovesCB.isSelected());
        settings.setTutorsForceGoodDamaging(mtForceGoodDamagingCB.isSelected());
        settings.setTutorsGoodDamagingPercent(mtForceGoodDamagingSlider.getValue());

        settings.setInGameTradesMod(igtUnchangedRB.isSelected(), igtGivenOnlyRB.isSelected(), igtBothRB.isSelected());
        settings.setRandomizeInGameTradesItems(igtRandomItemCB.isSelected());
        settings.setRandomizeInGameTradesIVs(igtRandomIVsCB.isSelected());
        settings.setRandomizeInGameTradesNicknames(igtRandomNicknameCB.isSelected());
        settings.setRandomizeInGameTradesOTs(igtRandomOTCB.isSelected());

        settings.setFieldItemsMod(fiUnchangedRB.isSelected(), fiShuffleRB.isSelected(), fiRandomRB.isSelected());
        settings.setBanBadRandomFieldItems(fiBanBadCB.isSelected());
        settings.setRandomizeGivenItems(fiRandomizeGivenItemsCB.isSelected());
        settings.setRandomizePickupTables(fiRandomizePickupTablesCB.isSelected());
        settings.setRandomizeBerryTrees(fiRandomizeBerryTreesCB.isSelected());
        settings.setRandomizeMarts(fiRandomizeMartsCB.isSelected());
        settings.setAllMartsHaveBallAndRepel(fiAllMartsHaveBallAndRepel.isSelected());
        settings.setRandomItemPrices(fiRandomItemPrices.isSelected());
        settings.setRandomizeFrontier(tpRandomFrontier.isSelected());
        settings.setFillBossTeams(tpFillBossTeams.isSelected());

        settings.setCurrentRestrictions(currentRestrictions);

        settings.setTypeChartMod(tcUnchangedRB.isSelected(), tcRandomShuffleRowsRB.isSelected(),
                tcRandomShuffleRB.isSelected(), tcRandomTotalRB.isSelected());

        if (wrUnchangedRB.isSelected()) {
            settings.setRandomWarps(false);
        } else if (wrRandomRB.isSelected()) {
            settings.setRandomWarps(true);
        }

        settings.setKeepUselessDeadends(wrKeepUselessDeadends.isSelected());
        settings.setRemoveOrderedGymLogic(wrRemoveGymOrderLogic.isSelected());

        int currentMiscTweaks = 0;
        int mtCount = MiscTweak.allTweaks.size();

        for (int mti = 0; mti < mtCount; mti++) {
            MiscTweak mt = MiscTweak.allTweaks.get(mti);
            JCheckBox mtCB = tweakCheckboxes.get(mti);
            if (mtCB.isSelected()) {
                currentMiscTweaks |= mt.getValue();
            }
        }

        settings.setCurrentMiscTweaks(currentMiscTweaks);

        settings.setCustomNames(customNames);

        return settings;
    }

    private void performRandomization(final String filename, final Long seed, CustomNamesSet customNames) {
        final Settings settings = createSettingsFromState(customNames);
        final boolean raceMode = settings.isRaceMode();
        // Setup verbose log
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream log;
        log = new PrintStream(baos, false, StandardCharsets.UTF_8);

        final PrintStream verboseLog = log;
        this.romHandler.setSeedUsed(seed);

        try {
            final AtomicInteger finishedCV = new AtomicInteger(0);
            opDialog = new OperationDialog(bundle.getString("RandomizerGUI.savingText"), this, true);
            Thread t = new Thread(() -> {
                SwingUtilities.invokeLater(() -> opDialog.setVisible(true));
                boolean succeededSave = false;
                try {
                    RandomizerGUI.this.romHandler.setLog(verboseLog);
                    finishedCV.set(new Randomizer(settings, RandomizerGUI.this.romHandler).randomize(filename,
                            verboseLog, seed, progress -> opDialog.setText(progress)));
                    succeededSave = true;
                } catch (RandomizationException ex) {
                    attemptToLogException(ex, "RandomizerGUI.saveFailedMessage",
                            "RandomizerGUI.saveFailedMessageNoLog", true);
                    verboseLog.close();
                } catch (Exception ex) {
                    attemptToLogException(ex, "RandomizerGUI.saveFailedIO", "RandomizerGUI.saveFailedIONoLog");
                    verboseLog.close();
                }
                if (succeededSave) {
                    SwingUtilities.invokeLater(() -> {
                        RandomizerGUI.this.opDialog.setVisible(false);
                        // Log?
                        verboseLog.close();
                        byte[] out = baos.toByteArray();

                        if (bulkSaveCounter <= 0) {
                            if (raceMode) {
                                JOptionPane.showMessageDialog(RandomizerGUI.this,
                                        String.format(bundle.getString("RandomizerGUI.raceModeCheckValuePopup"),
                                                finishedCV.get()));
                            } else {
                                int response = JOptionPane.showConfirmDialog(RandomizerGUI.this,
                                        bundle.getString("RandomizerGUI.saveLogDialog.text"),
                                        bundle.getString("RandomizerGUI.saveLogDialog.title"),
                                        JOptionPane.YES_NO_OPTION);
                                if (response == JOptionPane.YES_OPTION) {
                                    try {
                                        FileOutputStream fos = new FileOutputStream(filename + ".log");
                                        fos.write(0xEF);
                                        fos.write(0xBB);
                                        fos.write(0xBF);
                                        fos.write(out);
                                        fos.close();
                                    } catch (IOException e) {
                                        JOptionPane.showMessageDialog(RandomizerGUI.this,
                                                bundle.getString("RandomizerGUI.logSaveFailed"));
                                        return;
                                    }
                                    JOptionPane.showMessageDialog(RandomizerGUI.this,
                                            String.format(bundle.getString("RandomizerGUI.logSaved"), filename));
                                }
                            }
                            if (presetMode) {
                                JOptionPane.showMessageDialog(RandomizerGUI.this,
                                        bundle.getString("RandomizerGUI.randomizationDone"));
                                // Done
                            } else {
                                // Compile a config string
                                try {
                                    String configString = getCurrentSettings().toString();
                                    // Show the preset maker
                                    new PresetMakeDialog(RandomizerGUI.this, seed, configString);
                                } catch (IOException ex) {
                                    JOptionPane.showMessageDialog(RandomizerGUI.this,
                                            bundle.getString("RandomizerGUI.cantLoadCustomNames"));
                                }

                                // Done
                            }
                        }

                        String fileName = romHandler.loadedFilename();
                        RandomizerGUI.this.romHandler = null;
                        initialFormState();
                        try {
                            loadRomFile(new File(fileName));
                        } catch (Exception e) {
                            /* Stuff may have changed since the rom was loaded, if so just ignore it*/
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        RandomizerGUI.this.opDialog.setVisible(false);
                        String fileName = romHandler.loadedFilename();
                        RandomizerGUI.this.romHandler = null;
                        initialFormState();
                        try {
                            loadRomFile(new File(fileName));
                        } catch (Exception e) {
                            /* Stuff may have changed since the rom was loaded, if so just ignore it*/
                        }
                    });
                }
            });
            t.start();
        } catch (Exception ex) {
            attemptToLogException(ex, "RandomizerGUI.saveFailed", "RandomizerGUI.saveFailedNoLog");
            verboseLog.close();
        }
    }

    private void presetLoader() {
        PresetLoadDialog pld = new PresetLoadDialog(this);
        if (pld.isCompleted()) {
            // Apply it
            Long seed = pld.getSeed();
            String config = pld.getConfigString();
            this.romHandler = pld.getROM();
            this.romLoaded();
            Settings settings;
            try {
                settings = Settings.fromString(config);
                settings.tweakForRom(this.romHandler);
                this.restoreStateFromSettings(settings);
            } catch (UnsupportedEncodingException e) {
                // settings load failed
                this.romHandler = null;
                initialFormState();
            }
            romSaveChooser.setSelectedFile(null);
            int returnVal = romSaveChooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File fh = romSaveChooser.getSelectedFile();
                // Fix or add extension
                List<String> extensions = new ArrayList<>(Collections.singletonList("gba"));
                extensions.remove(this.romHandler.getDefaultExtension());
                fh = FileFunctions.fixFilename(fh, this.romHandler.getDefaultExtension(), extensions);

                // Apply the seed we were given
                RandomSource.seed(seed);
                presetMode = true;
                performRandomization(fh.getAbsolutePath(), seed, pld.getCustomNames());

            } else {
                this.romHandler = null;
                initialFormState();
            }
        }

    }

    private void attemptToLogException(Exception ex, String baseMessageKey, String noLogMessageKey) {
        attemptToLogException(ex, baseMessageKey, noLogMessageKey, false);
    }

    private void attemptToLogException(Exception ex, String baseMessageKey, String noLogMessageKey, boolean showMessage) {

        // Make sure the operation dialog doesn't show up over the error
        // dialog
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                RandomizerGUI.this.opDialog.setVisible(false);
            }
        });

        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        try {
            String errlog = "error_" + ft.format(now) + ".txt";
            PrintStream ps = new PrintStream(Files.newOutputStream(Paths.get(errlog)));
            ps.println("Randomizer Version: " + SysConstants.UPDATE_VERSION);
            PrintStream e1 = System.err;
            System.setErr(ps);
            if (this.romHandler != null) {
                try {
                    ps.println("ROM: " + romHandler.getROMName());
                    ps.println("Code: " + romHandler.getROMCode());
                    ps.println("Reported Support Level: " + romHandler.getSupportLevel());
                    ps.println();
                } catch (Exception ex2) {
                    // Do nothing, just don't fail
                }
            }
            ex.printStackTrace();
            System.setErr(e1);
            ps.close();
            if (showMessage) {
                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString(baseMessageKey), ex.getMessage(), errlog));
            } else {
                JOptionPane.showMessageDialog(this, String.format(bundle.getString(baseMessageKey), errlog));
            }
        } catch (Exception logex) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, String.format(bundle.getString(noLogMessageKey), ex.getMessage()));
            } else {
                JOptionPane.showMessageDialog(this, bundle.getString(noLogMessageKey));
            }
        }
    }

    // public response methods

    public void updateFound(int newVersion, String changelog) {
        new UpdateFoundDialog(this, newVersion, changelog);
    }

    public void noUpdateFound() {
        JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.noUpdates"));
    }

    // actions

    private void settingsButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_updateSettingsButtonActionPerformed
        if (autoUpdateEnabled) {
            toggleAutoUpdatesMenuItem.setText(bundle.getString("RandomizerGUI.disableAutoUpdate"));
        } else {
            toggleAutoUpdatesMenuItem.setText(bundle.getString("RandomizerGUI.enableAutoUpdate"));
        }

//        if (useScrollPaneMode) {
//            toggleScrollPaneMenuItem.setText(bundle.getString("RandomizerGUI.changeToTabbedPane"));
//        } else {
//            toggleScrollPaneMenuItem.setText(bundle.getString("RandomizerGUI.changeToScrollPane"));
//        }
        updateSettingsMenu.show(settingsButton, 0, settingsButton.getHeight());
    }// GEN-LAST:event_updateSettingsButtonActionPerformed

    @SuppressWarnings("unused")
    private void toggleAutoUpdatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_toggleAutoUpdatesMenuItemActionPerformed
        autoUpdateEnabled = !autoUpdateEnabled;
        if (autoUpdateEnabled) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.autoUpdateEnabled"));
        } else {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.autoUpdateDisabled"));
        }
        attemptWriteConfig();
    }// GEN-LAST:event_toggleAutoUpdatesMenuItemActionPerformed

    private void manualUpdateMenuItemActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_manualUpdateMenuItemActionPerformed
        new UpdateCheckThread(this, true).start();
    }// GEN-LAST:event_manualUpdateMenuItemActionPerformed

    private void toggleScrollPaneMenuItemActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_toggleScrollPaneMenuItemActionPerformed
        int response = JOptionPane.showConfirmDialog(RandomizerGUI.this,
                bundle.getString("RandomizerGUI.displayModeChangeDialog.text"),
                bundle.getString("RandomizerGUI.displayModeChangeDialog.title"), JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.displayModeChanged"));
            attemptWriteConfig();
            System.exit(0);
        }
    }// GEN-LAST:event_toggleScrollPaneMenuItemActionPerformed

    @SuppressWarnings("unused")
    private void customNamesEditorMenuItemActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_customNamesEditorMenuItemActionPerformed
        new CustomNamesEditorDialog(this);
    }// GEN-LAST:event_customNamesEditorMenuItemActionPerformed

    private void loadQSButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_loadQSButtonActionPerformed
        if (this.romHandler == null) {
            return;
        }
        qsOpenChooser.setSelectedFile(null);
        int returnVal = qsOpenChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fh = qsOpenChooser.getSelectedFile();
            loadSettingsFile(fh);
        }
    }// GEN-LAST:event_loadQSButtonActionPerformed

    private void loadSettingsFile(File fh) {
        try {
            Settings settings = Settings.read(Files.readString(fh.toPath(), StandardCharsets.UTF_8));
            settingsFilePath = fh.getAbsolutePath();

            // load settings
            initialFormState();
            romLoaded();
            Settings.TweakForROMFeedback feedback = settings.tweakForRom(this.romHandler);
            if (feedback.isChangedStarter() && settings.getStartersMod() == Settings.StartersMod.CUSTOM) {
                JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.starterUnavailable"));
            }
            this.restoreStateFromSettings(settings);

            if (settings.isUpdatedFromOldVersion()) {
                // show a warning dialog, but load it
                JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.settingsFileOlder"));
            }

            if (bulkSaveCounter <= 0) {

                Window[] windows = getWindows();

                for (Window window : windows)
                {
                    if (window instanceof JDialog)
                    {
                        window.dispose();
                    }
                }

                JOptionPane.showMessageDialog(this,
                        String.format(bundle.getString("RandomizerGUI.settingsLoaded"), fh.getName()));

            } else {

                // Get a seed
                if (romHandler.getSeedUsed() == null || romHandler.getSeedUsed() == 0) {

                    long seed;

                    if (!seedInput.getText().isEmpty()) {
                        seed = RandomSource.seedFromString(seedInput.getText());
                    } else {
                        seed = RandomSource.pickSeed();
                    }

                    romHandler.setSeedUsed(seed);
                }

                File saveFile = getBulkSaveFile();


                saveRom(saveFile);
            }
        } catch (UnsupportedOperationException ex) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.settingsFileNewer"));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.invalidSettingsFile"));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.settingsLoadFailed"));
        }
    }

    private File getBulkSaveFile() {
        File saveFile;
        if (bulkParent != null) {
            saveFile = new File(bulkParent.toFile(),romHandler.getROMName()
                                                                    .replaceAll("\\(", "")
                                                                    .replaceAll("\\)", "")
                                                                    .replaceAll(" ", "_") +
                                                                    "_" + bulkSaveCounter +
                                                                    "_" + romHandler.getSeedUsed() + ".gba");
        } else {
            saveFile = new File(romHandler.getROMName()
                                                    .replaceAll("\\(", "")
                                                    .replaceAll("\\)", "")
                                                    .replaceAll(" ", "_") +
                                                    "_" + bulkSaveCounter +
                                                    "_" + romHandler.getSeedUsed() + ".gba");
        }
        return saveFile;
    }

    private void saveQSButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_saveQSButtonActionPerformed
        if (this.romHandler == null) {
            return;
        }
        qsSaveChooser.setSelectedFile( new File(romHandler.getROMCode().toLowerCase() + "." + CONFIG_FILE_EXTENSION));
        int returnVal = qsSaveChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fh = qsSaveChooser.getSelectedFile();
            // Fix or add extension
            fh = FileFunctions.fixFilename(fh, CONFIG_FILE_EXTENSION);
            // Save now?
            try {
                Files.writeString(fh.toPath(), getCurrentSettings().toString(), StandardCharsets.UTF_8);
                settingsFilePath = fh.getAbsolutePath();
                hasUnsavedSettings = false;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, bundle.getString("RandomizerGUI.settingsSaveFailed"));
            }
        }
    }// GEN-LAST:event_saveQSButtonActionPerformed

    @SuppressWarnings("unused")
    private void pokeLimitBtnActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pokeLimitBtnActionPerformed
        GenerationLimitDialog gld = new GenerationLimitDialog(this, this.currentRestrictions,
                this.romHandler.generationOfPokemon());
        if (gld.pressedOK()) {
            this.currentRestrictions = gld.getChoice();
        }
    }// GEN-LAST:event_pokeLimitBtnActionPerformed

    @SuppressWarnings("unused")
    private void goUpdateMovesCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_goUpdateMovesCheckBoxActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_goUpdateMovesCheckBoxActionPerformed

    @SuppressWarnings("unused")
    private void pokeLimitCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pokeLimitCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pokeLimitCBActionPerformed

    @SuppressWarnings("unused")
    private void pmsMetronomeOnlyRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pmsMetronomeOnlyRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pmsMetronomeOnlyRBActionPerformed

    @SuppressWarnings("unused")
    private void igtUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_igtUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_igtUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void igtGivenOnlyRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_igtGivenOnlyRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_igtGivenOnlyRBActionPerformed

    @SuppressWarnings("unused")
    private void igtBothRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_igtBothRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_igtBothRBActionPerformed

    @SuppressWarnings("unused")
    private void wpARNoneRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpARNoneRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpARNoneRBActionPerformed

    @SuppressWarnings("unused")
    private void wpARSimilarStrengthRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpARSimilarStrengthRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpARSimilarStrengthRBActionPerformed

    @SuppressWarnings("unused")
    private void wpARCatchEmAllRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpARCatchEmAllRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpARCatchEmAllRBActionPerformed

    @SuppressWarnings("unused")
    private void wpARTypeThemedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpARTypeThemedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpARTypeThemedRBActionPerformed

    @SuppressWarnings("unused")
    private void pmsUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pmsUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pmsUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void pmsRandomTypeRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pmsRandomTypeRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pmsRandomTypeRBActionPerformed

    @SuppressWarnings("unused")
    private void pmsRandomTotalRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pmsRandomTotalRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pmsRandomTotalRBActionPerformed

    @SuppressWarnings("unused")
    private void mtmUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mtmUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_mtmUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void paUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_paUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_paUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void paRandomizeRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_paRandomizeRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_paRandomizeRBActionPerformed

    @SuppressWarnings("unused")
    private void openROMButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_openROMButtonActionPerformed
        loadROM();
    }// GEN-LAST:event_openROMButtonActionPerformed

    @SuppressWarnings("unused")
    private void saveROMButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_saveROMButtonActionPerformed
        saveROM();
    }// GEN-LAST:event_saveROMButtonActionPerformed

    @SuppressWarnings("unused")
    private void bulkSaveSelectionActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_saveROMButtonActionPerformed
        updateBulkSaveNumber();
    }// GEN-LAST:event_saveROMButtonActionPerformed

    @SuppressWarnings("unused")
    private void usePresetsButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_usePresetsButtonActionPerformed
        presetLoader();
    }// GEN-LAST:event_usePresetsButtonActionPerformed

    @SuppressWarnings("unused")
    private void wpUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void tpUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tpUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tpUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void tpRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tpRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tpRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void tpTypeThemedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tpTypeThemedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tpTypeThemedRBActionPerformed

    @SuppressWarnings("unused")
    private void tpTypeMatchRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tpTypeMatchRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tpTypeMatchRBActionPerformed

    @SuppressWarnings("unused")
    private void spUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void spCustomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spCustomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spCustomRBActionPerformed

    @SuppressWarnings("unused")
    private void spRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void wpRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void wpArea11RBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpArea11RBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpArea11RBActionPerformed

    @SuppressWarnings("unused")
    private void wpGlobalRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpGlobalRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpGlobalRBActionPerformed

    @SuppressWarnings("unused")
    private void tmmUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tmmUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tmmUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void tmmRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tmmRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tmmRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void mtmRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mtmRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_mtmRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void thcUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_thcUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_thcUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void thcRandomTypeRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_thcRandomTypeRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_thcRandomTypeRBActionPerformed

    @SuppressWarnings("unused")
    private void thcRandomTotalRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_thcRandomTotalRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_thcRandomTotalRBActionPerformed

    @SuppressWarnings("unused")
    private void mtcUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mtcUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_mtcUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void mtcRandomTypeRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mtcRandomTypeRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_mtcRandomTypeRBActionPerformed

    @SuppressWarnings("unused")
    private void mtcRandomTotalRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mtcRandomTotalRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_mtcRandomTotalRBActionPerformed

    @SuppressWarnings("unused")
    private void thcFullRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_thcFullRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_thcFullRBActionPerformed

    @SuppressWarnings("unused")
    private void mtcFullRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mtcFullRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_mtcFullRBActionPerformed

    @SuppressWarnings("unused")
    private void spHeldItemsCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spHeldItemsCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spHeldItemsCBActionPerformed

    @SuppressWarnings("unused")
    private void wpHeldItemsCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpHeldItemsCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpHeldItemsCBActionPerformed

    @SuppressWarnings("unused")
    private void fiUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_fiUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_fiUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void fiShuffleRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_fiShuffleRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_fiShuffleRBActionPerformed

    @SuppressWarnings("unused")
    private void fiRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_fiRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_fiRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void fiRandomMartCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_fiRandomMartCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_fiRandomMartCBActionPerformed

    @SuppressWarnings("unused")
    private void spRandom2EvosRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spRandom2EvosRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spRandom2EvosRBActionPerformed

    @SuppressWarnings("unused")
    private void spRandom1EvosRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spRandom1EvosRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spRandom1EvosRBActionPerformed

    @SuppressWarnings("unused")
    private void spRandom0EvosRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spRandom0EvosRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spRandom0EvosRBActionPerformed

    @SuppressWarnings("unused")
    private void spBanLegendaryStartersCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spBanLegendaryStartersCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spBanLegendaryStartersCBActionPerformed

    @SuppressWarnings("unused")
    private void spOnlyLegendaryStartersCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_spOnlyLegendaryStartersCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_spOnlyLegendaryStartersCBActionPerformed

    @SuppressWarnings("unused")
    private void goCondenseEvosCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_goCondenseEvosCheckBoxActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_goCondenseEvosCheckBoxActionPerformed

    @SuppressWarnings("unused")
    private void websiteLinkLabelMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_websiteLinkLabelMouseClicked
        Desktop desktop = java.awt.Desktop.getDesktop();
        try {
            desktop.browse(new URI(SysConstants.WEBSITE_URL));
        } catch (IOException | URISyntaxException ignored) {
            // Do Nothing
        }
    }// GEN-LAST:event_websiteLinkLabelMouseClicked

    @SuppressWarnings("unused")
    private void peUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_peUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_peUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void peRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_peRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_peRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void pbsChangesUnchangedRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pbsChangesUnchangedRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pbsChangesUnchangedRBActionPerformed

    @SuppressWarnings("unused")
    private void pbsChangesShuffleRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pbsChangesShuffleRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pbsChangesShuffleRBActionPerformed

    @SuppressWarnings("unused")
    private void pbsChangesRandomRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pbsChangesRandomRBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pbsChangesRandomRBActionPerformed

    @SuppressWarnings("unused")
    private void pbsChangesRandomBSTRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pbsChangesRandomBSTRBActionPerformed
    	this.enableOrDisableSubControls();
    }// GEN-LAST:event_pbsChangesRandomBSTRBActionPerformed

    @SuppressWarnings("unused")
    private void pbsChangesRandomBSTPERCRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pbsChangesRandomBSTPERCRBActionPerformed
    	this.enableOrDisableSubControls();
    }// GEN-LAST:event_pbsChangesRandomBSTPERCRBActionPerformed

    @SuppressWarnings("unused")
    private void pbsChangesEqualizeRBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pbsChangesEqualizeRBActionPerformed
    	this.enableOrDisableSubControls();
    }// GEN-LAST:event_pbsChangesEqualizeRBActionPerformed

    @SuppressWarnings("unused")
    private void tpForceFullyEvolvedCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tpForceFullyEvolvedCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tpForceFullyEvolvedCBActionPerformed

    @SuppressWarnings("unused")
    private void tpLevelModifierCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tpForceFullyEvolvedCBActionPerformed
        this.enableOrDisableSubControls();
    }

    @SuppressWarnings("unused")
    private void wpCatchRateCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wpCatchRateCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wpCatchRateCBActionPerformed

    @SuppressWarnings("unused")
    private void pmsForceGoodDamagingCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_pmsForceGoodDamagingCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_pmsForceGoodDamagingCBActionPerformed

    @SuppressWarnings("unused")
    private void tmForceGoodDamagingCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tmForceGoodDamagingCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_tmForceGoodDamagingCBActionPerformed

    @SuppressWarnings("unused")
    private void mtForceGoodDamagingCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mtForceGoodDamagingCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_mtForceGoodDamagingCBActionPerformed

    private void tabChangeActionPerformed(ChangeEvent evt) {// GEN-FIRST:event_mtForceGoodDamagingCBActionPerformed
        this.onTabChanged();
    }// GEN-LAST:event_mtForceGoodDamagingCBActionPerformed

    @SuppressWarnings("unused")
    private void wrCBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_wrCBActionPerformedCBActionPerformed
        this.enableOrDisableSubControls();
    }// GEN-LAST:event_wrCBActionPerformedCBActionPerformed

    /* @formatter:off */
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("Bundle", Locale.ROOT);// NOI18N

        pokeStatChangesButtonGroup = new javax.swing.ButtonGroup();
        pokeTypesButtonGroup = new javax.swing.ButtonGroup();
        typeChartButtonGroup = new javax.swing.ButtonGroup();
        warpRandomizerButtonGroup = new javax.swing.ButtonGroup();
        pokeMovesetsButtonGroup = new javax.swing.ButtonGroup();
        trainerPokesButtonGroup = new javax.swing.ButtonGroup();
        wildPokesButtonGroup = new javax.swing.ButtonGroup();
        wildPokesARuleButtonGroup = new javax.swing.ButtonGroup();
        starterPokemonButtonGroup = new javax.swing.ButtonGroup();
        romOpenChooser = new javax.swing.JFileChooser();
        romSaveChooser = new JFileChooser() {

            private static final long serialVersionUID = 3244234325234511L;
            public void approveSelection() {
                File fh = getSelectedFile();
                // Fix or add extension
                List<String> extensions = new ArrayList<>(Arrays.asList("sgb", "gbc", "gba", "nds"));
                extensions.remove(RandomizerGUI.this.romHandler.getDefaultExtension());
                fh = FileFunctions.fixFilename(fh, RandomizerGUI.this.romHandler.getDefaultExtension(), extensions);
                if (fh.exists() && getDialogType() == SAVE_DIALOG) {
                    int result = JOptionPane.showConfirmDialog(this,
                        "The file exists, overwrite?", "Existing file",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result) {
                        case JOptionPane.YES_OPTION:
                        super.approveSelection();
                        return;
                        case JOptionPane.CANCEL_OPTION:
                        cancelSelection();
                        return;
                        default:
                        return;
                    }
                }
                super.approveSelection();
            }
        };
        qsOpenChooser = new javax.swing.JFileChooser();
        qsSaveChooser = new javax.swing.JFileChooser();
        staticPokemonButtonGroup = new javax.swing.ButtonGroup();
        tmMovesButtonGroup = new javax.swing.ButtonGroup();
        tmHmCompatibilityButtonGroup = new javax.swing.ButtonGroup();
        pokeAbilitiesButtonGroup = new javax.swing.ButtonGroup();
        mtMovesButtonGroup = new javax.swing.ButtonGroup();
        mtCompatibilityButtonGroup = new javax.swing.ButtonGroup();
        ingameTradesButtonGroup = new javax.swing.ButtonGroup();
        fieldItemsButtonGroup = new javax.swing.ButtonGroup();
        updateSettingsMenu = new javax.swing.JPopupMenu();
        toggleAutoUpdatesMenuItem = new javax.swing.JMenuItem();
        manualUpdateMenuItem = new javax.swing.JMenuItem();
        toggleScrollPaneMenuItem = new javax.swing.JMenuItem();
        customNamesEditorMenuItem = new javax.swing.JMenuItem();
        pokeEvolutionsButtonGroup = new javax.swing.ButtonGroup();
        generalOptionsPanel = new javax.swing.JPanel();
        pokeLimitCB = new javax.swing.JCheckBox();
        pokeLimitBtn = new javax.swing.JButton();
        raceModeCB = new javax.swing.JCheckBox();
        brokenMovesCB = new javax.swing.JCheckBox();
        romInfoPanel = new javax.swing.JPanel();
        riRomNameLabel = new javax.swing.JLabel();
        riRomCodeLabel = new javax.swing.JLabel();
        riRomSupportLabel = new javax.swing.JLabel();
        openROMButton = new javax.swing.JButton();
        saveROMButton = new javax.swing.JButton();
        seedInput = new PrefixTextField(bundle.getString("RandomizerGUI.seedPrefix"));
        bulkSaveSelection = new javax.swing.JComboBox<>(new String[] {BULK_OPTION_1, BULK_OPTION_10, BULK_OPTION_25, BULK_OPTION_50, BULK_OPTION_100});
        usePresetsButton = new javax.swing.JButton();
        loadQSButton = new javax.swing.JButton();
        saveQSButton = new javax.swing.JButton();
        settingsButton = new javax.swing.JButton();
        randomizerOptionsPane = new javax.swing.JTabbedPane();
        pokeTraitsPanel = new javax.swing.JPanel();
        pokemonTypesPanel = new javax.swing.JPanel();
        typeChartPanel = new javax.swing.JPanel();
        ptUnchangedRB = new javax.swing.JRadioButton();
        ptRandomFollowEvosRB = new javax.swing.JRadioButton();
        ptRandomTotalRB = new javax.swing.JRadioButton();
        tcUnchangedRB = new javax.swing.JRadioButton();
        tcRandomShuffleRowsRB = new javax.swing.JRadioButton();
        tcRandomShuffleRB = new javax.swing.JRadioButton();
        tcRandomTotalRB = new javax.swing.JRadioButton();
        baseStatsPanel = new javax.swing.JPanel();
        pbsChangesUnchangedRB = new javax.swing.JRadioButton();
        pbsChangesShuffleRB = new javax.swing.JRadioButton();
        pbsChangesRandomRB = new javax.swing.JRadioButton();
        pbsStandardEXPCurvesCB = new javax.swing.JCheckBox();
        pbsFollowEvolutionsCB = new javax.swing.JCheckBox();
        pbsUpdateStatsCB = new javax.swing.JCheckBox();
        pbsChangesRandomBSTRB = new javax.swing.JRadioButton();
        pbsEvosBuffStatsCB = new javax.swing.JCheckBox();
        pbsBaseStatRangeSlider = new javax.swing.JSlider();
        pbsChangesRandomBSTPERCRB = new javax.swing.JRadioButton();
        pbsChangesEqualizeRB = new javax.swing.JRadioButton();
        pbsDontRandomizeRatioCB = new javax.swing.JCheckBox();
        abilitiesPanel = new javax.swing.JPanel();
        paUnchangedRB = new javax.swing.JRadioButton();
        paRandomizeRB = new javax.swing.JRadioButton();
        paWonderGuardCB = new javax.swing.JCheckBox();
        paFollowEvolutionsCB = new javax.swing.JCheckBox();
        paBansLabel = new javax.swing.JLabel();
        paBanTrappingCB = new javax.swing.JCheckBox();
        paBanNegativeCB = new javax.swing.JCheckBox();
        pokemonEvolutionsPanel = new javax.swing.JPanel();
        peUnchangedRB = new javax.swing.JRadioButton();
        peRandomRB = new javax.swing.JRadioButton();
        peSimilarStrengthCB = new javax.swing.JCheckBox();
        peSameTypeCB = new javax.swing.JCheckBox();
        peThreeStagesCB = new javax.swing.JCheckBox();
        peForceChangeCB = new javax.swing.JCheckBox();
        goRemoveTradeEvosCheckBox = new javax.swing.JCheckBox();
        goCondenseEvosCheckBox = new javax.swing.JCheckBox();
        startersInnerPanel = new javax.swing.JPanel();
        starterPokemonPanel = new javax.swing.JPanel();
        spUnchangedRB = new javax.swing.JRadioButton();
        spCustomRB = new javax.swing.JRadioButton();
        spCustomPoke1Chooser = new javax.swing.JComboBox<>();
        spCustomPoke2Chooser = new javax.swing.JComboBox<>();
        spCustomPoke3Chooser = new javax.swing.JComboBox<>();
        spRandomRB = new javax.swing.JRadioButton();
        spRandom2EvosRB = new javax.swing.JRadioButton();
        spHeldItemsCB = new javax.swing.JCheckBox();
        spHeldItemsBanBadCB = new javax.swing.JCheckBox();
        spRandom1EvosRB = new javax.swing.JRadioButton();
        spRandom0EvosRB = new javax.swing.JRadioButton();
        spBanLegendaryStartersCB = new javax.swing.JCheckBox();
        spOnlyLegendaryStartersCB = new javax.swing.JCheckBox();
        staticPokemonPanel = new javax.swing.JPanel();
        stpUnchangedRB = new javax.swing.JRadioButton();
        stpRandomL4LRB = new javax.swing.JRadioButton();
        stpRandomTotalRB = new javax.swing.JRadioButton();
        inGameTradesPanel = new javax.swing.JPanel();
        igtUnchangedRB = new javax.swing.JRadioButton();
        igtGivenOnlyRB = new javax.swing.JRadioButton();
        igtBothRB = new javax.swing.JRadioButton();
        igtRandomNicknameCB = new javax.swing.JCheckBox();
        igtRandomOTCB = new javax.swing.JCheckBox();
        igtRandomIVsCB = new javax.swing.JCheckBox();
        igtRandomItemCB = new javax.swing.JCheckBox();
        movesAndSetsPanel = new javax.swing.JPanel();
        pokemonMovesetsPanel = new javax.swing.JPanel();
        pmsUnchangedRB = new javax.swing.JRadioButton();
        pmsRandomTypeRB = new javax.swing.JRadioButton();
        pmsRandomTotalRB = new javax.swing.JRadioButton();
        pmsMetronomeOnlyRB = new javax.swing.JRadioButton();
        pms4MovesCB = new javax.swing.JCheckBox();
        pmsReorderDamagingMovesCB = new javax.swing.JCheckBox();
        pmsForceGoodDamagingCB = new javax.swing.JCheckBox();
        pmsForceGoodDamagingSlider = new javax.swing.JSlider();
        moveDataPanel = new javax.swing.JPanel();
        mdRandomPowerCB = new javax.swing.JCheckBox();
        mdRandomAccuracyCB = new javax.swing.JCheckBox();
        mdRandomPPCB = new javax.swing.JCheckBox();
        mdRandomTypeCB = new javax.swing.JCheckBox();
        mdRandomCategoryCB = new javax.swing.JCheckBox();
        goUpdateMovesCheckBox = new javax.swing.JCheckBox();
        goUpdateMovesLegacyCheckBox = new javax.swing.JCheckBox();
        trainersInnerPanel = new javax.swing.JPanel();
        trainersPokemonPanel = new javax.swing.JPanel();
        tpUnchangedRB = new javax.swing.JRadioButton();
        tpRandomRB = new javax.swing.JRadioButton();
        tpTypeThemedRB = new javax.swing.JRadioButton();
        tpTypeMatchRB = new javax.swing.JRadioButton();
        tpPowerLevelsCB = new javax.swing.JCheckBox();
        tpTypeWeightingCB = new javax.swing.JCheckBox();
        tpRivalCarriesStarterCB = new javax.swing.JCheckBox();
        tpNoLegendariesCB = new javax.swing.JCheckBox();
        tnRandomizeCB = new javax.swing.JCheckBox();
        tcnRandomizeCB = new javax.swing.JCheckBox();
        tpNoEarlyShedinjaCB = new javax.swing.JCheckBox();
        tpForceFullyEvolvedCB = new javax.swing.JCheckBox();
        tpForceFullyEvolvedSlider = new javax.swing.JSlider();
        tpLevelModifierCB = new javax.swing.JCheckBox();
        tpLevelModifierSlider = new javax.swing.JSlider();
        wildsInnerPanel = new javax.swing.JPanel();
        wildPokemonPanel = new javax.swing.JPanel();
        wpUnchangedRB = new javax.swing.JRadioButton();
        wpRandomRB = new javax.swing.JRadioButton();
        wpArea11RB = new javax.swing.JRadioButton();
        wpGlobalRB = new javax.swing.JRadioButton();
        wildPokemonARulePanel = new javax.swing.JPanel();
        wpARNoneRB = new javax.swing.JRadioButton();
        wpARCatchEmAllRB = new javax.swing.JRadioButton();
        wpARTypeThemedRB = new javax.swing.JRadioButton();
        wpARSimilarStrengthRB = new javax.swing.JRadioButton();
        wpUseTimeCB = new javax.swing.JCheckBox();
        wpNoLegendariesCB = new javax.swing.JCheckBox();
        wpCatchRateCB = new javax.swing.JCheckBox();
        wpHeldItemsCB = new javax.swing.JCheckBox();
        wpHeldItemsBanBadCB = new javax.swing.JCheckBox();
        wpCatchRateSlider = new javax.swing.JSlider();
        wpCondenseEncounterSlotsCB = new javax.swing.JCheckBox();
        tmHmTutorPanel = new javax.swing.JPanel();
        tmhmsPanel = new javax.swing.JPanel();
        tmMovesPanel = new javax.swing.JPanel();
        tmmUnchangedRB = new javax.swing.JRadioButton();
        tmmRandomRB = new javax.swing.JRadioButton();
        tmKeepFieldMovesCB = new javax.swing.JCheckBox();
        tmFullHMCompatCB = new javax.swing.JCheckBox();
        tmForceGoodDamagingCB = new javax.swing.JCheckBox();
        tmForceGoodDamagingSlider = new javax.swing.JSlider();
        tmHmCompatPanel = new javax.swing.JPanel();
        thcUnchangedRB = new javax.swing.JRadioButton();
        thcRandomTypeRB = new javax.swing.JRadioButton();
        thcRandomTotalRB = new javax.swing.JRadioButton();
        thcFullRB = new javax.swing.JRadioButton();
        moveTutorsPanel = new javax.swing.JPanel();
        mtMovesPanel = new javax.swing.JPanel();
        mtmUnchangedRB = new javax.swing.JRadioButton();
        mtmRandomRB = new javax.swing.JRadioButton();
        mtKeepFieldMovesCB = new javax.swing.JCheckBox();
        mtForceGoodDamagingCB = new javax.swing.JCheckBox();
        mtForceGoodDamagingSlider = new javax.swing.JSlider();
        mtCompatPanel = new javax.swing.JPanel();
        mtcUnchangedRB = new javax.swing.JRadioButton();
        mtcRandomTypeRB = new javax.swing.JRadioButton();
        mtcRandomTotalRB = new javax.swing.JRadioButton();
        mtcFullRB = new javax.swing.JRadioButton();
        mtNoExistLabel = new javax.swing.JLabel();
        fieldItemsInnerPanel = new javax.swing.JPanel();
        fieldItemsPanel = new javax.swing.JPanel();
        fiUnchangedRB = new javax.swing.JRadioButton();
        fiShuffleRB = new javax.swing.JRadioButton();
        fiRandomRB = new javax.swing.JRadioButton();
        fiBanBadCB = new javax.swing.JCheckBox();
        fiRandomizeGivenItemsCB = new javax.swing.JCheckBox();
        fiRandomizePickupTablesCB = new javax.swing.JCheckBox();
        fiRandomizeBerryTreesCB = new javax.swing.JCheckBox();
        fiRandomizeMartsCB = new javax.swing.JCheckBox();
        fiAllMartsHaveBallAndRepel = new javax.swing.JCheckBox();
        fiRandomItemPrices = new javax.swing.JCheckBox();
        tpRandomFrontier = new javax.swing.JCheckBox();
        tpFillBossTeams = new javax.swing.JCheckBox();
        miscTweaksInnerPanel = new javax.swing.JPanel();
        warpsInnerPanel = new javax.swing.JPanel();
        miscTweaksPanel = new javax.swing.JPanel();
        warpsPanel = new javax.swing.JPanel();
        mtNoneAvailableLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        websiteLinkLabel = new javax.swing.JLabel();
        gameMascotLabel = new javax.swing.JLabel();

        wrUnchangedRB = new javax.swing.JRadioButton();
        wrRandomRB = new javax.swing.JRadioButton();
        wrKeepUselessDeadends = new javax.swing.JCheckBox();
        wrRemoveGymOrderLogic = new javax.swing.JCheckBox();

        romOpenChooser.setFileFilter(new ROMFilter());

        romSaveChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        romSaveChooser.setFileFilter(new ROMFilter());

        qsOpenChooser.setFileFilter(new QSFileFilter());

        qsSaveChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        qsSaveChooser.setFileFilter(new QSFileFilter());

        toggleAutoUpdatesMenuItem.setText(bundle.getString("RandomizerGUI.toggleAutoUpdatesMenuItem.text")); // NOI18N
        toggleAutoUpdatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleAutoUpdatesMenuItemActionPerformed(evt);
            }
        });
        updateSettingsMenu.add(toggleAutoUpdatesMenuItem);

        manualUpdateMenuItem.setText(bundle.getString("RandomizerGUI.manualUpdateMenuItem.text")); // NOI18N
        manualUpdateMenuItem.addActionListener(this::manualUpdateMenuItemActionPerformed);
        updateSettingsMenu.add(manualUpdateMenuItem);

        toggleScrollPaneMenuItem.setText(bundle.getString("RandomizerGUI.toggleScrollPaneMenuItem.text")); // NOI18N
        toggleScrollPaneMenuItem.addActionListener(this::toggleScrollPaneMenuItemActionPerformed);
        updateSettingsMenu.add(toggleScrollPaneMenuItem);

        customNamesEditorMenuItem.setText(bundle.getString("RandomizerGUI.customNamesEditorMenuItem.text")); // NOI18N
        customNamesEditorMenuItem.addActionListener(this::customNamesEditorMenuItemActionPerformed);
        updateSettingsMenu.add(customNamesEditorMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(bundle.getString("RandomizerGUI.title")); // NOI18N

        generalOptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.generalOptionsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        pokeLimitCB.setText(bundle.getString("RandomizerGUI.pokeLimitCB.text")); // NOI18N
        pokeLimitCB.setToolTipText(bundle.getString("RandomizerGUI.pokeLimitCB.toolTipText")); // NOI18N
        pokeLimitCB.addActionListener(this::pokeLimitCBActionPerformed);

        pokeLimitBtn.setText(bundle.getString("RandomizerGUI.pokeLimitBtn.text")); // NOI18N
        pokeLimitBtn.addActionListener(this::pokeLimitBtnActionPerformed);

        raceModeCB.setText(bundle.getString("RandomizerGUI.raceModeCB.text")); // NOI18N
        raceModeCB.setToolTipText(bundle.getString("RandomizerGUI.raceModeCB.toolTipText")); // NOI18N

        brokenMovesCB.setText(bundle.getString("RandomizerGUI.brokenMovesCB.text")); // NOI18N
        brokenMovesCB.setToolTipText(bundle.getString("RandomizerGUI.brokenMovesCB.toolTipText")); // NOI18N

        javax.swing.GroupLayout generalOptionsPanelLayout = new javax.swing.GroupLayout(generalOptionsPanel);
        generalOptionsPanel.setLayout(generalOptionsPanelLayout);
        generalOptionsPanelLayout.setHorizontalGroup(
            generalOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generalOptionsPanelLayout.createSequentialGroup()
                        .addComponent(pokeLimitCB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pokeLimitBtn))
                    .addComponent(raceModeCB)
                    .addComponent(brokenMovesCB))
                .addContainerGap(22, Short.MAX_VALUE))
        );
        generalOptionsPanelLayout.setVerticalGroup(
            generalOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pokeLimitBtn)
                    .addComponent(pokeLimitCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(raceModeCB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(brokenMovesCB)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        romInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.romInfoPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        riRomNameLabel.setText(bundle.getString("RandomizerGUI.riRomNameLabel.text")); // NOI18N

        riRomCodeLabel.setText(bundle.getString("RandomizerGUI.riRomCodeLabel.text")); // NOI18N

        riRomSupportLabel.setText(bundle.getString("RandomizerGUI.riRomSupportLabel.text")); // NOI18N

        javax.swing.GroupLayout romInfoPanelLayout = new javax.swing.GroupLayout(romInfoPanel);
        romInfoPanel.setLayout(romInfoPanelLayout);
        romInfoPanelLayout.setHorizontalGroup(
            romInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(romInfoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(romInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(riRomNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                    .addComponent(riRomCodeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(riRomSupportLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        romInfoPanelLayout.setVerticalGroup(
            romInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(romInfoPanelLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(riRomNameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(riRomCodeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(riRomSupportLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        openROMButton.setText(bundle.getString("RandomizerGUI.openROMButton.text")); // NOI18N
        openROMButton.addActionListener(this::openROMButtonActionPerformed);
        FileDropListener romDropListener = new FileDropListener(this::loadRomFile, ".gba", ".zip");
        new DropTarget(openROMButton, romDropListener);

        saveROMButton.setText(bundle.getString("RandomizerGUI.saveROMButton.text")); // NOI18N
        saveROMButton.addActionListener(this::saveROMButtonActionPerformed);

        bulkSaveSelection.addActionListener(this::bulkSaveSelectionActionPerformed);
        bulkSaveSelection.setMaximumSize(bulkSaveSelection.getPreferredSize()); // added code
        bulkSaveSelection.setAlignmentX(Component.CENTER_ALIGNMENT);// added code

        usePresetsButton.setText(bundle.getString("RandomizerGUI.usePresetsButton.text")); // NOI18N
        usePresetsButton.addActionListener(this::usePresetsButtonActionPerformed);
        usePresetsButton.setVisible(false);

        loadQSButton.setText(bundle.getString("RandomizerGUI.loadQSButton.text")); // NOI18N
        loadQSButton.setToolTipText(bundle.getString("RandomizerGUI.loadQSButton.toolTipText")); // NOI18N
        loadQSButton.addActionListener(this::loadQSButtonActionPerformed);
        FileDropListener settingsDropListener = new FileDropListener(this::loadSettingsFile, "." + CONFIG_FILE_EXTENSION);
        new DropTarget(loadQSButton, settingsDropListener);

        saveQSButton.setText(bundle.getString("RandomizerGUI.saveQSButton.text")); // NOI18N
        saveQSButton.setToolTipText(bundle.getString("RandomizerGUI.saveQSButton.toolTipText")); // NOI18N
        saveQSButton.addActionListener(this::saveQSButtonActionPerformed);

        settingsButton.setText(bundle.getString("RandomizerGUI.settingsButton.text")); // NOI18N
        settingsButton.addActionListener(this::settingsButtonActionPerformed);
        settingsButton.setVisible(false);

        pokemonTypesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.pokemonTypesPanel.border.title"),
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        typeChartPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.typeChartPanel.border.title"),
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        pokeTypesButtonGroup.add(ptUnchangedRB);
        ptUnchangedRB.setSelected(true);
        ptUnchangedRB.setText(bundle.getString("RandomizerGUI.ptUnchangedRB.text")); // NOI18N
        ptUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.ptUnchangedRB.toolTipText")); // NOI18N

        pokeTypesButtonGroup.add(ptRandomFollowEvosRB);
        ptRandomFollowEvosRB.setText(bundle.getString("RandomizerGUI.ptRandomFollowEvosRB.text")); // NOI18N
        ptRandomFollowEvosRB.setToolTipText(bundle.getString("RandomizerGUI.ptRandomFollowEvosRB.toolTipText")); // NOI18N

        pokeTypesButtonGroup.add(ptRandomTotalRB);
        ptRandomTotalRB.setText(bundle.getString("RandomizerGUI.ptRandomTotalRB.text")); // NOI18N
        ptRandomTotalRB.setToolTipText(bundle.getString("RandomizerGUI.ptRandomTotalRB.toolTipText")); // NOI18N

        javax.swing.GroupLayout pokemonTypesPanelLayout = new javax.swing.GroupLayout(pokemonTypesPanel);
        pokemonTypesPanel.setLayout(pokemonTypesPanelLayout);
        pokemonTypesPanelLayout.setHorizontalGroup(
            pokemonTypesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokemonTypesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pokemonTypesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ptUnchangedRB)
                    .addComponent(ptRandomFollowEvosRB)
                    .addComponent(ptRandomTotalRB))
                .addContainerGap(68, Short.MAX_VALUE))
        );
        pokemonTypesPanelLayout.setVerticalGroup(
            pokemonTypesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokemonTypesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ptUnchangedRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ptRandomFollowEvosRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ptRandomTotalRB)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        typeChartButtonGroup.add(tcUnchangedRB);
        tcUnchangedRB.setSelected(true);
        tcUnchangedRB.setText(bundle.getString("RandomizerGUI.tcUnchangedRB.text")); // NOI18N
        tcUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.tcUnchangedRB.toolTipText")); // NOI18N


        typeChartButtonGroup.add(tcRandomShuffleRowsRB);
        tcRandomShuffleRowsRB.setText(bundle.getString("RandomizerGUI.tcRandomShuffleRowsRB.text")); // NOI18N
        tcRandomShuffleRowsRB.setToolTipText(bundle.getString("RandomizerGUI.tcRandomShuffleRowsRB.toolTipText")); // NOI18N

        typeChartButtonGroup.add(tcRandomShuffleRB);
        tcRandomShuffleRB.setText(bundle.getString("RandomizerGUI.tcRandomShuffleRB.text")); // NOI18N
        tcRandomShuffleRB.setToolTipText(bundle.getString("RandomizerGUI.tcRandomShuffleRB.toolTipText")); // NOI18N

        typeChartButtonGroup.add(tcRandomTotalRB);
        tcRandomTotalRB.setText(bundle.getString("RandomizerGUI.tcRandomTotalRB.text")); // NOI18N
        tcRandomTotalRB.setToolTipText(bundle.getString("RandomizerGUI.tcRandomTotalRB.toolTipText")); // NOI18N

        javax.swing.GroupLayout typeChartPanelLayout = new javax.swing.GroupLayout(typeChartPanel);
        typeChartPanel.setLayout(typeChartPanelLayout);
        typeChartPanelLayout.setHorizontalGroup(
                typeChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(typeChartPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(typeChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(tcUnchangedRB)
                                        .addComponent(tcRandomShuffleRowsRB)
                                        .addComponent(tcRandomShuffleRB)
                                        .addComponent(tcRandomTotalRB))
                                .addContainerGap(68, Short.MAX_VALUE))
        );
        typeChartPanelLayout.setVerticalGroup(
                typeChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(typeChartPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(tcUnchangedRB)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tcRandomShuffleRowsRB)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tcRandomShuffleRB)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tcRandomTotalRB)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        baseStatsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.baseStatsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        pokeStatChangesButtonGroup.add(pbsChangesUnchangedRB);
        pbsChangesUnchangedRB.setSelected(true);
        pbsChangesUnchangedRB.setText(bundle.getString("RandomizerGUI.pbsChangesUnchangedRB.text")); // NOI18N
        pbsChangesUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.pbsChangesUnchangedRB.toolTipText")); // NOI18N
        pbsChangesUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbsChangesUnchangedRBActionPerformed(evt);
            }
        });

        pokeStatChangesButtonGroup.add(pbsChangesShuffleRB);
        pbsChangesShuffleRB.setText(bundle.getString("RandomizerGUI.pbsChangesShuffleRB.text")); // NOI18N
        pbsChangesShuffleRB.setToolTipText(bundle.getString("RandomizerGUI.pbsChangesShuffleRB.toolTipText")); // NOI18N
        pbsChangesShuffleRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbsChangesShuffleRBActionPerformed(evt);
            }
        });

        pokeStatChangesButtonGroup.add(pbsChangesRandomRB);
        pbsChangesRandomRB.setText(bundle.getString("RandomizerGUI.pbsChangesRandomRB.text")); // NOI18N
        pbsChangesRandomRB.setToolTipText(bundle.getString("RandomizerGUI.pbsChangesRandomRB.toolTipText")); // NOI18N
        pbsChangesRandomRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbsChangesRandomRBActionPerformed(evt);
            }
        });

        pbsStandardEXPCurvesCB.setText(bundle.getString("RandomizerGUI.pbsStandardEXPCurvesCB.text")); // NOI18N
        pbsStandardEXPCurvesCB.setToolTipText(bundle.getString("RandomizerGUI.pbsStandardEXPCurvesCB.toolTipText")); // NOI18N

        pbsFollowEvolutionsCB.setText(bundle.getString("RandomizerGUI.pbsFollowEvolutionsCB.text")); // NOI18N
        pbsFollowEvolutionsCB.setToolTipText(bundle.getString("RandomizerGUI.pbsFollowEvolutionsCB.toolTipText")); // NOI18N

        pbsUpdateStatsCB.setText(bundle.getString("RandomizerGUI.pbsUpdateStatsCB.text")); // NOI18N
        pbsUpdateStatsCB.setToolTipText(bundle.getString("RandomizerGUI.pbsUpdateStatsCB.toolTipText")); // NOI18N

        pokeStatChangesButtonGroup.add(pbsChangesRandomBSTRB);
        pbsChangesRandomBSTRB.setText(bundle.getString("RandomizerGUI.pbsChangesRandomBSTRB.text")); // NOI18N
        pbsChangesRandomBSTRB.setToolTipText(bundle.getString("RandomizerGUI.pbsChangesRandomBSTRB.toolTipText")); // NOI18N
        pbsChangesRandomBSTRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbsChangesRandomBSTRBActionPerformed(evt);
            }
        });

        pbsEvosBuffStatsCB.setText(bundle.getString("RandomizerGUI.pbsEvosBuffStatsCB.text")); // NOI18N
        pbsEvosBuffStatsCB.setToolTipText(bundle.getString("RandomizerGUI.pbsEvosBuffStatsCB.toolTipText")); // NOI18N

        pbsBaseStatRangeSlider.setMajorTickSpacing(10);
        pbsBaseStatRangeSlider.setMaximum(50);
        pbsBaseStatRangeSlider.setMinorTickSpacing(5);
        pbsBaseStatRangeSlider.setPaintLabels(true);
        pbsBaseStatRangeSlider.setPaintTicks(true);
        pbsBaseStatRangeSlider.setSnapToTicks(true);
        pbsBaseStatRangeSlider.setToolTipText(bundle.getString("RandomizerGUI.pbsBaseStatRangeSlider.toolTipText")); // NOI18N
        pbsBaseStatRangeSlider.setValue(0);

        pokeStatChangesButtonGroup.add(pbsChangesRandomBSTPERCRB);
        pbsChangesRandomBSTPERCRB.setText(bundle.getString("RandomizerGUI.pbsChangesRandomBSTPERCRB.text")); // NOI18N
        pbsChangesRandomBSTPERCRB.setToolTipText(bundle.getString("RandomizerGUI.pbsChangesRandomBSTPERCRB.toolTipText")); // NOI18N
        pbsChangesRandomBSTPERCRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbsChangesRandomBSTPERCRBActionPerformed(evt);
            }
        });

        pokeStatChangesButtonGroup.add(pbsChangesEqualizeRB);
        pbsChangesEqualizeRB.setText(bundle.getString("RandomizerGUI.pbsChangesEqualizeRB.text")); // NOI18N
        pbsChangesEqualizeRB.setToolTipText(bundle.getString("RandomizerGUI.pbsChangesEqualizeRB.toolTipText")); // NOI18N
        pbsChangesEqualizeRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbsChangesEqualizeRBActionPerformed(evt);
            }
        });

        pbsDontRandomizeRatioCB.setText(bundle.getString("RandomizerGUI.pbsDontRandomizeRatioCB.text")); // NOI18N
        pbsDontRandomizeRatioCB.setToolTipText(bundle.getString("RandomizerGUI.pbsDontRandomizeRatioCB.toolTipText")); // NOI18N

        javax.swing.GroupLayout baseStatsPanelLayout = new javax.swing.GroupLayout(baseStatsPanel);
        baseStatsPanel.setLayout(baseStatsPanelLayout);
        baseStatsPanelLayout.setHorizontalGroup(
            baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(baseStatsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(baseStatsPanelLayout.createSequentialGroup()
                        .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pbsChangesShuffleRB)
                            .addComponent(pbsChangesUnchangedRB)
                            .addComponent(pbsChangesRandomRB)
                            .addComponent(pbsChangesRandomBSTRB))
                        .addGap(18, 18, 18)
                        .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pbsEvosBuffStatsCB)
                            .addComponent(pbsFollowEvolutionsCB)
                            .addComponent(pbsStandardEXPCurvesCB)
                            .addComponent(pbsUpdateStatsCB)))
                    .addGroup(baseStatsPanelLayout.createSequentialGroup()
                        .addComponent(pbsChangesRandomBSTPERCRB)
                        .addGap(6, 6, 6)
                        .addComponent(pbsBaseStatRangeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(baseStatsPanelLayout.createSequentialGroup()
                        .addComponent(pbsChangesEqualizeRB)
                        .addGap(18, 18, 18)
                        .addComponent(pbsDontRandomizeRatioCB)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        baseStatsPanelLayout.setVerticalGroup(
            baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(baseStatsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pbsChangesUnchangedRB)
                    .addComponent(pbsStandardEXPCurvesCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pbsChangesShuffleRB)
                    .addComponent(pbsFollowEvolutionsCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pbsChangesRandomRB)
                    .addComponent(pbsUpdateStatsCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pbsChangesRandomBSTRB)
                    .addComponent(pbsEvosBuffStatsCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pbsBaseStatRangeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pbsChangesRandomBSTPERCRB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(baseStatsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pbsDontRandomizeRatioCB)
                    .addComponent(pbsChangesEqualizeRB)))
        );

        abilitiesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.abilitiesPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        pokeAbilitiesButtonGroup.add(paUnchangedRB);
        paUnchangedRB.setSelected(true);
        paUnchangedRB.setText(bundle.getString("RandomizerGUI.paUnchangedRB.text")); // NOI18N
        paUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.paUnchangedRB.toolTipText")); // NOI18N
        paUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paUnchangedRBActionPerformed(evt);
            }
        });

        pokeAbilitiesButtonGroup.add(paRandomizeRB);
        paRandomizeRB.setText(bundle.getString("RandomizerGUI.paRandomizeRB.text")); // NOI18N
        paRandomizeRB.setToolTipText(bundle.getString("RandomizerGUI.paRandomizeRB.toolTipText")); // NOI18N
        paRandomizeRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paRandomizeRBActionPerformed(evt);
            }
        });

        paWonderGuardCB.setText(bundle.getString("RandomizerGUI.paWonderGuardCB.text")); // NOI18N
        paWonderGuardCB.setToolTipText(bundle.getString("RandomizerGUI.paWonderGuardCB.toolTipText")); // NOI18N

        paFollowEvolutionsCB.setText(bundle.getString("RandomizerGUI.paFollowEvolutionsCB.text")); // NOI18N
        paFollowEvolutionsCB.setToolTipText(bundle.getString("RandomizerGUI.paFollowEvolutionsCB.toolTipText")); // NOI18N

        paBansLabel.setText(bundle.getString("RandomizerGUI.paBansLabel.text")); // NOI18N

        paBanTrappingCB.setText(bundle.getString("RandomizerGUI.paBanTrappingCB.text")); // NOI18N
        paBanTrappingCB.setToolTipText(bundle.getString("RandomizerGUI.paBanTrappingCB.toolTipText")); // NOI18N

        paBanNegativeCB.setText(bundle.getString("RandomizerGUI.paBanNegativeCB.text")); // NOI18N
        paBanNegativeCB.setToolTipText(bundle.getString("RandomizerGUI.paBanNegativeCB.toolTipText")); // NOI18N

        javax.swing.GroupLayout abilitiesPanelLayout = new javax.swing.GroupLayout(abilitiesPanel);
        abilitiesPanel.setLayout(abilitiesPanelLayout);
        abilitiesPanelLayout.setHorizontalGroup(
            abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(abilitiesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(abilitiesPanelLayout.createSequentialGroup()
                        .addGroup(abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(paUnchangedRB)
                            .addComponent(paRandomizeRB))
                        .addGap(32, 32, 32))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, abilitiesPanelLayout.createSequentialGroup()
                        .addComponent(paBansLabel)
                        .addGap(18, 18, 18)))
                .addGroup(abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(paWonderGuardCB)
                    .addGroup(abilitiesPanelLayout.createSequentialGroup()
                        .addGroup(abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(paFollowEvolutionsCB)
                            .addComponent(paBanTrappingCB))
                        .addGap(18, 18, 18)
                        .addComponent(paBanNegativeCB)))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        abilitiesPanelLayout.setVerticalGroup(
            abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(abilitiesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(paUnchangedRB)
                    .addComponent(paWonderGuardCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(paRandomizeRB)
                    .addComponent(paFollowEvolutionsCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(abilitiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(paBansLabel)
                    .addComponent(paBanTrappingCB)
                    .addComponent(paBanNegativeCB))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pokemonEvolutionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.pokemonEvolutionsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        pokeEvolutionsButtonGroup.add(peUnchangedRB);
        peUnchangedRB.setSelected(true);
        peUnchangedRB.setText(bundle.getString("RandomizerGUI.peUnchangedRB.text")); // NOI18N
        peUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.peUnchangedRB.toolTipText")); // NOI18N
        peUnchangedRB.addActionListener(this::peUnchangedRBActionPerformed);

        pokeEvolutionsButtonGroup.add(peRandomRB);
        peRandomRB.setText(bundle.getString("RandomizerGUI.peRandomRB.text")); // NOI18N
        peRandomRB.setToolTipText(bundle.getString("RandomizerGUI.peRandomRB.toolTipText")); // NOI18N
        peRandomRB.addActionListener(this::peRandomRBActionPerformed);

        peSimilarStrengthCB.setText(bundle.getString("RandomizerGUI.peSimilarStrengthCB.text")); // NOI18N
        peSimilarStrengthCB.setToolTipText(bundle.getString("RandomizerGUI.peSimilarStrengthCB.toolTipText")); // NOI18N

        peSameTypeCB.setText(bundle.getString("RandomizerGUI.peSameTypeCB.text")); // NOI18N
        peSameTypeCB.setToolTipText(bundle.getString("RandomizerGUI.peSameTypeCB.toolTipText")); // NOI18N

        peThreeStagesCB.setText(bundle.getString("RandomizerGUI.peThreeStagesCB.text")); // NOI18N
        peThreeStagesCB.setToolTipText(bundle.getString("RandomizerGUI.peThreeStagesCB.toolTipText")); // NOI18N

        peForceChangeCB.setText(bundle.getString("RandomizerGUI.peForceChangeCB.text")); // NOI18N
        peForceChangeCB.setToolTipText(bundle.getString("RandomizerGUI.peForceChangeCB.toolTipText")); // NOI18N

        goRemoveTradeEvosCheckBox.setText(bundle.getString("RandomizerGUI.goRemoveTradeEvosCheckBox.text")); // NOI18N
        goRemoveTradeEvosCheckBox.setToolTipText(bundle.getString("RandomizerGUI.goRemoveTradeEvosCheckBox.toolTipText")); // NOI18N

        goCondenseEvosCheckBox.setText(bundle.getString("RandomizerGUI.goCondenseEvosCheckBox.text")); // NOI18N
        goCondenseEvosCheckBox.setToolTipText(bundle.getString("RandomizerGUI.goCondenseEvosCheckBox.toolTipText")); // NOI18N
        goCondenseEvosCheckBox.addActionListener(this::goCondenseEvosCheckBoxActionPerformed);


        javax.swing.GroupLayout pokemonEvolutionsPanelLayout = new javax.swing.GroupLayout(pokemonEvolutionsPanel);
        pokemonEvolutionsPanel.setLayout(pokemonEvolutionsPanelLayout);
        pokemonEvolutionsPanelLayout.setHorizontalGroup(
            pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokemonEvolutionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peUnchangedRB)
                    .addComponent(peRandomRB))
                .addGap(83, 83, 83)
                .addGroup(pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peForceChangeCB)
                    .addComponent(peThreeStagesCB)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pokemonEvolutionsPanelLayout.createSequentialGroup()
                        .addGroup(pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(peSimilarStrengthCB)
                            .addComponent(peSameTypeCB))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                        .addGroup(pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(goCondenseEvosCheckBox)
                            .addComponent(goRemoveTradeEvosCheckBox))))
                .addContainerGap())
        );
        pokemonEvolutionsPanelLayout.setVerticalGroup(
            pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokemonEvolutionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(peUnchangedRB)
                    .addComponent(peSimilarStrengthCB)
                    .addComponent(goRemoveTradeEvosCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pokemonEvolutionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(peRandomRB)
                    .addComponent(peSameTypeCB)
                    .addComponent(goCondenseEvosCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peThreeStagesCB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peForceChangeCB)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pokeTraitsPanelLayout = new javax.swing.GroupLayout(pokeTraitsPanel);
        pokeTraitsPanel.setLayout(pokeTraitsPanelLayout);
        pokeTraitsPanelLayout.setHorizontalGroup(
            pokeTraitsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokeTraitsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pokeTraitsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pokeTraitsPanelLayout.createSequentialGroup()
                        .addComponent(baseStatsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(abilitiesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pokeTraitsPanelLayout.createSequentialGroup()
                        .addComponent(pokemonTypesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pokemonEvolutionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pokeTraitsPanelLayout.setVerticalGroup(
            pokeTraitsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokeTraitsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pokeTraitsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(abilitiesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(baseStatsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pokeTraitsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pokemonTypesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pokemonEvolutionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(69, Short.MAX_VALUE))
        );

        starterPokemonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.starterPokemonPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        starterPokemonButtonGroup.add(spUnchangedRB);
        spUnchangedRB.setSelected(true);
        spUnchangedRB.setText(bundle.getString("RandomizerGUI.spUnchangedRB.text")); // NOI18N
        spUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.spUnchangedRB.toolTipText")); // NOI18N
        spUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spUnchangedRBActionPerformed(evt);
            }
        });

        starterPokemonButtonGroup.add(spCustomRB);
        spCustomRB.setText(bundle.getString("RandomizerGUI.spCustomRB.text")); // NOI18N
        spCustomRB.setToolTipText(bundle.getString("RandomizerGUI.spCustomRB.toolTipText")); // NOI18N
        spCustomRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spCustomRBActionPerformed(evt);
            }
        });

        spCustomPoke1Chooser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        spCustomPoke1Chooser.setEnabled(false);

        spCustomPoke2Chooser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        spCustomPoke2Chooser.setEnabled(false);

        spCustomPoke3Chooser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        spCustomPoke3Chooser.setEnabled(false);

        starterPokemonButtonGroup.add(spRandomRB);
        spRandomRB.setText(bundle.getString("RandomizerGUI.spRandomRB.text")); // NOI18N
        spRandomRB.setToolTipText(bundle.getString("RandomizerGUI.spRandomRB.toolTipText")); // NOI18N
        spRandomRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spRandomRBActionPerformed(evt);
            }
        });

        starterPokemonButtonGroup.add(spRandom2EvosRB);
        spRandom2EvosRB.setText(bundle.getString("RandomizerGUI.spRandom2EvosRB.text")); // NOI18N
        spRandom2EvosRB.setToolTipText(bundle.getString("RandomizerGUI.spRandom2EvosRB.toolTipText")); // NOI18N
        spRandom2EvosRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spRandom2EvosRBActionPerformed(evt);
            }
        });

        spHeldItemsCB.setText(bundle.getString("RandomizerGUI.spHeldItemsCB.text")); // NOI18N
        spHeldItemsCB.setToolTipText(bundle.getString("RandomizerGUI.spHeldItemsCB.toolTipText")); // NOI18N
        spHeldItemsCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spHeldItemsCBActionPerformed(evt);
            }
        });

        spHeldItemsBanBadCB.setText(bundle.getString("RandomizerGUI.spHeldItemsBanBadCB.text")); // NOI18N
        spHeldItemsBanBadCB.setToolTipText(bundle.getString("RandomizerGUI.spHeldItemsBanBadCB.toolTipText")); // NOI18N

        starterPokemonButtonGroup.add(spRandom1EvosRB);
        spRandom1EvosRB.setText(bundle.getString("RandomizerGUI.spRandom1EvosRB.text")); // NOI18N
        spRandom1EvosRB.setToolTipText(bundle.getString("RandomizerGUI.spRandom1EvosRB.toolTipText")); // NOI18N
        spRandom1EvosRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spRandom1EvosRBActionPerformed(evt);
            }
        });

        starterPokemonButtonGroup.add(spRandom0EvosRB);
        spRandom0EvosRB.setText(bundle.getString("RandomizerGUI.spRandom0EvosRB.text")); // NOI18N
        spRandom0EvosRB.setToolTipText(bundle.getString("RandomizerGUI.spRandom0EvosRB.toolTipText")); // NOI18N
        spRandom0EvosRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spRandom0EvosRBActionPerformed(evt);
            }
        });

        spBanLegendaryStartersCB.setText(bundle.getString("RandomizerGUI.spBanLegendaryStartersCB.text")); // NOI18N
        spBanLegendaryStartersCB.setToolTipText(bundle.getString("RandomizerGUI.spBanLegendaryStartersCB.toolTipText")); // NOI18N
        spBanLegendaryStartersCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spBanLegendaryStartersCBActionPerformed(evt);
            }
        });

        spOnlyLegendaryStartersCB.setText(bundle.getString("RandomizerGUI.spOnlyLegendaryStartersCB.text")); // NOI18N
        spOnlyLegendaryStartersCB.setToolTipText(bundle.getString("RandomizerGUI.spOnlyLegendaryStartersCB.toolTipText")); // NOI18N
        spOnlyLegendaryStartersCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spOnlyLegendaryStartersCBActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout starterPokemonPanelLayout = new javax.swing.GroupLayout(starterPokemonPanel);
        starterPokemonPanel.setLayout(starterPokemonPanelLayout);
        starterPokemonPanelLayout.setHorizontalGroup(
            starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starterPokemonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spRandom0EvosRB)
                    .addGroup(starterPokemonPanelLayout.createSequentialGroup()
                        .addGroup(starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(starterPokemonPanelLayout.createSequentialGroup()
                                .addComponent(spCustomRB)
                                .addGap(18, 18, 18)
                                .addComponent(spCustomPoke1Chooser, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spCustomPoke2Chooser, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spCustomPoke3Chooser, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(spRandomRB)
                            .addComponent(spUnchangedRB)
                            .addComponent(spRandom2EvosRB)
                            .addComponent(spRandom1EvosRB))
                        .addGap(18, 18, 18)
                        .addGroup(starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spOnlyLegendaryStartersCB)
                            .addComponent(spBanLegendaryStartersCB)
                            .addComponent(spHeldItemsBanBadCB)
                            .addComponent(spHeldItemsCB))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        starterPokemonPanelLayout.setVerticalGroup(
            starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(starterPokemonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spUnchangedRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spCustomRB)
                    .addComponent(spCustomPoke1Chooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spCustomPoke2Chooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spCustomPoke3Chooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spHeldItemsCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spRandomRB)
                    .addComponent(spHeldItemsBanBadCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spRandom2EvosRB)
                    .addComponent(spBanLegendaryStartersCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(starterPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spRandom1EvosRB)
                    .addComponent(spOnlyLegendaryStartersCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(spRandom0EvosRB)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        staticPokemonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.staticPokemonPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        staticPokemonButtonGroup.add(stpUnchangedRB);
        stpUnchangedRB.setSelected(true);
        stpUnchangedRB.setText(bundle.getString("RandomizerGUI.stpUnchangedRB.text")); // NOI18N
        stpUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.stpUnchangedRB.toolTipText")); // NOI18N

        staticPokemonButtonGroup.add(stpRandomL4LRB);
        stpRandomL4LRB.setText(bundle.getString("RandomizerGUI.stpRandomL4LRB.text")); // NOI18N
        stpRandomL4LRB.setToolTipText(bundle.getString("RandomizerGUI.stpRandomL4LRB.toolTipText")); // NOI18N

        staticPokemonButtonGroup.add(stpRandomTotalRB);
        stpRandomTotalRB.setText(bundle.getString("RandomizerGUI.stpRandomTotalRB.text")); // NOI18N
        stpRandomTotalRB.setToolTipText(bundle.getString("RandomizerGUI.stpRandomTotalRB.toolTipText")); // NOI18N

        javax.swing.GroupLayout staticPokemonPanelLayout = new javax.swing.GroupLayout(staticPokemonPanel);
        staticPokemonPanel.setLayout(staticPokemonPanelLayout);
        staticPokemonPanelLayout.setHorizontalGroup(
            staticPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(staticPokemonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(staticPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(stpUnchangedRB)
                    .addComponent(stpRandomL4LRB)
                    .addComponent(stpRandomTotalRB))
                .addContainerGap(407, Short.MAX_VALUE))
        );
        staticPokemonPanelLayout.setVerticalGroup(
            staticPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(staticPokemonPanelLayout.createSequentialGroup()
                .addComponent(stpUnchangedRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(stpRandomL4LRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(stpRandomTotalRB))
        );

        inGameTradesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.inGameTradesPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        ingameTradesButtonGroup.add(igtUnchangedRB);
        igtUnchangedRB.setSelected(true);
        igtUnchangedRB.setText(bundle.getString("RandomizerGUI.igtUnchangedRB.text")); // NOI18N
        igtUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.igtUnchangedRB.toolTipText")); // NOI18N
        igtUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                igtUnchangedRBActionPerformed(evt);
            }
        });

        ingameTradesButtonGroup.add(igtGivenOnlyRB);
        igtGivenOnlyRB.setText(bundle.getString("RandomizerGUI.igtGivenOnlyRB.text")); // NOI18N
        igtGivenOnlyRB.setToolTipText(bundle.getString("RandomizerGUI.igtGivenOnlyRB.toolTipText")); // NOI18N
        igtGivenOnlyRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                igtGivenOnlyRBActionPerformed(evt);
            }
        });

        ingameTradesButtonGroup.add(igtBothRB);
        igtBothRB.setText(bundle.getString("RandomizerGUI.igtBothRB.text")); // NOI18N
        igtBothRB.setToolTipText(bundle.getString("RandomizerGUI.igtBothRB.toolTipText")); // NOI18N
        igtBothRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                igtBothRBActionPerformed(evt);
            }
        });

        igtRandomNicknameCB.setText(bundle.getString("RandomizerGUI.igtRandomNicknameCB.text")); // NOI18N
        igtRandomNicknameCB.setToolTipText(bundle.getString("RandomizerGUI.igtRandomNicknameCB.toolTipText")); // NOI18N

        igtRandomOTCB.setText(bundle.getString("RandomizerGUI.igtRandomOTCB.text")); // NOI18N
        igtRandomOTCB.setToolTipText(bundle.getString("RandomizerGUI.igtRandomOTCB.toolTipText")); // NOI18N

        igtRandomIVsCB.setText(bundle.getString("RandomizerGUI.igtRandomIVsCB.text")); // NOI18N
        igtRandomIVsCB.setToolTipText(bundle.getString("RandomizerGUI.igtRandomIVsCB.toolTipText")); // NOI18N

        igtRandomItemCB.setText(bundle.getString("RandomizerGUI.igtRandomItemCB.text")); // NOI18N
        igtRandomItemCB.setToolTipText(bundle.getString("RandomizerGUI.igtRandomItemCB.toolTipText")); // NOI18N

        javax.swing.GroupLayout inGameTradesPanelLayout = new javax.swing.GroupLayout(inGameTradesPanel);
        inGameTradesPanel.setLayout(inGameTradesPanelLayout);
        inGameTradesPanelLayout.setHorizontalGroup(
            inGameTradesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inGameTradesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inGameTradesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(igtUnchangedRB)
                    .addComponent(igtGivenOnlyRB)
                    .addComponent(igtBothRB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 229, Short.MAX_VALUE)
                .addGroup(inGameTradesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(igtRandomItemCB)
                    .addComponent(igtRandomNicknameCB)
                    .addComponent(igtRandomOTCB)
                    .addComponent(igtRandomIVsCB))
                .addGap(113, 113, 113))
        );
        inGameTradesPanelLayout.setVerticalGroup(
            inGameTradesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inGameTradesPanelLayout.createSequentialGroup()
                .addGroup(inGameTradesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(igtUnchangedRB)
                    .addComponent(igtRandomNicknameCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inGameTradesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(igtGivenOnlyRB)
                    .addComponent(igtRandomOTCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inGameTradesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(igtBothRB)
                    .addComponent(igtRandomIVsCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(igtRandomItemCB)
                .addContainerGap(11, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout startersInnerPanelLayout = new javax.swing.GroupLayout(startersInnerPanel);
        startersInnerPanel.setLayout(startersInnerPanelLayout);
        startersInnerPanelLayout.setHorizontalGroup(
            startersInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(startersInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(startersInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(starterPokemonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(staticPokemonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(inGameTradesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        startersInnerPanelLayout.setVerticalGroup(
            startersInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(startersInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(starterPokemonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(staticPokemonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inGameTradesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pokemonMovesetsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.pokemonMovesetsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        pokeMovesetsButtonGroup.add(pmsUnchangedRB);
        pmsUnchangedRB.setSelected(true);
        pmsUnchangedRB.setText(bundle.getString("RandomizerGUI.pmsUnchangedRB.text")); // NOI18N
        pmsUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.pmsUnchangedRB.toolTipText")); // NOI18N
        pmsUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmsUnchangedRBActionPerformed(evt);
            }
        });

        pokeMovesetsButtonGroup.add(pmsRandomTypeRB);
        pmsRandomTypeRB.setText(bundle.getString("RandomizerGUI.pmsRandomTypeRB.text")); // NOI18N
        pmsRandomTypeRB.setToolTipText(bundle.getString("RandomizerGUI.pmsRandomTypeRB.toolTipText")); // NOI18N
        pmsRandomTypeRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmsRandomTypeRBActionPerformed(evt);
            }
        });

        pokeMovesetsButtonGroup.add(pmsRandomTotalRB);
        pmsRandomTotalRB.setText(bundle.getString("RandomizerGUI.pmsRandomTotalRB.text")); // NOI18N
        pmsRandomTotalRB.setToolTipText(bundle.getString("RandomizerGUI.pmsRandomTotalRB.toolTipText")); // NOI18N
        pmsRandomTotalRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmsRandomTotalRBActionPerformed(evt);
            }
        });

        pokeMovesetsButtonGroup.add(pmsMetronomeOnlyRB);
        pmsMetronomeOnlyRB.setText(bundle.getString("RandomizerGUI.pmsMetronomeOnlyRB.text")); // NOI18N
        pmsMetronomeOnlyRB.setToolTipText(bundle.getString("RandomizerGUI.pmsMetronomeOnlyRB.toolTipText")); // NOI18N
        pmsMetronomeOnlyRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmsMetronomeOnlyRBActionPerformed(evt);
            }
        });

        pms4MovesCB.setText(bundle.getString("RandomizerGUI.pms4MovesCB.text")); // NOI18N
        pms4MovesCB.setToolTipText(bundle.getString("RandomizerGUI.pms4MovesCB.toolTipText")); // NOI18N

        pmsReorderDamagingMovesCB.setText(bundle.getString("RandomizerGUI.pmsReorderDamagingMovesCB.text")); // NOI18N
        pmsReorderDamagingMovesCB.setToolTipText(bundle.getString("RandomizerGUI.pmsReorderDamagingMovesCB.toolTipText")); // NOI18N

        pmsForceGoodDamagingCB.setText(bundle.getString("RandomizerGUI.pmsForceGoodDamagingCB.text")); // NOI18N
        pmsForceGoodDamagingCB.setToolTipText(bundle.getString("RandomizerGUI.pmsForceGoodDamagingCB.toolTipText")); // NOI18N
        pmsForceGoodDamagingCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmsForceGoodDamagingCBActionPerformed(evt);
            }
        });

        pmsForceGoodDamagingSlider.setMajorTickSpacing(20);
        pmsForceGoodDamagingSlider.setMinorTickSpacing(5);
        pmsForceGoodDamagingSlider.setPaintLabels(true);
        pmsForceGoodDamagingSlider.setPaintTicks(true);
        pmsForceGoodDamagingSlider.setSnapToTicks(true);
        pmsForceGoodDamagingSlider.setToolTipText(bundle.getString("RandomizerGUI.pmsForceGoodDamagingSlider.toolTipText")); // NOI18N
        pmsForceGoodDamagingSlider.setValue(0);

        javax.swing.GroupLayout pokemonMovesetsPanelLayout = new javax.swing.GroupLayout(pokemonMovesetsPanel);
        pokemonMovesetsPanel.setLayout(pokemonMovesetsPanelLayout);
        pokemonMovesetsPanelLayout.setHorizontalGroup(
            pokemonMovesetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokemonMovesetsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pokemonMovesetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pokemonMovesetsPanelLayout.createSequentialGroup()
                        .addComponent(pmsUnchangedRB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pms4MovesCB)
                        .addGap(151, 151, 151))
                    .addGroup(pokemonMovesetsPanelLayout.createSequentialGroup()
                        .addComponent(pmsRandomTypeRB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pmsReorderDamagingMovesCB)
                        .addGap(203, 203, 203))
                    .addGroup(pokemonMovesetsPanelLayout.createSequentialGroup()
                        .addComponent(pmsRandomTotalRB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pmsForceGoodDamagingCB)
                        .addGap(161, 161, 161))
                    .addGroup(pokemonMovesetsPanelLayout.createSequentialGroup()
                        .addComponent(pmsMetronomeOnlyRB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pmsForceGoodDamagingSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(152, 152, 152))))
        );
        pokemonMovesetsPanelLayout.setVerticalGroup(
            pokemonMovesetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pokemonMovesetsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pokemonMovesetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pmsUnchangedRB)
                    .addComponent(pms4MovesCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pokemonMovesetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pmsRandomTypeRB)
                    .addComponent(pmsReorderDamagingMovesCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pokemonMovesetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pmsRandomTotalRB)
                    .addComponent(pmsForceGoodDamagingCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pokemonMovesetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pmsMetronomeOnlyRB)
                    .addComponent(pmsForceGoodDamagingSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        moveDataPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.moveDataPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        mdRandomPowerCB.setText(bundle.getString("RandomizerGUI.mdRandomPowerCB.text")); // NOI18N
        mdRandomPowerCB.setToolTipText(bundle.getString("RandomizerGUI.mdRandomPowerCB.toolTipText")); // NOI18N

        mdRandomAccuracyCB.setText(bundle.getString("RandomizerGUI.mdRandomAccuracyCB.text")); // NOI18N
        mdRandomAccuracyCB.setToolTipText(bundle.getString("RandomizerGUI.mdRandomAccuracyCB.toolTipText")); // NOI18N

        mdRandomPPCB.setText(bundle.getString("RandomizerGUI.mdRandomPPCB.text")); // NOI18N
        mdRandomPPCB.setToolTipText(bundle.getString("RandomizerGUI.mdRandomPPCB.toolTipText")); // NOI18N

        mdRandomTypeCB.setText(bundle.getString("RandomizerGUI.mdRandomTypeCB.text")); // NOI18N
        mdRandomTypeCB.setToolTipText(bundle.getString("RandomizerGUI.mdRandomTypeCB.toolTipText")); // NOI18N

        mdRandomCategoryCB.setText(bundle.getString("RandomizerGUI.mdRandomCategoryCB.text")); // NOI18N
        mdRandomCategoryCB.setToolTipText(bundle.getString("RandomizerGUI.mdRandomCategoryCB.toolTipText")); // NOI18N

        goUpdateMovesCheckBox.setText(bundle.getString("RandomizerGUI.goUpdateMovesCheckBox.text")); // NOI18N
        goUpdateMovesCheckBox.setToolTipText(bundle.getString("RandomizerGUI.goUpdateMovesCheckBox.toolTipText")); // NOI18N
        goUpdateMovesCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goUpdateMovesCheckBoxActionPerformed(evt);
            }
        });

        goUpdateMovesLegacyCheckBox.setText(bundle.getString("RandomizerGUI.goUpdateMovesLegacyCheckBox.text")); // NOI18N
        goUpdateMovesLegacyCheckBox.setToolTipText(bundle.getString("RandomizerGUI.goUpdateMovesLegacyCheckBox.toolTipText")); // NOI18N

        javax.swing.GroupLayout moveDataPanelLayout = new javax.swing.GroupLayout(moveDataPanel);
        moveDataPanel.setLayout(moveDataPanelLayout);
        moveDataPanelLayout.setHorizontalGroup(
            moveDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(moveDataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(moveDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mdRandomPowerCB)
                    .addComponent(mdRandomAccuracyCB)
                    .addComponent(mdRandomPPCB)
                    .addComponent(mdRandomTypeCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 211, Short.MAX_VALUE)
                .addGroup(moveDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mdRandomCategoryCB)
                    .addGroup(moveDataPanelLayout.createSequentialGroup()
                        .addComponent(goUpdateMovesCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(goUpdateMovesLegacyCheckBox)))
                .addGap(190, 190, 190))
        );
        moveDataPanelLayout.setVerticalGroup(
            moveDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(moveDataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(moveDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mdRandomPowerCB)
                    .addComponent(mdRandomCategoryCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mdRandomAccuracyCB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mdRandomPPCB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(moveDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mdRandomTypeCB)
                    .addComponent(goUpdateMovesCheckBox)
                    .addComponent(goUpdateMovesLegacyCheckBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout movesAndSetsPanelLayout = new javax.swing.GroupLayout(movesAndSetsPanel);
        movesAndSetsPanel.setLayout(movesAndSetsPanelLayout);
        movesAndSetsPanelLayout.setHorizontalGroup(
            movesAndSetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(movesAndSetsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(movesAndSetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(moveDataPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pokemonMovesetsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        movesAndSetsPanelLayout.setVerticalGroup(
            movesAndSetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(movesAndSetsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(moveDataPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pokemonMovesetsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(109, Short.MAX_VALUE))
        );

        trainersPokemonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.trainersPokemonPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        trainerPokesButtonGroup.add(tpUnchangedRB);
        tpUnchangedRB.setSelected(true);
        tpUnchangedRB.setText(bundle.getString("RandomizerGUI.tpUnchangedRB.text")); // NOI18N
        tpUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.tpUnchangedRB.toolTipText")); // NOI18N
        tpUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tpUnchangedRBActionPerformed(evt);
            }
        });

        trainerPokesButtonGroup.add(tpRandomRB);
        tpRandomRB.setText(bundle.getString("RandomizerGUI.tpRandomRB.text")); // NOI18N
        tpRandomRB.setToolTipText(bundle.getString("RandomizerGUI.tpRandomRB.toolTipText")); // NOI18N
        tpRandomRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tpRandomRBActionPerformed(evt);
            }
        });

        trainerPokesButtonGroup.add(tpTypeThemedRB);
        tpTypeThemedRB.setText(bundle.getString("RandomizerGUI.tpTypeThemedRB.text")); // NOI18N
        tpTypeThemedRB.setToolTipText(bundle.getString("RandomizerGUI.tpTypeThemedRB.toolTipText")); // NOI18N
        tpTypeThemedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tpTypeThemedRBActionPerformed(evt);
            }
        });

        trainerPokesButtonGroup.add(tpTypeMatchRB);
        tpTypeMatchRB.setText(bundle.getString("RandomizerGUI.tpTypeMatchRB.text")); // NOI18N
        tpTypeMatchRB.setToolTipText(bundle.getString("RandomizerGUI.tpTypeMatchRB.toolTipText")); // NOI18N
        tpTypeMatchRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tpTypeMatchRBActionPerformed(evt);
            }
        });

        tpPowerLevelsCB.setText(bundle.getString("RandomizerGUI.tpPowerLevelsCB.text")); // NOI18N
        tpPowerLevelsCB.setToolTipText(bundle.getString("RandomizerGUI.tpPowerLevelsCB.toolTipText")); // NOI18N
        tpPowerLevelsCB.setEnabled(false);

        tpTypeWeightingCB.setText(bundle.getString("RandomizerGUI.tpTypeWeightingCB.text")); // NOI18N
        tpTypeWeightingCB.setToolTipText(bundle.getString("RandomizerGUI.tpTypeWeightingCB.toolTipText")); // NOI18N
        tpTypeWeightingCB.setEnabled(false);

        tpRivalCarriesStarterCB.setText(bundle.getString("RandomizerGUI.tpRivalCarriesStarterCB.text")); // NOI18N
        tpRivalCarriesStarterCB.setToolTipText(bundle.getString("RandomizerGUI.tpRivalCarriesStarterCB.toolTipText")); // NOI18N
        tpRivalCarriesStarterCB.setEnabled(false);

        tpNoLegendariesCB.setText(bundle.getString("RandomizerGUI.tpNoLegendariesCB.text")); // NOI18N
        tpNoLegendariesCB.setEnabled(false);

        tnRandomizeCB.setText(bundle.getString("RandomizerGUI.tnRandomizeCB.text")); // NOI18N
        tnRandomizeCB.setToolTipText(bundle.getString("RandomizerGUI.tnRandomizeCB.toolTipText")); // NOI18N

        tcnRandomizeCB.setText(bundle.getString("RandomizerGUI.tcnRandomizeCB.text")); // NOI18N
        tcnRandomizeCB.setToolTipText(bundle.getString("RandomizerGUI.tcnRandomizeCB.toolTipText")); // NOI18N

        tpNoEarlyShedinjaCB.setText(bundle.getString("RandomizerGUI.tpNoEarlyShedinjaCB.text")); // NOI18N
        tpNoEarlyShedinjaCB.setToolTipText(bundle.getString("RandomizerGUI.tpNoEarlyShedinjaCB.toolTipText")); // NOI18N

        tpForceFullyEvolvedCB.setText(bundle.getString("RandomizerGUI.tpForceFullyEvolvedCB.text")); // NOI18N
        tpForceFullyEvolvedCB.setToolTipText(bundle.getString("RandomizerGUI.tpForceFullyEvolvedCB.toolTipText")); // NOI18N
        tpForceFullyEvolvedCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tpForceFullyEvolvedCBActionPerformed(evt);
            }
        });

        tpForceFullyEvolvedSlider.setMajorTickSpacing(5);
        tpForceFullyEvolvedSlider.setMaximum(65);
        tpForceFullyEvolvedSlider.setMinimum(30);
        tpForceFullyEvolvedSlider.setMinorTickSpacing(1);
        tpForceFullyEvolvedSlider.setPaintLabels(true);
        tpForceFullyEvolvedSlider.setPaintTicks(true);
        tpForceFullyEvolvedSlider.setToolTipText(bundle.getString("RandomizerGUI.tpForceFullyEvolvedSlider.toolTipText")); // NOI18N
        tpForceFullyEvolvedSlider.setValue(30);

        tpLevelModifierCB.setText(bundle.getString("RandomizerGUI.tpLevelModifierCB.text")); // NOI18N
        tpLevelModifierCB.setToolTipText(bundle.getString("RandomizerGUI.tpLevelModifierCB.toolTipText")); // NOI18N
        tpLevelModifierCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tpLevelModifierCBActionPerformed(evt);
            }
        });

        tpLevelModifierSlider.setMajorTickSpacing(10);
        tpLevelModifierSlider.setMaximum(50);
        tpLevelModifierSlider.setMinimum(-50);
        tpLevelModifierSlider.setMinorTickSpacing(2);
        tpLevelModifierSlider.setPaintLabels(true);
        tpLevelModifierSlider.setPaintTicks(true);
        tpLevelModifierSlider.setToolTipText(bundle.getString("RandomizerGUI.tpLevelModifierSlider.toolTipText")); // NOI18N
        tpLevelModifierSlider.setValue(0);

        javax.swing.GroupLayout trainersPokemonPanelLayout = new javax.swing.GroupLayout(trainersPokemonPanel);
        trainersPokemonPanel.setLayout(trainersPokemonPanelLayout);
        trainersPokemonPanelLayout.setHorizontalGroup(
            trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(trainersPokemonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tpTypeThemedRB)
                    .addComponent(tpTypeMatchRB)
                    .addGroup(trainersPokemonPanelLayout.createSequentialGroup()
                        .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tpUnchangedRB)
                            .addComponent(tpRandomRB))
                        .addGap(47, 47, 47)
                        .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(tpTypeWeightingCB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(tpRivalCarriesStarterCB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(tpPowerLevelsCB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(tpNoLegendariesCB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(tpNoEarlyShedinjaCB)
                            .addComponent(tpRandomFrontier)
                            .addComponent(tpFillBossTeams))
                        .addGap(18, 18, 18)
                        .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tnRandomizeCB)
                            .addComponent(tcnRandomizeCB)
                            .addComponent(tpForceFullyEvolvedCB)
                            .addComponent(tpForceFullyEvolvedSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tpLevelModifierCB)
                            .addComponent(tpLevelModifierSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(143, Short.MAX_VALUE))
        );
        trainersPokemonPanelLayout.setVerticalGroup(
            trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(trainersPokemonPanelLayout.createSequentialGroup()
                .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tpUnchangedRB)
                    .addComponent(tpRivalCarriesStarterCB)
                    .addComponent(tnRandomizeCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tpRandomRB)
                    .addComponent(tpPowerLevelsCB)
                    .addComponent(tcnRandomizeCB))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(tpTypeThemedRB)
                        .addComponent(tpTypeWeightingCB)
                        .addComponent(tpForceFullyEvolvedCB))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tpTypeMatchRB)
                            .addComponent(tpNoLegendariesCB)
                            .addComponent(tpForceFullyEvolvedSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(trainersPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(trainersPokemonPanelLayout.createSequentialGroup()
                        .addComponent(tpNoEarlyShedinjaCB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(tpRandomFrontier)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(tpFillBossTeams)))
                .addComponent(tpLevelModifierCB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tpLevelModifierSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout trainersInnerPanelLayout = new javax.swing.GroupLayout(trainersInnerPanel);
        trainersInnerPanel.setLayout(trainersInnerPanelLayout);
        trainersInnerPanelLayout.setHorizontalGroup(
            trainersInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(trainersInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(trainersPokemonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        trainersInnerPanelLayout.setVerticalGroup(
            trainersInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(trainersInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(trainersPokemonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(180, Short.MAX_VALUE))
        );

        wildPokemonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.wildPokemonPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        wildPokesButtonGroup.add(wpUnchangedRB);
        wpUnchangedRB.setSelected(true);
        wpUnchangedRB.setText(bundle.getString("RandomizerGUI.wpUnchangedRB.text")); // NOI18N
        wpUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.wpUnchangedRB.toolTipText")); // NOI18N
        wpUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpUnchangedRBActionPerformed(evt);
            }
        });

        wildPokesButtonGroup.add(wpRandomRB);
        wpRandomRB.setText(bundle.getString("RandomizerGUI.wpRandomRB.text")); // NOI18N
        wpRandomRB.setToolTipText(bundle.getString("RandomizerGUI.wpRandomRB.toolTipText")); // NOI18N
        wpRandomRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpRandomRBActionPerformed(evt);
            }
        });

        wildPokesButtonGroup.add(wpArea11RB);
        wpArea11RB.setText(bundle.getString("RandomizerGUI.wpArea11RB.text")); // NOI18N
        wpArea11RB.setToolTipText(bundle.getString("RandomizerGUI.wpArea11RB.toolTipText")); // NOI18N
        wpArea11RB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpArea11RBActionPerformed(evt);
            }
        });

        wildPokesButtonGroup.add(wpGlobalRB);
        wpGlobalRB.setText(bundle.getString("RandomizerGUI.wpGlobalRB.text")); // NOI18N
        wpGlobalRB.setToolTipText(bundle.getString("RandomizerGUI.wpGlobalRB.toolTipText")); // NOI18N
        wpGlobalRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpGlobalRBActionPerformed(evt);
            }
        });

        wildPokemonARulePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new EmptyBorder(4,4,4,4), bundle.getString("RandomizerGUI.wildPokemonARulePanel.border.title"))); // NOI18N

        wildPokesARuleButtonGroup.add(wpARNoneRB);
        wpARNoneRB.setSelected(true);
        wpARNoneRB.setText(bundle.getString("RandomizerGUI.wpARNoneRB.text")); // NOI18N
        wpARNoneRB.setToolTipText(bundle.getString("RandomizerGUI.wpARNoneRB.toolTipText")); // NOI18N
        wpARNoneRB.setEnabled(false);
        wpARNoneRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpARNoneRBActionPerformed(evt);
            }
        });

        wildPokesARuleButtonGroup.add(wpARCatchEmAllRB);
        wpARCatchEmAllRB.setText(bundle.getString("RandomizerGUI.wpARCatchEmAllRB.text")); // NOI18N
        wpARCatchEmAllRB.setToolTipText(bundle.getString("RandomizerGUI.wpARCatchEmAllRB.toolTipText")); // NOI18N
        wpARCatchEmAllRB.setEnabled(false);
        wpARCatchEmAllRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpARCatchEmAllRBActionPerformed(evt);
            }
        });

        wildPokesARuleButtonGroup.add(wpARTypeThemedRB);
        wpARTypeThemedRB.setText(bundle.getString("RandomizerGUI.wpARTypeThemedRB.text")); // NOI18N
        wpARTypeThemedRB.setToolTipText(bundle.getString("RandomizerGUI.wpARTypeThemedRB.toolTipText")); // NOI18N
        wpARTypeThemedRB.setEnabled(false);
        wpARTypeThemedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpARTypeThemedRBActionPerformed(evt);
            }
        });

        wildPokesARuleButtonGroup.add(wpARSimilarStrengthRB);
        wpARSimilarStrengthRB.setText(bundle.getString("RandomizerGUI.wpARSimilarStrengthRB.text")); // NOI18N
        wpARSimilarStrengthRB.setToolTipText(bundle.getString("RandomizerGUI.wpARSimilarStrengthRB.toolTipText")); // NOI18N
        wpARSimilarStrengthRB.setEnabled(false);
        wpARSimilarStrengthRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpARSimilarStrengthRBActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout wildPokemonARulePanelLayout = new javax.swing.GroupLayout(wildPokemonARulePanel);
        wildPokemonARulePanel.setLayout(wildPokemonARulePanelLayout);
        wildPokemonARulePanelLayout.setHorizontalGroup(
            wildPokemonARulePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wildPokemonARulePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(wildPokemonARulePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(wildPokemonARulePanelLayout.createSequentialGroup()
                        .addComponent(wpARTypeThemedRB)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(wildPokemonARulePanelLayout.createSequentialGroup()
                        .addGroup(wildPokemonARulePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(wpARSimilarStrengthRB)
                            .addComponent(wpARNoneRB)
                            .addComponent(wpARCatchEmAllRB))
                        .addContainerGap(58, Short.MAX_VALUE))))
        );
        wildPokemonARulePanelLayout.setVerticalGroup(
            wildPokemonARulePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wildPokemonARulePanelLayout.createSequentialGroup()
                .addComponent(wpARNoneRB)
                .addGap(3, 3, 3)
                .addComponent(wpARSimilarStrengthRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(wpARCatchEmAllRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 3, Short.MAX_VALUE)
                .addComponent(wpARTypeThemedRB, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        wpUseTimeCB.setText(bundle.getString("RandomizerGUI.wpUseTimeCB.text")); // NOI18N
        wpUseTimeCB.setToolTipText(bundle.getString("RandomizerGUI.wpUseTimeCB.toolTipText")); // NOI18N

        wpNoLegendariesCB.setText(bundle.getString("RandomizerGUI.wpNoLegendariesCB.text")); // NOI18N

        wpCatchRateCB.setText(bundle.getString("RandomizerGUI.wpCatchRateCB.text")); // NOI18N
        wpCatchRateCB.setToolTipText(bundle.getString("RandomizerGUI.wpCatchRateCB.toolTipText")); // NOI18N
        wpCatchRateCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpCatchRateCBActionPerformed(evt);
            }
        });

        wpHeldItemsCB.setText(bundle.getString("RandomizerGUI.wpHeldItemsCB.text")); // NOI18N
        wpHeldItemsCB.setToolTipText(bundle.getString("RandomizerGUI.wpHeldItemsCB.toolTipText")); // NOI18N
        wpHeldItemsCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wpHeldItemsCBActionPerformed(evt);
            }
        });

        wpHeldItemsBanBadCB.setText(bundle.getString("RandomizerGUI.wpHeldItemsBanBadCB.text")); // NOI18N
        wpHeldItemsBanBadCB.setToolTipText(bundle.getString("RandomizerGUI.wpHeldItemsBanBadCB.toolTipText")); // NOI18N

        wpCatchRateSlider.setMajorTickSpacing(1);
        wpCatchRateSlider.setMaximum(4);
        wpCatchRateSlider.setMinimum(1);
        wpCatchRateSlider.setPaintLabels(true);
        wpCatchRateSlider.setToolTipText(bundle.getString("RandomizerGUI.wpCatchRateSlider.toolTipText")); // NOI18N
        wpCatchRateSlider.setValue(1);

        wpCondenseEncounterSlotsCB.setText(bundle.getString("RandomizerGUI.wpCondenseEncounterSlotsCB.text")); // NOI18N
        wpCondenseEncounterSlotsCB.setToolTipText(bundle.getString("RandomizerGUI.wpCondenseEncounterSlotsCB.toolTipText")); // NOI18N

        javax.swing.GroupLayout wildPokemonPanelLayout = new javax.swing.GroupLayout(wildPokemonPanel);
        wildPokemonPanel.setLayout(wildPokemonPanelLayout);
        wildPokemonPanelLayout.setHorizontalGroup(
            wildPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wildPokemonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(wildPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(wpUnchangedRB)
                    .addComponent(wpRandomRB)
                    .addComponent(wpArea11RB)
                    .addComponent(wpGlobalRB))
                .addGap(18, 18, 18)
                .addComponent(wildPokemonARulePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(wildPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(wpUseTimeCB)
                    .addComponent(wpNoLegendariesCB)
                    .addComponent(wpHeldItemsBanBadCB)
                    .addGroup(wildPokemonPanelLayout.createSequentialGroup()
                        .addGroup(wildPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(wpCatchRateCB, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(wpHeldItemsCB, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wpCatchRateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(wpCondenseEncounterSlotsCB))
                .addContainerGap(13, Short.MAX_VALUE))
        );
        wildPokemonPanelLayout.setVerticalGroup(
            wildPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(wildPokemonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(wildPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(wildPokemonPanelLayout.createSequentialGroup()
                        .addComponent(wpUnchangedRB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wpRandomRB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wpArea11RB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wpGlobalRB))
                    .addGroup(wildPokemonPanelLayout.createSequentialGroup()
                        .addComponent(wpUseTimeCB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wpNoLegendariesCB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(wildPokemonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(wpCatchRateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(wildPokemonPanelLayout.createSequentialGroup()
                                .addComponent(wpCatchRateCB)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(wpHeldItemsCB)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wpHeldItemsBanBadCB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wpCondenseEncounterSlotsCB))
                    .addComponent(wildPokemonARulePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout wildsInnerPanelLayout = new javax.swing.GroupLayout(wildsInnerPanel);
        wildsInnerPanel.setLayout(wildsInnerPanelLayout);
        wildsInnerPanelLayout.setHorizontalGroup(
            wildsInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, wildsInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(wildPokemonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        wildsInnerPanelLayout.setVerticalGroup(
            wildsInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wildsInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(wildPokemonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(229, Short.MAX_VALUE))
        );

        tmhmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.tmhmsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        tmMovesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new EmptyBorder(4,4,4,4), bundle.getString("RandomizerGUI.tmMovesPanel.border.title"))); // NOI18N

        tmMovesButtonGroup.add(tmmUnchangedRB);
        tmmUnchangedRB.setSelected(true);
        tmmUnchangedRB.setText(bundle.getString("RandomizerGUI.tmmUnchangedRB.text")); // NOI18N
        tmmUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.tmmUnchangedRB.toolTipText")); // NOI18N
        tmmUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tmmUnchangedRBActionPerformed(evt);
            }
        });

        tmMovesButtonGroup.add(tmmRandomRB);
        tmmRandomRB.setText(bundle.getString("RandomizerGUI.tmmRandomRB.text")); // NOI18N
        tmmRandomRB.setToolTipText(bundle.getString("RandomizerGUI.tmmRandomRB.toolTipText")); // NOI18N
        tmmRandomRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tmmRandomRBActionPerformed(evt);
            }
        });

        tmKeepFieldMovesCB.setText(bundle.getString("RandomizerGUI.tmKeepFieldMovesCB.text")); // NOI18N
        tmKeepFieldMovesCB.setToolTipText(bundle.getString("RandomizerGUI.tmKeepFieldMovesCB.toolTipText")); // NOI18N

        tmFullHMCompatCB.setText(bundle.getString("RandomizerGUI.tmFullHMCompatCB.text")); // NOI18N
        tmFullHMCompatCB.setToolTipText(bundle.getString("RandomizerGUI.tmFullHMCompatCB.toolTipText")); // NOI18N

        tmForceGoodDamagingCB.setText(bundle.getString("RandomizerGUI.tmForceGoodDamagingCB.text")); // NOI18N
        tmForceGoodDamagingCB.setToolTipText(bundle.getString("RandomizerGUI.tmForceGoodDamagingCB.toolTipText")); // NOI18N
        tmForceGoodDamagingCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tmForceGoodDamagingCBActionPerformed(evt);
            }
        });

        tmForceGoodDamagingSlider.setMajorTickSpacing(20);
        tmForceGoodDamagingSlider.setMinorTickSpacing(5);
        tmForceGoodDamagingSlider.setPaintLabels(true);
        tmForceGoodDamagingSlider.setPaintTicks(true);
        tmForceGoodDamagingSlider.setSnapToTicks(true);
        tmForceGoodDamagingSlider.setToolTipText(bundle.getString("RandomizerGUI.tmForceGoodDamagingSlider.toolTipText")); // NOI18N
        tmForceGoodDamagingSlider.setValue(0);

        javax.swing.GroupLayout tmMovesPanelLayout = new javax.swing.GroupLayout(tmMovesPanel);
        tmMovesPanel.setLayout(tmMovesPanelLayout);
        tmMovesPanelLayout.setHorizontalGroup(
            tmMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tmMovesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tmMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tmmUnchangedRB)
                    .addComponent(tmmRandomRB)
                    .addComponent(tmFullHMCompatCB))
                .addGap(17, 17, 17)
                .addGroup(tmMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tmForceGoodDamagingSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tmForceGoodDamagingCB)
                    .addComponent(tmKeepFieldMovesCB))
                .addContainerGap(52, Short.MAX_VALUE))
        );
        tmMovesPanelLayout.setVerticalGroup(
            tmMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tmMovesPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(tmMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tmmUnchangedRB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(tmMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tmmRandomRB)
                    .addComponent(tmKeepFieldMovesCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(tmMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tmFullHMCompatCB)
                    .addComponent(tmForceGoodDamagingCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tmForceGoodDamagingSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        tmHmCompatPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new EmptyBorder(4,4,4,4), bundle.getString("RandomizerGUI.tmHmCompatPanel.border.title"))); // NOI18N

        tmHmCompatibilityButtonGroup.add(thcUnchangedRB);
        thcUnchangedRB.setSelected(true);
        thcUnchangedRB.setText(bundle.getString("RandomizerGUI.thcUnchangedRB.text")); // NOI18N
        thcUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.thcUnchangedRB.toolTipText")); // NOI18N
        thcUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thcUnchangedRBActionPerformed(evt);
            }
        });

        tmHmCompatibilityButtonGroup.add(thcRandomTypeRB);
        thcRandomTypeRB.setText(bundle.getString("RandomizerGUI.thcRandomTypeRB.text")); // NOI18N
        thcRandomTypeRB.setToolTipText(bundle.getString("RandomizerGUI.thcRandomTypeRB.toolTipText")); // NOI18N
        thcRandomTypeRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thcRandomTypeRBActionPerformed(evt);
            }
        });

        tmHmCompatibilityButtonGroup.add(thcRandomTotalRB);
        thcRandomTotalRB.setText(bundle.getString("RandomizerGUI.thcRandomTotalRB.text")); // NOI18N
        thcRandomTotalRB.setToolTipText(bundle.getString("RandomizerGUI.thcRandomTotalRB.toolTipText")); // NOI18N
        thcRandomTotalRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thcRandomTotalRBActionPerformed(evt);
            }
        });

        tmHmCompatibilityButtonGroup.add(thcFullRB);
        thcFullRB.setText(bundle.getString("RandomizerGUI.thcFullRB.text")); // NOI18N
        thcFullRB.setToolTipText(bundle.getString("RandomizerGUI.thcFullRB.toolTipText")); // NOI18N
        thcFullRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thcFullRBActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout tmHmCompatPanelLayout = new javax.swing.GroupLayout(tmHmCompatPanel);
        tmHmCompatPanel.setLayout(tmHmCompatPanelLayout);
        tmHmCompatPanelLayout.setHorizontalGroup(
            tmHmCompatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tmHmCompatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tmHmCompatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(thcUnchangedRB)
                    .addComponent(thcRandomTypeRB)
                    .addComponent(thcRandomTotalRB)
                    .addComponent(thcFullRB))
                .addContainerGap(79, Short.MAX_VALUE))
        );
        tmHmCompatPanelLayout.setVerticalGroup(
            tmHmCompatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tmHmCompatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(thcUnchangedRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(thcRandomTypeRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(thcRandomTotalRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(thcFullRB)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout tmhmsPanelLayout = new javax.swing.GroupLayout(tmhmsPanel);
        tmhmsPanel.setLayout(tmhmsPanelLayout);
        tmhmsPanelLayout.setHorizontalGroup(
            tmhmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tmhmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tmMovesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(tmHmCompatPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        tmhmsPanelLayout.setVerticalGroup(
            tmhmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tmhmsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(tmhmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(tmMovesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tmHmCompatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        moveTutorsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.moveTutorsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        mtMovesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new EmptyBorder(4,4,4,4), bundle.getString("RandomizerGUI.mtMovesPanel.border.title"))); // NOI18N

        mtMovesButtonGroup.add(mtmUnchangedRB);
        mtmUnchangedRB.setSelected(true);
        mtmUnchangedRB.setText(bundle.getString("RandomizerGUI.mtmUnchangedRB.text")); // NOI18N
        mtmUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.mtmUnchangedRB.toolTipText")); // NOI18N
        mtmUnchangedRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mtmUnchangedRBActionPerformed(evt);
            }
        });

        mtMovesButtonGroup.add(mtmRandomRB);
        mtmRandomRB.setText(bundle.getString("RandomizerGUI.mtmRandomRB.text")); // NOI18N
        mtmRandomRB.setToolTipText(bundle.getString("RandomizerGUI.mtmRandomRB.toolTipText")); // NOI18N
        mtmRandomRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mtmRandomRBActionPerformed(evt);
            }
        });

        mtKeepFieldMovesCB.setText(bundle.getString("RandomizerGUI.mtKeepFieldMovesCB.text")); // NOI18N
        mtKeepFieldMovesCB.setToolTipText(bundle.getString("RandomizerGUI.mtKeepFieldMovesCB.toolTipText")); // NOI18N

        mtForceGoodDamagingCB.setText(bundle.getString("RandomizerGUI.mtForceGoodDamagingCB.text")); // NOI18N
        mtForceGoodDamagingCB.setToolTipText(bundle.getString("RandomizerGUI.mtForceGoodDamagingCB.toolTipText")); // NOI18N
        mtForceGoodDamagingCB.addActionListener(this::mtForceGoodDamagingCBActionPerformed);

        mtForceGoodDamagingSlider.setMajorTickSpacing(20);
        mtForceGoodDamagingSlider.setMinorTickSpacing(5);
        mtForceGoodDamagingSlider.setPaintLabels(true);
        mtForceGoodDamagingSlider.setPaintTicks(true);
        mtForceGoodDamagingSlider.setSnapToTicks(true);
        mtForceGoodDamagingSlider.setToolTipText(bundle.getString("RandomizerGUI.mtForceGoodDamagingSlider.toolTipText")); // NOI18N
        mtForceGoodDamagingSlider.setValue(0);

        javax.swing.GroupLayout mtMovesPanelLayout = new javax.swing.GroupLayout(mtMovesPanel);
        mtMovesPanel.setLayout(mtMovesPanelLayout);
        mtMovesPanelLayout.setHorizontalGroup(
            mtMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mtMovesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mtMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mtmUnchangedRB)
                    .addComponent(mtmRandomRB))
                .addGap(64, 64, 64)
                .addGroup(mtMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mtForceGoodDamagingSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mtForceGoodDamagingCB)
                    .addComponent(mtKeepFieldMovesCB))
                .addContainerGap(52, Short.MAX_VALUE))
        );
        mtMovesPanelLayout.setVerticalGroup(
            mtMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mtMovesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mtMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mtmUnchangedRB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(mtMovesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mtmRandomRB)
                    .addComponent(mtKeepFieldMovesCB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mtForceGoodDamagingCB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mtForceGoodDamagingSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        mtCompatPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new EmptyBorder(4,4,4,4), bundle.getString("RandomizerGUI.mtCompatPanel.border.title"))); // NOI18N

        mtCompatibilityButtonGroup.add(mtcUnchangedRB);
        mtcUnchangedRB.setSelected(true);
        mtcUnchangedRB.setText(bundle.getString("RandomizerGUI.mtcUnchangedRB.text")); // NOI18N
        mtcUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.mtcUnchangedRB.toolTipText")); // NOI18N
        mtcUnchangedRB.addActionListener(this::mtcUnchangedRBActionPerformed);

        mtCompatibilityButtonGroup.add(mtcRandomTypeRB);
        mtcRandomTypeRB.setText(bundle.getString("RandomizerGUI.mtcRandomTypeRB.text")); // NOI18N
        mtcRandomTypeRB.setToolTipText(bundle.getString("RandomizerGUI.mtcRandomTypeRB.toolTipText")); // NOI18N
        mtcRandomTypeRB.addActionListener(this::mtcRandomTypeRBActionPerformed);

        mtCompatibilityButtonGroup.add(mtcRandomTotalRB);
        mtcRandomTotalRB.setText(bundle.getString("RandomizerGUI.mtcRandomTotalRB.text")); // NOI18N
        mtcRandomTotalRB.setToolTipText(bundle.getString("RandomizerGUI.mtcRandomTotalRB.toolTipText")); // NOI18N
        mtcRandomTotalRB.addActionListener(this::mtcRandomTotalRBActionPerformed);

        mtCompatibilityButtonGroup.add(mtcFullRB);
        mtcFullRB.setText(bundle.getString("RandomizerGUI.mtcFullRB.text")); // NOI18N
        mtcFullRB.setToolTipText(bundle.getString("RandomizerGUI.mtcFullRB.toolTipText")); // NOI18N
        mtcFullRB.addActionListener(this::mtcFullRBActionPerformed);

        javax.swing.GroupLayout mtCompatPanelLayout = new javax.swing.GroupLayout(mtCompatPanel);
        mtCompatPanel.setLayout(mtCompatPanelLayout);
        mtCompatPanelLayout.setHorizontalGroup(
            mtCompatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mtCompatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mtCompatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mtcUnchangedRB)
                    .addComponent(mtcRandomTypeRB)
                    .addComponent(mtcRandomTotalRB)
                    .addComponent(mtcFullRB))
                .addContainerGap(79, Short.MAX_VALUE))
        );
        mtCompatPanelLayout.setVerticalGroup(
            mtCompatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mtCompatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mtcUnchangedRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mtcRandomTypeRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mtcRandomTotalRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mtcFullRB)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        mtNoExistLabel.setText(bundle.getString("RandomizerGUI.mtNoExistLabel.text")); // NOI18N

        javax.swing.GroupLayout moveTutorsPanelLayout = new javax.swing.GroupLayout(moveTutorsPanel);
        moveTutorsPanel.setLayout(moveTutorsPanelLayout);
        moveTutorsPanelLayout.setHorizontalGroup(
            moveTutorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(moveTutorsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(moveTutorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(moveTutorsPanelLayout.createSequentialGroup()
                        .addComponent(mtNoExistLabel)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(moveTutorsPanelLayout.createSequentialGroup()
                        .addComponent(mtMovesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                        .addComponent(mtCompatPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(27, 27, 27))))
        );
        moveTutorsPanelLayout.setVerticalGroup(
            moveTutorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(moveTutorsPanelLayout.createSequentialGroup()
                .addComponent(mtNoExistLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(moveTutorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mtCompatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mtMovesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout tmHmTutorPanelLayout = new javax.swing.GroupLayout(tmHmTutorPanel);
        tmHmTutorPanel.setLayout(tmHmTutorPanelLayout);
        tmHmTutorPanelLayout.setHorizontalGroup(
            tmHmTutorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tmHmTutorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tmHmTutorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(moveTutorsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tmhmsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        tmHmTutorPanelLayout.setVerticalGroup(
            tmHmTutorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tmHmTutorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tmhmsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(moveTutorsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fieldItemsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER, bundle.getString("RandomizerGUI.fieldItemsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        fieldItemsButtonGroup.add(fiUnchangedRB);
        fiUnchangedRB.setSelected(true);
        fiUnchangedRB.setText(bundle.getString("RandomizerGUI.fiUnchangedRB.text")); // NOI18N
        fiUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.fiUnchangedRB.toolTipText")); // NOI18N
        fiUnchangedRB.addActionListener(this::fiUnchangedRBActionPerformed);

        fieldItemsButtonGroup.add(fiShuffleRB);
        fiShuffleRB.setText(bundle.getString("RandomizerGUI.fiShuffleRB.text")); // NOI18N
        fiShuffleRB.setToolTipText(bundle.getString("RandomizerGUI.fiShuffleRB.toolTipText")); // NOI18N
        fiShuffleRB.addActionListener(this::fiShuffleRBActionPerformed);

        fieldItemsButtonGroup.add(fiRandomRB);
        fiRandomRB.setText(bundle.getString("RandomizerGUI.fiRandomRB.text")); // NOI18N
        fiRandomRB.setToolTipText(bundle.getString("RandomizerGUI.fiRandomRB.toolTipText")); // NOI18N
        fiRandomRB.addActionListener(this::fiRandomRBActionPerformed);

        fiBanBadCB.setText(bundle.getString("RandomizerGUI.fiBanBadCB.text")); // NOI18N
        fiBanBadCB.setToolTipText(bundle.getString("RandomizerGUI.fiBanBadCB.toolTipText")); // NOI18N

        fiRandomizeGivenItemsCB.setText(bundle.getString("RandomizerGUI.fiRandomizeGivenItemsCB.text"));
        fiRandomizeGivenItemsCB.setToolTipText(bundle.getString("RandomizerGUI.fiRandomizeGivenItemsCB.toolTipText"));

        fiRandomizePickupTablesCB.setText(bundle.getString("RandomizerGUI.fiRandomizePickupTablesCB.text"));
        fiRandomizePickupTablesCB.setToolTipText(bundle.getString("RandomizerGUI.fiRandomizePickupTablesCB.toolTipText"));

        fiRandomizeBerryTreesCB.setText(bundle.getString("RandomizerGUI.fiRandomizeBerryTreesCB.text"));
        fiRandomizeBerryTreesCB.setToolTipText(bundle.getString("RandomizerGUI.fiRandomizeBerryTreesCB.toolTipText"));

        fiRandomizeMartsCB.setText(bundle.getString("RandomizerGUI.fiRandomizeMartsCB.text"));
        fiRandomizeMartsCB.setToolTipText(bundle.getString("RandomizerGUI.fiRandomizeMartsCB.toolTipText"));
        fiRandomizeMartsCB.addActionListener(this::fiRandomMartCBActionPerformed);

        fiAllMartsHaveBallAndRepel.setText(bundle.getString("RandomizerGUI.fiAllMartsHaveBallAndRepel.text"));
        fiAllMartsHaveBallAndRepel.setToolTipText(bundle.getString("RandomizerGUI.fiAllMartsHaveBallAndRepel.toolTipText"));

        fiRandomItemPrices.setText(bundle.getString("RandomizerGUI.fiRandomItemPrices.text"));
        fiRandomItemPrices.setToolTipText(bundle.getString("RandomizerGUI.fiRandomItemPrices.toolTipText"));

        tpRandomFrontier.setText(bundle.getString("RandomizerGUI.tpRandomFrontier.text"));
        tpRandomFrontier.setToolTipText(bundle.getString("RandomizerGUI.tpRandomFrontier.toolTipText"));

        tpFillBossTeams.setText(bundle.getString("RandomizerGUI.tpFillBossTeams.text"));
        tpFillBossTeams.setToolTipText(bundle.getString("RandomizerGUI.tpFillBossTeams.toolTipText"));

        javax.swing.GroupLayout fieldItemsPanelLayout = new javax.swing.GroupLayout(fieldItemsPanel);
        fieldItemsPanel.setLayout(fieldItemsPanelLayout);

        fieldItemsPanelLayout.setHorizontalGroup(
                fieldItemsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(fieldItemsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(fieldItemsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(fiUnchangedRB)
                                        .addComponent(fiShuffleRB)
                                        .addComponent(fiRandomRB))
                                .addGap(18, 18, 18)
                                .addGroup(fieldItemsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(fiRandomizeGivenItemsCB)
                                        .addComponent(fiRandomizePickupTablesCB)
                                        .addComponent(fiRandomizeBerryTreesCB)
                                        .addComponent(fiRandomizeMartsCB)
                                        .addComponent(fiBanBadCB))
                                .addGap(18, 18, 18)
                                .addGroup(fieldItemsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(fiAllMartsHaveBallAndRepel)
                                    .addComponent(fiRandomItemPrices))
                                .addContainerGap(13, Short.MAX_VALUE))
        );
        fieldItemsPanelLayout.setVerticalGroup(
                fieldItemsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(fieldItemsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(fieldItemsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(fieldItemsPanelLayout.createSequentialGroup()
                                                .addComponent(fiUnchangedRB)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fiShuffleRB)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fiRandomRB))
                                        .addGroup(fieldItemsPanelLayout.createSequentialGroup()
                                                .addComponent(fiRandomizeGivenItemsCB)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fiRandomizePickupTablesCB)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fiRandomizeBerryTreesCB)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fiRandomizeMartsCB)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fiBanBadCB))
                                        .addGroup(fieldItemsPanelLayout.createSequentialGroup()
                                                .addComponent(fiAllMartsHaveBallAndRepel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fiRandomItemPrices)))
                                .addContainerGap(11, Short.MAX_VALUE))
        );


        javax.swing.GroupLayout fieldItemsInnerPanelLayout = new javax.swing.GroupLayout(fieldItemsInnerPanel);
        fieldItemsInnerPanel.setLayout(fieldItemsInnerPanelLayout);
        fieldItemsInnerPanelLayout.setHorizontalGroup(
            fieldItemsInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fieldItemsInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldItemsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        fieldItemsInnerPanelLayout.setVerticalGroup(
            fieldItemsInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fieldItemsInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldItemsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(321, Short.MAX_VALUE))
        );



       miscTweaksPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER,
               bundle.getString("RandomizerGUI.miscTweaksPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
               TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        warpsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(DEFAULT_BORDER,
                bundle.getString("RandomizerGUI.warpsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.BELOW_TOP, new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 12))); // NOI18N

        mtNoneAvailableLabel.setText(bundle.getString("RandomizerGUI.mtNoneAvailableLabel.text")); // NOI18N

        javax.swing.GroupLayout miscTweaksPanelLayout = new javax.swing.GroupLayout(miscTweaksPanel);
        miscTweaksPanel.setLayout(miscTweaksPanelLayout);
        miscTweaksPanelLayout.setHorizontalGroup(
            miscTweaksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(miscTweaksPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mtNoneAvailableLabel)
                .addContainerGap(418, Short.MAX_VALUE))
        );
        miscTweaksPanelLayout.setVerticalGroup(
            miscTweaksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(miscTweaksPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mtNoneAvailableLabel)
                .addContainerGap(367, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout miscTweaksInnerPanelLayout = new javax.swing.GroupLayout(miscTweaksInnerPanel);
        miscTweaksInnerPanel.setLayout(miscTweaksInnerPanelLayout);
        miscTweaksInnerPanelLayout.setHorizontalGroup(
            miscTweaksInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(miscTweaksInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(miscTweaksPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        miscTweaksInnerPanelLayout.setVerticalGroup(
            miscTweaksInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(miscTweaksInnerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(miscTweaksPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        warpRandomizerButtonGroup.add(wrUnchangedRB);
        wrUnchangedRB.setSelected(true);
        wrUnchangedRB.setText(bundle.getString("RandomizerGUI.wrUnchangedRB.text")); // NOI18N
        wrUnchangedRB.setToolTipText(bundle.getString("RandomizerGUI.wrUnchangedRB.toolTipText")); // NOI18N

        warpRandomizerButtonGroup.add(wrRandomRB);
        wrRandomRB.setText(bundle.getString("RandomizerGUI.wrRandomRB.text")); // NOI18N
        wrRandomRB.setToolTipText(bundle.getString("RandomizerGUI.wrRandomRB.toolTipText")); // NOI18N

        wrKeepUselessDeadends.setText(bundle.getString("RandomizerGUI.wrAddUselessDeadends.text")); // NOI18N
        wrKeepUselessDeadends.setToolTipText(bundle.getString("RandomizerGUI.wrAddUselessDeadends.toolTipText")); // NOI18N

        wrRemoveGymOrderLogic.setText(bundle.getString("RandomizerGUI.wrAllowOutOfOrderGymLogic.text")); // NOI18N
        wrRemoveGymOrderLogic.setToolTipText(bundle.getString("RandomizerGUI.wrAllowOutOfOrderGymLogic.toolTipText")); // NOI18N

        wrUnchangedRB.addActionListener(this::wrCBActionPerformed);
        wrRandomRB.addActionListener(this::wrCBActionPerformed);

        javax.swing.GroupLayout warpsPanelLayout = new javax.swing.GroupLayout(warpsPanel);
        warpsPanel.setLayout(warpsPanelLayout);

        warpsPanelLayout.setHorizontalGroup(
                warpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(warpsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(warpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(wrUnchangedRB)
                                        .addComponent(wrRandomRB))
                                .addGap(50, 50, 50) // Space between the two columns
                                .addGroup(warpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(wrKeepUselessDeadends)
                                        .addComponent(wrRemoveGymOrderLogic))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        warpsPanelLayout.setVerticalGroup(
                warpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(warpsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(warpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(wrUnchangedRB)
                                        .addComponent(wrKeepUselessDeadends))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(warpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(wrRandomRB)
                                        .addComponent(wrRemoveGymOrderLogic))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout warpsInnerPanelLayout = new javax.swing.GroupLayout(warpsInnerPanel);
        warpsInnerPanel.setLayout(warpsInnerPanelLayout);
        warpsInnerPanelLayout.setHorizontalGroup(
                warpsInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(warpsInnerPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(warpsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        warpsInnerPanelLayout.setVerticalGroup(
                warpsInnerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(warpsInnerPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(warpsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );

        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.baseStatsPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.pokemonTypesPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.evosPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.starterPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.moveDataPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.pokemonMovesetsPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.trainersPokemonPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.wildPokemonPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.staticPokemonPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.tmhmsPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.moveTutorsPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.inGameTradesPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.fieldItemsInnerPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.miscTweaksInnerPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.miscTweaksInnerPanel.TabConstraints.tabTitle"), miscTweaksPanel); // NOI18N
        randomizerOptionsPane.addTab(bundle.getString("RandomizerGUI.warpsInnerPanel.TabConstraints.tabTitle"), new JLabel()); // NOI18N
        randomizerOptionsPane.addChangeListener(this::tabChangeActionPerformed);

        versionLabel.setFont(new java.awt.Font(FlatRobotoFont.FAMILY, Font.BOLD, 16)); // NOI18N
        versionLabel.setText(bundle.getString("RandomizerGUI.versionLabel.text")); // NOI18N
        versionLabel.setBorder(new EmptyBorder(10,10,10,0));

        websiteLinkLabel.setText(bundle.getString("RandomizerGUI.websiteLinkLabel.text")); // NOI18N
        websiteLinkLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        websiteLinkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                websiteLinkLabelMouseClicked(evt);
            }
        });
        websiteLinkLabel.setBorder(new EmptyBorder(10,0,10,0));

        gameMascotLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emptyIcon.png"))); // NOI18N
        gameMascotLabel.setText(bundle.getString("RandomizerGUI.gameMascotLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(generalOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(loadQSButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(saveQSButton))
                            .addComponent(romInfoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(28, 28, 28)
                        .addComponent(gameMascotLabel)
                        .addGap(18, 37, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(openROMButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(saveROMButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(usePresetsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(settingsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(versionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(websiteLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup().addComponent(randomizerOptionsPane)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(generalOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gameMascotLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(openROMButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(saveROMButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(usePresetsButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(settingsButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(romInfoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(loadQSButton)
                            .addComponent(saveQSButton))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(versionLabel)
                    .addComponent(websiteLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(randomizerOptionsPane))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel abilitiesPanel;
    private javax.swing.JPanel baseStatsPanel;
    private javax.swing.JCheckBox brokenMovesCB;
    private javax.swing.JMenuItem customNamesEditorMenuItem;
    private javax.swing.JCheckBox fiBanBadCB;
    private javax.swing.JCheckBox fiRandomizeGivenItemsCB;
    private javax.swing.JCheckBox fiRandomizePickupTablesCB;
    private javax.swing.JCheckBox fiRandomizeBerryTreesCB;
    private javax.swing.JCheckBox fiRandomizeMartsCB;
    private javax.swing.JRadioButton fiRandomRB;
    private javax.swing.JRadioButton fiShuffleRB;
    private javax.swing.JRadioButton fiUnchangedRB;
    private javax.swing.JCheckBox fiAllMartsHaveBallAndRepel;
    private javax.swing.JCheckBox fiRandomItemPrices;
    private javax.swing.JCheckBox tpRandomFrontier;
    private javax.swing.JCheckBox tpFillBossTeams;
    private javax.swing.ButtonGroup fieldItemsButtonGroup;
    private javax.swing.JPanel fieldItemsInnerPanel;
    private javax.swing.JPanel fieldItemsPanel;
    private javax.swing.JLabel gameMascotLabel;
    private javax.swing.JPanel generalOptionsPanel;
    private javax.swing.JCheckBox goCondenseEvosCheckBox;
    private javax.swing.JCheckBox goRemoveTradeEvosCheckBox;
    private javax.swing.JCheckBox goUpdateMovesCheckBox;
    private javax.swing.JCheckBox goUpdateMovesLegacyCheckBox;
    private javax.swing.JRadioButton igtBothRB;
    private javax.swing.JRadioButton igtGivenOnlyRB;
    private javax.swing.JCheckBox igtRandomIVsCB;
    private javax.swing.JCheckBox igtRandomItemCB;
    private javax.swing.JCheckBox igtRandomNicknameCB;
    private javax.swing.JCheckBox igtRandomOTCB;
    private javax.swing.JRadioButton igtUnchangedRB;
    private javax.swing.JPanel inGameTradesPanel;
    private javax.swing.ButtonGroup ingameTradesButtonGroup;
    private javax.swing.JButton loadQSButton;
    private javax.swing.JMenuItem manualUpdateMenuItem;
    private javax.swing.JCheckBox mdRandomAccuracyCB;
    private javax.swing.JCheckBox mdRandomCategoryCB;
    private javax.swing.JCheckBox mdRandomPPCB;
    private javax.swing.JCheckBox mdRandomPowerCB;
    private javax.swing.JCheckBox mdRandomTypeCB;
    private javax.swing.JPanel miscTweaksInnerPanel;
    private javax.swing.JPanel warpsInnerPanel;
    private javax.swing.JPanel miscTweaksPanel;
    private javax.swing.JPanel warpsPanel;
    private javax.swing.JPanel moveDataPanel;
    private javax.swing.JPanel moveTutorsPanel;
    private javax.swing.JPanel movesAndSetsPanel;
    private javax.swing.JPanel mtCompatPanel;
    private javax.swing.ButtonGroup mtCompatibilityButtonGroup;
    private javax.swing.JCheckBox mtForceGoodDamagingCB;
    private javax.swing.JSlider mtForceGoodDamagingSlider;
    private javax.swing.JCheckBox mtKeepFieldMovesCB;
    private javax.swing.ButtonGroup mtMovesButtonGroup;
    private javax.swing.JPanel mtMovesPanel;
    private javax.swing.JLabel mtNoExistLabel;
    private javax.swing.JLabel mtNoneAvailableLabel;
    private javax.swing.JRadioButton mtcFullRB;
    private javax.swing.JRadioButton mtcRandomTotalRB;
    private javax.swing.JRadioButton mtcRandomTypeRB;
    private javax.swing.JRadioButton mtcUnchangedRB;
    private javax.swing.JRadioButton mtmRandomRB;
    private javax.swing.JRadioButton mtmUnchangedRB;
    private javax.swing.JButton openROMButton;
    private javax.swing.JTextField seedInput;
    private javax.swing.JCheckBox paBanNegativeCB;
    private javax.swing.JCheckBox paBanTrappingCB;
    private javax.swing.JLabel paBansLabel;
    private javax.swing.JCheckBox paFollowEvolutionsCB;
    private javax.swing.JRadioButton paRandomizeRB;
    private javax.swing.JRadioButton paUnchangedRB;
    private javax.swing.JCheckBox paWonderGuardCB;
    private javax.swing.JSlider pbsBaseStatRangeSlider;
    private javax.swing.JRadioButton pbsChangesEqualizeRB;
    private javax.swing.JRadioButton pbsChangesRandomBSTPERCRB;
    private javax.swing.JRadioButton pbsChangesRandomBSTRB;
    private javax.swing.JRadioButton pbsChangesRandomRB;
    private javax.swing.JRadioButton pbsChangesShuffleRB;
    private javax.swing.JRadioButton pbsChangesUnchangedRB;
    private javax.swing.JCheckBox pbsDontRandomizeRatioCB;
    private javax.swing.JCheckBox pbsEvosBuffStatsCB;
    private javax.swing.JCheckBox pbsFollowEvolutionsCB;
    private javax.swing.JCheckBox pbsStandardEXPCurvesCB;
    private javax.swing.JCheckBox pbsUpdateStatsCB;
    private javax.swing.JCheckBox peForceChangeCB;
    private javax.swing.JRadioButton peRandomRB;
    private javax.swing.JCheckBox peSameTypeCB;
    private javax.swing.JCheckBox peSimilarStrengthCB;
    private javax.swing.JCheckBox peThreeStagesCB;
    private javax.swing.JRadioButton peUnchangedRB;
    private javax.swing.JCheckBox pms4MovesCB;
    private javax.swing.JCheckBox pmsForceGoodDamagingCB;
    private javax.swing.JSlider pmsForceGoodDamagingSlider;
    private javax.swing.JRadioButton pmsMetronomeOnlyRB;
    private javax.swing.JRadioButton pmsRandomTotalRB;
    private javax.swing.JRadioButton pmsRandomTypeRB;
    private javax.swing.JCheckBox pmsReorderDamagingMovesCB;
    private javax.swing.JRadioButton pmsUnchangedRB;
    private javax.swing.ButtonGroup pokeAbilitiesButtonGroup;
    private javax.swing.ButtonGroup pokeEvolutionsButtonGroup;
    private javax.swing.JButton pokeLimitBtn;
    private javax.swing.JCheckBox pokeLimitCB;
    private javax.swing.ButtonGroup pokeMovesetsButtonGroup;
    private javax.swing.ButtonGroup pokeStatChangesButtonGroup;
    private javax.swing.JPanel pokeTraitsPanel;
    private javax.swing.ButtonGroup pokeTypesButtonGroup;
    private javax.swing.ButtonGroup typeChartButtonGroup;
    private javax.swing.ButtonGroup warpRandomizerButtonGroup;
    private javax.swing.JPanel pokemonEvolutionsPanel;
    private javax.swing.JPanel pokemonMovesetsPanel;
    private javax.swing.JPanel pokemonTypesPanel;
    private javax.swing.JPanel typeChartPanel;
    private javax.swing.JRadioButton ptRandomFollowEvosRB;
    private javax.swing.JRadioButton ptRandomTotalRB;
    private javax.swing.JRadioButton ptUnchangedRB;
    private javax.swing.JRadioButton tcRandomShuffleRowsRB;
    private javax.swing.JRadioButton tcRandomShuffleRB;
    private javax.swing.JRadioButton tcRandomTotalRB;
    private javax.swing.JRadioButton tcUnchangedRB;
    private javax.swing.JRadioButton wrUnchangedRB;
    private javax.swing.JRadioButton wrRandomRB;
    private javax.swing.JCheckBox wrKeepUselessDeadends;
    private javax.swing.JCheckBox wrRemoveGymOrderLogic;
    private javax.swing.JFileChooser qsOpenChooser;
    private javax.swing.JFileChooser qsSaveChooser;
    private javax.swing.JCheckBox raceModeCB;
    private javax.swing.JTabbedPane randomizerOptionsPane;
    private javax.swing.JLabel riRomCodeLabel;
    private javax.swing.JLabel riRomNameLabel;
    private javax.swing.JLabel riRomSupportLabel;
    private javax.swing.JPanel romInfoPanel;
    private javax.swing.JFileChooser romOpenChooser;
    private javax.swing.JFileChooser romSaveChooser;
    private javax.swing.JButton saveQSButton;
    private javax.swing.JButton saveROMButton;
    private javax.swing.JComboBox<String> bulkSaveSelection;
    private javax.swing.JButton settingsButton;
    private javax.swing.JCheckBox spBanLegendaryStartersCB;
    private javax.swing.JComboBox spCustomPoke1Chooser;
    private javax.swing.JComboBox spCustomPoke2Chooser;
    private javax.swing.JComboBox spCustomPoke3Chooser;
    private javax.swing.JRadioButton spCustomRB;
    private javax.swing.JCheckBox spHeldItemsBanBadCB;
    private javax.swing.JCheckBox spHeldItemsCB;
    private javax.swing.JCheckBox spOnlyLegendaryStartersCB;
    private javax.swing.JRadioButton spRandom0EvosRB;
    private javax.swing.JRadioButton spRandom1EvosRB;
    private javax.swing.JRadioButton spRandom2EvosRB;
    private javax.swing.JRadioButton spRandomRB;
    private javax.swing.JRadioButton spUnchangedRB;
    private javax.swing.ButtonGroup starterPokemonButtonGroup;
    private javax.swing.JPanel starterPokemonPanel;
    private javax.swing.JPanel startersInnerPanel;
    private javax.swing.ButtonGroup staticPokemonButtonGroup;
    private javax.swing.JPanel staticPokemonPanel;
    private javax.swing.JRadioButton stpRandomL4LRB;
    private javax.swing.JRadioButton stpRandomTotalRB;
    private javax.swing.JRadioButton stpUnchangedRB;
    private javax.swing.JCheckBox tcnRandomizeCB;
    private javax.swing.JRadioButton thcFullRB;
    private javax.swing.JRadioButton thcRandomTotalRB;
    private javax.swing.JRadioButton thcRandomTypeRB;
    private javax.swing.JRadioButton thcUnchangedRB;
    private javax.swing.JCheckBox tmForceGoodDamagingCB;
    private javax.swing.JSlider tmForceGoodDamagingSlider;
    private javax.swing.JCheckBox tmFullHMCompatCB;
    private javax.swing.JPanel tmHmCompatPanel;
    private javax.swing.ButtonGroup tmHmCompatibilityButtonGroup;
    private javax.swing.JPanel tmHmTutorPanel;
    private javax.swing.JCheckBox tmKeepFieldMovesCB;
    private javax.swing.ButtonGroup tmMovesButtonGroup;
    private javax.swing.JPanel tmMovesPanel;
    private javax.swing.JPanel tmhmsPanel;
    private javax.swing.JRadioButton tmmRandomRB;
    private javax.swing.JRadioButton tmmUnchangedRB;
    private javax.swing.JCheckBox tnRandomizeCB;
    private javax.swing.JMenuItem toggleAutoUpdatesMenuItem;
    private javax.swing.JMenuItem toggleScrollPaneMenuItem;
    private javax.swing.JCheckBox tpForceFullyEvolvedCB;
    private javax.swing.JSlider tpForceFullyEvolvedSlider;
    private javax.swing.JCheckBox tpLevelModifierCB;
    private javax.swing.JSlider tpLevelModifierSlider;
    private javax.swing.JCheckBox tpNoEarlyShedinjaCB;
    private javax.swing.JCheckBox tpNoLegendariesCB;
    private javax.swing.JCheckBox tpPowerLevelsCB;
    private javax.swing.JRadioButton tpRandomRB;
    private javax.swing.JCheckBox tpRivalCarriesStarterCB;
    private javax.swing.JRadioButton tpTypeThemedRB;
    private javax.swing.JRadioButton tpTypeMatchRB;
    private javax.swing.JCheckBox tpTypeWeightingCB;
    private javax.swing.JRadioButton tpUnchangedRB;
    private javax.swing.ButtonGroup trainerPokesButtonGroup;
    private javax.swing.JPanel trainersInnerPanel;
    private javax.swing.JPanel trainersPokemonPanel;
    private javax.swing.JPopupMenu updateSettingsMenu;
    private javax.swing.JButton usePresetsButton;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JLabel websiteLinkLabel;
    private javax.swing.JPanel wildPokemonARulePanel;
    private javax.swing.JPanel wildPokemonPanel;
    private javax.swing.ButtonGroup wildPokesARuleButtonGroup;
    private javax.swing.ButtonGroup wildPokesButtonGroup;
    private javax.swing.JPanel wildsInnerPanel;
    private javax.swing.JRadioButton wpARCatchEmAllRB;
    private javax.swing.JRadioButton wpARNoneRB;
    private javax.swing.JRadioButton wpARSimilarStrengthRB;
    private javax.swing.JRadioButton wpARTypeThemedRB;
    private javax.swing.JRadioButton wpArea11RB;
    private javax.swing.JCheckBox wpCatchRateCB;
    private javax.swing.JSlider wpCatchRateSlider;
    private javax.swing.JCheckBox wpCondenseEncounterSlotsCB;
    private javax.swing.JRadioButton wpGlobalRB;
    private javax.swing.JCheckBox wpHeldItemsBanBadCB;
    private javax.swing.JCheckBox wpHeldItemsCB;
    private javax.swing.JCheckBox wpNoLegendariesCB;
    private javax.swing.JRadioButton wpRandomRB;
    private javax.swing.JRadioButton wpUnchangedRB;
    private javax.swing.JCheckBox wpUseTimeCB;
    private JScrollPane optionsScrollPane;
    // End of variables declaration//GEN-END:variables
    /* @formatter:on */
}
