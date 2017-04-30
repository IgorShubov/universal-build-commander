package com.ubc.plugin.nativecpp

import org.gradle.api.InvalidUserDataException

class ArtifactPogo {
    String group
    String name
    String version
    String aol // architecture; os; linker/compiler combo
    String ext
    String classifier

/**
 * Return the path of the component, relative to the repository base
 */
    String getURLPath() {
        String groupNoDots = group.replace('.', '/')
        if (aol && !aol.isEmpty())
            return "${groupNoDots}/${name}/${version}/${aol}/${getFileName()}"
        else
            return "${groupNoDots}/${name}/${version}/${getFileName()}"
    }

    String getArtifactName() {
        if (classifier && aol) {
            return "$name-$version-$aol-$classifier.$ext"
        } else if (classifier) {
            return "$name-$version-$classifier.$ext"
        } else if (aol) {
            return "$name-$version-$aol.$ext"
        } else {
            return "$name-$version.$ext"
        }
    }

    String getFileName() {
        if (name == null || version == null || ext == null) {
            throw new InvalidUserDataException("name, version, and ext are mandatory and cannot be null")
        }

        return getArtifactName()
    }


    int hashCode() {
        int result
        result = (group != null ? group.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
        result = 31 * result + (version != null ? version.hashCode() : 0)
        result = 31 * result + (aol != null ? aol.hashCode() : 0)
        result = 31 * result + (ext != null ? ext.hashCode() : 0)
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0)
        return result
    }

}
