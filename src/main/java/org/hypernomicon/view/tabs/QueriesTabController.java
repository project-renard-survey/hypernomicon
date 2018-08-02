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

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.controlsfx.control.MasterDetailPane;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import org.hypernomicon.HyperTask;
import org.hypernomicon.App;
import org.hypernomicon.model.Exceptions.*;
import org.hypernomicon.model.HDI_Schema;
import org.hypernomicon.model.HyperDB.Tag;
import org.hypernomicon.model.records.*;
import org.hypernomicon.model.records.HDT_Record.HyperDataCategory;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithDescription;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithPath;
import org.hypernomicon.model.relations.HyperSubjList;
import org.hypernomicon.model.relations.RelationSet.RelationType;
import org.hypernomicon.queryEngines.*;
import org.hypernomicon.querySources.CombinedFilteredQuerySource;
import org.hypernomicon.querySources.CombinedUnfilteredQuerySource;
import org.hypernomicon.querySources.DatasetQuerySource;
import org.hypernomicon.querySources.FilteredQuerySource;
import org.hypernomicon.querySources.QuerySource;
import org.hypernomicon.querySources.QuerySource.QuerySourceType;
import org.hypernomicon.view.HyperFavorites.QueryFavorite;
import org.hypernomicon.view.HyperFavorites.QueryRow;
import org.hypernomicon.view.HyperView.TextViewInfo;
import org.hypernomicon.view.dialogs.NewQueryFavDialogController;
import org.hypernomicon.view.mainText.MainTextWrapper;
import org.hypernomicon.view.populators.*;
import org.hypernomicon.view.populators.Populator.CellValueType;
import org.hypernomicon.view.reports.ReportEngine;
import org.hypernomicon.view.reports.ReportTable;
import org.hypernomicon.view.wrappers.*;
import org.hypernomicon.view.wrappers.CheckBoxOrCommandListCell.CheckBoxOrCommand;
import org.hypernomicon.view.wrappers.ResultsTable.ColumnGroup;
import org.hypernomicon.view.wrappers.ResultsTable.ResultCellValue;
import org.hypernomicon.queryEngines.QueryEngine.QueryType;
import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.Const.*;
import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.records.HDT_Record.HyperDataCategory.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;
import static org.hypernomicon.view.populators.ConnectivePopulator.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.*;
import static org.hypernomicon.view.wrappers.HyperTableCell.*;
import static org.hypernomicon.view.wrappers.ResultsTable.*;
import static org.hypernomicon.view.populators.Populator.CellValueType.*;
import static org.hypernomicon.queryEngines.QueryEngine.QueryType.*;
import static org.hypernomicon.view.populators.GenericOperandPopulator.*;
import static org.hypernomicon.view.previewWindow.PreviewWindow.PreviewSource.*;
import static org.hypernomicon.view.populators.BooleanPopulator.*;

//---------------------------------------------------------------------------

public class QueriesTabController extends HyperTab<HDT_Base, HDT_Base>
{
  public class QueryView
  {
    private MasterDetailPane spMain, spLower;
    private TableView<HyperTableRow> tvFields;
    private TableView<ResultsRow> tvResults;
    private AnchorPane apDescription, apResults;
    
    public HyperTable htFields;
    public ResultsTable resultsTable;
    public ReportTable reportTable;
    
    public ArrayList<ResultsRow> resultsBackingList;
    private Set<HDT_RecordType> resultTypes;
    private Set<Tag> resultTags;
       
    private Tab tab;
    private QueryFavorite fav = null;
    private HDT_Base curResult = null;
    
    private boolean programmaticFavNameChange = false,
                    programmaticFieldChange = false,
                    recordMode = true;
    
    public boolean inRecordMode()           { return recordMode; }
    public HDT_Base activeRecord()          { return curResult; }
    private void setRecord(HDT_Base record) { curResult = record; }
    
  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   
    
    public QueryView()
    {
      resultTypes = EnumSet.noneOf(HDT_RecordType.class);
      
      EventHandler<ActionEvent> onAction = event ->
      {
        btnExecute.requestFocus(); 
        btnExecuteClick(true); 
      };
           
      FXMLLoader loader = new FXMLLoader(App.class.getResource("view/tabs/QueryView.fxml"));

      try { tab = new Tab("New query", loader.load()); } 
      catch (IOException e)
      {
        messageDialog("Internal error #90203", mtError);
        return;
      }
      
      tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
      tab.setOnCloseRequest(event -> deleteView((Tab) event.getSource()));
            
      QueryViewController ctrlr = loader.getController();
      
      spMain = ctrlr.spMain;
      spLower = ctrlr.spLower;
      tvFields = ctrlr.tvFields;
      tvResults = ctrlr.tvResults;
      apDescription = ctrlr.apDescription;
      apResults = ctrlr.apResults;
            
      htFields = new HyperTable(tvFields, 1, true, "");
      
      HyperTable.loadColWidthsForTable(tvFields, PREF_KEY_HT_QUERY_FIELDS);
      
      htFields.autoCommitListSelections = true;
      
      htFields.addColAltPopulatorWithUpdateHandler(hdtNone, ctDropDownList, new QueryTypePopulator(), (row, cellVal, nextColNdx, nextPopulator) ->
      {
        int query = row.getID(1);
        QueryType qt = QueryType.codeToVal(HyperTableCell.getCellID(cellVal));
        
        boolean tempPFC = programmaticFieldChange;
        programmaticFieldChange = true;
        
        if ((qt == QueryType.qtReport) ||
            ((query != QUERY_ANY_FIELD_CONTAINS) && 
             (query != QUERY_WITH_NAME_CONTAINING) && 
             (query != QUERY_LIST_ALL)))
          row.updateCell(nextColNdx, new HyperTableCell(-1, "", nextPopulator.getRecordType(row)));
        
        QueryPopulator qp = (QueryPopulator)nextPopulator;
        qp.setQueryType(row, qt, this);
        
        programmaticFieldChange = tempPFC;
        
        if (programmaticFieldChange == false)
          htFields.edit(row, 1);        
      });
      
      htFields.addColAltPopulatorWithUpdateHandler(hdtNone, ctDropDownList, new QueryPopulator(), (row, cellVal, nextColNdx, nextPopulator) ->
      {
        boolean tempPFC = programmaticFieldChange;
        programmaticFieldChange = true;
        
        if (queryChange(cellVal.getID(), row))
          row.updateCell(nextColNdx, new HyperTableCell(-1, "", nextPopulator.getRecordType(row)));
        
        programmaticFieldChange = tempPFC;
        
        if (programmaticFieldChange == false)
          htFields.edit(row, 2);        
      });   
      
      htFields.addColAltPopulatorWithBothHandlers(hdtNone, ctDropDown, new VariablePopulator(), onAction, (row, cellVal, nextColNdx, nextPopulator) ->
      {
        if (op1Change(cellVal, row))
        {
          boolean tempPFC = programmaticFieldChange;
          programmaticFieldChange = true;
          
          row.updateCell(nextColNdx, new HyperTableCell(-1, "", nextPopulator.getRecordType(row)));
          
          if ((HyperTableCell.getCellID(cellVal) >= 0) && (VariablePopulator.class.cast(nextPopulator).getPopulator(row) instanceof GenericOperandPopulator))
          {
            GenericOperandPopulator gop = (GenericOperandPopulator) VariablePopulator.class.cast(nextPopulator).getPopulator(row);
            row.updateCell(nextColNdx, gop.getChoice(EQUAL_TO_OPERAND_ID));
            if (tempPFC == false)
              htFields.edit(row, 4);        
          }
          else
          {
            if (tempPFC == false)
              htFields.edit(row, 3);                  
          }
          
          programmaticFieldChange = tempPFC;
        }
      });
      
      htFields.addColAltPopulatorWithBothHandlers(hdtNone, ctDropDown, new VariablePopulator(), onAction, (row, cellVal, nextColNdx, nextPopulator) ->
      {
        boolean tempPFC = programmaticFieldChange;
        programmaticFieldChange = true;

        if (op2Change(cellVal, row))
          row.updateCell(nextColNdx, new HyperTableCell(-1, "", nextPopulator.getRecordType(row)));
        
        programmaticFieldChange = tempPFC;
        
        if (programmaticFieldChange == false)
          htFields.edit(row, 4);                  
      });
      
      htFields.addColAltPopulatorWithActionHandler(hdtNone, ctDropDown, new VariablePopulator(), onAction);
      htFields.addColAltPopulator(hdtNone, ctDropDownList, new ConnectivePopulator());
      
      htFields.getColumns().forEach(col -> col.setDontCreateNewRecord(true));
      
      resultsTable = new ResultsTable(tvResults);
      reportTable = new ReportTable(this);
      
      tvResults.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
      {
        refreshView(newValue.intValue());        
      });
           
