package org.openelisglobal.dataexchange.fhir.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.TaskWorker.TaskResult;
import org.openelisglobal.dataexchange.order.action.DBOrderExistanceChecker;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;

@Service
public class FhirApiWorkFlowServiceImpl implements FhirApiWorkflowService {

    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;

    @Value("${org.openelisglobal.fhirstore.uri}")
    private String localFhirStorePath;
    @Value("${org.openelisglobal.remote.source.uri}")
    private String[] remoteStorePaths;
    @Value("${org.openelisglobal.remote.source.updateStatus:false}")
    private Optional<Boolean> remoteStoreUpdateStatus;
    @Value("${org.openelisglobal.remote.source.identifier:}#{T(java.util.Collections).emptyList()}")
    private List<String> remoteStoreIdentifier;
    @Value("${org.openelisglobal.task.useBasedOn}")
    private Boolean useBasedOn;

    @Scheduled(initialDelay = 10 * 1000, fixedRate = 2 * 60 * 1000)
    @Override
    public void pollForRemoteTasks() {
        processWorkflow(ResourceType.Task);
    }

    @Override
    @Async
    public void processWorkflow(ResourceType resourceType) {
        for (String remoteStorePath : remoteStorePaths) {
            switch (resourceType) {
            case Task:
                try {
                    beginTaskPath(remoteStorePath);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getName(), "processWorkflow",
                            "could not process Task workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getName(), "processWorkflow", e.getMessage());
                }
            default:
            }
        }
    }

