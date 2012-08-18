/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.pcan.java.interceptor;

import it.pcan.java.interceptor.excpetions.InvocationAbortedException;

/**
 *
 * @author Pierantonio
 */
public interface MethodInterceptor {

    public void methodInvoked(Object object, String className, String methodName, Object[] params) throws InvocationAbortedException;

}
