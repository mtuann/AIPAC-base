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
package dicoogle.lucene.query;

import java.io.IOException;
import java.util.HashMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.sdk.datastructs.SearchResult;

/**
 * This class models an abstract stream of results.
 * 
 * In big-data scenarios query operations should not be bound to standard collections. 
 * As such, the stream pattern is used to guarantee that a minimal amount of resources is required when performing searches.
 * 
 * @author Tiago Marques Godinho, tmgodinho@ua.pt
 *
 */
public abstract class AbstractResultStream implements Iterable<SearchResult> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractResultStream.class);

	protected IndexSearcher searcher;
	protected HashMap<String, Object> xtraFields;

	/**
	 * Constructs a stream with all extrafields selected.
	 * 
	 * @param searcher The index searcher which will be used to search the index.
	 */
	public AbstractResultStream(IndexSearcher searcher) {
		this(searcher, null);
	}
	
	/**
	 * Constucts a stream with the specified extra fields.
	 * 
	 * @param searcher The index searcher which will be used to search the index.
	 * @param xtraFields The ExtraFields that should be retrieved for each search result.
	 */
	public AbstractResultStream(IndexSearcher searcher,
			HashMap<String, Object> xtraFields) {
		super();
		this.searcher = searcher;
		this.xtraFields = xtraFields;
	}

	/**
	 * Retrieves the document with the given docID from the lucene index.
	 * 
	 * @param docID 
	 * @return The document or null if it fails.
	 */
	public Document getDocument(int docID) {
		try {
			return searcher.doc(docID);
		} catch (IOException e) {
            logger.warn("Failed to retrieve Document {}", (Object)docID, e);
		}
		return null;
	}

}