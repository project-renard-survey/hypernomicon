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

import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;

import java.util.LinkedHashSet;
import java.util.List;

import org.hypernomicon.model.HyperDataset;
import org.hypernomicon.model.items.Author;
import org.hypernomicon.view.wrappers.HyperTable;

public class HDT_Position extends HDT_RecordWithConnector
{
  public List<HDT_Debate> debates;
  public List<HDT_Position> largerPositions;
  
  public List<HDT_Argument> arguments;
  public List<HDT_Position> subPositions;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HDT_Position(HDT_RecordState xmlState, HyperDataset<HDT_Position> dataset)
  {
    super(xmlState, dataset);
       
    nameTag = tagName;
    
    debates = getObjList(rtDebateOfPosition);
    largerPositions = getObjList(rtParentPosOfPos);
    
    arguments = getSubjList(rtPositionOfArgument);
    subPositions = getSubjList(rtParentPosOfPos);
  } 
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  
  @Override public String listName()        { return name(); }
  @Override public HDT_RecordType getType() { return hdtPosition; }
  @Override public boolean isUnitable()     { return true; }
  
  public void setLargerPositions(HyperTable ht) { updateObjectsFromHT(rtParentPosOfPos, ht, 3); };
  public void setDebates(HyperTable ht)         { updateObjectsFromHT(rtDebateOfPosition, ht, 3); };
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  
  public HDT_Debate getDebate()
  {
    HDT_Position position = this;

    while ((position.debates.size() == 0) && (position.largerPositions.size() > 0))
      position = position.largerPositions.get(0);

    if (position.debates.size() > 0)
      return position.debates.get(0);
    
    return null;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public LinkedHashSet<Author> getPeople()
  {
    LinkedHashSet<Author> people = new LinkedHashSet<>();
    
    subPositions.forEach(subPos -> people.addAll(subPos.getPeople()));
    
    for (HDT_Argument arg : arguments)
      if (arg.isInFavor(this))
        people.addAll(arg.getPeople());
    
    return people;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public class PositionSource
  {
    public HDT_Argument argument = null;
    public HDT_Work work = null;
    public HDT_Person author = null;   
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public PositionSource getLaunchableWork()   { return getSource(true, false, false); }
  public PositionSource getWork()             { return getSource(true, false, true); }
  public PositionSource getArgument()         { return getSource(true, true, true); }
  public PositionSource getWorkWithAuthor()   { return getSource(false, false, true); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private PositionSource getSource(boolean noAuthorOK, boolean noWorkOK, boolean noFileNameOK)
  { 
    PositionSource ps = new PositionSource();
    PositionSource webPs = null;

    for (HDT_Argument arg : arguments)
    {
      if (arg.isInFavor(this))
      {
        ps.argument = arg;
        
        for (HDT_Work work : arg.works)
        {          
          if (work.getPath().isEmpty() == false)
          {
            if (work.authorRecords.size() > 0)
            {
              ps.work = work;
              ps.author = work.authorRecords.get(0);
              return ps;
            }
            
            if (noAuthorOK) 
            {
              ps.work = work;
              return ps;
            }
          }
          
          else if ((work.getWebLink().isEmpty() == false) && (webPs == null))
          {            
            if (work.authorRecords.size() > 0)
            {
              webPs = new PositionSource();
              webPs.work = work;
              webPs.author = work.authorRecords.get(0);
            }
            else if (noAuthorOK) 
            {
              webPs = new PositionSource();
              webPs.work = work;
            }            
          }

          if (work.authorRecords.size() > 0)
          {
            if (noFileNameOK)
            {
              ps.work = work;
              ps.author = work.authorRecords.get(0);
              return ps;
            }
          }

          if ((noAuthorOK) && (noFileNameOK))
          {
            ps.work = work;
            return ps;
          }
        }
        
        if (noWorkOK) return ps;
      }
    }
  
    return webPs;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}