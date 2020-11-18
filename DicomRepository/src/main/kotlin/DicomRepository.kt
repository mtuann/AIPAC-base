import org.dcm4che3.io.DicomInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream


class DicomRepository {

    // https://mvnrepository.com/artifact/org.dcm4che/dcm4che-all/3.3.8
    // https://github.com/GoogleCloudPlatform/healthcare-dicom-dicomweb-adapter

    companion object {
//        const val PATH_DICOM_FILE = "/media/tuan/DATA/AI-Cardio/dicom_data_20200821_map_bv/train/1.2.40.0.13.0.11.2672.5.2014082744.2125466.20140512102451/1.2.840.113663.1500.1.341642571.3.2.20140512.104835.265____F5E1KM1I"
        const val PATH_DICOM_FILE = "data/F5E1KM1I"
    }



}

fun main() {
    println("TEST READING FILE DICOM ${DicomRepository.PATH_DICOM_FILE}")

//
//    DicomInputStream

    val dicomInputStream = DicomInputStream(File(DicomRepository.PATH_DICOM_FILE))
    val dataSet = dicomInputStream.readDataset(-1, -1);
//    val din = DicomInputStream(BufferedInputStream(FileInputStream("image.dcm")), TransferSyntax.ImplicitVRLittleEndian)
    dicomInputStream.close()
    println(dataSet)

}