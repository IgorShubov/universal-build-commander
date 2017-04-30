package com.ubc.plugin.nativecpp

import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Dependency

class NativeDependency extends ArtifactPogo implements Dependency {

    @Override
    String getGroup() {
        if (Object.group == null) {
            throw new InvalidUserDataException("group is mandatory and cannot be null")
        }
        return Object.group
    }

    String installDir

    Map asIvyDependencyMap() {
        def map = [:]
        map.put('org', group)
        map.put('name', name)
        map.put('rev', version)
        if (aol && !aol.empty) {
            map.put('aol', aol)
        }
        def conf = ""
        if (aol && !aol.empty)
            conf += aol
        if (classifier && !classifier.empty) {
            if (conf.empty) {
                conf += classifier
            } else {
                conf += "-$classifier"
            }
        }
        if (!conf.empty)
            map.put('conf', conf)

        return map
    }

    @Override
    boolean contentEquals(Dependency dependency) {
        if (this.is(dependency)) return true
        if (getClass() != dependency.class) return false

        NativeDependency that = (NativeDependency) dependency

        if (aol != that.aol) return false
        if (group != that.group) return false
        if (name != that.name) return false
        if (version != that.version) return false

        return true
    }

    @Override
    Dependency copy() {
        return new NativeDependency(group: group, name: name, version: version, aol: aol, ext: ext, installDir: installDir)
    }

}