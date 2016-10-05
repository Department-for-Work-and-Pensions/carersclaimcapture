package uk.gov.dwp.carersallowance.utils;

import org.apache.commons.lang3.StringUtils;

public class KeyValue {
    private String key;
    private String value;

    public KeyValue(String string, String separator) {
        Parameters.validateMandatoryArgs(separator, "separator");
        if(separator.equals("")) {
            throw new IllegalArgumentException("separator cannot be blank");
        }

        if(StringUtils.isBlank(string)) {
            return;
        }

        int pos = string.indexOf(separator);
        if(pos < 0) {
            key = string.trim();
            return;
        }

        key = string.substring(0, pos);
        key = key.trim();
        if(key.equals("")) {
            key = null;
        }
        if(pos < string.length()) {
            value = string.substring(pos + 1);
            value = value.trim();
            if(value.equals("")) {
                value = null;
            }
        }
    }

    public String getKey()   { return key; }
    public String getValue() { return value; }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(this.getClass().getName()).append("@").append(System.identityHashCode(this));
        buffer.append("=[");
        buffer.append("key = ").append(key);
        buffer.append(", value = ").append(value);
        buffer.append("]");

        return buffer.toString();
    }

    public static void main(String[] args) {
        String[] data = {null, "", " ", "=", " = ", "hello=world", " hello = world ", "=world", "hello=", " =world", " hello= ", "hello==world"};
        for(String string: data) {
            KeyValue keyValue = new KeyValue(string, "=");
            System.out.println("string = '" + string + "', key = '" + keyValue.getKey() + "', value = '" + keyValue.getValue() + "'");
        }
    }
}