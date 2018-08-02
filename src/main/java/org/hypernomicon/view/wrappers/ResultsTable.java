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

package org.hypernomicon.view.wrappers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hypernomicon.model.HyperDB.Tag;
import org.hypernomicon.model.records.HDT_Base;
import org.hypernomicon.model.records.HDT_RecordType;
import org.hypernomicon.view.dialogs.SelectColumnsDialogController;
import org.hypernomicon.view.dialogs.SelectColumnsDialogController.TypeCheckBox;
import org.hypernomicon.view.wrappers.HyperTable.HyperMenuItem;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.records.HDT_Record.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.util.Util.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class ResultsTable implements RecordListView
{
  public TableView<ResultsRow> tv;
  private boolean datesAdded = false;
  public static final ArrayList<ColumnGroup> colGroups = new ArrayList<ColumnGroup>();
  public static ColumnGroup generalGroup;
  private List<HyperMenuItem> contextMenuItems;

//---------------------------------------------------------------------------

  public static class ColumnGroupItem
  {
    public ColumnGroupItem(Tag tag, String caption)
    {
      this.tag = tag;
      this.caption = caption;
    }
    
    public Tag tag;
    public String caption;
    public TableColumn<ResultsRow, ? extends ResultCellValue<? extends Comparable<?>>> col;
  }
  
//---------------------------------------------------------------------------
  
  public static class ColumnGroup
  {
    public ColumnGroup(HDT_RecordType type, String caption, Set<Tag> tags)
    {
      this.type = type;
      this.caption = caption;
      
      for (Tag tag : tags)
        items.add(new ColumnGroupItem(tag, db.getTagHeader(tag)));
    }
    
    public <Comp_T extends Comparable<Comp_T>> void setColumns(TableColumn<ResultsRow, ResultCellValue<Comp_T>> col, Tag tag)
    {
      for (ColumnGroupItem item : items)
        if (item.tag == tag)
          item.col = col;
    }
    
    public HDT_RecordType type;
    public String caption;
    public ArrayList<ColumnGroupItem> items = new ArrayList<ColumnGroupItem>();
    public TypeCheckBox checkBox;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @FunctionalInterface public static interface StringToComparable<Comp_T>
  {
    public Comp_T get(String str);
  }
  
  public static class ResultCellValue<Comp_T extends Comparable<Comp_T>> implements Comparable<ResultCellValue<Comp_T>>
  {
    private String text;
    private Comparable<Comp_T> sortVal = null;
    private StringToComparable<Comp_T> strToComp = null;
    
    public ResultCellValue(String text, Comparable<Comp_T> sortVal)
    {
      this.text = text;
      this.sortVal = sortVal;
    }
    
    public ResultCellValue(String text, StringToComparable<Comp_T> strToComp)
    {
      this.text = text;
      this.strToComp = strToComp;
    }
    
    public ObservableValue<ResultCellValue<Comp_T>> getObservable() { return new SimpleObjectProperty<ResultCellValue<Comp_T>>(this); } 
    
    @Override public String toString() { return text; }

    @SuppressWarnings("unchecked")
    @Override public int compareTo(ResultCellValue<Comp_T> other)
    {
      if (strToComp != null)
        return strToComp.get(text).compareTo(other.strToComp.get(other.text));
      
      return sortVal.compareTo((Comp_T) other.sortVal);
    }    
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public ResultsTable(TableView<ResultsRow> tvResults)
  {
    tv = tvResults;
       
    tv.setItems(FXCollections.observableArrayList());
    contextMenuItems = new ArrayList<>();
    
    tv.setPlaceholder(new Label("There are no query results to display."));
    tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    
    initColumns();
       
    tv.setRowFactory(theTV ->
    {
      final TableRow<ResultsRow> row = new TableRow<>();

      row.setOnMouseClicked(mouseEvent ->
      {
        if ((mouseEvent.getButton().equals(MouseButton.PRIMARY)) && (mouseEvent.getClickCount() == 2))
          dblClick(row.getItem());
      });
      
      row.itemProperty().addListener((observable, oldValue, newValue) ->
      {
        if (newValue == null)
          row.setContextMenu(null);
        else
          row.setContextMenu(createContextMenu(newValue));
      });
      
      return row;
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private ContextMenu createContextMenu(ResultsRow row)
  {
    boolean visible, noneVisible = true;
    ContextMenu rowMenu = new ContextMenu();
    
    final HDT_Base record = row == null ? null : row.getRecord();
    
    for (HyperMenuItem hItem : contextMenuItems)
    {
      MenuItem newItem = new MenuItem(hItem.caption);
         
      newItem.setOnAction(event ->
      {
        rowMenu.hide();
        
        if (record == null) return;
        hItem.recordHandler.handle(record);
      });
      
      rowMenu.getItems().add(newItem);
      
      visible = false;
      
      if (record != null)
        if ((record.getType() == hItem.recordType) || (hItem.recordType == hdtNone))
          visible = (hItem.condRecordHandler.handle(record));
        
      newItem.setVisible(visible);
      if (visible) noneVisible = false;
    }
    
    if (noneVisible) return null;
    return rowMenu;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void dblClick(ResultsRow row)
  {
    if (row != null)    
      ui.goToRecord(row.getRecord(), true);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void clear()
  {
    tv.getColumns().clear();
    tv.getItems().clear();
    colGroups.clear();
    tv.setContextMenu(null);
    
    initColumns();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private <Comp_T extends Comparable<Comp_T>> ObservableValue<ResultCellValue<Comp_T>> getCustomCellValue(String str, StringToComparable<Comp_T> strToComp)
  {
    return new ResultCellValue<Comp_T>(str, strToComp).getObservable();
  }
  
  public static final double RESULT_COL_MAX_WIDTH = 400.0;
  
  public void initColumns()
  {
    datesAdded = false;
    generalGroup = new ColumnGroup(hdtNone, "General", EnumSet.noneOf(Tag.class));
    ColumnGroupItem item;
    
    colGroups.add(generalGroup);
    
    TableColumn<ResultsRow, ResultCellValue<Integer>> intCol;
    TableColumn<ResultsRow, ResultCellValue<String>> strCol;
    
    intCol = new TableColumn<ResultsRow, ResultCellValue<Integer>>("ID");
    
    intCol.setCellValueFactory(cellData -> getCustomCellValue(cellData.getValue().getRecordID(), str -> Integer.valueOf(parseInt(str, -1))));        
    intCol.setMaxWidth(RESULT_COL_MAX_WIDTH);
    tv.getColumns().add(intCol);
    
    item = new ColumnGroupItem(tagNone, "ID");
    generalGroup.items.add(item);
    item.col = intCol;
    
    strCol = new TableColumn<ResultsRow, ResultCellValue<String>>("Name");
    strCol.setCellValueFactory(cellData -> getCustomCellValue(cellData.getValue().getRecordName(), str -> makeSortKeyByType(str, hdtWork)));  
    strCol.setMaxWidth(RESULT_COL_MAX_WIDTH);
    tv.getColumns().add(strCol);

    item = new ColumnGroupItem(tagNone, "Name");
    generalGroup.items.add(item);
    item.col = strCol;
    
    strCol = new TableColumn<ResultsRow, ResultCellValue<String>>("Type");
    strCol.setCellValueFactory(cellData -> getCustomCellValue(cellData.getValue().getRecordType(), str -> str.trim().toLowerCase()));
    strCol.setMaxWidth(RESULT_COL_MAX_WIDTH);
    tv.getColumns().add(strCol);

    item = new ColumnGroupItem(tagNone, "Type");
    generalGroup.items.add(item);
    item.col = strCol;
    
    strCol = new TableColumn<ResultsRow, ResultCellValue<String>>("Search Key");
    strCol.setCellValueFactory(cellData -> getCustomCellValue(cellData.getValue().getSearchKey(), str -> str.trim().toLowerCase()));
    strCol.setMaxWidth(RESULT_COL_MAX_WIDTH);
    tv.getColumns().add(strCol);
    strCol.setVisible(false);
    
    item = new ColumnGroupItem(tagNone, "Search Key");
    generalGroup.items.add(item);
    item.col = strCol;
    
    strCol = new TableColumn<ResultsRow, ResultCellValue<String>>("Sort Key");
    strCol.setCellValueFactory(cellData -> 
    {
      String sortKey = cellData.getValue().getSortKey();
      return new ResultCellValue<String>(sortKey, sortKey).getObservable(); 
    });
    
    strCol.setMaxWidth(RESULT_COL_MAX_WIDTH);
    tv.getColumns().add(strCol);
    strCol.setVisible(false);
    
    item = new ColumnGroupItem(tagNone, "Sort Key");
    generalGroup.items.add(item);
    item.col = strCol;
    
    Node showHideColumnsButton = tv.lookup(".show-hide-columns-button");
    
    if (showHideColumnsButton != null)
      showHideColumnsButton.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> 
      {
        SelectColumnsDialogController.create("Select Columns").showModal();
        event.consume();
      });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void addDateColumns()
  {   
    if (datesAdded) return;
    datesAdded = true;
    
    Platform.runLater(() ->
    {
      TableColumn<ResultsRow, ResultCellValue<Instant>> col;
      ColumnGroupItem item;

      col = new TableColumn<ResultsRow, ResultCellValue<Instant>>("Date created");
      col.setCellValueFactory(cellData -> cellData.getValue().getCreationDateCellValue().getObservable());
      tv.getColumns().add(col);
      
      item = new ColumnGroupItem(tagNone, "Date created");
      generalGroup.items.add(item);
      item.col = col;
      
      col = new TableColumn<ResultsRow, ResultCellValue<Instant>>("Date modified");      
      col.setCellValueFactory(cellData -> cellData.getValue().getModifiedDateCellValue().getObservable());
      tv.getColumns().add(col);
      
      item = new ColumnGroupItem(tagNone, "Date modified");
      generalGroup.items.add(item);
      item.col = col;
      
      col = new TableColumn<ResultsRow, ResultCellValue<Instant>>("Date accessed");
      col.setCellValueFactory(cellData -> cellData.getValue().getViewDateCellValue().getObservable());
      tv.getColumns().add(col);
      
      item = new ColumnGroupItem(tagNone, "Date accessed");
      generalGroup.items.add(item);
      item.col = col;
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public TableColumn<ResultsRow, ResultCellValue<String>> addTagColumn(Tag tag)
  {
    TableColumn<ResultsRow, ResultCellValue<String>> col = new TableColumn<ResultsRow, ResultCellValue<String>>(db.getTagHeader(tag));
    StringToComparable<String> strToComp;
    
    if (tag == tagTitle)
      strToComp = str -> makeSortKeyByType(str, hdtWork);
    else
      strToComp = str -> str.trim().toLowerCase();
      
    col.setCellValueFactory(cellData -> getCustomCellValue(cellData.getValue().getTagText(tag), strToComp));
    
    col.setMaxWidth(RESULT_COL_MAX_WIDTH);
    
    tv.getColumns().add(col);
    
    return col;
  }

//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 
  
  @Override public HyperMenuItem addContextMenuItem(HDT_RecordType recordType, String caption, RecordListView.RecordHandler handler)
  {
    return addCondContextMenuItem(recordType, caption, record -> true, handler);
  }

//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 

  @Override public HyperMenuItem addCondContextMenuItem(HDT_RecordType recordType, String caption, RecordListView.CondRecordHandler condHandler, RecordListView.RecordHandler handler)
  {
    HyperMenuItem mnu;
       
    mnu = new HyperMenuItem(caption);
    mnu.recordType = recordType;
    mnu.condRecordHandler = condHandler;
    mnu.recordHandler = handler;
    
    contextMenuItems.add(mnu);
    return mnu;
  }

//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 

  public HDT_Base selectedRecord()
  {
    ResultsRow row = tv.getSelectionModel().getSelectedItem();
    HDT_Base record = null;
    
    if (row != null) record = row.getRecord();
    return record;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}