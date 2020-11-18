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
import java.util.Iterator;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.sdk.datastructs.SearchResult;


/**
 * Implementation of the ResultStream based on sharding.
 * 
 * Rather than having all the returned document ids in memory, this class uses a pagination strategy to minimize the amount of memory need for the search operation.
 * The page size is configurable.
 * 
 * @author Tiago Marques Godinho, tmgodinho@ua.pt
 *
 */
public class ShardResultStream extends AbstractResultStream {
    private static final Logger logger = LoggerFactory.getLogger(ShardResultStream.class);
	
	private Query query;
	private int bulkSize;
	
	/**
	 * Constructs the Stream with all the Extra-Fields returned and a page size of 50000.
	 * 
	 * @param searcher The underlying index searcher.
	 * @param query The query string.
	 */
	public ShardResultStream(IndexSearcher searcher, Query query) {
		this(searcher, query, null, 50000);
	}
	
	/**
	 * Constructs the Stream with the specified Extra-Fields returned and a page size of 50000.
	 * 
	 * @param searcher The underlying index searcher.
	 * @param query The query string.
	 * @param xtraFields The extraFields to be returned in the results.
	 */
	public ShardResultStream(IndexSearcher searcher, Query query, HashMap<String, Object> xtraFields) {
		this(searcher, query, xtraFields, 50000);
	}
	
	/**
	 * Constructs the Stream with the specified parameters.
	 * 
	 * @param searcher The underlying index searcher.
	 * @param query The query string.
	 * @param xtraFields The extraFields to be returned in the results.
	 * @param bulkSize The page size of the stream.
	 */
	public ShardResultStream(IndexSearcher searcher, Query query, HashMap<String, Object> xtraFields, int bulkSize) {
		super(searcher, xtraFields);
		this.query = query;
		this.bulkSize = bulkSize;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<SearchResult> iterator() {
		return new ResultIterator(this, new ShardIterator(), xtraFields);
	}
	
	/**
	 * Paginated Iterator.
	 * 
	 * This iterator is responsible for querying the lucene index searcher in a paginated manner.
	 * 
	 * Its usage should completely abstract the usage of the pagination strategy. 
	 * 
	 * @author Tiago Marques Godinho, tmgodinho@ua.pt
	 *
	 */
	private class ShardIterator implements Iterator<ScoreDoc>{
		
		private ScoreDoc[] docs;
		
		private int totalHits;
		private int index;
		private int bulkIndex;
		
		/**
		 * Initializes the iterator.
		 */
		public ShardIterator() {
			this.index = 0;
			this.bulkIndex = 0;
			search();			
		}

		/**
		 * Performs the search operation.
		 */
		private void search() {
			try {
				TopDocs results;
				
				if(index == 0){
					results = searcher.search(query, bulkSize);					
				}else{
					results = searcher.searchAfter(docs[docs.length-1], query, bulkSize);						
				}
								
				this.docs = results.scoreDocs;
				this.totalHits = results.totalHits;
				this.bulkIndex = 0;
				
			} catch (IOException e) {
				logger.error("Failed to perform search", e);
				this.totalHits = 0;
			}
		
		}

		@Override
		public boolean hasNext() {
			return index < totalHits;
		}

		@Override
		public ScoreDoc next() {
			
			if(bulkIndex >= docs.length)
				search();
			
			ScoreDoc doc = docs[bulkIndex];
			
			bulkIndex++;
			index++;
			
			return doc;			
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}
		
	}

}
