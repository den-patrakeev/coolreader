/*
 * CoolReader for Android
 * Copyright (C) 2012 Vadim Lopatin <coolreader.org@gmail.com>
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.coolreader.plugins;

import org.coolreader.crengine.Utils;

public class OnlineStoreAuthor {
	public String id;
	public String firstName;
	public String lastName;
	public String middleName;
	public String title;
	public String photo;
	public String getPrefix(int size) {
		String prefix = "";
		if (!Utils.empty(lastName)) {
			prefix = lastName.substring(0, size <= lastName.length() ? size : lastName.length());
		}
		while (prefix.length() < size)
			prefix = prefix + " ";
		return prefix.toUpperCase();
	}
	@Override
	public String toString() {
		return "LitresAuthor [id=" + id + ", lastName=" + lastName
				+ ", firstName=" + firstName + ", middleName=" + middleName
				+ ", title=" + title + ", photo=" + photo + "]";
	}
}