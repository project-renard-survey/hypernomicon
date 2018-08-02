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

package org.hypernomicon.bib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import static org.hypernomicon.util.Util.*;

import com.google.common.collect.Iterators;

import org.hypernomicon.bib.BibData.AuthorType;
import org.hypernomicon.model.PersonName;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

public class BibAuthorsStandalone extends BibAuthors
{
  private EnumMap<AuthorType, List<BibAuthor>> authors = new EnumMap<>(AuthorType.class);
  private String oneLiner; // Sometimes all the authors appear in one line, in various formats and with various delimiting characters
  
  public BibAuthorsStandalone()  
  {
    authors.put(AuthorType.author, new ArrayList<>());
    authors.put(AuthorType.editor, new ArrayList<>());
    authors.put(AuthorType.translator, new ArrayList<>());
  }
    
  @Override public void add(BibAuthor author)   { authors.get(author.getType()).add(author); }
   
  public void setOneLiner(String str)
  { 
    oneLiner = ultraTrim(convertToSingleLine(safeStr(str))); 
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean listsAreEmpty()
  {
    return ((authors.get(AuthorType.author).size() == 0) &&
            (authors.get(AuthorType.editor).size() == 0) &&
            (authors.get(AuthorType.translator).size() == 0));    
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean isEmpty()
  {
    if (listsAreEmpty() == false)
      return false;
    
    return safeStr(oneLiner).length() == 0;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void clear() 
  { 
    authors.get(AuthorType.author).clear(); 
    authors.get(AuthorType.editor).clear();
    authors.get(AuthorType.translator).clear();
    oneLiner = "";
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String getStr()
  {
    if (listsAreEmpty())
      if (safeStr(oneLiner).length() > 0)
        return oneLiner;
    
    return super.getStr();    
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String getStr(AuthorType authorType)
  {
    if (authorType == AuthorType.author)
    {
      if (listsAreEmpty())
        if (safeStr(oneLiner).length() > 0)
          return oneLiner;
    }
    
    return super.getStr(authorType);
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private List<BibAuthor> getOneLinerAsList()
  {
    if (oneLiner == null)
      oneLiner = "";
    
    if (oneLiner.length() == 0)
      return Collections.emptyList();
    
    List<BibAuthor> list = new ArrayList<>();
    
    for (String authorStr : oneLiner.split(";"))
      list.add(new BibAuthor(AuthorType.author, new PersonName(authorStr)));
    
    return list;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public Iterator<BibAuthor> iterator()
  {
    if (listsAreEmpty())
      return getOneLinerAsList().iterator();
    
    return readOnlyIterator(Iterators.concat(authors.get(AuthorType.author).iterator(),
                                             authors.get(AuthorType.editor).iterator(),
                                             authors.get(AuthorType.translator).iterator()));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void getLists(ArrayList<BibAuthor> authorList, ArrayList<BibAuthor> editorList, ArrayList<BibAuthor> translatorList)
  {
    if (listsAreEmpty())
    {
      authorList.addAll(getOneLinerAsList());
      return;
    }
    
    authorList.addAll(authors.get(AuthorType.author));
    editorList.addAll(authors.get(AuthorType.editor));
    translatorList.addAll(authors.get(AuthorType.translator));
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}