package com.framework.retry;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * RetryListener
 * =============
 * Automatically applies RetryAnalyzer to ALL test methods in the suite.
 *
 * WHY DO WE NEED THIS?
 * --------------------
 * Without this listener, you would have to add:
 *   @Test(retryAnalyzer = RetryAnalyzer.class)
 * to EVERY single test method.
 *
 * With this listener registered in testng.xml, RetryAnalyzer is automatically
 * attached to every @Test method — no per-test annotation needed.
 *
 * HOW IT WORKS:
 * -------------
 * TestNG calls transform() for every @Test annotation it finds.
 * We use this hook to inject RetryAnalyzer into each annotation's
 * retryAnalyzer attribute.
 *
 * REGISTRATION IN testng.xml:
 * ---------------------------
 *   <listeners>
 *       <listener class-name="com.framework.retry.RetryListener"/>
 *   </listeners>
 */
public class RetryListener implements IAnnotationTransformer {

    /**
     * Called by TestNG for every @Test annotation in the suite.
     * We inject RetryAnalyzer into each test's annotation here.
     *
     * @param annotation    the @Test annotation being processed
     * @param testClass     the class containing the test (may be null)
     * @param testConstructor the constructor (may be null)
     * @param testMethod    the @Test method (may be null)
     */
    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        // Set RetryAnalyzer for every @Test method automatically
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
