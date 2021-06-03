package org.openelisglobal.etl.form;

import java.util.List;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.SafeHtml;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.valueholder.Dictionary;

public class ETLMenuForm extends AdminOptionMenuForm<Dictionary> {
    /**
     *
     */
    private static final long serialVersionUID = -1585240883233995437L;

    // for display
    private List<Dictionary> menuList;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    @SafeHtml(whitelistType = SafeHtml.WhiteListType.NONE)
    private String searchString = "";

    public ETLMenuForm() {
        setFormName("ETLMenuForm");
    }

    @Override
    public List<Dictionary> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<Dictionary> menuList) {
        this.menuList = menuList;
    }

    @Override
    public List<String> getSelectedIDs() {
        return selectedIDs;
    }

    @Override
    public void setSelectedIDs(List<String> selectedIDs) {
        this.selectedIDs = selectedIDs;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }
}
