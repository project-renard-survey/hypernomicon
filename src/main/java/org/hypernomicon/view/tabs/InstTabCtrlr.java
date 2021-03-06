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

import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.HDT_Institution;
import org.hypernomicon.model.records.HDT_Person;
import org.hypernomicon.model.records.HDT_RecordType;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_Country;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_State;
import org.hypernomicon.view.populators.StandardPopulator;
import org.hypernomicon.view.wrappers.HyperCB;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.Const.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

//---------------------------------------------------------------------------

public class InstTabCtrlr extends HyperTab<HDT_Institution, HDT_Institution>
{
  private boolean alreadyChangingLocation;
  private HyperTable htSubInst, htPersons;
  private HyperCB hcbState, hcbCountry, hcbType, hcbParentInst;
  private HDT_Institution curInst;

  @FXML private TextField tfCity, tfName;
  @FXML private Button btnLink, btnParent;
  @FXML private TextField tfLink;
  @FXML private ComboBox<HyperTableCell> cbType, cbParentInst, cbState, cbCountry;
  @FXML private TableView<HyperTableRow> tvSubInstitutions, tvPersons;
  @FXML private Hyperlink hlGoogleMaps;
  @FXML private SplitPane spHoriz;

  @Override public String getRecordName()              { return tfName.getText(); }
  @Override HDT_RecordType getType()                   { return hdtInstitution; }
  @Override public void enable(boolean enabled)        { ui.tabInst.getContent().setDisable(enabled == false); }
  @Override void focusOnSearchKey()                    { return; }
  @Override public void setRecord(HDT_Institution rec) { curInst = rec; }
  @Override public void setDividerPositions()          { setDividerPosition(spHoriz, PREF_KEY_INST_MID_HORIZ, 0); }
  @Override public void getDividerPositions()          { getDividerPosition(spHoriz, PREF_KEY_INST_MID_HORIZ, 0); }
  @Override public void findWithinDesc(String text)    { messageDialog("Internal error #52009", mtError); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean update()
  {
    HashMap<HDT_Person, HashSet<HDT_Institution>> peopleMap = new HashMap<>();

    tfName.setText(curInst.name());
    tfCity.setText(curInst.getCity());
    tfLink.setText(curInst.getWebLink());

    hcbState     .addAndSelectEntryOrBlank(curInst.state     , HDT_Record::name);
    hcbCountry   .addAndSelectEntryOrBlank(curInst.country   , HDT_Record::name);
    hcbType      .addAndSelectEntryOrBlank(curInst.instType  , HDT_Record::name);
    hcbParentInst.addAndSelectEntryOrBlank(curInst.parentInst, HDT_Record::name);

 // Populate departments and people
 // -------------------------------

    addPersonsFromInst(curInst, curInst, peopleMap);

    htSubInst.buildRows(curInst.subInstitutions, (row, subInst) ->
    {
      row.setCellValue(1, subInst, subInst.name());
      if (subInst.instType.isNotNull())
        row.setCellValue(2, subInst.instType.get(), subInst.instType.get().name());
      row.setCellValue(3, subInst, subInst.getWebLink());
    });

    if (curInst.isDeptOrFaculty() && curInst.parentInst.isNotNull())
    {
      curInst.parentInst.get().subInstitutions.forEach(sibInst ->
      {
        if (sibInst != curInst)
          addSiblingInsts(sibInst, sibInst, peopleMap);
      });
    }

    htPersons.buildRows(peopleMap.keySet(), (row, person) ->
    {
      row.setCellValue(0, person, person.listName());
      row.setCellValue(1, person.rank.getID(), person.rank.isNotNull() ? person.rank.get().name() : "", hdtRank);
      row.setCellValue(2, person.field.getID(), person.field.isNotNull() ? person.field.get().name() : "", hdtField);

      ArrayList<HDT_Institution> instList = new ArrayList<>(peopleMap.get(person));
      instList.sort((inst1, inst2) -> inst1.name().compareTo(inst2.name()));

      String instStr = instList.stream().map(HDT_Institution::name).reduce((name1, name2) -> name1 + ", " + name2).orElse("");
      int instID = instList.size() == 0 ? -1 : instList.get(0).getID();

      row.setCellValue(3, instID, instStr, hdtInstitution);
    });

    htPersons.getTV().getSortOrder().setAll(List.of(htPersons.getTV().getColumns().get(0)));

    safeFocus(tfName);

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void addSiblingInsts(HDT_Institution sibInst, HDT_Institution cousinInst, HashMap<HDT_Person, HashSet<HDT_Institution>> peopleMap)
  {
    cousinInst.subInstitutions.forEach(subInst -> addSiblingInsts(sibInst, subInst, peopleMap));

    cousinInst.persons.forEach(person ->
    {
      if (peopleMap.containsKey(person))
        peopleMap.get(person).add(sibInst);
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void addPersonsFromInst(HDT_Institution nearestChildInst, HDT_Institution inst, HashMap<HDT_Person, HashSet<HDT_Institution>> peopleMap)
  {
    if (nearestChildInst == curInst)
      inst.subInstitutions.forEach(subInst -> addPersonsFromInst(subInst, subInst, peopleMap));
    else
      inst.subInstitutions.forEach(subInst -> addPersonsFromInst(nearestChildInst, subInst, peopleMap));

    inst.persons.forEach(person ->
    {
      if (peopleMap.containsKey(person) == false)
        peopleMap.put(person, new HashSet<HDT_Institution>());

      peopleMap.get(person).add(nearestChildInst);
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override void init()
  {
    alreadyChangingLocation = false;

    htSubInst = new HyperTable(tvSubInstitutions, 1, true, PREF_KEY_HT_INST_SUB);

    htSubInst.addActionColWithButtonHandler(ctLinkBtn, 3, (row, colNdx) ->
    {
      String link = row.getText(colNdx);

      if (link.length() == 0)
        searchGoogle(tfName.getText() + " " + row.getText(1), true);
      else
        openWebLink(row.getText(colNdx));
    });

    htSubInst.addTextEditCol(hdtInstitution, true, false);
    htSubInst.addCol(hdtInstitutionType, ctDropDownList);
    htSubInst.addTextEditCol(hdtInstitution, true, false);

    htSubInst.addContextMenuItem("Go to this record", HDT_Institution.class, inst -> ui.goToRecord(inst, true));

    htSubInst.addContextMenuItem("Delete this institution record", HDT_Institution.class, inst ->
    {
      if (ui.cantSaveRecord(true)) return;
      if (confirmDialog("Are you sure you want to delete this record?") == false) return;
      db.deleteRecord(hdtInstitution, inst.getID());
      ui.update();
    });

    htSubInst.addChangeOrderMenuItem(true, () -> curInst.subInstitutions.reorder(htSubInst.saveToList(1, hdtInstitution), true));

    htPersons = new HyperTable(tvPersons, 0, false, PREF_KEY_HT_INST_PEOPLE);

    htPersons.addCol(hdtPerson, ctNone);
    htPersons.addCol(hdtRank, ctNone);
    htPersons.addCol(hdtField, ctNone);
    htPersons.addCol(hdtInstitution, ctNone);

    hcbState = new HyperCB(cbState, ctDropDownList, new StandardPopulator(hdtState), null);
    hcbCountry = new HyperCB(cbCountry, ctDropDownList, new StandardPopulator(hdtCountry), null);
    hcbType = new HyperCB(cbType, ctDropDownList, new StandardPopulator(hdtInstitutionType), null);
    hcbParentInst = new HyperCB(cbParentInst, ctDropDownList, new StandardPopulator(hdtInstitution), null);

    cbState.valueProperty().addListener((ob, oldValue, newValue) ->
    {
      if (alreadyChangingLocation) return;

      if (HyperTableCell.getCellID(newValue) > 0)
      {
        alreadyChangingLocation = true;
        hcbCountry.selectID(1);
        alreadyChangingLocation = false;
      }
    });

    cbCountry.valueProperty().addListener((ob, oldValue, newValue) ->
    {
      if (alreadyChangingLocation) return;

      if (HyperTableCell.getCellID(newValue) > 1)
      {
        alreadyChangingLocation = true;
        hcbState.selectID(-1);
        alreadyChangingLocation = false;
      }
    });

    btnParent.setOnAction(event -> ui.goToRecord(HyperTableCell.getRecord(hcbParentInst.selectedHTC()), true));

    btnLink.setOnAction(event -> openWebLink(tfLink.getText()));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @FXML private void linkClick()
  {
    String link = "https://maps.google.com/maps?q=" + tfName.getText() + ",+" +
                  tfCity.getText() + ",+" + hcbState.getText() + ",+" +
                  hcbCountry.getText() + "&hl=en";

    openWebLink(link.replace(' ', '+'));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void clear()
  {
    tfName.clear();
    tfCity.clear();
    tfLink.clear();

    hcbState.clear();
    hcbCountry.clear();
    hcbParentInst.clear();
    hcbType.clear();

    htSubInst.clear();
    htPersons.clear();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean saveToRecord(boolean showMessage)
  {
    if (hcbType.selectedID() < 1)
    {
      if (showMessage)
        messageDialog("You must select a type.", mtError);

      safeFocus(cbType);
      return false;
    }

    boolean locationChanged = false;

    if (tfCity.getText().trim().length() > 0)
      if (curInst.getCity().trim().equalsIgnoreCase(tfCity.getText().trim()) == false)
        locationChanged = true;

    if (hcbState.selectedRecord() != null)
      if (curInst.state.get() != hcbState.selectedRecord())
        locationChanged = true;

    if (hcbCountry.selectedRecord() != null)
      if (curInst.country.get() != hcbCountry.selectedRecord())
        locationChanged = true;

    curInst.setCity(tfCity.getText());
    curInst.setName(tfName.getText());
    curInst.setWebLink(tfLink.getText());
    curInst.state.setID(hcbState.selectedID());
    curInst.country.setID(hcbCountry.selectedID());
    curInst.instType.setID(hcbType.selectedID());
    curInst.parentInst.setID(hcbParentInst.selectedID());

    htSubInst.getDataRows().forEach(row ->
    {
      int subInstID = row.getID(1);

      if ((subInstID > 0) || (row.getText(1).length() > 0) || (row.getText(3).length() > 0))
      {
        HDT_Institution subInst = subInstID < 1 ? db.createNewBlankRecord(hdtInstitution) : db.institutions.getByID(subInstID);

        subInstID = subInst.getID();
        subInst.setName(row.getText(1));
        subInst.parentInst.setID(curInst.getID());
        subInst.instType.setID(row.getID(2));
        subInst.setWebLink(row.getText(3));

        if ((subInst.name().length() == 0) &&
            (subInst.getWebLink().length() == 0) &&
            (subInst.persons.isEmpty()))
          db.deleteRecord(hdtInstitution, subInstID);
      }
    });

    if (locationChanged == false) return true;

    if (hasSubInstWithDifferentLocation(curInst, curInst))
      if (!confirmDialog("One or more sub-institutions have a different location than the present one. Should they also be updated?"))
        return true;

    curInst.overwriteSubInstLocations();

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean differentLocation(HDT_Institution instToCheck, HDT_Institution baseInst)
  {
    String city = instToCheck.getCity().trim();

    if ((city.length() > 0) && (city.equalsIgnoreCase(baseInst.getCity().trim()) == false))
      return true;

    HDT_State state = instToCheck.state.get();

    if ((state != null) && (state != baseInst.state.get()))
      return true;

    HDT_Country country = instToCheck.country.get();

    if ((country != null) && (country != baseInst.country.get()))
      return true;

    return hasSubInstWithDifferentLocation(instToCheck, baseInst);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean hasSubInstWithDifferentLocation(HDT_Institution instToCheck, HDT_Institution baseInst)
  {
    return instToCheck.subInstitutions.stream().anyMatch(subInst -> differentLocation(subInst, baseInst));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
