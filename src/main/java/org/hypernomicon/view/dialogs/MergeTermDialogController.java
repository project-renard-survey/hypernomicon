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

package org.hypernomicon.view.dialogs;

import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;

import org.hypernomicon.model.records.HDT_Term;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

public class MergeTermDialogController extends HyperDialog
{
  @FXML public RadioButton rbName1;
  @FXML public RadioButton rbName2;
  @FXML public RadioButton rbName3;
  @FXML public RadioButton rbKey1;
  @FXML public RadioButton rbKey2;
  @FXML public RadioButton rbKey3;
  @FXML public TextField tfName1;
  @FXML public TextField tfName2;
  @FXML public TextField tfName3;
  @FXML public TextField tfKey1;
  @FXML public TextField tfKey2;
  @FXML public TextField tfKey3;
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  @Override protected boolean isValid()
  {
    if (getKey().replace("^", "").replace("$", "").trim().length() < 3)
    {
      messageDialog("Search key of a term record cannot be zero-length.", mtError);
      
      if (rbKey1.isSelected())
        safeFocus(tfKey1);
      else if (rbKey2.isSelected())
        safeFocus(tfKey2);
      else
        safeFocus(tfKey3);
      
      return false;
    }
    
    return true;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public static MergeTermDialogController create(String title, HDT_Term term1, HDT_Term term2)
  {
    MergeTermDialogController mtd = HyperDialog.create("MergeTermDialog.fxml", title, true);
    mtd.init(term1, term2);
    return mtd;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private void init(HDT_Term term1, HDT_Term term2)
  {   
    String name1 = term1.listName(),
           name2 = term2.listName(),
           key1 = term1.getSearchKey(),
           key2 = term2.getSearchKey();
    
    tfName1.setText(name1);
    tfName2.setText(name2);
    
    if (name1.length() == 0)
      if (name2.length() > 0)
        rbName2.setSelected(true);

    tfKey1.setText(key1);
    tfKey2.setText(key2);
    
    if (key1.length() == 0)
      if (key2.length() > 0)
        rbKey2.setSelected(true);
    
    tfName3.textProperty().addListener((observable, oldValue, newValue) -> rbName3.setSelected(true));
    tfKey3.textProperty().addListener((observable, oldValue, newValue) -> rbKey3.setSelected(true));
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public String getName()
  {
    if (rbName1.isSelected())
      return tfName1.getText();
    else if (rbName2.isSelected())
      return tfName2.getText();
    else
      return tfName3.getText();
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public String getKey()
  {
    if (rbKey1.isSelected())
      return tfKey1.getText();
    else if (rbKey2.isSelected())
      return tfKey2.getText();
    else
      return tfKey3.getText();
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

}