      RecordListView.addDefaultMenuItems(resultsTable);
      
      resultsTable.addContextMenuItem(hdtNone, "Remove from query results", (record) ->
      {
        ArrayList<ResultsRow> rows = new ArrayList<>();
        
        rows.addAll(resultsTable.tv.getSelectionModel().getSelectedItems());
        
        rows.forEach(row -> resultsTable.tv.getItems().remove(row));
      });
      
      scaleNodeForDPI(ctrlr);
      setFontSize(ctrlr);
    }
   
  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   
   
    public void refreshView()
    {
      if (inRecordMode())
        refreshView(tvResults.getSelectionModel().getSelectedIndex());
      else
      {
        webView.getEngine().loadContent(reportTable.getHtmlForCurrentRow());
                
        ui.updateBottomPanel(false);     
      }
    }
    
    private void refreshView(int selRowNdx)
    {
      if (results().size() > 0)
      {
        ui.updateBottomPanel(false);
        curResult = null;
        
        if (selRowNdx > -1)
        {
          ResultsRow row = results().get(selRowNdx);
          curResult = row.getRecord();
        }
        
        if (curResult != null)
        {
          textToHilite = getTextToHilite();
          
          String mainText = "";
          if (curResult.hasDesc())
            mainText = HDT_RecordWithDescription.class.cast(curResult).getDesc().getHtml();
          
          MainTextWrapper.setReadOnlyHTML(getHtmlEditorText(mainText), webView.getEngine(), new TextViewInfo(), getRecordToHilite());
          
          if (curResult.getType() == hdtWork)
          {
            HDT_Work work = (HDT_Work) curResult;
            previewWindow.setPreview(pvsQueryTab, work.getPath().getFilePath(), work.getStartPageNum(), work.getEndPageNum(), work);            
          }
          else if (curResult.getType() == hdtMiscFile)
          {
            HDT_MiscFile miscFile = (HDT_MiscFile) curResult;
            previewWindow.setPreview(pvsQueryTab, miscFile.getPath().getFilePath(), -1, -1, miscFile);
          }
          else if (curResult.getType() == hdtWorkFile)
            previewWindow.setPreview(pvsQueryTab, HDT_RecordWithPath.class.cast(curResult).getPath().getFilePath(), 1, -1, null);
          else
            previewWindow.setPreview(pvsQueryTab, null, -1, -1, null);
        }
        else
        {
          webView.getEngine().loadContent("");          
          previewWindow.setPreview(pvsQueryTab, null, -1, -1, null);
        }
      }
      else
      {
        webView.getEngine().loadContent("");
        
        if (curResult == null)
          ui.updateBottomPanel(false);
      }
      
      setFavNameToggle(fav != null);
      
      programmaticFavNameChange = true;      
      if (fav == null) 
        tfName.setText("");
      else
        tfName.setText(fav.name);
      programmaticFavNameChange = false;
    }

    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   

    private void setFavNameToggle(boolean selected)
    {
      if (selected)
        btnToggleFavorite.setText("Remove from favorites");
      else
        btnToggleFavorite.setText("Add to favorites");
    }

    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   

    private void favNameChange()
    {
      if (programmaticFavNameChange) return;
      fav = null;
      setFavNameToggle(false);
    }
      
    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   

    // Returns true if search was completed
    
    private boolean showSearch(boolean doSearch, QueryType type, int query, QueryFavorite newFav, HyperTableCell op1, HyperTableCell op2, String caption)
    {   
      if (db.isLoaded() == false) return false;
           
      fav = newFav;
      
      if (newFav != null) invokeFavorite(newFav);
      
      if (doSearch)
      {
        if (type != null)
        {
          programmaticFieldChange = true;
          
          htFields.clear();
          htFields.selectID(0, null, type.getCode());
   
          if (query > -1)
          {
            htFields.selectID(1, null, query);
            
            if (op1 != null)
            {
              if (getCellID(op1) > 0)
                htFields.selectID(2, null, getCellID(op1));
              else if (getCellText(op1).length() == 0)
                htFields.selectType(2, null, getCellType(op1));
              else
                htFields.setDataItem(2, 0, getCellID(op1), getCellText(op1), getCellType(op1));
            }
            
            if (op2 != null)
            {
              if (getCellID(op2) > 0)
                htFields.selectID(3, null, getCellID(op2));
              else if (getCellText(op2).length() == 0)
                htFields.selectType(3, null, getCellType(op2));
              else
                htFields.setDataItem(3, 0, getCellID(op2), getCellText(op2), getCellType(op2));
            }
          }
          
          programmaticFieldChange = false;
          
          htFields.selectRow(0);
        }
    
        if (caption.length() > 0)        
          tab.setText(caption);

        return btnExecuteClick(caption.length() == 0);
      }
      
      return false;
    }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    private void btnFavoriteClick()
    {
      int colNdx, rowNdx;
      QueryRow row;

      if (db.isLoaded() == false) return;
      
      if (fav == null)
      {
        NewQueryFavDialogController ctrlr = NewQueryFavDialogController.create("Add Query Favorite", tfName.getText());
        
        if (ctrlr.showModal() == false) return;

        fav = new QueryFavorite();

        fav.name = ctrlr.getNewName();
        fav.autoexec = ctrlr.getAutoExec();
        
        for (rowNdx = 0; rowNdx < htFields.getDataRowCount(); rowNdx++)
        {
          row = new QueryRow();
          
          for (colNdx = 0; colNdx < 6; colNdx++)
          {
            HyperTableCell cell = htFields.getRowByRowNdx(rowNdx).getCell(colNdx);
            row.cells[colNdx] = new HyperTableCell(getCellID(cell), getCellText(cell), getCellType(cell)); 
          }
          
          fav.rows.add(row);
        }
        
        ui.mnuQueries.getItems().add(ui.new FavMenuItem(fav));
        
        programmaticFavNameChange = true;      
        tfName.setText(fav.name);
        programmaticFavNameChange = false;
        
        setFavNameToggle(true);     
      }
        
      else
      {
        fav.removeFromList(ui.mnuQueries.getItems());
        fav = null;
        
        programmaticFavNameChange = true;      
        tfName.setText("");
        programmaticFavNameChange = false;
        
        setFavNameToggle(false);     
      }
      
      ui.updateFavorites();
    }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

