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

package org.hypernomicon.view.populators;

import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;
import static org.hypernomicon.view.populators.Populator.CellValueType.*;

import java.util.List;

import org.hypernomicon.model.records.HDT_RecordType;

//---------------------------------------------------------------------------    

public class ExternalColumnPopulator extends Populator
{
  private HyperTable hT = null;
  private int colNdx = 0;

  @Override public List<HyperTableCell> populate(HyperTableRow row, boolean force) { return hT.getSelByCol(colNdx); }
  @Override public CellValueType getValueType()                                    { return cvtRecord; }
  @Override public HDT_RecordType getRecordType(HyperTableRow row)                 { return hT.getTypeByCol(colNdx); }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------    
  
  public ExternalColumnPopulator(HyperTable hT, int colNdx)
  {
    this.hT = hT;
    this.colNdx = colNdx;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------    

  @Override public HyperTableCell match(HyperTableRow row, HyperTableCell cell)
  {
    return hT.getSelByCol(colNdx).contains(cell) ? cell.clone() : null;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------    
  
}