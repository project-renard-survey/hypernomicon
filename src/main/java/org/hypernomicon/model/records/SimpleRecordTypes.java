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

import java.util.List;

import com.google.common.collect.EnumHashBiMap;

import org.hypernomicon.model.HyperDataset;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.items.MainText;

import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;
import static org.hypernomicon.model.records.SimpleRecordTypes.WorkTypeEnum.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;

//---------------------------------------------------------------------------

public class SimpleRecordTypes
{
 
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------  
  
  public interface HDT_RecordWithDescription extends HDT_Base { public MainText getDesc(); }
  public interface HDT_RecordWithPath extends HDT_Base        { public HyperPath getPath(); }
  
  public static abstract class HDT_SimpleRecord extends HDT_Record
  {    
    HDT_SimpleRecord(HDT_RecordState xmlState, HyperDataset<? extends HDT_SimpleRecord> dataset)
    {
      super(xmlState, dataset);
    }
    
    @Override public String listName() { return name(); }       
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static abstract class HDT_Verdict extends HDT_SimpleRecord
  {
    public HDT_Verdict(HDT_RecordState xmlState, HyperDataset<? extends HDT_Verdict> dataset)       { super(xmlState, dataset); }
    
    @Override public String getCBText() { return name(); }
    @Override public String listName()  { return getTagString(tagListName); }
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static final class HDT_State extends HDT_SimpleRecord
  { HDT_State(HDT_RecordState xmlState, HyperDataset<HDT_State> dataset)                            { super(xmlState, dataset);  } 
    public HDT_RecordType getType()                                                                 { return hdtState;           } }
  
  public static final class HDT_Country extends HDT_SimpleRecord
  { HDT_Country(HDT_RecordState xmlState, HyperDataset<HDT_Country> dataset)                        { super(xmlState, dataset);  } 
    public HDT_RecordType getType()                                                                 { return hdtCountry;         } }
  
  public static final class HDT_Rank extends HDT_SimpleRecord
  { HDT_Rank(HDT_RecordState xmlState, HyperDataset<HDT_Rank> dataset)                              { super(xmlState, dataset);  } 
    public HDT_RecordType getType()                                                                 { return hdtRank;            } }
  
  public static final class HDT_PersonStatus extends HDT_SimpleRecord
  { HDT_PersonStatus(HDT_RecordState xmlState, HyperDataset<HDT_PersonStatus> dataset)              { super(xmlState, dataset);  } 
    public HDT_RecordType getType()                                                                 { return hdtPersonStatus;    } }
 
  public static final class HDT_Field extends HDT_SimpleRecord
  { HDT_Field(HDT_RecordState xmlState, HyperDataset<HDT_Field> dataset)                            { super(xmlState, dataset);  } 
    public HDT_RecordType getType()                                                                 { return hdtField;           } }
 
  public static final class HDT_PositionVerdict extends HDT_Verdict
  { public HDT_PositionVerdict(HDT_RecordState xmlState, HyperDataset<HDT_PositionVerdict> dataset) { super(xmlState, dataset);  } 
    public HDT_RecordType getType()                                                                 { return hdtPositionVerdict; } }

  public static final class HDT_ArgumentVerdict extends HDT_Verdict 
  { public HDT_ArgumentVerdict(HDT_RecordState xmlState, HyperDataset<HDT_ArgumentVerdict> dataset) { super(xmlState, dataset);  } 
    public HDT_RecordType getType()                                                                 { return hdtArgumentVerdict; } }
  
  public static final class HDT_InstitutionType extends HDT_SimpleRecord
  { 
    public static final int FACULTY_INST_TYPE_ID = 9;
    public static final int DEPARTMENT_INST_TYPE_ID = 10;
    
    HDT_InstitutionType(HDT_RecordState xmlState, HyperDataset<HDT_InstitutionType> dataset)        { super(xmlState, dataset);  }
    public HDT_RecordType getType()                                                                 { return hdtInstitutionType; } }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static final class HDT_FileType extends HDT_SimpleRecord                    
  { 
    public List<HDT_MiscFile> miscFiles;
    
    HDT_FileType(HDT_RecordState xmlState, HyperDataset<HDT_FileType> dataset)               
    { 
      super(xmlState, dataset); 
      miscFiles = getSubjList(rtTypeOfFile);
    }
    
    @Override public HDT_RecordType getType()  { return hdtFileType; }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static enum WorkTypeEnum
  {
    wtNone,
    wtPaper,
    wtBook,
    wtWebPage,
    wtChapter,
    wtRecording,
    wtUnenteredSet
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static final class HDT_WorkType extends HDT_SimpleRecord
  {
    private static final EnumHashBiMap<WorkTypeEnum, Integer> enumMap = initEnumMap();
    
    private static final EnumHashBiMap<WorkTypeEnum, Integer> initEnumMap()
    {
      EnumHashBiMap<WorkTypeEnum, Integer> map = EnumHashBiMap.create(WorkTypeEnum.class);
      
      map.put(wtPaper, 1);
      map.put(wtBook, 2);
      map.put(wtWebPage, 3);
      map.put(wtChapter, 4);
      map.put(wtRecording, 5);
      map.put(wtUnenteredSet, 6);
      
      return map;
    }
    
    HDT_WorkType(HDT_RecordState xmlState, HyperDataset<HDT_WorkType> dataset) { super(xmlState, dataset); }

    @Override public HDT_RecordType getType()                { return hdtWorkType; }    
    public WorkTypeEnum getEnumVal()                         { return workTypeIDToEnumVal(getID()); }    
    public static HDT_WorkType get(WorkTypeEnum enumVal)     { return db.workTypes.getByID(enumMap.get(enumVal)); }    
    public static WorkTypeEnum workTypeIDToEnumVal(int wtID) { return enumMap.inverse().getOrDefault(wtID, wtNone); }
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------  

}