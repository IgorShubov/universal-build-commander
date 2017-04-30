package com.ubc.plugin.nativecpp

import org.gradle.api.InvalidUserDataException
import org.junit.Test

import static org.junit.Assert.*

class NativeDependencyTest {
    @Test
    void ctor() {
        def dep = new NativeDependency(name: "test", group: "org.test", aol: "x86-gcc4", version: "1.5", ext: "so")

        assertEquals "test", dep.getName()
        assertEquals "org.test", dep.getGroup()
        assertEquals "x86-gcc4", dep.getAol()
        assertEquals "1.5", dep.getVersion()
        assertEquals "so", dep.getExt()
        assertNull dep.getClassifier()
    }

    @Test(expected = InvalidUserDataException.class)
    void ctorFail() {
        def dep = new NativeDependency(name: "test", group: "org.test", aol: "x86-gcc4", version: "1.5")
        dep.getFileName()
    }

    @Test
    void getFileName() {
        def dep = new NativeDependency(name: "test", ext: "zip", aol: "x86-gcc4", version: "1.5")
        assertEquals "test-1.5-x86-gcc4.zip", dep.getFileName()
        dep.setClassifier("sources")
        assertEquals "test-1.5-x86-gcc4-sources.zip", dep.getFileName()
    }

    @Test
    void copyAndEquals() {
        def dep = new NativeDependency(name: "test", group: "org.test", aol: "x86-gcc4", version: "1.5")
        def copy = dep.copy()
        assertTrue(copy.contentEquals(dep))
    }

}
