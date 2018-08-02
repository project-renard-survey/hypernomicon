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

import static org.hypernomicon.model.records.HDT_RecordType.*;

import java.util.ArrayList;
import java.util.List;

import org.hypernomicon.queryEngines.QueryEngine.QueryType;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

public class QueryTypePopulator extends Populator
{
  @Override public CellValueType getValueType() { return CellValueType.cvtQueryType; }

  //---------------------------------------------------------------------------  

  @Override public List<HyperTableCell> populate(HyperTableRow row, boolean force)
  {
    List<HyperTableCell> choices = new ArrayList<>();
    
    choices.add(new HyperTableCell(QueryType.qtAllRecords.getCode(), "Any records", hdtNone));
    choices.add(new HyperTableCell(QueryType.qtPersons.getCode(), "Person records", hdtPerson));
    choices.add(new HyperTableCell(QueryType.qtInstitutions.getCode(), "Institution records", hdtInstitution));
    choices.add(new HyperTableCell(QueryType.qtWorks.getCode(), "Work records", hdtWork));
    choices.add(new HyperTableCell(QueryType.qtFiles.getCode(), "File records", hdtMiscFile));
    choices.add(new HyperTableCell(QueryType.qtDebates.getCode(), "Problem/debate records", hdtDebate));
    choices.add(new HyperTableCell(QueryType.qtPositions.getCode(), "Position records", hdtPosition));
    choices.add(new HyperTableCell(QueryType.qtArguments.getCode(), "Argument records", hdtArgument));
    choices.add(new HyperTableCell(QueryType.qtNotes.getCode(), "Note records", hdtNote));
    choices.add(new HyperTableCell(QueryType.qtConcepts.getCode(), "Concept records", hdtConcept));
    choices.add(new HyperTableCell(QueryType.qtInvestigations.getCode(), "Investigation records", hdtInvestigation));
    choices.add(new HyperTableCell(QueryType.qtReport.getCode(), "Report", hdtNone));    
    
    return choices;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public HyperTableCell match(HyperTableRow row, HyperTableCell cell)
  {
    if (row == null) row = dummyRow;
    
    List<HyperTableCell> choices = populate(row, false);
    
    for (HyperTableCell choice : choices)
      if (HyperTableCell.getCellID(choice) == HyperTableCell.getCellID(cell))
        return choice;
    
    return null;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

}