    private void invokeFavorite(QueryFavorite fav)
    {
      int rowNdx, colNdx;
      this.fav = fav;
      
      if (db.isLoaded() == false) return;            
      
      programmaticFieldChange = true;
      
      htFields.clear();
      
      for (rowNdx = 0; rowNdx < fav.rows.size(); rowNdx++)
      {
        for (colNdx = 0; colNdx < 6; colNdx++)
        {
          HyperTableCell cell = fav.rows.get(rowNdx).cells[colNdx];
          htFields.setDataItem(colNdx, rowNdx, getCellID(cell), getCellText(cell), getCellType(cell));
        }
      }
      
      refreshView();
      htFields.selectRow(0);
      
      programmaticFieldChange = false;
    }
    
  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    public void clear()
    {
      resultsTable.clear();
      resultTags = EnumSet.noneOf(Tag.class);
      resultsBackingList = new ArrayList<>();
      resultTypes = EnumSet.noneOf(HDT_RecordType.class);
      
      switchToRecordMode();
    }

    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   

    private void setCaption()
    {
      for (int rowNdx = 0; rowNdx < htFields.getDataRowCount(); rowNdx++)
      {
        for (int colNdx = 1; colNdx <= 4; colNdx++)
        {
          if (htFields.getText(colNdx, rowNdx).length() > 0)
            tab.setText(htFields.getText(colNdx, rowNdx)); 
        }
      }
    }
    
    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------
    
    private void switchToReportMode()
    {
      if (recordMode == false) return;
      
      removeFromAnchor(tvResults);
      reportTable.setParent(apResults);
      
      recordMode = false;
      curResult = null;
      
      webView.getEngine().loadContent("");
      
      ui.updateBottomPanel(false);
        
      updateCB();
    }

    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   

    private void switchToRecordMode()
    {
      if (recordMode) return;
      
      reportTable.removeFromParent();
      apResults.getChildren().add(tvResults);
      
      recordMode = true;
    }

    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   

    private void executeReport(boolean setCaption)
    {
      switchToReportMode();
      
      if (setCaption)
        setCaption();
      
      HyperTableRow row = htFields.getRowByRowNdx(0);
      
      ReportEngine reportEngine = ReportEngine.createEngine(row.getID(1));
      
      if (reportEngine == null)
      {
        reportTable.clear();
        return;
      }
      
      reportTable.format(reportEngine);
      
      task = new HyperTask() { @Override protected Boolean call() throws Exception
      {
        updateMessage("Generating report...");
        updateProgress(0, 1);
        
        reportEngine.generate(task, row.getCell(2), row.getCell(3), row.getCell(4));
        
        return true;
      }};
      
      if (!HyperTask.performTaskWithProgressDialog(task)) return; 
      
      reportTable.inject(reportEngine);
      
      if (reportEngine.alwaysShowDescription() && (reportEngine.getRows().size() > 0)) 
        chkShowDesc.setSelected(true);
    }
    
    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   
    
 // if any of the queries are unfiltered, they will all be treated as unfiltered
    
