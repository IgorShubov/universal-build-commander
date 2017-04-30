package com.ubc

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class BuildXmlParser {

    def static factory

    static void initSchema() {
        if (factory == null) {
            factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        }
    }

    /**
     * Parses the test-build.xml build configuration file
     *
     * @param path  the absolute path to the test-build.xml file
     * @return      the map of build properties
     */
    private Map parseBuildXmlFile(String buildXmlPath) {
        def externalDependencyMap = { node ->
            def externalDependency = [:]
            if (node.name() == 'dependency') {
                externalDependency.put(node.name(), node.text())
            } else {
                def dependencyDeclarations = node.childNodes()
                dependencyDeclarations.each {
                    externalDependency.put(it.name(), it.text())
                }
            }
            return externalDependency
        }

        def publicationMap = { node ->
            def publicationDeclarations = node.childNodes()
            def publication = [:]
            publicationDeclarations.each {
                publication.put(it.name(), it.text())
            }
            return publication
        }

        def root, elementToMap = { element ->
            root[element].children().collectEntries {
                if (it.name() == 'args' || it.name() == 'systemProperties' || it.name() == 'projectProperties')
                    [it.name(), it.childNodes().collect { "\"${it.text()}\"" }]
                else if (it.name() == 'tasks')
                    [it.name(), it.childNodes().collect { "${it.text()}" }]
                else if (it.name() == 'versionUpdateExcludes' || it.name() == 'targets' || it.name() == 'releaseTargets')
                    [it.name(), it.childNodes().collect { it.text() }]
                else if (it.name() == 'variables')
                    [it.name(), it.childNodes().collectEntries {
                        [it.text().tokenize('=').get(0), it.text().tokenize('=').get(1)]
                    }]
                else if (it.name() == 'parameters' || it.name() == 'releaseParameters')
                    [it.name(), it.childNodes().collectEntries {
                        [it.text().tokenize('=').get(0), it.text().tokenize('=').get(1)]
                    }]
                else if (it.name() == 'toolchains')
                    [it.name(), it.childNodes().collectEntries {
                        [it.children().get(0).text(), it.children().get(1).text()]
                    }]
                else if (it.name() == 'cmake' || it.name() == 'visualCpp') {
                    [it.name(), it.childNodes().collectEntries { child ->
                        [child.name(), child.text()]
                    }]
                } else if (it.name() == "nativeConfiguration") {
                    [it.name(), it.childNodes().collectEntries { child ->
                        if (child.name() == "externalDependencies") {
                            [child.name(), child.childNodes().collect{ depNode ->
                                externalDependencyMap(depNode)
                            }]
                        } else if (child.name() == "publication") {
                            [child.name(), publicationMap(child)]
                        } else {
                            [child.name(), child.text()]
                        }
                    }]
                } else {
                    [it.name(), it.text()]
                }
            }
        }

        try {
            root = new XmlSlurper().parse(buildXmlPath)
        }
        catch (Exception e) {
            println("Couldn't find an test-build file at $buildXmlPath: ${e.getMessage()}")
            return [:]
        }

        Map<String, Map<String, String>> props = [:]

        if (root.maven) {
            props += ['Maven' : [:]]
            props['Maven'] += elementToMap('maven')
        }

        if (root.msbuild) {
            props += ['MSBuild' : [:]]
            props['MSBuild'] += elementToMap('msbuild')
        }

        if (root.gradle) {
            props += ['Gradle' : [:]]
            props['Gradle'] += elementToMap('gradle')
        }

        if (root.native) {
            props += ['Native' : [:]]
            props['Native'] += elementToMap('native')
        }

        if(root.analysis) {
            props += ['analysis' : [:]]
            props['analysis'] += elementToMap('analysis')
        }

        props += ['environment' : [:]]
        props['environment'] += elementToMap('environment')

        return props
    }

    Map getEnvironmentProperties(String buildFilePath) {

        parseBuildXmlFile(buildFilePath)['environment']
    }

    Map getBuildProperties(String buildFilePath, BuildToolDescriptor desc) {

        Map declaredProperties = parseBuildXmlFile(buildFilePath)
        def buildProperties = [:]
        buildProperties += declaredProperties['environment']
        buildProperties += declaredProperties['analysis']

        if (buildProperties.containsKey('buildTool')) {
            def overrideBuildTool = BuildToolDescriptor.valueOf(buildProperties['buildTool'].toUpperCase())
            desc = overrideBuildTool
        }

        def buildPropertiesKey = (desc == BuildToolDescriptor.MAKE) ? BuildToolDescriptor.NATIVE.id :
                (desc == BuildToolDescriptor.VISUALSTUDIO) ? BuildToolDescriptor.MSBUILD.id : desc.id

        if (declaredProperties[buildPropertiesKey] != null) {
            buildProperties += declaredProperties[buildPropertiesKey]
        }
        return buildProperties
    }

    void validateBuildProperties(String buildXmlFilePath) {
        def xsd = BuildXmlParser.classLoader.getResource("ubc-schema.xsd")
        def schema = factory.newSchema(new StreamSource(xsd.newReader()))
        def validator = schema.newValidator()
        validator.validate(new StreamSource(new FileReader(buildXmlFilePath)))
    }
}
