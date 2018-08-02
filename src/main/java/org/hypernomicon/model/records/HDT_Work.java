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

package org.hypernomicon.model.records;

import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.Const.*;
import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;
import static org.hypernomicon.util.Util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hypernomicon.bib.BibData;
import org.hypernomicon.bib.BibUtils;
import org.hypernomicon.bib.WorkBibData;
import org.hypernomicon.model.HyperDataset;
import org.hypernomicon.model.items.Authors;
import org.hypernomicon.model.items.HDI_OfflineTernary.Ternary;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithPath;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_WorkType;
import org.hypernomicon.model.records.SimpleRecordTypes.WorkTypeEnum;
import org.hypernomicon.model.relations.HyperObjList;
import org.hypernomicon.model.relations.HyperObjPointer;
import org.hypernomicon.model.relations.HyperSubjList;
import org.hypernomicon.model.relations.ObjectGroup;
import org.hypernomicon.util.filePath.FilePath;
import org.hypernomicon.view.wrappers.HyperTable;

public class HDT_Work extends HDT_RecordWithConnector implements HDT_RecordWithPath
{ 
  private Authors authors;
  
  public List<HDT_Person> authorRecords;
  public HyperObjList<HDT_Work, HDT_Investigation> investigations;
  public HyperObjList<HDT_Work, HDT_WorkLabel> labels;
  public List<HDT_WorkFile> workFiles;
  public HyperSubjList<HDT_Work, HDT_Work> subWorks;
  public HyperSubjList<HDT_MiscFile, HDT_Work> miscFiles;
  public HyperSubjList<HDT_Argument, HDT_Work> arguments;
  
  public HyperObjPointer<HDT_Work, HDT_WorkType> workType;
  public HyperObjPointer<HDT_Work, HDT_Work> largerWork;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HDT_Work(HDT_RecordState xmlState, HyperDataset<HDT_Work> dataset)
  {
    super(xmlState, dataset);
     
    nameTag = tagTitle;
    
    if (dataset != null)
    {
      authors = new Authors(getObjList(rtAuthorOfWork), this);
      
      authorRecords = Collections.unmodifiableList(getObjList(rtAuthorOfWork));
      investigations = getObjList(rtInvestigationOfWork);
      labels = getObjList(rtLabelOfWork);
      workFiles = Collections.unmodifiableList(getObjList(rtWorkFileOfWork));
      
      subWorks = getSubjList(rtParentWorkOfWork);
      miscFiles = getSubjList(rtWorkOfMiscFile);
      arguments = getSubjList(rtWorkOfArgument);
      
      workType = getObjPointer(rtTypeOfWork);
      largerWork = getObjPointer(rtParentWorkOfWork);
    }
  }
  
  public WorkTypeEnum getWorkTypeValue()  { return HDT_WorkType.workTypeIDToEnumVal(workType.getID()); }
    
  public void setInvestigations(HyperTable ht)  { updateObjectsFromHT(rtInvestigationOfWork, ht, 2); }
  public void setWorkLabels(HyperTable ht)      { updateObjectsFromHT(rtLabelOfWork, ht, 2); }

  public void setWorkType(WorkTypeEnum val)     { workType.set(HDT_WorkType.get(val)); }

  @Override public String listName()        { return name(); }
  @Override public HDT_RecordType getType() { return hdtWork; }
  
  public String getYear()        { return getTagString(tagYear); }
  public String getBibEntryKey() { return getBibEntryKeyString(); }
  public String getMiscBib()     { return getTagString(tagMiscBib); }
  public String getDOI()         { return getTagString(tagDOI); }
  public String getWebLink()     { return getTagString(tagWebLink); }
  public Authors getAuthors()    { return authors; }
  
  public void setYear(String str)        { updateTagString(tagYear, str); }
  public void setBibEntryKey(String str) { updateBibEntryKey(str); }
  public void setMiscBib(String str)     { updateTagString(tagMiscBib, str); }
  public void setDOI(String str)         { updateTagString(tagDOI, BibUtils.matchDOI(str)); }
  public void setWebLink(String str)     { updateTagString(tagWebLink, str); }

//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 