    private boolean btnExecuteClick(boolean setCaption)
    {
      if (db.isLoaded() == false) return false;
      
      for (int rowNdx = 0; rowNdx < htFields.getDataRowCount(); rowNdx++)
      {
        if (QueryType.codeToVal(htFields.getID(0, rowNdx)) == QueryType.qtReport)
        {          
          while (htFields.getDataRowCount() > (rowNdx + 1))
            htFields.removeRow(htFields.getRowByRowNdx(rowNdx + 1));
          
          for (int ndx = 0; ndx < rowNdx; ndx++)
            htFields.removeRow(htFields.getRowByRowNdx(ndx));
          
          executeReport(setCaption);
          return true;
        }
      }
      
      switchToRecordMode();
      
      QuerySource combinedSource;
      Set<HDT_Base> filteredRecords = new LinkedHashSet<HDT_Base>();
      FilteredQuerySource fqs;
      DatasetQuerySource dqs;
      boolean hasFiltered = false, hasUnfiltered = false, needMentionsIndex = false; 
      EnumSet<HDT_RecordType> unfilteredTypes = EnumSet.noneOf(HDT_RecordType.class);
      LinkedHashMap<Integer, QuerySource> sources = new LinkedHashMap<>();
      HDT_RecordType singleType = null;

      for (int rowNdx = 0; rowNdx < htFields.getDataRowCount(); rowNdx++)
      {
        int query = htFields.getID(1, rowNdx); 
      
        if (query < 0) continue;
        
        HyperTableRow row = htFields.getRowByRowNdx(rowNdx);
        QueryEngine<? extends HDT_Base> engine = typeToEngine.get(getQueryType(row));
        
        if (queryNeedsMentionsIndex(query, engine))
          needMentionsIndex = true;
      }
      
      if (needMentionsIndex)
      {
        if (db.waitUntilRebuildIsDone() == false)
        {
          if (resultsBackingList == null)
            clear();
          
          return false;
        }
      }
      
      resultsTable.clear();
      webView.getEngine().loadContent("");
      resultTypes = EnumSet.noneOf(HDT_RecordType.class);
      
      if (setCaption)
        setCaption();
      
      // Build list of sources and list of unfiltered types
      
      for (int rowNdx = 0; rowNdx < htFields.getDataRowCount(); rowNdx++)
      {
        QuerySource source = getSource(htFields.getRowByRowNdx(rowNdx));
        
        if (source == null) continue;
        
        sources.put(rowNdx, source);
        
        if (source.sourceType() == QuerySourceType.QST_filteredRecords)
        {
          hasFiltered = true;
          fqs = (FilteredQuerySource) source;
          unfilteredTypes.add(fqs.recordType());
        }
        
        else if (source.sourceType() == QuerySourceType.QST_recordsByType)
        {
          dqs = (DatasetQuerySource) source;
          unfilteredTypes.add(dqs.recordType());
          hasUnfiltered = true;
        }
        
        else if (source.sourceType() == QuerySourceType.QST_allRecords)
        {
          hasUnfiltered = true;  
        
          unfilteredTypes = EnumSet.allOf(HDT_RecordType.class);
          
          unfilteredTypes.remove(hdtNone);
          unfilteredTypes.remove(hdtAuxiliary);
          unfilteredTypes.remove(hdtHub);
        }
      }      
            
      // Generate combined record source
      
      if (hasUnfiltered)
      {
        combinedSource = new CombinedUnfilteredQuerySource(unfilteredTypes);
        if (unfilteredTypes.size() == 1) singleType = (HDT_RecordType) unfilteredTypes.toArray()[0];
      }
      else if (hasFiltered)    
      {
        for (QuerySource src : sources.values())
          if (src.sourceType() == QuerySourceType.QST_filteredRecords)
          {
            fqs = (FilteredQuerySource) src;
            
            if (singleType == null)
              singleType = fqs.recordType();
            else if ((singleType != hdtNone) && (singleType != fqs.recordType()))
              singleType = hdtNone;
            
            fqs.addAllTo(filteredRecords);
          }
        
        combinedSource = new CombinedFilteredQuerySource(filteredRecords);
      }
      else
        combinedSource = new CombinedUnfilteredQuerySource(EnumSet.noneOf(HDT_RecordType.class));
      
      int total = combinedSource.count();
      if (singleType == null) singleType = hdtNone;
      final HDT_RecordType finalSingleType = singleType;
      
      // Evaluate record queries
      
      task = new HyperTask() { @Override protected Boolean call() throws Exception
      {
        boolean firstCall = true, result, add = false;
        
        int recordNdx;

        HDT_Base record;
        
        resultTags = EnumSet.noneOf(Tag.class);
        resultsBackingList = new ArrayList<>();
        
        updateMessage("Running query...");
        updateProgress(0, 1);
    
        for (recordNdx = 0; recordNdx < total; recordNdx++)
        {
          if (isCancelled()) 
            throw new TerminateTaskException();
          
          if ((recordNdx % 50) == 0)
            updateProgress(recordNdx, total);
          
          record = combinedSource.getRecord(recordNdx);
          
          for (Integer rowNdx : sources.keySet())
          {
            QuerySource source = sources.get(rowNdx);
                          
            if (source.containsRecord(record))
            {
              curQuery = htFields.getID(1, rowNdx); 
              
              if (curQuery > -1)
              {
                HyperTableRow row = htFields.getRowByRowNdx(rowNdx);
                
                param1 = row.getCell(2); 
                param2 = row.getCell(3); 
                param3 = row.getCell(4);
                
                result = evaluate(record, row, finalSingleType != hdtNone, firstCall, recordNdx == (total - 1));
                firstCall = false;
              }
              else
                result = false;
    
              if (rowNdx == 0)
                add = result;
              else if (htFields.getID(5, rowNdx - 1) == OR_CONNECTIVE_ID)
                add = (add || result);
              else
                add = (add && result);
            }
          }
    
          if (add)
            addRecord(record, false);
        }
       
        return true;
      }};
      
      if (!HyperTask.performTaskWithProgressDialog(task)) return false; 
      
      Platform.runLater(() -> resultsTable.tv.setItems(FXCollections.observableList(resultsBackingList)));
      
      for (Tag tag : resultTags)
        if (tag != tagName)
        {
          TableColumn<ResultsRow, ResultCellValue<String>> col = resultsTable.addTagColumn(tag);
          
          for (ColumnGroup colGroup : colGroups)
            colGroup.setColumns(col, tag);
        }
      
      return true;
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    public void addRecord(HDT_Base record, boolean addToObsList)
    {
      HDT_RecordType recordType = record.getType();
      
      if (resultTypes.contains(recordType) == false)
      {
        resultTypes.add(recordType);
        
        if (recordType.getDisregardDates() == false)
          resultsTable.addDateColumns();
        
        Set<Tag> tags = record.getAllTags();
        tags.remove(tagHub);
        tags.remove(tagPictureCrop);
               
        resultTags.addAll(tags);
        
        colGroups.add(new ColumnGroup(recordType, db.getTypeName(recordType), tags));
        
        if (addToObsList)
        {
          for (Tag tag : tags)
          {
            if (tag != tagName)
            {
              TableColumn<ResultsRow, ResultCellValue<String>> col = resultsTable.addTagColumn(tag);
              
              for (ColumnGroup colGroup : colGroups)
                colGroup.setColumns(col, tag);
            }
          }
        }
      }

      if (addToObsList)
        resultsTable.tv.getItems().add(new ResultsRow(record));
      else        
        resultsBackingList.add(new ResultsRow(record));
    }
    
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

    private boolean queryNeedsMentionsIndex(int query, QueryEngine<? extends HDT_Base> engine)
    {
      switch (query)
      {
        case QUERY_WITH_NAME_CONTAINING : 
        case QUERY_ANY_FIELD_CONTAINS :
        case QUERY_LIST_ALL :
        case QUERY_WHERE_FIELD :
        case QUERY_WHERE_RELATIVE :
          return false;
          
        default :
          return engine.needsMentionsIndex(query);
      }
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    private QueryType getQueryType(HyperTableRow row)
    {
      return QueryType.codeToVal(row.getID(0));
    }
    
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

    private QuerySource getSource(HyperTableRow row)
    {
      QueryType qt = getQueryType(row);
      
      if (qt == null) return null;
      
      return typeToEngine.get(qt).getSource(row.getID(1), row.getCell(2), row.getCell(3), row.getCell(4));
    }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

    private HDT_Base getRecordToHilite()
    {
      int rowNdx, queryType, query;
      HyperTableCell param;

      for (rowNdx = 0; rowNdx < htFields.getDataRowCount(); rowNdx++)
      {
        queryType = htFields.getID(0, rowNdx);
        if (queryType == QueryType.qtAllRecords.getCode())
        {
          query = htFields.getID(1, rowNdx);
          switch (query)
          {
            case AllQueryEngine.QUERY_LINKING_TO_RECORD : case AllQueryEngine.QUERY_MATCHING_RECORD :
              
              param = htFields.getRowByRowNdx(rowNdx).getCell(3);
              HDT_Base record = HyperTableCell.getRecord(param);
              if (record != null) return record;
              break;
              
            default :
              break;
          }
        }
      }
         
      return null;    
    }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    private String getTextToHilite()
    {
      int rowNdx, colNdx, query;
      HyperTableCell param;
      String cellText = "";
      VariablePopulator vp;

      for (rowNdx = 0; rowNdx < htFields.getDataRowCount(); rowNdx++)
      {
        query = htFields.getID(1, rowNdx); 

        if (query > -1)
        {
          for (colNdx = 2; colNdx <= 4; colNdx++)
          {
            vp = (VariablePopulator)htFields.getPopulator(colNdx);
            if (vp.getRestricted(htFields.getRowByRowNdx(rowNdx)) == false)
            {
              param = htFields.getRowByRowNdx(rowNdx).getCell(colNdx);
              cellText = HyperTableCell.getCellText(param);
              if (cellText.length() > 0)
                return cellText;
            }
          }
        }
      }
      
      return "";
    }
    
  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    //returns true if subsequent cells need to be updated
    private boolean queryChange(int query, HyperTableRow row)
    {
      if (db.isLoaded() == false) return false;
      
      VariablePopulator vp1 = (VariablePopulator)htFields.getPopulator(2);
      VariablePopulator vp2 = (VariablePopulator)htFields.getPopulator(3);
      VariablePopulator vp3 = (VariablePopulator)htFields.getPopulator(4);
      boolean samePop = false;
         
      switch (query)
      {
        case QUERY_WITH_NAME_CONTAINING : case QUERY_ANY_FIELD_CONTAINS :
          
          samePop = (vp1.getRestricted(row) == false);
          
          clearOperands(row, samePop ? 2 : 1);
          
          vp1.setRestricted(row, false);
          
          return false;
          
        case QUERY_LIST_ALL :
          
          clearOperands(row, 1);
          return true;
          
        case QUERY_WHERE_RELATIVE :
          
          if (vp1.getPopulator(row) != null)
          {
            if (vp1.getPopulator(row).getValueType() == CellValueType.cvtRelation)
            {
              RelationPopulator rp = (RelationPopulator) vp1.getPopulator(row);
              if (rp.getRecordType(row) == row.getType(0))
                samePop = true;
            }
          }
          
          if (samePop == false) 
          {
            clearOperands(row, 1);
            vp1.setPopulator(row, new RelationPopulator(row.getType(0)));
          }
          
          return true;       
          
        case QUERY_WHERE_FIELD :
          
          if (vp1.getPopulator(row) != null)
          {
            if (vp1.getPopulator(row).getValueType() == CellValueType.cvtTagItem)
            {
              TagItemPopulator tip = (TagItemPopulator) vp1.getPopulator(row);
              if (tip.getRecordType(null) == row.getType(0))
                samePop = true;
            }
          }
          
          if (samePop == false) 
          {
            clearOperands(row, 1);
            vp1.setPopulator(row, new TagItemPopulator(row.getType(0)));
          }
          
          return true;
          
        default :
          break;
      }
      
      typeToEngine.get(getQueryType(row)).queryChange(query, row, vp1, vp2, vp3);
      return true;
    }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    //returns true if subsequent cells need to be updated
    private boolean op1Change(HyperTableCell op1, HyperTableRow row)
    {
      if (db.isLoaded() == false) return false;
      
      VariablePopulator vp1 = (VariablePopulator)htFields.getPopulator(2);
      VariablePopulator vp2 = (VariablePopulator)htFields.getPopulator(3);
      VariablePopulator vp3 = (VariablePopulator)htFields.getPopulator(4);

      int query = row.getID(1);
      
      switch (query)
      {
        case QUERY_WHERE_RELATIVE :     
        case QUERY_WHERE_FIELD : 
          
          vp2.setPopulator(row, new GenericOperandPopulator());
          return true;
                 
        default :
          break;
      }
      
      typeToEngine.get(getQueryType(row)).op1Change(query, op1, row, vp1, vp2, vp3);
      return true;
    }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    // returns true if subsequent cells need to be updated
    private boolean op2Change(HyperTableCell op2, HyperTableRow row)
    {
      if (db.isLoaded() == false) return false;    
      
      VariablePopulator vp1 = (VariablePopulator)htFields.getPopulator(2);
      VariablePopulator vp2 = (VariablePopulator)htFields.getPopulator(3);
      VariablePopulator vp3 = (VariablePopulator)htFields.getPopulator(4);

      HDT_RecordType recordType, subjType, objType = hdtNone;
      RelationType relType;
      int query = row.getID(1);
      HyperTableCell op1 = row.getCell(2);

      switch (query)
      {
        case QUERY_WHERE_RELATIVE :
          
          objType = row.getType(0);
          relType = RelationType.codeToVal(row.getID(2));
                
          switch (row.getID(3))
          {
            case EQUAL_TO_OPERAND_ID : case NOT_EQUAL_TO_OPERAND_ID :
              
              vp3.setPopulator(row, new StandardPopulator(db.getSubjType(relType)));
              return true;
            
            default :
              clearOperands(row, 3);
              vp3.setRestricted(row, false);
              return true;
          }
          
        case QUERY_WHERE_FIELD :
          
          recordType = row.getType(0);
          HyperDataCategory cat = hdcString;
          boolean catSet = false;
                  
          Set<HDI_Schema> schemas = db.getSchemasByTag(Tag.getTagByNum(row.getID(2))); 

          for (HDI_Schema schema : schemas)
          {
            relType = schema.getRelType();
            
            if (relType == rtNone)
              subjType = hdtNone;
            else
              subjType = db.getSubjType(relType);
            
            if ((recordType == hdtNone) || (recordType == subjType))
            {
              if (catSet == false)
              {
                cat = schema.getCategory();
                catSet = true;
                
                if ((cat == hdcPointerMulti) || (cat == hdcPointerSingle) || (cat == hdcAuthors))
                  objType = db.getObjType(relType);
              }
              else
              {
                if ((cat == hdcPointerMulti) || (cat == hdcPointerSingle) || (cat == hdcAuthors))
                {
                  if ((schema.getCategory() != hdcPointerMulti) && (schema.getCategory() != hdcPointerSingle) && (schema.getCategory() != hdcAuthors))
                    cat = hdcString;
                  else
                  {
                    if (objType != db.getObjType(relType))
                      cat = hdcString;
                  }
                }
                else if (cat != schema.getCategory())
                  cat = hdcString;
              }
            }
          }
          
          if ((row.getID(3) != EQUAL_TO_OPERAND_ID) && (row.getID(3) != NOT_EQUAL_TO_OPERAND_ID))
            cat = hdcString;
          
          if ((cat == hdcString) || (cat == hdcPersonName) || (cat == hdcBibEntryKey) || (cat == hdcConnector)) 
          {
            clearOperands(row, 3);
            vp3.setRestricted(row, false);
          }
          else if (cat == hdcBoolean)
            vp3.setPopulator(row, new BooleanPopulator());
          else if (cat == hdcTernary)
            vp3.setPopulator(row, new TernaryPopulator());
          else
            vp3.setPopulator(row, new StandardPopulator(objType));
          
          return true;
          
        default :
          break;
      }    
      
      typeToEngine.get(getQueryType(row)).op2Change(query, op1, op2, row, vp1, vp2, vp3);
      return true;
    }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    public void clearOperands(HyperTableRow row, int startOpNum)
    {
      if ((startOpNum > 3) || (startOpNum < 1))
      {
        messageDialog("Internal error 90087", mtError);
        return;
      }
      
      if (startOpNum < 2) clearOperand(row, 1);
      if (startOpNum < 3) clearOperand(row, 2);
      clearOperand(row, 3);
    }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    private void clearOperand(HyperTableRow row, int opNum)
    {
      VariablePopulator vp = (VariablePopulator)htFields.getPopulator(opNum + 1);
      vp.setPopulator(row, null);
      vp.setRestricted(row, true);
      htFields.setDataItem(opNum + 1, row, -1, "", hdtNone);
    }
    
  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

    private void resetFields()
    {
      programmaticFieldChange = true;
      
      htFields.clear();
      htFields.selectID(0, null, QueryType.qtAllRecords.getCode());
      htFields.selectID(1, null, QUERY_ANY_FIELD_CONTAINS);
      htFields.selectRow(0);
      
      programmaticFieldChange = false;
    }

    //---------------------------------------------------------------------------  
    //---------------------------------------------------------------------------   

    public void updateCB()
    {
      if (cb == null) return;
      
      if (propToUnbind != null)
        cb.itemsProperty().unbindBidirectional(propToUnbind);
        
      if (recordMode == false)
      {
        cb.setItems(null);
        return;
      }
      
      cb.itemsProperty().bindBidirectional(tvResults.itemsProperty());
      
      propToUnbind = tvResults.itemsProperty();
      
      if (cbListenerToRemove != null)
        cb.getSelectionModel().selectedItemProperty().removeListener(cbListenerToRemove);
      
      cb.getSelectionModel().select(tvResults.getSelectionModel().getSelectedItem());
      
      cbListenerToRemove = (observable, oldValue, newValue) ->
      {
        if (newValue != null)
          if (newValue.getRecord() != null)
          { 
            tvResults.getSelectionModel().select(newValue);
            if (noScroll == false) tvResults.scrollTo(newValue);
          }
      };
      
      cb.getSelectionModel().selectedItemProperty().addListener(cbListenerToRemove);
      
      if (tvListenerToRemove != null)
        tvResults.getSelectionModel().selectedItemProperty().removeListener(tvListenerToRemove);
      
      tvListenerToRemove = (observable, oldValue, newValue) ->
      {     
        noScroll = true;
        cb.getSelectionModel().select(newValue);
        noScroll = false;
      };
      
      tvResults.getSelectionModel().selectedItemProperty().addListener(tvListenerToRemove);
    }
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   
 
  public static final int QUERY_WITH_NAME_CONTAINING = 1;
  public static final int QUERY_ANY_FIELD_CONTAINS = 2;
  public static final int QUERY_LIST_ALL = 3;
  public static final int QUERY_WHERE_FIELD = 4;
  public static final int QUERY_WHERE_RELATIVE = 5;
  public static final int QUERY_FIRST_NDX = 6;
  
  @FXML private CheckBox chkShowFields;
  @FXML private CheckBox chkShowDesc;
  @FXML private Button btnToggleFavorite;
  @FXML private TextField tfName;
  @FXML private Button btnClear;
  @FXML private Button btnExecute;
  @FXML private TabPane tabPane;
  @FXML private Tab tabNew;
  @FXML private AnchorPane apDescription;

  private ComboBox<CheckBoxOrCommand> fileBtn = null;
  public ArrayList<QueryView> queryViews;  
  public final static EnumMap<QueryType, QueryEngine<? extends HDT_Base>> typeToEngine = new EnumMap<>(QueryType.class);
  public static HyperTask task;
  private static boolean noScroll = false;
  public boolean clearingViews = false;  
  public static int curQuery;
  public static HyperTableCell param1, param2, param3;
  private String textToHilite = "";
  private ObjectProperty<ObservableList<ResultsRow>> propToUnbind = null;
  private ChangeListener<ResultsRow> cbListenerToRemove = null, tvListenerToRemove = null;
  public WebView webView;
  private ComboBox<ResultsRow> cb;
  SimpleBooleanProperty includeEdited = new SimpleBooleanProperty(),
                        excludeAnnots = new SimpleBooleanProperty();

  @Override public HDT_RecordType getType()              { return hdtNone; }
  @Override public void enable(boolean enabled)          { ui.tabQueries.getContent().setDisable(enabled == false); }
  @Override public boolean update()                      { return true; }
  @Override public void focusOnSearchKey()               { return; }
  @Override public void setRecord(HDT_Base activeRecord) { if (curQV != null) curQV.setRecord(activeRecord); }
  @Override public int getRecordCount()                  { return results().size(); }
  @Override public TextViewInfo getMainTextInfo()        { return new TextViewInfo(MainTextWrapper.getWebEngineScrollPos(webView.getEngine())); }
  @Override public void setDividerPositions()            { return; }
  @Override public void getDividerPositions()            { return; }
  
  @Override public void newClick(HDT_RecordType objType, HyperTableRow row) { return; }
  @Override public boolean saveToRecord(boolean showMessage)                { return false; }
  
  @FXML private void mnuCopyToFolderClick()              { copyFilesToFolder(true); }
  
  public void btnExecuteClick() { curQV.btnExecuteClick(true); }   // if any of the queries are unfiltered, they will all be treated as unfiltered
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   
  
  public ObservableList<ResultsRow> results() 
  { 
    if (curQV == null) return FXCollections.observableArrayList();
    if (curQV.inRecordMode() == false) return FXCollections.observableArrayList();
    
    return curQV.resultsTable.tv.getItems(); 
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  @Override protected void init(TabEnum tabEnum)
  {
    this.tabEnum = tabEnum;
    queryViews = new ArrayList<>();
                
    typeToEngine.put(qtPersons, new PersonQueryEngine());
    typeToEngine.put(qtPositions, new PositionQueryEngine());
    typeToEngine.put(qtConcepts, new ConceptQueryEngine());
    typeToEngine.put(qtWorks, new WorkQueryEngine());
    typeToEngine.put(qtNotes, new NoteQueryEngine());
    typeToEngine.put(qtDebates, new DebateQueryEngine());
    typeToEngine.put(qtArguments, new ArgumentQueryEngine());
    typeToEngine.put(qtInstitutions, new InstitutionQueryEngine());
    typeToEngine.put(qtInvestigations, new InvestigationQueryEngine());
    typeToEngine.put(qtFiles, new FileQueryEngine());
    typeToEngine.put(qtAllRecords, new AllQueryEngine());
    typeToEngine.put(qtReport, new ReportQueryEngine());
    
    btnExecute.setOnAction(event -> btnExecuteClick());
    btnClear.setOnAction(event -> curQV.resetFields());
    btnToggleFavorite.setOnAction(event -> curQV.btnFavoriteClick());
    
    tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
    { 
      if (clearingViews == false) tabPaneChange(newValue);
    });
    
    webView.getEngine().titleProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) ->
    {
      if (curQV.inRecordMode() == false)
      {
        MainTextWrapper.handleJSEvent("", webView.getEngine(), new TextViewInfo());
        return;
      }
      
      HDT_Base record = curQV.resultsTable.selectedRecord();
      if (record == null) return;

      textToHilite = curQV.getTextToHilite();     
      String mainText = "";
            
      if (record.hasDesc())
        mainText = HDT_RecordWithDescription.class.cast(record).getDesc().getHtml();      
      
      MainTextWrapper.handleJSEvent(getHtmlEditorText(mainText), webView.getEngine(), new TextViewInfo());
    });
    
    webView.setOnContextMenuRequested(event -> setHTMLContextMenu());
    
    webView.getEngine().getLoadWorker().stateProperty().addListener((ChangeListener<Worker.State>) (ov, oldState, newState) -> 
    {
      if (newState == Worker.State.SUCCEEDED) 
      {        
        if (textToHilite.length() > 0)
          MainTextWrapper.hiliteText(textToHilite, webView.getEngine());
        
        textToHilite = "";
      }
    });
          
    tfName.textProperty().addListener((observable, oldValue, newValue) ->
    {
      if (newValue == null) return;
      oldValue = safeStr(oldValue);
      if (oldValue.equals(newValue)) return;
      if (curQV == null) return;
      
      curQV.favNameChange();
    });
    
    addFilesButton();
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private void addFilesButton()
  {
    ObservableList<CheckBoxOrCommand> items = FXCollections.observableArrayList();

    items.add(new CheckBoxOrCommand("Include edited works", includeEdited));
    items.add(new CheckBoxOrCommand("Copy files without annotations", excludeAnnots));
    items.add(new CheckBoxOrCommand("Clear Search Results Folder and Add All Results", () -> { mnuCopyAllClick();           getFileBtn().hide(); }));
    items.add(new CheckBoxOrCommand("Clear Search Results Folder",                     () -> { mnuClearSearchFolderClick(); getFileBtn().hide(); }));
    items.add(new CheckBoxOrCommand("Copy Selected to Search Results Folder",          () -> { mnuCopyToFolderClick();      getFileBtn().hide(); }));
    items.add(new CheckBoxOrCommand("Show Search Results Folder",                      () -> { mnuShowSearchFolderClick();  getFileBtn().hide(); }));    

    fileBtn = CheckBoxOrCommand.createComboBox(items, "Files");
    
    fileBtn.setMaxSize(64.0, 24.0);
    fileBtn.setMinSize(64.0, 24.0);
    fileBtn.setPrefSize(64.0, 24.0);
    
    AnchorPane.setTopAnchor(fileBtn, 2.0);
    AnchorPane.setRightAnchor(fileBtn, 0.0);
    AnchorPane.class.cast(getTab().getContent()).getChildren().add(fileBtn);
  }
  
  private ComboBox<CheckBoxOrCommand> getFileBtn() { return fileBtn; }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private void tabPaneChange(Tab newValue)
  {
    QueryView qV = null;
    
    if (newValue == tabNew)
    {
      qV = addQueryView();
      tabPane.getSelectionModel().select(qV.tab);
      safeFocus(qV.tvFields);
    }
    else
    {
      for (QueryView view : queryViews)
        if (view.tab == newValue)
          qV = view;
      
      if (qV == null) return;
    }
    
    if (curQV != null)
    {
      curQV.spMain.showDetailNodeProperty().unbind();
      curQV.spLower.showDetailNodeProperty().unbind();
    }
    
    chkShowFields.setSelected(qV.spMain.isShowDetailNode());
    chkShowDesc.setSelected(qV.spLower.isShowDetailNode());
    
    qV.spMain.showDetailNodeProperty().bind(chkShowFields.selectedProperty());
    qV.spLower.showDetailNodeProperty().bind(chkShowDesc.selectedProperty());
    
    curQV = qV;
    updateCB();
    
    removeFromAnchor(webView);
    curQV.apDescription.getChildren().setAll(webView);
    
    qV.refreshView();
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void deleteView(Tab tab)
  {
    QueryView qV = null;
 
    for (QueryView view : queryViews)
      if (view.tab == tab)
        qV = view;
    
    if (qV == null) return;
    
    HyperTable.saveColWidthsForTable(qV.tvFields, PREF_KEY_HT_QUERY_FIELDS, false);
    queryViews.remove(qV);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public QueryView addQueryView()
  {
    QueryView newQV = new QueryView();
    
    queryViews.add(newQV);
    newQV.resetFields();
    
    return newQV;
  }
    
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   
  
  @Override public void clear()
  {
    clearingViews = true;
    
    Iterator<QueryView> viewIt = queryViews.iterator();
    removeFromAnchor(webView);
    apDescription.getChildren().add(webView);
    
    while (viewIt.hasNext())
    {
      QueryView queryView = viewIt.next();
      HyperTable.saveColWidthsForTable(queryView.tvFields, PREF_KEY_HT_QUERY_FIELDS, false);
      tabPane.getTabs().remove(queryView.tab);
      viewIt.remove();
    }
    
    clearingViews = false;
    
    webView.getEngine().loadContent("");
    QueryView newQV = addQueryView();    
    tabPane.getSelectionModel().select(newQV.tab);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   
  
  @SuppressWarnings("unchecked")
  public <HDT_T extends HDT_Base> boolean evaluate(HDT_T record, HyperTableRow row, boolean searchLinkedRecords, boolean firstCall, boolean lastCall)
  {
    QueryEngine<HDT_T> engine;
    int strNdx;
    String val1, val3;
    VariablePopulator vp3 = (VariablePopulator)curQV.htFields.getPopulator(4);
    CellValueType pm;
  
    ArrayList<String> list;
  
    switch (curQuery)
    {
      case QUERY_WITH_NAME_CONTAINING :
        
        return (record.listName().toUpperCase().indexOf(getCellText(param1).toUpperCase()) >= 0);
  
      case QUERY_ANY_FIELD_CONTAINS :
        list = new ArrayList<String>();
        record.getAllStrings(list, searchLinkedRecords);
  
        val1 = getCellText(param1).toLowerCase();
  
        for (strNdx = 0; strNdx < list.size(); strNdx++)
        {
          if (list.get(strNdx).toLowerCase().indexOf(val1) >= 0)
            return true;
        }
  
        return false;
  
      case QUERY_LIST_ALL :
        return true;
        
      case QUERY_WHERE_RELATIVE :
        
        RelationType relType = RelationType.codeToVal(getCellID(param1));
        if (record.getType() != db.getObjType(relType)) return false;
        
        int opID = getCellID(param2);

        HyperSubjList<HDT_Base, HDT_Base> subjList = db.getSubjectList(relType, record);
        int subjCount = subjList.size();

        if ((opID == IS_EMPTY_OPERAND_ID) || (opID == IS_NOT_EMPTY_OPERAND_ID))
          return ((subjCount == 0) == (opID == IS_EMPTY_OPERAND_ID));
          
        for (HDT_Base subjRecord : subjList)
        {
          switch (opID)
          {         
            case EQUAL_TO_OPERAND_ID : case NOT_EQUAL_TO_OPERAND_ID :
              
              if (subjRecord.getID() == getCellID(param3))
                return (opID == EQUAL_TO_OPERAND_ID);
              
            case CONTAINS_OPERAND_ID : case DOES_NOT_CONTAIN_OPERAND_ID :
              
              if (subjRecord.listName().toLowerCase().contains(getCellText(param3).toLowerCase()))
                return (opID == CONTAINS_OPERAND_ID);
              
            default :
              break;
          }
        }
        
        switch (opID)
        {  
          case EQUAL_TO_OPERAND_ID : case NOT_EQUAL_TO_OPERAND_ID :            
            return (opID == NOT_EQUAL_TO_OPERAND_ID);
            
          case CONTAINS_OPERAND_ID : case DOES_NOT_CONTAIN_OPERAND_ID :            
            return (opID == DOES_NOT_CONTAIN_OPERAND_ID);

          default :            
            return false;
        }
        
      case QUERY_WHERE_FIELD :
        
        Tag tag = Tag.getTagByNum(getCellID(param1));
        HDI_Schema schema = record.getSchema(tag);
        String tagStrVal;
        
        if (schema == null) return false;
        
        pm = (vp3.getPopulator(row) == null) ? cvtVaries : vp3.getPopulator(row).getValueType();
        
        switch (getCellID(param2))
        {
          case EQUAL_TO_OPERAND_ID : case NOT_EQUAL_TO_OPERAND_ID :
                
            switch (pm)
            {
              case cvtRecord :
              
                for (HDT_Base objRecord : db.getObjectList(schema.getRelType(), record, true))
                {
                  if ((objRecord.getID() == getCellID(param3)) && (objRecord.getType() == getCellType(param3)))
                    return (getCellID(param2) == EQUAL_TO_OPERAND_ID);
                }
                
                return (getCellID(param2) == NOT_EQUAL_TO_OPERAND_ID);
                
              case cvtBoolean :

                if ((getCellID(param3) != TRUE_BOOLEAN_ID) && (getCellID(param3) != FALSE_BOOLEAN_ID)) return false;
                
                return ((record.getTagBoolean(tag) == (getCellID(param3) == TRUE_BOOLEAN_ID)) == (getCellID(param2) == EQUAL_TO_OPERAND_ID));
                
              default :
                
                tagStrVal = record.getResultTextForTag(tag);
                if (tagStrVal.length() == 0) return false;
                
                return (tagStrVal.trim().equalsIgnoreCase(getCellText(param3).trim()) == (getCellID(param2) == EQUAL_TO_OPERAND_ID)); 
            }
            
          case CONTAINS_OPERAND_ID : case DOES_NOT_CONTAIN_OPERAND_ID :
            
            val3 = getCellText(param3).trim();
            if (val3.length() == 0) return false;
            
            tagStrVal = record.getResultTextForTag(tag).toLowerCase().trim();
            
            return ((tagStrVal.contains(val3.toLowerCase())) == (getCellID(param2) == CONTAINS_OPERAND_ID));
            
          case IS_EMPTY_OPERAND_ID : case IS_NOT_EMPTY_OPERAND_ID :
            
            switch (pm)
            {
              case cvtRecord :

                return (db.getObjectList(schema.getRelType(), record, true).size() > 0) == (getCellID(param2) == IS_NOT_EMPTY_OPERAND_ID);
                
              case cvtBoolean :
                
                return getCellID(param2) == IS_EMPTY_OPERAND_ID;
                
              default :
                
                tagStrVal = record.getResultTextForTag(tag);
                return (tagStrVal.length() > 0) == (getCellID(param2) == IS_NOT_EMPTY_OPERAND_ID);
            }
        }
  
      default :
        engine = (QueryEngine<HDT_T>) typeToEngine.get(curQV.getQueryType(row));
        return engine.evaluate(record, firstCall, lastCall);        
    }
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public static void addQueries(QueryPopulator pop, HyperTableRow row, QueryType newType)
  {   
    if (newType != QueryType.qtReport)
    {
      pop.addEntry(row, QUERY_WITH_NAME_CONTAINING, "with name containing");
      pop.addEntry(row, QUERY_ANY_FIELD_CONTAINS, "where any field contains");
      pop.addEntry(row, QUERY_LIST_ALL, "list all records");
      pop.addEntry(row, QUERY_WHERE_FIELD, "where field");
      pop.addEntry(row, QUERY_WHERE_RELATIVE, "where set of records having this record as");
    }
    
    QueryEngine<? extends HDT_Base> engine = typeToEngine.get(newType);
    
    engine.addQueries(pop, row);
  }
 
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public boolean showSearch(boolean doSearch, QueryType type, int query, QueryFavorite fav, HyperTableCell op1, HyperTableCell op2, String caption)
  {   
    if (db.isLoaded() == false) return false;
    
    QueryView qV = addQueryView();
    tabPane.getSelectionModel().select(qV.tab);
    
    return qV.showSearch(doSearch, type, query, fav, op1, op2, caption);    
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  @FXML private void mnuCopyAllClick()
  {
    boolean startWatcher = folderTreeWatcher.stop();
    
    mnuClearSearchFolderClick();
    
    if (copyFilesToFolder(false))
      mnuShowSearchFolderClick();
    
    if (startWatcher)
      folderTreeWatcher.createNewWatcherAndStart();
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private boolean copyFilesToFolder(boolean onlySelected)
  {
    SearchResultFileList fileList = new SearchResultFileList();
    
    if (db.isLoaded() == false) return false;    
    if (results().size() < 1) return false;

    task = new HyperTask() { @Override protected Boolean call() throws Exception
    {    
      updateMessage("Building list...");
      
      updateProgress(0, 1);

      ObservableList<ResultsRow> resultRowList;
      
      if (onlySelected)
        resultRowList = curQV.resultsTable.tv.getSelectionModel().getSelectedItems();
      else
        resultRowList = results();    
        
      int ndx = 0; for (ResultsRow row : resultRowList)
      {
        HDT_Base record = row.getRecord();
        if (record != null)
          if (record instanceof HDT_RecordWithPath)
            fileList.addRecord((HDT_RecordWithPath)record, includeEdited.getValue());
        
        if (isCancelled()) 
          throw new TerminateTaskException();
        
        updateProgress(ndx++, resultRowList.size());
      }
      return true;
    }};
    
    if (!HyperTask.performTaskWithProgressDialog(task))
      return false;

    boolean startWatcher = folderTreeWatcher.stop();

    task = new HyperTask() { @Override protected Boolean call() throws Exception
    {    
      updateMessage("Copying files...");
      
      updateProgress(0, 1);
      
      fileList.copyAll(excludeAnnots.getValue(), task);
      return true;
    }};
    
    HyperTask.performTaskWithProgressDialog(task);
    
    if (startWatcher == true)
      folderTreeWatcher.createNewWatcherAndStart();
    
    fileList.showErrors();
    
    return true;
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  @FXML private void mnuShowSearchFolderClick()
  {
    if (db.isLoaded() == false) return;
    launchFile(db.getPath(PREF_KEY_RESULTS_PATH, null));
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  @FXML private void mnuClearSearchFolderClick()
  {
    if (db.isLoaded() == false) return;
    
    boolean startWatcher = folderTreeWatcher.stop();
    
    try { FileUtils.cleanDirectory(db.getPath(PREF_KEY_RESULTS_PATH, null).toFile()); }
    catch (IOException e) { messageDialog("One or more files were not deleted. Reason: " + e.getMessage(), mtError); }
    
    if (startWatcher)
      folderTreeWatcher.createNewWatcherAndStart();    
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void findWithinDesc(String text)
  {
    if (activeRecord() != null)
      MainTextWrapper.hiliteText(text, webView.getEngine());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public HDT_Base activeRecord()
  {
    if (curQV == null) return null;
    return curQV.activeRecord();
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  
  @Override public int getRecordNdx()
  {    
    int count = getRecordCount();
    
    if (count > 0)
      return curQV.tvResults.getSelectionModel().getSelectedIndex();
    else
      return -1;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void setCB(ComboBox<ResultsRow> cb)
  {
    this.cb = cb;
    updateCB();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void updateCB()
  {
    if (curQV != null)
      curQV.updateCB();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void closeCurrentView()
  {
    int ndx = tabPane.getSelectionModel().getSelectedIndex(), nextNdx = ndx + 1;
    deleteView(tabPane.getSelectionModel().getSelectedItem());
    
    if ((nextNdx + 1) == tabPane.getTabs().size())
      nextNdx = ndx - 1;
    
    tabPane.getSelectionModel().select(nextNdx);
    
    tabPane.getTabs().remove(ndx);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}