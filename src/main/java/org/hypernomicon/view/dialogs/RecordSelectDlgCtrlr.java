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

package org.hypernomicon.view.dialogs;

import org.hypernomicon.model.records.HDT_RecordType;
import org.hypernomicon.view.wrappers.HyperTableCell;

import static org.hypernomicon.util.Util.*;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.util.StringConverter;

public class RecordSelectDlgCtrlr extends HyperDlg
{
  @FXML private Button btnOK, btnCancel;
  @FXML public ListView<HyperTableCell> listView;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static RecordSelectDlgCtrlr create(String title, List<HyperTableCell> list)
  {
    RecordSelectDlgCtrlr rsd = HyperDlg.create("RecordSelectDlg.fxml", title, true);
    rsd.init(list);
    return rsd;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void init(List<HyperTableCell> list)
  {
    if (collEmpty(list)) return;
    HDT_RecordType objType = HyperTableCell.getCellType(list.get(0));

    listView.setItems(FXCollections.observableArrayList(list));

    StringConverter<HyperTableCell> strConv = new StringConverter<HyperTableCell>()
    {
      @Override public String toString(HyperTableCell cell)     { return HyperTableCell.getCellText(cell); }
      @Override public HyperTableCell fromString(String string) { return new HyperTableCell(-1, string, objType); }
    };

    listView.setCellFactory(TextFieldListCell.forListView(strConv));

    listView.setOnMouseClicked(mouseEvent ->
    {
      if ((mouseEvent.getButton().equals(MouseButton.PRIMARY)) && (mouseEvent.getClickCount() == 2))
        btnOkClick();
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected boolean isValid()
  {
    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------


}