  public Ternary personIsInFileName(HDT_Person person)                       { return db.getNestedTernary(this, person, tagInFileName); }
  public void setPersonIsInFileName(HDT_Person person, Ternary inFileName)   { db.updateNestedTernary(this, person, tagInFileName, inFileName); }
  public boolean personIsEditor(HDT_Person person)                           { return db.getNestedBoolean(this, person, tagEditor); }
  public void setPersonIsEditor(HDT_Person person, boolean isEditor)         { db.updateNestedBoolean(this, person, tagEditor, isEditor); }  
  public boolean personIsTranslator(HDT_Person person)                       { return db.getNestedBoolean(this, person, tagTranslator); }
  public void setPersonIsTranslator(HDT_Person person, boolean isTranslator) { db.updateNestedBoolean(this, person, tagTranslator, isTranslator); }

//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 
  
  // pass -1 to clear the value
  
  public void setStartPageNum(HDT_WorkFile workFile, int val) { db.updateNestedString(this, workFile, tagStartPageNum, val < 0 ? "" : String.valueOf(val)); }
  public int getStartPageNum(HDT_WorkFile workFile)           { return parseInt(db.getNestedString(this, workFile, tagStartPageNum), -1); }
  public void setEndPageNum(HDT_WorkFile workFile, int val)   { db.updateNestedString(this, workFile, tagEndPageNum, val < 0 ? "" : String.valueOf(val)); }
  public int getEndPageNum(HDT_WorkFile workFile)             { return parseInt(db.getNestedString(this, workFile, tagEndPageNum), -1); }
  
//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 
  
  public void setAuthors(List<ObjectGroup> tableGroups)         
  { 
    boolean theSame = true;
        
    if (tableGroups.size() != authors.size())
      theSame = false;
    else
    {     
      for (int ndx = 0; ndx < tableGroups.size(); ndx++)
      {
        if (authors.get(ndx).equalsObjGroup(tableGroups.get(ndx)) == false)
          theSame = false;
      }
    }
    
    if (theSame) return;

    authors.update(tableGroups);
  }
 
//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 

