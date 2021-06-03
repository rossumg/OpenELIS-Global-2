/**
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 *
 */

package org.openelisglobal.etl.valueholder;

import java.sql.Timestamp;

import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;

public class ETLRecord extends BaseObject<String> {

    public enum SortOrder {
        STATUS_ID("statusId", "eorder.status"),
        LAST_UPDATED_ASC("lastupdatedasc", "eorder.lastupdatedasc"),
        LAST_UPDATED_DESC("lastupdateddesc", "eorder.lastupdateddesc"),
        EXTERNAL_ID("externalId", "eorder.externalid");

        private String value;
        private String displayKey;

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return MessageUtil.getMessage(displayKey);
        }

        SortOrder(String value, String displayKey) {
            this.value = value;
            this.displayKey = displayKey;
        }

        public static SortOrder fromString(String value) {
            for (SortOrder so : SortOrder.values()) {
                if (so.value.equalsIgnoreCase(value)) {
                    return so;
                }
            }
            return null;
        }
    }

    private static final long serialVersionUID = 5573858445160470854L;

    private String id;
    private String externalId;
    private ValueHolder patient;
    private String statusId;
    private StatusOfSample status; // not persisted
    private Timestamp orderTimestamp;
    private String data;
    
    private String labno;
    private String identifier;
    private String sex;
    private String birthdate;
    private String age_years;
    private String age_months;
    private String age_weeks;
    private String date_recpt;
    private String date_entered;
    private String date_collect;
    private String code_referer;
    private String referer;
    private String program;
    private String order_status;
    private String test;
 

//    Antibody Covid (IgM/IgG)(Blood)
//    COVID-19 PCR(Sputum)

    public ETLRecord() {
        patient = new ValueHolder();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Patient getPatient() {
        return (Patient) patient.getValue();
    }

    public void setPatient(Patient patient) {
        this.patient.setValue(patient);
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public StatusOfSample getStatus() {
        return status;
    }

    public void setStatus(StatusOfSample status) {
        this.status = status;
    }

    public Timestamp getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(Timestamp orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
    
 

    public void setPatient(ValueHolder patient) {
        this.patient = patient;
    }

    public String getLabno() {
        return labno;
    }

    public void setLabno(String labno) {
        this.labno = labno;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getAge_years() {
        return age_years;
    }

    public void setAge_years(String age_years) {
        this.age_years = age_years;
    }

    public String getAge_months() {
        return age_months;
    }

    public void setAge_months(String age_months) {
        this.age_months = age_months;
    }

    public String getAge_weeks() {
        return age_weeks;
    }

    public void setAge_weeks(String age_weeks) {
        this.age_weeks = age_weeks;
    }

    public String getDate_recpt() {
        return date_recpt;
    }

    public void setDate_recpt(String date_recpt) {
        this.date_recpt = date_recpt;
    }

    public String getDate_entered() {
        return date_entered;
    }

    public void setDate_entered(String date_entered) {
        this.date_entered = date_entered;
    }

    public String getDate_collect() {
        return date_collect;
    }

    public void setDate_collect(String date_collect) {
        this.date_collect = date_collect;
    }

    public String getCode_referer() {
        return code_referer;
    }

    public void setCode_referer(String code_referer) {
        this.code_referer = code_referer;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getOrder_status() {
        return order_status;
    }

    public void setOrder_status(String order_status) {
        this.order_status = order_status;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    private String result;

}
