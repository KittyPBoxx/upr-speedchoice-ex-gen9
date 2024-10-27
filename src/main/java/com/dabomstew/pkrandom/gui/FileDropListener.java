package com.dabomstew.pkrandom.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FileDropListener implements DropTargetListener {

    private final Consumer<File> action;

    final List<String> extensions;

    public FileDropListener(Consumer<File> action, String... extensions) {
        this.action = action;
        this.extensions = Arrays.asList(extensions);
    }

    @Override
    public void drop(DropTargetDropEvent event) {

        // Accept copy drops
        event.acceptDrop(DnDConstants.ACTION_COPY);

        // Get the transfer which can provide the dropped item data
        Transferable transferable = event.getTransferable();

        // Get the data formats of the dropped item
        DataFlavor[] flavors = transferable.getTransferDataFlavors();

        // Loop through the flavors
        for (DataFlavor flavor : flavors) {

            try {

                // If the drop items are files
                if (flavor.isFlavorJavaFileListType()) {

                    // Get all of the dropped files
                    List<File> files = (List<File>) transferable.getTransferData(flavor);

                    File romFile = files.stream()
                                        .filter(f -> filterForExtensions(f.getName().toLowerCase()))
                                        .findFirst()
                                        .orElse(null);

                    if (romFile != null) {
                        System.out.println("File path is '" + romFile.getPath() + "'.");
                        action.accept(romFile);
                    }
                    else {
                        files.forEach(f -> System.out.println("Unknown extension for '" + f.getPath() + "'."));
                    }

                }

            } catch (Exception e) {
                java.util.logging.Logger.getLogger(RandomizerGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
            }
        }

        // Inform that the drop is complete
        event.dropComplete(true);

    }

    @Override
    public void dragEnter(DropTargetDragEvent event) {
    }

    @Override
    public void dragExit(DropTargetEvent event) {
    }

    @Override
    public void dragOver(DropTargetDragEvent event) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
    }

    private boolean filterForExtensions(String fileName)
    {
        if (extensions.isEmpty())
            return true;

        return extensions.stream().anyMatch(fileName::endsWith);
    }
}
