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

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.Version;

/**
 *
 * Parsing Query Strings
 *
 * Check query: keywords like MyField:Int:[0 TO 200] is allowed
 * MyField:Float:[1 TO 2] is allowed as well.
 * The goal is remove the queries of queryString, and combines it.
 * @author Luís A. Bastião Silva <bastiao@ua.pt>
 */
public class GenericQueryParser extends QueryParser
{
    private List<String> numericTags = null;

    public GenericQueryParser(Version matchVersion, String field, Analyzer a){
        super(matchVersion, field, a);
        numericTags = new ArrayList<>();
    }

    public GenericQueryParser(Version matchVersion, String field, Analyzer a, List<String> fieldsNumeric){
        this(matchVersion, field, a);
        this.numericTags = fieldsNumeric;
    }

    @Override
    public Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException
    {
        TermRangeQuery query = (TermRangeQuery) super.getRangeQuery(field, part1, part2, inclusive);

        if (numericTags.contains(field))
        {
            return NumericRangeQuery.newFloatRange(field,
                    Float.parseFloat(query.getLowerTerm()),
                    Float.parseFloat(query.getUpperTerm()),
                    query.includesLower(),
                    query.includesUpper());
        }
        else
        {
            return query;
        }
        
    }

 /*   public static void main(String [] args)
    {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]*:Float:)+");
        Matcher matcher = pattern.matcher(" PatientName:A* OR  MyString2:Float:[20 TO 10] AND PatientName:A* OR  MyStringS:Float:[20 TO 10]" );
        String str ="  PatientName:A* OR  MyString2:Float:[20 TO 10] AND PatientName:A* OR  MyStringS:Float:[20 TO 10]";
        //System.out.println(str.replace(":Float", ""));

        while (matcher.find())
        {
            System.out.println(matcher.group());
            System.out.println(matcher.start());
            System.out.println(matcher.end());
            String field = matcher.group().split(":")[0];
            System.out.println("Found" + field);
        }
    }*/

}
