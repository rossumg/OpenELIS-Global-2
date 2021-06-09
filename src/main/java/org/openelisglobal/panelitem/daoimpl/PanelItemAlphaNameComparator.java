package org.openelisglobal.panelitem.daoimpl;

import java.util.Comparator;

import org.openelisglobal.panelitem.valueholder.PanelItem;

public class PanelItemAlphaNameComparator implements Comparator<Object> {

    @Override
    public int compare(Object o1, Object o2) {
            return ((PanelItem) o1).getTest().getName().compareTo(((PanelItem) o2).getTest().getName());
    }
}


