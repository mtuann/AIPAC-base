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

package pt.ua.ieeta.filestorageplugin.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import pt.ua.dicoogle.sdk.StorageInputStream;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class MyDICOMInputStream implements StorageInputStream {

    private final File file;
    private final URI uri;

    public MyDICOMInputStream(File file) {
        super();
        this.file = file;
        this.uri = file.toURI();
    }

    public MyDICOMInputStream(File file, File rootDir) {
        super();
        this.file = file;
        if (rootDir != null) {
            URI rootUri = rootDir.toURI();
            this.uri = URI.create(rootUri.getScheme() + ":/" + rootUri.relativize(file.toURI()).normalize().getSchemeSpecificPart());
        } else {
            this.uri = file.toURI();
        }
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public long getSize() throws IOException {
        return file.length();
    }

}
