package com.ting.ting.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User is not founded"),
    DUPLICATED_USER_REQUEST(HttpStatus.BAD_REQUEST, "It's a request between the same users."),
    DUPLICATED_REQUEST(HttpStatus.BAD_REQUEST, "Request information that already exists."),
    REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "Request information already processed"),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Request information is not founded"),
    GENDER_NOT_MATCH(HttpStatus.FORBIDDEN, "gender values do not match"),
    INVALID_PERMISSION(HttpStatus.UNAUTHORIZED, "Permission is invalid"),
    REACHED_MEMBERS_SIZE_LIMIT(HttpStatus.CONFLICT, "Maximum group capacity of the number of members reached"),
    LIMIT_NUMBER_OF_REQUEST(HttpStatus.CONFLICT, "The maximum number of requests(5) has been exceeded."),
    LIMIT_NUMBER_OF_BlIND_DATE(HttpStatus.CONFLICT, "The maximum number of blindDate(3) has been exceeded."),
    NO_AVAILABLE_MEMBER_AS_LEADER(HttpStatus.NOT_FOUND, "There is no available member as a leader"),
    ALREADY_JOINED(HttpStatus.CONFLICT, "User is already in the group"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Request doesn't meet the requirements"),
    REQUEST_NOT_MINE(HttpStatus.BAD_REQUEST,"This is not the request information that came to me"),
    NOT_MY_REQUEST(HttpStatus.BAD_REQUEST,"This is not a request I sent."),
    QR_GENERATOR_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred in the QR generator"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "Username already exists."),
    LIMIT_USERNAME_LENGTH(HttpStatus.CONFLICT, "Username's minimum length is 4"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Email already exists."),
    DUPLICATE_SOCIAL_EMAIL(HttpStatus.CONFLICT, "Social Email already exists."),
    TOKEN_ERROR(HttpStatus.BAD_REQUEST, "Token generation error"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Access Token is invalid"),
    INVALID_IDEAL_PHOTO(HttpStatus.BAD_REQUEST, "ideal photo is invalid"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AuthenticationException occurred"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getHttpStatus() {
        return httpStatus.value();
    }
}
