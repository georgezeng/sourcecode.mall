package com.sourcecode.malls.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
	Logger logger = LoggerFactory.getLogger(getClass());

    //https://jira.spring.io/browse/SPR-14651
    //Spring 4.3.5 supports RedirectAttributes
    @ExceptionHandler(Exception.class)
    public void handleError1(Exception e) {

    	logger.error(e.getMessage(), e);

    }

    /* Spring < 4.3.5
	@ExceptionHandler(MultipartException.class)
    public String handleError2(MultipartException e) {

        return "redirect:/errorPage";

    }*/

}