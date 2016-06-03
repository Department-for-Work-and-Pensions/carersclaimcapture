<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<t:mainPage pageTitle="${pageTitle}" currentPage="${currentPage}">

    <t:pageContent errors="${validationErrors}" pageTitle="About you - the carer" section="Section 3 of 11" backLink="${previousPage}">
    
        <t:textedit id="title" name="title" value="${title}" maxLength="20" label="Title" errors="${validationErrors}" /> 
        <t:textedit id="firstName" name="firstName" value="${firstName}" maxLength="17" label="First name" errors="${validationErrors}" />
        <t:textedit id="middleName" name="middleName" value="${middleName}" maxLength="17" label="Middle name(s)" errors="${validationErrors}" />
        <t:textedit id="surname" name="surname" value="${surname}" maxLength="35" label="Last name" errors="${validationErrors}" />
        
        <!-- We accept a possible space around each character with 9 max in nino ie. AB010203D so 9*2+1 ==> 19 chars max -->
        <t:textedit id="nationalInsuranceNumber" 
                    name="nationalInsuranceNumber" 
                    value="${nationalInsuranceNumber}" 
                    maxLength="19" 
                    label="National Insurance number" 
                    errors="${validationErrors}" 
                    additionalClasses="ni-number"
                    hintBefore='<p class="form-hint">For example, VO123456D</p>'
                    hintAfter='<p class="form-hint">This is on your NI number card, benefit letter, payslip or P60.</p>'
        />
        
        <t:datefield id="dateOfBirth" 
                     nameDay="dateOfBirth_day" 
                     nameMonth="dateOfBirth_month" 
                     nameYear="dateOfBirth_year" 
                     valueDay="${dateOfBirth_day}" 
                     valueMonth="${dateOfBirth_month}" 
                     valueYear="${dateOfBirth_year}" 
                     label="Date of birth" 
                     errors="${validationErrors}" 
                     hintBefore='<p class="form-hint" id="dateOfBirth_defaultDateContextualHelp">For example, 16 5 1976</p>'
        />

    </t:pageContent>
    
    <script type="text/javascript">
        <!-- i think this might relate to save for later -->
        $(document).ready(function(){
            $("#save").click(function(){
                var saveurl=$(this).attr("href");
                var saveurl=$(this).attr("href");
                $("form").attr( "action", saveurl );
                $("form").submit()
            });
        });
    </script>

    <script type="text/javascript">
        $ ( function ( ) {
            GOVUK.performance.stageprompt.setupForGoogleAnalytics();
            ;
        } )
    </script>

</t:mainPage>    
