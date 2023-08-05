package com.propagation.User.util;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Slf4j
public class LogAOP {
    @Around("com.propagation.User.util.Pointcuts.userServiceAndUpdateMethod()")
    public Object logAspect(ProceedingJoinPoint joinPoint) throws Throwable {

        try{
            log.info("");
            log.info("**********call {} tx**********", joinPoint.toShortString());
            log.info("[tx start] {}",joinPoint.getSignature());
            log.info("[tx name] = {}", TransactionSynchronizationManager.getCurrentTransactionName());
            log.info("[tx active] = {}", TransactionSynchronizationManager.isActualTransactionActive());
            return joinPoint.proceed();
        }finally {
            log.info("**********parent {} end**********", joinPoint.getSignature());
        }
    }




}
