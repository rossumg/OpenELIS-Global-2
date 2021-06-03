package org.openelisglobal.etl.service;

import java.util.List;

import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;
import org.openelisglobal.etl.dao.ETLDAO;
import org.openelisglobal.etl.valueholder.ETLRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ETLServiceImpl extends BaseObjectServiceImpl<ETLRecord, String>
        implements ETLService {
    @Autowired
    protected ETLDAO baseObjectDAO;

    ETLServiceImpl() {
        super(ETLRecord.class);
    }

    @Override
    protected ETLDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getAllElectronicOrdersOrderedBy(SortOrder order) {
        return getBaseObjectDAO().getAllElectronicOrdersOrderedBy(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getElectronicOrdersByExternalId(String id) {
        return getBaseObjectDAO().getElectronicOrdersByExternalId(id);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder order) {
        return getBaseObjectDAO().getAllElectronicOrdersContainingValueOrderedBy(searchValue, order);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order) {
        return getBaseObjectDAO().getAllElectronicOrdersContainingValuesOrderedBy(accessionNumber, patientLastName,
                patientFirstName, gender, order);
    }

}
