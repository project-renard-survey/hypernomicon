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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import com.adobe.internal.xmp.XMPException;

import org.hypernomicon.model.items.Author;
import org.hypernomicon.model.items.HDI_OfflineTernary.Ternary;
import org.hypernomicon.bib.authors.BibAuthors;
import org.hypernomicon.bib.data.BibData;
import org.hypernomicon.bib.data.BibDataStandalone;
import org.hypernomicon.bib.data.BibField.BibFieldEnum;
import org.hypernomicon.bib.data.CrossrefBibData;
import org.hypernomicon.bib.data.EntryType;
import org.hypernomicon.bib.data.GoogleBibData;
import org.hypernomicon.bib.data.PDFBibData;
import org.hypernomicon.model.Exceptions.TerminateTaskException;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.items.PersonName;
import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.HDT_MiscFile;
import org.hypernomicon.model.records.HDT_Person;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithPath;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.model.records.HDT_WorkFile;
import org.hypernomicon.model.records.HDT_WorkFile.FileNameAuthor;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_WorkType;
import org.hypernomicon.model.records.SimpleRecordTypes.WorkTypeEnum;
import org.hypernomicon.model.relations.ObjectGroup;

import static org.hypernomicon.bib.data.BibField.BibFieldEnum.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.model.records.SimpleRecordTypes.WorkTypeEnum.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;

import org.hypernomicon.util.AsyncHttpClient;
import org.hypernomicon.util.JsonHttpClient;
import org.hypernomicon.util.filePath.FilePath;
import org.hypernomicon.view.populators.Populator;
import org.hypernomicon.view.populators.StandardPopulator;
import org.hypernomicon.view.tabs.WorkTabCtrlr;
import org.hypernomicon.view.workMerge.MergeWorksDlgCtrlr;
import org.hypernomicon.view.wrappers.HyperCB;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTable.CellUpdateHandler;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.Const.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;

//---------------------------------------------------------------------------

public class WorkDlgCtrlr extends HyperDlg
{
  @FXML private Button btnBrowse, btnLaunch, btnOk, btnRegenerateFilename, btnStop;
  @FXML private CheckBox chkCreateBibEntry, chkKeepFilenameUnchanged;
  @FXML private ComboBox<EntryType> cbEntryType;
  @FXML private ComboBox<HyperTableCell> cbType;
  @FXML private Label lblAutoPopulated, lblCase;
  @FXML private MenuItem mnuPopulateFromPDF;
  @FXML private ProgressBar progressBar;
  @FXML private RadioButton rbCopy, rbCurrent, rbMove;
  @FXML private SplitMenuButton btnDOI;
  @FXML private TableView<HyperTableRow> tvAuthors, tvISBN;
  @FXML private TextArea taMisc;
  @FXML private TextField tfDOI, tfFileTitle, tfNewFile, tfOrigFile, tfTitle, tfYear;
  @FXML public Button btnCancel;

  private HyperCB hcbType;
  private HyperTable htAuthors, htISBN;
  private HDT_WorkFile oldWorkFile = null, newWorkFile = null;

  private FilePath origFilePath = null;
  private BibData pdfBD = null, curBD = null;
  private HDT_Work curWork;
  private boolean dontRegenerateFilename = false, alreadyChangingTitle = false;

  public static final AsyncHttpClient httpClient = new AsyncHttpClient();

  public List<ObjectGroup> getAuthorGroups() { return htAuthors.getAuthorGroups(curWork, 0, 2, 3, 4); }
  public boolean getCreateEntry()            { return chkCreateBibEntry.isVisible() && chkCreateBibEntry.isSelected(); }

//---------------------------------------------------------------------------

  public static WorkDlgCtrlr create(String title, FilePath filePathToUse)
  {
    WorkDlgCtrlr wdc = HyperDlg.create("WorkDlg.fxml", title, true);
    wdc.init(null, filePathToUse);
    return wdc;
  }

