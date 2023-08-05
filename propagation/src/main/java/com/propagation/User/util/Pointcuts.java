package com.propagation.User.util;

import org.aspectj.lang.annotation.Pointcut;

public class Pointcuts {


    @Pointcut("execution(* com.propagation.User.service..*(..))")
    public void userService(){} //pointcut signature

    //클래스 이름 패턴이 *Service
    @Pointcut("execution(* update*(..)))")
    public void updateMethod(){}



    @Pointcut("userService() && updateMethod()")
    public void userServiceAndUpdateMethod() {}


}
