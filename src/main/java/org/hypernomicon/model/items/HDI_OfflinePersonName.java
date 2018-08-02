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

package org.hypernomicon.model.items;

import org.hypernomicon.model.HDI_Schema;
import org.hypernomicon.model.HyperDB.Tag;
import org.hypernomicon.model.records.HDT_RecordState;
import org.hypernomicon.model.records.HDT_RecordType;

import static org.hypernomicon.model.records.HDT_RecordState.*;
import static org.hypernomicon.util.Util.*;

import java.util.LinkedHashMap;

public class HDI_OfflinePersonName extends HDI_OfflineBase
{
  String firstName = "", lastName = "";
  
  public HDI_OfflinePersonName(HDI_Schema newSchema, HDT_RecordState recordState)
  {
    super(newSchema, recordState);
  }
  
  public String getFirstName() { return firstName; }
  public String getLastName()  { return lastName; }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  
  @Override public void setFromXml(Tag tag, String nodeText, HDT_RecordType objType, int objID, LinkedHashMap<Tag, HDI_OfflineBase> nestedItems)
  {
    if (tag == Tag.tagFirstName)     firstName = ultraTrim(convertToSingleLine(nodeText));
    else if (tag == Tag.tagLastName) lastName  = ultraTrim(convertToSingleLine(nodeText));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void writeToXml(Tag tag, StringBuilder xml)
  {
    if (tag == Tag.tagFirstName)     writeStringTag(xml, tag, firstName);
    else if (tag == Tag.tagLastName) writeStringTag(xml, tag, lastName);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}