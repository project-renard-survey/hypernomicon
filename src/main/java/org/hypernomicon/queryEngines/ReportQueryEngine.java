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

package org.hypernomicon.queryEngines;

import org.hypernomicon.model.records.HDT_Base;
import org.hypernomicon.querySources.QuerySource;
import org.hypernomicon.querySources.ReportQuerySource;
import org.hypernomicon.view.populators.QueryPopulator;
import org.hypernomicon.view.populators.VariablePopulator;
import org.hypernomicon.view.reports.ReportEngine;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

public class ReportQueryEngine extends QueryEngine<HDT_Base>
{
  @Override public QueryType getQueryType() { return QueryType.qtReport; }
  @Override public void queryChange(int query, HyperTableRow row, VariablePopulator vp1, VariablePopulator vp2, VariablePopulator vp3) { }
  @Override public boolean evaluate(HDT_Base record, boolean firstCall, boolean lastCall) { return false; }
  @Override public boolean needsMentionsIndex(int query) { return false; }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public void addQueries(QueryPopulator pop, HyperTableRow row)
  {
    ReportEngine.addQueries(pop, row);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public QuerySource getSource(int query, HyperTableCell op1, HyperTableCell op2, HyperTableCell op3) 
  {
    return new ReportQuerySource(query, op1, op2, op3) { @Override protected void generate() { }};
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

}