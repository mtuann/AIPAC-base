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
package dicoogle.lucene;

import dicoogle.lucene.dicom.abstraction.DicomByteArrField;
import dicoogle.lucene.dicom.abstraction.DicomDocument;
import dicoogle.lucene.dicom.abstraction.DicomLongField;
import dicoogle.lucene.dicom.abstraction.DicomNumericField;
import dicoogle.lucene.dicom.abstraction.DicomTextField;
import dicoogle.lucene.dicom.abstraction.IDicomField;
import dicoogle.lucene.dicom.abstraction.IDoc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.datastructs.IndexReport2;
import pt.ua.dicoogle.sdk.datastructs.Report;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.JointQueryTask;
import pt.ua.dicoogle.sdk.task.ProgressCallable;
import pt.ua.dicoogle.sdk.task.Task;
import pt.ua.dicoogle.sdk.utils.TagValue;
import pt.ua.dicoogle.sdk.utils.TagsStruct;

/**
 * This class implements the indexing strategy of the Lucene Plugin
 *
 *
 * @author psytek
 */
public class LuceneIndexer implements IndexerInterface, PlatformCommunicatorInterface {

    //access to dicoogle platform
    private static DicooglePlatformInterface platform = null;


    private static final Logger log = LoggerFactory.getLogger(LuceneIndexer.class);
	static final String DEFAULT_INDEX_PATH = "./index/";
	static final String INDEX_PATH_DIR_SUFFIX = "indexed";
	static final String INDEX_PATH_COMPRESSED_SUFFIX = "compressed";


    private ConfigurationHolder settings;


    /**
     * where the index files will be located
     */
    private String indexFilePath;

    /**
     * lucene variables which we need to track
     */
    private Directory index;
    private Analyzer analyzer;
    
    private LuceneQuery lQuery = null;

    //private static long totalCommits = 0;

    /*private BufferedWriter bufWriter = null;
    private FileWriter fileWriter = null;
    */
    
    private final List<String> sopInstanceUIDs=Collections.synchronizedList(new ArrayList<String>());

    /**
     * constructs an indexer instance
     */
    public LuceneIndexer() {
        this.indexFilePath = DEFAULT_INDEX_PATH;
        log.info("Created Lucene Indexer Plugin");
        
    }
    
    public final void setIndexPath(String indexPath) {
        this.indexFilePath = indexPath;

        log.debug("LUCENE: indexing at {}", indexFilePath);
        /*try {
            fileWriter = new FileWriter("log.txt", true);
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
        bufWriter = new BufferedWriter(fileWriter);
        try {
            bufWriter.flush();
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }*/

        //TODO:review is this required? I think it removes a lock
        File fileLock = new File(indexFilePath + File.separator + INDEX_PATH_DIR_SUFFIX + "/write.lock");
        if (fileLock.exists()) {
            fileLock.delete();
        }

        try {
            index = FSDirectory.open(new File(indexFilePath + INDEX_PATH_DIR_SUFFIX));
            File f = new File(indexFilePath + File.separator + INDEX_PATH_COMPRESSED_SUFFIX);
            f.mkdirs();
            analyzer = new StandardAnalyzer(Version.LUCENE_30);

            IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_30, analyzer);
            indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            // this will create the index if it does not exist yet
            new IndexWriter(index, indexConfig).close();
            
            
            
        } catch (IOException ex) {
            //ex.printStackTrace();
            log.error("Failed to open index", ex);
        }

