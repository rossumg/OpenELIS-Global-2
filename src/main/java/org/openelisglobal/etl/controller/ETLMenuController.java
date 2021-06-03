package org.openelisglobal.etl.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.SystemConfiguration;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.etl.form.ETLMenuForm;
import org.openelisglobal.etl.service.ETLService;
import org.openelisglobal.etl.valueholder.ETLRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ETLMenuController extends BaseMenuController<Dictionary> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIDs*" };

    @Autowired
    DictionaryService dictionaryService;
    @Autowired
    ETLService etlService;
    @Autowired
    private FhirTransformService fhirTransformService;
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = { "/ETLMenu", "/SearchETLMenu" }, method = RequestMethod.GET)
    public ModelAndView showETLMenu(HttpServletRequest request, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ETLMenuForm form = new ETLMenuForm();

       // ETL
//        FhirTransformService, 
//          pull required fhir resources and map to ETL object with etl type and json data string
//        persistETLService(etl_json) will persist to etl 
//        uuid/last_updated timestamp /type/data table
//        
//        intention to share example json with front end user.
        
        List<ETLRecord> etlRecordList = fhirTransformService.getLatestFhirforETL(DateUtil.getNowAsTimestamp());
        for (ETLRecord etlRecord: etlRecordList) {
            etlService.insert(etlRecord);
        }       
        
        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            return findForward(forward, form);
        } else {
            request.setAttribute("menuDefinition", "ETLMenuDefinition");
            addFlashMsgsToRequest(request);
            return findForward(forward, form);
        }
    }

    @Override
    protected List<Dictionary> createMenuList(AdminOptionMenuForm<Dictionary> form, HttpServletRequest request) {

        List<Dictionary> dictionaries;

        String stringStartingRecNo = (String) request.getAttribute("startingRecNo");
        int startingRecNo = Integer.parseInt(stringStartingRecNo);
        // bugzilla 1413
        int total;
        if (YES.equals(request.getParameter("search"))) {
            dictionaries = dictionaryService.getPagesOfSearchedDictionaries(startingRecNo,
                    request.getParameter("searchString"));
            total = dictionaryService.getCountSearchedDictionaries(request.getParameter("searchString"));
        } else {
            dictionaries = dictionaryService.getPage(startingRecNo);
            total = dictionaryService.getCount();
            // end of bugzilla 1413
        }

        request.setAttribute("menuDefinition", "ETLMenuDefinition");

        // bugzilla 1411 set pagination variables
        // bugzilla 1413 set pagination variables for searched results
        request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(total));
        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));
        int numOfRecs = 0;
        if (dictionaries.size() > SystemConfiguration.getInstance().getDefaultPageSize()) {
            numOfRecs = SystemConfiguration.getInstance().getDefaultPageSize();
        } else {
            numOfRecs = dictionaries.size();
        }
        numOfRecs--;
        int endingRecNo = startingRecNo + numOfRecs;
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));
        // end bugzilla 1411

        // bugzilla 1413
        request.setAttribute(MENU_SEARCH_BY_TABLE_COLUMN, "dictionary.dictEntry");
        // bugzilla 1413 set up a seraching mode so the next and previous action will
        // know
        // what to do

        if (YES.equals(request.getParameter("search"))) {
            request.setAttribute(IN_MENU_SELECT_LIST_HEADER_SEARCH, "true");
        }

        return dictionaries;
    }

    @Override
    protected int getPageSize() {
        return SystemConfiguration.getInstance().getDefaultPageSize();
    }

    @Override
    protected String getDeactivateDisabled() {
        return "false";
    }

    @RequestMapping(value = "/DeleteETL", method = RequestMethod.POST)
    public ModelAndView showDeleteETL(HttpServletRequest request,
            @ModelAttribute("form") @Valid ETLMenuForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        List<String> selectedIDs = form.getSelectedIDs();

        List<Dictionary> dictionaries = new ArrayList<>();
        for (int i = 0; i < selectedIDs.size(); i++) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId(selectedIDs.get(i));
            dictionary.setSysUserId(getSysUserId(request));
            dictionaries.add(dictionary);
        }

        try {
            dictionaryService.deleteAll(dictionaries);
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e.toString(), e);
            if (e.getException() instanceof org.hibernate.StaleObjectStateException) {
                result.reject("errors.OptimisticLockException");
            } else {
                result.reject("errors.DeleteException");
            }
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
            return findForward(FWD_FAIL_DELETE, form);

        }

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_DELETE, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "masterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage.do";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/DictionaryMenu.do";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "redirect:/DictionaryMenu.do";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "dictionary.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "dictionary.browse.title";
    }

}
