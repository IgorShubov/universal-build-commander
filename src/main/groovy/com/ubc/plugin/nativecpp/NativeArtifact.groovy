package com.ubc.plugin.nativecpp

import org.gradle.api.artifacts.ArtifactIdentifier
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier

class NativeArtifact extends ArtifactPogo implements ArtifactIdentifier{

    String targetDir

    @Override
    String getName() {
        return super.name
    }

    @Override
    String getType() {
        return ext
    }

    @Override
    String getClassifier() {
        return super.classifier
    }

    @Override
    String getExtension() {
        return ext
    }

    @Override
    ModuleVersionIdentifier getModuleVersionIdentifier() {

        return new MVI(group: super.group, name: super.name, version: super.version)
    }

    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (targetDir != null ? targetDir.hashCode() : 0)
        return result
    }

    public class MVI implements ModuleVersionIdentifier {
        String group;
        String name;
        String version;

        @Override
        String getGroup() { return this.group }

        @Override
        String getName() { return this.name }

        @Override
        String getVersion() { return this.version }

        ModuleIdentifier getModule() {
            return new ModuleIdentifier() {
                @Override
                String getGroup() { return this.group }
                @Override
                String getName() { return this.name }
            }
        }
    }
}
