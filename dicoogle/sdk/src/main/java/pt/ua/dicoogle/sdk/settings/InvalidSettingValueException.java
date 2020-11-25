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
package pt.ua.dicoogle.sdk.settings;

/**
 * Handles the exception code to be used on the plugin settings assigning.
 *
 * @author António Novo <antonio.novo@ua.pt>
 */
public class InvalidSettingValueException extends RuntimeException
{
	private String paramName;

	public InvalidSettingValueException(String message, String paramName)
	{
		super(message);

		this.paramName = paramName;
	}

	public InvalidSettingValueException(String message)
	{
		super(message);

		this.paramName = null;
	}

	public String getParamName()
	{
		return paramName;
	}
}
