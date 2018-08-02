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

package org.hypernomicon.util.filePath;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.collect.Sets;

import java.util.Set;

import org.hypernomicon.util.FilenameMap;

public class FilePathSet implements Set<FilePath>
{
  private FilenameMap<Set<FilePath>> nameToPaths;

  public FilePathSet()
  {
    nameToPaths = new FilenameMap<Set<FilePath>>();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public int size()
  {
    int cnt = 0;
    
    for (Entry<String, Set<FilePath>> pathSet : nameToPaths.entrySet())
      cnt += pathSet.getValue().size();
    
    return cnt;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean isEmpty()
  {
    return size() == 0;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean contains(Object o)
  {
    FilePath filePath;
    
    if (o instanceof String)
      filePath = new FilePath((String)o);
    else if (o instanceof Path)
      filePath = new FilePath((Path)o);
    else if (o instanceof File)
      filePath = new FilePath((File)o);
    else if (o instanceof FilePath)
      filePath = (FilePath)o;
    else
      return false;
    
    Set<FilePath> set = nameToPaths.get(filePath.getNameOnly().toString());
    if (set == null) return false;
    
    for (FilePath setPath : set)
      if (setPath.equals(filePath))
        return true;
    
    return false;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void clear()
  {
    nameToPaths.clear();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public Iterator<FilePath> iterator()
  {
    return new FilePathIterator(nameToPaths);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public Object[] toArray()
  {
    Object[] array = new Object[size()];
    
    int ndx = 0;
    for (Set<FilePath> pathSet : nameToPaths.values())
      for (FilePath filePath : pathSet)
      {
        array[ndx] = filePath;
        ndx++;
      }
    
    return array;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  @Override public <T> T[] toArray(T[] a)
  { 
    if (a.length < size())
      a = (T[]) new FilePath[size()];
        
    int ndx = 0;
    for (Set<FilePath> pathSet : nameToPaths.values())
      for (FilePath filePath : pathSet)
      {
        a[ndx] = (T) filePath;
        ndx++;
      }
    
    return a;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean add(FilePath filePath)
  {
    if (FilePath.isEmpty(filePath))
      throw new UnsupportedOperationException("Unable to add null path to FilePathSet: That operation is not supported.");
    
    if (contains(filePath)) return false;
    
    String nameStr = filePath.getNameOnly().toString();
    
    Set<FilePath> set = nameToPaths.get(nameStr);
    
    if (set == null)
    {
      set = Sets.newConcurrentHashSet();
      nameToPaths.put(nameStr, set);
    }
    
    return set.add(filePath);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean remove(Object o)
  {
    if ((o instanceof FilePath) == false) return false;
    
    FilePath filePath = (FilePath)o;
    if (contains(filePath) == false) return false;
    
    String nameStr = filePath.getNameOnly().toString();
    
    Set<FilePath> set = nameToPaths.get(nameStr);
    
    if (set == null) return false;
    
    for (FilePath otherPath : set)
      if (otherPath.equals(filePath))
      {
        set.remove(otherPath);
        return true;
      }
    
    return false;    
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean containsAll(Collection<?> c)
  {
    for (Object o : c)
      if (this.contains(o) == false) return false;
    
    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean addAll(Collection<? extends FilePath> c)
  {
    boolean changed = false;
    
    for (FilePath filePath : c)
      if (add(filePath)) changed = true;
    
    return changed;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean retainAll(Collection<?> c)
  {
    boolean changed = false;
    Iterator<FilePath> iterator = iterator();
    
    while (iterator.hasNext())
    {
      FilePath filePath = iterator.next();
      if (c.contains(filePath) == false)
      {
        iterator.remove();
        changed = true;
      }
    }
    
    return changed;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean removeAll(Collection<?> c)
  {
    boolean changed = false;

    for (Object o : c)
      if (remove(o)) changed = true;
    
    return changed;
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}