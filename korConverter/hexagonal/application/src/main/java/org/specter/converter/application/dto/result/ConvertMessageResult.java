package org.specter.converter.application.dto.result;

public record ConvertMessageResult(String originalMessage, String convertedMessage, boolean converted) {}
