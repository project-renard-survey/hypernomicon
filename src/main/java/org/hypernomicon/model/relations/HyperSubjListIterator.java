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

package org.hypernomicon.model.relations;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.hypernomicon.model.records.HDT_Base;
import static org.hypernomicon.model.relations.HyperSubjList.*;

public class HyperSubjListIterator<HDT_SubjType extends HDT_Base, HDT_ObjType extends HDT_Base> implements ListIterator<HDT_SubjType>
{
  private HyperSubjList<HDT_SubjType, HDT_ObjType> list;
  private int nextNdx;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HyperSubjListIterator(HyperSubjList<HDT_SubjType, HDT_ObjType> list, int startNdx)
  {
    this.list = list;
    nextNdx = startNdx;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean hasNext()     { return nextNdx < list.size(); }
  @Override public boolean hasPrevious() { return nextNdx > 0; }
  @Override public int nextIndex()       { return nextNdx; }
  @Override public int previousIndex()   { return nextNdx - 1; }
  
  @Override public void remove()            { throw new UnsupportedOperationException(modErrMsg); }
  @Override public void set(HDT_SubjType e) { throw new UnsupportedOperationException(modErrMsg); }
  @Override public void add(HDT_SubjType e) { throw new UnsupportedOperationException(modErrMsg); }


//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public HDT_SubjType next()
  {   
    if (hasNext())
    {  
      HDT_SubjType record = list.get(nextNdx);
      nextNdx++;
      return record;
    }
    
    throw new NoSuchElementException();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public HDT_SubjType previous()
  {
    if (hasPrevious())
    {  
      nextNdx--;
      HDT_SubjType record = list.get(nextNdx);
      
      return record;
    }
    
    throw new NoSuchElementException();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}