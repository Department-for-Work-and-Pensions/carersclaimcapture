package uk.gov.dwp.carersallowance.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.ui.Model;

import uk.gov.dwp.carersallowance.session.SessionManager;

public class ThirdPartyController extends AbstractFormController {

    public ThirdPartyController(SessionManager sessionManager, MessageSource messageSource) {
        super(sessionManager, messageSource);
    }

    public String showForm(HttpServletRequest request, Model model) {
        return super.showForm(request, model);
    }

    public String postForm(HttpServletRequest request, HttpSession session, Model model) {
        return super.postForm(request, session, model);
    }

//    protected void validate(Map<String, String[]> fieldValues, String[] fields) {
//
//        validateMandatoryField(fieldValues, "thirdParty");
//        if(fieldValue_Equals(fieldValues, "thirdParty", "no")) {
//            validateMandatoryField(fieldValues, "nameAndOrganisation");
//        }
//
//    }
}
