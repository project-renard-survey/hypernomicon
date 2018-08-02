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

import org.hypernomicon.model.HyperDataset;
import org.hypernomicon.model.PersonName;
import org.hypernomicon.model.SearchKeys;
import org.hypernomicon.model.SearchKeys.SearchKeyword;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_Field;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_PersonStatus;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_Rank;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithPath;
import org.hypernomicon.model.relations.HyperObjPointer;
import org.hypernomicon.view.wrappers.HyperTable;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Rectangle2D;

import org.apache.commons.lang3.mutable.MutableInt;

import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;

//---------------------------------------------------------------------------

public class HDT_Person extends HDT_RecordWithConnector implements HDT_RecordWithPath
{
  public List<HDT_Institution> institutions;
  
  public List<HDT_Work> works;
  public List<HDT_MiscFile> miscFiles;
  public List<HDT_Investigation> investigations;
  
  public HyperObjPointer<HDT_Person, HDT_Rank> rank;
  public HyperObjPointer<HDT_Person, HDT_PersonStatus> status;
  public HyperObjPointer<HDT_Person, HDT_Field> field;
  public HyperObjPointer<HDT_Person, HDT_Subfield> subfield;
  
  private HyperPath picture;
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HDT_Person(HDT_RecordState xmlState, HyperDataset<HDT_Person> dataset)
  {
    super(xmlState, dataset);
    
    setNameInternal("|", false);
          
    institutions = getObjList(rtInstOfPerson);
    
    works = getSubjList(rtAuthorOfWork);
    miscFiles = getSubjList(rtAuthorOfFile);
    investigations = getSubjList(rtPersonOfInv);
    
    rank = getObjPointer(rtRankOfPerson);
    status = getObjPointer(rtStatusOfPerson);
    field = getObjPointer(rtFieldOfPerson);
    subfield = getObjPointer(rtSubfieldOfPerson);
    
    picture = new HyperPath(null, this);
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public String getWebLink()                      { return getTagString(tagWebLink); }
  public String getOrcID()                        { return getTagString(tagORCID); }
  public PersonName getName()                     { return getName(false); }
  public String getNameLastFirst(boolean engChar) { return getName(engChar).getLastFirst(); }
  public String getFullName(boolean engChar)      { return getName(engChar).getFull(); }

  @Override public void setName(String str)     { messageDialog("Internal error #19982", mtError); }
  @Override public HDT_RecordType getType()     { return hdtPerson; }
  @Override public String listName()            { return getNameLastFirst(false); }
  
  void setFirstNameInternal(String newStr, boolean update) { setNameInternal(getLastName() + "|" + newStr.replace("|", ""), update); };
  void setLastNameInternal(String newStr, boolean update)  { setNameInternal(newStr.replace("|", "") + "|" + getFirstName(), update); };
  
  public void setWebLink(String newStr)      { updateTagString(tagWebLink, newStr); };
  public void setORCID(String newOrcid)      { updateTagString(tagORCID, newOrcid); };
  public void setInstitutions(HyperTable ht) { updateObjectsFromHT(rtInstOfPerson, ht, 2); };
  
//---------------------------------------------------------------------------
  
  public PersonName getName(boolean engChar) 
  { 
    if (engChar)
      return new PersonName(getFirstNameEngChar(), getLastNameEngChar());
    
    return new PersonName(getFirstName(), getLastName()); 
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  
  public void setName(PersonName personName)
  {
    setFirstNameInternal(personName.getFirst(), true);
    setLastNameInternal(personName.getLast(), true);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public String getLastName()
  { 
    String s = name();
    return s.substring(0, s.indexOf('|'));
  }
  
  public String getLastNameEngChar()
  {
    String s = getNameEngChar();
    return s.substring(0, s.indexOf('|'));   
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public String getFirstName() 
  { 
    String s = name();
    return s.substring(s.indexOf('|') + 1);
  }
  
  public String getFirstNameEngChar()
  { 
    String s = getNameEngChar();
    return s.substring(s.indexOf('|') + 1);
  } 
    
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void expire()
  {   
    while (investigations.isEmpty() == false)
      db.deleteRecord(hdtInvestigation, investigations.get(0).getID());
       
    picture.clear();
    
    super.expire();
    
    // Delete unused subfields
    
    ArrayList<Integer> killList = new ArrayList<>();
    
    db.subfields.forEach(subfield ->
    {
      if (subfield.persons.isEmpty())
        killList.add(subfield.getID());
    });
    
    killList.forEach(subfieldID -> db.deleteRecord(hdtSubfield, subfieldID));
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private static HDT_Person addSearchKey(StringBuilder keys, String key, HDT_Person person)
  {
    key = key.trim();
    if (key.length() < 3) return null;
    
    SearchKeyword hyperKey = db.getKeyByKeyword(key);
    
    if (hyperKey != null)
    {
      if (hyperKey.record != person)
      {
        if (hyperKey.record.getType() == hdtPerson)
          return HDT_Person.class.cast(hyperKey.record);
        else
          return null;            
      }               
    }
    
    String[] vals = keys.toString().split(";");
    for (String val : vals)
    {
      if (val.trim().equalsIgnoreCase(key))
        return null;
    }
    
    if (keys.length() > 0) keys.append("; ");
    keys.append(key);
    
    return null;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public static HDT_Person lookUpByName(PersonName name)
  {
    StringBuilder searchKeySB = new StringBuilder();
    
    HDT_Person person = makeSearchKey(name, null, searchKeySB);
    
    if (searchKeySB.toString().length() == 0)
      return person;
    
    return null;    
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public static class PotentialKeySet
  {
    private ArrayList<String> keys = new ArrayList<>();
    private ArrayList<Boolean> useForDupCheck = new ArrayList<>();
    private boolean lowerCase;
    
    public PotentialKeySet(boolean lowerCase)
    {
      this.lowerCase = lowerCase;
    }
    
    public int size()                         { return keys.size(); }
    public String getKey(int ndx)             { return keys.get(ndx); }
    public boolean getUseForDupCheck(int ndx) { return useForDupCheck.get(ndx); }
    public boolean containsKey(String key)    { return keys.contains(key); }
    
  //---------------------------------------------------------------------------
    
    public void add(String newKey, boolean newUseForDupCheck)
    {
      if (newKey.length() == 0) return;
      
      if (lowerCase) newKey = newKey.toLowerCase();
      
      int ndx = keys.indexOf(newKey);
      
      if (ndx < 0)
      {
        keys.add(newKey);
        useForDupCheck.add(newUseForDupCheck);
      }
      else
      {
        if (newUseForDupCheck)
          if (useForDupCheck.get(ndx) == false)
          {
            keys.remove(ndx);
            useForDupCheck.remove(ndx);
            
            keys.add(newKey);
            useForDupCheck.add(true);
          }
      }
    }

    //---------------------------------------------------------------------------
    
    public boolean isSubsetOf(PotentialKeySet keySet)
    {
      for (String key : keys)
        if (keySet.containsKey(key) == false)
          return false;
      
      return true;
    }    
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  // Pass person=null if making a search key for a new record
  
  public static HDT_Person makeSearchKey(PersonName name, HDT_Person person, StringBuilder newSearchKey)
  {
    HDT_Person rv, otherPerson = null;
    StringBuilder keys = new StringBuilder();
    
    PotentialKeySet keySet = makeSearchKeySet(name, false, false, false);
    
    for (int ndx = 0; ndx < keySet.size(); ndx++)
    {
      rv = addSearchKey(keys, keySet.getKey(ndx), person);
      if (keySet.getUseForDupCheck(ndx) && (rv != null))
        otherPerson = rv;
    }
    
    assignSB(newSearchKey, keys.toString());    
    return otherPerson;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  // Returns last name, prepared for search key
  
  public static String getSearchKeyComponents(String first, String last, ArrayList<String> nameList, ArrayList<String> initialList, StringBuilder nickNames)
  {
    MutableInt pos = new MutableInt(0);
    String name;
    
    first = first.replace(".", ". ");
    
    first = SearchKeys.prepSearchKey(first);
    last = SearchKeys.prepSearchKey(last);
    
    if (first.contains("("))
    {
      int paren1 = first.indexOf('(');
      
      if (first.contains(")"))
      {
        int paren2 = first.indexOf(')');
        
        assignSB(nickNames, first.substring(paren1, paren2 + 1));
        first = first.substring(0, paren1) + first.substring(paren2 + 1);
      }
      else
        first = first.substring(0, paren1);
      
      first = first.trim();
    }
    
    while (pos.intValue() != -1)
    {
      name = nextSubString(first, " ", pos);
      if (name.length() == 0) continue;
      
      if (name.endsWith("."))
      {
        if ((name.length() == 2) && (name.equals(name.toUpperCase())))  // true if it is an initial
        {
          initialList.add(name.substring(0, 1));
          nameList.add("");
        }
        else
        {
          name = name + " " + nextSubString(first, " ", pos);
          initialList.add(name.substring(0, 1));
          nameList.add(name);
        }
      }
      else if ((name.length() == 1) && name.equals(name.toUpperCase())) // initial without period
      {
        initialList.add(name);
        nameList.add("");       
      }
      else
      {
        initialList.add(name.substring(0, 1));
        nameList.add(name);
      }
    }  
    
    return last;
  }
  
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  public static PotentialKeySet makeSearchKeySet(PersonName personName, boolean useAllInitials, boolean initialsOnly, boolean lowerCase)
  {
    PotentialKeySet keySet = new PotentialKeySet(lowerCase);
    String name, first = personName.getFirst(), last = personName.getLast();
    StringBuilder nickNames = new StringBuilder();
    int ndx;
    
    ArrayList<String> nameList = new ArrayList<>();
    ArrayList<String> initialList = new ArrayList<>();
      
    last = getSearchKeyComponents(first, last, nameList, initialList, nickNames);
         
    keySet.add(last, false);
    
    if (nameList.size() > 0)
    {
      if (nameList.get(0).length() > 0)
      {
        if (useAllInitials)
          keySet.add(initialList.get(0) + ". " + last, false);
        
        if (initialsOnly == false)
          keySet.add(nameList.get(0) + " " + last, true);
      }
      else
      {
        keySet.add(initialList.get(0) + ". " + last, false);
        
        if (initialsOnly == false)
        {
          for (ndx = 1; ndx < nameList.size(); ndx++)
            if (nameList.get(ndx).length() > 0)
            {
              keySet.add(nameList.get(ndx) + " " + last, false);
              break;
            }
        }
      }
        
      if (initialList.size() > 1)
      {
        name = "";
        for (String initial : initialList)
          name = name + initial + ". ";
        
        keySet.add(name + last, false);
        
        if ((initialsOnly == false) && (nameList.get(0).length() > 0))
        {
          name = nameList.get(0) + " ";
          for (ndx = 1; ndx < initialList.size(); ndx++)
            name = name + initialList.get(ndx) + ". ";
          
          keySet.add(name + last, true);
        }
      }
      
      if (nameList.size() > 1)
      {
        name = "";
        for (ndx = 0; ndx < nameList.size(); ndx++)
        {
          if ((initialsOnly == false) && (nameList.get(ndx).length() > 0))
            name = name + nameList.get(ndx) + " ";
          else
            name = name + initialList.get(ndx) + ". ";
        }
                
        keySet.add(name + last, true);
      }
    }
    
    if ((initialsOnly == false) && (nickNames.length() > 0))
    {
      char c;
      String nickName = "";
      List<String> nickNameList = new ArrayList<>();
      
      for (ndx = 0; ndx < nickNames.length(); ndx++)
      {
        c = nickNames.charAt(ndx);
        
        if ((c == ')') || (c == '(') || (c == ' ') || (c == ',') || (c == ';'))
        {
          if (nickName.length() > 0)
          {
            nickNameList.add(nickName);
            nickName = "";
          }
        }
        else
          nickName = nickName + c;
      }
      
      if (nickName.length() > 0)
        nickNameList.add(nickName);
      
      for (String nName : nickNameList)
        keySet.add(nName + " " + last, false);
    }
    
    return keySet;
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------
  
  public Rectangle2D getViewPort()
  {    
    String str = getTagString(tagPictureCrop);
    
    if (str.length() < 7) return null;
    
    String[] vals = str.split(";");
    if (vals.length != 4) return null;
    
    int x = parseInt(vals[0], -1);
    int y = parseInt(vals[1], -1);
    int width = parseInt(vals[2], -1);
    int height = parseInt(vals[3], -1);

    return new Rectangle2D(x, y, width, height);
  }
  
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------
  
  public void setViewPort(Rectangle2D viewPort)
  {
    if (viewPort == null)
    {
      updateTagString(tagPictureCrop, "");
      return;
    }
    
    int x = (int) viewPort.getMinX();
    int y = (int) viewPort.getMinY();
    int width = (int) viewPort.getWidth();
    int height = (int) viewPort.getHeight();
    
    updateTagString(tagPictureCrop, x + ";" + y + ";" + width + ";" + height);
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public HyperPath getPath()
  {
    return picture;
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

}