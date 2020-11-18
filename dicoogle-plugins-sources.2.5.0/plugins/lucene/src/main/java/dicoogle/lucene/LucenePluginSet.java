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

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.PluginBase;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 *
 * @author psytek
 */
@PluginImplementation
public class LucenePluginSet extends PluginBase{
    
	private static final Logger log = LoggerFactory.getLogger(LucenePluginSet.class);

	private final LuceneIndexer indexer;
	private final LuceneQuery query;
    
    /**
     * instantiates indexers and query interface implementations
     */
    public LucenePluginSet(){
        
    	log.info("Loading lucene plugin set");
        log.debug("Initializing lucene index interface");
        indexer = new LuceneIndexer();
        indexPlugins.add(indexer);
        
        log.debug("Initializing lucene query interface");
        query = new LuceneQuery();
        queryPlugins.add(query);
    }
    
    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        platform = core;
    }
    
    @Override
    public String getName() {return "luceneset";}

	@Override
	public void setSettings(ConfigurationHolder xmlSettings) {
        // TODO if we wish to deprecate setSettings at plugin set level,
        // this update must be done some other way
        this.query.setIndexPath(indexer.getLuceneDirectory());
	}

}
