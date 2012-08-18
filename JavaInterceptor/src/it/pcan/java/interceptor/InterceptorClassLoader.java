/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.pcan.java.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pierantonio
 */
public class InterceptorClassLoader extends ClassLoader {

    public InterceptorClassLoader() {
    }

    public InterceptorClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        Class c = fixupLoading(name);
        if (c == null) {
            c = super.loadClass(name);
        }

        return c;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        Class c = fixupLoading(name);
        if (c == null) {
            c = super.loadClass(name, resolve);
        }
        return c;
    }

    private Class fixupLoading(String name) {


        if (!name.startsWith("java.") && !name.startsWith("sun.") && !name.startsWith("it.pcan.")) {
            System.out.println("Loading class " + name);
            InputStream resourceAsStream = getResourceAsStream(name.replace(".", "/") + ".class");

            try {

                InterceptorCodeInjector injector = new InterceptorCodeInjector(resourceAsStream, name);
                byte[] injectedCode = injector.inject();
                return defineClass(name, injectedCode, 0, injectedCode.length);

            } catch (IOException ex) {
                Logger.getLogger(InterceptorClassLoader.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return null;

    }
}
