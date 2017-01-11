package uk.gov.dwp.carersallowance.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.gov.dwp.carersallowance.jsp.Functions;
import uk.gov.dwp.carersallowance.session.IllegalFieldValueException;
import uk.gov.dwp.carersallowance.utils.C3Constants;
import uk.gov.dwp.carersallowance.utils.Parameters;
import uk.gov.dwp.carersallowance.utils.xml.XPathMapping;
import uk.gov.dwp.carersallowance.utils.xml.XPathMappingList;
import uk.gov.dwp.carersallowance.utils.xml.XmlPrettyPrinter;

public class XmlBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(XmlBuilder.class);

    private static final String XML_MAPPING__CLAIM = "xml.mapping.claim";
    private static final String PATH_SEPARATOR = "/";

    private Document document;
    private Map<String, XPathMappingList> valueMappings;
    private final MessageSource messageSource;

    public XmlBuilder(final String rootNodeName, final Map<String, Object> values, final MessageSource messageSource) throws ParserConfigurationException, IOException, XPathMappingList.MappingException {
        this.messageSource = messageSource;
        Parameters.validateMandatoryArgs(new Object[]{rootNodeName}, new String[]{"rootNodeName"});
        Map<String, String> namespaces = getNamespaces();
        document = createDocument(rootNodeName, namespaces);
        this.valueMappings = loadXPathMappings();

        addAssistedDecisionToClaim(values);
        addNodes(values, null, document);
    }

    private Map<String, XPathMappingList> loadXPathMappings() throws IOException, XPathMappingList.MappingException {
        Map<String, XPathMappingList> mappings = new HashMap<>();
        URL claimTemplateUrl = this.getClass().getClassLoader().getResource(XML_MAPPING__CLAIM);
        List<String> xmlMappings = readLines(claimTemplateUrl);
        XPathMappingList valueMappings = new XPathMappingList();
        valueMappings.add(xmlMappings);
        mappings.put(null, valueMappings);
        return mappings;
    }


    private Map<String, String> getNamespaces() {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xmlns", "http://www.govtalk.gov.uk/dwp/carers-allowance");
        namespaces.put("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
        namespaces.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("xsi:schemaLocation", "http://www.govtalk.gov.uk/dwp/carers-allowance file:/future/schema/ca/CarersAllowance_Schema.xsd");
        return namespaces;
    }

    public static List<String> readLines(final URL url) throws IOException {
        if (url == null) {
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            List<String> list = IOUtils.readLines(inputStream, Charset.defaultCharset());
            return list;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private Document createDocument(String rootNodeName, Map<String, String> namespaces) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element rootNode = doc.createElement(rootNodeName);
        doc.appendChild(rootNode);

        if (namespaces != null) {
            for (Map.Entry<String, String> namespace : namespaces.entrySet()) {
                Attr attr = doc.createAttribute(namespace.getKey());
                attr.setValue(namespace.getValue());
                rootNode.setAttributeNode(attr);
            }
        }

        return doc;
    }

    /**
     * Render the contents of "values" as an XML document by iterating over XPathMappingList; which maps a named
     * value (in values) to a Node at a specific XPath location, populating the nodes with the corresponding values
     *
     * @param values
     * @param localRootNode XPath calculations use this as the root location, usually document, but can be different for FieldCollections
     */
    private void addNodes(Map<String, Object> values, String mappingName, Node localRootNode) {
        if (values == null) {
            return;
        }

        XPathMappingList mappingList = valueMappings.get(mappingName);
        if (mappingList == null) {
            throw new IllegalArgumentException("Unknown mapping: " + mappingName);
        }

        List<XPathMapping> list = mappingList.getList();

        // Store all string values by xpath to allow easy lookup when checking if questions need to be added to xml
        Map<String, String> valuesByValueKey = new HashMap<>();
        for (XPathMapping mapping : list) {
            String valueKey = mapping.getValue();
            Object value = getClaimValue(valueKey, values);
            LOG.debug("Build map ... checking valueKey:{} for xpath:{}->{}", valueKey, mapping.getXpath(), value);
            if (StringUtils.isNotBlank(mapping.getXpath()) && isValueEmpty(value) == false && value instanceof String) {
                LOG.debug("Build map ... ADDING valueKey:{}->{}", valueKey, value);
                valuesByValueKey.put(valueKey, (String) value);
            }
        }

        for (XPathMapping mapping : list) {
            String valueKey = mapping.getValue();
            Object value = getClaimValue(valueKey, values);
            String xpath = mapping.getXpath();
            String processingInstruction = mapping.getProcessingInstruction();
            LOG.debug("Checking xpath:{}", xpath);
            if (StringUtils.isNotBlank(xpath) && isValueEmpty(value) == false) {
                if (value instanceof String) {
                    // we may have add attribute without = which is a filter instruction i.e. DWPBody/DWPCATransaction/[@id]
                    if (processingInstruction != null && processingInstruction.length() > 0 && !processingInstruction.contains("=")) {
                        addAttr(xpath, processingInstruction, (String) value, localRootNode);
                    } else {
                        addNode(xpath, (String) value, true, localRootNode);   // create leaf node
                    }
                } else if (value instanceof List) {
                    // field collection, we can't reliably assert the parameterized types, so will go with <?>
                    List<Map<String, String>> fieldCollectionList = castFieldCollectionList(value);
                    add(fieldCollectionList, xpath);
                } else {
                    throw new IllegalFieldValueException("Unsupported value class: " + value.getClass().getName(), (String) null, (String[]) null);
                }
            } else if (valueKey != null && (valueKey.endsWith(".label") || valueKey.endsWith(".text"))) {
                LOG.debug("Checking QuestionLabel:{}", valueKey);
                String question = getQuestion(valueKey, values);
                String relatedAnswerKey = valueKey.replace(".label", "").replace(".text", "");
                // If we have a corresponding Answer and value set for this QuestionLabel then we add to xml
                if (valuesByValueKey.containsKey(relatedAnswerKey)) {
                    LOG.debug("Adding QuestionLabel:{}", xpath);
                    addNode(xpath, question, true, localRootNode);
                }
                // Address carerAddress.label has carerAddressLineOne, carerAddressLineTwo
                else if (valuesByValueKey.containsKey(relatedAnswerKey + "LineOne")){
                    LOG.debug("Adding Address QuestionLabel:{}", xpath);
                    addNode(xpath, question, true, localRootNode);
                }
            }
        }
    }

    private Object getClaimValue(String key, Map<String, Object> values) {
        Object value = values.get(key);
        if (value != null) {
            LOG.debug("GOT REGULAR KEY!!");
            return value;
        } else if (key != null && key.contains(".attribute")) {
            return values.get(key.replace(".attribute", ""));
        } else if (values.containsKey(key + "_day") && values.containsKey(key + "_month") && values.containsKey(key + "_year")) {
            LOG.debug("GOT DATE KEYS!!");
            String dateDay = (String) values.get(key + "_day");
            String dateMonth = (String) values.get(key + "_month");
            String dateYear = (String) values.get(key + "_year");
            String date = dateDay + "-" + dateMonth + "-" + dateYear;
            LOG.debug("RETURNING DATE:{}", date);
            return date;
        } else {
            return null;
        }
    }

    private String getQuestion(String questionKey, final Map<String, Object> values) {
        // TODO throw exception and stop processing if gap in messages. But too many Income / Breaks gaps as of 10/01/2016
        String questionMessage;
        try {
            Object[] parameters = getParameters(questionKey, values);
            questionMessage = messageSource.getMessage(questionKey, parameters, Locale.getDefault());
        } catch (NoSuchMessageException e) {
            LOG.error("NoSuchMessageException thrown looking for message for key:" + questionKey);
            questionMessage = "ERROR " + questionKey + " - message not found";
        } catch (Exception e) {
            LOG.error("Exception thrown looking for message for key:" + questionKey);
            questionMessage = "ERROR " + questionKey + " - exception";
        }
        return (questionMessage);
    }

    private Object[] getParameters(final String questionKey, final Map<String, Object> values) {
        try {
            final String expression = messageSource.getMessage(questionKey + ".args", null, null, Locale.getDefault());
            List<Object> expressions = evaluateExpressions(expression, values);
            return (expression == null) ? null : expressions.toArray();
        } catch (NoSuchMessageException e) {
        } catch (Exception e) {
            LOG.error("Exception thrown looking for message for key:" + questionKey);
        }
        return null;
    }

    /**
     * Adds an attribute to an existing node.
     */
    private void addAttr(String xPath, String name, String value, Node localRootNode) {
        Element node = (Element) getNamedNode(xPath, null, false, localRootNode);
        node.setAttribute(name.replace("@", ""), value);
    }


    /**
     * Add a single node value to the existing document merging in the XPath to the existing structure
     *
     * @param value e.g. DLA
     * @return the newly created node
     */
    private Node addNode(String xPath, String value, boolean createEmptyNodes, Node localRootNode) {
        if (xPath == null || (createEmptyNodes == false && isValueEmpty(value))) {
            return null;
        }

        Node node = getNamedNode(xPath, null, false, localRootNode);
        if (isValueEmpty(value) == false) {
            Node textNode = document.createTextNode(value.replace(C3Constants.YES, "Yes").replace(C3Constants.NO, "No"));
            node.appendChild(textNode);
        }

        return node;
    }

    private void add(List<Map<String, String>> fieldCollectionList, String xPath) {
//        // create enclosing node, one per list item using order attribute, then create the inner values
//        if(fieldCollectionList == null) {
//            return;
//        }
//
//        for(int index = 0; index < fieldCollectionList.size(); index++) {
//            Map<String, String> fieldCollection = fieldCollectionList.get(index);
//            Node collectionNode = getNamedNode(xPath, addToAttrMap(null, "order", Integer.toString(index)), false, document);
//            this.add(fieldCollection, valueMappings, collectionNode);
//            Node textNode = document.createTextNode(value);
//            current.appendChild(textNode);
//        }
        throw new UnsupportedOperationException("Not implemented");
    }

    private List<Map<String, String>> castFieldCollectionList(Object untypedfieldCollection) {
        if ((untypedfieldCollection instanceof List) == false) {
            throw new IllegalArgumentException("field collection list is not a 'List'");
        }
        List<?> list = (List<?>) untypedfieldCollection;
        for (Object item : list) {
            if (item != null && (item instanceof Map<?, ?>) == false) {
                throw new IllegalArgumentException("item in the field collection list is not a 'Map'");
            }
            Map<?, ?> map = (Map<?, ?>) item;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && (entry.getKey() instanceof String) == false) {
                    throw new IllegalArgumentException("key in map instance in field collection is not a String");
                }
                if (entry.getValue() != null && (entry.getValue() instanceof String) == false) {
                    throw new IllegalArgumentException("value in map instance in field collection is not a String");
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> result = (List<Map<String, String>>) list;

        return result;
    }

    private String subpath(String path, int start, int end) {
        if (path == null) {
            return null;
        }

        int startOffset = 0;
        int endOffset = -1;
        int searchStartIndex = 0;

        for (int index = 0; index < end; index++) {
            searchStartIndex = path.indexOf(PATH_SEPARATOR, searchStartIndex);
            if (searchStartIndex < 0) {
                throw new IndexOutOfBoundsException("index = " + index);
            } else {
                if ((index + 1) == start) {
                    startOffset = searchStartIndex;
                }
                if ((index + 1) == end) {
                    endOffset = searchStartIndex;
                    break;
                }
            }
        }

        String substring = path.substring(startOffset, endOffset);
        return substring;
    }

    /**
     * Return a pre-existing child to this specific node that matches the childName and attributes, or if
     * it does not exist: create a new child node if create = true, otherwise return null;
     *
     * @param localRootNode the rootNode used for xPath calculations
     * @return
     */
    private Node getNamedNode(String xPath, Map<String, String> attributes, boolean attrExactMatch, Node localRootNode) {
        //remove Line from path
        String[] pathElements = xPath.split(PATH_SEPARATOR);
        Node current = localRootNode;
        for (int index = 0; index < pathElements.length; index++) {
            String element = pathElements[index];
            Node childNode = getNamedNode(current, element, true, attributes, attrExactMatch);
            if (childNode == null) {
                throw new IllegalStateException("Unable to create node(" + element + ") at: " + subpath(xPath, 0, index));
            }
            current = childNode;
        }

        return current;
    }

    /**
     * Return a pre-existing child to this specific node that matches the childName and attributes, or if
     * it does not exist: create a new child node if create = true, otherwise return null;
     *
     * @return
     */
    private Node getNamedNode(Node node, String childName, boolean create, Map<String, String> attributes, boolean attrExactMatch) {
        if (node == null || childName == null) {
            return null;
        }

        Boolean hasChild = Boolean.FALSE;
        Node child = null;
        NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            child = children.item(index);
            hasChild = Boolean.FALSE;
            String name = child.getNodeName();
            if (childName.equals(name)) {
                if (attributes != null || attrExactMatch) {
                    Map<String, String> childAttrMap = attrsToMap(child.getAttributes());
                    if (attrsMatch(childAttrMap, attributes, attrExactMatch) == false) {
                        break;
                    }
                }
                hasChild = Boolean.TRUE;
                break;
            }
        }

        if ((create && !hasChild) || (create && "Line".equals(childName))) {
            // we can use node.getOwnerDocument also
            Element childNode = document.createElement(childName);
            node.appendChild(childNode);
            if (attributes != null) {
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    childNode.setAttribute(attribute.getKey(), attribute.getValue());
                }
            }
            return childNode;
        }

        return hasChild ? child : null;
    }

    private boolean attrsMatch(Map<String, String> childAttrs, Map<String, String> attributes, boolean attrExactMatch) {
        if (attrExactMatch) {
            return childAttrs.equals(attributes);
        }

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ((childAttrs.containsKey(key) == false)
                    || (Objects.equals(childAttrs.get(key), value) == false)) {
                return false;
            }
        }
        return true;

    }

    private Map<String, String> addToAttrMap(Map<String, String> map, String name, String value) {
        Parameters.validateMandatoryArgs(name, "name");
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(name, value);
        return map;
    }

    private Map<String, String> attrsToMap(NamedNodeMap rawAttrMap) {
        if (rawAttrMap == null) {
            return null;
        }

        Map<String, String> map = new HashMap<>();
        for (int index = 0; index < rawAttrMap.getLength(); index++) {
            Node attr = rawAttrMap.item(index);
            String name = attr.getNodeName();
            String value = attr.getNodeValue();
            map.put(name, value);
        }

        return map;
    }

    private boolean isValueEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            String string = (String) value;
            if (string.trim().equals("")) {
                return true;
            }
        }
        return false;
    }

    public String render() throws InstantiationException {
        return render(true, false);
    }

    public String render(boolean includeXmlDeclaration, boolean prettyPrint) throws InstantiationException {
        String xml = XmlPrettyPrinter.xmlToString(document, prettyPrint, includeXmlDeclaration);
        return xml;
    }

    private void addAssistedDecisionToClaim(Map<String, Object> values) {
        AssistedDecision assistedDecision = new AssistedDecision(65, values);
        values.put("assistedDecisionReason", assistedDecision.getReason());
        values.put("assistedDecisionDecision", assistedDecision.getDecision());
    }

    public String getNodeValue(String nodepath) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String nodevalue = null;
        try {
            nodevalue = xpath.compile(nodepath).evaluate(document);
        } catch (XPathException e) {
            LOG.error("Exception compiling xpath:{}", e.toString(), e);
        }
        return nodevalue;
    }

    public Document getDocument() {
        return document;
    }

    private static final String CADS_TLD_PREFIX_END = "}";
    private static final String CADS_TLD_PREFIX_START = "${cads:";

    private List<String> splitExpressions(final String expressionStr) {
        if (expressionStr == null) {
            return null;
        }

        List<String> results = new ArrayList<>();

        // split into token around ${}
        char[] characters = expressionStr.toCharArray();    // don't trim, it will change the output
        Deque<Integer> startStack = new ArrayDeque<>();
        int end = 0;
        for (int index = 0; index < characters.length; index++) {
            switch(characters[index]) {
                case '$':
                    if ((index + 1) < characters.length && characters[index + 1] == '{') {
                        if (index > end && startStack.isEmpty()) {
                            String subExpression = expressionStr.substring(end, index);
                            results.add(subExpression);
                            end = index;
                        }
                        startStack.push(index);
                        index++;
                    }
                    break;

                case '}':
                    if (startStack.isEmpty() == false) {
                        Integer start = startStack.pop();
                        if (startStack.isEmpty()) {
                            end = index + 1;    // include 'end'
                            String subExpression = expressionStr.substring(start, end);
                            results.add(subExpression);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        if (startStack.isEmpty() == false) {
            Integer firstStart = startStack.getFirst();
            String subExpression = expressionStr.substring(firstStart);
            results.add(subExpression);

        } else if (end < characters.length) {
            String subExpression = expressionStr.substring(end);
            results.add(subExpression);
        }

        return results;
    }


    private List<Object> evaluateExpressions(final String expressionStr, final Map<String, Object> values) {
        final String[] expressions = expressionStr.split("\\|");
        List<Object> parameters = new ArrayList<>();
        for (final String expression : expressions) {
            parameters.add(evaluateExpression(expression, values));
        }
        return parameters;
    }

    private String evaluateExpression(final String expressionStr, final Map<String, Object> values) {
        try {
            if ("${careeFirstName} ${careeSurname}".equals(expressionStr)) {
                return "@dpname";
            }
            if ("${carerFirstName} ${carerSurname}".equals(expressionStr)) {
                return "@yourname";
            }

            List<String> subExpressions = splitExpressions(expressionStr);
            if (subExpressions == null || subExpressions.isEmpty()) {
                return "";
            }

            if (subExpressions.size() > 1) {
                StringBuffer buffer = new StringBuffer();
                for (String subExpression :subExpressions) {
                    String result = evaluateExpression(subExpression, values);
                    buffer.append(result);
                }

                return buffer.toString();
            }

            if (expressionStr != null && expressionStr.startsWith(CADS_TLD_PREFIX_START)) {
                return evaluateCadsExpression(expressionStr, values);
            } else {
                return evaluateSingleExpression(expressionStr, values);
            }
        } catch(RuntimeException e) {
            LOG.error("Unexpected RuntimeException evaluating: " + expressionStr, e);
            throw e;
        }
    }

    private String evaluateSingleExpression(final String expression, final Map<String, Object> values) {
        try {
            Object evaluatedValue;
            if (expression.startsWith("${")) {
                final String prop = StringUtils.substringBefore(StringUtils.substringAfter(expression, "${"), "}");
                evaluatedValue = values.get(prop);
            } else {
                return expression;
            }

            if (evaluatedValue == null) {
                return "";
            }

            String evaluatedExpression;
            if (evaluatedValue instanceof String) {
                evaluatedExpression = (String)evaluatedValue;
            } else {
                evaluatedExpression = evaluatedValue.toString();
            }

            if (evaluatedExpression.equals(expression)) {
                // this results in at least one evaluation more than is needed, but allows for recursive evaluation
                return evaluatedExpression;
            }

            // re-evaluate in case the results contains further expressions to be evaluated
            return evaluateExpression(evaluatedExpression, values);
        } catch (RuntimeException e) {
            LOG.error("unexpected RuntimeException, evaluating single expression: " + expression);
            throw e;
        }
    }

    private String evaluateCadsExpression(final String expression, final Map<String, Object> values) {
        try {
            // e.g ${cads:dateOffset(dateOfClaim_day, dateOfClaim_month, dateOfClaim_year, "d MMMMMMMMMM yyyy", "")}"
            if (StringUtils.isEmpty(expression)) {
                return "";
            }

            if (expression.startsWith(CADS_TLD_PREFIX_START) == false || expression.endsWith(CADS_TLD_PREFIX_END) == false) {
                throw new IllegalArgumentException("expression(" + expression + ") is  not of the expected form: " + CADS_TLD_PREFIX_START + " ... " + CADS_TLD_PREFIX_END);
            }

            // expecting ${cads:fnName(arg, arg, ...)}
            String function = assertNotNull(expression.substring(CADS_TLD_PREFIX_START.length(), expression.length() - CADS_TLD_PREFIX_END.length()), "Unable to locate function"); // fnName(arg, arg, ...)
            String functionName = assertNotNull(StringUtils.substringBefore(function, "("), "unable to locate function name");           // fnName
            String allArguments = assertNotNull(function.substring(functionName.length()), "Unable to locate function brackets");            // (arg, arg, ...)
            String rawArguments = allArguments.substring(1, allArguments.length() - 1);
            String[] arguments = rawArguments.split(",");
            for (int index = 0; index < arguments.length; index++) {
                arguments[index] = arguments[index].trim();
            }

            switch (functionName) {
                case "dateOffset": {
                    if (arguments.length != 5) {
                        throw new IllegalArgumentException("Wrong number of arguments for dateOffset. Expecting dateOffset(String dayField, String monthField, String yearField, String format, String offset)");
                    }
                    String dayField = toString(values.get(arguments[0]));
                    String monthField = toString(values.get(arguments[1]));
                    String yearField = toString(values.get(arguments[2]));
                    String format = stripEnclosingQuotes(arguments[3]);
                    String offset = stripEnclosingQuotes(arguments[4]);

                    return Functions.dateOffset(dayField, monthField, yearField, format, offset);
                }
                case "dateOffsetFromCurrent" : {
                    if (arguments.length != 2) {
                        throw new IllegalArgumentException("Wrong number of arguments for dateOffsetFromCurrent. Expecting dateOffsetFromCurrent(String format, String offset)");
                    }
                    String format = stripEnclosingQuotes(arguments[0]);
                    String offset = stripEnclosingQuotes(arguments[1]);
                    return Functions.dateOffsetFromCurrent(format, offset);
                }
                case "prop" : {
                    if (arguments.length != 1) {
                        throw new IllegalArgumentException("Wrong number of arguments for prop. Expecting prop(String propertyValue)");
                    }
                    String prop = stripEnclosingQuotes(arguments[0]);
                    return evaluateExpression(Functions.prop(prop), values);
                }
                default:
                    throw new IllegalArgumentException("Unknown function: " + functionName);
            }
        } catch(RuntimeException e) {
            LOG.error("Problems evaluating CADS expression: " + expression, e);
            throw e;
        }
    }

    private String stripEnclosingQuotes(final String string) {
        if (string == null) {
            return null;
        }

        if (string.length() < 2) {
            return string;
        }

        // either single or double quotes
        if ((string.charAt(0) == '"' && (string.charAt(string.length() - 1) == '"'))
                || (string.charAt(0) == '\'' && (string.charAt(string.length() - 1) == '\''))) {
            String trimmed = string.substring(1, string.length() - 1);
            return trimmed;
        }
        return string;
    }

    private String assertNotNull(String string, String exceptionMessage) {
        if(string == null) {
            throw new IllegalArgumentException(exceptionMessage);
        }
        return string;
    }

    private String toString(Object value) {
        if(value == null || value instanceof String) {
            return (String)value;
        }
        return value.toString();
    }
}