package com.ubc.plugin.msbuild

import groovy.xml.dom.DOMCategory
import org.apache.xerces.parsers.DOMParser
import org.apache.xml.serialize.OutputFormat
import org.apache.xml.serialize.XMLSerializer
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import org.w3c.dom.Element

class SetupMSBuildTask extends DefaultTask {

    @OutputFile
    File nugetExe = project.msBuildPlugin.nugetExe
    @OutputFile
    File nunitExe = project.msBuildPlugin.nunitExe
    @OutputFile
    File openCoverExe = project.msBuildPlugin.openCoverExe
    @OutputFile
    File openCoverCoberturaExe = project.msBuildPlugin.openCoverCoberturaExe
    @OutputFile
    File sonarRunnerExe = project.msBuildPlugin.sonarRunnerExe

    /**
     * The Gradle action that sets up NuGet and MSBuild
     */
    @TaskAction
    def SetupMSBuildTools() {
        setupNuGet()
        if (project.msBuildPlugin.daBuild) {
            setupNUnit()
            setupOpenCover()
            setupOpenCoverCobertura()
        }
        setupMSBuild()
        if (project.msBuildPlugin.analysisBuild) {
            setupSonarRunner()
        }
    }

    /**
     * Extract NUnit from the .nupkg
     */
    def setupNUnit() {
        println ">>> Extracting NUnit v$project.msBuildPlugin.nunitVersion executable...\n"

        project.copy {
            from { project.zipTree { project.msBuildPlugin.nunitZip } }
            into { "clean-room-tests/nunit" }
        }
    }

    /**
     * Extract OpenCover from the .nupkg
     */
    def setupOpenCover() {
        println ">>> Extracting OpenCover v$project.msBuildPlugin.openCoverVersion executable...\n"

        project.copy {
            from { project.zipTree { project.msBuildPlugin.openCoverZip } }
            into { "clean-room-tests/opencover" }
        }
    }

    /**
     * Extract OpenCoverToCoberturaConverter from the .nupkg
     */
    def setupOpenCoverCobertura() {
        println ">>> Extracting OpenCoverToCoberturaConverter v$project.msBuildPlugin.openCoverCoberturaVersion executable...\n"

        project.copy {
            from { project.zipTree { project.msBuildPlugin.openCoverCoberturaZip } }
            into { "clean-room-tests/opencover" }
        }
    }

    /**
     * Extract NuGet from the .nupkg
     */
    def setupNuGet() {
        println "\n>>> Extracting NuGet v$project.msBuildPlugin.nugetVersion executable...\n"

        project.copy {
            from({ project.zipTree { project.msBuildPlugin.nugetZip } }) {
                include 'tools/NuGet.exe'
            }
            into { "clean-room-nuget" }
        }

        println ">>> Creating NuGet configuration file...\n"

        new File(project.msBuildPlugin.nugetConfig.path).withWriter { writer ->
            writer << this.class.classLoader.getResourceAsStream("NuGet.config")
        }

        project.ant.replace(file: project.msBuildPlugin.nugetConfig.path) {
            replacefilter(token: "@username@", value: project.msBuildPlugin.odyUsername)
            replacefilter(token: "@password@", value: encryptedPassword)
        }

        if (project.properties.containsKey('stageEnv')) {
            project.ant.replace(file: project.msBuildPlugin.nugetConfig.path) {
                replacefilter(token: "@isStage@", value: "-stage")
            }
        } else {
            project.ant.replace(file: project.msBuildPlugin.nugetConfig.path) {
                replacefilter(token: "@isStage@", value: "")
            }
        }
    }

    /**
     * Install MSBuild Tools
     */
    def setupMSBuild() {
//        println ">>> I totally just setup MSBuild...\n"
    }

    def setupSonarRunner() {
        println ">>> Extracting MSBuild SonarRunner v$project.msBuildPlugin.sonarRunnerVersion executable...\n"

        project.msBuildPlugin.sonarRunnerDir.mkdirs()
        project.copy {
            from { project.zipTree { project.msBuildPlugin.sonarRunnerZip } }
            into { project.msBuildPlugin.sonarRunnerDir.path }
        }

        project.copy {
            from { project.file("$project.msBuildPlugin.sonarRunnerDir/lib")}
            into { project.msBuildPlugin.sonarRunnerExe.parent }
        }

        def sonarAnalysisXml = project.file("$project.msBuildPlugin.sonarRunnerExe.parent/SonarQube.Analysis.xml")
        DOMParser parser = new DOMParser()
        parser.parse(sonarAnalysisXml.toURI().toString())
        Document document = parser.getDocument()
        def root = document.getDocumentElement()
        configureSonarXml(document, root, project)

        def fileOS = new FileOutputStream(sonarAnalysisXml.absolutePath)
        XMLSerializer serializer = new XMLSerializer(fileOS, new OutputFormat(document))
        serializer.serialize(document)
        serializer.reset()
        fileOS.close()
    }

    void configureSonarXml(Document document, Element root, Project project) {

        def indentedAppendChild = { Element parent, Element element ->
            def indent = "    ", newElement, elem = parent
            while (elem && elem != root) {
                indent += "    ";
                elem = elem.parentNode;
            }

            if (parent.hasChildNodes() && parent.lastChild.nodeType == 3 && (parent.lastChild.textContent =~ /^\s*[\r\n]\s*$/).count > 0) {
                parent.insertBefore(document.createTextNode("\n" + indent), parent.lastChild)
                newElement = parent.insertBefore(element, parent.lastChild)
            } else {
                parent.appendChild(document.createTextNode("\n" + indent))
                newElement = parent.appendChild(element)
                parent.appendChild(document.createTextNode("\n" + (indent - "    ")))
            }
            return newElement
        }

        def createElement = { Element existingElement, String newElement, String attributeName, String attributeValue, String newElementVal ->
            def element = document.createElement(newElement)
            element.setAttribute(attributeName, attributeValue)
            element.appendChild(document.createTextNode(newElementVal))
            return indentedAppendChild(existingElement, element)
        }

        def addSonarProperties = { rootNode ->
            createElement(rootNode, 'Property', 'Name', 'sonar.host.url', "http://$project.msBuildPlugin.sonar.sonarServer/sonar")
            createElement(rootNode, 'Property', 'Name', 'sonar.login', project.msBuildPlugin.sonar.sonarUsername)
            createElement(rootNode, 'Property', 'Name', 'sonar.password', project.msBuildPlugin.sonar.sonarPassword)
            createElement(rootNode, 'Property', 'Name', 'sonar.scm.disabled', 'true')
        }

        use(DOMCategory) {
            def propertyNodeList = root.getElementsByTagName('Property')
            def nodeIter = propertyNodeList.iterator()
            while (nodeIter.hasNext()) {
                root.removeChild(nodeIter.next())
            }
            addSonarProperties(root)
        }
    }
}