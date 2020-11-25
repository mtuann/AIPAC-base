/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle-sdk.
 *
 * Dicoogle/dicoogle-sdk is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle-sdk is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.sdk.utils.query;

import pt.ua.dicoogle.sdk.datastructs.SearchResult;

/**
 * This interface models the For Each method used in {@link Query}.
 * 
 * @see Query
 * 
 * @author Tiago Marques Godinho, tmgodinho@ua.pt
 *
 */
public interface ForEachAdapter {

	/**
	 * This method is executed each time an SearchResult is available.
	 * @param rs The return result.
	 */
	public abstract void forEach(SearchResult rs);
	
}
