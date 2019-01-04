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

package org.hypernomicon.model.records;

import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.hypernomicon.model.HyperDataset;
import org.hypernomicon.model.items.Author;
import org.hypernomicon.model.Exceptions.RelationCycleException;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_ArgumentVerdict;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_PositionVerdict;
import org.hypernomicon.model.relations.HyperObjList;

//---------------------------------------------------------------------------

public class HDT_Argument extends HDT_RecordWithConnector
{
  public final List<HDT_Position> positions;
  public final List<HDT_Argument> counteredArgs;
  public final List<HDT_Work> works;
  public final List<HDT_Argument> counterArgs;

  public HDT_Argument(HDT_RecordState xmlState, HyperDataset<HDT_Argument> dataset)
  {
    super(xmlState, dataset, tagName);

    if (dataset != null)
    {
      positions = Collections.unmodifiableList(getObjList(rtPositionOfArgument));
      counteredArgs = Collections.unmodifiableList(getObjList(rtCounterOfArgument));
      works = getObjList(rtWorkOfArgument);
      counterArgs = getSubjList(rtCounterOfArgument);
    }
    else
    {
      positions = null; counteredArgs = null; works = null; counterArgs = null;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String listName()            { return name(); }
  @Override public HDT_RecordType getType()     { return hdtArgument; }

  public void setWorks(List<HDT_Work> list) { updateObjectsFromList(rtWorkOfArgument, list); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HDT_PositionVerdict getPosVerdict(HDT_Position position)
  {
    return (HDT_PositionVerdict) db.getNestedPointer(this, position, tagPositionVerdict);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HDT_ArgumentVerdict getArgVerdict(HDT_Argument arg)
  {
    return (HDT_ArgumentVerdict) db.getNestedPointer(this, arg, tagArgumentVerdict);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void addCounterArg(HDT_Argument countered, HDT_ArgumentVerdict verdict)
  {
    HyperObjList<HDT_Argument, HDT_Argument> list = getObjList(rtCounterOfArgument);
    if (list.add(countered) == false)
    {
      try                              { list.throwLastException(); }
      catch (RelationCycleException e) { messageDialog(e.getMessage(), mtError); }

      return;
    }

    if (verdict == null) return;

    db.updateNestedPointer(this, countered, tagArgumentVerdict, verdict);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void addPosition(HDT_Position position, HDT_PositionVerdict verdict)
  {
    if (getObjList(rtPositionOfArgument).add(position) == false)
      return;

    if (verdict == null) return;

    db.updateNestedPointer(this, position, tagPositionVerdict, verdict);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean isInFavor(HDT_Position position)
  {
    HDT_PositionVerdict verdict = this.getPosVerdict(position);

    if (verdict == null) return false;

    switch (verdict.getID())
    {
      case 1: case 3: case 6: case 17:
        return true;
      default: break;
    }

    return false;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HDT_Debate getDebate()
  {
    HDT_Debate debate = null;

    for (HDT_Position curPos : positions)
    {
      debate = curPos.getDebate();
      if (debate != null) return debate;
    }

    for (HDT_Argument curArg : counteredArgs)
    {
      debate = curArg.getDebate();
      if (debate != null) return debate;
    }

    return null;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public LinkedHashSet<Author> getPeople()
  {
    LinkedHashSet<Author> people = new LinkedHashSet<>();

    works.forEach(work -> work.getAuthors().forEach(people::add));

    return people;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