  public static WorkDlgCtrlr create(String title, HDT_WorkFile workFileToUse)
  {
    WorkDlgCtrlr wdc = HyperDlg.create("WorkDlg.fxml", title, true);
    wdc.init(workFileToUse, null);
    return wdc;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void initControls()
  {
    lblAutoPopulated.setText("");
    tfOrigFile.setEditable(false);

    if (db.bibLibraryIsLinked())
      bibManagerDlg.initCB(cbEntryType);

    tfNewFile.disableProperty().bind(chkKeepFilenameUnchanged.selectedProperty());

    btnDOI.setOnAction(event -> btnDOIClick());
    mnuPopulateFromPDF.setOnAction(event -> extractDataFromPdf(false, true));

    htISBN = new HyperTable(tvISBN, 0, true, "");

    htISBN.addTextEditCol(hdtWork, true, false);

    htISBN.addContextMenuItem("Use this ISBN to fill in fields",
      row -> row.getText(0).length() > 0,
      row ->
      {
        List<String> list = matchISBN(row.getText(0));
        if (collEmpty(list) == false)
          mnuISBNClick(list.get(0));
      });

    htAuthors = new HyperTable(tvAuthors, 0, true, PREF_KEY_HT_WORK_DLG);

    htAuthors.addColWithUpdateHandler(hdtPerson, ctDropDownList, (row, cellVal, nextColNdx, nextPopulator) ->
    {
      dontRegenerateFilename = true;

      if (HyperTableCell.getCellID(cellVal) > 0)
        row.setCheckboxValue(1, true);

      if (htAuthors.getDataRowCount() == 1)
        row.setCheckboxValue(2, true);
      else if (HyperTableCell.getCellID(cellVal) > 0)
      {
        boolean useInFilename = true, keepGoing = true;
        Iterator<HyperTableRow> it = htAuthors.getDataRows().iterator();

        while (it.hasNext() && keepGoing && useInFilename)
        {
          HyperTableRow loopRow = it.next();
          if (loopRow == row)
            keepGoing = false;
          else if (loopRow.getID(0) > 0)
            useInFilename = false;
        }

        if (useInFilename)
        {
          htAuthors.getDataRows().forEach(loopRow -> loopRow.setCheckboxValue(2, false));
          row.setCheckboxValue(2, true);
        }
      }

      dontRegenerateFilename = false;

      btnRegenerateFilenameClick();
    });

    htAuthors.addCheckboxColWithUpdateHandler(createAuthorRecordHandler(htAuthors, () -> curWork));

    CellUpdateHandler handler = (row, cellVal, nextColNdx, nextPopulator) -> btnRegenerateFilenameClick();

    htAuthors.addCheckboxColWithUpdateHandler(handler);
    htAuthors.addCheckboxColWithUpdateHandler(handler);
    htAuthors.addCheckboxColWithUpdateHandler(handler);

    htAuthors.addRemoveMenuItem();
    htAuthors.addChangeOrderMenuItem(true);

    htAuthors.addContextMenuItem("Remove this row",
      row -> (row.getText(0).length() > 0) && (row.getID(0) < 1),
      htAuthors::removeRow);

    hcbType = new HyperCB(cbType, ctDropDownList, new StandardPopulator(hdtWorkType), null);

    tfTitle.textProperty().addListener((ob, oldValue, newValue) ->
    {
      int pos;
      String fileTitle = newValue;

      fileTitle = fileTitle.replace('?', ':')
                           .replace('/', '-');

      pos = fileTitle.indexOf(':');
      if (pos >= 0) fileTitle = fileTitle.substring(0, pos);

      fileTitle = FilePath.removeInvalidFileNameChars(fileTitle);

      tfFileTitle.setText(fileTitle.trim());
    });

    tfFileTitle.textProperty().addListener((ob, oldValue, newValue) -> btnRegenerateFilenameClick());

    cbType.getSelectionModel().selectedItemProperty().addListener((ob, oldValue, newValue) ->
    {
      if (newValue == null) return;

      WorkTypeEnum workTypeEnumVal = HDT_WorkType.workTypeIDToEnumVal(HyperTableCell.getCellID(newValue)),
                   oldEnumVal = curWork.getWorkTypeValue();

      if ((oldEnumVal == wtUnenteredSet) && (workTypeEnumVal != wtUnenteredSet))
      {
        messageDialog("You cannot change the work type after it has been set to Unentered Set of Work Files.", mtError);
        Platform.runLater(() -> cbType.setValue(oldValue));
        return;
      }

      if (workTypeEnumVal == wtUnenteredSet)
      {
        if ((oldEnumVal != wtUnenteredSet) && (oldEnumVal != wtNone))
        {
          messageDialog("You cannot change a work with an existing work type into an unentered set of work files.", mtError);
          Platform.runLater(() -> cbType.setValue(oldValue));
          return;
        }

        tfNewFile.disableProperty().unbind();

        disableAll(tfNewFile, tfFileTitle, tfYear, chkKeepFilenameUnchanged, btnRegenerateFilename, rbMove, rbCopy, rbCurrent);
      }
      else if (workTypeEnumVal != wtNone)
      {
        enableAll(tfFileTitle, tfYear, chkKeepFilenameUnchanged, btnRegenerateFilename, rbMove, rbCopy);

        tfNewFile.disableProperty().bind(chkKeepFilenameUnchanged.selectedProperty());
      }
    });

    tfYear.textProperty().addListener((ob, oldValue, newValue) -> btnRegenerateFilenameClick());

    tfOrigFile.textProperty().addListener((ob, oldValue, newValue) -> btnRegenerateFilenameClick());

    lblCase.setOnMouseClicked(event ->
    {
      alreadyChangingTitle = true;
      tfTitle.setText(HDT_Work.fixCase(tfTitle.getText()));
      alreadyChangingTitle = false;
    });

    tfTitle.setTextFormatter(new TextFormatter<>(change ->
    {
      if (alreadyChangingTitle) return change;

      if (change.getText().length() > 1)
      {
        alreadyChangingTitle = true;

        String title = convertToSingleLine(change.getControlNewText());

        if ((title.equals(title.toUpperCase())) || (title.equals(title.toLowerCase())))
          title = HDT_Work.fixCase(title);

        change.setRange(0, change.getControlText().length());
        change.setText(ultraTrim(title));
        alreadyChangingTitle = false;
      }

      return change;
    }));

    btnStop.setOnAction(event -> stopClicked());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static CellUpdateHandler createAuthorRecordHandler(HyperTable htAuthors, Supplier<HDT_Work> workSource)
  {
    return (row, cellVal, nextColNdx, nextPopulator) ->
    {
      if (row.getID(0) > 0)
      {
        Platform.runLater(() -> row.setCheckboxValue(1, true));
        return;
      }
      else if (cellVal.equals(HyperTableCell.falseCell))
        return;

      String text = row.getText(0);

      HDT_Person otherPerson = WorkTabCtrlr.otherPersonToUse(text);

      if (otherPerson != null)
      {
        htAuthors.selectID(0, row, otherPerson.getID());
        return;
      }

      Author author = null;
      HDT_Work work = workSource.get();

      if (work != null)
      {
        author = work.getAuthors().getAuthor(new PersonName(text));

        if (author == null)
          author = new Author(work, new PersonName(text), false, false, Ternary.Unset);
      }

      NewPersonDlgCtrlr npdc = NewPersonDlgCtrlr.create(true, text, author);

      if (npdc.showModal())
      {
        Populator pop = htAuthors.getPopulator(0);
        pop.setChanged(row);                      // A new record has been created so force it to repopulate
        htAuthors.selectID(0, row, npdc.getPerson().getID());
      }
      else
        Platform.runLater(() -> row.setCheckboxValue(1, false));
    };
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void init(HDT_WorkFile workFileToUse, FilePath filePathToUse)
  {
    initControls();

    onShown = () ->
    {
      disableCache(taMisc);

      if (oldWorkFile == null)
      {
        if (filePathToUse == null)
          btnBrowseClick();
        else
          useChosenFile(filePathToUse);
      }
    };

    curWork = ui.workHyperTab().activeRecord();
    curBD = new BibDataStandalone(curWork.getBibData());

    if ((db.bibLibraryIsLinked() == false) || (curWork.getBibEntryKey().length() > 0))
      setAllVisible(false, chkCreateBibEntry, cbEntryType);

    if (workFileToUse != null)
    {
      oldWorkFile = workFileToUse;
      newWorkFile = oldWorkFile;
      rbCurrent.setSelected(true);
      rbCopy.setDisable(true);
    }

    ui.workHyperTab().getBibDataFromGUI(curBD);
    populateFieldsFromBibData(curBD, false);

    htAuthors.clear();
    htAuthors.getPopulator(0).populate(null, false);

    boolean atLeastOneInFilename = false;

    for (HyperTableRow origRow : ui.workHyperTab().htAuthors.getDataRows())
    {
      int authID = origRow.getID(1);
      String authName = origRow.getText(1);
      Ternary isInFileName = Ternary.Unset;

      if (authID > 0)
        isInFileName = curWork.personIsInFileName(origRow.getRecord());
      else
      {
        htAuthors.getPopulator(0).addEntry(null, -1, authName);
        Author auth = curWork.getAuthors().getAuthor(new PersonName(authName));
        if (auth != null)
          isInFileName = auth.getInFileName();
      }

      HyperTableRow newRow = htAuthors.newDataRow();
      newRow.setCellValue(0, authID, authName, hdtPerson);
      newRow.setCheckboxValue(1, authID > 0);

      boolean boolVal;

      switch (isInFileName)
      {
        case True  : boolVal = true; break;
        case False : boolVal = false; break;
        default    : boolVal = !atLeastOneInFilename; atLeastOneInFilename = true; break;
      }

      newRow.setCheckboxValue(2, boolVal);
      newRow.setCheckboxValue(3, origRow.getCheckboxValue(2));
      newRow.setCheckboxValue(4, origRow.getCheckboxValue(3));
    }

    if (oldWorkFile != null)
    {
      origFilePath = oldWorkFile.getPath().getFilePath();
      tfOrigFile.setText(origFilePath.toString());
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @FXML private void btnLaunchClick()
  {
    launchFile(origFilePath);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @FXML private void btnRegenerateFilenameClick()
  {
    if (dontRegenerateFilename) return;

    String ext, year, fileName, newFileName = "";

    ext = FilenameUtils.getExtension(tfOrigFile.getText());
    if (ext.length() == 0)
      ext = FilenameUtils.getExtension(tfNewFile.getText());

    year = tfYear.getText();

    ArrayList<FileNameAuthor> authors = new ArrayList<>();

    htAuthors.getDataRows().forEach(row ->
    {
      if ((row.getRecord() != null) || (row.getText(0).length() > 0))
        if (row.getCheckboxValue(2))
          authors.add(new FileNameAuthor(row.getText(0), row.getCheckboxValue(3), row.getCheckboxValue(4)));
    });

    fileName = HDT_WorkFile.makeFileName(authors, year, tfFileTitle.getText(), ext);

    if (fileName.length() == 0)
    {
      tfNewFile.setText("");
      return;
    }

    boolean nameTaken = true;

    for (int ctr = 1; nameTaken; ctr++)
    {
      if (ctr >= 1000)
      {
        newFileName = fileName;
        break;
      }

      newFileName = FilenameUtils.getBaseName(fileName) + (ctr == 1 ? "" : "_" + String.valueOf(1000 + (ctr % 1000)).substring(1, 4)) +
                    FilenameUtils.EXTENSION_SEPARATOR_STR + FilenameUtils.getExtension(fileName);
      nameTaken = false;

      for (HDT_WorkFile file : curWork.workFiles)
      {
        if (file != oldWorkFile)
          if (FilenameUtils.equalsNormalizedOnSystem(file.getPath().getNameStr(), newFileName))
            nameTaken = true;
      }
    }

    tfNewFile.setText(newFileName.trim());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @FXML private void btnBrowseClick()
  {
    FileChooser fileChooser = new FileChooser();

    if (EnumSet.of(wtBook, wtChapter, wtNone, wtPaper).contains(curWork.getWorkTypeValue()))
      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Adobe PDF file (*.pdf)", "*.pdf"));

    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files (*.*)", "*.*"));

    fileChooser.setInitialDirectory(db.unenteredPath().toFile());

    useChosenFile(new FilePath(fileChooser.showOpenDialog(getStage())));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void useChosenFile(FilePath chosenFile)
  {
    if (FilePath.isEmpty(chosenFile)) return;

    if (db.isProtectedFile(chosenFile))
    {
      messageDialog("That file cannot be assigned to a work record.", mtError);
      return;
    }

    // See if the chosen file is currently assigned to a file record

    HDT_RecordWithPath file = HyperPath.getFileFromFilePath(chosenFile);

    if (file != null)
    {
      if (file instanceof HDT_MiscFile)
      {
        messageDialog("That file is already in use as a miscellaneous file, record ID: " + file.getID(), mtError);
        return;
      }

      // Set variable to the currently assigned file record of the chosen file

      newWorkFile = (HDT_WorkFile) file;
    }
    else
      newWorkFile = null; // no file record was already assigned to the chosen file

    rbMove.setDisable(false);

    rbCurrent.setDisable(db.getRootPath().isSubpath(chosenFile) == false);

    if (rbCurrent.isSelected())
    {
      if (rbCurrent.isDisabled() || db.unenteredPath().isSubpath(chosenFile))
        rbMove.setSelected(true);
    }

    // check if there will be any change in which file record will be assigned to the work.
    // if not, disable the option to "copy" ("copy" creates a new work file record)

    if ((newWorkFile == null) || (oldWorkFile != newWorkFile))
    {
      rbCopy.setDisable(false);
    }
    else
    {
      rbCopy.setDisable(true);

      if (rbCopy.isSelected())
        rbMove.setSelected(true);
    }

    origFilePath = chosenFile;
    tfOrigFile.setText(origFilePath.toString());

    boolean gotData = false;

    if (tfTitle.getText().length() == 0)
      if (tfYear.getText().length() == 0)
        gotData = extractDataFromPdf(appPrefs.getBoolean(PREF_KEY_AUTO_RETRIEVE_BIB, true), false);

    if ((gotData == false) && appPrefs.getBoolean(PREF_KEY_AUTO_OPEN_PDF, true))
      btnLaunchClick();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean extractDataFromPdf(boolean doWebQuery, boolean doMerge)
  {
    boolean dontLaunchPdf = true;

    if (FilePath.isEmpty(origFilePath) || origFilePath.exists() == false) return false;
    if (getMediaType(origFilePath).toString().contains("pdf") == false) return false;

    httpClient.stop();
    setAllVisible(false, btnStop, progressBar);

    PDFBibData tempPdfBD = null;

    try
    {
      tempPdfBD = new PDFBibData(origFilePath);
    }
    catch (IOException | XMPException e)
    {
      return falseWithErrorMessage("Error: " + e.getMessage());
    }

    List<String> isbns = tempPdfBD.getMultiStr(bfISBNs);

    String doi = tempPdfBD.getStr(bfDOI), isbn = "";
    if (doi.length() == 0)
      doi = matchDOI(origFilePath.getNameOnly().toString());

    if (isbns.size() == 0)
      isbns = matchISBN(origFilePath.getNameOnly().toString());

    if (isbns.size() > 0)
      isbn = isbns.get(0);

    if (doWebQuery)
    {
      if (doi.length() > 0)
        queryCrossref(doi);
      else if (isbn.length() > 0)
        queryGoogleBooks(isbns.iterator());
      else
        dontLaunchPdf = false;
    }

    tfDOI.setText(doi);
    htISBN.clear();

    htISBN.buildRows(isbns, (row, isbnStr) -> row.setCellValue(0, -1, isbnStr, hdtNone));

    if (doWebQuery && dontLaunchPdf)
    {
      tempPdfBD.populateFromFile();
      pdfBD = tempPdfBD;
    }
    else
    {
      if (doMerge)
      {
        try
        {
          tempPdfBD.populateFromFile();
          doMerge(tempPdfBD);
        }
        catch (IOException e)
        {
          messageDialog("Unable to initialize merge dialog window.", mtError);
          return true;
        }
      }
      else
      {
        lblAutoPopulated.setText("Fields auto-populated with information extracted from PDF file");
        tempPdfBD.populateFromFile();
        populateFieldsFromBibData(tempPdfBD, true);
      }
    }

    return dontLaunchPdf;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void doMerge(BibData bd) throws IOException
  {
    MergeWorksDlgCtrlr mwd = null;
    getBibDataFromGUI();

    mwd = MergeWorksDlgCtrlr.create("Merge Information From PDF File", curBD, bd, null, null, curWork, false, false);

    if (mwd.showModal())
    {
      lblAutoPopulated.setText("");
      mwd.mergeInto(curBD);
      populateFieldsFromBibData(curBD, true);
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  void queryCrossref(String doi)
  {
    lblAutoPopulated.setText("");
    setAllVisible(true, btnStop, progressBar);

    JsonHttpClient.getObjAsync(CrossrefBibData.getQueryUrl(doi), httpClient, jsonObj ->
    {
      BibData bibData = CrossrefBibData.createFromJSON(jsonObj, doi);

      if (bibData != null)
      {
        tfDOI.setText(doi);

        populateFieldsFromBibData(bibData, true);
        lblAutoPopulated.setText("Fields have been auto-populated from Crossref using doi: " + doi);
      }
      else
      {
        if (pdfBD != null)
        {
          lblAutoPopulated.setText("Fields auto-populated with information extracted from PDF file");
          populateFieldsFromBibData(pdfBD, true);
        }

        if (appPrefs.getBoolean(PREF_KEY_AUTO_OPEN_PDF, true))
          btnLaunchClick();
      }

      setAllVisible(false, btnStop, progressBar);
      pdfBD = null;

    }, e ->
    {
      lblAutoPopulated.setText("");
      setAllVisible(false, btnStop, progressBar);

      if (e instanceof ParseException)
      {
        if (appPrefs.getBoolean(PREF_KEY_AUTO_OPEN_PDF, true))
          btnLaunchClick();
      }
      else if (e instanceof TerminateTaskException)
        noOp();
      else if (e instanceof UnknownHostException)
        messageDialog("Unable to connect to host: " + e.getMessage(), mtError);
      else
        messageDialog("Error: " + e.getMessage(), mtError);

      if (pdfBD != null)
      {
        lblAutoPopulated.setText("Fields auto-populated with information extracted from PDF file");
        populateFieldsFromBibData(pdfBD, true);
      }

      pdfBD = null;
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  void queryGoogleBooks(Iterator<String> it)
  {
    String isbn = it.next();

    lblAutoPopulated.setText("");
    setAllVisible(true, btnStop, progressBar);

    JsonHttpClient.getObjAsync(GoogleBibData.getQueryUrl(isbn), httpClient, jsonObj ->
    {
      BibData bibData = GoogleBibData.createFromJSON(jsonObj, isbn);

      if (bibData != null)
      {
        populateFieldsFromBibData(bibData, true);
        lblAutoPopulated.setText("Fields have been auto-populated from Google Books using isbn: " + isbn);
        setAllVisible(false, btnStop, progressBar);
      }
      else
      {
        if (it.hasNext())
        {
          queryGoogleBooks(it);
        }
        else
        {
          if (appPrefs.getBoolean(PREF_KEY_AUTO_OPEN_PDF, true))
            btnLaunchClick();

          if (pdfBD != null)
          {
            lblAutoPopulated.setText("Fields auto-populated with information extracted from PDF file");
            populateFieldsFromBibData(pdfBD, true);
          }

          setAllVisible(false, btnStop, progressBar);
        }
      }

      pdfBD = null;

    }, e ->
    {
      lblAutoPopulated.setText("");
      setAllVisible(false, btnStop, progressBar);

      if (e instanceof ParseException)
      {
        if (appPrefs.getBoolean(PREF_KEY_AUTO_OPEN_PDF, true))
          btnLaunchClick();
      }
      else if (e instanceof TerminateTaskException)
        noOp();
      else if (e instanceof UnknownHostException)
        messageDialog("Unable to connect to host: " + e.getMessage(), mtError);
      else
        messageDialog("Error: " + e.getMessage(), mtError);

      if (pdfBD != null)
      {
        lblAutoPopulated.setText("Fields auto-populated with information extracted from PDF file");
        populateFieldsFromBibData(pdfBD, true);
      }

      pdfBD = null;
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void btnDOIClick()
  {
    String doi = matchDOI(tfDOI.getText());
    if (doi.length() == 0) return;

    lblAutoPopulated.setText("");
    setAllVisible(true, btnStop, progressBar);

    JsonHttpClient.getObjAsync(CrossrefBibData.getQueryUrl(doi), httpClient, jsonObj ->
    {
      BibData bd = CrossrefBibData.createFromJSON(jsonObj, doi);
      setAllVisible(false, btnStop, progressBar);

      if (bd == null)
        lblAutoPopulated.setText("Crossref query yielded no results for doi: " + doi);
      else
      {
        try
        {
          doMerge(bd);
        }
        catch (IOException e)
        {
          messageDialog("Unable to initialize merge dialog window.", mtError);
        }
      }
    }, e ->
    {
      lblAutoPopulated.setText("");
      setAllVisible(false, btnStop, progressBar);

      if ((e instanceof ParseException) || (e instanceof TerminateTaskException))
        return;

      if (e instanceof UnknownHostException)
        messageDialog("Unable to connect to host: " + e.getMessage(), mtError);
      else
        messageDialog("Error: " + e.getMessage(), mtError);
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void mnuISBNClick(String isbn)
  {
    lblAutoPopulated.setText("");
    setAllVisible(true, btnStop, progressBar);

    JsonHttpClient.getObjAsync(GoogleBibData.getQueryUrl(isbn), httpClient, jsonObj ->
    {
      BibData bd = GoogleBibData.createFromJSON(jsonObj, isbn);
      setAllVisible(false, btnStop, progressBar);

      if (bd == null)
        lblAutoPopulated.setText("Google Books query yielded no results for isbn: " + isbn);
      else
      {
        try
        {
          doMerge(bd);
        }
        catch (IOException e)
        {
          messageDialog("Unable to initialize merge dialog window.", mtError);
        }
      }
    }, e ->
    {
      lblAutoPopulated.setText("");
      setAllVisible(false, btnStop, progressBar);

      if ((e instanceof ParseException) || (e instanceof TerminateTaskException))
        return;

      if (e instanceof UnknownHostException)
        messageDialog("Unable to connect to host: " + e.getMessage(), mtError);
      else
        messageDialog("Error: " + e.getMessage(), mtError);
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void stopClicked()
  {
    httpClient.stop();

    lblAutoPopulated.setText("");
    setAllVisible(false, btnStop, progressBar);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static boolean addAuthorToTable(PersonName authorName, boolean editor, boolean trans, HDT_Person author, HyperTable htAuthors, boolean hasShowInFileCol)
  {
    if (authorName.isEmpty()) return false;

    HyperTableRow row = htAuthors.newDataRow();

    if (author != null)
    {
      row.setCellValue(0, author, author.listName());
      row.setCheckboxValue(1, true);
    }
    else
    {
      String authorStr = authorName.getLastFirst();

      htAuthors.getPopulator(0).addEntry(null, -1, authorStr);
      row.setCellValue(0, -1, authorStr, hdtPerson);
    }

    int addend = hasShowInFileCol ? 1 : 0;

    if (editor)
      row.setCheckboxValue(2 + addend, true);

    if (trans)
      row.setCheckboxValue(3 + addend, true);

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static void loadFromBibAuthors(BibAuthors bibAuthors, HyperTable htAuthors, boolean hasShowInFileCol, HDT_Work destWork)
  {
    if ((bibAuthors == null) || bibAuthors.isEmpty()) return;

    ArrayList<PersonName> nameList = new ArrayList<>();
    ArrayList<HDT_Person> personList = new ArrayList<>();
    HashMap<PersonName, Boolean> nameToEd = new HashMap<>(), nameToTr = new HashMap<>();

    bibAuthors.getListsForWorkMerge(nameList, personList, nameToEd, nameToTr, destWork);

    htAuthors.clear();
    htAuthors.getPopulator(0).populate(null, false);

    for (int ndx = 0; ndx < nameList.size(); ndx++)
    {
      PersonName name = nameList.get(ndx);

      addAuthorToTable(name, nameToEd.get(name), nameToTr.get(name), personList.get(ndx), htAuthors, hasShowInFileCol);
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public BibData getBibDataFromGUI()
  {
    curBD.setStr(bfYear, tfYear.getText());
    curBD.setStr(bfDOI, tfDOI.getText());
    curBD.setTitle(tfTitle.getText());

    curBD.setMultiStr(bfMisc, convertMultiLineStrToStrList(taMisc.getText(), true));

    curBD.setWorkType(hcbType.selectedRecord());

    curBD.setMultiStr(bfISBNs, htISBN.dataRowStream().map(row -> row.getText(0)).collect(Collectors.toList()));

    curBD.getAuthors().setAllFromTable(getAuthorGroups());

    return curBD;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void populateFieldsFromBibData(BibData bd, boolean populateAuthors)
  {
    if (bd != curBD)
      curBD.copyAllFieldsFrom(bd, populateAuthors, true);

    if (curBD.entryTypeNotEmpty() && db.bibLibraryIsLinked())
    {
      EntryType entryType = curBD.getEntryType();
      if (cbEntryType.getItems().contains(entryType) == false)
      {
        messageDialog("\"" + entryType.getUserFriendlyName() + "\" is not a valid " +
                      db.getBibLibrary().type().getUserFriendlyName() + " entry type.", mtWarning);
        cbEntryType.getSelectionModel().select(null);
      }
      else
        cbEntryType.getSelectionModel().select(entryType);
    }

    tfYear.setText(curBD.getStr(bfYear));

    alreadyChangingTitle = true;
    tfTitle.setText(curBD.getStr(bfTitle));
    alreadyChangingTitle = false;

    taMisc.setText(curBD.getStr(bfMisc));

    HDT_WorkType workType = curBD.getWorkType();

    hcbType.selectID(nullSwitch(workType, -1, HDT_Record::getID));

    tfDOI.setText(curBD.getStr(bfDOI));

    htISBN.clear();

    htISBN.buildRows(curBD.getMultiStr(bfISBNs), (row, isbnStr) -> row.setCellValue(0, -1, isbnStr, hdtNone));

    if (populateAuthors)
      loadFromBibAuthors(curBD.getAuthors(), htAuthors, true, curWork);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected boolean isValid()
  {
    boolean success = true;
    FilePath newFilePath;

    getBibDataFromGUI();
    EnumSet<BibFieldEnum> extFields = curBD.fieldsWithExternalData();

    if (chkCreateBibEntry.isVisible())
    {
      if ((extFields.size() > 0) && (chkCreateBibEntry.isSelected() == false))
      {
        String typeName = db.getBibLibrary().type().getUserFriendlyName(),
               msg = "The current work record is not associated with a " + typeName + " entry. Create one now?\n";

        msg = msg + "Otherwise, existing information for these fields will be lost: ";

        String fieldsStr = extFields.stream().map(BibFieldEnum::getUserFriendlyName).reduce((s1, s2) -> s1 + ", " + s2).orElse("");

        chkCreateBibEntry.setSelected(confirmDialog(msg + fieldsStr));
      }

      if (chkCreateBibEntry.isSelected() && (getEntryType() == null))
      {
        messageDialog("Select a bibliographic entry type.", mtWarning);
        safeFocus(cbEntryType);
        return false;
      }
    }

    if (chkKeepFilenameUnchanged.isSelected() == false)
      if ((tfOrigFile.getText().length() > 0) && (tfNewFile.getText().length() == 0))
      {
        messageDialog("Enter destination file name.", mtWarning);
        safeFocus(tfNewFile);
        return false;
      }

    if (hcbType.selectedID() < 1)
    {
      messageDialog("Select a work type.", mtWarning);
      safeFocus(cbType);
      return false;
    }

    if (tfOrigFile.getText().length() == 0)
    {
      if (oldWorkFile != null)
        return falseWithErrorMessage("Internal error #82709");

      return true;
    }

    if (rbCurrent.isSelected())
    {
      if (chkKeepFilenameUnchanged.isSelected())
        newFilePath = origFilePath;
      else
        newFilePath = origFilePath.getDirOnly().resolve(tfNewFile.getText());
    }
    else
    {
      if (chkKeepFilenameUnchanged.isSelected())
        newFilePath = HDT_Work.getBasePathForWorkTypeID(hcbType.selectedID()).resolve(origFilePath.getNameOnly());
      else
        newFilePath = HDT_Work.getBasePathForWorkTypeID(hcbType.selectedID()).resolve(tfNewFile.getText());
    }

    HDT_RecordWithPath existingFile = HyperPath.getFileFromFilePath(newFilePath);

    if (existingFile != null)
    {
      if (existingFile instanceof HDT_MiscFile)
        return falseWithErrorMessage("New file name is already in use as a miscellaneous file, record ID: " + existingFile.getID());

      HDT_WorkFile existingWorkFile = (HDT_WorkFile)existingFile;

      if (newWorkFile == null)
      {
        if (existingWorkFile.works.isEmpty())
          return falseWithErrorMessage("Internal error #79002");

        int oldWorkFileID = -1;
        if (oldWorkFile != null)
          oldWorkFileID = oldWorkFile.getID();

        if (oldWorkFileID == existingWorkFile.getID())
          newWorkFile = existingWorkFile;
        else
          return falseWithErrorMessage("New file name is already in use as a work file, work record ID: " + existingWorkFile.works.get(0).getID());
      }
      else
      {
        if (newWorkFile.getID() != existingFile.getID())
          return falseWithErrorMessage("Another work file already has that file name, record ID: " + existingFile.getID());
      }
    }
    else
    {
      if ((newWorkFile == null) && (oldWorkFile != null) && (oldWorkFile.works.size() > 1))
        if (confirmDialog("The same file that was associated with this work is associated with other works as well. Should these also be updated?"))
          newWorkFile = oldWorkFile;
    }

    try
    {
      if (rbCopy.isSelected()) // either oldWorkFile is null, or oldWorkFile != newWorkFile
      {
        success = origFilePath.copyTo(newFilePath, true);

        if (success)
        {
          if (newWorkFile == null)
          {
            newWorkFile = (HDT_WorkFile) HyperPath.createRecordAssignedToPath(hdtWorkFile, newFilePath);
            if (newWorkFile == null)
              return falseWithErrorMessage("Internal error #67830");

            curWork.addWorkFile(newWorkFile.getID(), true, true);
          }
          else
          {
            newWorkFile.getPath().assign(HyperPath.getFolderFromFilePath(newFilePath.getDirOnly(), true), newFilePath.getNameOnly());
          }
        }
      }
      else
      {
        if (newWorkFile == null)
        {
          if (origFilePath.equals(newFilePath))
            success = true;
          else
          {
            success = origFilePath.moveTo(newFilePath, true);
            if (success)
              db.unmapFilePath(origFilePath);
          }

          if (success)
          {
            newWorkFile = (HDT_WorkFile) HyperPath.createRecordAssignedToPath(hdtWorkFile, newFilePath);
            if (newWorkFile == null)
              return falseWithErrorMessage("Internal error #67830");

            curWork.addWorkFile(newWorkFile.getID(), true, true);
          }
        }
        else if (oldWorkFile == newWorkFile)
        {
          if (origFilePath.equals(newFilePath) == false)
          {
            if (origFilePath.moveTo(newFilePath, true) == false) return false;

            db.unmapFilePath(origFilePath);
            newWorkFile.getPath().assign(HyperPath.getFolderFromFilePath(newFilePath.getDirOnly(), true), newFilePath.getNameOnly());
          }

          success = true;
        }
        else
        {
          if (oldWorkFile != null)
            return falseWithErrorMessage("Unable to move the file. Reason: Cannot change assignment from one file to another that is already assigned to a different file record.");

          success = newWorkFile.getPath().moveToFolder(HyperPath.getFolderFromFilePath(newFilePath.getDirOnly(), true).getID(), true, true, newFilePath.getNameOnly().toString());
          if (success) curWork.addWorkFile(newWorkFile.getID(), true, true);
        }
      }
    }
    catch (IOException e)
    {
      return falseWithErrorMessage("Unable to " + (rbCopy.isSelected() ? "copy" : "move") + "/rename the file. Reason: " + e.getMessage());
    }

    if (success == false)
      return falseWithErrorMessage("Unable to " + (rbCopy.isSelected() ? "copy" : "move") + "/rename the file.");

    if ((oldWorkFile != null) && (newWorkFile.getID() != oldWorkFile.getID()))
      db.getObjectList(rtWorkFileOfWork, curWork, true).remove(oldWorkFile);

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public EntryType getEntryType()
  {
    EntryType entryType = cbEntryType.getValue();

    if (entryType == null) return null;

    switch (entryType)
    {
      case etUnentered : case etOther : case etNone : return null;
      default: return entryType;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
