/**
 * Copyright (C) 2015  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/lucene.
 *
 * Dicoogle/lucene is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/lucene is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package dicoogle.lucene.dicom.abstraction;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luís A. Bastião Silva <bastiao@ua.pt>
 */
public class DicomDocument implements IDoc
{
    
    private List<IDicomField> dicomFields = new ArrayList<>();

    @Override
    public void add(String name, String value) 
    {
        this.getDicomFields().add(new DicomTextField(name, value));
    }

    @Override
    public void add(String name, Float value) {
        this.getDicomFields().add(new DicomNumericField(name, value));
    }

    @Override
    public void add(String name, byte[] value) {
        this.getDicomFields().add(new DicomByteArrField(name, value));
    }
    
    /**
     * @return the dicomFields
     */
    @Override
    public List<IDicomField> getDicomFields() {
        return dicomFields;
    }

    /**
     * @param dicomFields the dicomFields to set
     */
    public void setDicomFields(List<IDicomField> dicomFields) {
        this.dicomFields = dicomFields;
    }


    
}
