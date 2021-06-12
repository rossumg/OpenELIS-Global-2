package org.openelisglobal.result.controller;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.service.ReferralTypeService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsPaging;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.form.LogbookResultsForm;
import org.openelisglobal.result.form.LogbookResultsForm.LogbookResults;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultInventoryService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.util.StatusRules;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LogbookResultsController extends LogbookResultsBaseController {

    private final String[] ALLOWED_FIELDS = new String[] { "collectionDate", "recievedDate", "selectedTest",
            "selectedAnalysisStatus", "selectedSampleStatus", "testSectionId", "type", "currentPageID",
            "testResult*.accessionNumber", "testResult*.isModified", "testResult*.analysisId", "testResult*.resultId",
            "testResult*.testId", "testResult*.technicianSignatureId", "testResult*.testKitId",
            "testResult*.resultLimitId", "testResult*.resultType", "testResult*.valid", "testResult*.referralId",
            "testResult*.referralCanceled", "testResult*.considerRejectReason", "testResult*.hasQualifiedResult",
            "testResult*.shadowResultValue", "testResult*.reflexJSONResult", "testResult*.testDate",
            "testResult*.analysisMethod", "testResult*.testMethod", "testResult*.testKitInventoryId",
            "testResult*.forceTechApproval", "testResult*.lowerNormalRange", "testResult*.upperNormalRange",
            "testResult*.significantDigits", "testResult*.resultValue", "testResult*.qualifiedResultValue",
            "testResult*.multiSelectResultValues", "testResult*.multiSelectResultValues",
            "testResult*.qualifiedResultValue", "testResult*.qualifiedResultValue", "testResult*.shadowReferredOut",
            "testResult*.referredOut", "testResult*.referralReasonId", "testResult*.technician",
            "testResult*.shadowRejected", "testResult*.rejected", "testResult*.rejectReasonId", "testResult*.note",
            "paging.currentPage" };

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private ResultSignatureService resultSigService;
    @Autowired
    private ResultInventoryService resultInventoryService;
    @Autowired
    private ReferralService referralService;
    @Autowired
    private ResultLimitService resultLimitService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private LogbookResultsPersistService logbookPersistService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private FhirTransformService fhirTransformService;

    private final String RESULT_SUBJECT = "Result Note";
    private final String REFERRAL_CONFORMATION_ID;

    private LogbookResultsController(ReferralTypeService referralTypeService) {
        ReferralType referralType = referralTypeService.getReferralTypeByName("Confirmation");
        if (referralType != null) {
            REFERRAL_CONFORMATION_ID = referralType.getId();
        } else {
            REFERRAL_CONFORMATION_ID = null;
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/LogbookResults", method = RequestMethod.GET)
    public ModelAndView showLogbookResults(HttpServletRequest request,
            @Validated(LogbookResults.class) @ModelAttribute("form") LogbookResultsForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        LogbookResultsForm newForm = new LogbookResultsForm();
        if (!(result.hasFieldErrors("type") || result.hasFieldErrors("testSectionId"))) {
            newForm.setType(form.getType());
            newForm.setTestSectionId(form.getTestSectionId());
        }
        return getLogbookResults(request, newForm);
    }

    private ModelAndView getLogbookResults(HttpServletRequest request, LogbookResultsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // boolean useTechnicianName = ConfigurationProperties.getInstance()
        // .isPropertyValueEqual(Property.resultTechnicianName, "true");
        // boolean alwaysValidate = ConfigurationProperties.getInstance()
        // .isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
        // boolean supportReferrals =
        // FormFields.getInstance().useField(Field.ResultsReferral);
        // String statusRuleSet =
        // ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules);

        String testSectionId = form.getTestSectionId();
        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        TestSection ts = null;

        String currentDate = getCurrentDate();
        form.setCurrentDate(currentDate);
        form.setReferralReasons(DisplayListService.getInstance().getList(DisplayListService.ListType.REFERRAL_REASONS));
        form.setRejectReasons(DisplayListService.getInstance()
                .getNumberedListWithLeadingBlank(DisplayListService.ListType.REJECTION_REASONS));

        // load testSections for drop down
        List<IdValuePair> testSections = DisplayListService.getInstance().getList(ListType.TEST_SECTION);
        form.setTestSections(testSections);
        form.setTestSectionsByName(DisplayListService.getInstance().getList(ListType.TEST_SECTION_BY_NAME));

        if (!GenericValidator.isBlankOrNull(testSectionId)) {
            ts = testSectionService.get(testSectionId);
        }

        setRequestType(ts == null ? MessageUtil.getMessage("workplan.unit.types") : ts.getLocalizedName());
        List<TestResultItem> tests;

        ResultsPaging paging = new ResultsPaging();
        List<InventoryKitItem> inventoryList = new ArrayList<>();
        ResultsLoadUtility resultsLoadUtility = SpringContext.getBean(ResultsLoadUtility.class);
        resultsLoadUtility.setSysUser(getSysUserId(request));

        String requestedPage = request.getParameter("page");

        if (GenericValidator.isBlankOrNull(requestedPage)) {
            requestedPage = "1";
            new StatusRules().setAllowableStatusForLoadingResults(resultsLoadUtility);

            if (!GenericValidator.isBlankOrNull(testSectionId)) {
                tests = resultsLoadUtility.getUnfinishedTestResultItemsInTestSection(testSectionId);
                int count = resultsLoadUtility.getTotalCountAnalysisByTestSectionAndStatus(testSectionId);
                request.setAttribute("analysisCount", count);
                request.setAttribute("pageSize", tests.size());
            } else {
                tests = new ArrayList<>();
            }

            if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.PATIENT_DATA_ON_RESULTS_BY_ROLE,
                    "true") && !userHasPermissionForModule(request, "PatientResults")) {
                for (TestResultItem resultItem : tests) {
                    resultItem.setPatientInfo("---");
                }

            }

            paging.setDatabaseResults(request, form, tests);

        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }
        form.setDisplayTestKit(false);
        if (ts != null) {
            // this does not look right what happens after a new page!!!
            boolean isHaitiClinical = ConfigurationProperties.getInstance()
                    .isPropertyValueEqual(Property.configurationName, "Haiti Clinical");
            if (resultsLoadUtility.inventoryNeeded() || (isHaitiClinical && ("VCT").equals(ts.getTestSectionName()))) {
                InventoryUtility inventoryUtility = SpringContext.getBean(InventoryUtility.class);
                inventoryList = inventoryUtility.getExistingActiveInventory();

                form.setDisplayTestKit(true);
            }
        }
        List<String> hivKits = new ArrayList<>();
        List<String> syphilisKits = new ArrayList<>();

        for (InventoryKitItem item : inventoryList) {
            if (item.getType().equals("HIV")) {
                hivKits.add(item.getInventoryLocationId());
            } else {
                syphilisKits.add(item.getInventoryLocationId());
            }
        }
        form.setHivKits(hivKits);
        form.setSyphilisKits(syphilisKits);
        form.setInventoryItems(inventoryList);

        addFlashMsgsToRequest(request);
        return findForward(FWD_SUCCESS, form);
    }

    private String getCurrentDate() {
        Date today = Calendar.getInstance().getTime();
        return DateUtil.formatDateAsText(today);

    }

    @RequestMapping(value = { "/LogbookResults", "/PatientResults", "/AccessionResults",
            "/StatusResults" }, method = RequestMethod.POST)
    public ModelAndView showLogbookResultsUpdate(HttpServletRequest request,
            @ModelAttribute("form") @Validated(LogbookResultsForm.LogbookResults.class) LogbookResultsForm form,
            BindingResult result, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        boolean useTechnicianName = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.resultTechnicianName, "true");
        boolean alwaysValidate = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
        boolean supportReferrals = FormFields.getInstance().useField(Field.ResultsReferral);
        String statusRuleSet = ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules);

        if ("true".equals(request.getParameter("pageResults"))) {
            return getLogbookResults(request, form);
        }

        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

