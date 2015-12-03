package com.ai.cloud.skywalking.protocol;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Span extends SpanData {

    private Logger logger = Logger.getLogger(Span.class.getName());

    public Span() {
    }

    public Span(String traceId, String applicationID) {
        this.traceId = traceId;
        this.applicationId = applicationID;
    }

    public Span(String traceId, String parentLevelId, int levelId, String applicationID) {
        this.traceId = traceId;
        this.applicationId = applicationID;
        this.parentLevel = parentLevelId;
        this.levelId = levelId;
    }

    public Span(String originData) {
        String[] fieldValues = originData.split(SPAN_FIELD_SPILT_PATTERN);
        traceId = fieldValues[0].trim();
        parentLevel = fieldValues[1].trim();
        levelId = Integer.valueOf(fieldValues[2]);
        viewPointId = fieldValues[3].trim();
        startDate = Long.valueOf(fieldValues[4]);
        cost = Long.parseLong(fieldValues[5]);
        address = fieldValues[6].trim();
        statusCode = Byte.valueOf(fieldValues[7].trim());
        //异常情况才会存在exceptionStack
        if (statusCode == 1) {
            exceptionStack = fieldValues[8].trim().replaceAll(SPAN_ATTR_SPILT_CHARACTER,
                    NEW_LINE_CHARACTER_PATTERN);
        }
        spanType = fieldValues[9].charAt(0);
        isReceiver = Boolean.valueOf(fieldValues[10]);

        businessKey = fieldValues[11].trim().replaceAll(SPAN_ATTR_SPILT_CHARACTER,
                NEW_LINE_CHARACTER_PATTERN);
        processNo = fieldValues[12].trim();
        applicationId = fieldValues[13].trim();
        this.originData = originData;
    }


    @Override
    public String toString() {
        StringBuilder toStringValue = new StringBuilder();
        toStringValue.append(traceId + SPAN_FIELD_SPILT_PATTERN);

        if (isNonBlank(parentLevel)) {
            toStringValue.append(parentLevel + SPAN_FIELD_SPILT_PATTERN);
        } else {
            toStringValue.append(" " + SPAN_FIELD_SPILT_PATTERN);
        }

        toStringValue.append(levelId + SPAN_FIELD_SPILT_PATTERN);

        if (isNonBlank(viewPointId)) {
            toStringValue.append(viewPointId + SPAN_FIELD_SPILT_PATTERN);
        } else {
            toStringValue.append(" " + SPAN_FIELD_SPILT_PATTERN);
        }

        toStringValue.append(startDate + SPAN_FIELD_SPILT_PATTERN);
        toStringValue.append(cost + SPAN_FIELD_SPILT_PATTERN);

        if (isNonBlank(address)) {
            toStringValue.append(address + SPAN_FIELD_SPILT_PATTERN);
        } else {
            toStringValue.append(" " + SPAN_FIELD_SPILT_PATTERN);
        }

        toStringValue.append(statusCode + SPAN_FIELD_SPILT_PATTERN);

        if (isNonBlank(exceptionStack)) {
            //换行符在各个系统中表现不一致，
            //windows平台的换行符为/r/n
            //linux平台的换行符为/n
            toStringValue.append(exceptionStack.replaceAll(CARRIAGE_RETURN_CHARACTER_PATTERN, "")
                    .replaceAll(NEW_LINE_CHARACTER_PATTERN, SPAN_ATTR_SPILT_CHARACTER)
                    + SPAN_FIELD_SPILT_PATTERN);
        } else {
            toStringValue.append(" " + SPAN_FIELD_SPILT_PATTERN);
        }

        toStringValue.append(spanType + SPAN_FIELD_SPILT_PATTERN);
        toStringValue.append(isReceiver + SPAN_FIELD_SPILT_PATTERN);


        if (isNonBlank(businessKey)) {
            //换行符在各个系统中表现不一致，
            //windows平台的换行符为/r/n
            //linux平台的换行符为/n
            toStringValue.append(businessKey.replaceAll(CARRIAGE_RETURN_CHARACTER_PATTERN, "")
                    .replaceAll(NEW_LINE_CHARACTER_PATTERN, SPAN_ATTR_SPILT_CHARACTER)
                    + SPAN_FIELD_SPILT_PATTERN);
        } else {
            toStringValue.append(" " + SPAN_FIELD_SPILT_PATTERN);
        }

        if (isNonBlank(processNo)) {
            toStringValue.append(processNo + SPAN_FIELD_SPILT_PATTERN);
        } else {
            toStringValue.append(" " + SPAN_FIELD_SPILT_PATTERN);
        }

        if (isNonBlank(applicationId)) {
            toStringValue.append(applicationId);
        } else {
            toStringValue.append(" " + SPAN_FIELD_SPILT_PATTERN);
        }

        return toStringValue.toString();
    }

    protected boolean isNonBlank(String str) {
        return str != null && str.length() > 0;
    }

    public void handleException(Throwable e, int maxExceptionStackLength) {
        this.statusCode = 1;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        StringBuilder expMessage = new StringBuilder();
        Throwable causeException = e;
        while (causeException != null && (causeException.getCause() != null || expMessage.length() < maxExceptionStackLength)) {
            causeException.printStackTrace(new java.io.PrintWriter(buf, true));
            expMessage.append(buf.toString());
            causeException = causeException.getCause();
        }
        try {
            buf.close();
        } catch (IOException e1) {
            logger.log(Level.ALL, "Close exception stack input stream failed");
        }
        this.exceptionStack = expMessage.toString();
    }

}