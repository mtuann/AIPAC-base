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

import java.io.File;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 *
 * @author tiago
 */
public class DicomUtils {

    public static String getDirectory(DicomObject d) {
        String institutionName = d.getString(Tag.InstitutionName);
        String modality = d.getString(Tag.Modality);
        String studyDate = d.getString(Tag.StudyDate);
        String accessionNumber = d.getString(Tag.AccessionNumber);
        String studyInstanceUID = d.getString(Tag.StudyInstanceUID);
        String patientName = d.getString(Tag.PatientName);

        if (institutionName == null || institutionName.equals("")) {
            institutionName = "UN_IN";
        }
        institutionName = institutionName.trim();
        institutionName = institutionName.replace(" ", "");
        institutionName = institutionName.replace(".", "");
        institutionName = institutionName.replace("&", "");


        if (modality == null || modality.equals("")) {
            modality = "UN_MODALITY";
        }

        if (studyDate == null || studyDate.equals("")) {
            studyDate = "UN_DATE";
        } else {
            try {
                String year = studyDate.substring(0, 4);
                String month = studyDate.substring(4, 6);
                String day = studyDate.substring(6, 8);

                studyDate = year + File.separator + month + File.separator + day;

            } catch (Exception e) {
                e.printStackTrace();
                studyDate = "UN_DATE";
            }
        }

        if (accessionNumber == null || accessionNumber.equals("")) {
            patientName = patientName.trim();
            patientName = patientName.replace(" ", "");
            patientName = patientName.replace(".", "");
            patientName = patientName.replace("&", "");

            if (patientName == null || patientName.equals("")) {
                if (studyInstanceUID == null || studyInstanceUID.equals("")) {
                    accessionNumber = "UN_ACC";
                } else {
                    accessionNumber = studyInstanceUID;
                }
            } else {
                accessionNumber = patientName;

            }

        }

        String result = institutionName + File.separator + modality + File.separator + studyDate + File.separator + accessionNumber;

        return result;
    }

    public static String getFullPathCache(String dir, DicomObject d) {
        return dir + File.separator + getBaseName(d);
    }

    public static String getBaseName(DicomObject d) {
        String sopInstanceUID = d.getString(Tag.SOPInstanceUID);
        return sopInstanceUID + ".dcm";
    }
}
