/*
 * Copyright 2015-2019 Jason Winning
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

package org.hypernomicon.view.tabs;

import java.util.EnumSet;

import org.hypernomicon.model.items.Authors;
import org.hypernomicon.model.records.*;
import org.hypernomicon.model.records.HDT_Position.PositionSource;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_PositionVerdict;
import org.hypernomicon.view.HyperView.TextViewInfo;
import org.hypernomicon.view.dialogs.NewArgDialogController;
import org.hypernomicon.view.populators.RecordByTypePopulator;
import org.hypernomicon.view.populators.RecordTypePopulator;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;
import org.hypernomicon.view.wrappers.RecordListView;
import org.hypernomicon.view.wrappers.HyperTableCell.HyperCellSortMethod;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.Const.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.*;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;

//---------------------------------------------------------------------------

public class PositionTab extends HyperNodeTab<HDT_Position, HDT_Position>
{
  private HyperTable htParents, htArguments, htSubpositions;
  private HDT_Position curPosition;

  @Override public HDT_RecordType getType()         { return hdtPosition; }
  @Override public void enable(boolean enabled)     { ui.tabPositions.getContent().setDisable(enabled == false); }
  @Override public void focusOnSearchKey()          { ctrlr.focusOnSearchKey(); }
  @Override public void findWithinDesc(String text) { ctrlr.hilite(text); }
  @Override public TextViewInfo getMainTextInfo()   { return ctrlr.getMainTextInfo(); }
  @Override public void setRecord(HDT_Position pos) { curPosition = pos; }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean update()
  {
    curPosition.addParentDisplayRecord();

    ctrlr.update(curPosition);

 // Select parent records in ComboBoxes
 // -----------------------------------

    htParents.buildRows(curPosition.largerPositions, (row, otherPos) ->
    {
      row.setCellValue(2, -1, db.getTypeName(hdtPosition), hdtPosition);
      row.setCellValue(3, otherPos, otherPos.listName());
    });

    htParents.buildRows(curPosition.debates, (row, debate) ->
    {
      row.setCellValue(2, -1, db.getTypeName(hdtDebate), hdtDebate);
      row.setCellValue(3, debate, debate.listName());
    });

// Populate arguments
// ------------------

    htArguments.buildRows(curPosition.arguments, (row, argument) ->
    {
      HDT_Work work = null;

      if (argument.works.size() > 0)
      {
        work = argument.works.get(0);
        if (work.authorRecords.size() > 0)
          row.setCellValue(1, work.authorRecords.get(0), work.getShortAuthorsStr(true));
        else
          row.setCellValue(1, work, work.getShortAuthorsStr(true));
      }

      if (work != null)
      {
        row.setCellValue(3, argument, work.getYear(), HyperCellSortMethod.hsmNumeric);
        row.setCellValue(4, work, work.name());
      }
      else
        row.setCellValue(3, argument, "");

      HDT_PositionVerdict verdict = argument.getPosVerdict(curPosition);
      if (verdict != null)
        row.setCellValue(2, argument, verdict.listName());

      row.setCellValue(5, argument, argument.listName());
    });

 // Populate subpositions
 // ---------------------

    htSubpositions.buildRows(curPosition.subPositions, (row, subPos) ->
    {
      row.setCellValue(1, subPos, subPos.getCBText());

      PositionSource ps = subPos.getWorkWithAuthor();
      if (ps != null)
        row.setCellValue(2, ps.author, Authors.getShortAuthorsStr(subPos.getPeople(), true, true));
      else
        row.setCellValue(2, -1, Authors.getShortAuthorsStr(subPos.getPeople(), true, true), hdtPerson);
    });

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected void init(TabEnum tabEnum)
  {
    this.tabEnum = tabEnum;
    ObservableList<TableColumn<HyperTableRow, ?>> cols;

    ctrlr.init(hdtPosition, this);

    cols = ctrlr.tvLeftChildren.getColumns();

    cols.get(2).setText("Title of Work");
    cols.add(2, new TableColumn<HyperTableRow, HyperTableCell>("Year"));
    cols.add(2, new TableColumn<HyperTableRow, HyperTableCell>("Argues Position Is"));
    cols.add(new TableColumn<HyperTableRow, HyperTableCell>("Arg. Name"));

    ctrlr.spChildren.setDividerPositions(0.6);

    cols = ctrlr.tvRightChildren.getColumns();

    cols.add(1, new TableColumn<HyperTableRow, HyperTableCell>("Sub-Position Name"));
    cols.get(2).setText("Person");

    htParents = new HyperTable(ctrlr.tvParents, 3, true, PREF_KEY_HT_POS_PARENTS);

    htParents.addActionCol(ctGoBtn, 3);
    htParents.addActionCol(ctBrowseBtn, 3);

    RecordTypePopulator rtp = new RecordTypePopulator();
    EnumSet<HDT_RecordType> types = EnumSet.noneOf(HDT_RecordType.class);

    types.add(hdtDebate);
    types.add(hdtPosition);

    rtp.setTypes(types);

    htParents.addColAltPopulatorWithUpdateHandler(hdtNone, ctDropDownList, rtp, (row, cellVal, nextColNdx, nextPopulator) ->
    {
      RecordByTypePopulator rbtp = (RecordByTypePopulator)nextPopulator;

      HDT_RecordType parentType = cellVal.getType();
      rbtp.setRecordType(row, parentType);
      rbtp.setChanged(row);
      row.setCellValue(nextColNdx, new HyperTableCell(-1, "", parentType));
    });

    htParents.addColAltPopulator(hdtNone, ctDropDownList, new RecordByTypePopulator());

    htParents.addRemoveMenuItem();
    htParents.addChangeOrderMenuItem(true);

    htArguments = new HyperTable(ctrlr.tvLeftChildren, 3, true, PREF_KEY_HT_POS_ARG);

    htArguments.addActionCol(ctGoNewBtn, 3);
    htArguments.addCol(hdtPerson, ctNone);
    htArguments.addCol(hdtPositionVerdict, ctNone);
    htArguments.addCol(hdtArgument, ctNone);
    htArguments.addCol(hdtWork, ctNone);
    htArguments.addCol(hdtArgument, ctNone);

    htSubpositions = new HyperTable(ctrlr.tvRightChildren, 1, true, PREF_KEY_HT_POS_SUB);

    htSubpositions.addActionCol(ctGoNewBtn, 1);
    htSubpositions.addCol(hdtPosition, ctNone);
    htSubpositions.addCol(hdtPerson, ctNone);

    initArgContextMenu();
    ui.initPositionContextMenu(htSubpositions);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void initArgContextMenu()
  {
    RecordListView.addDefaultMenuItems(htArguments);

    htArguments.addContextMenuItem("Go to work record", HDT_Work.class,
      work -> ui.goToRecord(work, true));

    htArguments.addContextMenuItem("Go to person record", HDT_Person.class,
      person -> ui.goToRecord(person, true));

    htArguments.addContextMenuItem("Go to argument record", HDT_Argument.class,
      arg -> ui.goToRecord(arg, true));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void clear()
  {
    ctrlr.clear();

    htParents.clear();
    htArguments.clear();
    htSubpositions.clear();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean saveToRecord(boolean showMessage)
  {
    if (!ctrlr.save(curPosition, showMessage, this)) return false;

    curPosition.setLargerPositions(htParents.saveToList(3, hdtPosition));
    curPosition.setDebates(htParents.saveToList(3, hdtDebate));

    ui.attachOrphansToRoots();

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void newClick(HDT_RecordType objType, HyperTableRow row)
  {
    if (ui.cantSaveRecord(true)) return;

    switch (objType)
    {
      case hdtPosition :

        HDT_Position newPos = db.createNewBlankRecord(hdtPosition);
        newPos.largerPositions.add(curPosition);
        ui.goToRecord(newPos, false);
        break;

      case hdtArgument :

        newArgumentClick();
        break;

      default:
        break;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void newArgumentClick()
  {
    NewArgDialogController newArgDialog = NewArgDialogController.create("New Argument", curPosition);

    if (newArgDialog.showModal() == false) return;

    HDT_Argument argument = db.createNewBlankRecord(hdtArgument);
    argument.addPosition(curPosition, newArgDialog.hcbPositionVerdict.selectedRecord());

    if      (newArgDialog.rbArgName1.isSelected()) argument.setName(newArgDialog.tfArgName1.getText());
    else if (newArgDialog.rbArgName2.isSelected()) argument.setName(newArgDialog.tfArgName2.getText());
    else if (newArgDialog.rbArgName3.isSelected()) argument.setName(newArgDialog.tfArgName3.getText());
    else if (newArgDialog.rbArgName4.isSelected()) argument.setName(newArgDialog.tfArgName4.getText());
    else if (newArgDialog.rbArgName5.isSelected()) argument.setName(newArgDialog.tfArgName5.getText());
    else if (newArgDialog.rbArgName6.isSelected()) argument.setName(newArgDialog.tfArgName6.getText());
    else if (newArgDialog.rbArgName7.isSelected()) argument.setName(newArgDialog.tfArgName7.getText());
    else                                           argument.setName(newArgDialog.tfArgName8.getText());

    HDT_Work work;

    if (newArgDialog.rbNew.isSelected())
    {
      work = db.createNewBlankRecord(hdtWork);

      work.setName(newArgDialog.tfTitle.getText());
      HDT_Person person = newArgDialog.hcbPerson.selectedRecord();
      if (person != null)
        work.getAuthors().add(person);
    }
    else
      work = newArgDialog.hcbWork.selectedRecord();

    if (work != null)
      argument.works.add(work);

    ui.goToRecord(argument, false);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void setDividerPositions()
  {
    setDividerPosition(ctrlr.spMain, PREF_KEY_POS_TOP_VERT, 0);
    setDividerPosition(ctrlr.spMain, PREF_KEY_POS_BOTTOM_VERT, 1);
    setDividerPosition(ctrlr.spChildren, PREF_KEY_POS_BOTTOM_HORIZ, 0);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void getDividerPositions()
  {
    getDividerPosition(ctrlr.spMain, PREF_KEY_POS_TOP_VERT, 0);
    getDividerPosition(ctrlr.spMain, PREF_KEY_POS_BOTTOM_VERT, 1);
    getDividerPosition(ctrlr.spChildren, PREF_KEY_POS_BOTTOM_HORIZ, 0);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
