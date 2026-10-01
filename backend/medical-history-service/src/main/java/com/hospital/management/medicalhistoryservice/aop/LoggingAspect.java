package com.hospital.management.medicalhistoryservice.aop;
import com.hospital.management.medicalhistoryservice.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect{

    @Around("execution(* com.hospital.management.medicalhistoryservice.service.MedicalHistoryService.*(..))")
    public Object logServiceOperations(ProceedingJoinPoint joinPoint) throws Throwable{
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();

        log.debug("Attempting to invoke {}() with arguments {}",methodName,Arrays.toString(args));

        try{
            Object result = joinPoint.proceed();
            log.info("Successfully executed {}()", methodName);
            return result;
        } catch (ResourceNotFoundException e){
            log.warn(e.getMessage());
            throw e;
        } catch (Exception e){
            log.error("Unexpected error occured");
            throw e;
        }
    }
    @Around("execution(* com.hospital.management.medicalhistoryservice.controller.MedicalHistoryController.*(..))")
    public Object logControllerOperations(ProceedingJoinPoint joinPoint) throws Throwable{
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("Controller received request for method {}",methodName);
        log.debug("Controller received arguments {}",Arrays.toString(args));
        try{
            Object result = joinPoint.proceed();
            log.info("CONTROLLER: Request successfully processed by {}()", methodName);
            return result;
        } catch (Exception e) {
            log.error("CONTROLLER: Request failed in {}() message : {}", methodName, e.getMessage());
            throw e;
        }
    }
}