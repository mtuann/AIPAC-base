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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import dicoogle.lucene.query.ShardResultStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 * Implementation of the Query Plugin.
 * 
 * @author Tiago Marques Godinho, tmgodinho@ua.pt
 * @author Frederico Valente
 * @author Eduardo Pinho
 *
 */
public class LuceneQuery implements QueryInterface{

    private static final Logger logger = LoggerFactory.getLogger(LuceneQuery.class);
	private static final Pattern pattern = Pattern.compile("([a-zA-Z_0-9]*:(Float|Numeric):)+");
	
	private ConfigurationHolder settings;
    private Directory indexDir;
    private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);
    
    private volatile IndexReader reader;
    

    public LuceneQuery() {
    }
    
    protected IndexReader reloadedReader() throws IOException {
        if (this.reader == null) {
            this.reader = IndexReader.open(indexDir, true);
            logger.debug("New Reader: {}", reader);
        } else {
            IndexReader nreader = IndexReader.openIfChanged(reader, true);
            if(nreader != null) {
                this.reader = nreader;
                logger.debug("New Reader: {}", reader);
            }
        }
        return this.reader;
    }
    
    protected IndexReader reader() throws IOException {
        if (this.reader == null) {
            this.reader = IndexReader.open(indexDir, true);
            logger.debug("Reader: {}", reader);
        }
        return this.reader;
    }
    
    public void setIndexPath(Directory index) {
        this.indexDir = index;
        this.reader = null;
    }
    
	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	public Iterable<SearchResult> query(String query, Object... parameters) {
		long time = System.currentTimeMillis();
		
        if (this.indexDir == null) {
            logger.warn("Query was attempted before settings were initialized");
            return Collections.EMPTY_LIST;
        }
        
		//Check for changes in the reader;
        try {
    	    this.reader = reloadedReader();
        } catch (IOException ex) {
            logger.warn("Trying to open index file", ex);
            return Collections.emptyList();
        }
	    
		IndexSearcher searcher = new IndexSearcher(reader);
		
		Matcher matcher = pattern.matcher(query);
		ArrayList<String> fieldsNumeric = new ArrayList<>();
		while (matcher.find()) {
			String field = matcher.group().split(":")[0];
			fieldsNumeric.add(field);
		}

		query = query.replace("Float:", "");
		query = query.replace("Numeric:", "");

		GenericQueryParser parser = new GenericQueryParser(Version.LUCENE_30, "FileName",
				analyzer, fieldsNumeric);
		parser.setAllowLeadingWildcard(true);
		AllDocCollector collector = new AllDocCollector();
		
		Query queryObject;
		try {
			queryObject = parser.parse(query);
		} catch (ParseException e) {
			logger.error("Error parsing query", e);
			return Collections.emptyList();
		}
		
		HashMap<String, Object> extrafields = null;
		if (parameters.length > 0)
			extrafields = (HashMap<String, Object>) parameters[0];		
				
		ShardResultStream rs = new ShardResultStream(searcher, queryObject, extrafields);
		
		time = System.currentTimeMillis() - time;
		logger.info("Finished opening result stream, Query: {},{},{}",
                (Object)collector.getHits().size(), (Object)time, query);
		
		return rs;
	}

    @Override
    public String getName() {return "lucene";}

    @Override
    public boolean enable() {return true;}

    @Override
    public boolean disable() {return false;}

    @Override
    public boolean isEnabled() {return true;}

    @Override
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
    }

    @Override
    public ConfigurationHolder getSettings() 
    {
        return settings;
    }
}
