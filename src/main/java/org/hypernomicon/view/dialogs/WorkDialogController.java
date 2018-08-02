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

package org.hypernomicon.view.dialogs;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import com.adobe.internal.xmp.XMPException;

import org.hypernomicon.model.items.Author;
import org.hypernomicon.model.items.HDI_OfflineTernary.Ternary;
import org.hypernomicon.bib.BibAuthors;
import org.hypernomicon.bib.BibData;
import org.hypernomicon.bib.BibData.BibFieldEnum;
import org.hypernomicon.bib.BibData.EntryType;
import org.hypernomicon.bib.BibDataStandalone;
import org.hypernomicon.bib.BibUtils;
import org.hypernomicon.bib.BibUtils.PdfMetadata;
import org.hypernomicon.model.PersonName;
import org.hypernomicon.model.Exceptions.TerminateTaskException;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.records.HDT_MiscFile;
import org.hypernomicon.model.records.HDT_Person;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithPath;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.model.records.HDT_WorkFile;
import org.hypernomicon.model.records.HDT_WorkFile.FileNameAuthor;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_WorkType;
import org.hypernomicon.model.records.SimpleRecordTypes.WorkTypeEnum;

import static org.hypernomicon.bib.BibData.BibFieldEnum.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.model.records.SimpleRecordTypes.WorkTypeEnum.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;

import org.hypernomicon.util.AsyncHttpClient;
import org.hypernomicon.util.JsonHttpClient;
import org.hypernomicon.util.filePath.FilePath;
import org.hypernomicon.view.populators.Populator;
import org.hypernomicon.view.populators.StandardPopulator;
import org.hypernomicon.view.tabs.WorkTabController;
import org.hypernomicon.view.workMerge.MergeWorksDialogController;
import org.hypernomicon.view.wrappers.HyperCB;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTable.CellUpdateHandler;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.bib.BibUtils.*;
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

public class WorkDialogController extends HyperDialog
{ 
  @FXML private Label lblCase;
  @FXML private Label lblAutoPopulated;
  @FXML private Button btnOk;
  @FXML private SplitMenuButton btnDOI;
  @FXML private MenuItem mnuPopulateFromPDF;
  @FXML public Button btnCancel;
  @FXML private TextField tfOrigFile;
  @FXML public TextField tfTitle;
  @FXML private TextField tfFileTitle;
  @FXML private TextField tfNewFile;
  @FXML public TextField tfYear;
  @FXML public TextField tfDOI;
  @FXML public TextArea taMisc;
  @FXML private Button btnBrowse;
  @FXML private Button btnLaunch;
  @FXML private Button btnStop;
  @FXML public TableView<HyperTableRow> tvAuthors;
  @FXML public TableView<HyperTableRow> tvISBN;
  @FXML private ComboBox<HyperTableCell> cbType;
  @FXML private Button btnRegenerate;
  @FXML private RadioButton rbMove;
  @FXML private RadioButton rbCopy;
  @FXML private RadioButton rbCurrent;
  @FXML private CheckBox chkKeepFilenameUnchanged;
  @FXML private ProgressBar progressBar;
  @FXML public CheckBox chkCreateBibEntry;
  @FXML public ComboBox<EntryType> cbEntryType;
 
  public HyperCB hcbType;
  private boolean hereAlready = false;
  public HDT_WorkFile oldWorkFile = null, newWorkFile = null;
  private FilePath origFilePath = null;
  private boolean alreadyChangingTitle = false;
  public HyperTable htAuthors, htISBN;
  private BibData pdfBD = null, curBD = null;
  private HDT_Work curWork;
  
  public static final AsyncHttpClient httpClient = new AsyncHttpClient();
  
//---------------------------------------------------------------------------  

  public static WorkDialogController create(String title, FilePath filePathToUse, WorkTabController workCtrlr)
  {
    WorkDialogController wdc = HyperDialog.create("WorkDialog.fxml", title, true);
    wdc.init(null, workCtrlr, filePathToUse);
    return wdc;   
  }
  
