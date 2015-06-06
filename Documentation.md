# Introduction #


> <i> « They're watching you, Neo. » </i>  <sub>(Trinity)</sub>


From Wikipedia:
<i>
In the field of software development, an interceptor pattern is a software design pattern that is used when software systems or frameworks want to offer a way to change, or augment, their usual processing cycle. [...] Key aspects of the pattern are that the change is transparent and used automatically. In essence, the rest of the systems does not have to know something has been added or changed and can keep working as before.<br>
</i>

A very interesting feature of EJB3 was the opportunity to intercept Session Beans method calls through a non-invasive language mechanism: Annotations.

In Java SE, although the user can create his/her own annotations and use them with built-in reflection facilities, there is no way to emulate the interceptor model introduced in J2EE. But, behind the scene, J2EE is built on top of Java SE, so where's the magic?
So, no magic at all. When a client invokes a Session bean's method, the EJB container is the <i>true</i> server that's listening clients. So, before instantiating (or reusing) an instance of a Bean, it checks for "@Interceptors" annotations, and when it finds some, invokes the interceptor method (marked with AroundInvoke annotation, in the target interceptor class).

Some time ago, a colleague asked me if a similar service could be used (or re-created) for POJOs' methods. My answer at that time was just NO. Because JVM should have a built-in runtime annotation checking for this purpose: it would be just too expensive in terms of execution time (JVM should check for ALL method calls for ALL objects).

Here the story changes. For other reasons, some days ago I was wandering on the net looking for information about "self-modifying" programs (genetic programming, etc.). I was just curious, this things look really Sci-Fi. So, yes, the idea I present here derives from self-modifying code. I should say self-modifying <i>byte</i>code.

# How it works #

The user puts "@InterceptedBy" annotation on the method he wants to intercept. As annotation argument, a class that implements MethodInterceptor interface must be specified. This class will be instantiated "on the fly" when the annotated method is called, and its "methodIntercepted" method is called before the execution of the target method.

To make this happen, here comes self-modifying code. A custom class loader (InterceptorClassLoader) has to load the user's class that contains the method to be intercepted. This class loader inspects the user's code and looks for "InterceptedBy" annotation. When the annotation is found, the bytecode of the target method gets modified: the call to "methodIntercepted" becomes the first actual instruction of the target method, in a completely trasparent way. At this point the modified class is loaded, and at each call to the intercepted method, the interceptor gets invoked first.

There are two ways to ensure correct classloading hierarchy during program execution:

  1. specify "-Djava.system.class.loader=it.pcan.java.interceptor.InterceptorClassLoader" property as JVM parameter (the simpler way).
  1. manually loading classes that contain methods to be intercepted, by using InterceptorClassLoader.

# Example #
```
//Target class & method

package example;
public class MyPojo {

	//fields....

	@InterceptedBy(MyInterceptor.class)
	public int methodToBeIntercepted(String argument1, Object argument2, float argument3) {
	
		int value=0; //first instruction
		
		//code...
	
		return value;
	}

}


//Interceptor class

package example;
public class MyInterceptor implements MethodInterceptor {

    @Override
    public void methodInvoked(Object object, String className, String methodName, Object[] params) throws InvocationAbortedException {
        System.out.println("Invoked " + methodName + " on object " + object + " of class " + className + " with " + params.length + " parameters.");
    }
}
```


When the code is compiled and the JVM is launched with "-Djava.system.class.loader=it.pcan.java.interceptor.InterceptorClassLoader" parameter, the classloader finds "@InterceptedBy(MyInterceptor.class)" annotation, and the "methodToBeIntercepted" code becomes as follow (if you try decompilation of the modified class, under the hood, this is the <u>actual</u> result):
```
public class MyPojo {

	//fields....

	@InterceptedBy(MyInterceptor.class)
	public int methodToBeIntercepted(String argument1, Object argument2, float argument3) {
		(new MyInterceptor()).methodInvoked(this, "example.MyPojo", "methodToBeIntercepted", new Object[] {
        	    argument1, argument2, Float.valueOf(argument3)
        	});
		int value=0; //first instruction
		
		//code...
	
		return value;
	}

}
```

Fantastic, isn't it? :)

Note: if "methodToBeIntercepted" was static, the resulting generated code would have been the following:

```
	(new MyInterceptor()).methodInvoked(null, "example.MyPojo", "methodToBeIntercepted", new Object[] {
            argument1, argument2, Float.valueOf(argument3)
        });
```

This is the reason why "methodInvoked" gets <i>intercepted object</i> and <i>intercepted class name</i> as two separate arguments.

# Implementation & release notes #

If you look at the source code, you should note that it's a bit "C-style". It is intentionally written in this way, becouse it must be <i>fast</i>. Many things could be done differently, i know, but this is my own idea, please don't complain about my code style (actually, it's usually different from this).

You should note that I didn't use any bytecode modification library like Apache BCEL. I decided to write a minimal library, with only the code to do the job. On the other hand, Java bytecode specification is very very wide, and a full-featured library like BCEL would have been like taking a hammer to crack a nut. It's just too <i>big</i>.

DISCLAIMER: Feel free to use my code in your own projects, but do it at your own risk. No guarantee is given about it.
This library is released under the Apache License v2.0.