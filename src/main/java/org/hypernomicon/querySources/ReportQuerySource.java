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

package org.hypernomicon.querySources;

import java.util.ArrayList;
import java.util.List;

import org.hypernomicon.model.HyperDB;
import org.hypernomicon.model.records.HDT_Base;
import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.view.wrappers.HyperTableCell;

public abstract class ReportQuerySource implements QuerySource
{
  protected List<HyperTableCell> list = new ArrayList<>();
  private boolean generated = false;
  protected int query;
  protected HyperTableCell op1, op2, op3;
  protected HyperDB db;
  
  public ReportQuerySource(int query, HyperTableCell op1, HyperTableCell op2, HyperTableCell op3) { init(query, op1, op2, op3); }
  public ReportQuerySource(int query, HyperTableCell op1, HyperTableCell op2)                     { init(query, op1, op2, null); }
  public ReportQuerySource(int query, HyperTableCell op1)                                         { init(query, op1, null, null); }
  public ReportQuerySource(int query)                                                             { init(query, null, null, null); }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  private void init(int query, HyperTableCell op1, HyperTableCell op2, HyperTableCell op3)
  {
    this.query = query;
    this.op1 = op1;
    this.op2 = op2;
    this.op3 = op3;
    this.db = HyperDB.db;
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  protected void ensureGenerated()
  {
    if (!generated)
    {
      generate();
      generated = true;
    }   
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  protected abstract void generate();
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  @Override public int count()
  {
    ensureGenerated();
    return list.size();
  }

//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  @Override public HyperTableCell getCell(int ndx)
  {
    ensureGenerated();
    return list.get(ndx);
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  @Override public HDT_Record getRecord(int ndx)
  {
    return null;
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  @Override public boolean containsRecord(HDT_Base record)
  {
    return false;
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 
  
  @Override public QuerySourceType sourceType()
  {
    return QuerySourceType.QST_report;
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 

}