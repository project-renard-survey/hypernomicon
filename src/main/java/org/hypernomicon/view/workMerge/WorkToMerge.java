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

package org.hypernomicon.view.workMerge;

import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.view.dialogs.WorkDialogController.createAuthorRecordHandler;
import static org.hypernomicon.bib.BibData.BibFieldEnum.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.ctDropDownList;

import java.util.List;

import org.hypernomicon.bib.BibData;
import org.hypernomicon.bib.BibData.BibFieldEnum;
import org.hypernomicon.model.items.Author;
import org.hypernomicon.model.records.HDT_Person;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_WorkType;
import org.hypernomicon.model.relations.ObjectGroup;
import org.hypernomicon.view.dialogs.WorkDialogController;
import org.hypernomicon.view.populators.Populator;
import org.hypernomicon.view.populators.StandardPopulator;
import org.hypernomicon.view.wrappers.HyperCB;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class WorkToMerge
{
  private BibData bibData = null;
  private HyperCB hcbType;
  private HyperTable htAuthors;
  private boolean creatingNewWork;

  public List<ObjectGroup> getAuthorGroups(HDT_Work work) { return htAuthors.getAuthorGroups(work, 0, -1, 2, 3); }
  public HDT_WorkType getWorkType()                       { return hcbType.selectedRecord(); }
  public BibData getBibData()                             { return bibData; }
   
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public WorkToMerge(BibData bibData, RadioButton rbTitle, TextField tfTitle, RadioButton rbType, ComboBox<HyperTableCell> cbType, 
                                      RadioButton rbYear, TextField tfYear, RadioButton rbAuthors, TableView<HyperTableRow> tvAuthors,
                                      boolean creatingNewWork)
  {
    this.bibData = bibData;
    this.creatingNewWork = creatingNewWork;
    
    hcbType = new HyperCB(cbType, ctDropDownList, new StandardPopulator(hdtWorkType), null);
    
    htAuthors = new HyperTable(tvAuthors, 0, true, "");
    
    htAuthors.addCol(hdtPerson, ctDropDownList);
    
    HDT_Work workRecord = bibData.getWork();
    
    htAuthors.addCheckboxColWithUpdateHandler(createAuthorRecordHandler(htAuthors, () -> workRecord));
    
    htAuthors.addCheckboxCol();
    htAuthors.addCheckboxCol();
    
    tfTitle.setText(bibData.getStr(bfTitle));
    if (tfTitle.getText().isEmpty() == false) rbTitle.setSelected(true); 
    
    tfYear.setText(bibData.getStr(bfYear));
    if (tfYear.getText().isEmpty() == false) rbYear.setSelected(true);
    
    if (workRecord != null)
      loadFromWork(workRecord, rbType);
    else
      loadFromBibData(rbType);
    
    if (htAuthors.getDataRowCount() > 0)
      rbAuthors.setSelected(true);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private void loadFromWork(HDT_Work workRecord, RadioButton rbType)
  {    
    if (creatingNewWork)
    {
      if (workRecord.workType.isNotNull())
        hcbType.addEntry(workRecord.workType.getID(), workRecord.workType.get().name(), workRecord.workType.getID());
      else
        hcbType.addEntry(-1, "", -1);
        
      rbType.setSelected(true);
    }
    
    int ndx = 0; for (Author author : workRecord.getAuthors())
    {
      HDT_Person authorRecord = author.getPerson();
      
      if (authorRecord == null)
      {
        Populator pop = htAuthors.getPopulator(0);
        pop.populate(null, false);
        pop.addEntry(null, -1, author.getNameLastFirst());
        htAuthors.setDataItem(0, ndx, -1, author.getNameLastFirst(), hdtPerson);
      }
      else
      {
        htAuthors.setDataItem(0, ndx, authorRecord.getID(), authorRecord.listName(), hdtPerson);
        htAuthors.setCheckboxValue(1, ndx, true);
      }

      htAuthors.setCheckboxValue(2, ndx, author.getIsEditor());
      htAuthors.setCheckboxValue(3, ndx, author.getIsTrans());

      ndx++;
    }
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private void loadFromBibData(RadioButton rbType)
  {   
    HDT_WorkType workType = bibData.getWorkType();
    
    if (workType != null)
    {
      hcbType.addEntry(workType.getID(), workType.name(), workType.getID());
      rbType.setSelected(true);
    }
    
    htAuthors.getPopulator(0).populate(null, false);
    
    WorkDialogController.loadFromBibAuthors(bibData.getAuthors(), htAuthors, false);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public boolean hasField(BibFieldEnum bibFieldEnum)
  {
    if (bibData == null) return false;
    
    return bibData.fieldNotEmpty(bibFieldEnum);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

}