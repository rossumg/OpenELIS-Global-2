package org.openelisglobal.dataexchange.fhir.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestIntent;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestPriority;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Specimen.SpecimenCollectionComponent;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskPriority;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.json.simple.JSONObject;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.JSONUtils;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.etl.valueholder.ETLRecord;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;

@Service
public class FhirTransformServiceImpl implements FhirTransformService {
    
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ProviderService providerService;

    @Transactional
    @Async
    @Override
    public AsyncResult<Bundle> transformPersistPatients(List<String> patientIds) throws FhirLocalPersistingException {
        FhirOperations fhirOperations = new FhirOperations();
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();

        Map<String, org.hl7.fhir.r4.model.Patient> fhirPatients = new HashMap<>();
        for (String patientId : patientIds) {
            Patient patient = patientService.get(patientId);
            if (patient.getFhirUuid() == null) {
                patient.setFhirUuid(UUID.randomUUID());
            }
            org.hl7.fhir.r4.model.Patient fhirPatient = this.transformToFhirPatient(patient);
            if (fhirPatients.containsKey(fhirPatient.getIdElement().getIdPart())) {
                LogEvent.logWarn("", "", "patient collision with id: " + fhirPatient.getIdElement().getIdPart());
            }
            fhirPatients.put(fhirPatient.getIdElement().getIdPart(), fhirPatient);

        }

        for (org.hl7.fhir.r4.model.Patient fhirPatient : fhirPatients.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, fhirPatient);
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        return new AsyncResult<>(responseBundle);
    }

