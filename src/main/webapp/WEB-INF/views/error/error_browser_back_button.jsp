<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html>

<t:mainPage pageTitle="unrecoverable.error">
    <div class="carers-allowance clearfix">
        <div class="grid-row main-grid">
            <article class="column-three-quarters main-content error-statement">
                <h1 class="form-title heading-large"><t:message code="session.timeout" /></h1>
                
                <p><t:message code="unrecoverable.error.cookie.help2" /></p>
                <p><t:message code="unrecoverable.error.cookie.help3" /></p>

                <nav class="form-steps">
                    <ul>
                        <li><button type="submit" name="action" value="next" class="button" @views.html.ga.trackClickEvent(linkName = "Chose try again") onClick="location.href='@url'" aria-label="<t:message code="restart.claim.help" />" ><t:message code="restart.claim" /></button></li>
                        <li>@common.feedbackLink(isFooterItem = false) <t:message code="feedback.takes30secs" /></li>
                    </ul>
                </nav>
            </article>
        </div>
    </div>
</t:mainPage>
