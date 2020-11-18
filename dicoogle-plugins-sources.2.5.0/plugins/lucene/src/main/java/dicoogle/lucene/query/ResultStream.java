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

import java.util.HashMap;
import java.util.Iterator;


import org.apache.lucene.search.IndexSearcher;

import dicoogle.lucene.AllDocCollector;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;


/**
 * Implementation of the ResultStream based on the old AllDocCollector.
 * 
 * This implementation is not the most big-data aware as it holds all the identifiers in memory simultaneously. However, this could be beneficial for certain scenarios.
 * 
 * For a more mature implementation @see ShardResultStream
 * 
 * @author Tiago Marques Godinho, tmgodinho@ua.pt
 *
 */
public class ResultStream extends AbstractResultStream{
	
	private final AllDocCollector documentSource;
	
	public ResultStream(IndexSearcher searcher, AllDocCollector documentSource) {
		super(searcher);
		this.documentSource = documentSource;
		this.searcher = searcher;
	}
	
	public ResultStream(IndexSearcher searcher, AllDocCollector documentSource, HashMap<String, Object> xtraFields) {
		super(searcher, xtraFields);
		this.documentSource = documentSource;
		this.searcher = searcher;
		this.xtraFields = xtraFields;
	}

	@Override
	public Iterator<SearchResult> iterator() {
		return new ResultIterator(this, documentSource.getHits().iterator(), xtraFields);
	}

}