    @Transactional
    @Async
    @Override
    public AsyncResult<Bundle> transformPersistObjectsUnderSamples(List<String> sampleIds)
            throws FhirLocalPersistingException {
        FhirOperations fhirOperations = new FhirOperations();
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();

        Map<String, Task> tasks = new HashMap<>();
        Map<String, org.hl7.fhir.r4.model.Patient> fhirPatients = new HashMap<>();
        Map<String, Specimen> specimens = new HashMap<>();
        Map<String, Practitioner> practitioners = new HashMap<>();
        Map<String, ServiceRequest> serviceRequests = new HashMap<>();
        Map<String, DiagnosticReport> diagnosticReports = new HashMap<>();
        Map<String, Observation> observations = new HashMap<>();
        for (String sampleId : sampleIds) {
            Sample sample = sampleService.get(sampleId);
            Patient patient = sampleHumanService.getPatientForSample(sample);
            Provider provider = sampleHumanService.getProviderForSample(sample);
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sampleId);
            List<Analysis> analysises = analysisService.getAnalysesBySampleId(sampleId);
            List<Result> results = resultService.getResultsForSample(sample);

            if (sample.getFhirUuid() == null) {
                sample.setFhirUuid(UUID.randomUUID());
            }
            if (patient.getFhirUuid() == null) {
                patient.setFhirUuid(UUID.randomUUID());
            }
            if (provider.getFhirUuid() == null) {
                provider.setFhirUuid(UUID.randomUUID());
            }
            
            sampleItems.stream().forEach((e) -> {
                if (e.getFhirUuid() == null) {
                    e.setFhirUuid(UUID.randomUUID());
                }
            });

            analysises.stream().forEach((e) -> {
                if (e.getFhirUuid() == null) {
                    e.setFhirUuid(UUID.randomUUID());
                }
            });

            results.stream().forEach((e) -> {
                if (e.getFhirUuid() == null) {
                    e.setFhirUuid(UUID.randomUUID());
                }
            });
            Task task = this.transformToTask(sample);
            if (tasks.containsKey(task.getIdElement().getIdPart())) {
                LogEvent.logWarn("", "", "task collision with id: " + task.getIdElement().getIdPart());
            }
            tasks.put(task.getIdElement().getIdPart(), task);

            org.hl7.fhir.r4.model.Patient fhirPatient = this.transformToFhirPatient(patient);
            if (fhirPatients.containsKey(fhirPatient.getIdElement().getIdPart())) {
                LogEvent.logWarn("", "", "patient collision with id: " + fhirPatient.getIdElement().getIdPart());
            }
            fhirPatients.put(fhirPatient.getIdElement().getIdPart(), fhirPatient);
            
            Practitioner practitioner = this.transformProviderToPractitioner(provider);
            if (practitioners.containsKey(practitioner.getIdElement().getIdPart())) {
                LogEvent.logWarn("", "", "practitioner collision with id: " + practitioner.getIdElement().getIdPart());
            }
            practitioners.put(practitioner.getIdElement().getIdPart(), practitioner);

            for (SampleItem sampleItem : sampleItems) {
                Specimen specimen = this.transformToSpecimen(sampleItem);
                if (specimens.containsKey(specimen.getIdElement().getIdPart())) {
                    LogEvent.logWarn("", "", "specimen collision with id: " + specimen.getIdElement().getIdPart());
                }
                specimens.put(specimen.getIdElement().getIdPart(), specimen);
            }
            for (Analysis analysis : analysises) {
                ServiceRequest serviceRequest = this.transformToServiceRequest(analysis);
                if (serviceRequests.containsKey(serviceRequest.getIdElement().getIdPart())) {
                    LogEvent.logWarn("", "",
                            "serviceRequest collision with id: " + serviceRequest.getIdElement().getIdPart());
                }
                serviceRequests.put(serviceRequest.getIdElement().getIdPart(), serviceRequest);
                if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                    DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis);
                    if (diagnosticReports.containsKey(analysis.getFhirUuidAsString())) {
                        LogEvent.logWarn("", "",
                                "diagnosticReport collision with id: " + diagnosticReport.getIdElement().getIdPart());
                    }
                    diagnosticReports.put(analysis.getFhirUuidAsString(), diagnosticReport);
                }
            }
            for (Result result : results) {
                Observation observation = this.transformResultToObservation(result);
                if (observations.containsKey(observation.getIdElement().getIdPart())) {
                    LogEvent.logWarn("", "",
                            "observation collision with id: " + observation.getIdElement().getIdPart());
                }
                observations.put(observation.getIdElement().getIdPart(), observation);
            }
        }

        for (Task task : tasks.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, task);
        }
        for (org.hl7.fhir.r4.model.Patient fhirPatient : fhirPatients.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, fhirPatient);
        }
        for (Practitioner practitioner : practitioners.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, practitioner);
        }
        for (Specimen specimen : specimens.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, specimen);
        }
        for (ServiceRequest serviceRequest : serviceRequests.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
        }
        for (Observation observation : observations.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }
        for (DiagnosticReport diagnosticReport : diagnosticReports.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        return new AsyncResult<>(responseBundle);
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void transformPersistPatient(PatientManagementInfo patientInfo) throws FhirLocalPersistingException {
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        org.hl7.fhir.r4.model.Patient patient = transformToFhirPatient(patientInfo.getPatientPK());
        this.addToOperations(fhirOperations, tempIdGenerator, patient);
        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void transformPersistOrderEntryFhirObjects(SamplePatientUpdateData updateData,
            PatientManagementInfo patientInfo) throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getName(), "createFhirFromSamplePatient",
                "accessionNumber - " + updateData.getAccessionNumber());
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();

        FhirOrderEntryObjects orderEntryObjects = new FhirOrderEntryObjects();
        // TODO should we create a task per service request that is part of this task so
        // we can have the ServiceRequest as the focus in those tasks?
        // task for entering the order
        Task task = transformToTask(updateData.getSample().getId());
        this.addToOperations(fhirOperations, tempIdGenerator, task);

        // patient
        org.hl7.fhir.r4.model.Patient patient = transformToFhirPatient(patientInfo.getPatientPK());
        this.addToOperations(fhirOperations, tempIdGenerator, patient);
        orderEntryObjects.patient = patient;

        Practitioner requester = transformProviderToPractitioner(updateData.getProvider().getId());
        this.addToOperations(fhirOperations, tempIdGenerator, requester);
        orderEntryObjects.requester = requester;

        // Specimens and service requests
        for (SampleTestCollection sampleTest : updateData.getSampleItemsTests()) {
            FhirSampleEntryObjects fhirSampleEntryObjects = new FhirSampleEntryObjects();
            fhirSampleEntryObjects.specimen = transformToFhirSpecimen(sampleTest);

            // TODO collector
//            fhirSampleEntryObjects.collector = transformCollectorToPractitioner(sampleTest.item.getCollector());
            fhirSampleEntryObjects.serviceRequests = transformToServiceRequests(updateData, sampleTest);

            this.addToOperations(fhirOperations, tempIdGenerator, fhirSampleEntryObjects.specimen);
//            this.addToOperations(fhirOperations, tempIdGenerator, fhirSampleEntryObjects.collector);

            for (ServiceRequest serviceRequest : fhirSampleEntryObjects.serviceRequests) {
                this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            }

            orderEntryObjects.sampleEntryObjectsList.add(fhirSampleEntryObjects);
        }

        // TODO location?
        // TODO create encounter?

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    private Practitioner transformProviderToPractitioner(String providerId) {
        return transformProviderToPractitioner(providerService.get(providerId));
    }

    private Practitioner transformProviderToPractitioner(Provider provider) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(provider.getFhirUuidAsString());
        practitioner.addName(new HumanName().setFamily(provider.getPerson().getLastName())
                .addGiven(provider.getPerson().getFirstName()));
        practitioner.setTelecom(transformToTelcom(provider.getPerson()));
        return practitioner;
    }

    private List<ContactPoint> transformToTelcom(Person person) {
        List<ContactPoint> contactPoints = new ArrayList<>();
        contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue(person.getPrimaryPhone()));
        contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.OTHER).setValue(person.getHomePhone()));
        contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.SMS).setValue(person.getWorkPhone()));
        contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.EMAIL).setValue(person.getEmail()));
        contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.FAX).setValue(person.getFax()));
        return contactPoints;
    }

    private Task transformToTask(String sampleId) {
        return this.transformToTask(sampleService.get(sampleId));
    }

    private Task transformToTask(Sample sample) {
        Task task = new Task();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        List<Analysis> analysises = sampleService.getAnalysis(sample);
        task.setId(sample.getFhirUuidAsString());

        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(sample.getReferringId());
        if (eOrders.size() > 0 && ElectronicOrderType.FHIR.equals(eOrders.get(0).getType())) {
            Task referredTask = fhirPersistanceService.getTaskBasedOnServiceRequest(sample.getReferringId())
                    .orElseThrow();
            task.addPartOf(this.createReferenceFor(referredTask));
            task.setIntent(TaskIntent.ORDER);
        } else {
            task.setIntent(TaskIntent.ORIGINALORDER);
        }
        if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Entered))) {
            task.setStatus(TaskStatus.READY);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Started))) {
            task.setStatus(TaskStatus.INPROGRESS);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Finished))) {
            task.setStatus(TaskStatus.COMPLETED);
        } else {
            task.setStatus(TaskStatus.NULL);
        }
        task.setAuthoredOn(sample.getEnteredDate());
        task.setPriority(TaskPriority.ROUTINE);
        task.addIdentifier(
                this.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_uuid", sample.getFhirUuidAsString()));
        task.addIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_accessionNumber",
                sample.getAccessionNumber()));

        for (Analysis analysis : analysises) {
            task.addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        }
        task.setFor(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));

        return task;
    }

    private DateType transformToDateElement(String strDate) throws ParseException {
        boolean dayAmbiguous = false;
        boolean monthAmbiguous = false;
        // TODO look at this logic for detecting ambiguity
        if (strDate.contains(DateUtil.AMBIGUOUS_DATE_SEGMENT)) {
            strDate = strDate.replaceFirst(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
            dayAmbiguous = true;
        }
        if (strDate.contains(DateUtil.AMBIGUOUS_DATE_SEGMENT)) {
            strDate = strDate.replaceFirst(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
            monthAmbiguous = true;
        }
        Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse(strDate);
        DateType dateType = new DateType();
        if (monthAmbiguous) {
            dateType.setValue(birthDate, TemporalPrecisionEnum.YEAR);
        } else if (dayAmbiguous) {
            dateType.setValue(birthDate, TemporalPrecisionEnum.MONTH);
        } else {
            dateType.setValue(birthDate, TemporalPrecisionEnum.DAY);
        }
        return dateType;
    }

    @Override
    public org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId) {
        return transformToFhirPatient(patientService.get(patientId));
    }

    private org.hl7.fhir.r4.model.Patient transformToFhirPatient(Patient patient) {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        String subjectNumber = patientService.getSubjectNumber(patient);
        String nationalId = patientService.getNationalId(patient);
        String guid = patientService.getGUID(patient);
        String stNumber = patientService.getSTNumber(patient);
        String uuid = patient.getFhirUuidAsString();

        fhirPatient.setId(uuid);
        fhirPatient.setIdentifier(createPatientIdentifiers(subjectNumber, nationalId, stNumber, guid, uuid));

        HumanName humanName = new HumanName();
        List<HumanName> humanNameList = new ArrayList<>();
        humanName.setFamily(patient.getPerson().getLastName());
        humanName.addGiven(patient.getPerson().getFirstName());
        humanNameList.add(humanName);
        fhirPatient.setName(humanNameList);
        
        Address address = new Address();
        address.addLine(patient.getPerson().getStreetAddress());
        address.setCity(patient.getPerson().getCity());
        address.setCountry(patient.getPerson().getCountry());
        fhirPatient.addAddress(address);

        try {
            if (patient.getBirthDateForDisplay() != null) {
                fhirPatient.setBirthDateElement(transformToDateElement(patient.getBirthDateForDisplay()));
            }
        } catch (ParseException e) {
            LogEvent.logError("patient date unparseable", e);
        }
        if (GenericValidator.isBlankOrNull(patient.getGender())) {
            fhirPatient.setGender(AdministrativeGender.UNKNOWN);
        } else if (patient.getGender().equalsIgnoreCase("M")) {
            fhirPatient.setGender(AdministrativeGender.MALE);
        } else {
            fhirPatient.setGender(AdministrativeGender.FEMALE);
        }
        fhirPatient.setTelecom(transformToTelcom(patient.getPerson()));

        return fhirPatient;
    }

    private List<Identifier> createPatientIdentifiers(String subjectNumber, String nationalId, String stNumber,
            String guid, String fhirUuid) {
        List<Identifier> identifierList = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(subjectNumber)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_subjectNumber", subjectNumber));
        }
        if (!GenericValidator.isBlankOrNull(nationalId)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_nationalId", nationalId));
        }
        if (!GenericValidator.isBlankOrNull(stNumber)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_stNumber", stNumber));
        }
        if (!GenericValidator.isBlankOrNull(guid)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_guid", guid));
        }
        if (!GenericValidator.isBlankOrNull(fhirUuid)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_uuid", fhirUuid));
        }
        return identifierList;
    }

    private List<ServiceRequest> transformToServiceRequests(SamplePatientUpdateData updateData,
            SampleTestCollection sampleTestCollection) {
        List<ServiceRequest> serviceRequestsForSampleItem = new ArrayList<>();

        for (Analysis analysis : sampleTestCollection.analysises) {
            serviceRequestsForSampleItem.add(this.transformToServiceRequest(analysis.getId()));
        }
        return serviceRequestsForSampleItem;
    }

    private ServiceRequest transformToServiceRequest(String anlaysisId) {
        return transformToServiceRequest(analysisService.get(anlaysisId));
    }

    private ServiceRequest transformToServiceRequest(Analysis analysis) {
        Sample sample = analysis.getSampleItem().getSample();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        Provider provider = sampleHumanService.getProviderForSample(sample);
        Test test = analysis.getTest();
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(analysis.getFhirUuidAsString());
        serviceRequest.addIdentifier(
                this.createIdentifier(fhirConfig.getOeFhirSystem() + "/analysis_uuid", analysis.getFhirUuidAsString()));
        serviceRequest.setRequisition(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/samp_labNo",
                analysis.getSampleItem().getSample().getAccessionNumber()));

        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(sample.getReferringId());

        if (eOrders.size() <= 0) {
            serviceRequest.setIntent(ServiceRequestIntent.ORIGINALORDER);
        } else if (ElectronicOrderType.FHIR.equals(eOrders.get(eOrders.size() - 1).getType())) {
            serviceRequest.addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, sample.getReferringId()));
            serviceRequest.setIntent(ServiceRequestIntent.ORDER);
        } else if (ElectronicOrderType.HL7_V2.equals(eOrders.get(eOrders.size() - 1).getType())) {
            serviceRequest.setIntent(ServiceRequestIntent.ORDER);
        }

        if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            serviceRequest.setStatus(ServiceRequestStatus.COMPLETED);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Canceled))) {
            serviceRequest.setStatus(ServiceRequestStatus.REVOKED);
        } else {
            serviceRequest.setStatus(ServiceRequestStatus.UNKNOWN);
        }
        ObservationHistory program = observationHistoryService.getObservationHistoriesBySampleIdAndType(sample.getId(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.PROGRAM));
        if (program != null) {
            serviceRequest.addCategory(transformSampleProgramToCodeableConcept(program));
        }
        serviceRequest.setPriority(ServiceRequestPriority.ROUTINE);
        serviceRequest.setCode(transformTestToCodeableConcept(test.getId()));
        serviceRequest.setAuthoredOn(new Date());
        for (Note note : noteService.getNotes(analysis)) {
            serviceRequest.addNote(transformNoteToAnnotation(note));
        }
        // TODO performer type?

        serviceRequest.addSpecimen(
                this.createReferenceFor(ResourceType.Specimen, analysis.getSampleItem().getFhirUuidAsString()));
        serviceRequest.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        
        if (provider != null && provider.getFhirUuid() != null) {
            serviceRequest
                    .setRequester(this.createReferenceFor(ResourceType.Practitioner, provider.getFhirUuidAsString()));
        }
        // sample.getReferringId needs to be the org uuid
