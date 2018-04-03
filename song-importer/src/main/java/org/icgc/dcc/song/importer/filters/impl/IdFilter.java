/*
 * Copyright (c) 2018. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.icgc.dcc.song.importer.filters.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.icgc.dcc.song.importer.filters.Filter;

import java.util.Set;

@RequiredArgsConstructor
public class IdFilter implements Filter<String> {

  @NonNull private final Set<String> ids;
  private final boolean isGoodIds;

  @Override public boolean isPass(String id) {
    val isContains = ids.contains(id);
    return isGoodIds == isContains;
  }

  public static IdFilter createIdFilter(Set<String> ids, boolean isGoodIds) {
    return new IdFilter(ids, isGoodIds);
  }

}