        lQuery = new LuceneQuery();
        lQuery.setIndexPath(getLuceneDirectory());

    }

    public Directory getLuceneDirectory() {
        return index;
    }

    @Override
    public Task<Report> index(final StorageInputStream file, Object ... args) {
        
        return new Task<>(
                new ProgressCallable<Report>() {
                	
                    private float progress = 0.0f;

                    @Override
                    public Report call() throws Exception {

                    	log.debug("Started single index task: {}", file.getURI());

                        IndexReport2 r = new IndexReport2();
                        r.started();
                        try
                        {
                            if(handles(file.getURI())){
                                beginTransaction();
                                indexStream(file, r);
                                endTransaction();
                            }
                        }
                        catch (Exception e)
                        {
                            log.error("Error in last commits", e);
                            r.setnErrors(1);
                        }
                        
                        progress = 1.0f;
                        log.info("Finished Single Index Task: {},{}", (Object)this.hashCode(), r);
                        r.finished();
                        return r;
                    }

                    @Override
                    public float getProgress() {
                        return progress;
                    }
                });
    }

	@Override
	public Task<Report> index(final Iterable<StorageInputStream> files, Object ... args) {

		Task<Report> t = new Task<>(new ProgressCallable<Report>() {
			private float progress = 0.0f;

			@Override
			public Report call() throws Exception {

				log.debug("Started Index Task: {}", (Object)this.hashCode());
				IndexReport2 taskReport = new IndexReport2();
				taskReport.started();
				
				try {

					Iterator<StorageInputStream> it = files.iterator();

					// while (it.hasNext() &&
					// !Thread.currentThread().isInterrupted())
					beginTransaction();
					int i = 1;

					while (it.hasNext()) {
						StorageInputStream s = it.next();
                        if(!handles(s.getURI())){continue;}

						log.debug("Started Indexing: {},{},{}", (Object)this.hashCode(), (Object)i, s.getURI());
						try {
							indexStream(s, taskReport);
						} catch (Exception e) {
							log.error("ERROR Indexing: {},{},{}", (Object)this.hashCode(), (Object)i, s.getURI(), e);
							taskReport.addError();
						}
						log.info("Finished Indexing: {},{},{},{}", (Object)this.hashCode(), (Object)i, s.getURI(), taskReport);
						i++;
					}
					endTransaction();
					progress = 1.0f;
					
				} catch (Exception e) {
					log.error("ERROR in Indexing Task", e);
					taskReport.addError();
				}

                taskReport.finished();
                log.info("Finished Index Task: {},{}", (Object)this.hashCode(), taskReport);
				return taskReport;
			}

			@Override
			public float getProgress() {
				return progress;
			}
		});
		return t;
	}
	
	private int transactions=0;
	private IndexWriter writer;

	private int maxRAMBufferSize;

	private boolean enabled;
	
	private synchronized void beginTransaction() throws IOException{		
		if(transactions==0) {
            IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_30, analyzer);
            indexConfig.setRAMBufferSizeMB(maxRAMBufferSize);
            indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			this.writer = new IndexWriter(index, indexConfig);
		}		
		this.transactions++;
	}
	
	private synchronized void endTransaction() throws IOException{
		this.transactions--;
		this.writer.commit();
		if(transactions==0){
			this.writer.close();
			this.writer = null;
		}
	}	
	
	private synchronized void indexStream(StorageInputStream stream, IndexReport2 r) {
		IDoc idoc = IDocFromStream(stream);

		//
		if (idoc != null) {
			Document luceneDoc = luceneDocFromIDoc(idoc);
			// Index file size
			try {
				NumericField fileSize = new NumericField("FileSize",
						Field.Store.YES, true);

				fileSize.setFloatValue(stream.getSize());
				luceneDoc.add(fileSize);
			} catch (Exception e) {
				log.warn("Failed to add file size field to document", e);
			}
			
			try {
				this.writer.addDocument(luceneDoc);
				r.addIndexFile();
			} catch (IOException ex) {
				log.error("Failed to add document to index", ex);
				r.addError();
			}

		} else {
			r.addError();
		}		
	}

    /**
     * Given an IDoc, this will return a proper Lucene Document apt for indexing
     *
     * @param doc
     * @return
     */
    private Document luceneDocFromIDoc(IDoc doc) {
        Document luceneDoc = new Document();

        for (IDicomField f : doc.getDicomFields()) {
            if (f instanceof DicomTextField) {
                DicomTextField _f = (DicomTextField) f;
                Field f2 = new Field(_f.getName(), _f.getValue(), Field.Store.YES, Field.Index.ANALYZED);
                luceneDoc.add(f2);
            } else if (f instanceof DicomByteArrField) {
                DicomByteArrField _f = (DicomByteArrField) f;
                Field f2 = new Field(_f.getName(), _f.getValue(), Field.Store.YES);
                luceneDoc.add(f2);
            } else if (f instanceof DicomNumericField) {
                DicomNumericField _f = (DicomNumericField) f;
                NumericField f2 = new NumericField(_f.getName(), Field.Store.YES, true);
                f2.setFloatValue(_f.getValue());
                luceneDoc.add(f2);
            } else if (f instanceof DicomLongField) {
                DicomLongField _f = (DicomLongField) f;
                NumericField f2 = new NumericField(_f.getName());
                f2.setLongValue(_f.getValue());
                luceneDoc.add(f2);
            }
        }
        return luceneDoc;
    }

    
    
    
    //currently files are re-indexed, removed repetition checks for simplicity
    private  IDoc IDocFromStream(StorageInputStream storage){

        IDoc returnDoc = null;
        
        // Let's do the query directoy in Lucene ! 
        Iterable<SearchResult> it = lQuery.query("uri:\"" + storage.getURI() + "\"");
        if (it==null ){
        	log.error("Could not find the query plugin");
        	return null;
        }
        if (it.iterator().hasNext()) {
            log.info("File already exists, ignoring");
        	//log.severe("File already exists" + storage.getURI().toString());
            //throw new FileAlreadyExistsException(storage.getURI().toString());
        	return null;
        }
        
        TagsStruct tagStruct = TagsStruct.getInstance();

        BufferedInputStream bufferedStream = null;
        DicomInputStream dicomStream = null;
        
        try (InputStream fileStream = storage.getInputStream()) {
            bufferedStream = new BufferedInputStream(fileStream);
            
            // TODO implement indexing content from gzip 

            //if (file.getAbsolutePath().endsWith(".gz")){bufferedStream = new BufferedInputStream(new GZIPInputStream(fileStream));}
            //else{bufferedStream = new BufferedInputStream(fileStream);}

            dicomStream = new DicomInputStream(bufferedStream);
            //dicomStream.setFileSize(file.length());
            dicomStream.setHandler(new StopTagInputHandler(Tag.PixelData));
            DicomObject dicomObject = dicomStream.readDicomObject();
            returnDoc = new DicomDocument();
            returnDoc.add("uri", storage.getURI().toString());
            String SOPInstanceUID = dicomObject.getString(Tag.SOPInstanceUID);
            it = lQuery.query("SOPInstanceUID:" + SOPInstanceUID);
            if (it==null ){
            	log.error("Could not find the query plugin");
            	return null;
            }
            if (it.iterator().hasNext()) {
                //log.severe("File already exists" + storage.getURI().toString());
            	log.warn("SOPInstanceUID already exists: {}", SOPInstanceUID);
                return null;
                //throw new FileAlreadyExistsException(storage.getURI().toString());
            }
            /**
             * Verify if SOPInstanceUID exists If it exists it will verify
             * columns + rows. If it is a big image it will be re-index
             * otherwise it will be skipped.
             */
            /*String SOPInstanceUID = dicomObject.getString(Tag.SOPInstanceUID);
            
             DicomElement e1 = dicomObject.get(Tag.Columns);
             DicomElement e2 = dicomObject.get(Tag.Rows);
             */
            //TODO:Review why not just use this method??
            //e1.getValueAsString(null, i)
            /**
             * How it it works? Actually Lucene 2.4.X doesn't support added
             * hidden fields, so the stretegy it is following: - DIM fields
             * specified on XML are already indexed - Other fields was getted
             * dinamic way (It depends of Settings) - Thumbnails are indexed, if
             * enabled on Settings
             */
            /**
             * *
             *
             * DIM Fields is mandatory -- No need to check on file XML file if
             * it is really enabled
             *
             *
             */
            
            
            
            //TODO POSSIBLE LEAK!!
            if (sopInstanceUIDs.contains(SOPInstanceUID))
            {
            	log.warn("SOPInstanceUID exists in list: {}", SOPInstanceUID);
                return null;
            }
            sopInstanceUIDs.add(SOPInstanceUID);
            String data;
            
            for (TagValue tag : tagStruct.getDIMFields()) {
                data = null;
                DicomElement e = dicomObject.get(tag.getTagNumber());
                /*if (settings.isIndexAnonymous()){
                 data = this.anonymousFilter(dim.get(key).getAlias(), d.getString(key));
                 }
                 else{*/
                if (e != null) {
                    data = getValue(e);
                }
                //}

                /**
                 * If it is null should be passed has a empty string, it's bad
                 * idea pass null to Lucene.
                 */
                if (data == null) {
                    data = "";
                }
                addField(returnDoc, dicomObject.vrOf(tag.getTagNumber()), tag.getAlias(), data);
            }

            String otherToIndex = "";

            if (tagStruct.isModalityEnable(dicomObject.getString(Tag.Modality)) || tagStruct.isIndexAllModalitiesEnabled()) {
                List<String> _list = getRecursiveDicomElement(returnDoc, dicomObject, "", -1, tagStruct.isDeepSearchModalitiesEnabled());
                otherToIndex = _list.get(0);
            }
                        
            returnDoc.add("others", otherToIndex);
            //System.out.print(".");
            //removed thumbnail storage in index
        } catch (Exception ex) {
            //log.severe(ex.getStackTrace().toString());
            log.error("IOError: {}", storage.getURI(), ex);
            //System.out.print("x");
            //ex.printStackTrace();
            return null;
        } finally {
            try {
                if (dicomStream != null) dicomStream.close();
                if (bufferedStream != null) bufferedStream.close();
            } catch (Exception ex) {
            	log.error("IOError: {}", storage.getURI(), ex);
                return null;
            }
        }

        //log.info("indexed:" + returnDoc);
        return returnDoc;
    }

    @Override
    public String getName() {
        return "lucene";
    }

    @Override
    public boolean enable() {
        return true;
    }

    @Override
    public boolean disable() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setSettings(ConfigurationHolder settings) {
    	
    	this.settings = settings;
        //loadDefaults
        
        XMLConfiguration cnf = this.settings.getConfiguration();
        
        cnf.setThrowExceptionOnMissing(true);

        try {
			this.enabled = cnf.getBoolean("indexer.enabled");
		} catch (NoSuchElementException ex) {
			this.enabled = true;
			cnf.setProperty("indexer.enabled", this.enabled);
		}
        
		try {
			this.maxRAMBufferSize = cnf.getInt("indexer.maxRAMBufferSize");
		} catch (NoSuchElementException ex) {
			this.maxRAMBufferSize = 255;
			cnf.setProperty("indexer.maxRAMBufferSize", this.maxRAMBufferSize);
		}
        
        this.setIndexPath(cnf.getString("indexer.path", DEFAULT_INDEX_PATH));
		
		try {
			cnf.save();

		} catch (ConfigurationException ex) {
			log.warn("Failed to save configuration", ex);
		}
		
		log.debug("Loaded lucene plugin configurations");
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }

    public static boolean isBinaryField(VR vr) {
        return vr == VR.SS || vr == VR.US || vr == VR.SL || vr == VR.UL || vr == VR.FL ||vr == VR.FD;
    }

    public static String getValue(DicomElement element) {

        if (!isBinaryField(element.vr())) {
            String value = null;
            Charset utf8charset = Charset.forName("UTF-8");
            Charset iso88591charset = Charset.forName("iso8859-1");
            byte[] values = element.getBytes();
            ByteBuffer inputBuffer = ByteBuffer.wrap(values);

            // decode UTF-8
            CharBuffer data = iso88591charset.decode(inputBuffer);

            // encode ISO-8559-1
            ByteBuffer outputBuffer = utf8charset.encode(data);

            byte[] outputData = outputBuffer.array();
            try {
                value = new String(outputData, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                log.error("ERROR : @TODO", ex);
            }
            /*
             * 
             *  ASCII         "ISO_IR 6"    =>  "UTF-8"
             UTF-8         "ISO_IR 192"  =>  "UTF-8"
             ISO Latin 1   "ISO_IR 100"  =>  "ISO-8859-1"
             ISO Latin 2   "ISO_IR 101"  =>  "ISO-8859-2"
             ISO Latin 3   "ISO_IR 109"  =>  "ISO-8859-3"
             ISO Latin 4   "ISO_IR 110"  =>  "ISO-8859-4"
             ISO Latin 5   "ISO_IR 148"  =>  "ISO-8859-9"
             Cyrillic      "ISO_IR 144"  =>  "ISO-8859-5"
             Arabic        "ISO_IR 127"  =>  "ISO-8859-6"
             Greek         "ISO_IR 126"  =>  "ISO-8859-7"
             Hebrew        "ISO_IR 138"  =>  "ISO-8859-8"
             */
            return value;
        }

        if (element.vr() == VR.FD && element.getBytes().length == 8) {
            double tmpValue = element.getDouble(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.FL && element.getBytes().length == 4) {
            float tmpValue = element.getFloat(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.UL && element.getBytes().length == 4) {
            long tmpValue = element.getInt(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.US && element.getBytes().length == 2) {
            short[] tmpValue = element.getShorts(true);
            return String.valueOf(tmpValue[0]);
        }

        if (element.vr() != VR.US) {
            long tmpValue = byteArrayToInt(element.getBytes());
            return String.valueOf(tmpValue);
        }

        int tmpValue = element.getInt(true);
        return String.valueOf(tmpValue);
    }

    /**
     * Convert the byte array to an int.
     *
     * @param b The byte array
     * @return The integer
     */
    private static long byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    private static long byteArrayToInt(byte[] b, int offset) {
        long value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    private void addField(IDoc docToAdd, VR vr, String tag, String value) {
        if (docToAdd == null) {
            return;
        }
        if (tag == null) {
            return;
        }
        if (value == null) {
            value = "";
        }
        if (vr == VR.IS || vr == VR.DS || vr == VR.US || vr == VR.FL || vr == VR.FD) {
            try {
                Float _v = Float.valueOf(value);
                docToAdd.add(tag, _v);
            } catch (NumberFormatException ex) {
                docToAdd.add(tag, value);
            }
        } else {
            docToAdd.add(tag, value);
        }
    }

    private String convertStringToHex(String str) {
        char[] chars = str.toCharArray();

        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            hex.append(Integer.toHexString((int) chars[i]));
        }
        return hex.toString();
    }

    /**
     * Really I have no ideia what this does...
     *
     * @param _doc
     * @param d
     * @param prefix
     * @param dim
     * @param others
     * @param nItems
     * @return
     */
    private List<String> getRecursiveDicomElement(IDoc _doc, DicomObject d, String prefix, int nItems, boolean deepSearch) {

        String otherToIndex = "";
        String tagList = "";
        List<String> tmp = new ArrayList<>();
        // Hard heuristic just to be sure that application will be not running forever
        if (prefix.length() > 512) {
            tmp.add(otherToIndex);
            tmp.add(tagList);
        }

        Map<String, String> sequences = new HashMap<>();

        //DictionaryAccess dictionaryDicom = DictionaryAccess.getInstance();
        Iterator<DicomElement> it = d.iterator();
        
        TagsStruct tagstruct = TagsStruct.getInstance();
        
        while (it.hasNext()) {
            DicomElement dcm = it.next();
            int tmpTag = dcm.tag();
            TagValue tag = tagstruct.getTagValue(tmpTag);
            
            boolean index = deepSearch || tagstruct.isOtherField(tag);
            
            if(tag != null && index){
            	
                String tagName = tag.getAlias();

                if (dcm.hasItems()) {
                    String prefixAux = prefix + tagName + "_";

                    if (dcm.countItems() > 0) {
                        List<String> list = getRecursiveDicomElement(_doc,
                                dcm.getDicomObject(0), prefixAux,
                                dcm.countItems(), deepSearch);
                        otherToIndex = otherToIndex + " " + list.get(0);
                        tagList = tagList + " " + list.get(1);
                        sequences.put(tagName, list.get(1));
                    }
                } /*
                 * Drop the non-search-valid fields (Pixel data etc)
                 */ else if (dcm.vr() != VR.OB && dcm.vr() != VR.OW && !tagName.equals("?")) {
                    String value;
                    value = getValue(dcm);

                    if (value != null) {
                        tagList = tagList + " " + prefix + tagName;
                        addField(_doc, dcm.vr(), prefix + tagName, value);
                        otherToIndex = otherToIndex + " " + value;
                    }
                }
            	
            }            
        }

        // There is a sequence
        for (String s : sequences.keySet()) {
            addField(_doc, VR.ST, s, sequences.get(s));
        }

        tmp = new ArrayList<>(2);
        tmp.add(otherToIndex);
        tmp.add(tagList);
        return tmp;
    }

    @Override
    public boolean unindex(URI uri) {
        QueryParser parser = new QueryParser(Version.LUCENE_30, "uri", analyzer);
        String s = uri.toString().replace("/", "\\/");
        try {
            Query q = parser.parse(String.format("uri:\"%s\"", s));
            log.debug("Query: {}", q.toString());
            beginTransaction();
            this.writer.deleteDocuments(q);
            endTransaction();
            return true;
        } catch (IOException ex) {
            log.error("Failed to unindex, attempting close", ex);
            if (this.writer != null) {
                try {
                    this.writer.close();
                } catch (IOException ex1) {
                    log.error(ex1.getMessage(), ex1);
                }
                this.writer = null;
            }
            return false;
        } catch (ParseException ex) {
            log.error("Failed to parse query", ex);
            return false;
        }
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        platform = core;
    }

    public Iterable<SearchResult> search(String query) {
        
        HashMap<String, String> extraFields = new HashMap<>();
        ArrayList<SearchResult> resultsArr = new ArrayList<>();
        extraFields.put("SOPInstanceUID", "SOPInstanceUID");
        MyHolder holder = new MyHolder();
        platform.queryAll(holder, query, extraFields);
        Iterable<SearchResult> results = null;
        try {
			results = holder.get();
		} catch (InterruptedException | ExecutionException e) {

			log.error("ERROR : @TODO", e);
		}
        
        if (results == null) {
            return results;
        }
        for (SearchResult r : results) {
            resultsArr.add(r);
        }

        return resultsArr;

    }

    @Override
    public boolean handles(URI path) {
        int indexExt = path.toString().lastIndexOf('.');
        if(indexExt == -1) return true; //a lot of dicom files have no extension
        
        String extension = path.toString().substring(indexExt);
        switch(extension.toLowerCase()){
            case ".jpg":    //these are not indexed
            case ".png":
            case ".gif":
            case ".bmp":
            case ".tiff":
            case ".jpeg":
                return false;
            
            case ".dicom":  //these are
            case ".dcm": return true;
        }
        
        // some DICOM files have no extension
        // the previous behavior was to index everything anyway
        return true;
    }

    class MyHolder extends JointQueryTask {

        @Override
        public void onCompletion() {

        }

        @Override
        public void onReceive(Task<Iterable<SearchResult>> e) {

        }

    }
}