//  Shows current session records, can be current, stale/empty vs. other user
//        ie: empty when another user saved and hasn't reloaded.

        List<Result> checkPagedResults = (List<Result>) request.getSession()
                .getAttribute(IActionConstants.RESULTS_SESSION_CACHE);
        List<Result> checkResults = (List<Result>) checkPagedResults.get(0);
        if (checkResults.size() == 0) {
            LogEvent.logDebug(this.getClass().getName(), "LogbookResults()", "Attempted save of stale page.");
            
            List<TestResultItem> resultList = form.getTestResult();
            for (TestResultItem item : resultList) {
                item.setFailedValidation(true);
                item.setNote("Result has been saved by another user.");
            }
            
            ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(getSysUserId(request));
            actionDataSet.filterModifiedItems(form.getTestResult());

            Errors errors = actionDataSet.validateModifiedItems();
            
            if (true) {
                saveErrors(errors);
                return findForward(FWD_VALIDATION_ERROR, form);
            }
        }

        List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();

        ResultsPaging paging = new ResultsPaging();
        paging.updatePagedResults(request, form);

        // gnr
//        a    |  si   |   s    | accession_number  |   r    |   sh   |  pat   |  pro   |    pro_fn    |    pro_ln     
//        --------+-------+--------+-------------------+--------+--------+--------+--------+--------------+---------------
//         164427 | 99598 | 162747 | 20210000000000099 | 163913 | 162703 | 137408 | 162721 | SWAB         | OROPHARYNGEAL
//         164428 | 99599 | 162748 | 20210000000000100 | 163914 | 162704 | 137409 | 162722 | SHYAMAL      | TAJAH
//         164429 | 99600 | 162749 | 20210000000000101 | 163915 | 162705 | 137410 | 162723 | SWAB         | OROPHARYNGEAL
//         164430 | 99601 | 162750 | 20210000000000102 | 163916 | 162706 | 137411 | 162724 | RUDRA AVISEK | RAMBURUTH
//         164431 | 99602 | 162751 | 20210000000000103 | 163917 | 162707 | 137412 | 162725 | SWAB         | OROPHARYNGEAL

        if (false) {
            TestResultItem testResultItem = new TestResultItem();
            testResultItem.setAccessionNumber("20210000000000100");
            testResultItem.setAnalysisId("164428"); 
            //            analysisMethod(null);    
            //            analysisStatusId    null    
            //            considerRejectReason    "" (id=16538)   
            //            dictionaryResults   null    
            testResultItem.setDisplayResultAsLog(false);   
            testResultItem.setFailedValidation(    false);
            //          forceTechApproval   null
            testResultItem.setHasQualifiedResult(  false   );
            //          initialSampleCondition  null
            testResultItem.setChildReflex(   false   );
            testResultItem.setIsGroupSeparator(    false   );
            testResultItem.setIsModified(  true    );
            testResultItem.setReflexGroup(   false   );
            testResultItem.setServingAsTestGroupIdentifier(  false   );
            testResultItem.setUserChoiceReflex(  false   );
            testResultItem.setLowerAbnormalRange(  0.0 );
            testResultItem.setLowerNormalRange(    0.0 );
            //          multiSelectResultValues null
            //          nationalId  null
            //          nextVisitDate   null
            //          testResultItem.setnonconforming   false
            //          testResultItem.setnormal  true
            //          normalRange "" (id=16538)
            //          note    "" (id=16538)
            testResultItem.setNotIncludedInWorkplan(   false   );
            //          pastNotes   null
            //          patientInfo null
            //          patientName null
            //          qualifiedDictionaryId   null
            //          qualifiedResultId   null
            //          qualifiedResultValue    "" (id=16538)
            testResultItem.setReadOnly(    false   );
            //          receivedDate    null
            testResultItem.setReferralCanceled(    false   );
            //          referralId  "" (id=16538)
            //          referralReasonId    "" (id=16538)
            //          testResultItem.setreferredOut false
            //          reflexJSONResult    null
            testResultItem.setReflexParentGroup(   0   );
            testResultItem.setRejected(    false   );
            testResultItem.setRejectReasonId(  "0"  );
            //        remarks null
            testResultItem.setRemove(  "no" );
            testResultItem.setReportable(  false   );
            //        result  null
            //         TODO testResultItem.setResultDisplayType   TestResultItem$ResultDisplayType  (id=16541)
            //        resultId    "" (id=16538)
            testResultItem.setResultLimitId(   "294"     );
            testResultItem.setResultType(  "D"   );
            testResultItem.setResultValue( "821"     );
            testResultItem.setSampleGroupingNumber(    1   );
            //        sampleSource    null
            //        sampleType  null
            //        sequenceNumber  null
            testResultItem.setShadowReferredOut(   false   );
            testResultItem.setShadowRejected(  false   );
            testResultItem.setShadowResultValue(   "821"    );
            testResultItem.setShowSampleDetails(   true    );
            //        siblingReflexKey    null
            //        testResultItem.setsignificantDigits   -1
            //        technician  null
            //        technicianSignatureId   "" (id=16538)
            testResultItem.setTestDate(    "10/06/2021");
            testResultItem.setTestId(  "378" );
            //        testKit1InventoryId null
            //        testKitId   "" (id=16538)
            testResultItem.setTestKitInactive( false   );
            //        testMethod  null
            //        testName    null
            //        testSortOrder   null
            //        thisReflexKey   null
            //        unitsOfMeasure  "" (id=16538)
            //        upperAbnormalRange  0.0
            //        upperNormalRange    0.0
            testResultItem.setValid(   true    );

            ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet("215");
            List<TestResultItem> tests = new ArrayList<TestResultItem>();
            tests.add(testResultItem);
            actionDataSet.filterModifiedItems(tests);

            createResultsFromItems(actionDataSet, supportReferrals, alwaysValidate, useTechnicianName, statusRuleSet);
            createAnalysisOnlyUpdates(actionDataSet);

            try {
                logbookPersistService.persistDataSet(actionDataSet, updaters, getSysUserId(request));
                try {
                    fhirTransformService.transformPersistResultsEntryFhirObjects(actionDataSet);
                } catch (FhirTransformationException | FhirPersistanceException e) {
                    LogEvent.logError(e);
                }
            } catch (LIMSRuntimeException e) {
                String errorMsg;
                if (e.getException() instanceof StaleObjectStateException) {
                    errorMsg = "errors.OptimisticLockException";
                } else {
                    LogEvent.logDebug(e);
                    errorMsg = "errors.UpdateException";
                }
            }

            for (IResultUpdate updater : updaters) {
                try {
                    updater.postTransactionalCommitUpdate(actionDataSet);
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getName(), "showLogbookResultsUpdate",
                            "error doing a post transactional commit");
                    LogEvent.logError(e);
                }
            }
            
            return findForward(FWD_SUCCESS_INSERT, form);
        } // end if
        
        List<TestResultItem> tests = paging.getResults(request);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(getSysUserId(request));
        
        actionDataSet.filterModifiedItems(tests);
        Errors errors = actionDataSet.validateModifiedItems();

        if (errors.hasErrors()) {
            saveErrors(errors);
            return findForward(FWD_VALIDATION_ERROR, form);
        }

        createResultsFromItems(actionDataSet, supportReferrals, alwaysValidate, useTechnicianName, statusRuleSet);
        createAnalysisOnlyUpdates(actionDataSet);

        try {
            logbookPersistService.persistDataSet(actionDataSet, updaters, getSysUserId(request));
            try {
                fhirTransformService.transformPersistResultsEntryFhirObjects(actionDataSet);
            } catch (FhirTransformationException | FhirPersistanceException e) {
                LogEvent.logError(e);
            }
        } catch (LIMSRuntimeException e) {
            String errorMsg;
            if (e.getException() instanceof StaleObjectStateException) {
                errorMsg = "errors.OptimisticLockException";
            } else {
                LogEvent.logDebug(e);
                errorMsg = "errors.UpdateException";
            }

            errors.reject(errorMsg, errorMsg);
            saveErrors(errors);
            return findForward(FWD_FAIL_INSERT, form);

        }

        for (IResultUpdate updater : updaters) {
            try {
                updater.postTransactionalCommitUpdate(actionDataSet);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "showLogbookResultsUpdate",
                        "error doing a post transactional commit");
                LogEvent.logError(e);
            }
        }
        
        // gnr end loop

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        if (GenericValidator.isBlankOrNull(form.getType())) {
            return findForward(FWD_SUCCESS_INSERT, form);
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("type", form.getType());
            return getForwardWithParameters(findForward(FWD_SUCCESS_INSERT, form), params);
        }
    }

    private void createAnalysisOnlyUpdates(ResultsUpdateDataSet actionDataSet) {
        for (TestResultItem testResultItem : actionDataSet.getAnalysisOnlyChangeResults()) {

            Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
            analysis.setSysUserId(getSysUserId(request));
            analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
            if (testResultItem.getAnalysisMethod() != null) {
                analysis.setAnalysisType(testResultItem.getAnalysisMethod());
            }
            actionDataSet.getModifiedAnalysis().add(analysis);
        }
    }

    private void createResultsFromItems(ResultsUpdateDataSet actionDataSet, boolean supportReferrals,
            boolean alwaysValidate, boolean useTechnicianName, String statusRuleSet) {

        for (TestResultItem testResultItem : actionDataSet.getModifiedItems()) {

            Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
            analysis.setStatusId(getStatusForTestResult(testResultItem, alwaysValidate));
            analysis.setSysUserId(getSysUserId(request));
            if (supportReferrals) {
                handleReferrals(testResultItem, analysis, actionDataSet);
            }
            actionDataSet.getModifiedAnalysis().add(analysis);

            actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.INTERNAL,
                    testResultItem.getNote(), RESULT_SUBJECT, getSysUserId(request)));

            if (testResultItem.isShadowRejected()) {
                String rejectedReasonId = testResultItem.getRejectReasonId();
                for (IdValuePair rejectReason : DisplayListService.getInstance().getList(ListType.REJECTION_REASONS)) {
                    if (rejectedReasonId.equals(rejectReason.getId())) {
                        actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.REJECTION_REASON,
                                rejectReason.getValue(), RESULT_SUBJECT, getSysUserId(request)));
                        break;
                    }
                }
            }

            ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
            ResultSaveService resultSaveService = new ResultSaveService(analysis, getSysUserId(request));
            // deletable Results will be written to, not read
            List<Result> results = resultSaveService.createResultsFromTestResultItem(bean,
                    actionDataSet.getDeletableResults());

            analysis.setCorrectedSincePatientReport(
                    resultSaveService.isUpdatedResult() && analysisService.patientReportHasBeenDone(analysis));

            if (analysisService.hasBeenCorrectedSinceLastPatientReport(analysis)) {
                actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.EXTERNAL,
                        MessageUtil.getMessage("note.corrected.result"), RESULT_SUBJECT, getSysUserId(request)));
            }

            // If there is more than one result then each user selected reflex gets mapped
            // to that result
            for (Result result : results) {
                addResult(result, testResultItem, analysis, results.size() > 1, actionDataSet, useTechnicianName);

                if (analysisShouldBeUpdated(testResultItem, result, supportReferrals)) {
                    updateAnalysis(testResultItem, testResultItem.getTestDate(), analysis, statusRuleSet);
                }
            }
        }
    }

    private void handleReferrals(TestResultItem testResultItem, Analysis analysis, ResultsUpdateDataSet actionDataSet) {

        Referral referral = null;
        // referredOut means the referral checkbox was checked, repeating
        // analysis means that we have multi-select results, so only do one.
        if (testResultItem.isShadowReferredOut()
                && !actionDataSet.getReferredAnalysisIds().contains(analysis.getId())) {
            actionDataSet.getReferredAnalysisIds().add(analysis.getId());
            // If it is a new result or there is no referral ID that means
            // that a new referral has to be created if it was checked and
            // it was canceled then we are un-canceling a canceled referral
            if (testResultItem.getResultId() == null
                    || GenericValidator.isBlankOrNull(testResultItem.getReferralId())) {
                referral = new Referral();
                referral.setFhirUuid(UUID.randomUUID());
                referral.setStatus(ReferralStatus.CREATED);
                referral.setReferralTypeId(REFERRAL_CONFORMATION_ID);
                referral.setSysUserId(getSysUserId(request));
                referral.setRequestDate(new Timestamp(new Date().getTime()));
                referral.setRequesterName(testResultItem.getTechnician());
                referral.setAnalysis(analysis);
                referral.setReferralReasonId(testResultItem.getReferralReasonId());
            } else if (testResultItem.isReferralCanceled()) {
                referral = referralService.get(testResultItem.getReferralId());
                referral.setFhirUuid(UUID.randomUUID());
                referral.setStatus(ReferralStatus.CREATED);
                referral.setSysUserId(getSysUserId(request));
                referral.setRequesterName(testResultItem.getTechnician());
                referral.setReferralReasonId(testResultItem.getReferralReasonId());
            }

            String originalResultNote = MessageUtil.getMessage("referral.original.result") + ": ";
            if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResultItem.getResultType())
                    || TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(testResultItem.getResultType())) {
                if ("0".equals(testResultItem.getResultValue())) {
                    originalResultNote = originalResultNote + "";
                } else {
                    Dictionary dictionary = dictionaryService.get(testResultItem.getResultValue());
                    if (dictionary.getLocalizedDictionaryName() == null) {
                        originalResultNote = originalResultNote + dictionary.getDictEntry();
                    } else {
                        originalResultNote = originalResultNote
                                + dictionary.getLocalizedDictionaryName().getLocalizedValue();
                    }
                }
            } else {
                originalResultNote = originalResultNote + testResultItem.getResultValue();
            }

            actionDataSet.getSavableReferrals().add(referral);
            actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.INTERNAL, originalResultNote,
                    RESULT_SUBJECT, this.getSysUserId(request)));

        }
    }

    protected boolean analysisShouldBeUpdated(TestResultItem testResultItem, Result result, boolean supportReferrals) {
        return result != null && !GenericValidator.isBlankOrNull(result.getValue())
                || (supportReferrals && ResultUtil.isReferred(testResultItem))
                || ResultUtil.isForcedToAcceptance(testResultItem) || testResultItem.isShadowRejected();
    }

    private void addResult(Result result, TestResultItem testResultItem, Analysis analysis,
            boolean multipleResultsForAnalysis, ResultsUpdateDataSet actionDataSet, boolean useTechnicianName) {
        boolean newResult = result.getId() == null;
        boolean newAnalysisInLoop = analysis != actionDataSet.getPreviousAnalysis();

        ResultSignature technicianResultSignature = null;

        if (useTechnicianName && newAnalysisInLoop) {
            technicianResultSignature = createTechnicianSignatureFromResultItem(testResultItem);
        }

        ResultInventory testKit = createTestKitLinkIfNeeded(testResultItem, ResultsLoadUtility.TESTKIT);

        analysis.setReferredOut(testResultItem.isReferredOut());
        analysis.setEnteredDate(DateUtil.getNowAsTimestamp());

        if (newResult) {
            analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
            analysis.setRevision("1");
        } else if (newAnalysisInLoop) {
            analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
        }

        SampleService sampleService = SpringContext.getBean(SampleService.class);
        Sample sample = sampleService.getSampleByAccessionNumber(testResultItem.getAccessionNumber());
        Patient patient = sampleService.getPatient(sample);

        Map<String, List<String>> triggersToReflexesMap = new HashMap<>();

        getSelectedReflexes(testResultItem.getReflexJSONResult(), triggersToReflexesMap);

        if (newResult) {
            actionDataSet.getNewResults().add(new ResultSet(result, technicianResultSignature, testKit, patient, sample,
                    triggersToReflexesMap, multipleResultsForAnalysis));
        } else {
            actionDataSet.getModifiedResults().add(new ResultSet(result, technicianResultSignature, testKit, patient,
                    sample, triggersToReflexesMap, multipleResultsForAnalysis));
        }

        actionDataSet.setPreviousAnalysis(analysis);
    }

    private void getSelectedReflexes(String reflexJSONResult, Map<String, List<String>> triggersToReflexesMap) {
        if (!GenericValidator.isBlankOrNull(reflexJSONResult)) {
            JSONParser parser = new JSONParser();
            try {
                JSONObject jsonResult = (JSONObject) parser.parse(reflexJSONResult.replaceAll("'", "\""));

                for (Object compoundReflexes : jsonResult.values()) {
                    if (compoundReflexes != null) {
                        String triggerIds = (String) ((JSONObject) compoundReflexes).get("triggerIds");
                        List<String> selectedReflexIds = new ArrayList<>();
                        JSONArray selectedReflexes = (JSONArray) ((JSONObject) compoundReflexes).get("selected");
                        for (Object selectedReflex : selectedReflexes) {
                            selectedReflexIds.add(((String) selectedReflex));
                        }
                        triggersToReflexesMap.put(triggerIds.trim(), selectedReflexIds);
                    }
                }
            } catch (ParseException e) {
                LogEvent.logDebug(e);
            }
        }
    }

    private String getStatusForTestResult(TestResultItem testResult, boolean alwaysValidate) {
        if (testResult.isShadowRejected()) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected);
        } else if (alwaysValidate || !testResult.isValid() || ResultUtil.isForcedToAcceptance(testResult)) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
        } else if (noResults(testResult.getShadowResultValue(), testResult.getMultiSelectResultValues(),
                testResult.getResultType())) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
        } else {
            if (!GenericValidator.isBlankOrNull(testResult.getResultLimitId())) {
                ResultLimit resultLimit = resultLimitService.get(testResult.getResultLimitId());
                if (resultLimit.isAlwaysValidate()) {
                    return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
                }
                if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(testResult.getResultType())
                        && !testResult.getResultValue().equals(resultLimit.getDictionaryNormalId())) {
                    return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
                }
            }

            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        }
    }

    private boolean noResults(String value, String multiSelectValue, String type) {

        return (GenericValidator.isBlankOrNull(value) && GenericValidator.isBlankOrNull(multiSelectValue))
                || (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(type) && "0".equals(value));
    }

    private ResultInventory createTestKitLinkIfNeeded(TestResultItem testResult, String testKitName) {
        ResultInventory testKit = null;

        if ((TestResultItem.ResultDisplayType.SYPHILIS == testResult.getRawResultDisplayType()
                || TestResultItem.ResultDisplayType.HIV == testResult.getRawResultDisplayType())
                && ResultsLoadUtility.TESTKIT.equals(testKitName)) {

            testKit = createTestKit(testResult, testKitName, testResult.getTestKitId());
        }

        return testKit;
    }

    private ResultInventory createTestKit(TestResultItem testResult, String testKitName, String testKitId)
            throws LIMSRuntimeException {
        ResultInventory testKit;
        testKit = new ResultInventory();

        if (!GenericValidator.isBlankOrNull(testKitId)) {
            testKit.setId(testKitId);
            testKit = resultInventoryService.get(testKitId);
        }

        testKit.setInventoryLocationId(testResult.getTestKitInventoryId());
        testKit.setDescription(testKitName);
        testKit.setSysUserId(getSysUserId(request));
        return testKit;
    }

    private void updateAnalysis(TestResultItem testResultItem, String testDate, Analysis analysis,
            String statusRuleSet) {
        if (testResultItem.getAnalysisMethod() != null) {
            analysis.setAnalysisType(testResultItem.getAnalysisMethod());
        }
        analysis.setStartedDateForDisplay(testDate);

        // This needs to be refactored -- part of the logic is in
        // getStatusForTestResult. RetroCI over rides to whatever was set before
        if (statusRuleSet.equals(STATUS_RULES_RETROCI)) {
            if (!SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)
                    .equals(analysis.getStatusId())) {
                analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
                analysis.setStatusId(
                        SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance));
            }
        } else if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(), AnalysisStatus.Finalized)
                || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                        AnalysisStatus.TechnicalAcceptance)
                || (analysis.isReferredOut()
                        && !GenericValidator.isBlankOrNull(testResultItem.getShadowResultValue()))) {
            analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
        }

    }

    private ResultSignature createTechnicianSignatureFromResultItem(TestResultItem testResult) {
        ResultSignature sig = null;

        // The technician signature may be blank if the user changed a
        // conclusion and then changed it back. It will be dirty
        // but will not need a signature
        if (!GenericValidator.isBlankOrNull(testResult.getTechnician())) {
            sig = new ResultSignature();

            if (!GenericValidator.isBlankOrNull(testResult.getTechnicianSignatureId())) {
                sig = resultSigService.get(testResult.getTechnicianSignatureId());
            }

            sig.setIsSupervisor(false);
            sig.setNonUserName(testResult.getTechnician());

            sig.setSysUserId(getSysUserId(request));
        }
        return sig;
    }

    private String findLogBookForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/LogbookResults.do";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "resultsLogbookDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findAccessionForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/AccessionResults.do";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "accessionResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "accessionResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findPatientForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/PatientResults.do";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "patientResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "patientResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findStatusForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/StatusResults.do?blank=true";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "statusResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "statusResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (request.getRequestURL().indexOf("LogbookResults") >= 0) {
            return findLogBookForward(forward);
        } else if (request.getRequestURL().indexOf("AccessionResults") >= 0) {
            return findAccessionForward(forward);
        } else if (request.getRequestURL().indexOf("PatientResults") >= 0) {
            return findPatientForward(forward);
        } else if (request.getRequestURL().indexOf("StatusResults") >= 0) {
            return findStatusForward(forward);
        } else {
            return "PageNotFound";
        }
    }
}
