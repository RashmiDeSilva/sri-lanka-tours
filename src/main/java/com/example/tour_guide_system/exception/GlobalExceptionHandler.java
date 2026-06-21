package com.example.tour_guide_system.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            MultipartException.class,
            UsernameNotFoundException.class
    })
    public ModelAndView handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorView(
                HttpStatus.BAD_REQUEST,
                "We could not process that request.",
                safeMessage(ex.getMessage(), "Please check your input and try again."),
                request
        );
    }

    @ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class})
    public ModelAndView handleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorView(
                HttpStatus.NOT_FOUND,
                "The requested item was not found.",
                safeMessage(ex.getMessage(), "The page or record you are looking for may have been removed."),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(Exception ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorView(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this page.",
                "Please sign in with an authorized account and try again.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrityViolation(Exception ex, HttpServletRequest request) {
        log.warn("Data integrity issue at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorView(
                HttpStatus.CONFLICT,
                "We could not save that change.",
                "The data conflicts with an existing record or database rule.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return buildErrorView(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side.",
                "Please try again in a moment. If the problem continues, contact support.",
                request
        );
    }

    private ModelAndView buildErrorView(HttpStatus status, String title, String message, HttpServletRequest request) {
        ModelMap model = new ModelMap();
        model.addAttribute("status", status.value());
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("backUrl", request.getHeader("Referer"));
        model.addAttribute("currentYear", java.time.Year.now().getValue());

        ModelAndView mav = new ModelAndView("error/general-error", model);
        mav.setStatus(status);
        return mav;
    }

    private String safeMessage(String actualMessage, String fallback) {
        if (actualMessage == null || actualMessage.trim().isEmpty()) {
            return fallback;
        }
        return actualMessage;
    }
}
