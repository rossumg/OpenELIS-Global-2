package org.openelisglobal.etl.service;

import java.util.List;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;
import org.openelisglobal.etl.valueholder.ETLRecord;

public interface ETLService extends BaseObjectService<ETLRecord, String> {

    List<ElectronicOrder> getAllElectronicOrdersOrderedBy(ElectronicOrder.SortOrder order);

    List<ElectronicOrder> getElectronicOrdersByExternalId(String id);

    List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder sortOrder);

    List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order);

}
