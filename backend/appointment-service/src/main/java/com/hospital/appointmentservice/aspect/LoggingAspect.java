package com.hospital.appointmentservice.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Aspect
@Component
public class LoggingAspect {



    @Pointcut("execution(public * com.hospital.appointmentservice.controller.AppointmentController.*(..))")
    public void controllerMethods() {}

    @Pointcut("execution(public * com.hospital.appointmentservice.service.AppointmentService.*(..))")
    public void serviceMethods() {}

    @Pointcut("execution(public * com.hospital.appointmentservice.exception.GlobalExceptionHandler.*(..))")
    public void exceptionHandlerMethods() {}



    @Before("controllerMethods()")
    public void logControllerRequest(JoinPoint joinPoint) {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        logger.info("→ REQUEST  | method={} | args={}",
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }


    @AfterReturning(pointcut = "controllerMethods()", returning = "returnValue")
    public void logControllerResponse(JoinPoint joinPoint, Object returnValue) {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        logger.info("← RESPONSE | method={} | result={}",
                joinPoint.getSignature().getName(),
                returnValue);
    }


    @Around("serviceMethods()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String methodName = joinPoint.getSignature().getName();

        logger.info("► SERVICE ENTER | method={} | args={}",
                methodName, Arrays.toString(joinPoint.getArgs()));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            logger.info("◄ SERVICE EXIT  | method={} | executionTime={}ms | result={}",
                    methodName, elapsed, result);
            return result;

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;

            boolean isBusinessException =
                    ex instanceof com.hospital.appointmentservice.exception.BusinessValidationException
                            || ex instanceof com.hospital.appointmentservice.exception.ResourceNotFoundException;

            if (isBusinessException) {
                logger.warn("◄ SERVICE EXCEPTION | method={} | executionTime={}ms | exception={} | message={}",
                        methodName, elapsed, ex.getClass().getSimpleName(), ex.getMessage());
            } else {
                logger.error("◄ SERVICE EXCEPTION | method={} | executionTime={}ms | exception={} | message={}",
                        methodName, elapsed, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            }
            throw ex;
        }
    }


    @AfterReturning(pointcut = "exceptionHandlerMethods()", returning = "returnValue")
    public void logExceptionHandlerInvocation(JoinPoint joinPoint, Object returnValue) {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        logger.info("EXCEPTION HANDLED | handler={} | response={}",
                joinPoint.getSignature().getName(),
                returnValue);
    }
}