  public String getShortAuthorsStr(boolean fullNameIfSingleton)
  {
    return Authors.getShortAuthorsStr(getAuthors().asCollection(), false, fullNameIfSingleton);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public String getLongAuthorsStr(boolean fullNameIfSingleton)
  {   
    return Authors.getLongAuthorsStr(getAuthors().asCollection(), fullNameIfSingleton);
  }
  
//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 

  @Override public String getCBText()
  {
    String authorStr = getShortAuthorsStr(false);
    String yearStr = getYear();
    String titleStr = name();
    
    String cbStr = "";
    
    if (authorStr.length() > 0)
      cbStr = authorStr + " ";
    
    if (yearStr.length() > 0)
      cbStr = cbStr + "(" + yearStr + ") ";
    
    if (titleStr.length() > 0)
      cbStr = cbStr + titleStr;
    
    return cbStr;
  }
 
//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 
  
  public void setLargerWork(int newID)
  {
    setLargerWork(newID, false);  
  }
  
  public void setLargerWork(int newID, boolean noIsbnUpdate)          
  { 
    boolean ask = false;
    
    if (largerWork.getID() == newID) return;
    
    largerWork.setID(newID);
    
    if (newID < 1) return;
    HDT_Work largerWork = db.works.getByID(newID);
    
    /***********************************************/
    /*          Update ISBNs                       */
    /***********************************************/
    
    if (noIsbnUpdate == false)
    {
      List<String> ISBNs = getISBNs(), lwISBNs = largerWork.getISBNs();
      
      boolean notAllInLW = false, notAllInSW = false;
      
      for (String isbn : ISBNs)
        if (lwISBNs.contains(isbn) == false)
          notAllInLW = true;
      
      for (String isbn : lwISBNs)
        if (ISBNs.contains(isbn) == false)
          notAllInSW = true;
      
      if ((notAllInLW == false) && (notAllInSW == true))
      {
        updateISBNstrRecursively(largerWork.getTagString(tagISBN));
      }   
      else if (notAllInLW)
      {
        if (confirmDialog("Recursively update ISBN(s) for contained/container works?"))
        {
          List<String> allISBNs = new ArrayList<>();
          allISBNs.addAll(ISBNs);
          
          for (String isbn : lwISBNs)
            if (ISBNs.contains(isbn) == false)
              allISBNs.add(isbn);
          
          String isbnStr = "";
          for (String isbn : allISBNs)
          {
            if (isbnStr.length() > 0)
              isbnStr = isbnStr + "; " + isbn;
            else
              isbnStr = isbn;
          }
          
          HDT_Work ancestor = this;
          while (ancestor.largerWork.isNotNull())
            ancestor = ancestor.largerWork.get();
          
          ancestor.updateISBNstrRecursively(isbnStr);
        }
      }
    }
        
    if (largerWork.workFiles.isEmpty()) return;

    /***********************************************/
    /*          Update work files                  */
    /***********************************************/
    
    if (workFiles.isEmpty())
    {
      largerWork.workFiles.forEach(workFile -> addWorkFile(workFile.getID(), true, true));      
      return;
    }
    
    if (workFiles.size() != largerWork.workFiles.size())
      ask = true;
    else
    {
      for (HDT_WorkFile workFile : workFiles)
        if (largerWork.workFiles.contains(workFile) == false)
          ask = true;
    }
      
    if (ask)
    {
      String msg = (workFiles.size() == 1) ? " file is " : " files are "; 
      if (confirmDialog("Currently, " + workFiles.size() + msg + "attached to the child work. Replace with parent work file(s)?"))
      {
        getObjList(rtWorkFileOfWork).clear();
        largerWork.workFiles.forEach(workFile -> addWorkFile(workFile.getID(), true, true));
      }
    }
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void replaceWorkFile(HDT_WorkFile oldWorkFile, HDT_WorkFile workFile)
  {
    HyperObjList<HDT_Work, HDT_WorkFile> workFileList = getObjList(rtWorkFileOfWork);
    
    int ndx = workFileList.indexOf(oldWorkFile);
    
    if (ndx > -1)
      workFileList.set(ndx, workFile);
    
    subWorks.forEach(childWork -> childWork.replaceWorkFile(oldWorkFile, workFile));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void addWorkFile(int newID, boolean alsoAddToEmptySubworks, boolean confirmToRemoveFromUnenteredSet)                  
  { 
    HDT_WorkFile workFile = db.workFiles.getByID(newID);
    boolean okToRemoveFromUnenteredSet;
    
    getObjList(rtWorkFileOfWork).add(workFile);
    
    for (HDT_Work work : workFile.works)
    {
      if ((work.getID() != getID()) && (work.getWorkTypeValue() == WorkTypeEnum.wtUnenteredSet))
      {
        if (confirmToRemoveFromUnenteredSet)
          okToRemoveFromUnenteredSet = confirmDialog("Okay to remove the file from the the unentered set of work files: \"" + work.name() + "\"?");          
        else
          okToRemoveFromUnenteredSet = true;
        
        if (okToRemoveFromUnenteredSet)
          db.getObjectList(rtWorkFileOfWork, work, true).remove(workFile);
      }
    }
    
    if (alsoAddToEmptySubworks == false) return;
    
    for (HDT_Work childWork : subWorks)
      if (childWork.workFiles.isEmpty()) childWork.addWorkFile(newID, true, confirmToRemoveFromUnenteredSet);
  }
 
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static FilePath getBasePathForWorkTypeID(int workTypeID)
  {
    WorkTypeEnum enumVal = HDT_WorkType.workTypeIDToEnumVal(workTypeID);
    
    switch (enumVal)
    {
      case wtBook:    return db.getPath(PREF_KEY_BOOKS_PATH, null);
      case wtChapter: return db.getPath(PREF_KEY_BOOKS_PATH, null);
      case wtPaper:   return db.getPath(PREF_KEY_PAPERS_PATH, null);
      default:        return db.getPath(PREF_KEY_MISC_FILES_PATH, null);     
    }    
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public HyperPath getPath()
  {
    if (workFiles.isEmpty()) return HyperPath.EmptyPath;
    return workFiles.get(0).getPath(); 
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public int getStartPageNum()
  {
    if (workFiles.isEmpty()) return -1;
    return getStartPageNum(workFiles.get(0)); 
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public int getEndPageNum()
  {
    if (workFiles.isEmpty()) return -1;
    return getEndPageNum(workFiles.get(0)); 
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void launch(int pageNum)
  {
    if (workFiles.isEmpty() && getWebLink().isEmpty()) return;
    
    viewNow();
    
    if (getPath().isEmpty())
    {
      openWebLink(getWebLink());
      return;
    }
      
    if (pageNum < 1) pageNum = getStartPageNum();    
    launchWorkFile(getPath().getFilePath(), pageNum);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public String getInvText(HDT_Person person)
  {
    String invText = "";
    for (HDT_Investigation inv : investigations)
    {
      if (inv.person.get() == person)
        invText = invText.length() == 0 ? inv.listName() : invText + ", " + inv.listName();
    }
    
    return invText;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean canLaunch()
  {
    if (getPath().isEmpty() == false) return true;
    return getWebLink().isEmpty() == false;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static String addFileIndicator(String str, HDT_Work work)
  {
    if (work == null) return str;
    
    String indicator = "";
    
    if (work.workFiles.isEmpty() == false)
      indicator = work.workFiles.get(0).getPath().getFilePath().getExtensionOnly();
    else if (safeStr(work.getWebLink()).length() > 0)
      indicator = "web";
    
    if (indicator.length() == 0) return str;
    
    if (str.length() > 0) str = str + " ";
    
    return str + "(" + indicator + ")";
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public List<String> getISBNs()      
  { 
    String isbnStr = getTagString(tagISBN);
    
    List<String> isbns = BibUtils.matchISBN(isbnStr);
    
    if (isbns == null) return Collections.emptyList();
    return isbns;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void updateISBNstrRecursively(String newISBNs)
  {
    updateTagString(tagISBN, newISBNs);
    
    subWorks.forEach(child -> child.updateISBNstrRecursively(newISBNs));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void setISBNs(List<String> list)
  {   
    String isbnStr = "";
    List<String> allIsbns = new ArrayList<>();
    
    list.forEach(isbn ->
    {
      List<String> isbns = BibUtils.matchISBN(isbn);
      if (isbns != null)
      {
        for (String realIsbn : isbns)
          if (allIsbns.contains(realIsbn) == false)
            allIsbns.add(realIsbn);
      }
    });
    
    boolean unequal = false;
    List<String> curISBNs = getISBNs();
    for (String isbn : allIsbns)
      if (curISBNs.contains(isbn) == false)
        unequal = true;
    
    for (String isbn : curISBNs)
      if (allIsbns.contains(isbn) == false)
        unequal = true;
    
    if (unequal == false) return;
    
    for (String isbn : allIsbns)
    {
      if (isbnStr.length() > 0)
        isbnStr = isbnStr + "; " + isbn;
      else
        isbnStr = isbn;
    }
   
    if (largerWork.isNotNull())
    {
      if (confirmDialog("Recursively update ISBN(s) for contained/container works?"))
      {
        HDT_Work ancestor = this;
        while (ancestor.largerWork.isNotNull())
          ancestor = ancestor.largerWork.get();
        
        ancestor.updateISBNstrRecursively(isbnStr);
        return;
      }
    }
    else if (subWorks.isEmpty() == false)
    {
      if (confirmDialog("Recursively update ISBN(s) for contained/container works?"))
      {
        updateISBNstrRecursively(isbnStr);
        return;
      }
    }

    updateTagString(tagISBN, isbnStr);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public BibData getBibData()
  {
    String entryKey = getBibEntryKey();
    
    if (entryKey.length() > 0)
    {
      BibData bibData = db.getBibEntryByKey(entryKey);
      if (bibData != null) return bibData;
    }
    
    return new WorkBibData(this);
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}