//        serviceRequest.addLocationReference(this.createReferenceFor(ResourceType.Person, sample.getReferringId()));

        return serviceRequest;
    }

    private CodeableConcept transformSampleProgramToCodeableConcept(ObservationHistory program) {
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(
                new Coding(fhirConfig.getOeFhirSystem() + "/sample_program", program.getValue(), program.getValue()));
        return codeableConcept;
    }

    private CodeableConcept transformTestToCodeableConcept(String testId) {
        return transformTestToCodeableConcept(testService.get(testId));
    }

    private CodeableConcept transformTestToCodeableConcept(Test test) {
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept
                .addCoding(new Coding("http://loinc.org", test.getLoinc(), test.getLocalizedTestName().getEnglish()));
        return codeableConcept;
    }

    private Specimen transformToFhirSpecimen(SampleTestCollection sampleTest) {
        Specimen specimen = this.transformToSpecimen(sampleTest.item.getId());
        if (sampleTest.initialSampleConditionIdList != null) {
            for (ObservationHistory initialSampleCondition : sampleTest.initialSampleConditionIdList) {
                specimen.addCondition(transformSampleConditionToCodeableConcept(initialSampleCondition));
            }
        }

        return specimen;
    }

    private Specimen transformToSpecimen(String sampleItemId) {
        return transformToSpecimen(sampleItemService.get(sampleItemId));
    }

    private Specimen transformToSpecimen(SampleItem sampleItem) {
        Specimen specimen = new Specimen();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());
        specimen.setId(sampleItem.getFhirUuidAsString());
        specimen.addIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/sampleItem_uuid",
                sampleItem.getFhirUuidAsString()));
        specimen.setAccessionIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/sampleItem_labNo",
                sampleItem.getSample().getAccessionNumber() + "-" + sampleItem.getSortOrder()));
        specimen.setStatus(SpecimenStatus.AVAILABLE);
        specimen.setType(transformTypeOfSampleToCodeableConcept(sampleItem.getTypeOfSample()));
        specimen.setReceivedTime(new Date());
        specimen.setCollection(transformToCollection(sampleItem.getCollectionDate(), sampleItem.getCollector()));

        for (Analysis analysis : analysisService.getAnalysesBySampleItem(sampleItem)) {
            specimen.addRequest(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        }
        specimen.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));

        return specimen;
    }

    @SuppressWarnings("unused")
    private CodeableConcept transformSampleConditionToCodeableConcept(String sampleConditionId) {
        return transformSampleConditionToCodeableConcept(observationHistoryService.get(sampleConditionId));
    }

    private CodeableConcept transformSampleConditionToCodeableConcept(ObservationHistory initialSampleCondition) {
        String observationValue;
        String observationDisplay;
        if (ValueType.DICTIONARY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = dictionaryService.get(initialSampleCondition.getValue()).getDictEntry();
            observationDisplay = dictionaryService.get(initialSampleCondition.getValue()).getDictEntryDisplayValue();
        } else if (ValueType.KEY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = localizationService.get(initialSampleCondition.getValue()).getEnglish();
            observationDisplay = "";
        } else {
            observationValue = initialSampleCondition.getValue();
            observationDisplay = "";
        }

        CodeableConcept condition = new CodeableConcept();
        condition.addCoding(
                new Coding(fhirConfig.getOeFhirSystem() + "/sample_condition", observationValue, observationDisplay));
        return condition;
    }

    private SpecimenCollectionComponent transformToCollection(Timestamp collectionDate, String collector) {
        SpecimenCollectionComponent specimenCollectionComponent = new SpecimenCollectionComponent();
        specimenCollectionComponent.setCollected(new DateTimeType(collectionDate));
        // TODO create a collector from this info
//        specimenCollectionComponent.setCollector(collector);
        return specimenCollectionComponent;
    }

    private CodeableConcept transformTypeOfSampleToCodeableConcept(String typeOfSampleId) {
        return transformTypeOfSampleToCodeableConcept(typeOfSampleService.get(typeOfSampleId));
    }

    private CodeableConcept transformTypeOfSampleToCodeableConcept(TypeOfSample typeOfSample) {
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(new Coding(fhirConfig.getOeFhirSystem() + "/sampleType",
                typeOfSample.getLocalAbbreviation(), typeOfSample.getLocalizedName()));
        return codeableConcept;
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void transformPersistResultsEntryFhirObjects(ResultsUpdateDataSet actionDataSet)
            throws FhirLocalPersistingException {
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        for (ResultSet resultSet : actionDataSet.getNewResults()) {
            Observation observation = transformResultToObservation(resultSet.result.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }
        for (ResultSet resultSet : actionDataSet.getModifiedResults()) {
            Observation observation = this.transformResultToObservation(resultSet.result.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : actionDataSet.getModifiedAnalysis()) {
            ServiceRequest serviceRequest = this.transformToServiceRequest(analysis.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);

    }

    @Async
    @Override
    @Transactional(readOnly = true)
    public void transformPersistResultValidationFhirObjects(List<Result> deletableList,
            List<Analysis> analysisUpdateList, ArrayList<Result> resultUpdateList, List<AnalysisItem> resultItemList,
            ArrayList<Sample> sampleUpdateList, ArrayList<Note> noteUpdateList) throws FhirLocalPersistingException {
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();

        for (Result result : deletableList) {
            Observation observation = transformResultToObservation(result.getId());
            observation.setStatus(ObservationStatus.CANCELLED);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Result result : resultUpdateList) {
            Observation observation = this.transformResultToObservation(result.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : analysisUpdateList) {
            ServiceRequest serviceRequest = this.transformToServiceRequest(analysis.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }
        }

        for (Sample sample : sampleUpdateList) {
            Task task = this.transformToTask(sample.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, task);
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    private void addToOperations(FhirOperations fhirOperations, TempIdGenerator tempIdGenerator, Resource resource) {
        if (this.setTempIdIfMissing(resource, tempIdGenerator)) {
            if (fhirOperations.createResources.containsKey(resource.getIdElement().getIdPart())) {
                LogEvent.logError("", "",
                        "collision on id: " + resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
            }
            fhirOperations.createResources.put(resource.getIdElement().getIdPart(), resource);
        } else {
            if (fhirOperations.updateResources.containsKey(resource.getIdElement().getIdPart())) {
                LogEvent.logError("", "",
                        "collision on id: " + resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
            }
            fhirOperations.updateResources.put(resource.getIdElement().getIdPart(), resource);
        }
    }

    private DiagnosticReport transformResultToDiagnosticReport(String analysisId) {
        return transformResultToDiagnosticReport(analysisService.get(analysisId));
    }

    private DiagnosticReport transformResultToDiagnosticReport(Analysis analysis) {
        List<Result> allResults = resultService.getResultsByAnalysis(analysis);
        SampleItem sampleItem = analysis.getSampleItem();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());

        DiagnosticReport diagnosticReport = genNewDiagnosticReport(analysis);

        if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.FINAL);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.PRELIMINARY);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.PARTIAL);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.REGISTERED);
        } else {
            diagnosticReport.setStatus(DiagnosticReportStatus.UNKNOWN);
        }

        diagnosticReport
                .addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        diagnosticReport.addSpecimen(this.createReferenceFor(ResourceType.Specimen, sampleItem.getFhirUuidAsString()));
        diagnosticReport.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        for (Result curResult : allResults) {
            diagnosticReport
                    .addResult(this.createReferenceFor(ResourceType.Observation, curResult.getFhirUuidAsString()));
        }

        return diagnosticReport;
    }

    private DiagnosticReport genNewDiagnosticReport(Analysis analysis) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.addIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/analysisResult_uuid",
                analysis.getFhirUuidAsString()));
        return diagnosticReport;
    }

    private Observation transformResultToObservation(String resultId) {
        return transformResultToObservation(resultService.get(resultId));
    }

    private Observation transformResultToObservation(Result result) {
        Analysis analysis = result.getAnalysis();
        SampleItem sampleItem = analysis.getSampleItem();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());
        Observation observation = new Observation();

        observation.setId(result.getFhirUuidAsString());
        observation.addIdentifier(
                this.createIdentifier(fhirConfig.getOeFhirSystem() + "/result_uuid", result.getFhirUuidAsString()));

        // TODO make sure these align with each other.
        // we may need to add detection for when result is changed and add those status
        // to list
        if (result.getAnalysis().getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            observation.setStatus(ObservationStatus.FINAL);
        } else if (result.getAnalysis().getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            LogEvent.logError(this.getClass().getName(), "transformResultToObservation",
                    "recording result for analysis that is not started.");
            observation.setStatus(ObservationStatus.UNKNOWN);
        } else {
            observation.setStatus(ObservationStatus.PRELIMINARY);
        }

        if (!GenericValidator.isBlankOrNull(result.getValue())) {
            if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())
                    && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
                observation.setValue(new CodeableConcept(
                        new Coding(fhirConfig.getOeFhirSystem() + "/dictionary_entry", dictionary.getDictEntry(),
                                dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                        : dictionary.getLocalizedDictionaryName().getEnglish())));
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())
                    && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
                observation.setValue(new CodeableConcept(
                        new Coding(fhirConfig.getOeFhirSystem() + "/dictionary_entry", dictionary.getDictEntry(),
                                dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                        : dictionary.getLocalizedDictionaryName().getEnglish())));
            } else if (TypeOfTestResultServiceImpl.ResultType.isNumeric(result.getResultType())) {
                Quantity quantity = new Quantity();
                quantity.setValue(new BigDecimal(result.getValue()));
                quantity.setUnit(resultService.getUOM(result));
                observation.setValue(quantity);
            } else if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(result.getResultType())) {
                observation.setValue(new StringType(result.getValue()));
            }
        }

        observation.addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        observation.setSpecimen(this.createReferenceFor(ResourceType.Specimen, sampleItem.getFhirUuidAsString()));
        observation.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        return observation;
    }

    @Override
    public Practitioner transformNameToPractitioner(String practitionerName) {
        Practitioner practitioner = new Practitioner();
        String[] names = practitionerName.split(" ", 2);
        HumanName name = practitioner.addName();
        if (names.length >= 1) {
            name.addGiven(names[0]);
        }
        if (names.length >= 2) {
            name.setFamily(names[1]);
        }
        return practitioner;
    }

    @Override
    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization) {
        org.hl7.fhir.r4.model.Organization fhirOrganization = new org.hl7.fhir.r4.model.Organization();
        fhirOrganization
                .setId(organization.getFhirUuid() == null ? organization.getId() : organization.getFhirUuidAsString());
        fhirOrganization.setName(organization.getOrganizationName());
        this.setFhirOrganizationIdentifiers(fhirOrganization, organization);
        this.setFhirAddressInfo(fhirOrganization, organization);
        this.setFhirOrganizationTypes(fhirOrganization, organization);
        return fhirOrganization;
    }

    @Override
    @Transactional(readOnly = true)
    public Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization) {
        Organization organization = new Organization();
        organization.setOrganizationName(fhirOrganization.getName());
        organization.setIsActive(IActionConstants.YES);

        setOeOrganizationIdentifiers(organization, fhirOrganization);
        setOeOrganizationAddressInfo(organization, fhirOrganization);
        setOeOrganizationTypes(organization, fhirOrganization);

        organization.setMlsLabFlag(IActionConstants.NO);
        organization.setMlsSentinelLabFlag(IActionConstants.NO);

        return organization;
    }

    private void setOeOrganizationIdentifiers(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        organization.setFhirUuid(UUID.fromString(fhirOrganization.getIdElement().getIdPart()));
        for (Identifier identifier : fhirOrganization.getIdentifier()) {
            if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_cliaNum")) {
                organization.setCliaNum(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_shortName")) {
                organization.setShortName(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_code")) {
                organization.setCode(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_uuid")) {
                organization.setFhirUuid(UUID.fromString(identifier.getValue()));
            }
        }
    }

    private void setFhirOrganizationIdentifiers(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        if (!GenericValidator.isBlankOrNull(organization.getCliaNum())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_cliaNum")
                    .setValue(organization.getCliaNum()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getShortName())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_shortName")
                    .setValue(organization.getShortName()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_code")
                    .setValue(organization.getCode()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_uuid")
                    .setValue(organization.getFhirUuidAsString()));
        }
    }

    private void setOeOrganizationTypes(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        Set<OrganizationType> orgTypes = new HashSet<>();
        OrganizationType orgType = null;
        for (CodeableConcept type : fhirOrganization.getType()) {
            for (Coding coding : type.getCoding()) {
                if (coding.getSystem() != null
                        && coding.getSystem().equals(fhirConfig.getOeFhirSystem() + "/orgType")) {
                    orgType = new OrganizationType();
                    orgType.setName(coding.getCode());
                    orgType.setDescription(type.getText());
                    orgType.setNameKey("org_type." + coding.getCode() + ".name");
                    orgType.setOrganizations(new HashSet<>());
                    orgType.getOrganizations().add(organization);
                    orgTypes.add(orgType);
                }
            }
        }
        organization.setOrganizationTypes(orgTypes);
    }

    private void setFhirOrganizationTypes(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        Set<OrganizationType> orgTypes = organization.getOrganizationTypes();
        for (OrganizationType orgType : orgTypes) {
            fhirOrganization.addType(new CodeableConcept() //
                    .setText(orgType.getDescription()) //
                    .addCoding(new Coding() //
                            .setSystem(fhirConfig.getOeFhirSystem() + "/orgType") //
                            .setCode(orgType.getName())));
        }
    }

    private void setOeOrganizationAddressInfo(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        organization.setStreetAddress(fhirOrganization.getAddressFirstRep().getLine().stream()
                .map(e -> e.asStringValue()).collect(Collectors.joining("\\n")));
        organization.setCity(fhirOrganization.getAddressFirstRep().getCity());
        organization.setState(fhirOrganization.getAddressFirstRep().getState());
        organization.setZipCode(fhirOrganization.getAddressFirstRep().getPostalCode());
    }

    private void setFhirAddressInfo(org.hl7.fhir.r4.model.Organization fhirOrganization, Organization organization) {
        if (!GenericValidator.isBlankOrNull(organization.getStreetAddress())) {
            fhirOrganization.getAddressFirstRep().addLine(organization.getStreetAddress());
        }
        if (!GenericValidator.isBlankOrNull(organization.getCity())) {
            fhirOrganization.getAddressFirstRep().setCity(organization.getCity());
        }
        if (!GenericValidator.isBlankOrNull(organization.getState())) {
            fhirOrganization.getAddressFirstRep().setState(organization.getState());
        }
        if (!GenericValidator.isBlankOrNull(organization.getZipCode())) {
            fhirOrganization.getAddressFirstRep().setPostalCode(organization.getZipCode());
        }
    }

    private Annotation transformNoteToAnnotation(Note note) {
        Annotation annotation = new Annotation();
        annotation.setText(note.getText());
        return annotation;
    }

    @Override
    public boolean setTempIdIfMissing(Resource resource, TempIdGenerator tempIdGenerator) {
        if (GenericValidator.isBlankOrNull(resource.getId())) {
            resource.setId(tempIdGenerator.getNextId());
            return true;
        }
        return false;
    }

    @Override
    public Reference createReferenceFor(Resource resource) {
        if (resource == null) {
            return null;
        }
        Reference reference = new Reference(resource);
        reference.setReference(resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
        return reference;
    }

    @Override
    public Reference createReferenceFor(ResourceType resourceType, String id) {
        Reference reference = new Reference();
        reference.setReference(resourceType + "/" + id);
        return reference;
    }

    @Override
    public String getIdFromLocation(String location) {
        String id = location.substring(location.indexOf("/") + 1);
        while (id.lastIndexOf("/") > 0) {
            id = id.substring(0, id.lastIndexOf("/"));
        }
        return id;
    }

    @Override
    public Identifier createIdentifier(String system, String value) {
        Identifier identifier = new Identifier();
        identifier.setSystem(system);
        identifier.setValue(value);
        return identifier;
    }

    private class FhirOrderEntryObjects {
        @SuppressWarnings("unused")
        public org.hl7.fhir.r4.model.Patient patient;
        public Practitioner requester;
        List<FhirSampleEntryObjects> sampleEntryObjectsList = new ArrayList<>();
    }

    private class FhirSampleEntryObjects {
        public Practitioner collector;
        public Specimen specimen;
        public List<ServiceRequest> serviceRequests = new ArrayList<>();
    }
    
    @Override
    public List<ETLRecord> getLatestFhirforETL(List<Observation> observations) {
        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", "getLatestFhirforETL ");

        List<ETLRecord> etlRecordList = new ArrayList<>();

        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        org.hl7.fhir.r4.model.Observation fhirObservation = new org.hl7.fhir.r4.model.Observation();
        org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest = new org.hl7.fhir.r4.model.ServiceRequest();
        org.hl7.fhir.r4.model.Specimen fhirSpecimen = new org.hl7.fhir.r4.model.Specimen();
        IGenericClient localFhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
        org.json.simple.JSONObject code = null;
        org.json.simple.JSONArray coding = null;
        org.json.simple.JSONObject jCoding = null;
        
        // gnr

        for (Observation observation : observations) {
            fhirObservation = (Observation) observation;
            System.out.println("observation: " +  fhirContext.newJsonParser().encodeResourceToString(fhirObservation));
            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL",   fhirObservation.getBasedOnFirstRep().getReference().toString());
            String srString = fhirObservation.getBasedOnFirstRep().getReference().toString();
            String srUuidString = srString.substring(srString.lastIndexOf("/") + 1);

            //sr, prac
            Bundle srBundle = (Bundle) localFhirClient.search().forResource(ServiceRequest.class)
                    .where(new TokenClientParam("_id").exactly().code(srUuidString))
                    .prettyPrint()
                    .execute();

            if (srBundle.hasEntry()) {
                fhirServiceRequest = (ServiceRequest) srBundle.getEntryFirstRep().getResource();
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL",   fhirContext.newJsonParser().encodeResourceToString(fhirServiceRequest));
                //                    get Practitioner
            }

            //pat
//            fhirPatient = fhirPersistanceService.getPatientByUuid(fhirObservation.getSubject().getIdentifier().getValue()).orElseThrow();
            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL",   fhirObservation.getSubject().getReference().toString());
            String patString = fhirObservation.getSubject().getReference().toString();
            String patUuidString = patString.substring(patString.lastIndexOf("/") + 1);

            Bundle patBundle = (Bundle) localFhirClient.search().forResource(org.hl7.fhir.r4.model.Patient.class)
                    .where(new TokenClientParam("_id").exactly().code(patUuidString))
                    .prettyPrint()
                    .execute();

            if (patBundle.hasEntry()) {
                fhirPatient = (org.hl7.fhir.r4.model.Patient) patBundle.getEntryFirstRep().getResource();
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL",   fhirContext.newJsonParser().encodeResourceToString(fhirPatient));
            }
            
            //sp
            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL",   fhirObservation.getSpecimen().getReference().toString());
            String spString = fhirObservation.getSpecimen().getReference().toString();
            String spUuidString = spString.substring(spString.lastIndexOf("/") + 1);

            Bundle spBundle = (Bundle) localFhirClient.search().forResource(Specimen.class)
                    .where(new TokenClientParam("_id").exactly().code(spUuidString))
                    .prettyPrint()
                    .execute();

            if (spBundle.hasEntry()) {
                fhirSpecimen = (Specimen) spBundle.getEntryFirstRep().getResource();
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL",   fhirContext.newJsonParser().encodeResourceToString(fhirSpecimen));
            }

            List<String> practitionerList = LoadPractitioners();

            JSONObject jResultUUID = null;
            JSONObject jSRRef = null;
            JSONObject reqRef = null;
           
            int i, j = 0;

            ETLRecord etlRecord = new ETLRecord();
            try {
                String observationStr = fhirContext.newJsonParser().encodeResourceToString(fhirObservation);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", observationStr);
                JSONObject observationJson = null;
                observationJson = JSONUtils.getAsObject(observationStr);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", observationJson.toString());
                if (!JSONUtils.isEmpty(observationJson)) {

                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(observationJson.get("identifier"));
                    for (j = 0; j < identifier.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", identifier.get(j).toString());
                        jResultUUID = JSONUtils.getAsObject(identifier.get(j));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jResultUUID.get("system").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jResultUUID.get("value").toString());
                    }
                    try {
                        code = JSONUtils.getAsObject(observationJson.get("valueCodeableConcept"));
                        coding = JSONUtils.getAsArray(code.get("coding"));
                        for (j = 0; j < coding.size(); ++j) {
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", coding.get(0).toString());
                            jCoding = JSONUtils.getAsObject(coding.get(0));
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("system").toString());
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("code").toString());
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("display").toString());
                        }
                    } catch(Exception e) { 
                        e.printStackTrace();
                    }
                    etlRecord.setResult(jCoding.get("display").toString());
                    LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", observationJson.get("subject").toString());

                    JSONObject subjectRef = null;
                    subjectRef = JSONUtils.getAsObject(observationJson.get("subject"));
                    LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", subjectRef.get("reference").toString());

                    JSONObject specimenRef = null;
                    specimenRef = JSONUtils.getAsObject(observationJson.get("specimen"));
                    LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", specimenRef.get("reference").toString());

                    org.json.simple.JSONArray serviceRequestRef = null;
                    serviceRequestRef = JSONUtils.getAsArray(observationJson.get("basedOn"));
                    for (j = 0; j < serviceRequestRef.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", serviceRequestRef.get(j).toString());
                        jSRRef = JSONUtils.getAsObject(serviceRequestRef.get(j));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jSRRef.get("reference").toString());
                    }
                    etlRecord.setOrder_status(observationJson.get("status").toString());
                    etlRecord.setData(observationStr);
                }

                String patientStr = fhirContext.newJsonParser().encodeResourceToString(fhirPatient);
                System.out.println( "patientStr: " + patientStr);
                JSONObject patientJson = null;
                patientJson = JSONUtils.getAsObject(patientStr);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", patientJson.toString());
                if (!JSONUtils.isEmpty(patientJson)) {

                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(patientJson.get("identifier"));
                    for (j = 0; j < identifier.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", identifier.get(j).toString());
                        JSONObject patIds = JSONUtils.getAsObject(identifier.get(j));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", patIds.get("system").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", patIds.get("value").toString());
                        if (patIds.get("system").toString()
                                .equalsIgnoreCase("http://openelis-global.org/pat_nationalId")) {
                            etlRecord.setIdentifier(patIds.get("value").toString());
                        }
                        etlRecord.setSex(patientJson.get("gender").toString());
                        //       1994
                        try {
                            //                            String timestampToDate = patientJson.get("birthDate").toString().substring(0,10);
                            String timestampToDate = patientJson.get("birthDate").toString().substring(0,4);
                            timestampToDate = timestampToDate +"-01-01";
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date parsedDate = dateFormat.parse(timestampToDate);
                            Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                            etlRecord.setBirthdate(timestamp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    org.json.simple.JSONArray name = JSONUtils.getAsArray(patientJson.get("name"));
                    for (j = 0; j < name.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", name.get(j).toString());
                        JSONObject jName = JSONUtils.getAsObject(name.get(j));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jName.get("family").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jName.get("given").toString());
                        etlRecord.setLast_name(jName.get("family").toString());
                        org.json.simple.JSONArray givenName = JSONUtils.getAsArray(jName.get("given"));
                        etlRecord.setFirst_name(givenName.get(0).toString());
                    }

                    org.json.simple.JSONArray address = JSONUtils.getAsArray(patientJson.get("address"));
                    for (j = 0; j < name.size(); ++j) {
                        JSONObject jAddress = JSONUtils.getAsObject(address.get(j));
                        org.json.simple.JSONArray jLines = JSONUtils.getAsArray(jAddress.get("line"));
                        etlRecord.setAddress_street(jLines.get(0).toString());
                        etlRecord.setAddress_city(jAddress.get("city").toString());
                        etlRecord.setAddress_country(jAddress.get("country").toString());
                    }

                    org.json.simple.JSONArray telecom = JSONUtils.getAsArray(patientJson.get("telecom"));
                    for (j = 0; j < telecom.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", telecom.get(j).toString());
                        JSONObject jTelecom = JSONUtils.getAsObject(telecom.get(j));

                        if (jTelecom.get("system").toString().equalsIgnoreCase("other")) {
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jTelecom.get("system").toString());
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jTelecom.get("value").toString());
                            etlRecord.setHome_phone(jTelecom.get("value").toString());
                        } else if (jTelecom.get("system").toString().equalsIgnoreCase("sms")) {
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jTelecom.get("system").toString());
                            LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jTelecom.get("value").toString());
                            etlRecord.setWork_phone(jTelecom.get("value").toString());
                        }
                    }
                    etlRecord.setPatientId(fhirPatient.getId());
                }

                String serviceRequestStr = fhirContext.newJsonParser().encodeResourceToString(fhirServiceRequest);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", serviceRequestStr);
                JSONObject srJson = null;
                srJson = JSONUtils.getAsObject(serviceRequestStr);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", srJson.toString());

                if (!JSONUtils.isEmpty(srJson)) {

                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(srJson.get("identifier"));
                    for (j = 0; j < identifier.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", identifier.get(j).toString());
                        JSONObject srIds = JSONUtils.getAsObject(identifier.get(j));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", srIds.get("system").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", srIds.get("value").toString());
                    }

                    reqRef = JSONUtils.getAsObject(srJson.get("requisition"));
                    LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", reqRef.get("system").toString());
                    LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", reqRef.get("value").toString());
                    etlRecord.setLabno(reqRef.get("value").toString());

                    code = JSONUtils.getAsObject(srJson.get("code"));
                    coding = JSONUtils.getAsArray(code.get("coding"));
                    
                    org.json.simple.JSONArray jCategoryArray = JSONUtils.getAsArray(srJson.get("category"));
                    LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCategoryArray.get(0).toString());
                    JSONObject jCatJson = (JSONObject) jCategoryArray.get(0);
                    coding = JSONUtils.getAsArray(jCatJson.get("coding"));
                    for (j = 0; j < coding.size(); ++j) {
                        jCoding = (JSONObject) coding.get(j);
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("system").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("code").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("display").toString());
                    }
                    etlRecord.setProgram(jCoding.get("display").toString());

