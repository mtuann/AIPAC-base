/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle-sdk-ext.
 *
 * Dicoogle/dicoogle-sdk-ext is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle-sdk-ext is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.sdk.observables;

import java.util.ArrayList;

/**
 *
 * @author bastiao
 */
public class ListObservableSearch<type> extends ListObservable
{
    
    public ListObservableSearch()
    {
        super();   
    }
    
    private Boolean finish = false;
    private String queryId = "";

    /**
     * @return the finish
     */
    public Boolean isFinish() {
        return finish;
    }

    /**
     * @param finish the finish to set
     */
    public void setFinish(Boolean finish) {
        
        this.finish = finish;
    }
    
    @Override
    public ArrayList getArray()
    {
        return super.getArray();
    }

    /**
     * @return the queryId
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * @param queryId the queryId to set
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
}
