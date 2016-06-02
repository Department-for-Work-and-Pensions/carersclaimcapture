package uk.gov.dwp.carersallowance.controller.started;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.gov.dwp.carersallowance.controller.AbstractFormController;

@Controller
public class SelfEmployedPensionAndExpensesController extends AbstractFormController {
    private static final Logger LOG = LoggerFactory.getLogger(SelfEmployedPensionAndExpensesController.class);

    private static final String CURRENT_PAGE  = "/your-income/self-employment/pensions-and-expenses";
    private static final String PAGE_TITLE    = "Pensions and expenses Self-employment";

    private static final String[] FIELDS = {"payPensionScheme",
                                            "payPensionSchemeText",
                                            "haveExpensesForJob",
                                            "haveExpensesForJobText"};

    @Override
    public String getCurrentPage() {
        return CURRENT_PAGE;
    }

    @Override
    public String[] getFields() {
        return FIELDS;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    @RequestMapping(value=CURRENT_PAGE, method = RequestMethod.GET)
    public String showForm(HttpServletRequest request, Model model) {
        return super.showForm(request, model);
    }

    @RequestMapping(value=CURRENT_PAGE, method = RequestMethod.POST)
    public String postForm(HttpServletRequest request, HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        return super.postForm(request, session, model, redirectAttrs);
    }

    /**
     * Might use BindingResult, and spring Validator, not sure yet
     * don't want to perform type conversion prior to controller as we have no control
     * over the (rather poor) reporting behaviour
     * @return
     */
    protected void validate(Map<String, String[]> fieldValues, String[] fields) {
        LOG.trace("Starting BenefitsController.validate");

        validateMandatoryFields(fieldValues, "Do you pay into a pension?", "payPensionScheme");
        if(fieldValue_Equals(fieldValues, "payPensionScheme", "yes")) {
            validateMandatoryFields(fieldValues, "Give details of each pension you pay into, including how much and how often you pay.", "payPensionSchemeText");
        }

        validateMandatoryFields(fieldValues, "Do you have any care costs because of this work?", "haveExpensesForJob");
        if(fieldValue_Equals(fieldValues, "haveExpensesForJob", "yes")) {
            validateMandatoryFields(fieldValues, "Give details of who you pay and what it costs.", "haveExpensesForJobText");
        }

        LOG.trace("Ending BenefitsController.validate");
    }
}
