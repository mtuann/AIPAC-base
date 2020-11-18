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

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.ScoreDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.sdk.datastructs.SearchResult;

/**
 * The ResultIterator is responsible for gathering the extra fields involved in the query and assemble the Search Results.
 * 
 * The querying in lucene involves the following steps.
 * Search the index --> Document Ids ---> Gather extra fields --> Assemble the Search Result
 * 
 * @author Tiago Marques Godinho, tmgodinho@ua.pt
 *
 */
public class ResultIterator implements Iterator<SearchResult> {
    private static final Logger logger = LoggerFactory.getLogger(ResultIterator.class);
	
    private final AbstractResultStream resultStream;
    
	private Map<String, Object> extraFields;
	private final Iterator<ScoreDoc> documentIterator;
	private SearchResult bufferedResult;
	
	/**
	 * Creates the iterator with all the fields selected.
	 * 
	 * @param resultStream The stream of results performing the search operation.
	 * @param documentIterator The iterator for the document ids returned by the query.
	 */
	public ResultIterator(AbstractResultStream resultStream,
			Iterator<ScoreDoc> documentIterator) {
		super();
		this.resultStream = resultStream;
		this.documentIterator = documentIterator;
	}

	/**
	 * Creates the iterator with all the fields selected.
	 * 
	 * @param resultStream The stream of results performing the search operation.
	 * @param documentIterator The iterator for the document ids returned by the query.
	 * @param extraFields The extra-fields which should be returned.
	 */
	public ResultIterator(AbstractResultStream resultStream,			
			Iterator<ScoreDoc> documentIterator, Map<String, Object> extraFields) {
		super();
		this.resultStream = resultStream;
		this.extraFields = extraFields;
		this.documentIterator = documentIterator;
	}

	@Override
	public boolean hasNext() {
		reloadBuffer();
		return bufferedResult != null;
	}
	
	/**
	 * This iterator uses a pre-fetching strategy, so that the first result from the query is readily available.
	 * This method reloads the buffered result.
	 */
	private void reloadBuffer(){
		
		while(bufferedResult == null && documentIterator.hasNext()){		

			ScoreDoc score = documentIterator.next();		
			
			Document doc = resultStream.getDocument(score.doc);
			if(doc != null){				
				bufferedResult = createSearchResult(doc);				
			}			
		}
		
	}

	@Override
	public SearchResult next() {
		SearchResult r = bufferedResult;
		bufferedResult = null;
		
		reloadBuffer();
		
		return r;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param field The field which value should be extracted.
	 * @return The string formated value of the field.
	 */
	private static String getValue(Fieldable field) {
		if(field == null)
			return null;
		
		String value = null;
		if (field.isBinary()) {
			AbstractField binaryF = (AbstractField) field;
			byte[] temp = binaryF.getBinaryValue();
			byte[] tempb64 = Base64.encodeBase64(temp);
			
			value = new String(tempb64);
		} else {
			value = field.stringValue();
		}				
		return value;
	}
	
	/**
	 * Converts a Lucene Document to a search result.
	 * 
	 * @param doc Document to be converted.
	 * @return The assembled search result
	 */
	private SearchResult createSearchResult(Document doc){
		
		HashMap<String, Object> xtraFields = new HashMap<>();
		
		if(extraFields != null){
			
			for (String field : extraFields.keySet()) {
			
				Fieldable f = doc.getFieldable(field);
				String value = getValue(f);
				if(value != null)
					xtraFields.put(field, value);
			}
			
		}else{
			
			for(Fieldable f : doc.getFields()){
				String value = getValue(f);
				if(value != null)
					xtraFields.put(f.name(), value);
			}
			
		}

		//logger.trace("Retrieved Extra Fields: {}", extraFields.size());
		URI resultUri = null;

		String uriString = doc.get("uri");
		if (uriString == null){
			uriString = "error://invalid_uri";
			logger.warn("Cannot create URI: {}", uriString);
		}
		
		// System.err.println("ResultURI: " +uriString );
		try {
			resultUri = new URI(uriString);
		} catch (Exception ex) {
			logger.warn("Cannot create URI: {}", uriString, ex);
			resultUri = URI.create("error://invalid_uri");
		}
		// System.err.println("uri q:"+resultUri.toString());
		
		return new SearchResult(resultUri, 0.0f, xtraFields);
	}

}
