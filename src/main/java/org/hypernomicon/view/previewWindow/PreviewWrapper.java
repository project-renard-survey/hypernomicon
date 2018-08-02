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

package org.hypernomicon.view.previewWindow;

import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.App.*;
import static org.hypernomicon.util.Util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tika.mime.MediaType;

import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

import org.hypernomicon.App;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.records.HDT_Base;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithPath;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.model.records.HDT_WorkFile;
import org.hypernomicon.util.filePath.FilePath;
import org.hypernomicon.view.previewWindow.PDFJSWrapper.PDFJSCommand;
import org.hypernomicon.view.previewWindow.PreviewWindow.PreviewSource;
import org.hypernomicon.view.tabs.HyperTab.TabEnum;
import org.hypernomicon.view.tabs.WorkTabController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;

public class PreviewWrapper
{
  public class PreviewFile
  {
    public FilePath filePath;
    public HDT_RecordWithPath record;
    public List<Integer> navList;
    public int navNdx;
  }
  
  private FilePath filePathShowing = null;
  private int fileNdx, pageNum = -1, pageNumShowing = -1, workStartPageNum = -1, workEndPageNum = -1, numPages = 0;
  private PreviewSource src;
  private PreviewWindow window;
  private Tab tab;
  private boolean viewerErrOccurred = false, needsRefresh = true, initialized = false, viewerClear = false;
  private PDFJSWrapper jsWrapper;
  private Map<String, Integer> labelToPage;
  private Map<Integer, String> pageToLabel;
  private List<Integer> hilitePages;
  private List<PreviewFile> fileList;
  private PreviewFile curPrevFile;
  private ToggleButton btn;
  private AnchorPane ap;
  
