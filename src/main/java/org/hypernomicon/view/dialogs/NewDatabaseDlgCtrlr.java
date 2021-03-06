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

package org.hypernomicon.view.dialogs;

import java.util.EnumSet;
import java.util.HashMap;

import org.hypernomicon.model.records.HDT_RecordType;
import org.hypernomicon.util.filePath.FilePath;
import org.hypernomicon.util.filePath.FilePathSet;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import static org.hypernomicon.Const.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;

public class NewDatabaseDlgCtrlr extends HyperDlg
{
  @FXML private Button btnOK, btnCancel;
  @FXML private CheckBox cbInst, cbFields, cbRanks, cbStatus, cbStates, cbCountries;
  @FXML private TextField tfPapers, tfBooks, tfUnentered, tfPictures, tfTopicFolders, tfMiscFiles, tfResults;

  private String newPath;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean addNameToSet(FilePathSet set, String name)
  {
    name = ultraTrim(name);

    if (name.length() == 0)
      return falseWithErrorMessage("Folder name cannot be blank.");

    if (name.equalsIgnoreCase(DEFAULT_XML_PATH))
      return falseWithErrorMessage("The name XML is resevered for the XML folder.");

    if ((FilePath.isFilenameValid(name) == false) || (name.equals(new FilePath(name).getNameOnly().toString()) == false))
      return falseWithErrorMessage("Folder name is invalid: " + name);

    set.add(new FilePath(name));
    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected boolean isValid()
  {
    boolean success;
    FilePath filePath = new FilePath(newPath);

    FilePathSet set = new FilePathSet();

    if (! (addNameToSet(set, tfPictures    .getText()) &&
           addNameToSet(set, tfBooks       .getText()) &&
           addNameToSet(set, tfPapers      .getText()) &&
           addNameToSet(set, tfUnentered   .getText()) &&
           addNameToSet(set, tfMiscFiles   .getText()) &&
           addNameToSet(set, tfResults     .getText()) &&
           addNameToSet(set, tfTopicFolders.getText())))
      return false;

    if (set.size() < 7)
      return falseWithErrorMessage("Enter a unique name for each folder.");

    try
    {
      saveStringBuilderToFile(new StringBuilder(DEFAULT_XML_PATH + "/" + SETTINGS_FILE_NAME), filePath.resolve(HDB_DEFAULT_FILENAME));

      success              = filePath.resolve(DEFAULT_XML_PATH        ).toFile().mkdirs();
      if (success) success = filePath.resolve(ultraTrim(tfPictures    .getText())).toFile().mkdirs();
      if (success) success = filePath.resolve(ultraTrim(tfBooks       .getText())).toFile().mkdirs();
      if (success) success = filePath.resolve(ultraTrim(tfPapers      .getText())).toFile().mkdirs();
      if (success) success = filePath.resolve(ultraTrim(tfUnentered   .getText())).toFile().mkdirs();
      if (success) success = filePath.resolve(ultraTrim(tfMiscFiles   .getText())).toFile().mkdirs();
      if (success) success = filePath.resolve(ultraTrim(tfResults     .getText())).toFile().mkdirs();
      if (success) success = filePath.resolve(ultraTrim(tfTopicFolders.getText())).toFile().mkdirs();
    }
    catch(Exception e)
    {
      return falseWithErrorMessage("An error occurred while trying to create the directories: " + e.getMessage());
    }

    return success ? true : falseWithErrorMessage("An error occurred while trying to create the directories.");
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public EnumSet<HDT_RecordType> getChoices()
  {
    EnumSet<HDT_RecordType> choices = EnumSet.noneOf(HDT_RecordType.class);

    if (cbCountries.isSelected()) choices.add(hdtCountry);

    if (cbFields.isSelected())
    {
      choices.add(hdtField);
      choices.add(hdtSubfield);
    }

    if (cbInst.isSelected())      choices.add(hdtInstitution);
    if (cbRanks.isSelected())     choices.add(hdtRank);
    if (cbStates.isSelected())    choices.add(hdtState);
    if (cbStatus.isSelected())    choices.add(hdtPersonStatus);

    return choices;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static NewDatabaseDlgCtrlr create(String title, String newPath)
  {
    NewDatabaseDlgCtrlr ndd = HyperDlg.create("NewDatabaseDlg.fxml", title, true);
    ndd.init(newPath);
    return ndd;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void init(String newPath)
  {
    tfPictures    .setText(DEFAULT_PICTURES_PATH);
    tfBooks       .setText(DEFAULT_BOOKS_PATH);
    tfPapers      .setText(DEFAULT_PAPERS_PATH);
    tfUnentered   .setText(DEFAULT_UNENTERED_PATH);
    tfMiscFiles   .setText(DEFAULT_MISC_FILES_PATH);
    tfResults     .setText(DEFAULT_RESULTS_PATH);
    tfTopicFolders.setText(DEFAULT_TOPICAL_PATH);

    this.newPath = newPath;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HashMap<String, String> getFolders()
  {
    HashMap<String, String> folders = new HashMap<>();

    folders.put(PREF_KEY_PICTURES_PATH  , ultraTrim(tfPictures    .getText()));
    folders.put(PREF_KEY_BOOKS_PATH     , ultraTrim(tfBooks       .getText()));
    folders.put(PREF_KEY_PAPERS_PATH    , ultraTrim(tfPapers      .getText()));
    folders.put(PREF_KEY_UNENTERED_PATH , ultraTrim(tfUnentered   .getText()));
    folders.put(PREF_KEY_MISC_FILES_PATH, ultraTrim(tfMiscFiles   .getText()));
    folders.put(PREF_KEY_RESULTS_PATH   , ultraTrim(tfResults     .getText()));
    folders.put(PREF_KEY_TOPICAL_PATH   , ultraTrim(tfTopicFolders.getText()));

    return folders;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
