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

package org.hypernomicon.view.reports;

import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.*;
import static org.hypernomicon.view.dialogs.NewPersonDialogController.*;
import static org.hypernomicon.view.tabs.HyperTab.TabEnum.queryTab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.hypernomicon.HyperTask;
import org.hypernomicon.model.Exceptions.TerminateTaskException;
import org.hypernomicon.model.items.Author;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.view.dialogs.NewPersonDialogController;
import org.hypernomicon.view.dialogs.NewPersonDialogController.PersonForDupCheck;
import org.hypernomicon.view.tabs.HyperTab;
import org.hypernomicon.view.tabs.QueriesTabController;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

public class DupAuthorsReportEngine extends ReportEngine
{
  private ArrayList<HyperTableRow> rows = new ArrayList<>();
  private HashMap<HyperTableRow, LinkedHashSet<Author>> rowToMatch = new HashMap<>();
  private HyperTable ht;
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 

  @Override public List<HyperTableRow> getRows()     { return rows; }
  @Override public String getHtml(HyperTableRow row) { return ""; }
  @Override public boolean alwaysShowDescription()   { return false; }

//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 

//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 

  @Override public void generate(HyperTask task, HyperTableCell param1, HyperTableCell param2, HyperTableCell param3) throws TerminateTaskException
  {
    ArrayList<Author> matchedAuthors;
    LinkedHashMap<Author, List<Author>> matchMap = new LinkedHashMap<>();
    LinkedList<PersonForDupCheck> list = createListForDupCheck();
    
    PersonForDupCheck person = list.poll();
    
    int ndx = 0, total = list.size();
    total = total * (total + 1) / 2;
    
    while (list.size() > 0)
    {
      matchedAuthors = new ArrayList<>();
      doDupCheck(person, list, matchedAuthors, task, ndx, total);

      if (matchedAuthors.size() > 0)
        matchMap.put(person.getAuthor(), matchedAuthors);
      
      ndx = ndx + list.size();
      
      person = list.poll();
    }
    
    rows.clear();
    rowToMatch.clear();
    
    for (Entry<Author, List<Author>> entry : matchMap.entrySet())
    {
      Author author = entry.getKey();
      
      for (Author match : entry.getValue())
      {
        ObservableList<HyperTableCell> cells = FXCollections.observableArrayList();
        
        cells.add(new HyperTableCell(-1, "", hdtNone));
        
        if (author.getPerson() == null)
          cells.add(new HyperTableCell(-1, author.getFullName(false), hdtNone));
        else
          cells.add(new HyperTableCell(author.getPerson().getID(), author.getFullName(false), hdtPerson));
        
        cells.add(getWorkCell(author));
        
        if (match.getPerson() == null)
          cells.add(new HyperTableCell(-1, match.getFullName(false), hdtNone));
        else
          cells.add(new HyperTableCell(match.getPerson().getID(), match.getFullName(false), hdtPerson));
        
        cells.add(getWorkCell(match));
        
        HyperTableRow row = new HyperTableRow(cells, ht);
        
        rows.add(row);
        
        LinkedHashSet<Author> pair = new LinkedHashSet<>();
        
        pair.add(author);
        pair.add(match);
        
        rowToMatch.put(row, pair);
      }
    }
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 

  private HyperTableCell getWorkCell(Author author)
  {
    HDT_Work work = author.getWork();
    
    if (work == null) return new HyperTableCell(-1, "", hdtWork);
    
    return new HyperTableCell(work.getID(), work.getCBText(), hdtWork);
  }

//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 

  @Override public HyperTable prepTable(TableView<HyperTableRow> tv)
  {
    this.tv = tv;
    
    addCol("", 100);
    addCol("Name 1", 200);
    addCol("Work 1", 500);
    addCol("Name 2", 200);
    addCol("Work 2", 500);
    
    ht = new HyperTable(tv, -1, false, "");
    
    ht.addCustomActionCol(-1, "Merge", (row, colNdx) ->
    {
      ArrayList<Author> pair = makeArrayList(rowToMatch.get(row));
      
      Author author1 = null, author2 = null;
           
      if ((pair.get(0).getPerson() == null) && (pair.get(1).getPerson() != null))
      {
        author1 = pair.get(1);
        author2 = pair.get(0);
      }
      else
      {
        author1 = pair.get(0);
        author2 = pair.get(1);
      }
           
      NewPersonDialogController npdc = NewPersonDialogController.create(author1.getName(), null, false, author1.getPerson(), 
                                                                        author1, new ArrayList<Author>(Collections.singletonList(author2)));
      
      if (npdc.showModal() == false) return;
      
      QueriesTabController queriesTab = (QueriesTabController) HyperTab.getHyperTab(queryTab);
      queriesTab.btnExecuteClick();
    });
    
    ht.addCol(hdtNone, ctNone);
    ht.addCol(hdtWork, ctNone);
    ht.addCol(hdtNone, ctNone);
    ht.addCol(hdtWork, ctNone);
    
    return ht;
  }
  
//---------------------------------------------------------------------------  
//--------------------------------------------------------------------------- 

}