    private void beginTaskPath(String remoteStorePath) {
        LogEvent.logDebug(this.getClass().getName(), "beginTaskPath", "searching for Tasks");
        if (remoteStoreIdentifier.isEmpty()) {
            return;
        }
        IGenericClient sourceFhirClient = fhirUtil.getFhirClient(remoteStorePath);
        IQuery<Bundle> searchQuery = sourceFhirClient.search()//
                .forResource(Task.class)//
                .returnBundle(Bundle.class)//
                // TODO use include
//                .include(Task.INCLUDE_PATIENT)//
//                .include(Task.INCLUDE_BASED_ON)//
                .where(Task.STATUS.exactly().code(TaskStatus.REQUESTED.toCode()));
        if (!remoteStoreIdentifier.isEmpty()) {
            searchQuery.where(Task.OWNER.hasAnyOfIds(remoteStoreIdentifier));
        }
        Bundle bundle = searchQuery.execute();

        if (bundle.hasEntry()) {
            LogEvent.logDebug(this.getClass().getName(), "beginTaskPath",
                    "received bundle with " + bundle.getEntry().size() + " entries");
        } else {
            LogEvent.logDebug(this.getClass().getName(), "beginTaskPath", "received bundle with 0 entries");
        }
        for (BundleEntryComponent bundleComponent : bundle.getEntry()) {

            if (bundleComponent.hasResource()
                    && ResourceType.Task.equals(bundleComponent.getResource().getResourceType())) {

                Task remoteTask = (Task) bundleComponent.getResource();
                try {
                    processTask(remoteTask, remoteStorePath, sourceFhirClient, bundle);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getName(), "beginTaskPath",
                            "could not process Task with identifier : " + remoteTask.getId());
                }

            }
        }
    }

    private void processTask(Task remoteTask, String remoteStorePath, IGenericClient sourceFhirClient, Bundle bundle) {
        // should contain the Patient, the ServiceRequest, and the Task
        Bundle transactionResponseBundle = saveRemoteTaskAsLocalTask(sourceFhirClient, remoteTask, bundle,
                remoteStorePath);
        Task taskBasedOnRemoteTask = getLocalTaskBasedOnTask(remoteTask, remoteStorePath);
        if (useBasedOn && taskBasedOnRemoteTask == null) {
            taskBasedOnRemoteTask = saveTaskBasedOnRemoteTask(sourceFhirClient, remoteTask, bundle, remoteStorePath);
        }

        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Map<String, List<String>> localSearchParams = new HashMap<>();
        Task localTask = getTaskWithSameIdentifier(remoteTask, remoteStorePath);
        localSearchParams.put("_id", Arrays.asList(localTask.getId()));
        Bundle localBundle = localFhirClient.search()//
                .forResource(Task.class)//
                // TODO use include
                .include(Task.INCLUDE_PATIENT)//
                .include(Task.INCLUDE_BASED_ON)//
                .include(ServiceRequest.INCLUDE_PATIENT.asRecursive())//
                .whereMap(localSearchParams)//
                .returnBundle(Bundle.class)//
                .execute();

        List<ServiceRequest> serviceRequestList = getBasedOnServiceRequestFromBundle(localBundle, localTask);
        List<Patient> patients = new ArrayList<>();
        Patient forPatient = getForPatientFromBundle(localBundle, localTask);
        if (forPatient == null) {
            patients = getForPatientFromBundle(localBundle, serviceRequestList);
        } else {
            patients.add(forPatient);
        }
        TaskResult taskResult = null;
//        if(true) {
        if (localTask.getStatus() == null || !(localTask.getStatus().equals(TaskStatus.ACCEPTED)
                || localTask.getStatus().equals(TaskStatus.COMPLETED))) {
            Boolean taskOrderAcceptedFlag = false;
            for (ServiceRequest serviceRequest : serviceRequestList) {

                Patient patient = getPatientForTaskOrServiceRequest(remoteTask, serviceRequest, patients);
                if (patient == null) {
                    throw new IllegalStateException("could not find a patient for task or service request");
                }
                TaskWorker worker = new TaskWorker(remoteTask,
                        fhirContext.newJsonParser().encodeResourceToString(remoteTask), serviceRequest, patient);

                worker.setInterpreter(SpringContext.getBean(TaskInterpreter.class));
                worker.setExistanceChecker(SpringContext.getBean(DBOrderExistanceChecker.class));
                worker.setPersister(SpringContext.getBean(IOrderPersister.class));

                taskResult = worker.handleOrderRequest();
                if (taskResult == TaskResult.OK) {
                    taskOrderAcceptedFlag = true; // at least one order was accepted per Piotr 5/14/2020
                }
            }

            TaskStatus taskStatus = taskOrderAcceptedFlag ? TaskStatus.ACCEPTED : TaskStatus.REJECTED;
            localTask.setStatus(taskStatus);
            if (remoteStoreUpdateStatus.isPresent() && remoteStoreUpdateStatus.get()) {
                LogEvent.logDebug(this.getClass().getName(), "beginTaskPath",
                        "updating remote status to " + taskStatus);
                remoteTask.setStatus(taskStatus);
                sourceFhirClient.update().resource(remoteTask).execute();
            }
            localFhirClient.update().resource(localTask).execute();
            if (useBasedOn) {
                taskBasedOnRemoteTask.setStatus(taskStatus);
                localFhirClient.update().resource(taskBasedOnRemoteTask).execute();
            }
        }
    }

    private Patient getPatientForTaskOrServiceRequest(Task task, ServiceRequest serviceRequest,
            List<Patient> patients) {
        for (Patient patient : patients) {
            if (taskForPatient(task, patient)) {
                return patient;
            } else if (serviceRequestForPatient(serviceRequest, patient)) {
                return patient;
            }
        }
        return null;
    }

    private boolean taskForPatient(Task task, Patient patient) {
        if (task.getFor() != null && task.getFor().getReference() != null) {
            return task.getFor().getReference()
                    .equals(ResourceType.Patient.toString() + "/" + patient.getIdElement().getIdPart());
        }
        return false;
    }

    private boolean serviceRequestForPatient(ServiceRequest serviceRequest, Patient patient) {
        if (serviceRequest.getSubject() != null && serviceRequest.getSubject().getReference() != null) {
            return serviceRequest.getSubject().getReference()
                    .equals(ResourceType.Patient.toString() + "/" + patient.getIdElement().getIdPart());
        }
        return false;
    }

    private Task getLocalTaskBasedOnTask(Task remoteTask, String remoteStorePath) {
        Map<String, List<String>> localSearchParams = new HashMap<>();
        localSearchParams.put(Task.SP_BASED_ON, Arrays
                .asList(remoteStorePath + ResourceType.Task.toString() + "/" + remoteTask.getIdElement().getIdPart()));

        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search().forResource(Task.class).whereMap(localSearchParams)
                .returnBundle(Bundle.class).execute();
        return (Task) localBundle.getEntryFirstRep().getResource();
    }

    private Task saveTaskBasedOnRemoteTask(IGenericClient fhirClient, Task remoteTask, Bundle bundle,
            String remoteStorePath) {
        Task taskBasedOnRemoteTask = new Task();
        taskBasedOnRemoteTask.setId(UUID.randomUUID().toString());
        Reference reference = new Reference();
        String referenceString = remoteStorePath;
        if (!referenceString.endsWith("/")) {
            referenceString = referenceString + "/";
        }
        referenceString = referenceString + ResourceType.Task.toString() + "/" + remoteTask.getIdElement().getIdPart();
        reference.setReference(referenceString);
        taskBasedOnRemoteTask.addBasedOn(reference);

        MethodOutcome outcome = fhirUtil.getFhirClient(localFhirStorePath).update()
                .resource(taskBasedOnRemoteTask).execute();

        return (Task) outcome.getResource();
    }

    private Bundle saveRemoteTaskAsLocalTask(IGenericClient sourceFhirClient, Task remoteTask, Bundle bundle,
            String remoteStorePath) {
        List<Resource> createResources = new ArrayList<>();
        List<Resource> updateResources = new ArrayList<>();

        List<ServiceRequest> remoteServiceRequests = getBasedOnServiceRequestsFromServer(sourceFhirClient, remoteTask);
        List<Patient> remotePatients = new ArrayList<>();
        Patient remotePatientForTask = getForPatientFromServer(sourceFhirClient, remoteTask);
        if (remotePatientForTask == null) {
            remotePatients = getForPatientFromServer(sourceFhirClient, remoteServiceRequests);
        } else {
            remotePatients.add(remotePatientForTask);
        }

        Task localTask = getTaskWithSameIdentifier(remoteTask, remoteStorePath);
        if (localTask == null) {
            localTask = remoteTask.addIdentifier(createIdentifierToRemoteResource(remoteTask, remoteStorePath));
//            localTask.setStatus(TaskStatus.ACCEPTED);
            createResources.add(localTask);
        } else {
//            updateResources.add(localTask.setId(taskWithSameIdentifier.getIdElement().getValue()));
        }

        if (localTask.hasEncounter()) {
            replaceLocalReferenceWithAbsoluteReference(remoteStorePath, localTask.getEncounter());
        }
        if (localTask.hasOwner()) {
            replaceLocalReferenceWithAbsoluteReference(remoteStorePath, localTask.getOwner());
        }

        // ServiceRequests
//      List<ServiceRequest> basedOnServiceRequests = getBasedOnServiceRequestFromBundle(bundle, remoteTask);
        List<ServiceRequest> localServiceRequests = new ArrayList<>();
        ServiceRequest localBasedOn = null;
        for (ServiceRequest remoteBasedOn : remoteServiceRequests) {
            localBasedOn = getServiceRequestWithSameIdentifier(remoteBasedOn, remoteStorePath);
            if (localBasedOn == null) {
                localBasedOn = remoteBasedOn
                        .addIdentifier(createIdentifierToRemoteResource(remoteBasedOn, remoteStorePath));
                createResources.add(localBasedOn);
                localServiceRequests.add(localBasedOn);
            } else {
//                updateResources.add(basedOn.setId(serviceRequestWithSameIdentifier.getIdElement().getValue()));
            }
        }

        // Patient
//      Patient forPatient = getForPatientFromBundle(bundle, remoteTask);
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : remotePatients) {
            Patient localForPatient = getPatientWithSameServiceIdentifier(patient, remoteStorePath);
            if (localForPatient == null) {
                localForPatient = patient.addIdentifier(createIdentifierToRemoteResource(patient, remoteStorePath));
//                localForPatient.addLink(new PatientLinkComponent().setType(LinkType.SEEALSO).setOther(patient));
                createResources.add(localForPatient);
                patients.add(localForPatient);
            } else {
                // patient already exists so we should update the reference to ours
//                updateResources.add(forPatient.setId(patientWithSameIdentifier.getIdElement().getValue()));
            }
        }

        // Run the transaction
        return fhirUtil.getFhirClient(localFhirStorePath).transaction()
                .withBundle(createBundleFromResources(createResources, updateResources)).execute();
    }

    private List<Patient> getForPatientFromServer(IGenericClient fhirClient, List<ServiceRequest> serviceRequests) {
        List<Patient> patients = new ArrayList<>();
        for (ServiceRequest serviceRequest : serviceRequests) {
            if (serviceRequest.getSubject() != null && serviceRequest.getSubject().getReference() != null) {
                patients.add(fhirClient.read().resource(Patient.class)
                        .withId(serviceRequest.getSubject().getReference()).execute());
            }
        }
        return patients;
    }

    private void replaceLocalReferenceWithAbsoluteReference(String fhirStorePath, Reference reference) {
        reference.setReference(fhirStorePath + reference.getReference());

    }

    private Identifier createIdentifierToRemoteResource(IDomainResource remoteResource, String remoteStorePath) {
        Identifier identifier = new Identifier();
        identifier.setSystem(remoteStorePath);
        identifier.setType(new CodeableConcept()
                .addCoding(new Coding().setCode("externalId").setSystem(fhirConfig.getOeFhirSystem() + "/genIdType")));
        identifier.setValue(remoteResource.getIdElement().getIdPart());
        return identifier;
    }

    private Task getTaskWithSameIdentifier(Task remoteTask, String remoteStorePath) {
        Map<String, List<String>> localSearchParams = new HashMap<>();
        localSearchParams.put(Task.SP_IDENTIFIER,
                Arrays.asList(remoteStorePath + "|" + remoteTask.getIdElement().getIdPart()));

        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search().forResource(Task.class).whereMap(localSearchParams)
                .returnBundle(Bundle.class).execute();
        return (Task) localBundle.getEntryFirstRep().getResource();
    }

    private ServiceRequest getServiceRequestWithSameIdentifier(ServiceRequest basedOn, String remoteStorePath) {
        Map<String, List<String>> localSearchParams = new HashMap<>();
        localSearchParams.put(ServiceRequest.SP_IDENTIFIER,
                Arrays.asList(remoteStorePath + "|" + basedOn.getIdElement().getIdPart()));

        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search().forResource(ServiceRequest.class).whereMap(localSearchParams)
                .returnBundle(Bundle.class).execute();
        return (ServiceRequest) localBundle.getEntryFirstRep().getResource();
    }

    private Patient getPatientWithSameServiceIdentifier(Patient remotePatient, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Map<String, List<String>> localSearchParams = new HashMap<>();
        localSearchParams.put(Patient.SP_IDENTIFIER,
                Arrays.asList(remoteStorePath + "|" + remotePatient.getIdElement().getIdPart()));

        Bundle localBundle = localFhirClient.search().forResource(Patient.class).whereMap(localSearchParams)
                .returnBundle(Bundle.class).execute();
        return (Patient) localBundle.getEntryFirstRep().getResource();
    }

    private Patient getPatientWithSameIdIfExists(Patient remotePatient, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search().forResource(Patient.class)
                .where(Patient.RES_ID.exactly().code(remotePatient.getIdElement().getId())).returnBundle(Bundle.class)
                .execute();
        return (Patient) localBundle.getEntryFirstRep().getResource();
    }

    private Bundle createBundleFromResources(List<Resource> createResources, List<Resource> updateResources) {
        Bundle transactionBundle = new Bundle();
        transactionBundle.setType(BundleType.TRANSACTION);
        for (Resource createResource : createResources) {
            transactionBundle.addEntry(createTransactionComponentFromResource(createResource, HTTPVerb.PUT));
//            transactionBundle.addEntry(createTransactionComponentFromResource(createResource, HTTPVerb.POST));
            transactionBundle.setTotal(transactionBundle.getTotal() + 1);
        }
        for (Resource updateResource : updateResources) {
            transactionBundle.addEntry(createTransactionComponentFromResource(updateResource, HTTPVerb.PUT));
            transactionBundle.setTotal(transactionBundle.getTotal() + 1);
        }
        LogEvent.logDebug(this.getClass().getName(), "createBundleFromResources",
                fhirContext.newJsonParser().encodeResourceToString(transactionBundle));
        return transactionBundle;
    }

    private BundleEntryComponent createTransactionComponentFromResource(Resource resource, HTTPVerb method) {
        String sourceResourceId = resource.getIdElement().getIdPart();

        BundleEntryComponent transactionComponent = new BundleEntryComponent();
        transactionComponent.setResource(resource);
        transactionComponent.getRequest().setMethod(method);
        transactionComponent.getRequest().setUrl(resource.getResourceType() + "/" + UUID.randomUUID());
//        transactionComponent.getRequest().setUrl(resource.getResourceType() + "/" + sourceResourceId);

        return transactionComponent;
    }

    private List<ServiceRequest> getBasedOnServiceRequestsFromServer(IGenericClient fhirClient, Task remoteTask) {
        List<ServiceRequest> basedOn = new ArrayList<>();
        for (Reference basedOnElement : remoteTask.getBasedOn()) {
            basedOn.add(
                    fhirClient.read().resource(ServiceRequest.class).withId(basedOnElement.getReference()).execute());
        }

        return basedOn;
    }

    private Patient getForPatientFromServer(IGenericClient fhirClient, Task remoteTask) {
        if (remoteTask.getFor() == null || remoteTask.getFor().getReference() == null) {
            return null;
        }
        Patient forPatient = fhirClient.read().resource(Patient.class).withId(remoteTask.getFor().getReference())
                .execute();
        return forPatient;
    }

    // these methods can find the results in the bundle when include is used instead
    // of reaching back to the server
    private List<ServiceRequest> getBasedOnServiceRequestFromBundle(Bundle bundle, Task task) {
        List<ServiceRequest> basedOn = new ArrayList<>();
        for (Reference reference : task.getBasedOn()) {
            basedOn.add((ServiceRequest) findResourceInBundle(bundle, reference.getReference()));
        }
        return basedOn;
    }

    private Patient getForPatientFromBundle(Bundle bundle, Task task) {
        if (task.getFor() != null && task.getFor().getReference() != null) {
            return (Patient) findResourceInBundle(bundle, task.getFor().getReference());
        } else {
            return null;
        }
    }

    private IBaseResource findResourceInBundle(Bundle bundle, String reference) {
        for (BundleEntryComponent bundleComponent : bundle.getEntry()) {
            if (bundleComponent.hasResource() && bundleComponent.getFullUrl().endsWith(reference)) {
                return bundleComponent.getResource();
            }
        }
        return null;

    }

    private List<Patient> getForPatientFromBundle(Bundle bundle, List<ServiceRequest> serviceRequestList) {
        List<Patient> patients = new ArrayList<>();

        for (ServiceRequest serviceRequest : serviceRequestList) {
            if (serviceRequest.getSubject() != null && serviceRequest.getSubject().getReference() != null) {
                patients.add((Patient) findResourceInBundle(bundle, serviceRequest.getSubject().getReference()));
            }
        }
        return patients;
    }

}
