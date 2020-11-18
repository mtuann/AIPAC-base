/**
 * Copyright (C) 2015  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/filestorage.
 *
 * Dicoogle/filestorage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/filestorage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.ieeta.filestorageplugin;

import pt.ua.ieeta.filestorageplugin.utils.MyDICOMInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import metal.utils.fileiterator.FileIterator;

import org.apache.commons.configuration.XMLConfiguration;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.ieeta.filestorageplugin.utils.DicomUtils;

/**
 *
 * @author Tiago Godinho
 * @author Eduardo Pinho
 */
public class FileStoragePlugin implements StorageInterface {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FileStoragePlugin.class);

    // SETTINGS
    private ConfigurationHolder settings;
    private boolean isEnabled = false;
    private String scheme;
    private File rootDir;
    private boolean useRelativePath;

    public FileStoragePlugin() {
        this.scheme = "file"; // new File("").toURI().getScheme() resolves to "file"
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
        XMLConfiguration cnf = xmlSettings.getConfiguration();

        cnf.setThrowExceptionOnMissing(true);

        String rootdir = "/temp";
        try {
            rootdir = cnf.getString("root-dir");
        } catch (NoSuchElementException ex) {
            cnf.setProperty("root-dir", rootdir);
        }
        try {
            this.useRelativePath = cnf.getBoolean("use-relative-path");
        } catch (NoSuchElementException ex) {
            this.useRelativePath = false;
            cnf.setProperty("use-relative-path", false);
        }
        this.rootDir = new File(rootdir);

        this.scheme = "file";
        try {
            this.scheme = cnf.getString("schema");
            logger.warn("Settings property \"schema\" is deprecated. Please use \"scheme\" instead.");
        } catch (NoSuchElementException ex) {
        }
        try {
            this.scheme = cnf.getString("scheme");
        } catch (NoSuchElementException ex) {
            cnf.setProperty("scheme", this.scheme);
        }

        this.isEnabled = cnf.getBoolean("enabled", true);
    }

    @Override
    public String getName() {
        return "file-storage";
    }

    /**
     *
     * @return Returns the scheme of this Storage Plugin example: storage
     */
    @Override
    public String getScheme() {
        return scheme;
    }

    /**
     * Stores a DICOM Object.
     *
     * @param dicomObject DICOM Object to be Stored
     * @param args variable length arguments, currently unused
     * @return The URI of the previously stored object.
     */
    @Override
    public URI store(DicomObject dicomObject, Object... args) {
        if (!isEnabled) {
            return null;
        }

        String extraPath = DicomUtils.getDirectory(dicomObject);
        String relPath = extraPath + "/" + DicomUtils.getBaseName(dicomObject);
        String fileStr = rootDir.getAbsolutePath() + relPath;

        URI fileUri;
        try {
            fileUri = new URI(scheme, fileStr, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }

        logger.debug("Trying to store in: {}", fileUri);

        File file = new File(fileStr);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DicomOutputStream dos = new DicomOutputStream(bos)) {
            logger.debug("Trying to store in: {}", file.getAbsolutePath());

            // TODO: InitFileMetaInformation should be done outside this method
            // dicomObject.initFileMetaInformation(cuid, iuid, tsuid);
            dos.writeDicomFile(dicomObject);

        } catch (IOException ex) {
            logger.error("Failed to store into {}", fileUri, ex);
            // FOOO I COULD NOT WRITE THE FILE.
            return null;
        }

        return (this.useRelativePath)
                ? URI.create(this.scheme + ":" + relPath)
                : fileUri;
    }

    /**
     * Stores a DICOM InputStream
     *
     * @param inStream InputStream to be stored
     * @param args variable length arguments, currently unused
     * @return The URI of the previously stored stream
     * @throws IOException
     */
    @Override
    public URI store(DicomInputStream inStream, Object... args) throws IOException {
        if (!isEnabled) {
            return null;
        }

        DicomInputStream inputStream = inStream;
        DicomObject obj = inputStream.readDicomObject();

        return store(obj);
    }

    /**
     * Retrieves a file from the storage
     *
     * @param uri Object Identifier
     * @return Input Stream wrapping the previously store object
     */
    public InputStream retrieve(URI uri) {
        if (!isEnabled) {
            return null;
        }

        if (!uri.getScheme().equals(scheme)) {
            return null;
        }

        File f = new File(uri.getSchemeSpecificPart());
        if (f.exists()) {
            try {
                BufferedInputStream inStream = new BufferedInputStream(
                        new FileInputStream(f));
                return inStream;
            } catch (IOException ex) {
                logger.error("Failed to retrieve file at {}", uri, ex);
            }
        }
        return null;
    }

    @Override
    public boolean enable() {
        this.isEnabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.isEnabled = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     *
     * @param uri
     */
    @Override
    public void remove(URI uri) {
        if (!isEnabled) {
            return;
        }

        if (!uri.getScheme().equals(scheme)) {
            return;
        }
        File f;
        if (this.useRelativePath) {
            String path = uri.getSchemeSpecificPart();
            f = new File(rootDir, path);
        } else {
            f = new File(uri);
        }
        
        if (f.exists()) {
            f.delete();
        }
    }

    @Override
    public boolean handles(URI location) {

        if (location.getScheme() == null) {
            return true;
        }
        return location.getScheme().equals(scheme);

    }

    @Override
    public Iterable<StorageInputStream> at(URI location, Object... args) {
        return new MyIterable(location);
    }

    private class MyIterable implements Iterable<StorageInputStream> {

        private final URI baseLocation;

        public MyIterable(URI baseLocation) {
            super();
            this.baseLocation = baseLocation;
        }

        @Override
        public Iterator<StorageInputStream> iterator() {
            return createIterator(baseLocation);
        }

    }

    private static class MyIterator implements Iterator<StorageInputStream> {

        private final Iterator<File> it;
        private final File rootDir;

        public MyIterator(Iterator<File> it) {
            this(it, null);
        }

        public MyIterator(Iterator<File> it, File rootDir) {
            super();
            this.it = it;
            this.rootDir = rootDir;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public StorageInputStream next() {
            File f = it.next();
            MyDICOMInputStream stream = new MyDICOMInputStream(f, rootDir);
            return stream;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not allowed");
        }
    }

    public Iterator<StorageInputStream> createIterator(URI baseLocation) {

        if (!handles(baseLocation)) {
            return Collections.emptyIterator();
        }

        File parent;

        if (this.useRelativePath) {
            parent = new File(this.rootDir, baseLocation.getSchemeSpecificPart());
        } else {
            parent = new File(baseLocation.getSchemeSpecificPart());
        }

        Iterator<File> fileIt;
        if (parent.isDirectory()) {
            fileIt = new FileIterator(parent);
        } else {
            List<File> files = new ArrayList<>(1);
            files.add(parent);
            fileIt = files.iterator();
        }

        return new MyIterator(fileIt, useRelativePath ? rootDir : null);
    }
}