//                    reqRef = JSONUtils.getAsObject(srJson.get("locationReference"));
//                    System.out.println("srReq:" + reqRef.get("system").toString());
//                    System.out.println("srReq:" + reqRef.get("value").toString());
//                    etlRecord.setCode_referer(reqRef.get("value").toString());
                    
//                    requester is practitioner
//                    reqRef = JSONUtils.getAsObject(srJson.get("requester"));
//                    System.out.println("srReq:" + reqRef.get("reference").toString());
//                    etlRecord.setReferer(reqRef.get("reference").toString());

                    code = JSONUtils.getAsObject(srJson.get("code"));
                    coding = JSONUtils.getAsArray(code.get("coding"));
                    for (j = 0; j < coding.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", coding.get(0).toString());
                        jCoding = JSONUtils.getAsObject(coding.get(0));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("system").toString());
//                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("code").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("display").toString());
                    }
                    
                    etlRecord.setTest(jCoding.get("display").toString()); //test description

                    //                    2021-05-06T12:51:58-07:00
                    try {
                        String timestampToDate = srJson.get("authoredOn").toString().substring(0,10);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date parsedDate = dateFormat.parse(timestampToDate);
                        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                        etlRecord.setDate_entered(timestamp);
                    } catch(Exception e) { 
                        e.printStackTrace();
                    }
                    
                    // done here because data_entered is used for age
                    LocalDate birthdate = LocalDate.parse(etlRecord.getBirthdate().toString().substring(0,10));
                    LocalDate date_entered = LocalDate.parse(etlRecord.getDate_entered().toString().substring(0,10));
                    if ((etlRecord.getBirthdate() != null) && (etlRecord.getDate_entered() != null)) {
                        int age_days = Period.between(birthdate, date_entered).getDays();
                        int age_years = Period.between(birthdate, date_entered).getYears();
                        int age_months = Period.between(birthdate, date_entered).getMonths();
                        int age_weeks = Math.round(age_days)/7;

                        if (age_days > 3) etlRecord.setAge_weeks(age_weeks + 1);
                        if (age_weeks > 2) etlRecord.setAge_months(age_months + 1);
                        etlRecord.setAge_years((age_months > 5) ? age_years + 1 : age_years);
                        etlRecord.setAge_months((12*age_years) + age_months); 
                        etlRecord.setAge_weeks((52*age_years) + (4*age_months) + age_weeks); 
                    }
                }

                String specimenStr = fhirContext.newJsonParser().encodeResourceToString(fhirSpecimen);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", specimenStr);
                JSONObject specimenJson = null;
                specimenJson = JSONUtils.getAsObject(specimenStr);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", specimenJson.toString());
                if (!JSONUtils.isEmpty(specimenJson)) {

                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(specimenJson.get("identifier"));
                    for (j = 0; j < identifier.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", identifier.get(j).toString());
                        JSONObject specimenId = JSONUtils.getAsObject(identifier.get(j));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", specimenId.get("system").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", specimenId.get("value").toString());
                    }

                    code = JSONUtils.getAsObject(specimenJson.get("type"));
                    coding = JSONUtils.getAsArray(code.get("coding"));
                    for (j = 0; j < coding.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", coding.get(0).toString());
                        jCoding = JSONUtils.getAsObject(coding.get(0));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("system").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("code").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jCoding.get("display").toString());
                    }
                    //                  2021-04-29T16:58:51-07:00
                    try {
                        String timestampToDate = specimenJson.get("receivedTime").toString().substring(0,10);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date parsedDate = dateFormat.parse(timestampToDate);
                        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                        etlRecord.setDate_recpt(timestamp);
                    } catch(Exception e) { 
                        e.printStackTrace();
                    }
                }

                String practitionerStr = practitionerList.get(0);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", practitionerStr);
                JSONObject practitionerJson = null;
                practitionerJson = JSONUtils.getAsObject(practitionerStr);
                LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", practitionerJson.toString());
                if (!JSONUtils.isEmpty(practitionerJson)) {
                    org.json.simple.JSONArray name = JSONUtils.getAsArray(practitionerJson.get("name"));
                    for (j = 0; j < name.size(); ++j) {
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", name.get(j).toString());
                        JSONObject jName = JSONUtils.getAsObject(name.get(j));
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jName.get("family").toString());
                        LogEvent.logDebug(this.getClass().getName(), "getLatestFhirforETL", jName.get("given").toString());
                        org.json.simple.JSONArray givenName = JSONUtils.getAsArray(jName.get("given"));
                        etlRecord.setReferer(givenName.get(0).toString() + " " + 
                                jName.get("family").toString());
                    }
                }
            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
            
            etlRecordList.add(etlRecord);
        }
        return etlRecordList;
    }
    
    @Override
    public List<String> LoadPractitioners() {
        List<String> list = new ArrayList<String>();
        String practitionerStr = new String(
                "{" 
        +"      \"resourceType\": \"Practitioner\"," 
        +"      \"id\": \"1bc2ad64-a16e-45b9-b2ad-169124893ef1\"," 
        +"      \"meta\": {" 
        +"        \"versionId\": \"1\"," 
        +"        \"lastUpdated\": \"2021-05-20T18:28:56.010-07:00\"," 
        +"        \"source\": \"#B5UHocnLZEvaLUgl\"" 
        +"      }," 
        +"      \"name\": [ {" 
        +"        \"family\": \"\"," 
        +"        \"given\": [ \"\" ]" 
        +"      } ]," 
        +"      \"telecom\": [ {" 
        +"        \"system\": \"phone\"" 
        +"      }, {" 
        +"        \"system\": \"email\"" 
        +"      }, {" 
        +"        \"system\": \"fax\"" 
        +"      } ]" 
        +"    }" 
        );
        list.add(practitionerStr);
        list.add(practitionerStr);
        return list;
    }

    @Override
    public List<String> LoadPatients() {
        List<String> list = new ArrayList<String>();
        String patientStr = new String(
        "{"
        +"  \"resourceType\": \"Patient\","
        +"  \"id\": \"329f09da-0fc9-419d-9575-ace689544269\","
        +"  \"meta\": {"
        +"  \"versionId\": \"2\","
        +"  \"lastUpdated\": \"2021-05-27T15:55:52.463-07:00\","
        +"  \"source\": \"#4CaEBBCXbRZaKmtI\""
        +"  },"
        +"  \"text\": {"
        +"  \"status\": \"generated\","
        +"  },"
        +"  \"identifier\": [ {"
        +"  \"system\": \"http://openelis-global.org/pat_subjectNumber\","
        +"  \"value\": \"121212\""
        +"}, {"
        +"  \"system\": \"http://openelis-global.org/pat_nationalId\","
        +"  \"value\": \"121212\""
        +"}, {"
        +"  \"system\": \"http://openelis-global.org/pat_guid\","
        +"    \"value\": \"329f09da-0fc9-419d-9575-ace689544269\""
        +"  }, {"
        +"    \"system\": \"http://openelis-global.org/pat_uuid\","
        +"    \"value\": \"329f09da-0fc9-419d-9575-ace689544269\""
        +"  } ],"
        +"  \"name\": [ {"
        +"    \"family\": \"cray\","
        +"    \"given\": [ \"crày\" ]"
        +"  } ],"
        
        +"  \"address\": [ {"
        +"    \"line\": \"123 Anystreet\","
        +"    \"city\": \"Seattle\","
        +"    \"country\": \"United States\""
        +"  } ],"

        
        +"  \"telecom\": [ {"
        +"    \"system\": \"home phone\""
        +"        \"value\": \"513 555 1212\"" 
        +"  }, {"
        +"    \"system\": \"email\""
        +"  }, {"
        +"    \"system\": \"work phone\""
        +"        \"value\": \"604 555 1212\"" 
        +"  } ],"
        
        +"  \"gender\": \"male\","
        +"  \"birthDate\": \"1994-05-16\""
        +"}"
);
        list.add(patientStr);
        patientStr = 
        "{" 
        +"      \"resourceType\": \"Patient\"," 
        +"      \"id\": \"28697d12-a750-45b1-8283-158e8f3b2f6f\"," 
        +"      \"meta\": {" 
        +"        \"versionId\": \"1\"," 
        +"        \"lastUpdated\": \"2021-05-11T14:04:15.022-07:00\"," 
        +"        \"source\": \"#V5Ie3QEBCYprWGyP\"" 
        +"      }," 
        +"      \"text\": {" 
        +"        \"status\": \"generated\"," 
        +"      }," 
        +"      \"identifier\": [ {" 
        +"        \"system\": \"http://openelis-global.org/pat_nationalId\"," 
        +"        \"value\": \"CA95678\"" 
        +"      }, {" 
        +"        \"system\": \"http://openelis-global.org/pat_guid\"," 
        +"        \"value\": \"28697d12-a750-45b1-8283-158e8f3b2f6f\"" 
        +"      }, {" 
        +"        \"system\": \"http://openelis-global.org/pat_uuid\"," 
        +"        \"value\": \"28697d12-a750-45b1-8283-158e8f3b2f6f\"" 
        +"      } ]," 
        +"      \"name\": [ {" 
        +"        \"family\": \"Erickson\"," 
        +"        \"given\": [ \"Sharlene\" ]" 
        +"      } ]," 
        
        +"  \"address\": [ {"
        +"    \"line\": \"321 Anystreet\","
        +"    \"city\": \"Seattle\","
        +"    \"country\": \"United States\""
        +"  } ],"
        
        +"  \"telecom\": [ {"
        +"    \"system\": \"home phone\""
        +"        \"value\": \"513 555 1212\"" 
        +"  }, {"
        +"    \"system\": \"email\""
        +"  }, {"
        +"    \"system\": \"work phone\""
        +"        \"value\": \"604 555 1212\"" 
        +"  } ],"
        
        +"      \"gender\": \"female\"," 
        +"      \"birthDate\": \"1974-08-25\"" 
        +"    }";
        list.add(patientStr);
        return list;
    }
    
    @Override
    public List<String> LoadServiceRequests() {
        List<String> list = new ArrayList<String>();
        String serviceRequestStr = new String(
        "{" 
        +"  \"resourceType\": \"ServiceRequest\"," 
        +"  \"id\": \"c8ba6917-5810-49cf-86b4-a642db903532\"," 
        +"  \"meta\": {" 
        +"    \"versionId\": \"2\"," 
        +"    \"lastUpdated\": \"2021-05-06T12:51:58.661-07:00\"," 
        +"    \"source\": \"#cyqENaoLKk9WbBMv\"" 
        +"  }," 
        +"  \"identifier\": [ {" 
        +"    \"system\": \"http://openelis-global.org/analysis_uuid\"," 
        +"    \"value\": \"c8ba6917-5810-49cf-86b4-a642db903532\"" 
        +"  } ]," 
        
        +"  \"locationReference\": {" 
        +"  \"system\": \"http://openelis-global.org/locationReference\"," 
        +"  \"value\": \"Lab01\"" 
        +"  }," 
        
        
        +"  \"requisition\": {" 
        +"    \"system\": \"http://openelis-global.org/samp_labNo\"," 
        +"    \"value\": \"20210000000000051\"" 
        +"  }," 
        +"  \"status\": \"active\"," 
        +"  \"intent\": \"original-order\"," 
        
        +"  \"category\": {" 
        +"    \"coding\": [ {" 
        +"      \"system\": \"http://openelis-global.org/sample_program\"," 
        +"      \"code\": \"1400\"," 
        +"      \"display\": \"1400\"" 
        +"    } ]" 
        +"  }," 

        +"  \"priority\": \"routine\"," 
        +"  \"code\": {" 
        +"    \"coding\": [ {" 
        +"      \"system\": \"http://loinc.org\"," 
        +"      \"code\": \"94500-6\"," 
        +"      \"display\": \"COVID-19 PCR\"" 
        +"    } ]" 
        +"  }," 
        +"  \"subject\": {" 
        +"    \"reference\": \"Patient/329f09da-0fc9-419d-9575-ace689544269\"" 
        +"  }," 
        +"  \"authoredOn\": \"2021-05-06T12:51:58-07:00\"," 
        +"  \"requester\": {" 
        +"    \"reference\": \"Practitioner/1bc2ad64-a16e-45b9-b2ad-169124893ef1\"" 
        +"  }," 
        +"  \"specimen\": [ {" 
        +"    \"reference\": \"Specimen/313513fd-f64c-4ee9-9142-910880f31767\"" 
        +"  } ]" 
        +" }" 
        );
        
        list.add(serviceRequestStr);
        serviceRequestStr = 
        "{" 
        +"      \"resourceType\": \"ServiceRequest\"," 
        +"      \"id\": \"0b5e317e-fda0-4b21-b4bd-7f1db48328e5\"," 
        +"      \"meta\": {" 
        +"        \"versionId\": \"2\"," 
        +"        \"lastUpdated\": \"2021-05-20T18:29:41.580-07:00\"," 
        +"        \"source\": \"#ItyHOGLWHwxGDnyN\"" 
        +"      }," 
        +"      \"identifier\": [ {" 
        +"        \"system\": \"http://openelis-global.org/analysis_uuid\"," 
        +"        \"value\": \"0b5e317e-fda0-4b21-b4bd-7f1db48328e5\"" 
        +"      } ]," 
        
        +"      \"locationReference\": {" 
        +"      \"system\": \"http://openelis-global.org/locationReference\"," 
        +"      \"value\": \"Lab01\"" 
        +"      }," 
        
        +"      \"requisition\": {" 
        +"        \"system\": \"http://openelis-global.org/samp_labNo\"," 
        +"        \"value\": \"20210000000000159\"" 
        +"      }," 
        +"      \"status\": \"active\"," 
        +"      \"intent\": \"original-order\","
        
        +"      \"category\": {" 
        +"        \"coding\": [ {" 
        +"          \"system\": \"http://openelis-global.org/sample_program\"," 
        +"          \"code\": \"1400\"," 
        +"          \"display\": \"1400\"" 
        +"        } ]" 
        +"      },"
        
        +"      \"priority\": \"routine\"," 
        +"      \"code\": {" 
        +"        \"coding\": [ {" 
        +"          \"system\": \"http://loinc.org\"," 
        +"          \"code\": \"94500-6\"," 
        +"          \"display\": \"COVID-19 PCR\"" 
        +"        } ]" 
        +"      }," 
        +"      \"subject\": {" 
        +"        \"reference\": \"Patient/5dc3d795-1d16-47c7-9708-ac6b8416d670\"" 
        +"      }," 
        +"      \"authoredOn\": \"2021-05-20T18:29:41-07:00\"," 
        +"      \"requester\": {" 
        +"        \"reference\": \"Practitioner/1bc2ad64-a16e-45b9-b2ad-169124893ef1\"" 
        +"      }," 
        +"      \"specimen\": [ {" 
        +"        \"reference\": \"Specimen/d00235b4-8401-4b0b-ac0d-94dd67be799b\"" 
        +"      } ]"
        +"}";
        list.add(serviceRequestStr);
        return list;
    }
    
    @Override
    public List<String> LoadSpecimens() {
        List<String> list = new ArrayList<String>();
        String specimenStr = new String(
                "{" 
                        +"  \"resourceType\": \"Specimen\"," 
                        +"  \"id\": \"313513fd-f64c-4ee9-9142-910880f31767\"," 
                        +"  \"meta\": {" 
                        +"    \"versionId\": \"1\"," 
                        +"    \"lastUpdated\": \"2021-05-06T12:16:44.243-07:00\"," 
                        +"    \"source\": \"#SYYftPeogJs3qLf2\"" 
                        +"  }," 
                        +"  \"identifier\": [ {" 
                        +"    \"system\": \"http://openelis-global.org/sampleItem_uuid\"," 
                        +"    \"value\": \"313513fd-f64c-4ee9-9142-910880f31767\"" 
                        +"  } ]," 
                        +"  \"accessionIdentifier\": {" 
                        +"    \"system\": \"http://openelis-global.org/sampleItem_labNo\"," 
                        +"    \"value\": \"20210000000000051-1\"" 
                        +"  }," 
                        +"  \"status\": \"available\"," 
                        +"  \"type\": {" 
                        +"    \"coding\": [ {" 
                        +"      \"system\": \"http://openelis-global.org/sampleType\"," 
                        +"      \"code\": \"Fluid\"," 
                        +"      \"display\": \"Fluid\"" 
                        +"    } ]" 
                        +"  }," 
                        +"  \"subject\": {" 
                        +"    \"reference\": \"Patient/329f09da-0fc9-419d-9575-ace689544269\"" 
                        +"  }," 
                        +"  \"receivedTime\": \"2021-05-06T12:16:44-07:00\"," 
                        +"  \"request\": [ {" 
                        +"    \"reference\": \"ServiceRequest/c8ba6917-5810-49cf-86b4-a642db903532\"" 
                        +"  } ]" 
                        +"}" 
                        );
        list.add(specimenStr);
        specimenStr = 
                "{"
                        +"      \"resourceType\": \"Specimen\","
                        +"      \"id\": \"3bbfcc48-b526-47e7-a4d8-8b23bef9b4a9\","
                        +"      \"meta\": {"
                        +"        \"versionId\": \"1\","
                        +"        \"lastUpdated\": \"2021-04-29T16:58:51.510-07:00\","
                        +"        \"source\": \"#lWpUeCrJ7acg2bLv\""
                        +"      },"
                        +"      \"identifier\": [ {"
                        +"        \"system\": \"http://openelis-global.org/sampleItem_uuid\","
                        +"        \"value\": \"3bbfcc48-b526-47e7-a4d8-8b23bef9b4a9\""
                        +"      } ],"
                        +"      \"accessionIdentifier\": {"
                        +"        \"system\": \"http://openelis-global.org/sampleItem_labNo\","
                        +"        \"value\": \"20210000000000033-1\""
                        +"      },"
                        +"      \"status\": \"available\","
                        +"      \"type\": {"
                        +"        \"coding\": [ {"
                        +"          \"system\": \"http://openelis-global.org/sampleType\","
                        +"          \"code\": \"Swab\","
                        +"          \"display\": \"Swab\""
                        +"        } ]"
                        +"      },"
                        +"      \"subject\": {"
                        +"        \"reference\": \"Patient/329f09da-0fc9-419d-9575-ace689544269\""
                        +"      },"
                        +"      \"receivedTime\": \"2021-04-29T16:58:51-07:00\","
                        +"      \"request\": [ {"
                        +"        \"reference\": \"ServiceRequest/b1eaae7f-4b56-4ea2-aeef-6e3d60c824b7\""
                        +"      } ]"
                        +" }";

        list.add(specimenStr);
        return list;
    }
        
        
    
    @Override
    public List<String> LoadObservations() {
        List<String> list = new ArrayList<String>();
        String observationStr = new String(
        "{" 
        +"  \"resourceType\": \"Observation\"," 
        +"  \"id\": \"770c29a9-7927-4b56-99c9-f82bd03fa498\"," 
        +"  \"meta\": {" 
        +"    \"versionId\": \"1\"," 
        +"    \"lastUpdated\": \"2021-05-06T12:51:58.661-07:00\"," 
        +"    \"source\": \"#cyqENaoLKk9WbBMv\"" 
        +"  }," 
        +"  \"identifier\": [ {" 
        +"    \"system\": \"http://openelis-global.org/result_uuid\"," 
        +"    \"value\": \"770c29a9-7927-4b56-99c9-f82bd03fa498\"" 
        +"  } ]," 
        +"  \"basedOn\": [ {" 
        +"    \"reference\": \"ServiceRequest/c8ba6917-5810-49cf-86b4-a642db903532\"" 
        +"  } ]," 
        +"  \"status\": \"preliminary\"," 
        +"  \"subject\": {" 
        +"    \"reference\": \"Patient/329f09da-0fc9-419d-9575-ace689544269\"" 
        +"  }," 
        +"  \"valueString\": \"SARS-COV-2 RNA NOT DETECTED\"," 
        +"  \"specimen\": {" 
        +"    \"reference\": \"Specimen/313513fd-f64c-4ee9-9142-910880f31767\"" 
        +"  }" 
        +"}" 
        );
        
        list.add(observationStr);
        observationStr = 
        "{" 
        +"      \"resourceType\": \"Observation\"," 
        +"     \"id\": \"7e7e0352-6bd2-41b7-8f29-2f256504ba23\"," 
        +"      \"meta\": {" 
        +"        \"versionId\": \"1\"," 
        +"        \"lastUpdated\": \"2021-05-06T12:51:34.970-07:00\"," 
        +"        \"source\": \"#KDaMEJtsyz4J9bpM\"" 
        +"      }," 
        +"      \"identifier\": [ {" 
        +"        \"system\": \"http://openelis-global.org/result_uuid\"," 
        +"        \"value\": \"7e7e0352-6bd2-41b7-8f29-2f256504ba23\"" 
        +"      } ]," 
        +"      \"basedOn\": [ {" 
        +"        \"reference\": \"ServiceRequest/469d6708-a7e7-4829-ad5e-1c8ea3ea4a3b\"" 
        +"      } ]," 
        +"      \"status\": \"final\"," 
        +"      \"subject\": {" 
        +"        \"reference\": \"Patient/c77a2ed8-3481-4e95-ae4f-2cfc56184794\"" 
        +"      }," 
        +"      \"valueString\": \"SARS-CoV-2 RNA DETECTED\"," 
        +"      \"specimen\": {" 
        +"        \"reference\": \"Specimen/906f9f8c-90cd-411e-98ed-3b04ee107c89\"" 
        +"      }" 
        +"    }" 
        ;
        list.add(observationStr);
        
        return list;
    }

}