  public PreviewSource getSource()      { return src; }
  public int getPageNum()               { return pageNum; }
  public int getNumPages()              { return numPages; }
  public Tab getTab()                   { return tab; }
  public FilePath getFilePath()         { return curPrevFile == null ? null : curPrevFile.filePath; }
  public boolean needsRefresh()         { return needsRefresh; }
  public void setNeedsRefresh()         { needsRefresh = true; }
  public int getWorkStartPageNum()      { return workStartPageNum; }
  public int getWorkEndPageNum()        { return workEndPageNum; }
  public HDT_RecordWithPath getRecord() { return curPrevFile == null ? null : curPrevFile.record; }
  public ToggleButton getToggleButton() { return btn; }
  FilePath getFilePathShowing()         { return filePathShowing; }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public PreviewWrapper(PreviewSource src, AnchorPane ap, Tab tab, ToggleButton btn, PreviewWindow window)
  {     
    this.src = src;
    this.tab = tab;
    this.window = window;
    this.btn = btn;
    this.ap = ap;
    
    fileList = new ArrayList<>();
    fileNdx = -1;
           
    btn.selectedProperty().addListener((observable, oldValue, newValue) ->
    {
      if (newValue) window.tpPreview.getSelectionModel().select(tab);
    });
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  @SuppressWarnings("unused")
  private void doneHndlr(PDFJSCommand cmd, boolean success, String errMessage)
  {
    switch (cmd)
    {
      case pjsOpen:
                 
        if (curPrevFile == null) return;
        
        numPages = jsWrapper.getNumPages();
        Platform.runLater(() -> 
        {          
          if (curPrevFile != null)
            if (curPrevFile.navNdx == -1) incrementNav();
          
          refreshControls(); 
        });
        
        break;
        
      case pjsClose:
        
        viewerClear = true;
        break;
        
      default :
    }
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private void pageChangeHndlr(int newPageNumShowing)
  { 
    pageNumShowing = newPageNumShowing;
    
    if (pageNum != pageNumShowing)
    {
      pageNum = pageNumShowing;
      
      incrementNav();
      
      if (window.curSource() == src)
        Platform.runLater(this::refreshControls);
    }
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private void retrievedDataHndlr(Map<String, Integer> labelToPage, Map<Integer, String> pageToLabel, List<Integer> hilitePages)
  {
    this.labelToPage = labelToPage;
    this.pageToLabel = pageToLabel;
    this.hilitePages = hilitePages;
    
    if (window.curSource() == src)
      Platform.runLater(this::refreshControls);    
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private void initJS()
  {
    if (App.jxBrowserDisabled)
      return;
    
    jsWrapper = new PDFJSWrapper(ap, 
        (cmd, success, errMessage) ->              doneHndlr(cmd, success, errMessage),
        (newPageNumShowing) ->                     pageChangeHndlr(newPageNumShowing),
        (labelToPage, pageToLabel, hilitePages) -> retrievedDataHndlr(labelToPage, pageToLabel, hilitePages));
    
    if (App.jxBrowserDisabled)
      return;
    
    initialized = true;
    viewerClear = false;    
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void setWorkPageNums(int start, int end) 
  { 
    workStartPageNum = start;
    workEndPageNum = end;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void go()
  {
    if (curPrevFile == null) return;
    
    if (curPrevFile.record != null)
      ui.goToRecord(curPrevFile.record, true);
    else if (curPrevFile.filePath != null)
      ui.goToRecord(HyperPath.getFileFromFilePath(curPrevFile.filePath), true);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public boolean enableNavButton(boolean isForward)
  {
    if (curPrevFile == null) return false;
    
    if (isForward)
      return ((curPrevFile.navNdx + 1) < curPrevFile.navList.size());

    return curPrevFile.navNdx >= 1;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public boolean enableFileNavButton(boolean isForward)
  {
    if (isForward)
      return ((fileNdx + 1) < fileList.size());
    
    return fileNdx >= 1;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private boolean addMenuItem(ObservableList<MenuItem> menu, int ndx)
  {
    menu.add(getMenuItemForNavNdx(ndx));
    
    return menu.size() == 15;   
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void refreshNavMenu(ObservableList<MenuItem> menu, boolean isForward)
  {
    menu.clear();
    if (curPrevFile == null) return;
    
    if (isForward)
    {
      for (int ndx = curPrevFile.navNdx + 1; ndx < curPrevFile.navList.size(); ndx++)
        if (addMenuItem(menu, ndx)) return;
    }
    else
    {
      for (int ndx = curPrevFile.navNdx - 1; ndx >= 0; ndx--)
        if (addMenuItem(menu, ndx)) return;
    }    
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  private MenuItem getMenuItemForNavNdx(int ndx)
  {
    MenuItem item;
    int page = curPrevFile.navList.get(ndx);
    String pageLabel = safeStr(getLabelByPage(page)), pageStr = String.valueOf(page);
    
    if ((pageLabel.length() > 0) && (pageLabel.equals(pageStr) == false)) 
      item = new MenuItem("Page " + pageLabel + " (" + pageStr + ")");
    else
      item = new MenuItem("Page " + pageStr);
    
    item.setOnAction(event ->
    {
      curPrevFile.navNdx = ndx;
      setPreview(page, false);
    });
    
    return item;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void navClick(boolean isForward)
  {
    if (enableNavButton(isForward) == false) return;
    
    if (isForward)
      curPrevFile.navNdx++;
    else
      curPrevFile.navNdx--;
    
    setPreview(curPrevFile.navList.get(curPrevFile.navNdx), false);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void fileNavClick(boolean isForward)
  {
    if (isForward)
      fileNdx++;
    else
      fileNdx--;
    
    PreviewFile prevFile = fileList.get(fileNdx);
    
    int pageNum = 1;
    if (prevFile.navNdx > -1)
      pageNum = prevFile.navList.get(prevFile.navNdx);
    
    setPreview(prevFile.filePath, pageNum, prevFile.record, false, prevFile);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void clearPreview()
  {
    filePathShowing = null;
    pageNum = -1;
    pageNumShowing = -1;
    workStartPageNum = -1;
    workEndPageNum = -1;
    curPrevFile = null;
    fileNdx = -1;
    
    if (window.curSource() == src) window.clearControls();
    
    if (initialized == false) return;
    
    if (initialized)
      if (viewerClear == false)
        jsWrapper.close();    
    
    if (window.curSource() == src)
      needsRefresh = false;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void setPreview(FilePath filePath, int pageNum, HDT_Base record)
  {
    setPreview(filePath, pageNum, record, true, null);
  }
  
  private void setPreview(FilePath filePath, int pageNum, HDT_Base record, boolean incrementNav, PreviewFile prevFile)
  {
    boolean fileChanged = true;
    
    if (record != null)
      if ((record.getType() != hdtWork) && (record.getType() != hdtMiscFile))
        record = null;
    
    if (getRecord() == record)
      if (FilePath.isEmpty(getFilePath()) == false)
        if (curPrevFile.filePath.equals(filePath))
        {
          fileChanged = false;
          
          if (this.pageNum == pageNum)
            return;
        }
    
    if (fileChanged)
    {
      if (prevFile != null)
        curPrevFile = prevFile;
      else
      {
        curPrevFile = new PreviewFile();
        
        curPrevFile.filePath = filePath;
        curPrevFile.record = (HDT_RecordWithPath)record;
        curPrevFile.navList = new ArrayList<>();
        curPrevFile.navNdx = -1;
          
        fileNdx++;
        while (fileList.size() > fileNdx)
          fileList.remove(fileNdx);
        
        fileList.add(curPrevFile);
      }
    }
    
    this.pageNum = pageNum;
    
    if (filePath == null)
      clearPreview();
    else if ((window.curSource() == src) && window.getStage().isShowing())
      refreshPreview(false, incrementNav);
    else
    {
      if ((window.curSource() == src) && contentsWindow.getStage().isShowing())
        refreshControls();
      
      needsRefresh = true;
    }      
  }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

  public void setPreview(int pageNum, boolean incrementNav)
  {
    setPreview(getFilePath(), pageNum, getRecord(), incrementNav, null);
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void refreshControls()
  {   
    FilePath filePath = getFilePath();
    
    if ((pageNum <= 0) || FilePath.isEmpty(filePath) || viewerErrOccurred)
    {
      clearPreview();
      return;
    }
    
    if (filePath.exists() == false)
    {
      clearPreview();
      return;
    }
    
    window.refreshControls(pageNum, numPages, filePath, getRecord());    
  }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

  private void finishRefresh(boolean forceReload, boolean incrementNav)
  {
    if ((pageNum <= 0) || getFilePath() == null || (initialized == false))
    {
      clearPreview();
      return;
    }
    
    if (curPrevFile.filePath.equals(filePathShowing) && viewerErrOccurred)
    {
      clearPreview();
      return;
    }
    
    viewerErrOccurred = false;
    
    if (forceReload || (curPrevFile.filePath.equals(filePathShowing) == false))
    {
      MediaType mimetype = getMediaType(curPrevFile.filePath);
      
      if (mimetype.toString().contains("html") ||
          mimetype.toString().contains("openxmlformats-officedocument") ||
          mimetype.toString().contains("pdf")  ||
          mimetype.toString().contains("image") ||
          mimetype.toString().contains("plain") ||
          mimetype.toString().contains("video") ||
          mimetype.toString().contains("audio"))
      {
        filePathShowing = null;
        pageNumShowing = -1;
             
        window.clearControls();
        needsRefresh = false;
        
        viewerClear = false;

        if (viewerErrOccurred) return;
        
        labelToPage = null;
        pageToLabel = null;
        hilitePages = null;
        
        if (mimetype.toString().contains("pdf"))
          jsWrapper.loadPdf(curPrevFile.filePath, pageNum);
        else if (mimetype.toString().contains("openxmlformats-officedocument"))
        {
          try
          {
            DocumentConverter converter = new DocumentConverter();
            Result<String> result = converter.convertToHtml(curPrevFile.filePath.toFile());
            String html = result.getValue(); // The generated HTML
            
            result.getWarnings().forEach(msg -> System.out.println(msg));
            
            jsWrapper.loadHtml(html);
          }
          catch (IOException e)
          {
            jsWrapper.loadFile(curPrevFile.filePath);
          }
        }
        else
          jsWrapper.loadFile(curPrevFile.filePath);
        
        filePathShowing = curPrevFile.filePath;
        pageNumShowing = -1;
               
        return;
      }
      else
      {
        clearPreview();
        return;
      }      
    }
      
    if (pageNum != pageNumShowing)
    {
      jsWrapper.goToPage(pageNum);
    }
    
    if (incrementNav)
      incrementNav();
    
    refreshControls();    
  }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   
  
  public void refreshPreview(boolean forceReload, boolean incrementNav)
  {            
    if (initialized == false)
      initJS();
    
    if (window.disablePreviewUpdating) return;
    
    needsRefresh = false;
    
    if (forceReload)
      jsWrapper.reloadBrowser(() -> Platform.runLater(() -> finishRefresh(forceReload, incrementNav)));
    else
      finishRefresh(forceReload, incrementNav);
  }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

  private void incrementNav()
  {
    curPrevFile.navNdx++;
    
    while (curPrevFile.navList.size() > curPrevFile.navNdx)
      curPrevFile.navList.remove(curPrevFile.navNdx);
    
    curPrevFile.navList.add(pageNum);
    
    // Now remove adjacent duplicates
    
    Iterator<Integer> it = curPrevFile.navList.iterator();
    int ndx = 0;
    int prevPage = -1;
    
    while (it.hasNext())
    {
      int page = it.next();
      if (page == prevPage)
      {
        it.remove();
        if (curPrevFile.navNdx >= ndx)
          curPrevFile.navNdx--;
      }
      else
      {
        ndx++;
        prevPage = page;
      }
    }
  }
  
  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

  public int getPageByLabel(String label)   
  { 
    if (labelToPage != null)
      if (labelToPage.isEmpty() == false)
        return labelToPage.getOrDefault(label, -1);
    
    return parseInt(label, -1);
  }

  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

  public String getLabelByPage(int page)    
  { 
    if (pageToLabel != null)
      if (pageToLabel.isEmpty() == false)
        return pageToLabel.getOrDefault(page, "");
    
    return String.valueOf(page);
  }
  
  //---------------------------------------------------------------------------  
  //---------------------------------------------------------------------------   

  public void updatePage(int newPageNum)
  {
    if (newPageNum < 1) return;
    if (pageNum < 1) return;
    if (curPrevFile == null) return;
    if (curPrevFile.filePath == null) return;
    if (newPageNum > numPages) return;
    
    setPreview(curPrevFile.filePath, newPageNum, curPrevFile.record);
  }
   
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public void setWorkPageFromContentsWindow(int pageNum, boolean isStart)
  {
    if (isStart)
      workStartPageNum = pageNum;
    else
      workEndPageNum = pageNum;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public boolean setCurPageAsWorkPage(boolean isStart)
  {
    if (curPrevFile == null)
      return false;
    
    if (curPrevFile.record == null)
      return false;
    
    if (curPrevFile.record.getType() != hdtWork)
      return false;
    
    if (isStart)
      workStartPageNum = pageNum;
    else
      workEndPageNum = pageNum;
    
    HDT_Work work = (HDT_Work) curPrevFile.record;
    HDT_WorkFile workFile = (HDT_WorkFile) HyperPath.getFileFromFilePath(curPrevFile.filePath);
    
    if (isStart)
      work.setStartPageNum(workFile, pageNum);
    else
      work.setEndPageNum(workFile, pageNum);

    if (ui.currentTab().getTabEnum() == TabEnum.workTab)
      if (ui.currentTab().activeRecord() == curPrevFile.record)
        WorkTabController.class.cast(ui.currentTab()).setPageNum(workFile, pageNum, isStart);
        
    contentsWindow.update(workFile, pageNum, true);
    
    return true;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public int lowestHilitePage()
  {
    if (hilitePages != null)
      if (hilitePages.isEmpty() == false)
        return hilitePages.get(0);
    
    return -1;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public int highestHilitePage()
  {
    if (hilitePages != null)
      if (hilitePages.isEmpty() == false)
        return hilitePages.get(hilitePages.size() - 1);
    
    return -1;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public int getPrevHilite(int curPage)
  {
    if (hilitePages == null) return -1;
    if (hilitePages.isEmpty()) return -1;
    
    int newPage = -1;
    
    for (Integer page : hilitePages)
      if (page < curPage)
        if (page > newPage)
          newPage = page;
      
    return newPage;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  public int getNextHilite(int curPage)
  {
    if (hilitePages == null) return -1;
    if (hilitePages.isEmpty()) return -1;
    
    int newPage = numPages + 1;
    
    for (Integer page : hilitePages)
      if (page > curPage)
        if (page < newPage)
          newPage = page;
      
    if (newPage > numPages) return -1;
    return newPage;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  void cleanup(Runnable disposeHndlr)
  {   
    if (initialized)
      jsWrapper.cleanup(disposeHndlr);
    else
      disposeHndlr.run();
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  void prepareToHide()
  {
    if (initialized)
      jsWrapper.prepareToHide();
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

  void prepareToShow()
  {
    if (initialized)
      jsWrapper.prepareToShow();
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------   

}