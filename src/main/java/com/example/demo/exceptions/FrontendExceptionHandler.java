package com.example.demo.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class FrontendExceptionHandler {

    @ExceptionHandler(HttpStatusCodeException.class)
    public ModelAndView handleBackendApiError(HttpStatusCodeException ex) {

        ModelAndView mv = new ModelAndView("error");

        HttpStatusCode status = ex.getStatusCode();

        String message = "Something went wrong";

        String body = ex.getResponseBodyAsString();

        if (body != null && body.contains("A user with this email already exists")) {
            message = "A user with this email already exists";
        } else if (body != null && body.contains("Invalid linked record")) {
            message = "Invalid linked record selected";
        } else if (body != null && body.contains("Requested resource not found")) {
            message = "Requested data was not found";
        }

        mv.addObject("status", status.value());
        mv.addObject("message", message);

        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneralFrontendError(Exception ex) {

        ModelAndView mv = new ModelAndView("error");
        mv.addObject("status", 500);
        mv.addObject("message", "Something went wrong. Please try again.");

        return mv;
    }
}