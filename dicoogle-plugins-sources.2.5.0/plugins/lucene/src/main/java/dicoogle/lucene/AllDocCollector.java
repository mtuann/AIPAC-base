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
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;

/**
 *
 * A collector that gathers all matching documents and scores into a List
 *
 * This file was based on a sample from Lucene in Action, 2nd Edition
 * Special Thanks to the Michael McCandless (mikemccand - freenode irc)
 *
 * @author Luís A. Bastião Silva <bastiao@ua.pt>
 */
public class AllDocCollector extends Collector
{
    List<ScoreDoc> docs = new ArrayList<>();
    private Scorer scorer;
    private int docBase;

    @Override
    public boolean acceptsDocsOutOfOrder()
    {
        return true;
    }

    @Override
    public void setScorer(Scorer scorer)
    {
        this.scorer = scorer;
    }

    @Override
    public void setNextReader(IndexReader reader, int docBase)
    {
        this.docBase = docBase;
    }
    @Override
    public void collect(int doc) throws IOException
    {
        docs.add(new ScoreDoc(doc+docBase, scorer.score()));
    }

    public void reset()
    {
        docs.clear();
    }

    public List<ScoreDoc> getHits()
    {
        return docs;
    }
}



