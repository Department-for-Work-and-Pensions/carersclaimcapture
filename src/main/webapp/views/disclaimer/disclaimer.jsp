<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<!-- <c:out value="${errors}"/> -->

<t:mainPage>
    <article class="column-three-quarters main-content">
        <nav class="back-nav" role="navigation">
            <a id="topBackButton" name="topBackButton" href="<c:url value='${previousPage}' />">Back</a>
        </nav>

        <h1 class="form-title heading-large">Before you start</h1>

        <form action="<c:url value='${currentPage}' />" method="POST" role="form" style="margin-bottom: 30px; margin-top: -30px;">    
            <jsp:include page="../validationSummary.jsp" />
            <input type="hidden" name="csrfToken" value="ac56313b00f29fa2952b1e80"/>

            <fieldset class="form-elements">
                <ul>
                    <t:hmtlsection value='<p>When you claim Carer&rsquo;s Allowance the person you care for may stop getting:</p>'/> 

                    <t:hmtlsection value='<ul class="list-bullet">
                        <li>a severe disability premium paid with their benefits, if they get one</li>
                        <li>an extra amount for severe disability paid with Pension Credit, if they get one</li>
                        <li>reduced Council Tax, if 
                            <a rel="external" 
                               href="https://www.gov.uk/find-your-local-council" 
                               target="_blank" 
                            >their local council</a> 
                            offers it
                        </li>
                    </ul>' />

                    <t:hmtlsection value='<p>Read more about 
                        <a rel="external" 
                           href="https://www.gov.uk/carers-allowance/what-youll-get" 
                           target="_blank"
                        >how Carer&rsquo;s Allowance affects other benefits</a>.
                    </p>' />
                    
                    <t:hmtlsection value='<p>You <b>must</b> tell the person you care for before you claim.</p>' />
                </ul>
            </fieldset>

            <nav class="form-steps" role="navigation">
                <ul>
                    <li>
                        <button type="submit" id="next" name="read" value="yes" class="button">I agree - start my application</button>
                    </li>
                </ul>
            </nav>
        </form>
        
        <nav class="back-nav" role="navigation">
            <a id="bottomBackButton" name="bottomBackButton" href="<c:url value='${previousPage}' />">Back</a>
        </nav>

        <script type="text/javascript">
            $(document).ready(function(){
                $("#save").click(function(){
                    var saveurl=$(this).attr("href");
                    var saveurl=$(this).attr("href");
                    $("form").attr( "action", saveurl );
                    $("form").submit()
                });
            });
        </script>
    </article>
</t:mainPage>                