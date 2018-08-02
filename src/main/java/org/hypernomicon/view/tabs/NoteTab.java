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

package org.hypernomicon.view.tabs;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import static javafx.scene.control.Control.*;

import java.util.HashSet;
import java.util.Set;

import org.hypernomicon.model.records.HDT_Base;
import org.hypernomicon.model.records.HDT_Folder;
import org.hypernomicon.model.records.HDT_Note;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.items.StrongLink;
import org.hypernomicon.model.records.HDT_RecordType;
import org.hypernomicon.model.records.HDT_RecordWithConnector;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithDescription;
import org.hypernomicon.util.filePath.FilePath;
import org.hypernomicon.view.HyperView.TextViewInfo;
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

//---------------------------------------------------------------------------

public class NoteTab extends HyperNodeTab<HDT_Note, HDT_Note>
{
  public SplitMenuButton btnFolder = new SplitMenuButton();
  public Button btnBrowse = new Button("...");
  public TextField tfFolder = new TextField();
  private TabPane tabPane;
  private Tab tabSubnotes, tabMentioners;
  public HyperTable htParents, htSubnotes, htMentioners;  
  public FilePath folderPath;
  private HDT_Note curNote;

  @Override public HDT_RecordType getType()              { return hdtNote; }
  @Override public void enable(boolean enabled)          { ui.tabNotes.getContent().setDisable(enabled == false); }
  @Override public void focusOnSearchKey()               { ctrlr.focusOnSearchKey(); }
  @Override public void findWithinDesc(String text)      { ctrlr.hilite(text); }
  @Override public TextViewInfo getMainTextInfo()        { return ctrlr.getMainTextInfo(); }
  @Override public void setRecord(HDT_Note activeRecord) { curNote = activeRecord; }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public boolean update()
  {
    int ndx;
    
    if (db.isLoaded() == false) return false;
    
    clear();
    
    if (curNote == null)
    {
      enable(false);
      return false;
    }
      
    if (!ctrlr.update(curNote)) return false;

    tfFolder.setText(curNote.getFolderStr());
    
    if (curNote.folder.get() == null)
      folderPath = null;
    else
      folderPath = curNote.folder.get().getPath().getFilePath();

  // Populate parent notes
  // ---------------------

    ndx = 0; for (HDT_Note otherNote : curNote.parentNotes)
    {
      htParents.setDataItem(2, ndx, otherNote.getID(), otherNote.name(), hdtNote);
      ndx++;
    }

  // Populate child notes
  // --------------------

    ndx = 0; for (HDT_Note subNote : curNote.subNotes)
    {
      htSubnotes.setDataItem(1, ndx, subNote.getID(), subNote.name(), hdtNote);
      htSubnotes.setDataItem(2, ndx, subNote.getID(), subNote.getMainText().getPlainForDisplay(), hdtNote);
      htSubnotes.setDataItem(3, ndx, subNote.getID(), subNote.getFolderStr(), hdtNote);
      
      ndx++;
    }
    
    tabSubnotes.setText(subnotesTabTitle + " (" + curNote.subNotes.size() + ")");
    
  // Populate mentioners
  // -------------------
    updateMentioners();   
        
    return true; 
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private void updateMentioners()
  {
    htMentioners.clear();
    tabMentioners.setText(mentionersTabTitle);
    
    if (db.isLoaded() == false) return;
    if (curNote == null) return;
    
    if (db.reindexingMentioners())
    {
      htMentioners.setDataItem(1, 0, -1, "(Indexing in progress)", hdtNone);
      return;
    }
      
    Set<HDT_Base> mentioners = db.getMentionerSet(curNote, true);
    mentioners = removeDupMentioners(mentioners);
    
    int rowNdx = 0; for (HDT_Base mentioner : mentioners)
    {
      htMentioners.setDataItem(0, rowNdx, mentioner.getID(), "", mentioner.getType());
      htMentioners.setDataItem(1, rowNdx, mentioner.getID(), mentioner.getCBText(), mentioner.getType());
      
      if (mentioner.hasDesc())
        htMentioners.setDataItem(2, rowNdx, mentioner.getID(), HDT_RecordWithDescription.class.cast(mentioner).getDesc().getPlainForDisplay(), mentioner.getType());
      
      rowNdx++;
    }
    
    tabMentioners.setText(mentionersTabTitle + " (" + rowNdx + ")");
    
    if ((curNote.subNotes.size() == 0) && (htMentioners.getDataRowCount() > 0))
      tabPane.getSelectionModel().select(tabMentioners);
    
    if ((curNote.subNotes.size() > 0) && (htMentioners.getDataRowCount() == 0))
      tabPane.getSelectionModel().select(tabSubnotes);      
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private Set<HDT_Base> removeDupMentioners(Set<HDT_Base> mentioners)
  {
    Set<HDT_Base> output = new HashSet<>();
    Set<StrongLink> usedLinks = new HashSet<>();
    
    mentioners.forEach(mentioner ->
    {
      if (mentioner.isUnitable())
      {
        HDT_RecordWithConnector spoke = (HDT_RecordWithConnector) mentioner;
        StrongLink link = spoke.getLink();
        
        if (spoke.getLink() != null)
        {
          if (usedLinks.contains(link) == false)
          {
            usedLinks.add(link);
            
            if (link.getDebate() != null)        output.add(link.getDebate());
            else if (link.getPosition() != null) output.add(link.getPosition());
            else if (link.getConcept() != null)  output.add(link.getConcept());
            else                                 output.add(link.getNote());
          }
        }
        else
          output.add(mentioner);        
      }
      else
        output.add(mentioner);
    });
    
    return output;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private void addFolderMenuItem(String text, EventHandler<ActionEvent> handler)
  {
    MenuItem menuItem = new MenuItem(text);
    menuItem.setOnAction(handler);
    btnFolder.getItems().add(menuItem);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override protected void init(TabEnum tabEnum)
  {
    this.tabEnum = tabEnum;
    ctrlr.init(hdtNote, this);
    ctrlr.tvParents.getColumns().remove(2);
    
    tabSubnotes = new Tab("Sub-Notes", ctrlr.tvLeftChildren);
    tabMentioners = new Tab("Mentioners", ctrlr.tvRightChildren);
    tabPane = new TabPane(tabSubnotes, tabMentioners);
    
    AnchorPane.setLeftAnchor(tabPane, 0.0);
    AnchorPane.setRightAnchor(tabPane, 0.0);
    AnchorPane.setTopAnchor(tabPane, 0.0);
    AnchorPane.setBottomAnchor(tabPane, 0.0);

    ctrlr.apLowerPane.getChildren().setAll(tabPane);
    
    ctrlr.tvLeftChildren.getColumns().get(1).setText("Sub-Notes Under This Note");
    ctrlr.tvLeftChildren.getColumns().get(2).setText("Text");
    ctrlr.tvLeftChildren.getColumns().add(new TableColumn<HyperTableRow, HyperTableCell>("Folder"));
    
    ctrlr.tvRightChildren.getColumns().get(0).setText("Type");
    ctrlr.tvRightChildren.getColumns().get(1).setText("Name of Record");
    ctrlr.tvRightChildren.getColumns().add(new TableColumn<HyperTableRow, HyperTableCell>("Description"));
    
    ctrlr.spMain.setDividerPosition(1, 0.8);
    
    btnFolder.setText("Folder:");
    addFolderMenuItem("Show in system explorer", event -> launchFile(folderPath));
    addFolderMenuItem("Show in file manager", event -> ui.goToFileInManager(folderPath));
    addFolderMenuItem("Copy path to clipboard", event -> copyToClipboard(folderPath.toString()));
    addFolderMenuItem("Unassign folder", event -> 
    {
      if (ui.cantSaveRecord(true)) return;
      curNote.folder.set(null);
      ui.update();
    });
    
    AnchorPane ap = new AnchorPane();
    ap.getChildren().addAll(btnFolder, tfFolder, btnBrowse);
    AnchorPane.setLeftAnchor(btnFolder, 0.0);
    AnchorPane.setRightAnchor(btnBrowse, 0.0);
    AnchorPane.setLeftAnchor(tfFolder, 75.0);
    AnchorPane.setRightAnchor(tfFolder, 28.0);
    tfFolder.setEditable(false);
    
    GridPane.setColumnIndex(ap, 1);
    ctrlr.gpToolBar.getColumnConstraints().get(0).setMinWidth(510.0);
    ctrlr.gpToolBar.getColumnConstraints().get(0).setMaxWidth(510.0);
    ctrlr.gpToolBar.getColumnConstraints().get(0).setHgrow(javafx.scene.layout.Priority.NEVER);
    
    ctrlr.gpToolBar.getColumnConstraints().get(1).setMinWidth(USE_COMPUTED_SIZE);
    ctrlr.gpToolBar.getColumnConstraints().get(1).setMaxWidth(USE_COMPUTED_SIZE);
    ctrlr.gpToolBar.getColumnConstraints().get(1).setPrefWidth(USE_COMPUTED_SIZE);
    ctrlr.gpToolBar.getColumnConstraints().get(1).setHgrow(javafx.scene.layout.Priority.ALWAYS);
    ctrlr.gpToolBar.getChildren().set(1, ap);

    htParents = new HyperTable(ctrlr.tvParents, 2, true, PREF_KEY_HT_NOTE_PARENTS);
    
    htParents.addActionCol(ctGoBtn, 2);
    htParents.addActionCol(ctBrowseBtn, 2);
    htParents.addCol(hdtNote, ctDropDownList);
    
    htParents.addRemoveMenuItem();
    htParents.addChangeOrderMenuItem(true);
    
    htSubnotes = new HyperTable(ctrlr.tvLeftChildren, 2, true, PREF_KEY_HT_NOTE_SUB);
    
    htSubnotes.addActionCol(ctGoNewBtn, 2);
    htSubnotes.addCol(hdtNote, ctNone);
    htSubnotes.addCol(hdtNote, ctNone);
    htSubnotes.addCol(hdtNote, ctNone);
    
    htMentioners = new HyperTable(ctrlr.tvRightChildren, 1, false, PREF_KEY_HT_NOTE_MENTIONERS);
    
    htMentioners.addCol(hdtNone, ctIcon);
    htMentioners.addCol(hdtNone, ctNone);
    htMentioners.addCol(hdtNone, ctNone);
    
    db.addMentionsNdxCompleteHandler(() -> updateMentioners());
    
    btnFolder.setOnAction(event -> launchFile(folderPath));
    btnBrowse.setOnAction(event -> browseClick());
    
    htSubnotes.addCondContextMenuItem(hdtNote, "Launch subnote folder", 
      record -> HDT_Note.class.cast(record).folder.getID() > 0,
      record -> launchFile(HDT_Note.class.cast(record).folder.get().getPath().getFilePath()));
    
    htSubnotes.addContextMenuItem(hdtNote, "Go to subnote",
      record -> ui.goToRecord(record, true));
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  void browseClick()
  {
    if (ui.cantSaveRecord(true)) return;
    
    DirectoryChooser dirChooser = new DirectoryChooser();
    
    if (FilePath.isEmpty(folderPath))
    {
      HDT_Folder folder = curNote.getDefaultFolder();
      
      if (folder != null)
        dirChooser.setInitialDirectory(folder.getPath().getFilePath().toFile()); 
      else
        dirChooser.setInitialDirectory(db.getPath(PREF_KEY_TOPICAL_PATH, null).toFile());
    }
    else
      dirChooser.setInitialDirectory(folderPath.toFile());
         
    dirChooser.setTitle("Select Folder");
       
    FilePath filePath = new FilePath(dirChooser.showDialog(app.getPrimaryStage()));

    if (FilePath.isEmpty(filePath) == false)
    {
      HDT_Folder folder = HyperPath.getFolderFromFilePath(filePath, true);
            
      if (folder == null)
      {
        messageDialog("You must choose a subfolder of the main database folder.", mtError);
        return;
      }
      
      curNote.folder.set(folder);
      ui.update();
    }
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public void clear()
  {
    ctrlr.clear();
    
    htParents.clear();
    htSubnotes.clear();
    htMentioners.clear();
    tfFolder.clear();
    
    tabSubnotes.setText(subnotesTabTitle);
    tabMentioners.setText(mentionersTabTitle);
  }
  
  private static final String subnotesTabTitle = "Sub-Notes";
  private static final String mentionersTabTitle = "Records linking to here";

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public boolean saveToRecord(boolean showMessage)
  {    
    if (!ctrlr.save(curNote, showMessage, this)) return false;
    
    curNote.setParentNotes(htParents);
    
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
      case hdtNote :
        
        HDT_Note subNote = db.createNewBlankRecord(hdtNote);

        subNote.parentNotes.add(curNote);

        ui.goToRecord(subNote, false);
        
        break;
      
      default:
        break;
    }
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public void setDividerPositions()
  {
    setDividerPosition(ctrlr.spMain, PREF_KEY_NOTE_TOP_VERT, 0);
    setDividerPosition(ctrlr.spMain, PREF_KEY_NOTE_BOTTOM_VERT, 1);
  }  
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override public void getDividerPositions()
  {
    getDividerPosition(ctrlr.spMain, PREF_KEY_NOTE_TOP_VERT, 0);
    getDividerPosition(ctrlr.spMain, PREF_KEY_NOTE_BOTTOM_VERT, 1);
  }  

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

}