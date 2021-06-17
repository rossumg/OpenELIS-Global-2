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
import org.openelisglobal.internationalization.MessageUtil;
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
    private String patientId;

    private String statusId;
    private StatusOfSample status; // not persisted
    private Timestamp orderTimestamp;
    private String data;
    
    private String labno;
    private String identifier;
    private String first_name;
    private String last_name;
    private String home_phone;
    private String work_phone;
    private String sex;
    private Timestamp birthdate;
    private String address_street;
    private String address_city;
    private String address_country;

    private Integer age_years;
    private Integer age_months;
    private Integer age_weeks;
    
    private Timestamp date_recpt;
    private Timestamp date_entered;
    private Timestamp date_collect;
    private String code_referer;
    private String referer;
    private String program;
    private String order_status;
    private String test;
 

//    Antibody Covid (IgM/IgG)(Blood)
//    COVID-19 PCR(Sputum)

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

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
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
    
    public Timestamp getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(Timestamp birthdate) {
        this.birthdate = birthdate;
    }

    public int getAge_years() {
        return age_years;
    }

    public void setAge_years(int age_years) {
        this.age_years = age_years;
    }

    public int getAge_months() {
        return age_months;
    }

    public void setAge_months(int age_months) {
        this.age_months = age_months;
    }

    public int getAge_weeks() {
        return age_weeks;
    }

    public void setAge_weeks(int age_weeks) {
        this.age_weeks = age_weeks;
    }

    public Timestamp getDate_recpt() {
        return date_recpt;
    }

    public void setDate_recpt(Timestamp date_recpt) {
        this.date_recpt = date_recpt;
    }

    public Timestamp getDate_entered() {
        return date_entered;
    }

    public void setDate_entered(Timestamp date_entered) {
        this.date_entered = date_entered;
    }

    public Timestamp getDate_collect() {
        return date_collect;
    }

    public void setDate_collect(Timestamp date_collect) {
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
    
    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getHome_phone() {
        return home_phone;
    }

    public void setHome_phone(String home_phone) {
        this.home_phone = home_phone;
    }

    public String getWork_phone() {
        return work_phone;
    }

    public void setWork_phone(String work_phone) {
        this.work_phone = work_phone;
    }

    public String getAddress_street() {
        return address_street;
    }

    public void setAddress_street(String address_street) {
        this.address_street = address_street;
    }

    public String getAddress_city() {
        return address_city;
    }

    public void setAddress_city(String address_city) {
        this.address_city = address_city;
    }

    public String getAddress_country() {
        return address_country;
    }

    public void setAddress_country(String address_country) {
        this.address_country = address_country;
    }

}
