/*
 * Copyright 2015-2018 Jason Winning
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.hypernomicon.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hypernomicon.util.Util.*;

public class VersionNumber implements Comparable<VersionNumber>
{
  private List<Integer> parts = new ArrayList<>();

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public VersionNumber(int minParts, int... parts)
  {
    for (int part : parts)
      this.parts.add(part);
    
    while (this.parts.size() < minParts)
      this.parts.add(0);
  }
  
  public VersionNumber(int minParts, String str)
  {
    Arrays.asList(str.split("\\.")).forEach(partStr -> parts.add(parseInt(partStr, 0)));
    
    while (this.parts.size() < minParts)
      this.parts.add(0);
  }

  public int numParts() { return parts.size(); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public int getPart(int ndx)
  {
    if (ndx >= parts.size())
      return 0;
    
    return parts.get(ndx);
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String toString()
  {
    String str = "";
    
    for (int part : parts)
      str = (str.length() == 0) ? String.valueOf(part) : str + "." + String.valueOf(part);
      
    return str;
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    
    List<Integer> newList = new ArrayList<>();
    boolean gotNonzero = false;
    
    for (int ndx = parts.size() - 1; ndx >= 0; ndx--)
    {
      if (parts.get(ndx) > 0)
        gotNonzero = true;
      
      if (gotNonzero)
        newList.add(0, parts.get(ndx));
    }
    
    for (int part : newList)
      result = prime * result + part;

    return result;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    VersionNumber other = (VersionNumber) obj;
    
    for (int ndx = 0; ndx < Math.max(parts.size(), other.numParts()); ndx++)
      if (getPart(ndx) != other.getPart(ndx))
        return false;
    
    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public int compareTo(VersionNumber o)
  {
    for (int ndx = 0; ndx < Math.max(parts.size(), o.numParts()); ndx++)
    {
      int cmp = Integer.compare(parts.get(ndx), o.getPart(ndx));
      if (cmp != 0) return cmp;
    }
    
    return 0;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  
}