  public static WorkDialogController create(String title, HDT_WorkFile workFileToUse, WorkTabController workCtrlr)
  {
    WorkDialogController wdc = HyperDialog.create("WorkDialog.fxml", title, true);
    wdc.init(workFileToUse, workCtrlr, null);
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
    
    htISBN.addCondRowBasedContextMenuItem("Use this ISBN to fill in fields",
      row -> row.getText(0).length() > 0,
      row -> 
      {
        List<String> list = BibUtils.matchISBN(row.getText(0));
        if (list == null) return;
        if (list.size() == 0) return;
        mnuISBNClick(list.get(0));   
      });
    
    htAuthors = new HyperTable(tvAuthors, 0, true, PREF_KEY_HT_WORK_DLG);
    
    htAuthors.addColWithUpdateHandler(hdtPerson, ctDropDownList, (row, cellVal, nextColNdx, nextPopulator) ->
    {     
      if (hereAlready) return;
      hereAlready = true;
      
      if (HyperTableCell.getCellID(cellVal) > 0)
        htAuthors.setCheckboxValue(1, row, true);
      
      if (htAuthors.getDataRowCount() == 1)
        htAuthors.setCheckboxValue(2, row, true);
                   
      btnRegenerateClick();
      
      hereAlready = false;
    });
       
    htAuthors.addCheckboxColWithUpdateHandler(createAuthorRecordHandler(htAuthors, () -> curWork));
        
    CellUpdateHandler handler = (row, cellVal, nextColNdx, nextPopulator) ->
    {
      if (hereAlready) return;
      hereAlready = true;
                
      btnRegenerateClick();
      
      hereAlready = false;
    };
        
    htAuthors.addCheckboxColWithUpdateHandler(handler);
    htAuthors.addCheckboxColWithUpdateHandler(handler);
    htAuthors.addCheckboxColWithUpdateHandler(handler);
    
    htAuthors.addRemoveMenuItem();
    htAuthors.addChangeOrderMenuItem(true);
    
    htAuthors.addCondRowBasedContextMenuItem("Remove this row",
        row -> (row.getText(0).length() > 0) && (row.getID(0) < 1),
        row -> htAuthors.removeRow(row));
       
    hcbType = new HyperCB(cbType, ctDropDownList, new StandardPopulator(hdtWorkType), null);
   
    tfTitle.textProperty().addListener((observable, oldValue, newValue) ->
    {
      int pos;
      String fileTitle = newValue;

      fileTitle = fileTitle.replace('?', ':');
      fileTitle = fileTitle.replace('/', '-');
      
      pos = fileTitle.indexOf(':');
      if (pos >= 0) fileTitle = fileTitle.substring(0, pos);
      
      fileTitle = FilePath.removeInvalidFileNameChars(fileTitle);
      
      tfFileTitle.setText(fileTitle.trim());  
    });
    
    tfFileTitle.textProperty().addListener((observable, oldValue, newValue) -> btnRegenerateClick());
         
    cbType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
    {    
      if (newValue == null) return;
      
      WorkTypeEnum workTypeEnumVal = HDT_WorkType.workTypeIDToEnumVal(HyperTableCell.getCellID(newValue));
      WorkTypeEnum oldEnumVal = curWork.getWorkTypeValue();
      
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
        tfNewFile.setDisable(true);
        tfFileTitle.setDisable(true);
        tfYear.setDisable(true);
        chkKeepFilenameUnchanged.setDisable(true);
        btnRegenerate.setDisable(true);
        rbMove.setDisable(true);
        rbCopy.setDisable(true);
        rbCurrent.setDisable(true);
      }
      else if (workTypeEnumVal != wtNone)
      {
        tfFileTitle.setDisable(false);
        tfYear.setDisable(false);
        chkKeepFilenameUnchanged.setDisable(false);
        tfNewFile.disableProperty().bind(chkKeepFilenameUnchanged.selectedProperty());
        btnRegenerate.setDisable(false);
        rbMove.setDisable(false);
        rbCopy.setDisable(false);
      }
    });   
    
    tfYear.textProperty().addListener((observable, oldValue, newValue) -> btnRegenerateClick());
    
    tfOrigFile.textProperty().addListener((observable, oldValue, newValue) -> btnRegenerateClick());
    
    lblCase.setOnMouseClicked(event ->
    {
      alreadyChangingTitle = true;
      tfTitle.setText(titleCase(tfTitle.getText()));
      alreadyChangingTitle = false;
    });
    
    UnaryOperator<TextFormatter.Change> filter = (change) ->
    {
      if (alreadyChangingTitle) return change;
      
      if (change.getText().length() > 1)
      {
        alreadyChangingTitle = true;

        String title = convertToSingleLine(change.getControlNewText());
  
        if ((title.equals(title.toUpperCase())) || (title.equals(title.toLowerCase())))
          title = titleCase(title);
        
        change.setRange(0, change.getControlText().length());
        change.setText(ultraTrim(title));
        alreadyChangingTitle = false;  
      }
      
      return change;
    };
         
    tfTitle.setTextFormatter(new TextFormatter<>(filter));
    
    btnStop.setOnAction(event -> stopClicked());
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @FunctionalInterface public static interface WorkSource { public HDT_Work getWork(); }
  
  public static CellUpdateHandler createAuthorRecordHandler(HyperTable htAuthors, WorkSource workSource)
  {
    return (row, cellVal, nextColNdx, nextPopulator) ->
    {
      if (row.getID(0) > 0)
      {
        Platform.runLater(() -> htAuthors.setCheckboxValue(1, row, true));
        return;
      }
      else if (cellVal.equals(HyperTableCell.falseCell))
        return;
      
      String text = row.getText(0);
      
      HDT_Person otherPerson = WorkTabController.otherPersonToUse(text);
      
      if (otherPerson != null)
      {
        htAuthors.selectID(0, row, otherPerson.getID());
        return;
      }
      
      Author author = null;
      HDT_Work work = workSource.getWork();
      
      if (work != null)
        author = work.getAuthors().getAuthor(new PersonName(text));
      
      NewPersonDialogController npdc = NewPersonDialogController.create(true, text, author);
      
      if (npdc.showModal())
      {
        Populator pop = htAuthors.getPopulator(0);
        pop.setChanged(row);                      // A new record has been created so force it to repopulate
        htAuthors.selectID(0, row, npdc.getPerson().getID());
      }
      else
        Platform.runLater(() -> htAuthors.setCheckboxValue(1, row, false));        
    };
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private void init(HDT_WorkFile workFileToUse, WorkTabController workCtrlr, FilePath filePathToUse)
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
    
    curWork = workCtrlr.activeRecord();
    curBD = new BibDataStandalone(curWork.getBibData());
       
    if (db.bibLibraryIsLinked() == false)
    {
      chkCreateBibEntry.setVisible(false);
      cbEntryType.setVisible(false);
    }
    else if (curWork.getBibEntryKey().length() > 0)
    {
      chkCreateBibEntry.setVisible(false);
      cbEntryType.setVisible(false);
    }
    
    if (workFileToUse != null)
    {
      oldWorkFile = workFileToUse;
      newWorkFile = oldWorkFile;
      rbCurrent.setSelected(true);
      rbCopy.setDisable(true);
    }
    
    workCtrlr.getBibDataFromGUI(curBD);
    populateFieldsFromBibData(curBD, false);
                    
    htAuthors.clear();
    htAuthors.getPopulator(0).populate(null, false);
    
    for (int rowNdx = 0; rowNdx < workCtrlr.htAuthors.getDataRowCount(); rowNdx++)
    {
      HyperTableRow row = workCtrlr.htAuthors.getRowByRowNdx(rowNdx);
      
      int authID = row.getID(1);
      String authName = row.getText(1);
      Ternary isInFileName = Ternary.Unset;
      
      if (authID > 0)
        isInFileName = curWork.personIsInFileName(HDT_Person.class.cast(row.getRecord()));
      else
      {
        htAuthors.getPopulator(0).addEntry(null, -1, authName);
        Author auth = curWork.getAuthors().getAuthor(new PersonName(authName));
        if (auth != null)
          isInFileName = auth.getInFileName();
      }
      
      htAuthors.setDataItem(0, rowNdx, authID, authName, hdtPerson);
      htAuthors.setCheckboxValue(1, rowNdx, authID > 0);
      
      boolean boolVal;
      
      switch (isInFileName)
      {
        case True :  boolVal = true; break;
        case False : boolVal = false; break;
        default :    boolVal = (rowNdx == 0);
      }
      
      htAuthors.setCheckboxValue(2, rowNdx, boolVal);
      htAuthors.setCheckboxValue(3, rowNdx, workCtrlr.htAuthors.getCheckboxValue(2, rowNdx));
      htAuthors.setCheckboxValue(4, rowNdx, workCtrlr.htAuthors.getCheckboxValue(3, rowNdx));
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

  @FXML private void btnRegenerateClick()
  {
    String ext, year, fileName, newFileName = "";
         
    ext = FilenameUtils.getExtension(tfOrigFile.getText());
    if (ext.length() == 0)
      ext = FilenameUtils.getExtension(tfNewFile.getText());

    year = tfYear.getText();

    ArrayList<FileNameAuthor> authors = new ArrayList<>();
    
    for (int rowNdx = 0; rowNdx < htAuthors.getDataRowCount(); rowNdx++)
    {
      HyperTableRow row = htAuthors.getRowByRowNdx(rowNdx);
      
      if ((row.getRecord() != null) || (row.getText(0).length() > 0))
        if (htAuthors.getCheckboxValue(2, row))
          authors.add(new FileNameAuthor(row.getText(0), htAuthors.getCheckboxValue(3, row), htAuthors.getCheckboxValue(4, row)));
    }
    
    fileName = HDT_WorkFile.makeFileName(authors, year, tfFileTitle.getText(), ext);
    
    if (fileName.length() == 0)
    {
      tfNewFile.setText("");
      return;
    }
    
    boolean nameTaken = true;
    int ctr = 0;
    
    while (nameTaken)
    {
      ctr++;
      if (ctr >= 1000)
      {
        newFileName = fileName;
        break;
      }
      
      newFileName = FilenameUtils.getBaseName(fileName) + ((ctr == 1) ? "" : "_" + String.valueOf(1000 + (ctr % 1000)).substring(1, 4)) + 
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

    WorkTypeEnum enumVal = curWork.getWorkTypeValue();
    
    switch (enumVal)
    {
      case wtBook: case wtChapter: case wtNone: case wtPaper:

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Adobe PDF file (*.pdf)", "*.pdf"));       
        break;
        
      default :
        break;
    }
    
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files (*.*)", "*.*"));
    
    fileChooser.setInitialDirectory(db.getPath(PREF_KEY_UNENTERED_PATH, null).toFile());

    FilePath chosenFile = new FilePath(fileChooser.showOpenDialog(getStage()));

    useChosenFile(chosenFile);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private void useChosenFile(FilePath chosenFile)
  {    
    if (FilePath.isEmpty(chosenFile)) return;
    
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

    rbCurrent.setDisable(db.getRootFilePath().isSubpath(chosenFile) == false);
    
    if (rbCurrent.isSelected() && rbCurrent.isDisabled())
      rbMove.setSelected(true);

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
        if (curBD.getAuthors().isEmpty())
          gotData = extractDataFromPdf(appPrefs.getBoolean(PREF_KEY_AUTO_RETRIEVE_BIB, true), false);
    
    if ((gotData == false) && appPrefs.getBoolean(PREF_KEY_AUTO_OPEN_PDF, true))
      btnLaunchClick();
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private boolean extractDataFromPdf(boolean doWebQuery, boolean doMerge)
  {
    BibData bd = null;
    boolean dontLaunchPdf = true;
    
    if (FilePath.isEmpty(origFilePath)) return false;
    if (origFilePath.exists() == false) return false;
    if (getMediaType(origFilePath).toString().contains("pdf") == false) return false;
    
    httpClient.stop();
    btnStop.setVisible(false);
    progressBar.setVisible(false);
    
    PdfMetadata md = new PdfMetadata();
    
    try
    {
      BibUtils.getPdfMetadata(origFilePath, md);
    } 
    catch (IOException | XMPException e)
    {
      messageDialog("Error: " + e.getMessage(), mtError);
      return false;
    }

    List<String> isbns = md.bd.getMultiStr(bfISBNs);
    
    String doi = md.bd.getStr(bfDOI);
    String isbn = "";
    
    if (isbns.size() > 0)
      isbn = isbns.get(0);        
    
    if ((doi.length() == 0) && (isbn.length() == 0))
    {
      bd = new BibDataStandalone();
      BibUtils.extractDOIandISBNs(origFilePath.getNameOnly().toString(), bd);
      doi = bd.getStr(bfDOI);
      
      if (bd.getMultiStr(bfISBNs).size() > 0)
        isbn = bd.getMultiStr(bfISBNs).get(0);
      
      bd = null;
    }

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
    int ndx = 0; for (String isbnStr : isbns)
    {
      htISBN.setDataItem(0, ndx, -1, isbnStr, hdtNone);
      ndx++;
    }
         
    if (doWebQuery && dontLaunchPdf)
      pdfBD = md.extractBibData();
    else
    {
      if (doMerge)
      {
        try
        {
          doMerge(md.extractBibData());
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
        populateFieldsFromBibData(md.extractBibData(), true);
      }
    }

    return dontLaunchPdf;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private void doMerge(BibData bd) throws IOException
  {
    MergeWorksDialogController mwd = null;
    getBibDataFromGUI();
    
    mwd = MergeWorksDialogController.create("Merge Information From PDF File", curBD, bd, null, null, false, false);
    
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
    final String finalDOI = doi;
    
    lblAutoPopulated.setText("");
    btnStop.setVisible(true);
    progressBar.setVisible(true);
    
    JsonHttpClient.getObjAsync(getCrossrefUrl(null, doi), httpClient, jsonObj ->
    {
      BibData bibData = BibData.createFromCrossrefJSON(jsonObj);
      
      if (bibData != null)
      {          
        tfDOI.setText(finalDOI);
        
        populateFieldsFromBibData(bibData, true);
        lblAutoPopulated.setText("Fields have been auto-populated from Crossref using doi: " + finalDOI); 
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
      
      btnStop.setVisible(false);
      progressBar.setVisible(false);
      pdfBD = null;
      
    }, e ->
    {
      lblAutoPopulated.setText("");
      btnStop.setVisible(false);
      progressBar.setVisible(false);
      
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
    final String finalISBN = it.next();
    
    lblAutoPopulated.setText("");
    btnStop.setVisible(true);
    progressBar.setVisible(true);
    
    JsonHttpClient.getObjAsync(getGoogleUrl(null, finalISBN), httpClient, jsonObj ->
    {
      BibData bibData = BibData.createFromGoogleJSON(jsonObj);
      
      if (bibData != null)
      {          
        populateFieldsFromBibData(bibData, true);
        lblAutoPopulated.setText("Fields have been auto-populated from Google Books using isbn: " + finalISBN);
        btnStop.setVisible(false);
        progressBar.setVisible(false); 
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
                    
          btnStop.setVisible(false);
          progressBar.setVisible(false);                        
        }
      }
      
      pdfBD = null;
       
    }, e ->
    {
      lblAutoPopulated.setText("");
      btnStop.setVisible(false);
      progressBar.setVisible(false);
      
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

  public void btnDOIClick()
  {
    String doi = BibUtils.matchDOI(tfDOI.getText());
    if (doi.length() == 0) return;
    
    lblAutoPopulated.setText("");
    btnStop.setVisible(true);
    progressBar.setVisible(true);
    
    JsonHttpClient.getObjAsync(getCrossrefUrl(null, doi), httpClient, jsonObj ->
    {
      BibData bd = BibData.createFromCrossrefJSON(jsonObj);
  
      btnStop.setVisible(false);
      progressBar.setVisible(false);
      
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
      btnStop.setVisible(false);
      progressBar.setVisible(false);
      
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

  public void mnuISBNClick(String isbn)
  {
    lblAutoPopulated.setText("");
    btnStop.setVisible(true);
    progressBar.setVisible(true);
    
    JsonHttpClient.getObjAsync(getGoogleUrl(null, isbn), httpClient, jsonObj ->
    {
      BibData bd = BibData.createFromGoogleJSON(jsonObj);
      
      btnStop.setVisible(false);
      progressBar.setVisible(false);
      
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
      btnStop.setVisible(false);
      progressBar.setVisible(false);
      
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
    btnStop.setVisible(false);
    progressBar.setVisible(false);    
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private static boolean addAuthorToTable(int rowNdx, PersonName authorName, boolean editor, boolean trans, HDT_Person author, HyperTable htAuthors, boolean hasShowInFileCol)
  {
    if (authorName.isEmpty()) return false;
    
    if (author != null)
    {
      htAuthors.setDataItem(0, rowNdx, author.getID(), author.listName(), hdtPerson);
      htAuthors.setCheckboxValue(1, rowNdx, true);
    }
    else
    {
      String authorStr = authorName.getLastFirst();
      
      htAuthors.getPopulator(0).addEntry(null, -1, authorStr);
      htAuthors.setDataItem(0, rowNdx, -1, authorStr, hdtPerson);
    }
    
    int addend = hasShowInFileCol ? 1 : 0;
    
    if (editor)
      htAuthors.setCheckboxValue(2 + addend, rowNdx, true);
    
    if (trans)
      htAuthors.setCheckboxValue(3 + addend, rowNdx, true);
    
    return true;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public static void loadFromBibAuthors(BibAuthors bibAuthors, HyperTable htAuthors, boolean hasShowInFileCol)
  {
    if (bibAuthors == null) return;
    if (bibAuthors.isEmpty()) return;

    ArrayList<PersonName> nameList = new ArrayList<>();
    ArrayList<HDT_Person> personList = new ArrayList<>();
    HashMap<PersonName, Boolean> nameToEd = new HashMap<>();
    HashMap<PersonName, Boolean> nameToTr = new HashMap<>();

    bibAuthors.getListsForWorkMerge(nameList, personList, nameToEd, nameToTr);
        
    int rowNdx = 0;
    
    htAuthors.clear();
    htAuthors.getPopulator(0).populate(null, false);
    
    for (int ndx = 0; ndx < nameList.size(); ndx++)
    {
      PersonName name = nameList.get(ndx);
      
      if (addAuthorToTable(rowNdx, name, nameToEd.get(name), nameToTr.get(name), personList.get(ndx), htAuthors, hasShowInFileCol))
        rowNdx++;
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
      
    ArrayList<String> isbns = new ArrayList<>();
    for (int rowNdx = 0; rowNdx < htISBN.getDataRowCount(); rowNdx++)
      isbns.add(htISBN.getText(0, rowNdx));
    
    curBD.setMultiStr(bfISBNs, isbns);
        
    curBD.getAuthors().setAllFromTable(htAuthors.getAuthorGroups(curWork, 0, 2, 3, 4));
    
    return curBD;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public void populateFieldsFromBibData(BibData bd, boolean populateAuthors)
  {   
    if (bd != curBD)
      curBD.copyAllFieldsFrom(bd, populateAuthors);
    
    if (curBD.entryTypeNotEmpty())
      cbEntryType.setValue(curBD.getEntryType());
    
    tfYear.setText(curBD.getStr(bfYear));
    
    alreadyChangingTitle = true;
    tfTitle.setText(curBD.getStr(bfTitle));
    alreadyChangingTitle = false;
      
    taMisc.setText(curBD.getStr(bfMisc));
    
    HDT_WorkType workType = curBD.getWorkType();
    
    if (workType == null)
      hcbType.selectID(-1);
    else
      hcbType.selectID(workType.getID());
    
    tfDOI.setText(curBD.getStr(bfDOI));
    
    htISBN.clear();
    int ndx = 0; for (String isbnStr : curBD.getMultiStr(bfISBNs))
    {
      htISBN.setDataItem(0, ndx, -1, isbnStr, hdtNone);
      ndx++;
    }
    
    if (populateAuthors)
      loadFromBibAuthors(curBD.getAuthors(), htAuthors, true);
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
        String typeName = db.getBibLibrary().type().getUserReadableName();
        String msg = "The current work record is not associated with a " + typeName + " entry. Create one now?\n";
        
        msg = msg + "Otherwise, existing information for these fields will be lost: ";
        
        String fieldsStr = "";
        
        for (BibFieldEnum extField : extFields)
          fieldsStr = fieldsStr + (fieldsStr.length() > 0 ? ", " : "") + BibData.getFieldName(extField);
                
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
      {
        messageDialog("Internal error #82709", mtError);
        return false;
      }
      
      return true;
    }

    if (rbCurrent.isSelected())
    {
      if (chkKeepFilenameUnchanged.isSelected())
        newFilePath = origFilePath;
      else
        newFilePath = origFilePath.getDirOnly().resolve(new FilePath(tfNewFile.getText()));
    }
    else
    {
      if (chkKeepFilenameUnchanged.isSelected())
        newFilePath = HDT_Work.getBasePathForWorkTypeID(hcbType.selectedID()).resolve(origFilePath.getNameOnly()); 
      else
        newFilePath = HDT_Work.getBasePathForWorkTypeID(hcbType.selectedID()).resolve(new FilePath(tfNewFile.getText()));
    }
    
    HDT_RecordWithPath existingFile = HyperPath.getFileFromFilePath(newFilePath);
    
    if (existingFile != null)
    {
      if (existingFile instanceof HDT_MiscFile)
      {
        messageDialog("New file name is already in use as a miscellaneous file, record ID: " + existingFile.getID(), mtError);
        return false;
      }
      
      HDT_WorkFile existingWorkFile = (HDT_WorkFile)existingFile;
      
      if (newWorkFile == null)
      {
        if (existingWorkFile.works.isEmpty())
        {
          messageDialog("Internal error #79002", mtError);
          return false;
        }

        int oldWorkFileID = -1;
        if (oldWorkFile != null)
          oldWorkFileID = oldWorkFile.getID();
        
        if (oldWorkFileID == existingWorkFile.getID())
          newWorkFile = existingWorkFile;          
        else
        {
          messageDialog("New file name is already in use as a work file, work record ID: " + existingWorkFile.works.get(0).getID(), mtError);
          return false;
        }
      }
      else
      {     
        if (newWorkFile.getID() != existingFile.getID())
        {
          messageDialog("Another work file already has that file name, record ID: " + existingFile.getID(), mtError);
          return false;
        }
      }
    }
    else
    {
      if ((newWorkFile == null) && (oldWorkFile != null))
        if (oldWorkFile.works.size() > 1)
          if (confirmDialog("The same file that was associated with this work is associated with or works as well. Should these also be updated?"))
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
            {
              messageDialog("Internal error #67830", mtError);
              return false;
            }
            
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
            {
              messageDialog("Internal error #67830", mtError);
              return false;
            }
            
            curWork.addWorkFile(newWorkFile.getID(), true, true);
          }
        }
        else if (oldWorkFile == newWorkFile)
        {
          if (origFilePath.equals(newFilePath) == false)
          {
            success = origFilePath.moveTo(newFilePath, true);
            if (success == false) return false;
           
            db.unmapFilePath(origFilePath);
            newWorkFile.getPath().assign(HyperPath.getFolderFromFilePath(newFilePath.getDirOnly(), true), newFilePath.getNameOnly());
          }
          else
            success = true;          
        }
        else
        {
          if (oldWorkFile != null)
          {
            messageDialog("Unable to move the file. Reason: Cannot change assignment from one file to another that is already assigned to a different file record.", mtError);
            return false;
          }
          
          success = newWorkFile.getPath().moveToFolder(HyperPath.getFolderFromFilePath(newFilePath.getDirOnly(), true).getID(), true, true, newFilePath.getNameOnly().toString());
          if (success) curWork.addWorkFile(newWorkFile.getID(), true, true);
        }        
      }
    }
    catch (IOException e)
    {
      messageDialog("Unable to " + (rbCopy.isSelected() ? "copy" : "move") + "/rename the file. Reason: " + e.getMessage(), mtError);
      return false;      
    }
    
    if (success == false)
    {
      messageDialog("Unable to " + (rbCopy.isSelected() ? "copy" : "move") + "/rename the file.", mtError);
      return false;           
    }
    
    if (oldWorkFile != null)
      if (newWorkFile.getID() != oldWorkFile.getID())
        db.getObjectList(rtWorkFileOfWork, curWork, true).remove(oldWorkFile);
    
    return true;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public boolean getCreateEntry()
  {
    return chkCreateBibEntry.isVisible() && chkCreateBibEntry.isSelected();
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public EntryType getEntryType()
  {
    EntryType entryType = cbEntryType.getValue();
    
    if (entryType != null)
    {
      switch (entryType)
      {
        case etUnentered : case etOther : case etNone : entryType = null; break;
        default: break;        
      }
    }
    
    return entryType;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

}